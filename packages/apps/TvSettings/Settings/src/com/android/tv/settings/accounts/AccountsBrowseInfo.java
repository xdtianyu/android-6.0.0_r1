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

package com.android.tv.settings.accounts;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;

import com.android.tv.settings.BrowseInfoBase;
import com.android.tv.settings.MenuItem;
import com.android.tv.settings.R;

/**
 * Browse info for accounts.
 */
public class AccountsBrowseInfo extends BrowseInfoBase {

    private static final int ID_SYNC = 0;
    private static final int ID_REMOVE_ACCOUNT = 1;

    private static final String AUTHORITY_REMOVE = "remove_account";

    private final Context mContext;
    private final String mAccountName;
    private final AuthenticatorHelper mAuthenticatorHelper;
    private int mNextItemId;

    AccountsBrowseInfo(Context context, String accountName) {
        mContext = context;
        mAccountName = accountName;
        mAuthenticatorHelper = new AuthenticatorHelper();
        mAuthenticatorHelper.updateAuthDescriptions(context);
        mAuthenticatorHelper.onAccountsUpdated(context, null);
        Resources resources = context.getResources();
        mRows.put(ID_SYNC, new ArrayObjectAdapter());
        mRows.put(ID_REMOVE_ACCOUNT, new ArrayObjectAdapter());

        loadCacheItems();
    }

    private void loadCacheItems() {
        Resources resources = mContext.getResources();
        mHeaderItems.clear();
        addBrowseHeader(ID_SYNC, resources.getString(R.string.account_header_sync));
        addBrowseHeader(ID_REMOVE_ACCOUNT,
                resources.getString(R.string.account_header_remove_account));

        updateMenuItems(mAccountName);
    }

    private void addBrowseHeader(int id, String title) {
        mHeaderItems.add(new HeaderItem(id, title));
    }

    private void updateMenuItems(String accountName) {
        Resources resources = mContext.getResources();

        // sync
        Intent intent = new Intent(mContext, AccountSyncSettings.class)
                .putExtra(AccountSettingsActivity.EXTRA_ACCOUNT, accountName);
        mRows.get(ID_SYNC).add(new MenuItem.Builder().id(mNextItemId++)
                .title(resources.getString(R.string.account_sync))
                .imageResourceId(mContext, R.drawable.ic_settings_sync)
                .intent(intent).build());

        // remove
        Intent removeIntent = new Intent(mContext, RemoveAccountDialog.class).putExtra(
                AccountSettingsActivity.EXTRA_ACCOUNT, accountName);
        mRows.get(ID_REMOVE_ACCOUNT).add(new MenuItem.Builder().id(mNextItemId++)
                .title(resources.getString(R.string.account_remove))
                .imageResourceId(mContext, R.drawable.ic_settings_remove)
                .intent(removeIntent)
                .build());
    }
}
