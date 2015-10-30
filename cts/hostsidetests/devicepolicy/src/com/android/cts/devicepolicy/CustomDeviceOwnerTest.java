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

import java.io.File;
import java.lang.Exception;

/**
 * This class is used for tests that need to do something special before setting the device
 * owner, so they cannot use the regular DeviceOwnerTest class
 */
public class CustomDeviceOwnerTest extends BaseDevicePolicyTest {

    private static final String DEVICE_OWNER_PKG = "com.android.cts.deviceowner";
    private static final String DEVICE_OWNER_APK = "CtsDeviceOwnerApp.apk";
    private static final String DEVICE_OWNER_ADMIN
            = DEVICE_OWNER_PKG + ".BaseDeviceOwnerTest$BasicAdminReceiver";
    private static final String DEVICE_OWNER_ADMIN_COMPONENT
            = DEVICE_OWNER_PKG + "/" + DEVICE_OWNER_ADMIN;
    private static final String DEVICE_OWNER_CLEAR
            = DEVICE_OWNER_PKG + ".ClearDeviceOwnerTest";

    private static final String DEVICE_AND_PROFILE_OWNER_PKG
            = "com.android.cts.deviceandprofileowner";
    protected static final String DEVICE_AND_PROFILE_OWNER_APK = "CtsDeviceAndProfileOwnerApp.apk";
    protected static final String DEVICE_AND_PROFILE_OWNER_ADMIN
            = ".BaseDeviceAdminTest$BasicAdminReceiver";
    protected static final String DEVICE_AND_PROFILE_OWNER_ADMIN_COMPONENT
            = DEVICE_AND_PROFILE_OWNER_PKG + "/" + DEVICE_AND_PROFILE_OWNER_ADMIN;
    protected static final String DEVICE_AND_PROFILE_OWNER_CLEAR
            = DEVICE_AND_PROFILE_OWNER_PKG + ".ClearDeviceOwnerTest";

    private static final String INTENT_RECEIVER_PKG = "com.android.cts.intent.receiver";
    private static final String INTENT_RECEIVER_APK = "CtsIntentReceiverApp.apk";

    private static final String TEST_APP_APK = "CtsSimpleApp.apk";
    private static final String TEST_APP_PKG = "com.android.cts.launcherapps.simpleapp";
    private static final String TEST_APP_LOCATION = "/data/local/tmp/";

    private static final String PACKAGE_INSTALLER_PKG = "com.android.cts.packageinstaller";
    private static final String PACKAGE_INSTALLER_APK = "CtsPackageInstallerApp.apk";
    private static final String PACKAGE_INSTALLER_ADMIN_COMPONENT =
            PACKAGE_INSTALLER_PKG + "/" + ".ClearDeviceOwnerTest$BasicAdminReceiver";
    private static final String PACKAGE_INSTALLER_CLEAR_DEVICE_OWNER_TEST_CLASS =
            PACKAGE_INSTALLER_PKG + ".ClearDeviceOwnerTest";

    @Override
    public void tearDown() throws Exception {
        if (mHasFeature) {
            getDevice().uninstallPackage(DEVICE_OWNER_PKG);
            getDevice().uninstallPackage(DEVICE_AND_PROFILE_OWNER_PKG);
        }

        super.tearDown();
    }

    public void testOwnerChangedBroadcast() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installApp(DEVICE_OWNER_APK);
        try {
            installApp(INTENT_RECEIVER_APK);

            String testClass = INTENT_RECEIVER_PKG + ".OwnerChangedBroadcastTest";

            // Running this test also gets the intent receiver app out of the stopped state, so it
            // can receive broadcast intents.
            assertTrue(runDeviceTests(INTENT_RECEIVER_PKG, testClass,
                    "testOwnerChangedBroadcastNotReceived", 0));

            // Setting the device owner should send the owner changed broadcast.
            assertTrue(setDeviceOwner(DEVICE_OWNER_ADMIN_COMPONENT));

            assertTrue(runDeviceTests(INTENT_RECEIVER_PKG, testClass,
                    "testOwnerChangedBroadcastReceived", 0));
        } finally {
            getDevice().uninstallPackage(INTENT_RECEIVER_PKG);
            assertTrue("Failed to remove device owner.",
                    runDeviceTests(DEVICE_OWNER_PKG, DEVICE_OWNER_CLEAR));
        }
    }

    public void testCannotSetDeviceOwnerWhenSecondaryUserPresent() throws Exception {
        if (!mHasFeature || getMaxNumberOfUsersSupported() < 2) {
            return;
        }
        int userId = -1;
        installApp(DEVICE_OWNER_APK);
        try {
            userId = createUser();
            assertFalse(setDeviceOwner(DEVICE_OWNER_ADMIN_COMPONENT));
        } finally {
            removeUser(userId);
            // make sure we clean up in case we succeeded in setting the device owner
            runDeviceTests(DEVICE_OWNER_PKG, DEVICE_OWNER_CLEAR);
        }
    }

    public void testCannotSetDeviceOwnerWhenAccountPresent() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installApp(DEVICE_AND_PROFILE_OWNER_APK);
        try {
            assertTrue(runDeviceTestsAsUser(DEVICE_AND_PROFILE_OWNER_PKG, ".AccountUtilsTest",
                    "testAddAccount", 0));
            assertFalse(setDeviceOwner(DEVICE_AND_PROFILE_OWNER_ADMIN_COMPONENT));
        } finally {
            // make sure we clean up in case we succeeded in setting the device owner
            runDeviceTests(DEVICE_AND_PROFILE_OWNER_PKG, DEVICE_AND_PROFILE_OWNER_CLEAR);
            assertTrue(runDeviceTestsAsUser(DEVICE_AND_PROFILE_OWNER_PKG, ".AccountUtilsTest",
                    "testRemoveAccounts", 0));
        }
    }

    public void testSilentPackageInstall() throws Exception {
        if (!mHasFeature) {
            return;
        }
        final File apk = mCtsBuild.getTestApp(TEST_APP_APK);
        try {
            // Install the test and prepare the test apk.
            installApp(PACKAGE_INSTALLER_APK);
            assertTrue(setDeviceOwner(PACKAGE_INSTALLER_ADMIN_COMPONENT));

            getDevice().uninstallPackage(TEST_APP_PKG);
            assertTrue(getDevice().pushFile(apk, TEST_APP_LOCATION + apk.getName()));
            assertTrue(runDeviceTests(PACKAGE_INSTALLER_PKG,
                    PACKAGE_INSTALLER_PKG + ".SilentPackageInstallTest"));
        } finally {
            assertTrue("Failed to remove device owner.", runDeviceTests(PACKAGE_INSTALLER_PKG,
                    PACKAGE_INSTALLER_CLEAR_DEVICE_OWNER_TEST_CLASS));
            String command = "rm " + TEST_APP_LOCATION + apk.getName();
            String commandOutput = getDevice().executeShellCommand(command);
            getDevice().uninstallPackage(TEST_APP_PKG);
            getDevice().uninstallPackage(PACKAGE_INSTALLER_PKG);
        }
    }
}
