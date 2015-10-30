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

package android.sample.cts;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.util.AbiUtils;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.testtype.IAbiReceiver;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;
import java.lang.String;
import java.util.Scanner;

/**
 * Test to check the APK logs to Logcat.
 *
 * When this test builds, it also builds {@link android.sample.app.SampleDeviceActivity} into an APK
 * which it then installs at runtime and starts. The activity simply prints a message to Logcat and
 * then gets uninstalled.
 */
public class SampleHostTest extends DeviceTestCase implements IAbiReceiver, IBuildReceiver {

    /**
     * The package name of the APK.
     */
    private static final String PACKAGE = "android.sample.app";

    /**
     * The file name of the APK.
     */
    private static final String APK = "CtsSampleDeviceApp.apk";

    /**
     * The class name of the main activity in the APK.
     */
    private static final String CLASS = "SampleDeviceActivity";

    /**
     * The command to launch the main activity.
     */
    private static final String START_COMMAND = String.format(
            "am start -W -a android.intent.action.MAIN -n %s/%s.%s", PACKAGE, PACKAGE, CLASS);

    /**
     * The test string to look for.
     */
    private static final String TEST_STRING = "SampleTestString";

    /**
     * The ABI to use.
     */
    private IAbi mAbi;

    /**
     * A reference to the build.
     */
    private CtsBuildHelper mBuild;

    /**
     * A reference to the device under test.
     */
    private ITestDevice mDevice;

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
        // Get the device, this gives a handle to run commands and install APKs.
        mDevice = getDevice();
        // Remove any previously installed versions of this APK.
        mDevice.uninstallPackage(PACKAGE);
        // Get the APK from the build.
        File app = mBuild.getTestApp(APK);
        // Get the ABI flag.
        String[] options = {AbiUtils.createAbiFlag(mAbi.getName())};
        // Install the APK on the device.
        mDevice.installPackage(app, false, options);
    }

    @Override
    protected void tearDown() throws Exception {
        // Remove the package once complete.
        mDevice.uninstallPackage(PACKAGE);
        super.tearDown();
    }

    /**
     * Tests the string was successfully logged to Logcat from the activity.
     *
     * @throws Exception
     */
    public void testLogcat() throws Exception {
        // Clear logcat.
        mDevice.executeAdbCommand("logcat", "-c");
        // Start the APK and wait for it to complete.
        mDevice.executeShellCommand(START_COMMAND);
        // Dump logcat.
        String logs = mDevice.executeAdbCommand("logcat", "-v", "brief", "-d", CLASS + ":I", "*:S");
        // Search for string.
        String testString = "";
        Scanner in = new Scanner(logs);
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if(line.startsWith("I/"+CLASS)) {
                testString = line.split(":")[1].trim();
            }
        }
        in.close();
        // Assert the logged string matches the test string.
        assertEquals("Incorrect test string", TEST_STRING, testString);
    }
}
