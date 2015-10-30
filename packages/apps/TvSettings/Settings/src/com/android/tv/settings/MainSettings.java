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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.android.tv.settings.connectivity.ConnectivityListener;

import java.util.Locale;

/**
 * Main settings which loads up the top level headers.
 */
public class MainSettings extends MenuActivity implements OnAccountsUpdateListener,
        ConnectivityListener.Listener {

    private static MainSettings sInstance;

    private BrowseInfo mBrowseInfo;
    private AccountManager mAccountManager;
    private Locale mCurrentLocale;
    private ConnectivityListener mConnectivityListener;

    public static synchronized MainSettings getInstance() {
        return sInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mBrowseInfo = new BrowseInfo(this);
        mBrowseInfo.init();
        mCurrentLocale = Locale.getDefault();
        mAccountManager = AccountManager.get(this);

        super.onCreate(savedInstanceState);

        mConnectivityListener = new ConnectivityListener(this, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAccountManager.addOnAccountsUpdatedListener(this, null, true);
        // Update here, just in case.
        onAccountsUpdated(null);
        mConnectivityListener.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sInstance = this;
        updateAccessories();
        mBrowseInfo.checkForDeveloperOptionUpdate();
        mBrowseInfo.updateSounds();

        // Update network item forcefully here
        // because mBrowseInfo doesn't have information regarding Ethernet availability.
        mBrowseInfo.updateWifi(mConnectivityListener.isEthernetAvailable());
    }

    @Override
    protected void onPause() {
        sInstance = null;
        super.onPause();
    }

    @Override
    protected void onStop() {
        mAccountManager.removeOnAccountsUpdatedListener(this);
        super.onStop();
        mConnectivityListener.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAccountManager.removeOnAccountsUpdatedListener(this);
    }

    @Override
    public void onConnectivityChange(Intent intent) {
        mBrowseInfo.updateWifi(mConnectivityListener.isEthernetAvailable());
    }

    @Override
    protected String getBrowseTitle() {
        return getString(R.string.settings_app_name);
    }

    @Override
    protected Drawable getBadgeImage() {
        return getResources().getDrawable(R.drawable.ic_settings_app_icon);
    }

    @Override
    protected BrowseInfoFactory getBrowseInfoFactory() {
        if (!mCurrentLocale.equals(Locale.getDefault())) {
            // the System Locale information has changed
            mCurrentLocale = Locale.getDefault();
            mBrowseInfo.rebuildInfo();
        }

        return mBrowseInfo;
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        mBrowseInfo.updateAccounts();
    }

    public void updateAccessories() {
        mBrowseInfo.updateAccessories();
    }
}
