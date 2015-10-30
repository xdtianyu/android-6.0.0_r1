/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.cts.deviceandprofileowner;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.Bundle;
import android.test.AndroidTestCase;

/**
 * Utility class that allows adding and removing accounts.
 *
 * This test depend on {@link MockAccountService}, which provides authenticator of type
 * {@link MockAccountService#ACCOUNT_TYPE}.
 */
public class AccountUtilsTest extends AndroidTestCase {

    private final static Account ACCOUNT_0 = new Account("user0",
            MockAccountAuthenticator.ACCOUNT_TYPE);
    private AccountManager mAccountManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAccountManager = (AccountManager) getContext().getSystemService(Context.ACCOUNT_SERVICE);
    }

    public void testAddAccount() throws Exception {
        assertEquals(0, mAccountManager.getAccountsByType(MockAccountAuthenticator.ACCOUNT_TYPE)
                .length);
        assertTrue(mAccountManager.addAccountExplicitly(ACCOUNT_0, "password", null));
        assertEquals(1, mAccountManager.getAccountsByType(MockAccountAuthenticator.ACCOUNT_TYPE)
                .length);
    }

    public void testRemoveAccounts() throws Exception {
        removeAllAccountsForType(mAccountManager, MockAccountAuthenticator.ACCOUNT_TYPE);
    }

    static void removeAllAccountsForType(AccountManager am, String accountType)
            throws Exception {
        Account[] accounts = am.getAccountsByType(accountType);
        for (Account account : accounts) {
            AccountManagerFuture<Boolean> result = am.removeAccount(account, null, null);
            assertTrue(result.getResult());
        }
        assertEquals(0, am.getAccountsByType(accountType).length);
    }
}
