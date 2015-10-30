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

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import com.android.tv.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles clearing an application's defaults.
 */
class DefaultClearer {

    interface Listener {
        void defaultCleared();
    }

    private final Listener mListener;
    private final AppInfo mAppInfo;
    private boolean mIsDefault;

    DefaultClearer(Listener listener, AppInfo appInfo) {
        mListener = listener;
        mAppInfo = appInfo;
        mIsDefault = true;
    }

    void clearDefault(Context context) {
        PackageManager packageManager = context.getPackageManager();

        packageManager.clearPackagePreferredActivities(mAppInfo.getPackageName());
        try {
            IBinder b = ServiceManager.getService(Context.USB_SERVICE);
            if (b != null) {
                IUsbManager usbManager = IUsbManager.Stub.asInterface(b);
                usbManager.clearDefaults(mAppInfo.getPackageName(), UserHandle.myUserId());
            }
        } catch (RemoteException e) {
            // Ignore
        }

        mIsDefault = false;
        mListener.defaultCleared();
    }

    boolean isDefault(Context context) {
        if (mIsDefault) {
            PackageManager packageManager = context.getPackageManager();

            // Get list of preferred activities
            List<ComponentName> prefActList = new ArrayList<>();

            List<IntentFilter> intentList = new ArrayList<>();
            packageManager.getPreferredActivities(
                    intentList, prefActList, mAppInfo.getPackageName());

            boolean hasUsbDefaults = false;
            try {
                IBinder b = ServiceManager.getService(Context.USB_SERVICE);
                if (b != null) {
                    IUsbManager usbManager = IUsbManager.Stub.asInterface(b);
                    hasUsbDefaults =
                            usbManager
                                    .hasDefaults(mAppInfo.getPackageName(), UserHandle.myUserId());
                }
            } catch (RemoteException e) {
                // Ignore
            }

            if (prefActList.size() <= 0 && !hasUsbDefaults) {
                mIsDefault = false;
            }
        }

        return mIsDefault;
    }

    String getDescription(Context context) {
        if (isDefault(context)) {
            return context.getString(R.string.device_apps_app_management_clear_default_set);
        }
        return context.getString(R.string.device_apps_app_management_clear_default_none);
    }
}
