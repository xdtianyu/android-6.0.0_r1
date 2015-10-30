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

import com.android.tv.settings.MenuItem;
import com.android.tv.settings.R;
import com.android.tv.settings.util.UriUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Gets an icon uri based on the current connectivity status.
 */
public class ConnectivityStatusIconUriGetter implements MenuItem.UriGetter {

    private static final String TAG = "ConnectivityStatusIconUriGetter";
    private static final boolean DEBUG = true;

    private final Context mContext;
    private final int mEthernetDrawableResourceId;
    private final int mDisconnectedDrawableResourceId;
    private final ConnectivityManager mConnectivityManager;
    private final WifiManager mWifiManager;

    public static ConnectivityStatusIconUriGetter createWifiStatusIconUriGetter(Context context) {
        return new ConnectivityStatusIconUriGetter(context, R.drawable.ic_settings_wifi_4,
                R.drawable.ic_settings_wifi_4);
    }

    public ConnectivityStatusIconUriGetter(Context context) {
        this(context, R.drawable.ic_settings_ethernet_active,
                R.drawable.ic_settings_connection_status);
    }

    public ConnectivityStatusIconUriGetter(Context context, int ethernetDrawableResourceId,
            int disconnectedDrawableResourceId) {
        mContext = context;
        mEthernetDrawableResourceId = ethernetDrawableResourceId;
        mDisconnectedDrawableResourceId = disconnectedDrawableResourceId;
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public String getUri() {
        String uri = UriUtils.getAndroidResourceUri(mContext, getConnectionStatusResId());
        if (DEBUG) {
            Log.d(TAG, "Returning connectivity status icon: " + uri);
        }
        return uri;
    }

    private int getConnectionStatusResId() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return mDisconnectedDrawableResourceId;
        }

        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return getWifiDrawableResourceId();
            case ConnectivityManager.TYPE_ETHERNET:
                return mEthernetDrawableResourceId;
            default:
                return mDisconnectedDrawableResourceId;
        }
    }

    private int getWifiDrawableResourceId() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        boolean isOpen = isOpenNetwork(mWifiManager, wifiInfo);
        int strength = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 4);

        switch (strength) {
            case 0:
                return isOpen ? R.drawable.ic_settings_wifi_active_1
                        : R.drawable.ic_settings_wifi_secure_active_1;
            case 1:
                return isOpen ? R.drawable.ic_settings_wifi_active_2
                        : R.drawable.ic_settings_wifi_secure_active_2;
            case 2:
                return isOpen ? R.drawable.ic_settings_wifi_active_3
                        : R.drawable.ic_settings_wifi_secure_active_3;
            case 3:
                return isOpen ? R.drawable.ic_settings_wifi_active_4
                        : R.drawable.ic_settings_wifi_secure_active_4;
        }
        return mDisconnectedDrawableResourceId;
    }

    private boolean isOpenNetwork(WifiManager wifiMan, WifiInfo wifiInfo) {
        WifiConfiguration network = WifiConfigHelper.getWifiConfiguration(wifiMan,
                wifiInfo.getNetworkId());
        if (network != null) {
            return WifiSecurity.isOpen(network);
        }
        Log.w(TAG, "Could not determine if network is secure or not! Defaulting to open icon.");
        return true;
    }
}
