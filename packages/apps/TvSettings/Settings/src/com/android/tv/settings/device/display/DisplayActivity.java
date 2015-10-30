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

import android.graphics.drawable.Drawable;
import com.android.tv.settings.BrowseInfoFactory;
import com.android.tv.settings.R;
import com.android.tv.settings.MenuActivity;

/**
 * Activity allowing the management of display settings.
 */
public class DisplayActivity extends MenuActivity {

    @Override
    protected String getBrowseTitle() {
        return getString(R.string.device_display);
    }

    @Override
    protected Drawable getBadgeImage() {
        return getResources().getDrawable(R.drawable.ic_settings_display);
    }

    @Override
    protected BrowseInfoFactory getBrowseInfoFactory() {
        DisplayBrowseInfo displayBrowseInfo = new DisplayBrowseInfo(this);
        displayBrowseInfo.init();
        return displayBrowseInfo;
    }
}
