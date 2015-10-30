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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Bundle;

import java.io.IOException;

/**
 * Functionality tests for {@link DevicePolicyManager#setAccountManagementDisabled}
 *
 * Fire up a remote unprivileged service and attempt to add/remove/list
 * accounts from it to verify the enforcement is in place.
 *
 * This test depend on {@link MockAccountService}, which provides authenticator of type
 * {@link MockAccountService#ACCOUNT_TYPE}.
 */
public class AccountManagementTest extends BaseDeviceAdminTest {

    // Account type for MockAccountAuthenticator
    private final static String ACCOUNT_TYPE_1 = MockAccountAuthenticator.ACCOUNT_TYPE;
    private final static String ACCOUNT_TYPE_2 = "com.dummy.account";
    private final static Account ACCOUNT_0 = new Account("user0", ACCOUNT_TYPE_1);
    private final static Account ACCOUNT_1 = new Account("user1", ACCOUNT_TYPE_1);

    private AccountManager mAccountManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAccountManager = (AccountManager) mContext.getSystemService(Context.ACCOUNT_SERVICE);
        clearAllAccountManagementDisabled();
        AccountUtilsTest.removeAllAccountsForType(mAccountManager, ACCOUNT_TYPE_1);
    }

    @Override
    protected void tearDown() throws Exception {
        clearAllAccountManagementDisabled();
        AccountUtilsTest.removeAllAccountsForType(mAccountManager, ACCOUNT_TYPE_1);
        super.tearDown();
    }

    public void testAccountManagementDisabled_setterAndGetter() {
        // Some local tests: adding and removing disabled accounts and make sure
        // DevicePolicyManager keeps track of the disabled set correctly
        assertEquals(0, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);

        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_1,
                true);
        assertEquals(1, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);
        assertEquals(ACCOUNT_TYPE_1,
                mDevicePolicyManager.getAccountTypesWithManagementDisabled()[0]);

        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_1,
                false);
        assertEquals(0, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);
    }

    public void testAccountManagementDisabled_addAccount() throws AuthenticatorException,
            IOException, OperationCanceledException {
        // Test for restriction on addAccount()
        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_1,
                true);
        // Test if disabling ACCOUNT_TYPE_2 affects ACCOUNT_TYPE_1
        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_2,
                false);
        assertEquals(1, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);

        assertEquals(0, mAccountManager.getAccountsByType(ACCOUNT_TYPE_1).length);
        // Management is disabled, adding account should fail.
        try {
            mAccountManager.addAccount(ACCOUNT_TYPE_1, null, null, null, null, null, null)
                    .getResult();
            fail("Expected OperationCanceledException is not thrown.");
        } catch (OperationCanceledException e) {
            // Expected
        }
        assertEquals(0, mAccountManager.getAccountsByType(ACCOUNT_TYPE_1).length);

        // Management is re-enabled, adding account should succeed.
        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_1,
                false);
        assertEquals(0, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);
        Bundle result = mAccountManager.addAccount(ACCOUNT_TYPE_1,
                null, null, null, null, null, null).getResult();

        // Normally the expected result of addAccount() is AccountManager returning
        // an intent to start the authenticator activity for adding new accounts.
        // But MockAccountAuthenticator returns a new account straightway.
        assertEquals(ACCOUNT_TYPE_1, result.getString(AccountManager.KEY_ACCOUNT_TYPE));
    }

    public void testAccountManagementDisabled_removeAccount() throws AuthenticatorException,
            IOException, OperationCanceledException {
        // Test for restriction on removeAccount()
        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_1,
                true);
        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_2,
                false);
        assertEquals(1, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);

        assertEquals(0, mAccountManager.getAccountsByType(ACCOUNT_TYPE_1).length);
        // First prepare some accounts by manually adding them,
        // setAccountManagementDisabled(true) should not stop addAccountExplicitly().
        assertTrue(mAccountManager.addAccountExplicitly(ACCOUNT_0, "password", null));
        assertTrue(mAccountManager.addAccountExplicitly(ACCOUNT_1, "password", null));
        assertEquals(2, mAccountManager.getAccountsByType(ACCOUNT_TYPE_1).length);

        // Removing account should fail, as we just disabled it.
        try {
            mAccountManager.removeAccount(ACCOUNT_0, null, null).getResult();
            fail("Expected OperationCanceledException is not thrown.");
        } catch (OperationCanceledException e) {
            // Expected
        }
        assertEquals(2, mAccountManager.getAccountsByType(ACCOUNT_TYPE_1).length);

        // Re-enable management, so we can successfully remove account this time.
        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_1,
                false);
        assertEquals(0, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);
        assertTrue(mAccountManager.removeAccount(ACCOUNT_0, null, null).getResult());

        // Make sure the removal actually succeeds.
        Account[] accounts = mAccountManager.getAccountsByType(ACCOUNT_TYPE_1);
        assertEquals(1, accounts.length);
        assertEquals(ACCOUNT_1, accounts[0]);

        // Disable account type 2, we should still be able to remove from type 1.
        mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, ACCOUNT_TYPE_2,
                true);
        assertEquals(1, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);
        assertTrue(mAccountManager.removeAccount(ACCOUNT_1, null, null).getResult());

        // Make sure the removal actually succeeds.
        assertEquals(0, mAccountManager.getAccountsByType(ACCOUNT_TYPE_1).length);
    }

    private void clearAllAccountManagementDisabled() {
        for (String accountType : mDevicePolicyManager.getAccountTypesWithManagementDisabled()) {
            mDevicePolicyManager.setAccountManagementDisabled(ADMIN_RECEIVER_COMPONENT, accountType,
                    false);
        }
        assertEquals(0, mDevicePolicyManager.getAccountTypesWithManagementDisabled().length);
    }
}
