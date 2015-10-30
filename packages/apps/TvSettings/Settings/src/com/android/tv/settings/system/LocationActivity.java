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

package com.android.tv.settings.system;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;

import com.android.tv.settings.R;
import com.android.tv.settings.device.apps.AppManagementActivity;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.util.List;

/**
 * Controls location settings.
 */
public class LocationActivity extends SettingsLayoutActivity {

    private static final String TAG = "LocationActivity";
    private static final boolean DEBUG = false;

    private static final int ACTION_LOCATION_ON = 1;
    private static final int ACTION_LOCATION_OFF = 2;

    private static final int RECENT_TIME_INTERVAL_MILLIS = 15 * 60 * 1000;

    private final Layout.LayoutGetter mRecentRequestsLayoutGetter =
            new RecentRequestsLayoutGetter();

    @Override
    public Layout createLayout() {
        final Resources res = getResources();
        return new Layout().breadcrumb(getString(R.string.header_category_personal))
                .add(new Layout.Header.Builder(res)
                        .title(R.string.system_location)
                        .icon(R.drawable.ic_settings_location)
                        .detailedDescription(R.string.system_desc_location)
                        .build()
                        .add(new Layout.Header.Builder(res)
                                .title(R.string.location_status)
                                .build()
                                .setSelectionGroup(new Layout.SelectionGroup.Builder(2)
                                        .add(getString(R.string.location_mode_wifi_description),
                                                null, ACTION_LOCATION_ON)
                                        .add(getString(R.string.off), null, ACTION_LOCATION_OFF)
                                        .select(isLocationEnabled() ?
                                                ACTION_LOCATION_ON : ACTION_LOCATION_OFF)
                                        .build()))
                        .add(mRecentRequestsLayoutGetter));
    }

    private class RecentRequestsLayoutGetter extends Layout.LayoutGetter {

        @Override
        public Layout get() {
            if (isLocationEnabled()) {
                return new Layout()
                        .add(getRecentRequestHeader());
            } else {
                return new Layout();
            }
        }
    }

    @Override
    public void onActionClicked(Layout.Action action) {
        switch (action.getId()) {
            case ACTION_LOCATION_ON:
                setLocationMode(true);
                break;
            case ACTION_LOCATION_OFF:
                setLocationMode(false);
                break;
            default:
                final Intent intent = action.getIntent();
                if (intent != null) {
                    startActivity(intent);
                }
        }
    }

    /**
     * Fills a list of applications which queried location recently within
     * specified time. TODO: add icons
     */
    private Layout.Header getRecentRequestHeader() {
        final Layout.Header header = new Layout.Header.Builder(getResources())
                .title(R.string.location_category_recent_location_requests)
                .build();

        // Retrieve a location usage list from AppOps
        AppOpsManager aoManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> appOps = aoManager.getPackagesForOps(
                new int[] {
                        AppOpsManager.OP_MONITOR_LOCATION,
                        AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION,
                });
        long now = System.currentTimeMillis();
        for (AppOpsManager.PackageOps ops : appOps) {
            Layout.Action action = getActionFromOps(now, ops);
            if (action != null) {
                header.add(action);
            }
        }

        return header;
    }

    private Layout.Action getActionFromOps(long now, AppOpsManager.PackageOps ops) {
        String packageName = ops.getPackageName();
        List<AppOpsManager.OpEntry> entries = ops.getOps();
        boolean highBattery = false;
        boolean normalBattery = false;

        // Earliest time for a location request to end and still be shown in
        // list.
        long recentLocationCutoffTime = now - RECENT_TIME_INTERVAL_MILLIS;
        for (AppOpsManager.OpEntry entry : entries) {
            if (entry.isRunning() || entry.getTime() >= recentLocationCutoffTime) {
                switch (entry.getOp()) {
                    case AppOpsManager.OP_MONITOR_LOCATION:
                        normalBattery = true;
                        break;
                    case AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION:
                        highBattery = true;
                        break;
                    default:
                        break;
                }
            }
        }

        if (!highBattery && !normalBattery) {
            if (DEBUG) {
                Log.v(TAG, packageName + " hadn't used location within the time interval.");
            }
            return null;
        }

        Layout.Action.Builder builder = new Layout.Action.Builder(getResources(),
                AppManagementActivity.getLaunchIntent(packageName));
        // The package is fresh enough, continue.
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            if (appInfo.uid == ops.getUid()) {
                builder.title(getPackageManager().getApplicationLabel(appInfo).toString())
                        .description(highBattery ? getString(R.string.location_high_battery_use)
                                : getString(R.string.location_low_battery_use));
            } else if (DEBUG) {
                Log.v(TAG, "package " + packageName + " with Uid " + ops.getUid() +
                        " belongs to another inactive account, ignored.");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Package not found: " + packageName);
        }
        return builder.build();
    }

    private boolean isLocationEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) !=
                Settings.Secure.LOCATION_MODE_OFF;
    }

    private void setLocationMode(boolean enable) {
        if (enable) {
            // TODO
            // com.google.android.gms/com.google.android.location.network.ConfirmAlertActivity
            // pops up when we turn this on.
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        } else {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
        }
    }
}
