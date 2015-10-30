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
public class LauncherAppsSingleUserTest extends BaseLauncherAppsTest {

    private boolean mHasLauncherApps;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mHasLauncherApps = getDevice().getApiLevel() >= 21;

        if (mHasLauncherApps) {
            installTestApps();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mHasLauncherApps) {
            uninstallTestApps();
        }
        super.tearDown();
    }

    public void testInstallAppMainUser() throws Exception {
        if (!mHasLauncherApps) {
            return;
        }
        installApp(SIMPLE_APP_APK);
        try {
            int serialNumber = getUserSerialNumber(0);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS, "testSimpleAppInstalledForUser",
                            0, "-e testUser " + serialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLauncherCallbackPackageAddedMainUser() throws Exception {
        if (!mHasLauncherApps) {
            return;
        }
        startCallbackService();
        installApp(SIMPLE_APP_APK);
        try {
            int serialNumber = getUserSerialNumber(0);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS,
                            "testPackageAddedCallbackForUser",
                            0, "-e testUser " + serialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLauncherCallbackPackageRemovedMainUser() throws Exception {
        if (!mHasLauncherApps) {
            return;
        }
        installApp(SIMPLE_APP_APK);
        try {
            startCallbackService();
            int serialNumber = getUserSerialNumber(0);
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS,
                            "testPackageRemovedCallbackForUser",
                            0, "-e testUser " + serialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLauncherCallbackPackageChangedMainUser() throws Exception {
        if (!mHasLauncherApps) {
            return;
        }
        installApp(SIMPLE_APP_APK);
        try {
            startCallbackService();
            int serialNumber = getUserSerialNumber(0);
            installApp(SIMPLE_APP_APK);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS,
                            "testPackageChangedCallbackForUser",
                            0, "-e testUser " + serialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLauncherNonExportedAppFails() throws Exception {
        if (!mHasLauncherApps) {
            return;
        }
        installApp(SIMPLE_APP_APK);
        try {
            int serialNumber = getUserSerialNumber(0);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS, "testLaunchNonExportActivityFails",
                            0, "-e testUser " + serialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLaunchNonExportActivityFails() throws Exception {
        if (!mHasLauncherApps) {
            return;
        }
        installApp(SIMPLE_APP_APK);
        try {
            int serialNumber = getUserSerialNumber(0);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS, "testLaunchNonExportLauncherFails",
                            0, "-e testUser " + serialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }

    public void testLaunchMainActivity() throws Exception {
        if (!mHasLauncherApps) {
            return;
        }
        installApp(SIMPLE_APP_APK);
        try {
            int serialNumber = getUserSerialNumber(0);
            assertTrue(runDeviceTests(LAUNCHER_TESTS_PKG,
                    LAUNCHER_TESTS_CLASS, "testLaunchMainActivity",
                            0, "-e testUser " + serialNumber));
        } finally {
            getDevice().uninstallPackage(SIMPLE_APP_PKG);
        }
    }
}
