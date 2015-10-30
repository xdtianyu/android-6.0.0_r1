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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import com.android.tv.settings.BrowseInfoFactory;
import com.android.tv.settings.MenuActivity;
import com.android.tv.settings.util.UriUtils;
import com.android.tv.settings.widget.BitmapDownloader;
import com.android.tv.settings.widget.BitmapDownloader.BitmapCallback;
import com.android.tv.settings.widget.BitmapWorkerOptions;

public class AccountSettingsActivity extends MenuActivity {

    public static final String EXTRA_ACCOUNT = "account_name";

    private Drawable mAccountDrawable;

    @Override
    protected String getBrowseTitle() {
        return getIntent().getStringExtra(EXTRA_ACCOUNT);
    }

    @Override
    protected Drawable getBadgeImage() {
        return mAccountDrawable;
    }

    @Override
    protected BrowseInfoFactory getBrowseInfoFactory() {
        return new AccountsBrowseInfo(this, getIntent().getStringExtra(EXTRA_ACCOUNT));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri imageUri = UriUtils.getAccountImageUri(getIntent().getStringExtra(EXTRA_ACCOUNT), null);
        BitmapCallback bitmapCallBack = new BitmapCallback() {
            @Override
            public void onBitmapRetrieved(Bitmap bitmap) {
                if (bitmap != null) {
                    mAccountDrawable = new BitmapDrawable(getResources(), bitmap);
                    updateBrowseParams();
                }
            }
        };

        BitmapWorkerOptions bitmapWorkerOptions = new BitmapWorkerOptions.Builder(this)
                .resource(imageUri).build();
        BitmapDownloader.getInstance(this).getBitmap(bitmapWorkerOptions, bitmapCallBack);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String accountName = getIntent().getStringExtra(EXTRA_ACCOUNT);
        for (Account account : AccountManager.get(this).getAccounts()) {
            if (account.name.equals(accountName)) {
                return;
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

}
