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

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.android.tv.settings.BrowseInfoFactory;
import com.android.tv.settings.MenuActivity;
import com.android.tv.settings.R;

/**
 * Activity allowing the management of apps settings.
 */
public class AppsActivity extends MenuActivity {

    // Used for storage only.
    public static final String EXTRA_VOLUME_UUID = "volumeUuid";
    public static final String EXTRA_VOLUME_NAME = "volumeName";

    private String mVolumeUuid;
    private String mVolumeName; // TODO: surface this to the user somewhere

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Bundle args = getIntent().getExtras();
        if (args != null && args.containsKey(EXTRA_VOLUME_UUID)) {
            mVolumeUuid = args.getString(EXTRA_VOLUME_UUID);
            mVolumeName = args.getString(EXTRA_VOLUME_NAME);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String getBrowseTitle() {
        return getString(R.string.device_apps);
    }
    
    @Override
    protected Drawable getBadgeImage() {
        return getDrawable(R.drawable.ic_settings_apps);
    }
    
    @Override
    protected BrowseInfoFactory getBrowseInfoFactory() {
        AppsBrowseInfo appsBrowseInfo = new AppsBrowseInfo(this, mVolumeUuid, mVolumeName);
        appsBrowseInfo.init();
        return appsBrowseInfo;
    }
}
