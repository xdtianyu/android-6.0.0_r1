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

/**
 * Maps between an Action key and an Type/Behavior Enum pair.
 */
public class ActionKey<T extends Enum<T>, B extends Enum<B>> {

    private final T mType;
    private final String mTypeString;
    private final B mBehavior;
    private final String mBehaviorString;

    public ActionKey(T type, B behavior) {
        mType = type;
        mTypeString = (mType != null) ? mType.name() : "";
        mBehavior = behavior;
        mBehaviorString = (mBehavior != null) ? mBehavior.name() : "";
    }

    public ActionKey(Class<T> clazzT, Class<B> clazzB, String key) {
        String[] typeAndBehavior = key.split(":");

        mTypeString = typeAndBehavior[0];
        if (typeAndBehavior.length > 1) {
            mBehaviorString = typeAndBehavior[1];
        } else {
            mBehaviorString = "";
        }

        T type = null;
        try {
            type = T.valueOf(clazzT, mTypeString);
        } catch (IllegalArgumentException iae) {
        }
        mType = type;

        B behavior = null;
        try {
            behavior = B.valueOf(clazzB, mBehaviorString);
        } catch (IllegalArgumentException iae) {
        }
        mBehavior = behavior;
    }

    public String getKey() {
        return mTypeString + ":" + mBehaviorString;
    }

    public T getType() {
        return mType;
    }

    public String getTypeString() {
        return mTypeString;
    }

    public B getBehavior() {
        return mBehavior;
    }

    public String getBehaviorString() {
        return mBehaviorString;
    }
}
