/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.exchange.eas;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.format.DateUtils;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.CommandStatusException.CommandStatus;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.PingParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.mail.utils.LogUtils;

import org.apache.http.HttpEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Performs an Exchange Ping, which is the command for receiving push notifications.
 * See http://msdn.microsoft.com/en-us/library/ee200913(v=exchg.80).aspx for more details.
 */
public class EasPing extends EasOperation {
    private static final String TAG = Eas.LOG_TAG;

    private static final String WHERE_ACCOUNT_KEY_AND_SERVER_ID =
            MailboxColumns.ACCOUNT_KEY + "=? and " + MailboxColumns.SERVER_ID + "=?";

    private final android.accounts.Account mAmAccount;
    private long mPingDuration;

    /**
     * The default heartbeat interval specified to the Exchange server. This is the maximum amount
     * of time (in seconds) that the server should wait before responding to the ping request.
     */
    private static final long DEFAULT_PING_HEARTBEAT =
            8 * (DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);

    /**
     * The minimum heartbeat interval we should ever use, in seconds.
     */
    private static final long MINIMUM_PING_HEARTBEAT =
            8 * (DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);

    /**
     * The maximum heartbeat interval we should ever use, in seconds.
     */
    private static final long MAXIMUM_PING_HEARTBEAT =
            28 * (DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);

    /**
     * The maximum amount that we can change with each adjustment, in seconds.
     */
    private static final long MAXIMUM_HEARTBEAT_INCREMENT =
            5 * (DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);

    /**
     * The extra time for the timeout used for the HTTP POST (in milliseconds). Notionally this
     * should be the same as ping heartbeat but in practice is a few seconds longer to allow for
     * latency in the server's response.
     */
    private static final long EXTRA_POST_TIMEOUT_MILLIS = 5 * DateUtils.SECOND_IN_MILLIS;

    public EasPing(final Context context, final Account account,
            final android.accounts.Account amAccount) {
        super(context, account);
        mAmAccount = amAccount;
        mPingDuration = account.mPingDuration;
        if (mPingDuration == 0) {
            mPingDuration = DEFAULT_PING_HEARTBEAT;
        }
        LogUtils.d(TAG, "initial ping duration " + mPingDuration + " account " + getAccountId());
    }

    public final int doPing() {
        final long startTime = SystemClock.elapsedRealtime();
        final int result = performOperation();
        if (result == RESULT_RESTART) {
            return PingParser.STATUS_EXPIRED;
        } else  if (result == RESULT_NETWORK_PROBLEM) {
            final long timeoutDuration = SystemClock.elapsedRealtime() - startTime;
            LogUtils.d(TAG, "doPing request failure, timed out after %d millis", timeoutDuration);
            decreasePingDuration();
        }
        return result;
    }

    private void decreasePingDuration() {
        mPingDuration = Math.max(MINIMUM_PING_HEARTBEAT,
                mPingDuration - MAXIMUM_HEARTBEAT_INCREMENT);
        LogUtils.d(TAG, "decreasePingDuration adjusting by " + MAXIMUM_HEARTBEAT_INCREMENT +
                " new duration " + mPingDuration + " account " + getAccountId());
        storePingDuration();
    }

    private void increasePingDuration() {
        mPingDuration = Math.min(MAXIMUM_PING_HEARTBEAT,
                mPingDuration + MAXIMUM_HEARTBEAT_INCREMENT);
        LogUtils.d(TAG, "increasePingDuration adjusting by " + MAXIMUM_HEARTBEAT_INCREMENT +
                " new duration " + mPingDuration + " account " + getAccountId());
        storePingDuration();
    }

    private void storePingDuration() {
        final ContentValues values = new ContentValues(1);
        values.put(AccountColumns.PING_DURATION, mPingDuration);
        Account.update(mContext, Account.CONTENT_URI, getAccountId(), values);
    }

    public final android.accounts.Account getAmAccount() {
        return mAmAccount;
    }

    @Override
    protected String getCommand() {
        return "Ping";
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        // Get the mailboxes that need push notifications.
        final Cursor c = Mailbox.getMailboxesForPush(mContext.getContentResolver(),
                getAccountId());
        if (c == null) {
            throw new IllegalStateException("Could not read mailboxes");
        }

        // TODO: Ideally we never even get here unless we already know we want a push.
        Serializer s = null;
        try {
            while (c.moveToNext()) {
                final Mailbox mailbox = new Mailbox();
                mailbox.restore(c);
                s = handleOneMailbox(s, mailbox);
            }
        } finally {
            c.close();
        }

        if (s == null) {
            abort();
            throw new IOException("No mailboxes want push");
        }
        // This sequence of end()s corresponds to the start()s that occur in handleOneMailbox when
        // the Serializer is first created. If either side changes, the other must be kept in sync.
        s.end().end().done();
        return makeEntity(s);
    }

    @Override
    protected int handleResponse(final EasResponse response) throws IOException {
        if (response.isEmpty()) {
            // TODO this should probably not be an IOException, maybe something more descriptive?
            throw new IOException("Empty ping response");
        }

        LogUtils.d(TAG, "EasPing.handleResponse");

        // Handle a valid response.
        final PingParser pp = new PingParser(response.getInputStream());
        pp.parse();
        final int pingStatus = pp.getPingStatus();

        // Take the appropriate action for this response.
        // Many of the responses require no explicit action here, they just influence
        // our re-ping behavior, which is handled by the caller.
        final long accountId = getAccountId();
        switch (pingStatus) {
            case PingParser.STATUS_EXPIRED:
                LogUtils.i(TAG, "Ping expired for account %d", accountId);
                // On successful expiration, we can increase our ping duration
                increasePingDuration();
                break;
            case PingParser.STATUS_CHANGES_FOUND:
                LogUtils.i(TAG, "Ping found changed folders for account %d", accountId);
                requestSyncForSyncList(pp.getSyncList());
                break;
            case PingParser.STATUS_REQUEST_INCOMPLETE:
            case PingParser.STATUS_REQUEST_MALFORMED:
                // These two cases indicate that the ping request was somehow bad.
                // TODO: It's insanity to re-ping with the same data and expect a different
                // result. Improve this if possible.
                LogUtils.e(TAG, "Bad ping request for account %d", accountId);
                break;
            case PingParser.STATUS_REQUEST_HEARTBEAT_OUT_OF_BOUNDS:
                long newDuration = pp.getHeartbeatInterval();
                LogUtils.i(TAG, "Heartbeat out of bounds for account %d, " +
                        "old duration %d new duration %d", accountId, mPingDuration, newDuration);
                mPingDuration = newDuration;
                storePingDuration();
                break;
            case PingParser.STATUS_REQUEST_TOO_MANY_FOLDERS:
                LogUtils.i(TAG, "Too many folders for account %d", accountId);
                break;
            case PingParser.STATUS_FOLDER_REFRESH_NEEDED:
                LogUtils.i(TAG, "FolderSync needed for account %d", accountId);
                requestFolderSync();
                break;
            case PingParser.STATUS_SERVER_ERROR:
                LogUtils.i(TAG, "Server error for account %d", accountId);
                break;
            case CommandStatus.SERVER_ERROR_RETRY:
                // Try again later.
                LogUtils.i(TAG, "Retryable server error for account %d", accountId);
                return RESULT_RESTART;

            // These errors should not happen.
            case CommandStatus.USER_DISABLED_FOR_SYNC:
            case CommandStatus.USERS_DISABLED_FOR_SYNC:
            case CommandStatus.USER_ON_LEGACY_SERVER_CANT_SYNC:
            case CommandStatus.DEVICE_QUARANTINED:
            case CommandStatus.ACCESS_DENIED:
            case CommandStatus.USER_ACCOUNT_DISABLED:
            case CommandStatus.NOT_PROVISIONABLE_PARTIAL:
            case CommandStatus.NOT_PROVISIONABLE_LEGACY_DEVICE:
            case CommandStatus.TOO_MANY_PARTNERSHIPS:
                LogUtils.e(TAG, "Unexpected error %d on ping", pingStatus);
                return RESULT_AUTHENTICATION_ERROR;

            // These errors should not happen.
            case CommandStatus.SYNC_STATE_NOT_FOUND:
            case CommandStatus.SYNC_STATE_LOCKED:
            case CommandStatus.SYNC_STATE_CORRUPT:
            case CommandStatus.SYNC_STATE_EXISTS:
            case CommandStatus.SYNC_STATE_INVALID:
            case CommandStatus.NEEDS_PROVISIONING_WIPE:
            case CommandStatus.NEEDS_PROVISIONING:
            case CommandStatus.NEEDS_PROVISIONING_REFRESH:
            case CommandStatus.NEEDS_PROVISIONING_INVALID:
            case CommandStatus.WTF_INVALID_COMMAND:
            case CommandStatus.WTF_INVALID_PROTOCOL:
            case CommandStatus.WTF_DEVICE_CLAIMS_EXTERNAL_MANAGEMENT:
            case CommandStatus.WTF_UNKNOWN_ITEM_TYPE:
            case CommandStatus.WTF_REQUIRES_PROXY_WITHOUT_SSL:
            case CommandStatus.ITEM_NOT_FOUND:
                LogUtils.e(TAG, "Unexpected error %d on ping", pingStatus);
                return RESULT_OTHER_FAILURE;

            default:
                break;
        }

        return pingStatus;
    }


    @Override
    protected boolean addPolicyKeyHeaderToRequest() {
        return false;
    }

    @Override
    protected long getTimeout() {
        return mPingDuration * DateUtils.SECOND_IN_MILLIS + EXTRA_POST_TIMEOUT_MILLIS;
    }

    /**
     * If mailbox is eligible for push, add it to the ping request, creating the {@link Serializer}
     * for the request if necessary.
     * @param mailbox The mailbox to check.
     * @param s The {@link Serializer} for this request, or null if it hasn't been created yet.
     * @return The {@link Serializer} for this request, or null if it hasn't been created yet.
     * @throws IOException
     */
    private Serializer handleOneMailbox(Serializer s, final Mailbox mailbox) throws IOException {
        // We can't push until the initial sync is done
        if (mailbox.mSyncKey != null && !mailbox.mSyncKey.equals("0")) {
            if (ContentResolver.getSyncAutomatically(mAmAccount,
                    Mailbox.getAuthority(mailbox.mType))) {
                if (s == null) {
                    // No serializer yet, so create and initialize it.
                    // Note that these start()s correspond to the end()s in doInBackground.
                    // If either side changes, the other must be kept in sync.
                    s = new Serializer();
                    s.start(Tags.PING_PING);
                    s.data(Tags.PING_HEARTBEAT_INTERVAL, Long.toString(mPingDuration));
                    s.start(Tags.PING_FOLDERS);
                }
                s.start(Tags.PING_FOLDER);
                s.data(Tags.PING_ID, mailbox.mServerId);
                s.data(Tags.PING_CLASS, Eas.getFolderClass(mailbox.mType));
                s.end();
            }
        }
        return s;
    }

    /**
     * Make the appropriate calls to {@link ContentResolver#requestSync} indicated by the
     * current ping response.
     * @param syncList The list of folders that need to be synced.
     */
    private void requestSyncForSyncList(final ArrayList<String> syncList) {
        final String[] bindArguments = new String[2];
        bindArguments[0] = Long.toString(getAccountId());

        final ArrayList<Long> emailMailboxIds = new ArrayList<Long>();
        final ArrayList<Long> calendarMailboxIds = new ArrayList<Long>();
        final ArrayList<Long> contactsMailboxIds = new ArrayList<Long>();

        for (final String serverId : syncList) {
            bindArguments[1] = serverId;
            // TODO: Rather than one query per ping mailbox, do it all in one?
            final Cursor c = mContext.getContentResolver().query(Mailbox.CONTENT_URI,
                    Mailbox.CONTENT_PROJECTION, WHERE_ACCOUNT_KEY_AND_SERVER_ID,
                    bindArguments, null);
            if (c == null) {
                // TODO: proper error handling.
                break;
            }
            try {
                /**
                 * Check the boxes reporting changes to see if there really were any...
                 * We do this because bugs in various Exchange servers can put us into a
                 * looping behavior by continually reporting changes in a mailbox, even
                 * when there aren't any.
                 *
                 * This behavior is seemingly random, and therefore we must code
                 * defensively by backing off of push behavior when it is detected.
                 *
                 * One known cause, on certain Exchange 2003 servers, is acknowledged by
                 * Microsoft, and the server hotfix for this case can be found at
                 * http://support.microsoft.com/kb/923282
                 */
                // TODO: Implement the above
                if (c.moveToFirst()) {
                    final long mailboxId = c.getLong(Mailbox.CONTENT_ID_COLUMN);
                    final int contentType = c.getInt(Mailbox.CONTENT_TYPE_COLUMN);
                    switch (contentType) {
                        case Mailbox.TYPE_MAIL:
                        case Mailbox.TYPE_INBOX:
                        case Mailbox.TYPE_DRAFTS:
                        case Mailbox.TYPE_SENT:
                        case Mailbox.TYPE_TRASH:
                        case Mailbox.TYPE_JUNK:
                            emailMailboxIds.add(mailboxId);
                        case Mailbox.TYPE_CALENDAR:
                            calendarMailboxIds.add(mailboxId);
                        case Mailbox.TYPE_CONTACTS:
                            contactsMailboxIds.add(mailboxId);
                        default:
                            LogUtils.e(LOG_TAG, "unexpected collectiontype %d in EasPing",
                                    contentType);
                    }
                }
            } finally {
                c.close();
            }
        }
        requestSyncForMailboxes(mAmAccount, EmailContent.AUTHORITY, emailMailboxIds);
        requestSyncForMailboxes(mAmAccount, CalendarContract.AUTHORITY, calendarMailboxIds);
        requestSyncForMailboxes(mAmAccount, ContactsContract.AUTHORITY, contactsMailboxIds);
    }

    /**
     * Issue a {@link ContentResolver#requestSync} to trigger a FolderSync for an account.
     */
    private void requestFolderSync() {
        final Bundle extras = new Bundle(1);
        extras.putBoolean(Mailbox.SYNC_EXTRA_ACCOUNT_ONLY, true);
        ContentResolver.requestSync(mAmAccount, EmailContent.AUTHORITY, extras);
        LogUtils.i(LOG_TAG, "requestFolderSync EasPing %s, %s",
                mAmAccount.toString(), extras.toString());
    }

    /**
     * Request a ping-only sync via the SyncManager. This is used in error paths, which is also why
     * we don't just create and start a new ping task immediately: in the case where we have loss
     * of network, we want to take advantage of the SyncManager to schedule this when we expect it
     * to be able to work.
     * @param amAccount Account that needs to ping.
     */
    public static void requestPing(final android.accounts.Account amAccount) {
        final Bundle extras = new Bundle(2);
        extras.putBoolean(Mailbox.SYNC_EXTRA_PUSH_ONLY, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(amAccount, EmailContent.AUTHORITY, extras);
        LogUtils.i(LOG_TAG, "requestPing EasOperation %s, %s",
                amAccount.toString(), extras.toString());
    }

}
