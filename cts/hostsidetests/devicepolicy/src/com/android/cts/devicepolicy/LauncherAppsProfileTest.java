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

import com.android.ddmlib.Log.LogLevel;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;

/**
 * Set of tests for LauncherApps with managed profiles.
 */
public class LauncherAppsProfileTest extends BaseLauncherAppsTest {

    private static final String MANAGED_PROFILE_PKG = "com.android.cts.managedprofile";
    private static final String MANAGED_PROFILE_APK = "CtsManagedProfileApp.apk";
    private static final String ADMIN_RECEIVER_TEST_CLASS =
            MANAGED_PROFILE_PKG + ".BaseManagedProfileTest$BasicAdminReceiver";

    private int mProfileUserId;
    private int mProfileSerialNumber;
    private int mMainUserSerialNumber;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mHasFeature = mHasFeature && hasDeviceFeature("android.software.managed_users");
        if (mHasFeature) {
            removeTestUsers();
            installTestApps();
            // Create a managed profile
            mProfileUserId = createManagedProfile();
            installApp(MANAGED_PROFILE_APK);
            setProfileOwnerOrFail(MANAGED_PROFILE_PKG + "/" + ADMIN_RECEIVER_TEST_CLASS,
                    mProfileUserId);
            mProfileSerialNumber = getUserSerialNumber(mProfileUserId);
            mMainUserSerialNumber = getUserSerialNumber(0);
            startUser(mProfileUserId);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mHasFeature) {
            removeUser(mProfileUserId);
            uninstallTestApps();
            getDevice().uninstallPackage(MANAGED_PROFILE_PKG);
        }
        super.tearDown();
    }

    public void testGetActivitiesWithProfile() throws Exception {
        if (!mHasFeature) {
            return;
        }
        // Install app for all users.
        installApp(SIMPLE_APP_APK);
        try {
            // Run tests to check SimpleApp exists in both profile and main user.
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS,
                    "testSimpleAppInstalledForUser",
                            0, "-e testUser " + mProfileSerialNumber));
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_PKG + ".LauncherAppsTests", "testSimpleAppInstalledForUser",
                            0, "-e testUser " + mMainUserSerialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLauncherCallbackPackageAddedProfile() throws Exception {
        if (!mHasFeature) {
            return;
        }
        startCallbackService();
        installApp(SIMPLE_APP_APK);
        try {
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS,
                            "testPackageAddedCallbackForUser",
                            0, "-e testUser " + mProfileSerialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLauncherCallbackPackageRemovedProfile() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installApp(SIMPLE_APP_APK);
        try {
            startCallbackService();
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS,
                            "testPackageRemovedCallbackForUser",
                            0, "-e testUser " + mProfileSerialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLauncherCallbackPackageChangedProfile() throws Exception {
        if (!mHasFeature) {
            return;
        }
        installApp(SIMPLE_APP_APK);
        try {
            startCallbackService();
            installApp(SIMPLE_APP_APK);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS,
                            "testPackageChangedCallbackForUser",
                            0, "-e testUser " + mProfileSerialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }
}
