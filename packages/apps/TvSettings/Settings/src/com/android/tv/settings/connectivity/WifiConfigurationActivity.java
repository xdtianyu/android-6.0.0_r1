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
import android.net.ConnectivityManager;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.BaseSettingsActivity;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ContentFragment;

/**
 * Activity to view the status and modify the configuration of the currently
 * connected wifi network.
 */

public class WifiConfigurationActivity extends BaseSettingsActivity
        implements ActionAdapter.Listener, ConnectivityListener.Listener {

    protected static final String TAG = "WifiConfigurationActivity";
    private static final boolean DEBUG = false;

    private static final int INET_CONDITION_THRESHOLD = 50;
    private static final int REQUEST_CODE_ADVANCED_OPTIONS = 1;

    private ConnectivityListener mConnectivityListener;
    private ConnectivityStatusIconUriGetter mWifiStatusIconUriGetter;
    private ConnectivityManager mConnectivityManager;
    private WifiManager mWifiManager;
    private boolean mInetConnected;
    private Handler mHandler;

    private final Runnable mMainRefreshView = new Runnable() {
        @Override
        public void run() {
            updateView();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mConnectivityListener = new ConnectivityListener(this, this);
        mWifiStatusIconUriGetter =
            ConnectivityStatusIconUriGetter.createWifiStatusIconUriGetter(this);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onCreate");
        mHandler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.d(TAG, "onResume");
        mConnectivityListener.start();
    }

    @Override
    protected void onPause() {
        mConnectivityListener.stop();
        super.onPause();
        if (DEBUG) Log.d(TAG, "onPause");
    }

    @Override
    protected Object getInitialState() {
        if (DEBUG) Log.d(TAG, "getInitialState");
        return ActionType.CONECTIVITY_SETTINGS_MAIN;
    }

    @Override
    public void onConnectivityChange(Intent intent) {
        if (DEBUG) Log.d(TAG, "onConnectivityChange  intent " + intent);
        String intentAction = intent.getAction();
        boolean inetConnectedChanged = false;
        if (intentAction.equals(ConnectivityManager.CONNECTIVITY_ACTION)
                || intentAction.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);
            boolean ic = connectionStatus > INET_CONDITION_THRESHOLD;
            if (ic != mInetConnected) {
                inetConnectedChanged = true;
                mInetConnected = ic;
            }
            if (DEBUG) Log.d(TAG, "onConnectivityChange  mInetConnected " + mInetConnected);
        }
        if (inetConnectedChanged) {
            mHandler.post(mMainRefreshView);
        } else {
            updateIconUriIfNecessary();
        }
    }

    private void updateIconUriIfNecessary() {
        if(mContentFragment instanceof ContentFragment) {
            ContentFragment cf = (ContentFragment) mContentFragment;
            Uri oldUri = cf.getIconResourceUri();
            Uri newUri = Uri.parse(mWifiStatusIconUriGetter.getUri());
            if (!oldUri.equals(newUri)) {
                cf.setIcon(newUri);
            }
        }
    }

    private WifiInfo getWifiInfo() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            return null;
        } else {
            return mWifiManager.getConnectionInfo();
        }
    }

    @Override
    protected void refreshActionList() {
        if (DEBUG) Log.d(TAG, "refreshActionList");
        mActions.clear();
        switch ((ActionType) mState) {
            case CONECTIVITY_SETTINGS_MAIN:
                mActions.add(ActionType.CONECTIVITY_SETTINGS_STATUS_INFO.toAction(mResources));
                mActions.add(ActionType.CONECTIVITY_SETTINGS_ADVANCED_OPTIONS.toAction(mResources));
                mActions.add(ActionType.CONECTIVITY_SETTINGS_FORGET_NETWORK.toAction(mResources));
                break;
            case CONECTIVITY_SETTINGS_STATUS_INFO: {
                boolean isConnected = false;
                WifiInfo wifiInfo = getWifiInfo();
                if (wifiInfo != null) {
                    NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
                    if (networkInfo != null &&
                        networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                        mInetConnected) {
                        isConnected = true;
                    }
                }

                if (!isConnected) {
                    mActions.add(ActionType.CONECTIVITY_SETTINGS_CONNECTION.
                                     toInfo(mResources, R.string.not_connected));
                } else {
                    // If we're on a wifi-network and the status is good...
                    mActions.add(
                        ActionType.CONECTIVITY_SETTINGS_CONNECTION.
                            toInfo(mResources, R.string.connected));

                    int ip = wifiInfo.getIpAddress();
                    mActions.add(ActionType.CONECTIVITY_SETTINGS_IP_ADDRESS.
                        toInfo(mResources,
                                 String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff),
                                               (ip >> 16 & 0xff), (ip >> 24 & 0xff))));

                    mActions.add(ActionType.CONECTIVITY_SETTINGS_MAC_ADDRESS.
                        toInfo(mResources, wifiInfo.getMacAddress()));

                    String[] signalLevels =
                        getResources().getStringArray(R.array.wifi_signal_strength);
                    int strength =
                        WifiManager.
                            calculateSignalLevel(wifiInfo.getRssi(), signalLevels.length);
                    mActions.add(ActionType.CONECTIVITY_SETTINGS_SIGNAL_STRENGTH.
                        toInfo(mResources, signalLevels[strength]));
                }
                break;
            }
            case CONECTIVITY_SETTINGS_ADVANCED_OPTIONS: {
                WifiInfo wifiInfo = getWifiInfo();
                if (wifiInfo != null) {
                    WifiConfiguration wifiConfiguration =
                        WifiConfigHelper.getWifiConfiguration(
                            mWifiManager, wifiInfo.getNetworkId());
                    if (wifiConfiguration != null) {
                        int proxySettingsResourceId =
                            (wifiConfiguration.getProxySettings() == ProxySettings.NONE) ?
                                R.string.wifi_action_proxy_none :
                                R.string.wifi_action_proxy_manual;
                        mActions.add(ActionType.CONECTIVITY_SETTINGS_PROXY_SETTINGS.
                                            toAction(mResources, proxySettingsResourceId));

                        int ipSettingsResourceId =
                           (wifiConfiguration.getIpAssignment() == IpAssignment.STATIC) ?
                                R.string.wifi_action_static :
                                R.string.wifi_action_dhcp;
                        mActions.add(ActionType.CONECTIVITY_SETTINGS_IP_SETTINGS.
                                            toAction(mResources, ipSettingsResourceId));
                    }
                } else {
                    mActions.add(ActionType.CONECTIVITY_SETTINGS_CONNECTION.
                                     toInfo(mResources, R.string.not_connected));
                }
                break;
            }

            case CONECTIVITY_SETTINGS_FORGET_NETWORK: {
                String okKey =
                    new ActionKey<>(
                        ActionType.CONECTIVITY_SETTINGS_FORGET_NETWORK, ActionBehavior.OK).getKey();
                mActions.add(
                    new Action.Builder()
                        .key(okKey)
                        .title(getString(R.string.wifi_forget_network))
                        .build());
                String cancelKey =
                    new ActionKey<>(
                            ActionType.CONECTIVITY_SETTINGS_FORGET_NETWORK,
                            ActionBehavior.CANCEL).getKey();
                mActions.add(
                    new Action.Builder()
                        .key(cancelKey)
                        .title(getString(R.string.settings_cancel))
                        .build());
                break;
            }
        }
    }

    private String getNetworkName() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        String name = getString(R.string.connectivity_wifi);
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                name = WifiInfo.removeDoubleQuotes(wifiInfo.getSSID());
            }
        }
        return name;
    }

    @Override
    protected void updateView() {
        refreshActionList();
        if (DEBUG) Log.d(TAG, "updateView  mState " + mState);
        switch ((ActionType) mState) {
            case CONECTIVITY_SETTINGS_MAIN: {
                setView(getNetworkName(), null, null,
                        Uri.parse(mWifiStatusIconUriGetter.getUri()));
                break;
            }
            case CONECTIVITY_SETTINGS_STATUS_INFO: {
                setView(getString(R.string.wifi_action_status_info), getNetworkName(), null,
                        Uri.parse(mWifiStatusIconUriGetter.getUri()));
                break;
            }
            case CONECTIVITY_SETTINGS_ADVANCED_OPTIONS: {
                setView(getString(R.string.wifi_action_advanced_options_title),
                        getNetworkName(), null, Uri.parse(mWifiStatusIconUriGetter.getUri()));
                break;
            }
            case CONECTIVITY_SETTINGS_FORGET_NETWORK: {
                setView(R.string.wifi_forget_network, getNetworkName(),
                        R.string.wifi_forget_network_description,
                        Uri.parse(mWifiStatusIconUriGetter.getUri()));
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ADVANCED_OPTIONS && resultCode == RESULT_OK) {
            updateView();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void setProperty(boolean enable) {
    }

    private int getNetworkId() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null) {
              return wifiInfo.getNetworkId();
        }
        return -1;
    }

    @Override
    public void onActionClicked(Action action) {
        if (DEBUG) Log.d(TAG, "onActionClicked " + action.getKey());

        ActionKey<ActionType, ActionBehavior> actionKey =
            new ActionKey<>(
                ActionType.class, ActionBehavior.class, action.getKey());
        final ActionType type = actionKey.getType();
        final ActionBehavior behavior = actionKey.getBehavior();

        switch (type) {
            case CONECTIVITY_SETTINGS_STATUS_INFO:
                switch (behavior) {
                    case INIT:
                        setState(type, true);
                        break;
                }
                break;
            case CONECTIVITY_SETTINGS_ADVANCED_OPTIONS:
                switch (behavior) {
                    case INIT:
                        setState(type, true);
                        break;
                }
                break;
            case CONECTIVITY_SETTINGS_PROXY_SETTINGS:
                switch (behavior) {
                    case INIT: {
                        int networkId = getNetworkId();
                        if (networkId != -1) {
                            startActivityForResult(
                                EditProxySettingsActivity.createIntent(this, networkId),
                                REQUEST_CODE_ADVANCED_OPTIONS);
                        }
                        break;
                    }
                }
                break;
            case CONECTIVITY_SETTINGS_IP_SETTINGS:
                switch (behavior) {
                    case INIT: {
                        int networkId = getNetworkId();
                        if (networkId != -1) {
                            startActivityForResult(
                                EditIpSettingsActivity.createIntent(this, networkId),
                                REQUEST_CODE_ADVANCED_OPTIONS);
                        }
                        break;
                    }
                }
                break;
            case CONECTIVITY_SETTINGS_FORGET_NETWORK: {
                switch (behavior) {
                    case INIT:
                        setState(type, true);
                        break;
                    case OK: {
                        int networkId = getNetworkId();
                        if (networkId != -1) {
                            mWifiManager.forget(networkId, null);
                        }
                        setResult(RESULT_OK);
                        finish();
                        break;
                    }
                    case CANCEL:
                        goBack();
                        break;
                }
                break;
            }
        }
    }
}
