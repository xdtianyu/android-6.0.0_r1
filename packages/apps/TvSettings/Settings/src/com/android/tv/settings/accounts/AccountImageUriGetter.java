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

import com.android.tv.settings.MenuItem;
import com.android.tv.settings.util.UriUtils;
import com.android.tv.settings.util.AccountImageHelper;

import android.accounts.Account;
import android.content.Context;

import android.net.Uri;

/**
 * Gets a URI to the account picture.
 */
public class AccountImageUriGetter implements MenuItem.UriGetter {

    private final Context mContext;
    private final Account mAccount;
    private final Uri mNotifyUri;
    private String mIconUri;

    public AccountImageUriGetter(Context context, Account account) {
        this(context, account, null);
    }

    public AccountImageUriGetter(Context context, Account account, Uri changeNotifyUri) {
        mContext = context;
        mAccount = account;
        mNotifyUri = changeNotifyUri;
    }

    @Override
    public String getUri() {
        if (mIconUri != null) {
            return mIconUri;
        }

        if (mAccount != null) {
            mIconUri = UriUtils.getAccountImageUri(mAccount.name, mNotifyUri).toString();
            if (mIconUri != null) {
                return mIconUri;
            }
        }

        // Return this but don't save into mIconUri, requery later.
        return AccountImageHelper.getDefaultPictureUri(mContext);
    }
}
