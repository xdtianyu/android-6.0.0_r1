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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Gets a text string based on the current connectivity status.
 */
public class ConnectivityStatusTextGetter implements MenuItem.TextGetter {

    private final Context mContext;
    private final int mEthernetStringResourceId;
    private final int mWifiStringResourceId;
    private final boolean mUseSsid;
    private final ConnectivityManager mConnectivityManager;
    private final WifiManager mWifiManager;

    public static ConnectivityStatusTextGetter createEthernetStatusTextGetter(Context context) {
        return new ConnectivityStatusTextGetter(context, R.string.connected,
                R.string.not_connected, false);
    }

    public static ConnectivityStatusTextGetter createWifiStatusTextGetter(Context context) {
        return new ConnectivityStatusTextGetter(context, R.string.not_connected,
                R.string.not_connected, true);
    }

    public ConnectivityStatusTextGetter(Context context) {
        this(context, R.string.connectivity_ethernet, R.string.connectivity_wifi, true);
    }

    public ConnectivityStatusTextGetter(Context context, int ethernetStringResourceId,
            int wifiStringResourceId, boolean useSsid) {
        mContext = context;
        mEthernetStringResourceId = ethernetStringResourceId;
        mWifiStringResourceId = wifiStringResourceId;
        mUseSsid = useSsid;
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public String getText() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();

        if (networkInfo != null) {
            switch (networkInfo.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return getWifiString();
                case ConnectivityManager.TYPE_ETHERNET:
                    return mContext.getString(mEthernetStringResourceId);
                default:
                    break;
            }
        }

        return mContext.getString(R.string.not_connected);
    }

    private String getWifiString() {
        if (mUseSsid) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                if (ssid != null) {
                    return WifiInfo.removeDoubleQuotes(ssid);
                }
            }
        }
        return mContext.getString(mWifiStringResourceId);
    }
}
