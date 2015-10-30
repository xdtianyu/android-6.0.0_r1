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

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.tv.settings.BrowseInfoBase;
import com.android.tv.settings.MenuItem;
import com.android.tv.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Gets the list of browse headers and browse items.
 */
public class WifiNetworksBrowseInfo extends BrowseInfoBase {

    private static final String TAG = "WifiNetworksBrowseInfo";
    private static final boolean DEBUG = false;

    private static final int FIRST_HEADER_ID = 1;
    private static final int SECOND_HEADER_ID = 2;
    private static final int OTHER_OPTIONS_WPS = 0;
    private static final int OTHER_OPTIONS_ADD_NETWORK = 1;
    private static final int MSG_NETWORK_REFRESH = 1;
    private static final int NETWORK_REFRESH_TIMEOUT = 15000;
    private static final int NUMBER_SIGNAL_LEVELS = 4;

    private final Context mContext;
    private final Handler mHandler;
    private final WifiManager mWifiManager;
    private int mNextItemId;
    private final HashMap<Pair<String, WifiSecurity>, Integer> mIdToSsidMap;

    WifiNetworksBrowseInfo(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                refreshAvailableNetworksBrowseItems();
                mHandler.sendEmptyMessageDelayed(MSG_NETWORK_REFRESH, NETWORK_REFRESH_TIMEOUT);
            }
        };
        mIdToSsidMap = new HashMap<Pair<String, WifiSecurity>, Integer>();
        mNextItemId = 0;
        mRows.put(FIRST_HEADER_ID, new ArrayObjectAdapter());
        mRows.put(SECOND_HEADER_ID, new ArrayObjectAdapter());
    }

    public void onShutdown() {
        stopScanning();
        mIdToSsidMap.clear();
    }

    void init() {
        mHeaderItems.clear();
        String header1 = mContext.getString(R.string.wifi_setting_header_available_networks);
        String header2 = mContext.getString(R.string.wifi_setting_header_other_options);
        addBrowseHeader(FIRST_HEADER_ID, header1);
        addBrowseHeader(SECOND_HEADER_ID, header2);

        ArrayObjectAdapter row = mRows.get(FIRST_HEADER_ID);
        initAvailableNetworksBrowseItems(row);
        initOtherOptionsBrowseItems();
    }

    protected void startScanning() {
        mHandler.sendEmptyMessage(MSG_NETWORK_REFRESH);
    }

    protected void stopScanning() {
        mHandler.removeMessages(MSG_NETWORK_REFRESH);
    }

    private void addBrowseHeader(int id, String name) {
        mHeaderItems.add(new HeaderItem(id, name));
    }

    private void initAvailableNetworksBrowseItems(ArrayObjectAdapter row) {
        WifiInfo currentConnection = mWifiManager.getConnectionInfo();
        List<ScanResult> networks = getAvailableNetworks(currentConnection);

        if (networks.size() > 0) {
            for (ScanResult network : networks) {
                if (network != null) {
                    WifiSecurity security = WifiSecurity.getSecurity(network);

                    String networkDescription = security.isOpen() ? "" : security.getName(mContext);
                    Intent intent =
                            WifiConnectionActivity.createIntent(mContext, network, security);
                    int signalLevel = WifiManager.calculateSignalLevel(
                            network.level, NUMBER_SIGNAL_LEVELS);
                    int imageResourceId = getNetworkIconResourceId(
                            network, signalLevel);
                    if (WifiConfigHelper.areSameNetwork(mWifiManager, network, currentConnection)) {
                        networkDescription = mContext.getString(R.string.connected);
                        intent = getConnectedNetworkIntent();
                        signalLevel = WifiManager.calculateSignalLevel(
                                currentConnection.getRssi(), NUMBER_SIGNAL_LEVELS);
                        imageResourceId = getCurrentNetworkIconResourceId(network, signalLevel);
                    }

                    Integer itemId = getItemId(network, security);

                    if (DEBUG) {
                        Log.d(TAG, "Network " + itemId + " has SSID=" + network.SSID +
                                ", BSSID=" + network.BSSID +
                                ", current connected SSID=" + currentConnection.getSSID() +
                                ", current connected BSSID=" + currentConnection.getBSSID());
                    }

                    row.add(new MenuItem.Builder().id(itemId).title(network.SSID)
                            .description(networkDescription)
                            .imageResourceId(mContext, imageResourceId).intent(intent)
                            .build());
                }
            }
        } else {
            row.add(new MenuItem.Builder().id(generateNextItemId())
                    .title(mContext.getString(R.string.title_wifi_no_networks_available))
                    .imageResourceId(mContext, R.drawable.ic_settings_wifi_scan).build());
        }
    }

    private Integer getItemId(ScanResult network, WifiSecurity security) {
        Pair<String, WifiSecurity> key = Pair.create(network.SSID, security);
        Integer itemId = mIdToSsidMap.get(key);
        if (itemId == null) {
            itemId = generateNextItemId();
            mIdToSsidMap.put(key, itemId);
        }
        return itemId;
    }

    private int generateNextItemId() {
        int id = mNextItemId;
        mNextItemId++;
        return id;
    }

    private Intent getConnectedNetworkIntent() {
        return new Intent(mContext, WifiConfigurationActivity.class);
    }

    private void initOtherOptionsBrowseItems() {
        String wpsName = mContext.getString(R.string.wifi_setting_other_options_wps);
        String addNetworkName = mContext.getString(R.string.wifi_setting_other_options_add_network);
        ArrayObjectAdapter row = mRows.get(SECOND_HEADER_ID);

        addOtherOptionBrowseItem(row, OTHER_OPTIONS_WPS, wpsName);
        addOtherOptionBrowseItem(row, OTHER_OPTIONS_ADD_NETWORK, addNetworkName);
    }

    private void addOtherOptionBrowseItem(ArrayObjectAdapter row, int id, String name) {
        row.add(new MenuItem.Builder().id(id).title(name)
                .imageResourceId(mContext, getOtherOptionsIconResourceId(id))
                .intent(getOtherOptionsIntent(id)).build());
    }

    private Intent getOtherOptionsIntent(int otherOptionIndex) {
        Intent intent = null;
        switch (otherOptionIndex) {
            case OTHER_OPTIONS_WPS:
                intent = new Intent(mContext, WpsConnectionActivity.class);
                break;
            case OTHER_OPTIONS_ADD_NETWORK:
                intent = new Intent(mContext, AddWifiNetworkActivity.class);
                break;
            default:
                Log.d(TAG, "Unknown otherOptionIndex: " + otherOptionIndex);
                break;
        }
        return intent;
    }

    private int getCurrentNetworkIconResourceId(
            ScanResult scanResult, int signalLevel) {
        int resourceId = 0;
        if (scanResult != null) {
            WifiSecurity security = WifiSecurity.getSecurity(scanResult);
            if (security.isOpen()) {
                switch (signalLevel)
                {
                    case 0:
                        resourceId = R.drawable.ic_settings_wifi_active_1;
                        break;
                    case 1:
                        resourceId = R.drawable.ic_settings_wifi_active_2;
                        break;
                    case 2:
                        resourceId = R.drawable.ic_settings_wifi_active_3;
                        break;
                    case 3:
                        resourceId = R.drawable.ic_settings_wifi_active_4;
                        break;
                }
            } else {
                switch (signalLevel)
                {
                    case 0:
                        resourceId = R.drawable.ic_settings_wifi_secure_active_1;
                        break;
                    case 1:
                        resourceId = R.drawable.ic_settings_wifi_secure_active_2;
                        break;
                    case 2:
                        resourceId = R.drawable.ic_settings_wifi_secure_active_3;
                        break;
                    case 3:
                        resourceId = R.drawable.ic_settings_wifi_secure_active_4;
                        break;
                }
            }
        }
        return resourceId;
    }

    private int getNetworkIconResourceId(ScanResult scanResult, int signalLevel) {
        int resourceId = 0;
        if (scanResult != null) {
            WifiSecurity security = WifiSecurity.getSecurity(scanResult);
            if (security.isOpen()) {
                switch (signalLevel)
                {
                    case 0:
                        resourceId = R.drawable.ic_settings_wifi_1;
                        break;
                    case 1:
                        resourceId = R.drawable.ic_settings_wifi_2;
                        break;
                    case 2:
                        resourceId = R.drawable.ic_settings_wifi_3;
                        break;
                    case 3:
                        resourceId = R.drawable.ic_settings_wifi_4;
                        break;
                }
            } else {
                switch (signalLevel)
                {
                    case 0:
                        resourceId = R.drawable.ic_settings_wifi_secure_1;
                        break;
                    case 1:
                        resourceId = R.drawable.ic_settings_wifi_secure_2;
                        break;
                    case 2:
                        resourceId = R.drawable.ic_settings_wifi_secure_3;
                        break;
                    case 3:
                        resourceId = R.drawable.ic_settings_wifi_secure_4;
                        break;
                }
            }
        }
        return resourceId;
    }

    private List<ScanResult> getAvailableNetworks(WifiInfo connectedWifiInfo) {
        String currentConnectedSSID = connectedWifiInfo == null ? "" : connectedWifiInfo.getSSID();
        currentConnectedSSID = WifiInfo.removeDoubleQuotes(currentConnectedSSID);
        WifiSecurity currentConnectedSecurity = WifiConfigHelper.getCurrentConnectionSecurity(
                mWifiManager, connectedWifiInfo);

        // TODO : Refactor with similar code in SelectFromListWizard
        final List<ScanResult> results = mWifiManager.getScanResults();

        if (results.size() == 0) {
            Log.w(TAG, "No results found! Initiate scan...");
            mWifiManager.startScan();
        }

        final HashMap<Pair<String, WifiSecurity>, ScanResult> consolidatedScanResults =
                new HashMap<Pair<String, WifiSecurity>, ScanResult>();
        HashMap<Pair<String, WifiSecurity>, Boolean> specialNetworks = new HashMap<
                Pair<String, WifiSecurity>, Boolean>();
        for (ScanResult result : results) {
            if (TextUtils.isEmpty(result.SSID)) {
                continue;
            }

            Pair<String, WifiSecurity> key = Pair.create(
                    result.SSID, WifiSecurity.getSecurity(result));
            ScanResult existing = consolidatedScanResults.get(key);

            if (WifiConfigHelper.areSameNetwork(mWifiManager, result, connectedWifiInfo)) {
                // The currently connected network should always be included.
                consolidatedScanResults.put(key, result);
                specialNetworks.put(key, true);
            } else {
                if (existing == null ||
                        (!specialNetworks.containsKey(key) && existing.level < result.level)) {
                    consolidatedScanResults.put(key, result);
                }
            }
        }

        ArrayList<ScanResult> networkList = new ArrayList<ScanResult>();
        networkList.addAll(consolidatedScanResults.values());
        ScanResultComparator comparator = connectedWifiInfo == null ? new ScanResultComparator() :
                new ScanResultComparator(currentConnectedSSID, currentConnectedSecurity);
        Collections.sort(networkList, comparator);
        return networkList;
    }

    private int getOtherOptionsIconResourceId(int otherOptionIndex) {
        int resourceId = 0;
        switch (otherOptionIndex) {
            case OTHER_OPTIONS_WPS:
                resourceId = R.drawable.ic_settings_wifi_wps;
                break;
            case OTHER_OPTIONS_ADD_NETWORK:
                resourceId = R.drawable.ic_settings_add;
                break;
        }
        return resourceId;
    }

    private void refreshAvailableNetworksBrowseItems() {
        ArrayObjectAdapter row = mRows.get(FIRST_HEADER_ID);
        row.clear();
        initAvailableNetworksBrowseItems(row);
    }
}
