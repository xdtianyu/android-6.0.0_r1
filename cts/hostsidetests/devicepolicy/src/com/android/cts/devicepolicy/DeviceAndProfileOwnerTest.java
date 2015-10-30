/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;

import java.io.File;

/**
 * Set of tests for usecases that apply to profile and device owner.
 * This class is the base class of MixedProfileOwnerTest and MixedDeviceOwnerTest and is abstract
 * to avoid running spurious tests.
 */
public abstract class DeviceAndProfileOwnerTest extends BaseDevicePolicyTest {

    protected static final String DEVICE_ADMIN_PKG = "com.android.cts.deviceandprofileowner";
    protected static final String DEVICE_ADMIN_APK = "CtsDeviceAndProfileOwnerApp.apk";
    protected static final String ADMIN_RECEIVER_TEST_CLASS
            = ".BaseDeviceAdminTest$BasicAdminReceiver";

    private static final String PERMISSIONS_APP_PKG = "com.android.cts.permissionapp";
    private static final String PERMISSIONS_APP_APK = "CtsPermissionApp.apk";

    private static final String SIMPLE_PRE_M_APP_PKG = "com.android.cts.launcherapps.simplepremapp";
    private static final String SIMPLE_PRE_M_APP_APK = "CtsSimplePreMApp.apk";

    private static final String CERT_INSTALLER_PKG = "com.android.cts.certinstaller";
    private static final String CERT_INSTALLER_APK = "CtsCertInstallerApp.apk";

    private static final String TEST_APP_APK = "CtsSimpleApp.apk";
    private static final String TEST_APP_PKG = "com.android.cts.launcherapps.simpleapp";
    private static final String TEST_APP_LOCATION = "/data/local/tmp/";

    private static final String PACKAGE_INSTALLER_PKG = "com.android.cts.packageinstaller";
    private static final String PACKAGE_INSTALLER_APK = "CtsPackageInstallerApp.apk";

    protected static final int USER_OWNER = 0;

    private static final String ADD_RESTRICTION_COMMAND = "add-restriction";
    private static final String CLEAR_RESTRICTION_COMMAND = "clear-restriction";

    // ID of the user all tests are run as. For device owner this will be 0, for profile owner it
    // is the user id of the created profile.
    protected int mUserId;

    @Override
    protected void tearDown() throws Exception {
        if (mHasFeature) {
            getDevice().uninstallPackage(DEVICE_ADMIN_PKG);
            getDevice().uninstallPackage(PERMISSIONS_APP_PKG);
            getDevice().uninstallPackage(SIMPLE_PRE_M_APP_PKG);
            getDevice().uninstallPackage(CERT_INSTALLER_PKG);
        }
        super.tearDown();
    }

    public void testApplicationRestrictions() throws Exception {
        if (!mHasFeature) {
            return;
        }
        executeDeviceTestClass(".ApplicationRestrictionsTest");
    }

    public void testPermissionGrant() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionGrantState");
    }

    public void testPermissionPolicy() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionPolicy");
    }

    public void testPermissionMixedPolicies() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionMixedPolicies");
    }

    public void testPermissionPrompts() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionPrompts");
    }

    public void testPermissionAppUpdate() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_setDeniedState");
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkDenied");
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkDenied");

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_setGrantedState");
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkGranted");
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkGranted");

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_setAutoDeniedPolicy");
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkDenied");
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkDenied");

        assertNull(getDevice().uninstallPackage(PERMISSIONS_APP_PKG));
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_setAutoGrantedPolicy");
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkGranted");
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionUpdate_checkGranted");
    }

    public void testPermissionGrantPreMApp() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(SIMPLE_PRE_M_APP_APK, mUserId);
        executeDeviceTestMethod(".PermissionsTest", "testPermissionGrantStatePreMApp");
    }

    public void testPersistentIntentResolving() throws Exception {
        if (!mHasFeature) {
            return;
        }
        executeDeviceTestClass(".PersistentIntentResolvingTest");
    }

    public void testScreenCaptureDisabled() throws Exception {
        if (!mHasFeature) {
            return;
        }
        // We need to ensure that the policy is deactivated for the device owner case, so making
        // sure the second test is run even if the first one fails
        try {
            executeDeviceTestMethod(".ScreenCaptureDisabledTest",
                    "testSetScreenCaptureDisabled_true");
        } finally {
            executeDeviceTestMethod(".ScreenCaptureDisabledTest",
                    "testSetScreenCaptureDisabled_false");
        }
    }

    public void testApplicationHidden() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(PERMISSIONS_APP_APK, mUserId);
        executeDeviceTestClass(".ApplicationHiddenTest");
    }

    public void testAccountManagement() throws Exception {
        if (!mHasFeature) {
            return;
        }

        executeDeviceTestClass(".AccountManagementTest");

        // Send a home intent to dismiss an error dialog.
        String command = "am start -a android.intent.action.MAIN -c android.intent.category.HOME";
        CLog.logAndDisplay(LogLevel.INFO, "Output for command " + command + ": "
                + getDevice().executeShellCommand(command));
    }

    public void testDelegatedCertInstaller() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installAppAsUser(CERT_INSTALLER_APK, mUserId);
        installAppAsUser(DEVICE_ADMIN_APK, USER_OWNER);
        setDeviceAdmin(DEVICE_ADMIN_PKG + "/.PrimaryUserDeviceAdmin");

        final String adminHelperClass = ".PrimaryUserAdminHelper";
        try {
            // Set a non-empty device lockscreen password, which is a precondition for installing
            // private key pairs.
            assertTrue("Set lockscreen password failed", runDeviceTestsAsUser(DEVICE_ADMIN_PKG,
                    adminHelperClass, "testSetPassword", 0 /* user 0 */));
            assertTrue("DelegatedCertInstaller failed", runDeviceTestsAsUser(DEVICE_ADMIN_PKG,
                    ".DelegatedCertInstallerTest", mUserId));
        } finally {
            // Reset lockscreen password and remove device admin.
            assertTrue("Clear lockscreen password failed", runDeviceTestsAsUser(DEVICE_ADMIN_PKG,
                    adminHelperClass, "testClearPassword", 0 /* user 0 */));
            assertTrue("Clear device admin failed", runDeviceTestsAsUser(DEVICE_ADMIN_PKG,
                    adminHelperClass, "testClearDeviceAdmin", 0 /* user 0 */));
        }
    }

    public void testPackageInstallUserRestrictions() throws Exception {
        // UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES
        final String DISALLOW_INSTALL_UNKNOWN_SOURCES = "no_install_unknown_sources";
        final String UNKNOWN_SOURCES_SETTING = "install_non_market_apps";
        final String SECURE_SETTING_CATEGORY = "secure";
        final File apk = mCtsBuild.getTestApp(TEST_APP_APK);
        String unknownSourceSetting = null;
        try {
            // Install the test and prepare the test apk.
            installApp(PACKAGE_INSTALLER_APK);
            assertTrue(getDevice().pushFile(apk, TEST_APP_LOCATION + apk.getName()));

            // Add restrictions and test if we can install the apk.
            getDevice().uninstallPackage(TEST_APP_PKG);
            changeUserRestrictionForUser(DISALLOW_INSTALL_UNKNOWN_SOURCES,
                    ADD_RESTRICTION_COMMAND, mUserId);
            assertTrue(runDeviceTestsAsUser(PACKAGE_INSTALLER_PKG, ".ManualPackageInstallTest",
                    "testManualInstallBlocked", mUserId));

            // Clear restrictions and test if we can install the apk.
            changeUserRestrictionForUser(DISALLOW_INSTALL_UNKNOWN_SOURCES,
                    CLEAR_RESTRICTION_COMMAND, mUserId);

            // Enable Unknown sources in Settings.
            unknownSourceSetting =
                    getSettings(SECURE_SETTING_CATEGORY, UNKNOWN_SOURCES_SETTING, mUserId);
            putSettings(SECURE_SETTING_CATEGORY, UNKNOWN_SOURCES_SETTING, "1", mUserId);
            assertEquals("1",
                    getSettings(SECURE_SETTING_CATEGORY, UNKNOWN_SOURCES_SETTING, mUserId));
            assertTrue(runDeviceTestsAsUser(PACKAGE_INSTALLER_PKG, ".ManualPackageInstallTest",
                    "testManualInstallSucceeded", mUserId));
        } finally {
            String command = "rm " + TEST_APP_LOCATION + apk.getName();
            getDevice().executeShellCommand(command);
            getDevice().uninstallPackage(TEST_APP_PKG);
            getDevice().uninstallPackage(PACKAGE_INSTALLER_APK);
            if (unknownSourceSetting != null) {
                putSettings(SECURE_SETTING_CATEGORY, UNKNOWN_SOURCES_SETTING, unknownSourceSetting,
                        mUserId);
            }
        }
    }

    protected void executeDeviceTestClass(String className) throws Exception {
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, className, mUserId));
    }

    protected void executeDeviceTestMethod(String className, String testName) throws Exception {
        assertTrue(runDeviceTestsAsUser(DEVICE_ADMIN_PKG, className, testName, mUserId));
    }

    private void changeUserRestrictionForUser(String key, String command, int userId)
            throws DeviceNotAvailableException {
        String adbCommand = "am start -W --user " + userId
                + " -c android.intent.category.DEFAULT "
                + " --es extra-command " + command
                + " --es extra-restriction-key " + key
                + " " + DEVICE_ADMIN_PKG + "/.UserRestrictionActivity";
        String commandOutput = getDevice().executeShellCommand(adbCommand);
        CLog.logAndDisplay(LogLevel.INFO,
                "Output for command " + adbCommand + ": " + commandOutput);
        assertTrue("Command was expected to succeed " + commandOutput,
                commandOutput.contains("Status: ok"));
    }
}
