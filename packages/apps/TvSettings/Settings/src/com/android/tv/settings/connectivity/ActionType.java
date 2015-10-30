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

package com.android.tv.settings.connectivity;

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;

import android.content.res.Resources;

enum ActionType {

    /*
     * Wifi settings.
     */
    CONECTIVITY_SETTINGS_MAIN(0),
    CONECTIVITY_SETTINGS_STATUS_INFO(R.string.wifi_action_status_info),
    CONECTIVITY_SETTINGS_ADVANCED_OPTIONS(R.string.wifi_action_advanced_options_title),
    CONECTIVITY_SETTINGS_FORGET_NETWORK(R.string.wifi_forget_network),

    /*
     * Status info.
     */
    CONECTIVITY_SETTINGS_CONNECTION(R.string.title_internet_connection),
    CONECTIVITY_SETTINGS_IP_ADDRESS(R.string.title_ip_address),
    CONECTIVITY_SETTINGS_MAC_ADDRESS(R.string.title_mac_address),
    CONECTIVITY_SETTINGS_SIGNAL_STRENGTH(R.string.title_signal_strength),

    /*
     * Advanced settings.
     */
    CONECTIVITY_SETTINGS_PROXY_SETTINGS(R.string.title_wifi_proxy_settings),
    CONECTIVITY_SETTINGS_IP_SETTINGS(R.string.title_wifi_ip_settings);

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

    String getDesc(Resources resources) {
        if (mDescResource != 0) {
            return resources.getString(mDescResource);
        }
        return null;
    }

    Action toAction(Resources resources) {
        return toAction(resources, getDesc(resources));
    }

    Action toAction(Resources resources, String description) {
        return new Action.Builder()
                .key(getKey(this, ActionBehavior.INIT))
                .title(getTitle(resources))
                .description(description)
                .build();
    }

    Action toInfo(Resources resources, String description) {
        return new Action.Builder()
                .key(getKey(this, ActionBehavior.INIT))
                .title(getTitle(resources))
                .description(description)
                .enabled(false)
                .build();
    }

    Action toInfo(Resources resources, int descResource) {
        return toInfo(resources, resources.getString(descResource));
    }

    Action toAction(Resources resources, int descResource) {
        return new Action.Builder()
                .key(getKey(this, ActionBehavior.INIT))
                .title(getTitle(resources))
                .description(resources.getString(descResource))
                .build();
    }

    private String getKey(ActionType t, ActionBehavior b) {
        return new ActionKey<ActionType, ActionBehavior>(t, b).getKey();
    }
}
