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
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.BrowseInfoBase;
import com.android.tv.settings.MenuItem;
import com.android.tv.settings.R;
import com.android.tv.settings.util.UriUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Gets the list of browse headers and browse items.
 */
public class AppsBrowseInfo extends BrowseInfoBase {

    private static final String TAG = "AppsBrowseInfo";

    private static final int DOWNLOADED_ID = 0;
    private static final int SYSTEM_ID = 1;
    private static final int RUNNING_ID = 2;
    private static final int PERMISSIONS_ID = 3;

    private final Context mContext;
    private final HashMap<String, Integer> mAppItemIdList;
    private final ApplicationsState mApplicationsState;
    private final ApplicationsState.Session mSessionSystem;
    private final ApplicationsState.AppFilter mFilterSystem;
    private final ApplicationsState.Session mSessionDownloaded;
    private final ApplicationsState.AppFilter mFilterDownloaded;
    private final ApplicationsState.Session mSessionRunning;
    private final ApplicationsState.AppFilter mFilterRunning;
    private int mNextItemId;

    private final String mVolumeUuid;

    private final Handler mHandler = new Handler();
    private final Map<ArrayObjectAdapter,
        ArrayList<ApplicationsState.AppEntry>> mUpdateMap = new ArrayMap<>(3);
    private long mRunAt = Long.MIN_VALUE;
    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            for (final ArrayObjectAdapter adapter : mUpdateMap.keySet()) {
                final ArrayList<ApplicationsState.AppEntry> entries = mUpdateMap.get(adapter);
                updateAppListInternal(adapter, entries);
            }
            mUpdateMap.clear();
            mRunAt = 0;
        }
    };

    AppsBrowseInfo(Activity context, String volumeUuid, String volumeDescription) {
        mContext = context;
        mVolumeUuid = volumeUuid;
        mAppItemIdList = new HashMap<>();
        mApplicationsState = ApplicationsState.getInstance(context.getApplication());
        mRows.put(SYSTEM_ID, new ArrayObjectAdapter());
        mRows.put(DOWNLOADED_ID, new ArrayObjectAdapter());
        mRows.put(RUNNING_ID, new ArrayObjectAdapter());

        // The UUID of internal storage is null, so we check if there's a volume name to see if we
        // should only be showing the apps on the internal storage or all apps.
        if (!TextUtils.isEmpty(volumeUuid) || !TextUtils.isEmpty(volumeDescription)) {
            ApplicationsState.AppFilter volumeFilter =
                    new ApplicationsState.VolumeFilter(volumeUuid);

            mFilterSystem =
                    new ApplicationsState.CompoundFilter(FILTER_SYSTEM, volumeFilter);
            mFilterDownloaded =
                    new ApplicationsState.CompoundFilter(FILTER_DOWNLOADED, volumeFilter);
            mFilterRunning =
                    new ApplicationsState.CompoundFilter(FILTER_RUNNING, volumeFilter);
        } else {
            mFilterSystem = FILTER_SYSTEM;
            mFilterDownloaded = FILTER_DOWNLOADED;
            mFilterRunning = FILTER_RUNNING;
        }

        mSessionSystem = mApplicationsState.newSession(new RowUpdateCallbacks() {
            @Override
            protected void doRebuild() {
                rebuildSystem();
            }

            @Override
            public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                updateAppList(mRows.get(SYSTEM_ID), apps);
            }
        });
        rebuildSystem();

        mSessionDownloaded = mApplicationsState.newSession(new RowUpdateCallbacks() {
            @Override
            protected void doRebuild() {
                rebuildDownloaded();
            }

            @Override
            public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                updateAppList(mRows.get(DOWNLOADED_ID), apps);
            }
        });
        rebuildDownloaded();

        mSessionRunning = mApplicationsState.newSession(new RowUpdateCallbacks() {
            @Override
            protected void doRebuild() {
                rebuildRunning();
            }

            @Override
            public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                updateAppList(mRows.get(RUNNING_ID), apps);
            }
        });
        rebuildRunning();

        mNextItemId = 0;
        final ArrayObjectAdapter permissionsAdapter = new ArrayObjectAdapter();
        // TODO: different icon
        final MenuItem permissionsItem = new MenuItem.Builder()
                .title(mContext.getString(R.string.device_apps_permissions))
                .imageResourceId(mContext, R.drawable.ic_settings_security)
                .intent(new Intent(Intent.ACTION_MANAGE_PERMISSIONS))
                .build();
        permissionsAdapter.add(permissionsItem);
        mRows.put(PERMISSIONS_ID, permissionsAdapter);
    }

    private void rebuildSystem() {
        ArrayList<ApplicationsState.AppEntry> apps =
            mSessionSystem.rebuild(mFilterSystem, ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            updateAppList(mRows.get(SYSTEM_ID), apps);
        }
    }

    private void rebuildDownloaded() {
        ArrayList<ApplicationsState.AppEntry> apps =
                mSessionDownloaded.rebuild(mFilterDownloaded, ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            updateAppList(mRows.get(DOWNLOADED_ID), apps);
        }
    }

    private void rebuildRunning() {
        ArrayList<ApplicationsState.AppEntry> apps =
                mSessionRunning.rebuild(mFilterRunning, ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            updateAppList(mRows.get(RUNNING_ID), apps);
        }
    }

    private void updateAppList(ArrayObjectAdapter row,
            ArrayList<ApplicationsState.AppEntry> entries) {
        mUpdateMap.put(row, entries);

        // We can get spammed with updates, so coalesce them to reduce jank and flicker
        if (mRunAt == Long.MIN_VALUE) {
            // First run, no delay
            mHandler.removeCallbacks(mUpdateRunnable);
            mHandler.post(mUpdateRunnable);
        } else {
            if (mRunAt == 0) {
                mRunAt = SystemClock.uptimeMillis() + 1000;
            }
            int delay = (int) (mRunAt - SystemClock.uptimeMillis());
            delay = delay < 0 ? 0 : delay;

            mHandler.removeCallbacks(mUpdateRunnable);
            mHandler.postDelayed(mUpdateRunnable, delay);
        }
    }

    private void updateAppListInternal(ArrayObjectAdapter row,
            ArrayList<ApplicationsState.AppEntry> entries) {
        if (entries != null) {
            row.clear();

            for (final ApplicationsState.AppEntry entry : entries) {
                row.add(getMenuItemForApp(entry));
            }
        }
    }

    private MenuItem getMenuItemForApp(ApplicationsState.AppEntry entry) {
        final AppInfo info = new AppInfo(mContext, entry);
        final String packageName = info.getPackageName();
        Integer itemId = mAppItemIdList.get(packageName);
        if (itemId == null) {
            itemId = mNextItemId++;
            mAppItemIdList.put(packageName, itemId);
        }

        return new MenuItem.Builder()
                .id(itemId)
                .title(info.getName())
                .description(info.getSize())
                .imageUri(getAppIconUri(mContext, info))
                .intent(AppManagementActivity.getLaunchIntent(info.getPackageName()))
                .build();
    }

    private void loadBrowseHeaders() {
        mHeaderItems.add(new HeaderItem(DOWNLOADED_ID,
                mContext.getString(R.string.apps_downloaded)));
        // Only show these rows if we're not browsing adopted storage
        if (TextUtils.isEmpty(mVolumeUuid)) {
            mHeaderItems.add(new HeaderItem(SYSTEM_ID, mContext.getString(R.string.apps_system)));
            mHeaderItems.add(new HeaderItem(RUNNING_ID, mContext.getString(R.string.apps_running)));
            mHeaderItems.add(new HeaderItem(PERMISSIONS_ID,
                    mContext.getString(R.string.apps_permissions)));
        }
    }

    void init() {
        mSessionSystem.resume();
        mSessionDownloaded.resume();
        mSessionRunning.resume();
        loadBrowseHeaders();
    }

    static String getAppIconUri(Context context, AppInfo info) {
        int iconRes = info.getIconResource();
        String iconUri = null;
        if (iconRes != 0) {
            try {
                Resources resources = context.getPackageManager()
                        .getResourcesForApplication(info.getApplicationInfo());
                ShortcutIconResource iconResource = new ShortcutIconResource();
                iconResource.packageName = info.getPackageName();
                iconResource.resourceName = resources.getResourceName(iconRes);
                iconUri = UriUtils.getShortcutIconResourceUri(iconResource).toString();
            } catch (Exception e1) {
                Log.w("AppsBrowseInfo", e1.toString());
            }
        } else {
            iconUri = UriUtils.getAndroidResourceUri(Resources.getSystem(),
                    com.android.internal.R.drawable.sym_def_app_icon);
        }

        if (iconUri == null) {
            iconUri = UriUtils.getAndroidResourceUri(context.getResources(),
                    com.android.internal.R.drawable.sym_app_on_sd_unavailable_icon);
        }
        return iconUri;
    }

    private abstract class RowUpdateCallbacks implements ApplicationsState.Callbacks {

        protected abstract void doRebuild();

        @Override
        public void onRunningStateChanged(boolean running) {
            doRebuild();
        }

        @Override
        public void onPackageListChanged() {
            doRebuild();
        }

        @Override
        public void onPackageIconChanged() {
            doRebuild();
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            doRebuild();
        }

        @Override
        public void onAllSizesComputed() {
            doRebuild();
        }

        @Override
        public void onLauncherInfoChanged() {
            doRebuild();
        }

        @Override
        public void onLoadEntriesCompleted() {
            doRebuild();
        }
    }

    private static final ApplicationsState.AppFilter FILTER_SYSTEM =
            new ApplicationsState.AppFilter() {

                @Override
                public void init() {}

                @Override
                public boolean filterApp(ApplicationsState.AppEntry info) {
                    return (info.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                }
            };

    private static final ApplicationsState.AppFilter FILTER_DOWNLOADED =
            new ApplicationsState.AppFilter() {

                @Override
                public void init() {}

                @Override
                public boolean filterApp(ApplicationsState.AppEntry info) {
                    return (info.info.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
                }
            };

    private static final ApplicationsState.AppFilter FILTER_RUNNING =
            new ApplicationsState.AppFilter() {

                @Override
                public void init() {}

                @Override
                public boolean filterApp(ApplicationsState.AppEntry info) {
                    return (info.info.flags & ApplicationInfo.FLAG_STOPPED) == 0;
                }
            };
}
