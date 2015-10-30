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

import com.android.tv.settings.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;

/**
 * Handles clearing an application's data.
 */
class DataClearer {

    interface Listener {
        void dataCleared(boolean succeeded);
    }

    private final Listener mListener;
    private final AppInfo mAppInfo;
    private boolean mClearingData;

    DataClearer(Listener listener, AppInfo appInfo) {
        mListener = listener;
        mAppInfo = appInfo;
        mClearingData = false;
    }

    void onActivityResult(int resultCode) {
        mClearingData = false;
        if (resultCode == Activity.RESULT_OK) {
            mListener.dataCleared(true);
        } else {
            mListener.dataCleared(false);
        }
    }

    void clearData(Activity activity, int requestId) {
        mClearingData = true;
        String spaceManagementActivityName = mAppInfo.getSpaceManagerActivityName();
        if (spaceManagementActivityName != null) {
            if (!ActivityManager.isUserAMonkey()) {
                Intent intent = new Intent(Intent.ACTION_DEFAULT);
                intent.setClassName(mAppInfo.getPackageName(), spaceManagementActivityName);
                activity.startActivityForResult(intent, requestId);
            }
        } else {
            ActivityManager am = (ActivityManager) activity.getSystemService(
                    Context.ACTIVITY_SERVICE);
            boolean res = am.clearApplicationUserData(
                    mAppInfo.getPackageName(), new IPackageDataObserver.Stub() {
                        public void onRemoveCompleted(
                                final String mPackageName, final boolean succeeded) {
                            mClearingData = false;
                            if (succeeded) {
                                mListener.dataCleared(true);
                            } else {
                                mListener.dataCleared(false);
                            }
                        }
                    });
            if (!res) {
                mClearingData = false;
                mListener.dataCleared(false);
            }
        }

        // Send out broadcast to clear corresponding app's canvas disk cache.
        Intent intent = new Intent();
        intent.setAction("com.google.android.canvas.data.ClusterDiskCache.CLEAR_CACHE_APP");
        intent.putExtra("packageName", mAppInfo.getPackageName());
        activity.sendBroadcast(intent);
    }

    String getDataSize(Context context) {
        return (mClearingData) ? context.getString(R.string.computing_size)
                : mAppInfo.getDataSize();
    }
}
