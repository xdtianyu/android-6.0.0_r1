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

package com.android.cts.devicepolicy;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.InstrumentationResultParser;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.ddmlib.testrunner.TestResult;
import com.android.ddmlib.testrunner.TestResult.TestStatus;
import com.android.ddmlib.testrunner.TestRunResult;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Base class for device policy tests. It offers utility methods to run tests, set device or profile
 * owner, etc.
 */
public class BaseDevicePolicyTest extends DeviceTestCase implements IBuildReceiver {

    private static final String RUNNER = "android.support.test.runner.AndroidJUnitRunner";

    protected CtsBuildHelper mCtsBuild;

    private String mPackageVerifier;
    private HashSet<String> mAvailableFeatures;
    protected boolean mHasFeature;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertNotNull(mCtsBuild);  // ensure build has been set before test is run.
        mHasFeature = getDevice().getApiLevel() >= 21 /* Build.VERSION_CODES.L */
                && hasDeviceFeature("android.software.device_admin");
        // disable the package verifier to avoid the dialog when installing an app
        mPackageVerifier = getDevice().executeShellCommand(
                "settings get global package_verifier_enable");
        getDevice().executeShellCommand("settings put global package_verifier_enable 0");
    }

    @Override
    protected void tearDown() throws Exception {
        // reset the package verifier setting to its original value
        getDevice().executeShellCommand("settings put global package_verifier_enable "
                + mPackageVerifier);
        super.tearDown();
    }

    protected void installApp(String fileName)
            throws FileNotFoundException, DeviceNotAvailableException {
        CLog.logAndDisplay(LogLevel.INFO, "Installing app " + fileName);
        String installResult = getDevice().installPackage(mCtsBuild.getTestApp(fileName), true);
        assertNull(String.format("Failed to install %s, Reason: %s", fileName, installResult),
                installResult);
    }

    protected void installAppAsUser(String appFileName, int userId) throws FileNotFoundException,
            DeviceNotAvailableException {
        final ITestDevice device = getDevice();

        final File apk = mCtsBuild.getTestApp(appFileName);
        final String remotePath = "/data/local/tmp/" + apk.getName();
        if (!device.pushFile(apk, remotePath)) {
            throw new IllegalStateException("Failed to push " + apk);
        }

        final String result = device.executeShellCommand(
                "pm install -r --user " + userId + " " + remotePath);
        assertTrue(result, result.contains("\nSuccess"));
    }

    /** Initializes the user with the given id. This is required so that apps can run on it. */
    protected void startUser(int userId) throws Exception {
        String command = "am start-user " + userId;
        CLog.logAndDisplay(LogLevel.INFO, "Starting command " + command);
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);
        assertTrue(commandOutput + " expected to start with \"Success:\"",
                commandOutput.startsWith("Success:"));
    }

    protected int getMaxNumberOfUsersSupported() throws DeviceNotAvailableException {
        // TODO: move this to ITestDevice once it supports users
        String command = "pm get-max-users";
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.i("Output for command " + command + ": " + commandOutput);

        try {
            return Integer.parseInt(commandOutput.substring(commandOutput.lastIndexOf(" ")).trim());
        } catch (NumberFormatException e) {
            fail("Failed to parse result: " + commandOutput);
        }
        return 0;
    }

    protected ArrayList<Integer> listUsers() throws DeviceNotAvailableException {
        String command = "pm list users";
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.i("Output for command " + command + ": " + commandOutput);

        // Extract the id of all existing users.
        String[] lines = commandOutput.split("\\r?\\n");
        assertTrue(commandOutput + " should contain at least one line", lines.length >= 1);
        assertEquals(commandOutput, lines[0], "Users:");

        ArrayList<Integer> users = new ArrayList<Integer>();
        for (int i = 1; i < lines.length; i++) {
            // Individual user is printed out like this:
            // \tUserInfo{$id$:$name$:$Integer.toHexString(flags)$} [running]
            String[] tokens = lines[i].split("\\{|\\}|:");
            assertTrue(lines[i] + " doesn't contain 4 or 5 tokens",
                    tokens.length == 4 || tokens.length == 5);
            users.add(Integer.parseInt(tokens[1]));
        }
        return users;
    }

    protected void removeUser(int userId) throws Exception  {
        String stopUserCommand = "am stop-user -w " + userId;
        CLog.logAndDisplay(LogLevel.INFO, "starting command \"" + stopUserCommand + "\" and waiting.");
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + stopUserCommand + ": "
                + getDevice().executeShellCommand(stopUserCommand));
        String removeUserCommand = "pm remove-user " + userId;
        CLog.logAndDisplay(LogLevel.INFO, "starting command " + removeUserCommand);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + removeUserCommand + ": "
                + getDevice().executeShellCommand(removeUserCommand));
    }

    protected void removeTestUsers() throws Exception {
        for (int userId : listUsers()) {
            if (userId != 0) {
                removeUser(userId);
            }
        }
    }

    /** Returns true if the specified tests passed. Tests are run as user owner. */
    protected boolean runDeviceTests(String pkgName, @Nullable String testClassName)
            throws DeviceNotAvailableException {
        return runDeviceTests(pkgName, testClassName, null /*testMethodName*/, null /*userId*/);
    }

    /** Returns true if the specified tests passed. Tests are run as given user. */
    protected boolean runDeviceTestsAsUser(
            String pkgName, @Nullable String testClassName, int userId)
            throws DeviceNotAvailableException {
        return runDeviceTestsAsUser(pkgName, testClassName, null, userId);
    }

    /** Returns true if the specified tests passed. Tests are run as given user. */
    protected boolean runDeviceTestsAsUser(
            String pkgName, @Nullable String testClassName, String testMethodName, int userId)
            throws DeviceNotAvailableException {
        return runDeviceTests(pkgName, testClassName, testMethodName, userId);
    }

    protected boolean runDeviceTests(String pkgName, @Nullable String testClassName,
            @Nullable String testMethodName, @Nullable Integer userId)
            throws DeviceNotAvailableException {
        return runDeviceTests(pkgName, testClassName, testMethodName, userId, /*params*/ null);
    }

    protected boolean runDeviceTests(String pkgName, @Nullable String testClassName,
            @Nullable String testMethodName, @Nullable Integer userId, @Nullable String params)
                   throws DeviceNotAvailableException {
        if (testClassName != null && testClassName.startsWith(".")) {
            testClassName = pkgName + testClassName;
        }
        TestRunResult runResult = (userId == null && params == null)
                ? doRunTests(pkgName, testClassName, testMethodName)
                : doRunTestsAsUser(pkgName, testClassName, testMethodName,
                        userId != null ? userId : 0, params != null ? params : "");
        printTestResult(runResult);
        return !runResult.hasFailedTests() && runResult.getNumTestsInState(TestStatus.PASSED) > 0;
    }

    /** Helper method to run tests and return the listener that collected the results. */
    private TestRunResult doRunTests(
            String pkgName, String testClassName,
            String testMethodName) throws DeviceNotAvailableException {
        RemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(
                pkgName, RUNNER, getDevice().getIDevice());
        if (testClassName != null && testMethodName != null) {
            testRunner.setMethodName(testClassName, testMethodName);
        } else if (testClassName != null) {
            testRunner.setClassName(testClassName);
        }

        CollectingTestListener listener = new CollectingTestListener();
        assertTrue(getDevice().runInstrumentationTests(testRunner, listener));
        return listener.getCurrentRunResults();
    }

    private TestRunResult doRunTestsAsUser(String pkgName, @Nullable String testClassName,
            @Nullable String testMethodName, int userId, String params)
            throws DeviceNotAvailableException {
        // TODO: move this to RemoteAndroidTestRunner once it supports users. Should be straight
        // forward to add a RemoteAndroidTestRunner.setUser(userId) method. Then we can merge both
        // doRunTests* methods.
        StringBuilder testsToRun = new StringBuilder();
        if (testClassName != null) {
            testsToRun.append("-e class " + testClassName);
            if (testMethodName != null) {
                testsToRun.append("#" + testMethodName);
            }
        }
        String command = "am instrument --user " + userId + " " + params + " -w -r "
                + testsToRun + " " + pkgName + "/" + RUNNER;
        CLog.i("Running " + command);

        CollectingTestListener listener = new CollectingTestListener();
        InstrumentationResultParser parser = new InstrumentationResultParser(pkgName, listener);
        getDevice().executeShellCommand(command, parser);
        return listener.getCurrentRunResults();
    }

    private void printTestResult(TestRunResult runResult) {
        for (Map.Entry<TestIdentifier, TestResult> testEntry :
                runResult.getTestResults().entrySet()) {
            TestResult testResult = testEntry.getValue();
            CLog.logAndDisplay(LogLevel.INFO,
                    "Test " + testEntry.getKey() + ": " + testResult.getStatus());
            if (testResult.getStatus() != TestStatus.PASSED) {
                CLog.logAndDisplay(LogLevel.WARN, testResult.getStackTrace());
            }
        }
    }

    protected boolean hasDeviceFeature(String requiredFeature) throws DeviceNotAvailableException {
        if (mAvailableFeatures == null) {
            // TODO: Move this logic to ITestDevice.
            String command = "pm list features";
            String commandOutput = getDevice().executeShellCommand(command);
            CLog.i("Output for command " + command + ": " + commandOutput);

            // Extract the id of the new user.
            mAvailableFeatures = new HashSet<>();
            for (String feature: commandOutput.split("\\s+")) {
                // Each line in the output of the command has the format "feature:{FEATURE_VALUE}".
                String[] tokens = feature.split(":");
                assertTrue("\"" + feature + "\" expected to have format feature:{FEATURE_VALUE}",
                        tokens.length > 1);
                assertEquals(feature, "feature", tokens[0]);
                mAvailableFeatures.add(tokens[1]);
            }
        }
        boolean result = mAvailableFeatures.contains(requiredFeature);
        if (!result) {
            CLog.logAndDisplay(LogLevel.INFO, "Device doesn't have required feature "
            + requiredFeature + ". Test won't run.");
        }
        return result;
    }

    protected int createUser() throws Exception {
        String command ="pm create-user TestUser_"+ System.currentTimeMillis();
        CLog.logAndDisplay(LogLevel.INFO, "Starting command " + command);
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);

        // Extract the id of the new user.
        String[] tokens = commandOutput.split("\\s+");
        assertTrue(tokens.length > 0);
        assertEquals("Success:", tokens[0]);
        return Integer.parseInt(tokens[tokens.length-1]);
    }

    protected int createManagedProfile() throws DeviceNotAvailableException {
        String command =
                "pm create-user --profileOf 0 --managed TestProfile_" + System.currentTimeMillis();
        CLog.logAndDisplay(LogLevel.INFO, "Starting command " + command);
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);

        // Extract the id of the new user.
        String[] tokens = commandOutput.split("\\s+");
        assertTrue(commandOutput + " expected to have format \"Success: {USER_ID}\"",
                tokens.length > 0);
        assertEquals(commandOutput, "Success:", tokens[0]);
        return Integer.parseInt(tokens[tokens.length-1]);
    }

    protected int getUserSerialNumber(int userId) throws DeviceNotAvailableException{
        // dumpsys user return lines like "UserInfo{0:Owner:13} serialNo=0"
        String commandOutput = getDevice().executeShellCommand("dumpsys user");
        String[] tokens = commandOutput.split("\\n");
        for (String token : tokens) {
            token = token.trim();
            if (token.contains("UserInfo{" + userId + ":")) {
                String[] split = token.split("serialNo=");
                assertTrue(split.length == 2);
                int serialNumber = Integer.parseInt(split[1]);
                CLog.logAndDisplay(LogLevel.INFO, "Serial number of user " + userId + ": "
                        + serialNumber);
                return serialNumber;
            }
        }
        fail("Couldn't find user " + userId);
        return -1;
    }

    protected boolean setProfileOwner(String componentName, int userId)
            throws DeviceNotAvailableException {
        String command = "dpm set-profile-owner --user " + userId + " '" + componentName + "'";
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);
        return commandOutput.startsWith("Success:");
    }

    protected void setProfileOwnerOrFail(String componentName, int userId)
            throws Exception {
        if (!setProfileOwner(componentName, userId)) {
            removeUser(userId);
            fail("Failed to set profile owner");
        }
    }

    protected void setDeviceAdmin(String componentName) throws DeviceNotAvailableException {
        String command = "dpm set-active-admin '" + componentName + "'";
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);
        assertTrue(commandOutput + " expected to start with \"Success:\"",
                commandOutput.startsWith("Success:"));
    }

    protected boolean setDeviceOwner(String componentName) throws DeviceNotAvailableException {
        String command = "dpm set-device-owner '" + componentName + "'";
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);
        return commandOutput.startsWith("Success:");
    }

    protected String getSettings(String namespace, String name, int userId)
            throws DeviceNotAvailableException {
        String command = "settings --user " + userId + " get " + namespace + " " + name;
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);
        return commandOutput.replace("\n", "").replace("\r", "");
    }

    protected void putSettings(String namespace, String name, String value, int userId)
            throws DeviceNotAvailableException {
        String command = "settings --user " + userId + " put " + namespace + " " + name
                + " " + value;
        String commandOutput = getDevice().executeShellCommand(command);
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": " + commandOutput);
    }
}
