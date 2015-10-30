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

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;

import android.content.res.Resources;

enum ActionType {
    /*
     * Keyboard
     */
    KEYBOARD_OVERVIEW(R.string.system_keyboard),
    KEYBOARD_OVERVIEW_CURRENT_KEYBOARD(R.string.title_current_keyboard),
    KEYBOARD_OVERVIEW_CONFIGURE(R.string.title_configure, R.string.desc_configure_keyboard),

    /*
     * Security
     */
    SECURITY_OVERVIEW(R.string.system_security),
    SECURITY_UNKNOWN_SOURCES(R.string.security_unknown_sources_title,
            R.string.security_unknown_sources_desc),
    SECURITY_UNKNOWN_SOURCES_CONFIRM(R.string.security_unknown_sources_title,
                    R.string.security_unknown_sources_confirm_desc),
    SECURITY_VERIFY_APPS(R.string.security_verify_apps_title,
             R.string.security_verify_apps_desc),

    CAPTIONS_OVERVIEW(R.string.accessibility_captions,
                      R.string.accessibility_captions_description),
    CAPTIONS_DISPLAY(R.string.captions_display),
    CAPTIONS_CONFIGURE(R.string.captions_configure),
    CAPTIONS_LANGUAGE(R.string.captions_lanaguage),
    CAPTIONS_TEXTSIZE(R.string.captions_textsize),
    CAPTIONS_CAPTIONSTYLE(R.string.captions_captionstyle),
    CAPTIONS_CUSTOMOPTIONS(R.string.captions_customoptions),
    CAPTIONS_FONTFAMILY(R.string.captions_fontfamily),
    CAPTIONS_TEXTCOLOR(R.string.captions_textcolor),
    CAPTIONS_TEXTOPACITY(R.string.captions_textopacity),
    CAPTIONS_EDGETYPE(R.string.captions_edgetype),
    CAPTIONS_EDGECOLOR(R.string.captions_edgecolor),
    CAPTIONS_BACKGROUNDCOLOR(R.string.captions_backgroundcolor),
    CAPTIONS_BACKGROUNDOPACITY(R.string.captions_backgroundopacity),
    CAPTIONS_WINDOWCOLOR(R.string.captions_windowcolor),
    CAPTIONS_WINDOWOPACITY(R.string.captions_windowopacity);

    private final int mTitleResource;
    private final int mDescResource;

    private ActionType(int titleResource) {
        mTitleResource = titleResource;
        mDescResource = 0;
    }

    private ActionType(int titleResource, int descResource) {
        mTitleResource = titleResource;
        mDescResource = descResource;
    }
    String getTitle(Resources resources) {
        return resources.getString(mTitleResource);
    }

    String getDescription(Resources resources) {
        if (mDescResource != 0) {
            return resources.getString(mDescResource);
        }
        return null;
    }

    Action toAction(Resources resources) {
        return toAction(resources, true/*enabled*/);
    }

    Action toAction(Resources resources, boolean enabled) {
        return toAction(resources, getDescription(resources), enabled, false/* not checked */);
    }

    Action toAction(Resources resources, String description) {
        return toAction(resources, description, true/*enabled*/, false /* not checked */);
    }

    Action toAction(Resources resources, String description, boolean enabled) {
        return toAction(resources, description, enabled, false /* not checked */);
    }

    Action toAction(Resources resources, String description, boolean enabled, boolean checked) {
        return new Action.Builder()
                .key(getKey(this, ActionBehavior.INIT))
                .title(getTitle(resources))
                .description(description)
                .enabled(enabled)
                .checked(checked)
                .build();
    }

    private String getKey(ActionType t, ActionBehavior b) {
        return new ActionKey<>(t, b).getKey();
    }
}
