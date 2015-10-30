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
import android.content.res.Resources;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.Layout.Action;
import com.android.tv.settings.dialog.Layout.Header;
import com.android.tv.settings.dialog.Layout.LayoutGetter;
import com.android.tv.settings.dialog.Layout.SelectionGroup;
import com.android.tv.settings.dialog.Layout.Static;
import com.android.tv.settings.dialog.Layout.Status;
import com.android.tv.settings.dialog.Layout.StringGetter;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to manage network settings.
 */
public class NetworkActivity extends SettingsLayoutActivity implements
        ConnectivityListener.Listener, ConnectivityListener.WifiNetworkListener {

    private static final int REQUEST_CODE_ADVANCED_OPTIONS = 1;
    private static final int WIFI_SCAN_INTERVAL_CAP_MILLIS = 10 * 1000;
    private static final int WIFI_UI_REFRESH_INTERVAL_CAP_MILLIS = 15 * 1000;

    private static final int NUMBER_SIGNAL_LEVELS = 4;
    private static final int ACTION_WIFI_FORGET_NETWORK = 1;
    private static final int ACTION_WIFI_PROXY_SETTINGS = 4;
    private static final int ACTION_WIFI_IP_SETTINGS = 5;
    private static final int ACTION_ETHERNET_PROXY_SETTINGS = 6;
    private static final int ACTION_ETHERNET_IP_SETTINGS = 7;
    private static final int ACTION_SCAN_WIFI_ON = 8;
    private static final int ACTION_SCAN_WIFI_OFF = 9;
    private static final int ACTION_WIFI_ENABLE_ON = 10;
    private static final int ACTION_WIFI_ENABLE_OFF = 11;

    private ConnectivityListener mConnectivityListener;
    private Resources mRes;
    private final Handler mHandler = new Handler();

    private final Runnable mRefreshWifiAccessPoints = new Runnable() {
        @Override
        public void run() {
            mConnectivityListener.scanWifiAccessPoints(NetworkActivity.this);
            mHandler.removeCallbacks(mRefreshWifiAccessPoints);
            mHandler.postDelayed(mRefreshWifiAccessPoints, WIFI_SCAN_INTERVAL_CAP_MILLIS);
        }
    };

    private final Runnable mSetScanAlways = new Runnable () {
        @Override
        public void run() {
            int setting = mAlwaysScanWifi.getId() == ACTION_SCAN_WIFI_ON ? 1 : 0;
            Settings.Global.putInt(getContentResolver(),
                        Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, setting);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mRes = getResources();
        mConnectivityListener = new ConnectivityListener(this, this);

        mAlwaysScanWifi = new SelectionGroup(getResources(), new int[][] {
            { R.string.on, ACTION_SCAN_WIFI_ON },
            { R.string.off, ACTION_SCAN_WIFI_OFF } });
        int scanAlwaysAvailable = 0;
        try {
            scanAlwaysAvailable = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE);
        } catch (Settings.SettingNotFoundException e) {
        }
        mAlwaysScanWifi.setSelected(scanAlwaysAvailable == 1 ? ACTION_SCAN_WIFI_ON :
                ACTION_SCAN_WIFI_OFF);

        mEnableWifi = new SelectionGroup(mRes, new int[][] {
                { R.string.on, ACTION_WIFI_ENABLE_ON },
                { R.string.off, ACTION_WIFI_ENABLE_OFF } });
        mEnableWifi.setSelected(mConnectivityListener.isWifiEnabled() ? ACTION_WIFI_ENABLE_ON :
                ACTION_WIFI_ENABLE_OFF);
        refreshEnableWifiSelection();
        // The ConectivityListenter must be started before calling "super.OnCreate(.)" to ensure
        // that connectivity status is available before the layout is constructed.
        mConnectivityListener.start();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        mConnectivityListener.start();
        mHandler.removeCallbacks(mRefreshWifiAccessPoints);
        mHandler.post(mRefreshWifiAccessPoints);
        onConnectivityChange(null);

        // TODO(lanechr): It's an anti-pattern that we have to notify Layout here; see b/18889239.
        mWifiAdvancedLayout.refreshView();

        super.onResume();
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(mRefreshWifiAccessPoints);
        mConnectivityListener.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        mConnectivityListener = null;
        super.onDestroy();
    }

    // ConnectivityListener.Listener overrides.
    @Override
    public void onConnectivityChange(Intent intent) {
        mEthernetConnectedDescription.refreshView();
        mWifiConnectedDescription.refreshView();
        refreshEnableWifiSelection();
        onWifiListChanged();
        mWifiLayout.refreshView();
    }

    private void refreshEnableWifiSelection() {
        mEnableWifi.setSelected(mConnectivityListener.isWifiEnabled() ? ACTION_WIFI_ENABLE_ON :
                ACTION_WIFI_ENABLE_OFF);
    }

    @Override
    public void onWifiListChanged() {
        mWifiShortListLayout.onWifiListChanged();
        mWifiAllListLayout.onWifiListChanged();
    }

    final StringGetter mEthernetConnectedDescription = new StringGetter() {
        private boolean lastIsEthernetConnected;
        @Override
        public String get() {
            lastIsEthernetConnected =
                    mConnectivityListener.getConnectivityStatus().isEthernetConnected();
            int resId = lastIsEthernetConnected ? R.string.connected : R.string.not_connected;
            return mRes.getString(resId);
        }
        @Override
        public void refreshView() {
            if (mConnectivityListener.getConnectivityStatus().isEthernetConnected() !=
                    lastIsEthernetConnected) {
                super.refreshView();
            }
        }
    };

    final StringGetter mWifiConnectedDescription = new StringGetter() {
        private boolean lastIsWifiConnected;
        @Override
        public String get() {
            lastIsWifiConnected = mConnectivityListener.getConnectivityStatus().isWifiConnected();
            int resId = lastIsWifiConnected ? R.string.connected : R.string.not_connected;
            return mRes.getString(resId);
        }
        @Override
        public void refreshView() {
            if (mConnectivityListener.getConnectivityStatus().isWifiConnected() !=
                    lastIsWifiConnected) {
                super.refreshView();
            }
        }
    };

    final StringGetter mEthernetIPAddress = new StringGetter() {
        public String get() {
            ConnectivityListener.ConnectivityStatus status =
                    mConnectivityListener.getConnectivityStatus();
            if (status.isEthernetConnected()) {
                return mConnectivityListener.getEthernetIpAddress();
            } else {
                return "";
            }
        }
    };

    final StringGetter mEthernetMacAddress = new StringGetter() {
        public String get() {
            return mConnectivityListener.getEthernetMacAddress();
        }
    };

    final LayoutGetter mEthernetAdvancedLayout = new LayoutGetter() {
        public Layout get() {
            Layout layout = new Layout();
            // Do not check Ethernet's availability here
            // because it might not be active due to the invalid configuration.
            IpConfiguration ipConfiguration = mConnectivityListener.getIpConfiguration();
            if (ipConfiguration != null) {
                int proxySettingsResourceId =
                    (ipConfiguration.getProxySettings() == ProxySettings.STATIC) ?
                        R.string.wifi_action_proxy_manual :
                        R.string.wifi_action_proxy_none;
                int ipSettingsResourceId =
                    (ipConfiguration.getIpAssignment() == IpAssignment.STATIC) ?
                        R.string.wifi_action_static :
                        R.string.wifi_action_dhcp;
                layout
                    .add(new Action.Builder(mRes, ACTION_ETHERNET_PROXY_SETTINGS)
                            .title(R.string.title_wifi_proxy_settings)
                            .description(proxySettingsResourceId).build())
                    .add(new Action.Builder(mRes, ACTION_ETHERNET_IP_SETTINGS)
                            .title(R.string.title_wifi_ip_settings)
                            .description(ipSettingsResourceId).build());
            } else {
                layout
                    .add(new Status.Builder(mRes)
                            .title(R.string.title_internet_connection)
                            .description(R.string.not_connected).build());
            }
            return layout;
        }
    };

    final LayoutGetter mEthernetLayout = new LayoutGetter() {
        public Layout get() {
            boolean ethernetConnected =
                    mConnectivityListener.getConnectivityStatus().isEthernetConnected();
            if (ethernetConnected) {
                return new Layout()
                    .add(new Status.Builder(mRes).title(R.string.title_internet_connection)
                            .description(R.string.connected).build())
                    .add(new Status.Builder(mRes)
                            .title(R.string.title_ip_address)
                            .description(mEthernetIPAddress)
                            .build())
                    .add(new Status.Builder(mRes)
                            .title(R.string.title_mac_address)
                            .description(mEthernetMacAddress)
                            .build())
                    .add(new Header.Builder(mRes)
                            .title(R.string.wifi_action_advanced_options_title).build()
                        .add(mEthernetAdvancedLayout)
                     );

            } else {
                return new Layout()
                    .add(new Status.Builder(mRes)
                            .title(R.string.title_internet_connection)
                            .description(R.string.not_connected)
                            .build())
                    .add(new Header.Builder(mRes)
                            .title(R.string.wifi_action_advanced_options_title).build()
                        .add(mEthernetAdvancedLayout)
                    );
            }
        }
    };

    private final Context mContext = this;

    private String getSignalStrength() {
        String[] signalLevels = mRes.getStringArray(R.array.wifi_signal_strength);
        int strength = mConnectivityListener.getWifiSignalStrength(signalLevels.length);
        return signalLevels[strength];
    }

    final LayoutGetter mWifiInfoLayout = new LayoutGetter() {
        public Layout get() {
            Layout layout = new Layout();
            ConnectivityListener.ConnectivityStatus status =
                    mConnectivityListener.getConnectivityStatus();
            boolean isConnected = status.isWifiConnected();
            if (isConnected) {
                layout
                    .add(new Status.Builder(mRes)
                            .title(R.string.title_internet_connection)
                            .description(R.string.connected).build())
                    .add(new Status.Builder(mRes)
                            .title(R.string.title_ip_address)
                            .description(mConnectivityListener.getWifiIpAddress()).build())
                    .add(new Status.Builder(mRes)
                             .title(R.string.title_mac_address)
                            .description(mConnectivityListener.getWifiMacAddress()).build())
                    .add(new Status.Builder(mRes)
                             .title(R.string.title_signal_strength)
                            .description(getSignalStrength()).build());
            } else {
                layout
                    .add(new Status.Builder(mRes)
                            .title(R.string.title_internet_connection)
                            .description(R.string.not_connected).build());
            }
            return layout;
        }
    };

    final LayoutGetter mWifiAdvancedLayout = new LayoutGetter() {
        public Layout get() {
            Layout layout = new Layout();
            WifiConfiguration wifiConfiguration = mConnectivityListener.getWifiConfiguration();
            if (wifiConfiguration != null) {
                int proxySettingsResourceId =
                    (wifiConfiguration.getProxySettings() == ProxySettings.NONE) ?
                        R.string.wifi_action_proxy_none :
                        R.string.wifi_action_proxy_manual;
                int ipSettingsResourceId =
                   (wifiConfiguration.getIpAssignment() == IpAssignment.STATIC) ?
                        R.string.wifi_action_static :
                        R.string.wifi_action_dhcp;
                layout
                    .add(new Action.Builder(mRes, ACTION_WIFI_PROXY_SETTINGS)
                            .title(R.string.title_wifi_proxy_settings)
                            .description(proxySettingsResourceId).build())
                    .add(new Action.Builder(mRes, ACTION_WIFI_IP_SETTINGS)
                            .title(R.string.title_wifi_ip_settings)
                            .description(ipSettingsResourceId).build());
            } else {
                layout
                    .add(new Status.Builder(mRes)
                            .title(R.string.title_internet_connection)
                            .description(R.string.not_connected).build());
            }
            return layout;
        }
    };

    private SelectionGroup mAlwaysScanWifi;
    private SelectionGroup mEnableWifi;

    final LayoutGetter mWifiLayout = new LayoutGetter() {
        public Layout get() {
            final Layout layout = new Layout()
                    .add(new Header.Builder(mRes)
                            .title(R.string.wifi_setting_enable_wifi)
                            .description(mEnableWifi)
                            .build()
                            .add(mEnableWifi));
            if (mConnectivityListener.isWifiEnabled()) {
                layout
                        .add(new Static.Builder(mRes)
                                .title(R.string.wifi_setting_available_networks)
                                .build())
                        .add(mWifiShortListLayout)
                        .add(new Header.Builder(mRes)
                                .title(R.string.wifi_setting_see_all)
                                .build()
                                .add(mWifiAllListLayout))
                        .add(new Static.Builder(mRes)
                                .title(R.string.wifi_setting_header_other_options)
                                .build())
                        .add(new Action.Builder(mRes,
                                new Intent(NetworkActivity.this,
                                        WpsConnectionActivity.class))
                                .title(R.string.wifi_setting_other_options_wps)
                                .build())
                        .add(new Action.Builder(mRes,
                                new Intent(NetworkActivity.this,
                                        AddWifiNetworkActivity.class))
                                .title(R.string.wifi_setting_other_options_add_network)
                                .build())
                        .add(new Header.Builder(mRes)
                                .title(R.string.wifi_setting_always_scan)
                                .description(mAlwaysScanWifi)
                                .detailedDescription(
                                        R.string.wifi_setting_always_scan_context)
                                .build()
                                .add(mAlwaysScanWifi));
            }
            return layout;
        }
    };

    private void addWifiConnectedHeader(Layout layout, String SSID, int iconResId) {
        layout
            .add(new Header.Builder(mRes)
                    .title(SSID)
                    .icon(iconResId)
                    .description(R.string.connected).build()
                .add(new Header.Builder(mRes)
                        .title(R.string.wifi_action_status_info).build()
                    .add(mWifiInfoLayout)
                )
                .add(new Header.Builder(mRes)
                        .title(R.string.wifi_action_advanced_options_title).build()
                    .add(mWifiAdvancedLayout)
                )
                .add(new Header.Builder(mRes)
                        .title(R.string.wifi_forget_network).build()
                    .add(new Action.Builder(mRes, ACTION_WIFI_FORGET_NETWORK)
                            .title(R.string.title_ok).build())
                    .add(new Action.Builder(mRes, Action.ACTION_BACK)
                            .title(R.string.title_cancel).build())
                 )
            );
    }

    private class WifiListLayout extends LayoutGetter {
        private final boolean mTop3EntriesOnly;
        private String mSelectedTitle;
        private long mLastWifiRefresh = 0;

        private final Runnable mRefreshViewRunnable = new Runnable() {
            @Override
            public void run() {
                Layout.Node selected = getSelectedNode();
                if (selected != null) {
                    mSelectedTitle = selected.getTitle();
                }
                refreshView();
            }
        };

        WifiListLayout(boolean top3EntriesOnly) {
            mTop3EntriesOnly = top3EntriesOnly;
        }

        @Override
        public Layout get() {
            mLastWifiRefresh = SystemClock.elapsedRealtime();
            mHandler.removeCallbacks(mRefreshViewRunnable);
            return initAvailableWifiNetworks(mTop3EntriesOnly, mSelectedTitle).
                    setSelectedByTitle(mSelectedTitle);
        }

        /**
         * Wifi network list has changed and an eventual refresh of the UI is required.
         * Rate limit the UI refresh to once per WIFI_UI_REFRESH_INTERVAL_CAP_MILLIS.
         */
        public void onWifiListChanged() {
            long now = SystemClock.elapsedRealtime();
            long millisToNextRefreshView =
                    WIFI_UI_REFRESH_INTERVAL_CAP_MILLIS - now + mLastWifiRefresh;
            mHandler.removeCallbacks(mRefreshViewRunnable);
            mHandler.postDelayed(mRefreshViewRunnable, millisToNextRefreshView);
        }

        /**
         * Wifi network configuration has changed and an immediate refresh of the list of Wifi
         * networks is required.
         */
        public void onWifiListInvalidated() {
            mHandler.removeCallbacks(mRefreshViewRunnable);
            mHandler.post(mRefreshViewRunnable);
        }

        /**
         * Create a list of available Wifi networks sorted by connection status (a connected Wifi
         * network is shown at the first position on the list) and signal strength, with the
         * provisio that the wireless network with SSID "mustHave" should be included in the list
         * even if it would be otherwise excluded.
         *
         * @param top3EntriesOnly Show only 3 entries in the list.
         * @param mustHave        Include this wifi network in the list even if it would otherwise
         *                        be excluded by virtue of inadequate signal strength.
         */
        private Layout initAvailableWifiNetworks(boolean top3EntriesOnly, String mustHave) {
            List<ScanResult> networks = mConnectivityListener.getAvailableNetworks();
            Layout layout = new Layout();
            if (networks.size() > 0) {
                int maxItems = top3EntriesOnly ? 3 : Integer.MAX_VALUE;
                // "networks" is already sorted by the signal strength and connection status.
                // Generate a new list with size less than "maxItems" that ensures "mustHave" is
                // included.
                boolean haveMustHave = false;
                List<ScanResult> displayList = new ArrayList<>();
                for (ScanResult scanResult : networks) {
                    if (!haveMustHave && TextUtils.equals(scanResult.SSID, mustHave)) {
                        haveMustHave = true;
                        if (displayList.size() == maxItems) {
                            displayList.remove(maxItems-1);
                        }
                        displayList.add(scanResult);
                    } else if (displayList.size() < maxItems) {
                        displayList.add(scanResult);
                    }
                    if (haveMustHave && displayList.size() == maxItems) {
                        break;
                    }
                }

                // If a network is connected, it will be the first on the list.
                boolean isConnected =
                    mConnectivityListener.getConnectivityStatus().isWifiConnected();
                for (ScanResult network : displayList) {
                    if (network != null) {
                        WifiSecurity security = WifiSecurity.getSecurity(network);
                        int signalLevel = WifiManager.calculateSignalLevel(
                                network.level, NUMBER_SIGNAL_LEVELS);
                        int imageResourceId = getNetworkIconRes(security.isOpen(), signalLevel);

                        if (isConnected) {
                            addWifiConnectedHeader(layout, network.SSID, imageResourceId);
                        } else {
                            Intent intent =
                                WifiConnectionActivity.createIntent(mContext, network, security);
                            String networkDescription =
                                security.isOpen() ? "" : security.getName(mContext);
                            layout.add(new Action.Builder(mRes, intent)
                                    .title(network.SSID)
                                    .icon(imageResourceId)
                                    .description(networkDescription).build());
                        }
                    }
                    isConnected = false;
                }
            } else {
                layout.add(new Action.Builder(mRes, 0)
                       .title(R.string.title_wifi_no_networks_available).build());
            }
            return layout;
        }
    }

    private final WifiListLayout mWifiShortListLayout = new WifiListLayout(true);

    private final WifiListLayout mWifiAllListLayout = new WifiListLayout(false);

    @Override
    public Layout createLayout() {
        // Note: This only updates the layout the activity is loaded,
        //       not if the user plugs/unplugs in an adapter.
        if (mConnectivityListener.isEthernetAvailable()) {
            return new Layout()
                .breadcrumb(getString(R.string.header_category_device))
                .add(new Header.Builder(mRes)
                        .icon(R.drawable.ic_settings_wifi_4)
                        .title(R.string.connectivity_network)
                        .description(mWifiConnectedDescription)
                        .build()
                    .add(new Header.Builder(mRes)
                            .title(R.string.connectivity_wifi)
                            .contentIconRes(R.drawable.ic_settings_wifi_4)
                            .description(mWifiConnectedDescription)
                            .build()
                        .add(mWifiLayout))
                    .add(new Header.Builder(mRes)
                            .title(R.string.connectivity_ethernet)
                            .contentIconRes(R.drawable.ic_settings_ethernet)
                            .description(mEthernetConnectedDescription)
                            .build()
                        .add(mEthernetLayout)));
        } else {
            // Only Wifi is available.
            return new Layout()
                .breadcrumb(getString(R.string.header_category_device))
                .add(new Header.Builder(mRes)
                        .icon(R.drawable.ic_settings_wifi_4)
                        .title(R.string.connectivity_wifi)
                        .description(mWifiConnectedDescription)
                        .build()
                    .add(mWifiLayout));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ADVANCED_OPTIONS && resultCode == RESULT_OK) {
            //TODO make sure view reflects model deltas
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onActionFocused(Layout.LayoutRow item) {
        int resId = item.getContentIconRes();
        if (resId != 0) {
            setIcon(resId);
        }
    }

    @Override
    public void onActionClicked(Action action) {
        switch (action.getId()) {
            case Action.ACTION_INTENT:
                startActivityForResult(action.getIntent(), REQUEST_CODE_ADVANCED_OPTIONS);
                break;
            case ACTION_WIFI_FORGET_NETWORK:
                mConnectivityListener.forgetWifiNetwork();
                goBackToTitle(mRes.getString(R.string.connectivity_wifi));
                mWifiShortListLayout.onWifiListInvalidated();
                mWifiAllListLayout.onWifiListInvalidated();
                break;
            case ACTION_WIFI_PROXY_SETTINGS: {
                int networkId = mConnectivityListener.getWifiNetworkId();
                if (networkId != -1) {
                    startActivityForResult(EditProxySettingsActivity.createIntent(this, networkId),
                            REQUEST_CODE_ADVANCED_OPTIONS);
                }
                break;
            }
            case ACTION_WIFI_IP_SETTINGS: {
                int networkId = mConnectivityListener.getWifiNetworkId();
                if (networkId != -1) {
                    startActivityForResult(EditIpSettingsActivity.createIntent(this, networkId),
                            REQUEST_CODE_ADVANCED_OPTIONS);
                }
                break;
            }
            case ACTION_ETHERNET_PROXY_SETTINGS: {
                int networkId = WifiConfiguration.INVALID_NETWORK_ID;
                startActivityForResult(EditProxySettingsActivity.createIntent(this, networkId),
                        REQUEST_CODE_ADVANCED_OPTIONS);
                break;
            }
            case ACTION_ETHERNET_IP_SETTINGS: {
                int networkId = WifiConfiguration.INVALID_NETWORK_ID;
                startActivityForResult(EditIpSettingsActivity.createIntent(this, networkId),
                        REQUEST_CODE_ADVANCED_OPTIONS);
                break;
            }
            case ACTION_SCAN_WIFI_ON:
            case ACTION_SCAN_WIFI_OFF:
                mHandler.post(mSetScanAlways);
                break;
            case ACTION_WIFI_ENABLE_ON:
                mConnectivityListener.setWifiEnabled(true);
                break;
            case ACTION_WIFI_ENABLE_OFF:
                mConnectivityListener.setWifiEnabled(false);
                break;
        }
    }

    private int getNetworkIconRes(boolean isOpen, int signalLevel) {
        int resourceId = R.drawable.ic_settings_wifi_not_connected;

        if (isOpen) {
            switch (signalLevel) {
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
            switch (signalLevel) {
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

        return resourceId;
    }
}
