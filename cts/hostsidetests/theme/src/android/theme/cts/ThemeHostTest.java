/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.theme.cts;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.util.AbiUtils;
import com.android.cts.util.TimeoutReq;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.testtype.IAbiReceiver;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.String;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test to check non-modifiable themes have not been changed.
 */
public class ThemeHostTest extends DeviceTestCase implements IAbiReceiver, IBuildReceiver {
    private static final String LOG_TAG = "ThemeHostTest";
    private static final String APK_NAME = "CtsThemeDeviceApp";
    private static final String APP_PACKAGE_NAME = "android.theme.app";

    private static final String GENERATED_ASSETS_ZIP = "/sdcard/cts-theme-assets.zip";

    /** The class name of the main activity in the APK. */
    private static final String CLASS = "GenerateImagesActivity";

    /** The command to launch the main activity. */
    private static final String START_CMD = String.format(
            "am start -W -a android.intent.action.MAIN -n %s/%s.%s", APP_PACKAGE_NAME,
            APP_PACKAGE_NAME, CLASS);

    private static final String CLEAR_GENERATED_CMD = "rm -rf %s/*.png";
    private static final String STOP_CMD = String.format("am force-stop %s", APP_PACKAGE_NAME);
    private static final String HARDWARE_TYPE_CMD = "dumpsys | grep android.hardware.type";
    private static final String DENSITY_PROP_DEVICE = "ro.sf.lcd_density";
    private static final String DENSITY_PROP_EMULATOR = "qemu.sf.lcd_density";

    private final HashMap<String, File> mReferences = new HashMap<>();

    /** The ABI to use. */
    private IAbi mAbi;

    /** A reference to the build. */
    private CtsBuildHelper mBuild;

    /** A reference to the device under test. */
    private ITestDevice mDevice;

    private ExecutorService mExecutionService;

    private ExecutorCompletionService<Boolean> mCompletionService;

    @Override
    public void setAbi(IAbi abi) {
        mAbi = abi;
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        // Get the build, this is used to access the APK.
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mDevice = getDevice();
        mDevice.uninstallPackage(APP_PACKAGE_NAME);

        // Get the APK from the build.
        final File app = mBuild.getTestApp(String.format("%s.apk", APK_NAME));
        final String[] options = {AbiUtils.createAbiFlag(mAbi.getName())};

        mDevice.installPackage(app, false, options);

        final String density = getDensityBucketForDevice(mDevice);
        final String zipFile = String.format("/%s.zip", density);
        Log.logAndDisplay(LogLevel.INFO, LOG_TAG, "Loading resources from " + zipFile);

        final InputStream zipStream = ThemeHostTest.class.getResourceAsStream(zipFile);
        if (zipStream != null) {
            final ZipInputStream in = new ZipInputStream(zipStream);
            try {
                ZipEntry ze;
                final byte[] buffer = new byte[1024];
                while ((ze = in.getNextEntry()) != null) {
                    final String name = ze.getName();
                    final File tmp = File.createTempFile("ref_" + name, ".png");
                    final FileOutputStream out = new FileOutputStream(tmp);

                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }

                    out.flush();
                    out.close();
                    mReferences.put(name, tmp);
                }
            } catch (IOException e) {
                Log.logAndDisplay(LogLevel.ERROR, LOG_TAG, "Failed to unzip assets: " + zipFile);
            } finally {
                in.close();
            }
        } else {
            Log.logAndDisplay(LogLevel.ERROR, LOG_TAG, "Failed to get resource: " + zipFile);
        }

        final int numCores = Runtime.getRuntime().availableProcessors();
        mExecutionService = Executors.newFixedThreadPool(numCores * 2);
        mCompletionService = new ExecutorCompletionService<>(mExecutionService);
    }

    @Override
    protected void tearDown() throws Exception {
        // Delete the temp files
        for (File ref : mReferences.values()) {
            ref.delete();
        }

        mExecutionService.shutdown();

        // Remove the APK.
        mDevice.uninstallPackage(APP_PACKAGE_NAME);

        // Remove generated images.
        mDevice.executeShellCommand(CLEAR_GENERATED_CMD);

        super.tearDown();
    }

    @TimeoutReq(minutes = 60)
    public void testThemes() throws Exception {
        if (checkHardwareTypeSkipTest(mDevice.executeShellCommand(HARDWARE_TYPE_CMD).trim())) {
            Log.logAndDisplay(LogLevel.INFO, LOG_TAG, "Skipped themes test for watch");
            return;
        }

        if (mReferences.isEmpty()) {
            Log.logAndDisplay(LogLevel.INFO, LOG_TAG, "Skipped themes test due to no reference images");
            return;
        }

        Log.logAndDisplay(LogLevel.INFO, LOG_TAG, "Generating device images...");

        assertTrue("Aborted image generation", generateDeviceImages());

        // Pull ZIP file from remote device.
        final File localZip = File.createTempFile("generated", ".zip");
        mDevice.pullFile(GENERATED_ASSETS_ZIP, localZip);

        int numTasks = 0;

        Log.logAndDisplay(LogLevel.INFO, LOG_TAG, "Extracting generated images...");

        // Extract generated images to temporary files.
        final byte[] data = new byte[4096];
        final ZipInputStream zipInput = new ZipInputStream(new FileInputStream(localZip));
        ZipEntry entry;
        while ((entry = zipInput.getNextEntry()) != null) {
            final String name = entry.getName();
            final File expected = mReferences.get(name);
            if (expected != null && expected.exists()) {
                final File actual = File.createTempFile("actual_" + name, ".png");
                final FileOutputStream pngOutput = new FileOutputStream(actual);

                int count;
                while ((count = zipInput.read(data, 0, data.length)) != -1) {
                    pngOutput.write(data, 0, count);
                }

                pngOutput.flush();
                pngOutput.close();

                mCompletionService.submit(new ComparisonTask(mDevice, expected, actual));
                numTasks++;
            } else {
                Log.logAndDisplay(LogLevel.INFO, LOG_TAG, "Missing reference image for " + name);
            }

            zipInput.closeEntry();
        }

        zipInput.close();

        Log.logAndDisplay(LogLevel.INFO, LOG_TAG, "Waiting for comparison tasks...");

        int failures = 0;
        for (int i = numTasks; i > 0; i--) {
            failures += mCompletionService.take().get() ? 0 : 1;
        }

        assertTrue(failures + " failures in theme test", failures == 0);

        Log.logAndDisplay(LogLevel.INFO, LOG_TAG, "Finished!");
    }

    private boolean generateDeviceImages() throws Exception {
        // Clear logcat
        mDevice.executeAdbCommand("logcat", "-c");

        // Stop any existing instances
        mDevice.executeShellCommand(STOP_CMD);

        // Start activity
        mDevice.executeShellCommand(START_CMD);

        Log.logAndDisplay(LogLevel.VERBOSE, LOG_TAG, "Starting image generation...");

        boolean aborted = false;
        boolean waiting = true;
        do {
            // Dump logcat.
            final String logs = mDevice.executeAdbCommand(
                    "logcat", "-v", "brief", "-d", CLASS + ":I", "*:S");

            // Search for string.
            final Scanner in = new Scanner(logs);
            while (in.hasNextLine()) {
                final String line = in.nextLine();
                if (line.startsWith("I/" + CLASS)) {
                    final String[] lineSplit = line.split(":");
                    if (lineSplit.length >= 3) {
                        final String cmd = lineSplit[1].trim();
                        final String arg = lineSplit[2].trim();
                        switch (cmd) {
                            case "FAIL":
                                Log.logAndDisplay(LogLevel.WARN, LOG_TAG, line);
                                Log.logAndDisplay(LogLevel.WARN, LOG_TAG, "Aborting! Check host logs for details.");
                                aborted = true;
                                // fall-through
                            case "OKAY":
                                waiting = false;
                                break;
                        }
                    }
                }
            }
            in.close();
        } while (waiting && !aborted);

        Log.logAndDisplay(LogLevel.VERBOSE, LOG_TAG, "Image generation completed!");

        return !aborted;
    }

    private static String getDensityBucketForDevice(ITestDevice device) {
        final String densityProp;
        if (device.getSerialNumber().startsWith("emulator-")) {
            densityProp = DENSITY_PROP_EMULATOR;
        } else {
            densityProp = DENSITY_PROP_DEVICE;
        }

        final int density;
        try {
            density = Integer.parseInt(device.getProperty(densityProp));
        } catch (DeviceNotAvailableException e) {
            return "unknown";
        }

        switch (density) {
            case 120:
                return "ldpi";
            case 160:
                return "mdpi";
            case 213:
                return "tvdpi";
            case 240:
                return "hdpi";
            case 320:
                return "xhdpi";
            case 400:
                return "400dpi";
            case 480:
                return "xxhdpi";
            case 560:
                return "560dpi";
            case 640:
                return "xxxhdpi";
            default:
                return "" + density;
        }
    }

    private static boolean checkHardwareTypeSkipTest(String hardwareTypeString) {
        if (hardwareTypeString.contains("android.hardware.type.watch")) {
            return true;
        }

        return false;
    }
}
