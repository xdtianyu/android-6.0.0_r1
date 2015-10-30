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

package com.android.tv.settings.device.apps;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;

import com.android.settingslib.applications.ApplicationsState;

/**
 * Handles force stopping an application.
 */
class ForceStopManager {

    private final Context mContext;
    private final AppInfo mAppInfo;
    private boolean mShowForceStop;

    ForceStopManager(Context context, AppInfo appInfo) {
        mContext = context;
        mAppInfo = appInfo;
        mShowForceStop = false;
    }

    boolean canForceStop() {
        checkForceStop();
        return mShowForceStop;
    }

    void forceStop(ApplicationsState state) {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        am.forceStopPackage(mAppInfo.getPackageName());
        final int userId = UserHandle.getUserId(mAppInfo.getUid());
        state.invalidatePackage(mAppInfo.getPackageName(), userId);
        ApplicationsState.AppEntry newEnt = state.getEntry(mAppInfo.getPackageName(), userId);
        if (newEnt != null) {
            mAppInfo.setEntry(newEnt);
        }
    }

    private void checkForceStop() {
        DevicePolicyManager dpm = (DevicePolicyManager) mContext.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm.packageHasActiveAdmins(mAppInfo.getPackageName())) {
            // User can't force stop device admin.
            mShowForceStop = false;
        } else if (!mAppInfo.isStopped()) {
            // If the app isn't explicitly stopped, then always show the
            // force stop action.
            mShowForceStop = true;
        } else {
            Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                    Uri.fromParts("package", mAppInfo.getPackageName(), null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[] {
            mAppInfo.getPackageName() });
            intent.putExtra(Intent.EXTRA_UID, mAppInfo.getUid());
            intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mAppInfo.getUid()));
            mContext.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mShowForceStop = (getResultCode() != Activity.RESULT_CANCELED);
                }
            }, null, Activity.RESULT_CANCELED, null, null);
        }
    }
}
