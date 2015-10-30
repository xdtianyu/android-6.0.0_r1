/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.exchange.service;

import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.exchange.Eas;
import com.android.mail.utils.LogUtils;

public class ContactsSyncAdapterService extends AbstractSyncAdapterService {
    private static final String TAG = Eas.LOG_TAG;
    private static final String ACCOUNT_AND_TYPE_CONTACTS =
        MailboxColumns.ACCOUNT_KEY + "=? AND " + MailboxColumns.TYPE + '=' + Mailbox.TYPE_CONTACTS;

    private static final Object sSyncAdapterLock = new Object();
    private static AbstractThreadedSyncAdapter sSyncAdapter = null;

    public ContactsSyncAdapterService() {
        super();
    }

    @Override
    protected AbstractThreadedSyncAdapter getSyncAdapter() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapterImpl(this);
            }
            return sSyncAdapter;
        }
    }

    private class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
        public SyncAdapterImpl(Context context) {
            super(context, true /* autoInitialize */);
        }

        @Override
        public void onPerformSync(android.accounts.Account acct, Bundle extras,
                String authority, ContentProviderClient provider, SyncResult syncResult) {
            if (LogUtils.isLoggable(TAG, Log.DEBUG)) {
                LogUtils.d(TAG, "onPerformSync contacts starting %s, %s", acct.toString(),
                        extras.toString());
            } else {
                LogUtils.i(TAG, "onPerformSync contacts starting %s", extras.toString());
            }
            if (!waitForService()) {
                // The service didn't connect, nothing we can do.
                return;
            }

            final Account emailAccount = Account.restoreAccountWithAddress(
                    ContactsSyncAdapterService.this, acct.name);
            if (emailAccount == null) {
                // There could be a timing issue with onPerformSync() being called and
                // the account being removed from our database.
                LogUtils.w(TAG,
                        "onPerformSync() - Could not find an Account, skipping contacts sync.");
                return;
            }

            // TODO: is this still needed?
            // If we've been asked to do an upload, make sure we've got work to do
            if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD)) {
                Uri uri = RawContacts.CONTENT_URI.buildUpon()
                        .appendQueryParameter(RawContacts.ACCOUNT_NAME, acct.name)
                        .appendQueryParameter(RawContacts.ACCOUNT_TYPE,
                                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE)
                        .build();
                // See if we've got dirty contacts or dirty groups containing our contacts
                boolean changed = hasDirtyRows(getContentResolver(), uri, RawContacts.DIRTY);
                if (!changed) {
                    uri = Groups.CONTENT_URI.buildUpon()
                            .appendQueryParameter(RawContacts.ACCOUNT_NAME, acct.name)
                            .appendQueryParameter(RawContacts.ACCOUNT_TYPE,
                                    Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE)
                            .build();
                    changed = hasDirtyRows(getContentResolver(), uri, Groups.DIRTY);
                }
                if (!changed) {
                    LogUtils.d(TAG, "Upload sync; no changes");
                    return;
                }
            }

            // TODO: move this to some common place.
            // Push only means this sync request should only refresh the ping (either because
            // settings changed, or we need to restart it for some reason).
            final boolean pushOnly = Mailbox.isPushOnlyExtras(extras);

            if (pushOnly) {
                LogUtils.d(TAG, "onPerformSync email: mailbox push only");
                if (mEasService != null) {
                    try {
                        mEasService.pushModify(emailAccount.mId);
                        return;
                    } catch (final RemoteException re) {
                        LogUtils.e(TAG, re, "While trying to pushModify within onPerformSync");
                        // TODO: how to handle this?
                    }
                }
                return;
            } else {
                try {
                    final int result = mEasService.sync(emailAccount.mId, extras);
                    writeResultToSyncResult(result, syncResult);
                    if (syncResult.stats.numAuthExceptions > 0 &&
                            result != EmailServiceStatus.PROVISIONING_ERROR) {
                        showAuthNotification(emailAccount.mId, emailAccount.mEmailAddress);
                    }
                } catch (RemoteException e) {
                    LogUtils.e(TAG, e, "While trying to pushModify within onPerformSync");
                }
            }

            LogUtils.d(TAG, "onPerformSync contacts: finished");
        }
    }

    private static boolean hasDirtyRows(ContentResolver resolver, Uri uri, String dirtyColumn) {
        Cursor c = resolver.query(uri, EmailContent.ID_PROJECTION, dirtyColumn + "=1", null, null);
        if (c == null) {
            return false;
        }
        try {
            return c.getCount() > 0;
        } finally {
            c.close();
        }
    }
}
