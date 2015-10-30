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

import junit.framework.AssertionFailedError;

/**
 * Set of tests for profile owner use cases that also apply to device owners.
 * Tests that should be run identically in both cases are added in DeviceAndProfileOwnerTest.
 */
public class MixedProfileOwnerTest extends DeviceAndProfileOwnerTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // We need managed users to be supported in order to create a profile of the user owner.
        mHasFeature &= hasDeviceFeature("android.software.managed_users");

        if (mHasFeature) {
            removeTestUsers();
            mUserId = createManagedProfile();

            installAppAsUser(DEVICE_ADMIN_APK, mUserId);
            setProfileOwnerOrFail(DEVICE_ADMIN_PKG + "/" + ADMIN_RECEIVER_TEST_CLASS, mUserId);
            startUser(mUserId);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mHasFeature) {
            removeUser(mUserId);
        }
        super.tearDown();
    }

    // Most tests for this class are defined in DeviceAndProfileOwnerTest

    /**
     * Verify that screenshots are still possible for activities in the primary user when the policy
     * is set on the profile owner.
     */
    public void testScreenCaptureDisabled_allowedPrimaryUser() throws Exception {
        if (!mHasFeature) {
            return;
        }
        executeDeviceTestMethod(".ScreenCaptureDisabledTest", "testSetScreenCaptureDisabled_true");
        // start the ScreenCaptureDisabledActivity in the primary user
        installAppAsUser(DEVICE_ADMIN_APK, USER_OWNER);
        String command = "am start -W --user 0 " + DEVICE_ADMIN_PKG + "/"
                + DEVICE_ADMIN_PKG + ".ScreenCaptureDisabledActivity";
        getDevice().executeShellCommand(command);
        executeDeviceTestMethod(".ScreenCaptureDisabledTest", "testScreenCapturePossible");
    }
}
