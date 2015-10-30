/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.verifier.managedprovisioning;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Profile owner receiver for BYOD flow test.
 * Setup cross-profile intent filter after successful provisioning.
 */
public class DeviceAdminTestReceiver extends DeviceAdminReceiver {
        private static final String TAG = "DeviceAdminTestReceiver";
        private static final String DEVICE_OWNER_PKG =
                "com.android.cts.verifier";
        private static final String ADMIN_RECEIVER_TEST_CLASS =
                DEVICE_OWNER_PKG + ".managedprovisioning.DeviceAdminTestReceiver";
        private static final ComponentName RECEIVER_COMPONENT_NAME = new ComponentName(
                DEVICE_OWNER_PKG, ADMIN_RECEIVER_TEST_CLASS);

        public static ComponentName getReceiverComponentName() {
            return RECEIVER_COMPONENT_NAME;
        }

        @Override
        public void onProfileProvisioningComplete(Context context, Intent intent) {
            Log.d(TAG, "Provisioning complete intent received");
            setupProfile(context);
        }

        private void setupProfile(Context context) {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.setProfileEnabled(new ComponentName(context.getApplicationContext(), getClass()));

            // Setup cross-profile intent filter to allow communications between the two versions of CtsVerifier
            // Primary -> work direction
            IntentFilter filter = new IntentFilter();
            filter.addAction(ByodHelperActivity.ACTION_QUERY_PROFILE_OWNER);
            filter.addAction(ByodHelperActivity.ACTION_REMOVE_PROFILE_OWNER);
            filter.addAction(ByodHelperActivity.ACTION_INSTALL_APK);
            filter.addAction(ByodHelperActivity.ACTION_CHECK_INTENT_FILTERS);
            filter.addAction(ByodHelperActivity.ACTION_CAPTURE_AND_CHECK_IMAGE);
            filter.addAction(ByodHelperActivity.ACTION_CAPTURE_AND_CHECK_VIDEO);
            filter.addAction(ByodHelperActivity.ACTION_CAPTURE_AND_CHECK_AUDIO);
            filter.addAction(ByodHelperActivity.ACTION_KEYGUARD_DISABLED_FEATURES);
            filter.addAction(ByodHelperActivity.ACTION_LOCKNOW);
            filter.addAction(ByodHelperActivity.ACTION_TEST_NFC_BEAM);
            filter.addAction(ByodHelperActivity.ACTION_TEST_CROSS_PROFILE_INTENTS_DIALOG);
            filter.addAction(ByodHelperActivity.ACTION_TEST_APP_LINKING_DIALOG);
            filter.addAction(CrossProfileTestActivity.ACTION_CROSS_PROFILE_TO_WORK);
            filter.addAction(WorkNotificationTestActivity.ACTION_WORK_NOTIFICATION);
            filter.addAction(WorkNotificationTestActivity.ACTION_WORK_NOTIFICATION_ON_LOCKSCREEN);
            filter.addAction(WorkNotificationTestActivity.ACTION_CLEAR_WORK_NOTIFICATION);
            filter.addAction(WorkStatusTestActivity.ACTION_WORK_STATUS_TOAST);
            filter.addAction(WorkStatusTestActivity.ACTION_WORK_STATUS_ICON);
            filter.addAction(
                    PermissionLockdownTestActivity.ACTION_MANAGED_PROFILE_CHECK_PERMISSION_LOCKDOWN);
            dpm.addCrossProfileIntentFilter(getWho(context), filter,
                    DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT);

            // Work -> primary direction
            filter = new IntentFilter();
            filter.addAction(ByodHelperActivity.ACTION_PROFILE_OWNER_STATUS);
            filter.addAction(CrossProfileTestActivity.ACTION_CROSS_PROFILE_TO_PERSONAL);
            dpm.addCrossProfileIntentFilter(getWho(context), filter,
                    DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED);

            Intent intent = new Intent(context, ByodHelperActivity.class);
            intent.setAction(ByodHelperActivity.ACTION_PROFILE_PROVISIONED);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
}
