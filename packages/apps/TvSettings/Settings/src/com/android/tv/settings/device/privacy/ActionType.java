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

package com.android.tv.settings.device.privacy;

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;

import android.content.res.Resources;

enum ActionType {
    BACKUP_DATA(R.string.privacy_backup_data),
    BACKUP_ACCOUNT(R.string.privacy_backup_account),
    AUTOMATIC_RESTORE(R.string.privacy_automatic_restore),
    FACTORY_RESET(R.string.device_reset),
    FACTORY_RESET_CONFIRM(R.string.confirm_factory_reset_device);


    private final int mTitleResource;

    private ActionType(int titleResource) {
        mTitleResource = titleResource;
    }

    String getTitle(Resources resources) {
        return resources.getString(mTitleResource);
    }

    Action toAction(Resources resources) {
        return toAction(resources, null);
    }

    Action toAction(Resources resources, String description) {
        return new Action.Builder()
                .key(getKey(this, ActionBehavior.INIT))
                .title(getTitle(resources))
                .description(description)
                .build();
    }

    private String getKey(ActionType t, ActionBehavior b) {
        return new ActionKey<ActionType, ActionBehavior>(t, b).getKey();
    }
}
