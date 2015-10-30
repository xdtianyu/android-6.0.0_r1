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
import android.content.pm.ApplicationInfo;
import android.text.format.Formatter;

import com.android.settingslib.applications.ApplicationsState;

/**
 * Contains all the info necessary to manage an application.
 */
public class AppInfo {

    private final Object mLock = new Object();
    private final Context mContext;
    private ApplicationsState.AppEntry mEntry;

    public AppInfo(Context context, ApplicationsState.AppEntry entry) {
        mContext = context;
        mEntry = entry;
    }

    public void setEntry(ApplicationsState.AppEntry entry) {
        synchronized (mLock) {
            mEntry = entry;
        }
    }

    public String getName() {
        synchronized (mLock) {
            mEntry.ensureLabel(mContext);
            return mEntry.label;
        }
    }

    public String getSize() {
        synchronized (mLock) {
            return mEntry.sizeStr;
        }
    }

    public int getIconResource() {
        synchronized (mLock) {
            return mEntry.info.icon;
        }
    }

    public String getPackageName() {
        synchronized (mLock) {
            return mEntry.info.packageName;
        }
    }

    public ApplicationInfo getApplicationInfo() {
        synchronized (mLock) {
            return mEntry.info;
        }
    }

    public boolean isStopped() {
        synchronized (mLock) {
            return (mEntry.info.flags & ApplicationInfo.FLAG_STOPPED) != 0;
        }
    }

    public boolean isInstalled() {
        synchronized (mLock) {
            return (mEntry.info.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
        }
    }

    public boolean isUpdatedSystemApp() {
        synchronized (mLock) {
            return (mEntry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        }
    }

    public boolean isEnabled() {
        synchronized (mLock) {
            return mEntry.info.enabled;
        }
    }

    public boolean isSystemApp() {
        synchronized (mLock) {
            return (mEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }
    }

    public String getCacheSize() {
        synchronized (mLock) {
            return Formatter.formatFileSize(mContext, mEntry.cacheSize + mEntry.externalCacheSize);
        }
    }

    public String getDataSize() {
        synchronized (mLock) {
            return Formatter.formatFileSize(mContext, mEntry.dataSize + mEntry.externalDataSize);
        }
    }

    public String getSpaceManagerActivityName() {
        synchronized (mLock) {
            return mEntry.info.manageSpaceActivityName;
        }
    }

    public int getUid() {
        synchronized (mLock) {
            return mEntry.info.uid;
        }
    }

    public String getVersion() {
        synchronized (mLock) {
            return mEntry.getVersion(mContext);
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
