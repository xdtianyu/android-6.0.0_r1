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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Handles opening an application.
 */
class OpenManager {

    private final Context mContext;
    private final AppInfo mAppInfo;
    private Intent mLaunchIntent;

    OpenManager(Context context, AppInfo appInfo) {
        mContext = context;
        mAppInfo = appInfo;
    }

    public boolean canOpen() {
        return getLaunchIntent() != null;
    }

    public Intent getLaunchIntent() {
        if (mLaunchIntent != null) {
            return mLaunchIntent;
        }
        PackageManager pm = mContext.getPackageManager();
        mLaunchIntent = pm.getLeanbackLaunchIntentForPackage(mAppInfo.getPackageName());
        if (mLaunchIntent == null) {
            mLaunchIntent = pm.getLaunchIntentForPackage(mAppInfo.getPackageName());
        }
        return mLaunchIntent;
    }
}
