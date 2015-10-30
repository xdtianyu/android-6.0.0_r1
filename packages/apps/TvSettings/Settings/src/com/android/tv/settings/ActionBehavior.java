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

package com.android.tv.settings;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;

import android.content.res.Resources;

/**
 * The different possible action behaviors.
 */
public enum ActionBehavior {
    INIT(),
    ON(R.string.settings_on),
    OFF(R.string.settings_off),
    OK(R.string.settings_ok),
    CANCEL(R.string.settings_cancel);

    private final int mTitleResource;

    private ActionBehavior() {
        this(0);
    }

    private ActionBehavior(int titleResource) {
        mTitleResource = titleResource;
    }

    public Action toAction(String key, Resources resources) {
        return new Action.Builder().key(key).title(resources.getString(mTitleResource)).build();
    }

    public Action toAction(String key, Resources resources, boolean selected) {
        return new Action.Builder()
                .key(key).title(resources.getString(mTitleResource)).checked(selected).build();
    }

    public Action.Builder toActionBuilder(String key, Resources resources) {
        return new Action.Builder()
                .key(key)
                .title(resources.getString(mTitleResource));
    }

    public static String getOnKey(String typeName) {
        return typeName + ":" + ActionBehavior.ON.name();
    }

    public static String getOffKey(String typeName) {
        return typeName + ":" + ActionBehavior.OFF.name();
    }
}
