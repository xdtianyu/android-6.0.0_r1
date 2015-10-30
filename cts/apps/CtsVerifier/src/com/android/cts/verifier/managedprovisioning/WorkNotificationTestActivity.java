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

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.cts.verifier.R;

/**
 * Test activity used to generate a notification.
 */
public class WorkNotificationTestActivity extends Activity {
    public static final String ACTION_WORK_NOTIFICATION =
            "com.android.cts.verifier.managedprovisioning.WORK_NOTIFICATION";
    public static final String ACTION_WORK_NOTIFICATION_ON_LOCKSCREEN =
            "com.android.cts.verifier.managedprovisioning.LOCKSCREEN_NOTIFICATION";
    public static final String ACTION_CLEAR_WORK_NOTIFICATION =
            "com.android.cts.verifier.managedprovisioning.CLEAR_WORK_NOTIFICATION";
    private static final int NOTIFICATION_ID = 7;
    private NotificationManager mNotificationManager;

    private void showWorkNotification(int visibility) {
        final Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.provisioning_byod_work_notification_title))
                .setVisibility(visibility)
                .setAutoCancel(true)
                .build();
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String action = getIntent().getAction();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (ACTION_WORK_NOTIFICATION.equals(action)) {
            showWorkNotification(Notification.VISIBILITY_PUBLIC);
        } else if (ACTION_WORK_NOTIFICATION_ON_LOCKSCREEN.equals(action)) {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(
                    Context.DEVICE_POLICY_SERVICE);
            dpm.lockNow();
            showWorkNotification(Notification.VISIBILITY_PRIVATE);
        } else if (ACTION_CLEAR_WORK_NOTIFICATION.equals(action)) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
        finish();
    }
}
