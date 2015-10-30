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

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;
import com.android.tv.settings.widget.SettingsToast;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * OK / Cancel dialog.
 */
public class RemoveAccountDialog extends DialogActivity implements AccountManagerCallback<Boolean> {

    private static final String TAG = "RemoveAccountDialog";

    private static final String KEY_OK = "ok";
    private static final String KEY_CANCEL = "cancel";
    private String mAccountName;
    private boolean mIsRemoving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountName = getIntent().getStringExtra(AccountSettingsActivity.EXTRA_ACCOUNT);
        setContentAndActionFragments(ContentFragment.newInstance(
                getString(R.string.account_remove), mAccountName, "",
                R.drawable.ic_settings_remove, getResources().getColor(R.color.icon_background)),
            ActionFragment.newInstance(getActions()));
    }

    @Override
    public void onActionClicked(Action action) {
        if (KEY_OK.equals(action.getKey())) {
            if (ActivityManager.isUserAMonkey()) {
                // Don't let the monkey remove accounts.
                finish();
                return;
            }
            // Block this from happening more than once.
            if (mIsRemoving) {
                return;
            }
            mIsRemoving = true;
            AccountManager manager = AccountManager.get(getApplicationContext());
            Account account = null;
            for (Account accountLoop : manager.getAccounts()) {
                if (accountLoop.name.equals(mAccountName)) {
                    account = accountLoop;
                    break;
                }
            }
            manager.removeAccount(account, this, new Handler());
        } else {
            finish();
        }
    }

    private ArrayList<Action> getActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action.Builder()
            .key(KEY_CANCEL)
            .title(getString(R.string.settings_cancel))
            .build());
        actions.add(new Action.Builder()
            .key(KEY_OK)
            .title(getString(R.string.settings_ok))
            .build());
        return actions;
    }


    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (!isResumed()) {
            return;
        }
        try {
            if (!future.getResult()) {
                // Wasn't removed, toast this.
                SettingsToast.makeText(this, R.string.account_remove_failed,
                        SettingsToast.LENGTH_LONG)
                        .show();
            }
        } catch (OperationCanceledException e) {
            Log.e(TAG, "Could not remove", e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Could not remove", e);
        } catch (IOException e) {
            Log.e(TAG, "Could not remove", e);
        }
        finish();
    }
}
