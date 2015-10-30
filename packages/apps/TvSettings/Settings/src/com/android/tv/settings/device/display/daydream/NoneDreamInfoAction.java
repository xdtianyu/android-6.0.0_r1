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

package com.android.tv.settings.device.display.daydream;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;


public class NoneDreamInfoAction extends DreamInfoAction {

    private static final String TAG = "DreamInfoAction";

    NoneDreamInfoAction(String title, boolean dreamsEnabled) {
        super(null, !dreamsEnabled, title, null, null);
    }

    NoneDreamInfoAction(NoneDreamInfoAction noneDreamInfoAction, boolean dreamsEnabled) {
        this(noneDreamInfoAction.getTitle(), dreamsEnabled);
    }

    @Override
    public Drawable getIndicator(Context context) {
        return null;
    }

    @SuppressWarnings("hiding")
    public static Parcelable.Creator<NoneDreamInfoAction> CREATOR =
            new Parcelable.Creator<NoneDreamInfoAction>() {

                @Override
                public NoneDreamInfoAction createFromParcel(Parcel source) {
                    return new NoneDreamInfoAction(source.readString(),
                            source.readInt() == 1);
                }

                @Override
                public NoneDreamInfoAction[] newArray(int size) {
                    return new NoneDreamInfoAction[size];
                }
            };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getTitle());
        dest.writeInt(isChecked() ? 1 : 0);
    }

    @Override
    public Intent getSettingsIntent() {
        return null;
    }

    @Override
    public void setDream(DreamBackend dreamBackend) {
        if (dreamBackend.isEnabled()) {
            dreamBackend.setEnabled(false);
        }
        dreamBackend.setActiveDreamInfoAction(this);
    }
}
