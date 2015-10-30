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

package com.android.tv.settings.device.display;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;

import com.android.tv.settings.BrowseInfoBase;
import com.android.tv.settings.MenuItem;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsConstant;

/**
 * Gets the list of browse headers and browse items.
 */
public class DisplayBrowseInfo extends BrowseInfoBase {

    private static final boolean WIFI_DISPLAY_SUPPORTED = false;
    private static final int HEADER_ID = 1;
    private final Context mContext;
    private final ArrayObjectAdapter mRow = new ArrayObjectAdapter();
    private int mNextItemId;

    DisplayBrowseInfo(Context context) {
        mContext = context;
        mNextItemId = 0;
        mHeaderItems.add(new HeaderItem(HEADER_ID, mContext.getString(R.string.device_display)));
        mRows.put(HEADER_ID, mRow);
    }

    void init() {
        mRow.clear();

        mRow.add(new MenuItem.Builder().id(mNextItemId++)
                .title(mContext.getString(R.string.device_daydream))
                .imageResourceId(mContext, R.drawable.ic_settings_daydream)
                .intent(getIntent(SettingsConstant.PACKAGE,
                        SettingsConstant.PACKAGE + ".device.display.daydream.DaydreamActivity"))
                .build());

        if (WIFI_DISPLAY_SUPPORTED) {
            mRow.add(new MenuItem.Builder().id(mNextItemId++)
                    .title(mContext.getString(R.string.accessories_wifi_display))
                    .imageResourceId(mContext, R.drawable.ic_settings_widi)
                    .intent(getIntent(SettingsConstant.PACKAGE,
                            SettingsConstant.PACKAGE + ".device.display.widi.WifiDisplayActivity"))
                    .build());
        }

        mRow.add(new MenuItem.Builder().id(mNextItemId++)
                .title(mContext.getString(R.string.device_calibration))
                .imageResourceId(mContext, R.drawable.ic_settings_overscan)
                .intent(new Intent("com.google.android.athome.overscan.CALIBRATE")).build());
    }

    private Intent getIntent(String targetPackage, String targetClass) {
        ComponentName componentName = new ComponentName(targetPackage, targetClass);
        Intent i = new Intent();
        i.setComponent(componentName);
        return i;
    }
}
