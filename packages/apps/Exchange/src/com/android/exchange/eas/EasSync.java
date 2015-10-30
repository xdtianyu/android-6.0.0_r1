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
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.MessageStateChange;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.EmailSyncParser;
import com.android.exchange.adapter.Parser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.mail.utils.LogUtils;

import org.apache.http.HttpEntity;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Performs an Exchange Sync operation for one {@link Mailbox}.
 * TODO: For now, only handles upsync.
 * TODO: Handle multiple folders in one request. Not sure if parser can handle it yet.
 */
public class EasSync extends EasOperation {

    /** Result code indicating that the mailbox for an upsync is no longer present. */
    public final static int RESULT_NO_MAILBOX = 0;
    public final static int RESULT_OK = 1;

    // TODO: When we handle downsync, this will become relevant.
    private boolean mInitialSync;

    // State for the mailbox we're currently syncing.
    private long mMailboxId;
    private String mMailboxServerId;
    private String mMailboxSyncKey;
    private List<MessageStateChange> mStateChanges;
    private Map<String, Integer> mMessageUpdateStatus;

    public EasSync(final Context context, final Account account) {
        super(context, account);
        mInitialSync = false;
    }

    private long getMessageId(final String serverId) {
        // TODO: Improve this.
        for (final MessageStateChange change : mStateChanges) {
            if (change.getServerId().equals(serverId)) {
                return change.getMessageId();
            }
        }
        return EmailContent.Message.NO_MESSAGE;
    }

    private void handleMessageUpdateStatus(final Map<String, Integer> messageStatus,
            final long[][] messageIds, final int[] counts) {
        for (final Map.Entry<String, Integer> entry : messageStatus.entrySet()) {
            final String serverId = entry.getKey();
            final int status = entry.getValue();
            final int index;
            if (EmailSyncParser.shouldRetry(status)) {
                index = 1;
            } else {
                index = 0;
            }
            final long messageId = getMessageId(serverId);
            if (messageId != EmailContent.Message.NO_MESSAGE) {
                messageIds[index][counts[index]] = messageId;
                ++counts[index];
            }
        }
    }

    /**
     * @return Number of messages successfully synced, or a negative response code from
     *         {@link EasOperation} if we encountered any errors.
     */
    public final int upsync() {
        final List<MessageStateChange> changes = MessageStateChange.getChanges(mContext,
                getAccountId(), getProtocolVersion() < Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE);
        if (changes == null) {
            return 0;
        }
        final LongSparseArray<List<MessageStateChange>> allData =
                MessageStateChange.convertToChangesMap(changes);
        if (allData == null) {
            return 0;
        }

        final long[][] messageIds = new long[2][changes.size()];
        final int[] counts = new int[2];
        int result = 0;

        for (int i = 0; i < allData.size(); ++i) {
            mMailboxId = allData.keyAt(i);
            mStateChanges = allData.valueAt(i);
            boolean retryMailbox = true;
            // If we've already encountered a fatal error, don't even try to upsync subsequent
            // mailboxes.
            if (result >= 0) {
                final Cursor mailboxCursor = mContext.getContentResolver().query(
                        ContentUris.withAppendedId(Mailbox.CONTENT_URI, mMailboxId),
                        Mailbox.ProjectionSyncData.PROJECTION, null, null, null);
                if (mailboxCursor != null) {
                    try {
                        if (mailboxCursor.moveToFirst()) {
                            mMailboxServerId = mailboxCursor.getString(
                                    Mailbox.ProjectionSyncData.COLUMN_SERVER_ID);
                            mMailboxSyncKey = mailboxCursor.getString(
                                    Mailbox.ProjectionSyncData.COLUMN_SYNC_KEY);
                            if (TextUtils.isEmpty(mMailboxSyncKey) || mMailboxSyncKey.equals("0")) {
                                // For some reason we can get here without a valid mailbox sync key
                                // b/10797675
                                // TODO: figure out why and clean this up
                                LogUtils.d(LOG_TAG,
                                        "Tried to sync mailbox %d with invalid mailbox sync key",
                                        mMailboxId);
                            } else {
                                result = performOperation();
                                if (result >= 0) {
                                    // Our request gave us back a legitimate answer; this is the
                                    // only case in which we don't retry this mailbox.
                                    retryMailbox = false;
                                    if (result == RESULT_OK) {
                                        handleMessageUpdateStatus(mMessageUpdateStatus, messageIds,
                                                counts);
                                    } else if (result == RESULT_NO_MAILBOX) {
                                        // A retry here is pointless -- the message's mailbox (and
                                        // therefore the message) is gone, so mark as success so
                                        // that these entries get wiped from the change list.
                                        for (final MessageStateChange msc : mStateChanges) {
                                            messageIds[0][counts[0]] = msc.getMessageId();
                                            ++counts[0];
                                        }
                                    } else {
                                        LogUtils.wtf(LOG_TAG, "Unrecognized result code: %d",
                                                result);
                                    }
                                }
                            }
                        }
                    } finally {
                        mailboxCursor.close();
                    }
                }
            }
            if (retryMailbox) {
                for (final MessageStateChange msc : mStateChanges) {
                    messageIds[1][counts[1]] = msc.getMessageId();
                    ++counts[1];
                }
            }
        }

        final ContentResolver cr = mContext.getContentResolver();
        MessageStateChange.upsyncSuccessful(cr, messageIds[0], counts[0]);
        MessageStateChange.upsyncRetry(cr, messageIds[1], counts[1]);

        if (result < 0) {
            return result;
        }
        return counts[0];
    }

    @Override
    protected String getCommand() {
        return "Sync";
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        final Serializer s = new Serializer();
        s.start(Tags.SYNC_SYNC);
        s.start(Tags.SYNC_COLLECTIONS);
        addOneCollectionToRequest(s, Mailbox.TYPE_MAIL, mMailboxServerId, mMailboxSyncKey,
                mStateChanges);
        s.end().end().done();
        return makeEntity(s);
    }

    @Override
    protected int handleResponse(final EasResponse response)
            throws IOException, CommandStatusException {
        final Mailbox mailbox = Mailbox.restoreMailboxWithId(mContext, mMailboxId);
        if (mailbox == null) {
            return RESULT_NO_MAILBOX;
        }
        final EmailSyncParser parser = new EmailSyncParser(mContext, mContext.getContentResolver(),
                response.getInputStream(), mailbox, mAccount);
        try {
            parser.parse();
            mMessageUpdateStatus = parser.getMessageStatuses();
        } catch (final Parser.EmptyStreamException e) {
            // This indicates a compressed response which was empty, which is OK.
        }
        return RESULT_OK;
    }

    @Override
    protected long getTimeout() {
        if (mInitialSync) {
            return 120 * DateUtils.SECOND_IN_MILLIS;
        }
        return super.getTimeout();
    }

    /**
     * Create date/time in RFC8601 format.  Oddly enough, for calendar date/time, Microsoft uses
     * a different format that excludes the punctuation (this is why I'm not putting this in a
     * parent class)
     */
    private static String formatDateTime(final Calendar calendar) {
        final StringBuilder sb = new StringBuilder();
        //YYYY-MM-DDTHH:MM:SS.MSSZ
        sb.append(calendar.get(Calendar.YEAR));
        sb.append('-');
        sb.append(String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1));
        sb.append('-');
        sb.append(String.format(Locale.US, "%02d", calendar.get(Calendar.DAY_OF_MONTH)));
        sb.append('T');
        sb.append(String.format(Locale.US, "%02d", calendar.get(Calendar.HOUR_OF_DAY)));
        sb.append(':');
        sb.append(String.format(Locale.US, "%02d", calendar.get(Calendar.MINUTE)));
        sb.append(':');
        sb.append(String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND)));
        sb.append(".000Z");
        return sb.toString();
    }

    private void addOneCollectionToRequest(final Serializer s, final int collectionType,
            final String mailboxServerId, final String mailboxSyncKey,
            final List<MessageStateChange> stateChanges) throws IOException {

        s.start(Tags.SYNC_COLLECTION);
        if (getProtocolVersion() < Eas.SUPPORTED_PROTOCOL_EX2007_SP1_DOUBLE) {
            s.data(Tags.SYNC_CLASS, Eas.getFolderClass(collectionType));
        }
        s.data(Tags.SYNC_SYNC_KEY, mailboxSyncKey);
        s.data(Tags.SYNC_COLLECTION_ID, mailboxServerId);
        if (getProtocolVersion() >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
            // Exchange 2003 doesn't understand the concept of setting this flag to false. The
            // documentation indicates that its presence alone, with no value, requests a two-way
            // sync.
            // TODO: handle downsync here so we don't need this at all
            s.data(Tags.SYNC_GET_CHANGES, "0");
        }
        s.start(Tags.SYNC_COMMANDS);
        for (final MessageStateChange change : stateChanges) {
            s.start(Tags.SYNC_CHANGE);
            s.data(Tags.SYNC_SERVER_ID, change.getServerId());
            s.start(Tags.SYNC_APPLICATION_DATA);
            final int newFlagRead = change.getNewFlagRead();
            if (newFlagRead != MessageStateChange.VALUE_UNCHANGED) {
                s.data(Tags.EMAIL_READ, Integer.toString(newFlagRead));
            }
            final int newFlagFavorite = change.getNewFlagFavorite();
            if (newFlagFavorite != MessageStateChange.VALUE_UNCHANGED) {
                // "Flag" is a relatively complex concept in EAS 12.0 and above.  It is not only
                // the boolean "favorite" that we think of in Gmail, but it also represents a
                // follow up action, which can include a subject, start and due dates, and even
                // recurrences.  We don't support any of this as yet, but EAS 12.0 and higher
                // require that a flag contain a status, a type, and four date fields, two each
                // for start date and end (due) date.
                if (newFlagFavorite != 0) {
                    // Status 2 = set flag
                    s.start(Tags.EMAIL_FLAG).data(Tags.EMAIL_FLAG_STATUS, "2");
                    // "FollowUp" is the standard type
                    s.data(Tags.EMAIL_FLAG_TYPE, "FollowUp");
                    final long now = System.currentTimeMillis();
                    final Calendar calendar =
                            GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
                    calendar.setTimeInMillis(now);
                    // Flags are required to have a start date and end date (duplicated)
                    // First, we'll set the current date/time in GMT as the start time
                    String utc = formatDateTime(calendar);
                    s.data(Tags.TASK_START_DATE, utc).data(Tags.TASK_UTC_START_DATE, utc);
                    // And then we'll use one week from today for completion date
                    calendar.setTimeInMillis(now + DateUtils.WEEK_IN_MILLIS);
                    utc = formatDateTime(calendar);
                    s.data(Tags.TASK_DUE_DATE, utc).data(Tags.TASK_UTC_DUE_DATE, utc);
                    s.end();
                } else {
                    s.tag(Tags.EMAIL_FLAG);
                }
            }
            s.end().end();  // SYNC_APPLICATION_DATA, SYNC_CHANGE
        }
        s.end().end();  // SYNC_COMMANDS, SYNC_COLLECTION
    }
}
