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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.IPackageDataObserver;

/**
 * Handles clearing an application's cache.
 */
class CacheClearer {

    interface Listener {
        void cacheCleared(boolean succeeded);
    }

    private final Listener mListener;
    private final AppInfo mAppInfo;
    private boolean mClearingCache;

    CacheClearer(Listener listener, AppInfo appInfo) {
        mListener = listener;
        mAppInfo = appInfo;
        mClearingCache = false;
    }

    void clearCache(Context context) {
        PackageManager packageManager = context.getPackageManager();

        mClearingCache = true;
        packageManager.deleteApplicationCacheFiles(mAppInfo.getPackageName(),
                new IPackageDataObserver.Stub() {
                    public void onRemoveCompleted(final String packageName,
                            final boolean succeeded) {
                        mClearingCache = false;
                        mListener.cacheCleared(succeeded);
                    }
                });

        // Send out broadcast to clear canvas disk cache.
        Intent intent = new Intent();
        intent.setAction("com.google.android.canvas.data.ClusterDiskCache.CLEAR_CACHE_APP");
        intent.putExtra("packageName", mAppInfo.getPackageName());
        context.sendBroadcast(intent);
    }

    String getCacheSize(Context context) {
        return (mClearingCache) ? context.getString(R.string.computing_size)
                : mAppInfo.getCacheSize();
    }
}
