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

import static android.provider.Settings.Secure.SCREENSAVER_ENABLED;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages communication with the dream manager service.
 */
class DreamBackend {

    private static final String TAG = "DreamBackend";
    private static final boolean DEBUG = false;

    private final ContentResolver mContentResolver;
    private final PackageManager mPackageManager;
    private final Resources mResources;
    private final IDreamManager mDreamManager;
    private final boolean mDreamsEnabledByDefault;
    private final ArrayList<DreamInfoAction> mDreamInfoActions;
    private String mActiveDreamTitle;

    DreamBackend(Context context) {
        mContentResolver = context.getContentResolver();
        mPackageManager = context.getPackageManager();
        mResources = context.getResources();
        mDreamManager = IDreamManager.Stub.asInterface(
                ServiceManager.getService(DreamService.DREAM_SERVICE));
        mDreamsEnabledByDefault = mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsEnabledByDefault);
        mDreamInfoActions = new ArrayList<DreamInfoAction>();
    }

    void initDreamInfoActions() {
        ComponentName activeDream = getActiveDream();
        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentServices(
                new Intent(DreamService.SERVICE_INTERFACE), PackageManager.GET_META_DATA);
        for (int i = 0, size = resolveInfos.size(); i< size; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            if (resolveInfo.serviceInfo == null) {
                continue;
            }
            DreamInfoAction action = new DreamInfoAction(resolveInfo,
                    isEnabled() ? activeDream : null, mPackageManager);
            mDreamInfoActions.add(action);
            if(action.isChecked() && isEnabled()) {
                mActiveDreamTitle = action.getTitle();
            }
        }
        Collections.sort(mDreamInfoActions,
                new DreamInfoAction.DreamInfoActionComparator(getDefaultDream()));
        DreamInfoAction none = new NoneDreamInfoAction(
                mResources.getString(R.string.device_daydreams_none), isEnabled());
        mDreamInfoActions.add(0, none);
        if(mActiveDreamTitle == null) {
            mActiveDreamTitle = none.getTitle();
        }
    }

    ArrayList<Action> getDreamInfoActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.addAll(mDreamInfoActions);
        return actions;
    }

    boolean isEnabled() {
        int enableDefault = mDreamsEnabledByDefault ? 1 : 0;
        return Settings.Secure.getInt(mContentResolver, SCREENSAVER_ENABLED, enableDefault) == 1;
    }

    void setEnabled(boolean value) {
        Settings.Secure.putInt(mContentResolver, SCREENSAVER_ENABLED, value ? 1 : 0);
    }

    void setActiveDream(ComponentName dream) {
        if (mDreamManager != null) {
            try {
                ComponentName[] dreams = dream == null ? null : new ComponentName[] { dream };
                mDreamManager.setDreamComponents(dreams);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to set active dream to " + dream, e);
            }
        }
    }

    void setActiveDreamInfoAction(DreamInfoAction dreamInfoAction) {
        mActiveDreamTitle = dreamInfoAction.getTitle();
    }

    String getActiveDreamTitle() {
        return mActiveDreamTitle;
    }

    ComponentName getActiveDream() {
        if (mDreamManager != null) {
            try {
                ComponentName[] dreams = mDreamManager.getDreamComponents();
                return dreams != null && dreams.length > 0 ? dreams[0] : null;
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to get active dream", e);
            }
        }
        return null;
    }

    void startDreaming() {
        if (mDreamManager != null) {
            try {
                mDreamManager.dream();
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to dream", e);
            }
        }
    }

    private ComponentName getDefaultDream() {
        if (mDreamManager != null) {
            try {
                return mDreamManager.getDefaultDreamComponent();
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to get default dream", e);
            }
        }
        return null;
    }
}
