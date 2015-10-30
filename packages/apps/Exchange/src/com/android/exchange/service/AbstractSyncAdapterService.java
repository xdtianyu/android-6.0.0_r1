/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.text.format.DateUtils;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.emailcommon.service.IEmailService;
import com.android.emailcommon.utility.IntentUtilities;
import com.android.exchange.R;
import com.android.mail.utils.LogUtils;

/**
 * Base class for services that handle sync requests from the system SyncManager.
 * This class covers the boilerplate for using an {@link AbstractThreadedSyncAdapter}. Subclasses
 * should just implement their sync adapter, and override {@link #getSyncAdapter}.
 */
public abstract class AbstractSyncAdapterService extends Service {
    private static final String TAG = LogUtils.TAG;

    // The call to ServiceConnection.onServiceConnected is asynchronous to bindService. It's
    // possible for that to be delayed if, in which case, a call to onPerformSync
    // could occur before we have a connection to the service.
    // In onPerformSync, if we don't yet have our EasService, we will wait for up to 10
    // seconds for it to appear. If it takes longer than that, we will fail the sync.
    private static final long MAX_WAIT_FOR_SERVICE_MS = 10 * DateUtils.SECOND_IN_MILLIS;

    public AbstractSyncAdapterService() {
        super();
    }

    protected IEmailService mEasService;
    protected ServiceConnection mConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        // Make sure EmailContent is initialized in Exchange app
        EmailContent.init(this);
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name,  IBinder binder) {
                LogUtils.v(TAG, "onServiceConnected");
                synchronized (mConnection) {
                    mEasService = IEmailService.Stub.asInterface(binder);
                    mConnection.notify();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mEasService = null;
            }
        };
        bindService(new Intent(this, EasService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return getSyncAdapter().getSyncAdapterBinder();
    }

    /**
     * Subclasses should override this to supply an instance of its sync adapter. Best practice is
     * to create a singleton and return that.
     * @return An instance of the sync adapter.
     */
    protected abstract AbstractThreadedSyncAdapter getSyncAdapter();

    /**
     * Create and return an intent to display (and edit) settings for a specific account, or -1
     * for any/all accounts.  If an account name string is provided, a warning dialog will be
     * displayed as well.
     */
    public static Intent createAccountSettingsIntent(long accountId, String accountName) {
        final Uri.Builder builder = IntentUtilities.createActivityIntentUrlBuilder(
                IntentUtilities.PATH_SETTINGS);
        IntentUtilities.setAccountId(builder, accountId);
        IntentUtilities.setAccountName(builder, accountName);
        return new Intent(Intent.ACTION_EDIT, builder.build());
    }

    protected void showAuthNotification(long accountId, String accountName) {
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                createAccountSettingsIntent(accountId, accountName),
                0);

        final Notification notification = new Notification.Builder(this)
                .setContentTitle(this.getString(R.string.auth_error_notification_title))
                .setContentText(this.getString(
                        R.string.auth_error_notification_text, accountName))
                .setSmallIcon(R.drawable.stat_notify_auth)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .getNotification();

        final NotificationManager nm = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify("AuthError", 0, notification);
    }

    /**
     * Interpret a result code from an {@link IEmailService.sync()} and, if it's an error, write
     * it to the appropriate field in {@link android.content.SyncResult}.
     * @param result
     * @param syncResult
     * @return Whether an error code was written to syncResult.
     */
    public static boolean writeResultToSyncResult(final int result, final SyncResult syncResult) {
        switch (result) {
            case EmailServiceStatus.SUCCESS:
                return false;

            case EmailServiceStatus.REMOTE_EXCEPTION:
            case EmailServiceStatus.LOGIN_FAILED:
            case EmailServiceStatus.SECURITY_FAILURE:
            case EmailServiceStatus.CLIENT_CERTIFICATE_ERROR:
            case EmailServiceStatus.ACCESS_DENIED:
                syncResult.stats.numAuthExceptions = 1;
                return true;

            case EmailServiceStatus.HARD_DATA_ERROR:
            case EmailServiceStatus.INTERNAL_ERROR:
                syncResult.databaseError = true;
                return true;

            case EmailServiceStatus.CONNECTION_ERROR:
            case EmailServiceStatus.IO_ERROR:
                syncResult.stats.numIoExceptions = 1;
                return true;

            case EmailServiceStatus.TOO_MANY_REDIRECTS:
                syncResult.tooManyRetries = true;
                return true;

            case EmailServiceStatus.IN_PROGRESS:
            case EmailServiceStatus.MESSAGE_NOT_FOUND:
            case EmailServiceStatus.ATTACHMENT_NOT_FOUND:
            case EmailServiceStatus.FOLDER_NOT_DELETED:
            case EmailServiceStatus.FOLDER_NOT_RENAMED:
            case EmailServiceStatus.FOLDER_NOT_CREATED:
            case EmailServiceStatus.ACCOUNT_UNINITIALIZED:
            case EmailServiceStatus.PROTOCOL_ERROR:
                LogUtils.e(TAG, "Unexpected sync result %d", result);
                return false;
        }
        return false;
    }

    protected final boolean waitForService() {
        synchronized(mConnection) {
            if (mEasService == null) {
                LogUtils.d(TAG, "service not yet connected");
                try {
                    mConnection.wait(MAX_WAIT_FOR_SERVICE_MS);
                } catch (InterruptedException e) {
                    LogUtils.wtf(TAG, "InterrupedException waiting for EasService to connect");
                    return false;
                }
                if (mEasService == null) {
                    LogUtils.wtf(TAG, "timed out waiting for EasService to connect");
                    return false;
                }
            }
        }
        return true;
    }

    protected final Account getAccountFromAndroidAccount(final android.accounts.Account acct) {
        final Account emailAccount;
        emailAccount = Account.restoreAccountWithAddress(this, acct.name);
        return emailAccount;
    }

}

