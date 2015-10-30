// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.exchange;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;

import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.R;
import com.android.mail.utils.LogUtils;

public class ExchangeBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final Account[] accounts = AccountManager.get(context)
                .getAccountsByType(context.getString(R.string.account_manager_type_exchange));
        LogUtils.i(Eas.LOG_TAG, "Accounts changed - requesting FolderSync for unsynced accounts");
        for (final Account account : accounts) {
            // Only do a sync for accounts that are not configured to sync any types, since the
            // initial sync will do the right thing if at least one of those is enabled.
            if (!ContentResolver.getSyncAutomatically(account, EmailContent.AUTHORITY) &&
                    !ContentResolver.getSyncAutomatically(account, CalendarContract.AUTHORITY) &&
                    !ContentResolver.getSyncAutomatically(account, ContactsContract.AUTHORITY)) {
                final Bundle bundle = new Bundle(3);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, true);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                bundle.putBoolean(Mailbox.SYNC_EXTRA_ACCOUNT_ONLY, true);
                ContentResolver.requestSync(account, EmailContent.AUTHORITY, bundle);
            }
        }
    }
}
