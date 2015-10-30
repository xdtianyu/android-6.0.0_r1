/*
 * Copyright (C) 2008-2009 Marc Blank
 * Licensed to The Android Open Source Project.
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

package com.android.exchange.adapter;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.SyncWindow;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.exchange.CommandStatusException;
import com.android.exchange.CommandStatusException.CommandStatus;
import com.android.exchange.Eas;
import com.android.exchange.eas.EasSyncContacts;
import com.android.exchange.eas.EasSyncCalendar;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogUtils;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parse the result of a FolderSync command
 *
 * Handles the addition, deletion, and changes to folders in the user's Exchange account.
 **/

public class FolderSyncParser extends AbstractSyncParser {

    public static final String TAG = "FolderSyncParser";

    /**
     * Mapping from EAS type values to {@link Mailbox} types.
     * See http://msdn.microsoft.com/en-us/library/gg650877(v=exchg.80).aspx for the list of EAS
     * type values.
     * If an EAS type is not in the map, or is inserted with a value of {@link Mailbox#TYPE_NONE},
     * then we don't support that type and we should ignore it.
     * TODO: Maybe we should store the mailbox anyway, otherwise it'll be annoying to upgrade.
     */
    private static final SparseIntArray MAILBOX_TYPE_MAP;
    static {
        MAILBOX_TYPE_MAP = new SparseIntArray(11);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_USER_GENERIC,  Mailbox.TYPE_MAIL);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_INBOX,  Mailbox.TYPE_INBOX);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_DRAFTS,  Mailbox.TYPE_DRAFTS);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_DELETED,  Mailbox.TYPE_TRASH);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_SENT,  Mailbox.TYPE_SENT);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_OUTBOX,  Mailbox.TYPE_OUTBOX);
        //MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_TASKS,  Mailbox.TYPE_TASKS);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_CALENDAR,  Mailbox.TYPE_CALENDAR);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_CONTACTS,  Mailbox.TYPE_CONTACTS);
        //MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_NOTES, Mailbox.TYPE_NONE);
        //MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_JOURNAL, Mailbox.TYPE_NONE);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_USER_MAIL, Mailbox.TYPE_MAIL);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_USER_CALENDAR, Mailbox.TYPE_CALENDAR);
        MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_USER_CONTACTS, Mailbox.TYPE_CONTACTS);
        //MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_USER_TASKS, Mailbox.TYPE_TASKS);
        //MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_USER_JOURNAL, Mailbox.TYPE_NONE);
        //MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_USER_NOTES, Mailbox.TYPE_NONE);
        //MAILBOX_TYPE_MAP.put(Eas.MAILBOX_TYPE_UNKNOWN, Mailbox.TYPE_NONE);
        //MAILBOX_TYPE_MAP.put(MAILBOX_TYPE_RECIPIENT_INFORMATION_CACHE, Mailbox.TYPE_NONE);
    }

    /** Content selection for all mailboxes belonging to an account. */
    private static final String WHERE_ACCOUNT_KEY = MailboxColumns.ACCOUNT_KEY + "=?";

    /**
     * Content selection to find a specific mailbox by server id. Since server ids aren't unique
     * across all accounts, this must also check account id.
     */
    private static final String WHERE_SERVER_ID_AND_ACCOUNT = MailboxColumns.SERVER_ID + "=? and " +
        MailboxColumns.ACCOUNT_KEY + "=?";

    /**
     * Content selection to find a specific mailbox by display name and account.
     */
    private static final String WHERE_DISPLAY_NAME_AND_ACCOUNT = MailboxColumns.DISPLAY_NAME +
        "=? and " + MailboxColumns.ACCOUNT_KEY + "=?";

    /**
     * Content selection to find children by parent's server id. Since server ids aren't unique
     * across accounts, this must also use account id.
     */
    private static final String WHERE_PARENT_SERVER_ID_AND_ACCOUNT =
        MailboxColumns.PARENT_SERVER_ID +"=? and " + MailboxColumns.ACCOUNT_KEY + "=?";

    /** Projection used when fetching a Mailbox's ids. */
    private static final String[] MAILBOX_ID_COLUMNS_PROJECTION =
        new String[] {MailboxColumns.ID, MailboxColumns.SERVER_ID, MailboxColumns.PARENT_SERVER_ID};
    private static final int MAILBOX_ID_COLUMNS_ID = 0;
    private static final int MAILBOX_ID_COLUMNS_SERVER_ID = 1;
    private static final int MAILBOX_ID_COLUMNS_PARENT_SERVER_ID = 2;

    /** Projection used for changed parents during parent/child fixup. */
    private static final String[] FIXUP_PARENT_PROJECTION =
            { MailboxColumns.ID, MailboxColumns.FLAGS };
    private static final int FIXUP_PARENT_ID_COLUMN = 0;
    private static final int FIXUP_PARENT_FLAGS_COLUMN = 1;

    /** Projection used for changed children during parent/child fixup. */
    private static final String[] FIXUP_CHILD_PROJECTION =
            { MailboxColumns.ID };
    private static final int FIXUP_CHILD_ID_COLUMN = 0;

    /** Flags that are set or cleared when a mailbox's child status changes. */
    private static final int HAS_CHILDREN_FLAGS =
            Mailbox.FLAG_HAS_CHILDREN | Mailbox.FLAG_CHILDREN_VISIBLE;

    /** Mailbox.NO_MAILBOX, as a string (convenience since this is used in several places). */
    private static final String NO_MAILBOX_STRING = Long.toString(Mailbox.NO_MAILBOX);

    @VisibleForTesting
    long mAccountId;
    @VisibleForTesting
    String mAccountIdAsString;

    private final String[] mBindArguments = new String[2];

    /** List of pending operations to send as a batch to the content provider. */
    private final ArrayList<ContentProviderOperation> mOperations =
            new ArrayList<ContentProviderOperation>();
    /** Indicates whether this sync is an initial FolderSync. */
    private boolean mInitialSync;
    /** List of folder server ids whose children changed with this sync. */
    private final Set<String> mParentFixupsNeeded = new LinkedHashSet<String>();
    /** Indicates whether the sync response provided a different sync key than we had. */
    private boolean mSyncKeyChanged = false;

    // If true, we only care about status (this is true when validating an account) and ignore
    // other data
    private final boolean mStatusOnly;

    /** Map of folder types that have been created during this sync. */
    private final SparseBooleanArray mCreatedFolderTypes =
            new SparseBooleanArray(Mailbox.REQUIRED_FOLDER_TYPES.length);

    private static final ContentValues UNINITIALIZED_PARENT_KEY = new ContentValues();

    static {
        UNINITIALIZED_PARENT_KEY.put(MailboxColumns.PARENT_KEY, Mailbox.PARENT_KEY_UNINITIALIZED);
    }

    public FolderSyncParser(final Context context, final ContentResolver resolver,
            final InputStream in, final Account account, final boolean statusOnly)
                    throws IOException {
        super(context, resolver, in, null, account);
        mAccountId = mAccount.mId;
        mAccountIdAsString = Long.toString(mAccountId);
        mStatusOnly = statusOnly;
    }

    public FolderSyncParser(InputStream in, AbstractSyncAdapter adapter) throws IOException {
        this(in, adapter, false);
    }

    public FolderSyncParser(InputStream in, AbstractSyncAdapter adapter, boolean statusOnly)
            throws IOException {
        super(in, adapter);
        mAccountId = mAccount.mId;
        mAccountIdAsString = Long.toString(mAccountId);
        mStatusOnly = statusOnly;
    }

    @Override
    public boolean parse() throws IOException, CommandStatusException {
        int status;
        boolean res = false;
        boolean resetFolders = false;
        mInitialSync = (mAccount.mSyncKey == null) || "0".equals(mAccount.mSyncKey);
        if (mInitialSync) {
            // We're resyncing all folders for this account, so nuke any existing ones.
            // wipe() will also backup and then restore non default sync settings.
            wipe();
        }
        if (nextTag(START_DOCUMENT) != Tags.FOLDER_FOLDER_SYNC)
            throw new EasParserException();
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.FOLDER_STATUS) {
                status = getValueInt();
                // Do a sanity check on the account here; if we have any duplicated folders, we'll
                // act as though we have a bad folder sync key (wipe/reload mailboxes)
                // Note: The ContentValues isn't used, but no point creating a new one
                int dupes = 0;
                if (mAccountId > 0) {
                    dupes = mContentResolver.update(
                            ContentUris.withAppendedId(EmailContent.ACCOUNT_CHECK_URI, mAccountId),
                            UNINITIALIZED_PARENT_KEY, null, null);
                }
                if (dupes > 0) {
                    LogUtils.w(TAG, "Duplicate mailboxes found for account %d: %d", mAccountId,
                            dupes);
                    status = Eas.FOLDER_STATUS_INVALID_KEY;
                }
                if (status != Eas.FOLDER_STATUS_OK) {
                    // If the account hasn't been saved, this is a validation attempt, so we don't
                    // try reloading the folder list...
                    if (CommandStatus.isDeniedAccess(status) ||
                            CommandStatus.isNeedsProvisioning(status) ||
                            (mAccount.mId == Account.NOT_SAVED)) {
                        LogUtils.e(LogUtils.TAG, "FolderSync: Unknown status: " + status);
                        throw new CommandStatusException(status);
                    // Note that we need to catch both old-style (Eas.FOLDER_STATUS_INVALID_KEY)
                    // and EAS 14 style command status
                    } else if (status == Eas.FOLDER_STATUS_INVALID_KEY ||
                            CommandStatus.isBadSyncKey(status)) {
                        wipe();
                        // Reconstruct _main
                        res = true;
                        resetFolders = true;
                    } else {
                        // Other errors are at the server, so let's throw an error that will
                        // cause this sync to be retried at a later time
                        throw new EasParserException("Folder status error");
                    }
                }
            } else if (tag == Tags.FOLDER_SYNC_KEY) {
                final String newKey = getValue();
                if (newKey != null && !resetFolders) {
                    mSyncKeyChanged = !newKey.equals(mAccount.mSyncKey);
                    mAccount.mSyncKey = newKey;
                }
            } else if (tag == Tags.FOLDER_CHANGES) {
                if (mStatusOnly) return res;
                changesParser();
            } else
                skipTag();
        }
        if (!mStatusOnly) {
            commit();
        }
        return res;
    }

    /**
     * Get a cursor with folder ids for a specific folder.
     * @param serverId The server id for the folder we are interested in.
     * @return A cursor for the folder specified by serverId for this account.
     */
    private Cursor getServerIdCursor(final String serverId) {
        mBindArguments[0] = serverId;
        mBindArguments[1] = mAccountIdAsString;
        return mContentResolver.query(Mailbox.CONTENT_URI, MAILBOX_ID_COLUMNS_PROJECTION,
                WHERE_SERVER_ID_AND_ACCOUNT, mBindArguments, null);
    }

    /**
     * Add the appropriate {@link ContentProviderOperation} to {@link #mOperations} for a Delete
     * change in the FolderSync response.
     * @throws IOException
     */
    private void deleteParser() throws IOException {
        while (nextTag(Tags.FOLDER_DELETE) != END) {
            switch (tag) {
                case Tags.FOLDER_SERVER_ID:
                    final String serverId = getValue();
                    // Find the mailbox in this account with the given serverId
                    final Cursor c = getServerIdCursor(serverId);
                    try {
                        if (c.moveToFirst()) {
                            LogUtils.d(TAG, "Deleting %s", serverId);
                            final long mailboxId = c.getLong(MAILBOX_ID_COLUMNS_ID);
                            mOperations.add(ContentProviderOperation.newDelete(
                                    ContentUris.withAppendedId(Mailbox.CONTENT_URI,
                                            mailboxId)).build());
                            AttachmentUtilities.deleteAllMailboxAttachmentFiles(mContext,
                                    mAccountId, mailboxId);
                            final String parentId =
                                    c.getString(MAILBOX_ID_COLUMNS_PARENT_SERVER_ID);
                            if (!TextUtils.isEmpty(parentId)) {
                                mParentFixupsNeeded.add(parentId);
                            }
                        }
                    } finally {
                        c.close();
                    }
                    break;
                default:
                    skipTag();
            }
        }
    }

    private static class SyncOptions {
        private final int mInterval;
        private final int mLookback;
        private final int mSyncState;

        private SyncOptions(int interval, int lookback, int syncState) {
            mInterval = interval;
            mLookback = lookback;
            mSyncState = syncState;
        }
    }

    private static final String MAILBOX_STATE_SELECTION =
        MailboxColumns.ACCOUNT_KEY + "=? AND (" + MailboxColumns.SYNC_INTERVAL + "!=" +
            Account.CHECK_INTERVAL_NEVER + " OR " + Mailbox.SYNC_LOOKBACK + "!=" +
            SyncWindow.SYNC_WINDOW_ACCOUNT + ")";

    private static final String[] MAILBOX_STATE_PROJECTION = new String[] {
        MailboxColumns.SERVER_ID, MailboxColumns.SYNC_INTERVAL, MailboxColumns.SYNC_LOOKBACK,
            MailboxColumns.UI_SYNC_STATUS};
    private static final int MAILBOX_STATE_SERVER_ID = 0;
    private static final int MAILBOX_STATE_INTERVAL = 1;
    private static final int MAILBOX_STATE_LOOKBACK = 2;
    private static final int MAILBOX_STATE_SYNC_STATUS = 3;
    @VisibleForTesting
    final HashMap<String, SyncOptions> mSyncOptionsMap = new HashMap<String, SyncOptions>();

    /**
     * For every mailbox in this account that has a non-default interval or lookback, save those
     * values.
     */
    @VisibleForTesting
    void saveMailboxSyncOptions() {
        // Shouldn't be necessary, but...
        mSyncOptionsMap.clear();
        Cursor c = mContentResolver.query(Mailbox.CONTENT_URI, MAILBOX_STATE_PROJECTION,
                MAILBOX_STATE_SELECTION, new String[] {mAccountIdAsString}, null);
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    int syncStatus = c.getInt(MAILBOX_STATE_SYNC_STATUS);
                    // The only sync status I would ever want to propagate is INITIAL_SYNC_NEEDED.
                    // This is so that after a migration from the old Email to Unified Gmail
                    // won't appear to be empty, but not syncing.
                    if (syncStatus != UIProvider.SyncStatus.INITIAL_SYNC_NEEDED) {
                        syncStatus = UIProvider.SyncStatus.NO_SYNC;
                    }
                    mSyncOptionsMap.put(c.getString(MAILBOX_STATE_SERVER_ID),
                            new SyncOptions(c.getInt(MAILBOX_STATE_INTERVAL),
                                    c.getInt(MAILBOX_STATE_LOOKBACK),
                                    syncStatus));
                }
            } finally {
                c.close();
            }
        }
    }

    /**
     * For every set of saved mailbox sync options, try to find and restore those values
     */
    @VisibleForTesting
    void restoreMailboxSyncOptions() {
        try {
            ContentValues cv = new ContentValues();
            mBindArguments[1] = mAccountIdAsString;
            for (String serverId: mSyncOptionsMap.keySet()) {
                SyncOptions options = mSyncOptionsMap.get(serverId);
                cv.put(MailboxColumns.SYNC_INTERVAL, options.mInterval);
                cv.put(MailboxColumns.SYNC_LOOKBACK, options.mLookback);
                mBindArguments[0] = serverId;
                // If we match account and server id, set the sync options
                mContentResolver.update(Mailbox.CONTENT_URI, cv, WHERE_SERVER_ID_AND_ACCOUNT,
                        mBindArguments);
            }
        } finally {
            mSyncOptionsMap.clear();
        }
    }

    /**
     * Add a {@link ContentProviderOperation} to {@link #mOperations} to add a mailbox.
     * @param name The new mailbox's name.
     * @param serverId The new mailbox's server id.
     * @param parentServerId The server id of the new mailbox's parent ("0" if none).
     * @param mailboxType The mailbox's type, which is one of the values defined in {@link Mailbox}.
     * @param fromServer Whether this mailbox was synced from server (as opposed to local-only).
     * @throws IOException
     */
    private void addMailboxOp(final String name, final String serverId,
            final String parentServerId, final int mailboxType, final boolean fromServer)
            throws IOException {
        final ContentValues cv = new ContentValues(10);
        cv.put(MailboxColumns.DISPLAY_NAME, name);
        if (fromServer) {
            cv.put(MailboxColumns.SERVER_ID, serverId);
            final String parentId;
            if (parentServerId.equals("0")) {
                parentId = NO_MAILBOX_STRING;
                cv.put(MailboxColumns.PARENT_KEY, Mailbox.NO_MAILBOX);
            } else {
                parentId = parentServerId;
                mParentFixupsNeeded.add(parentId);
            }
            cv.put(MailboxColumns.PARENT_SERVER_ID, parentId);
        } else {
            cv.put(MailboxColumns.SERVER_ID, "");
            cv.put(MailboxColumns.PARENT_KEY, Mailbox.NO_MAILBOX);
            cv.put(MailboxColumns.PARENT_SERVER_ID, NO_MAILBOX_STRING);
            cv.put(MailboxColumns.TOTAL_COUNT, -1);
        }
        cv.put(MailboxColumns.ACCOUNT_KEY, mAccountId);
        cv.put(MailboxColumns.TYPE, mailboxType);

        final boolean shouldSync = fromServer && Mailbox.getDefaultSyncStateForType(mailboxType);
        cv.put(MailboxColumns.SYNC_INTERVAL, shouldSync ? 1 : 0);
        if (shouldSync) {
            cv.put(MailboxColumns.UI_SYNC_STATUS, UIProvider.SyncStatus.INITIAL_SYNC_NEEDED);
        } else {
            cv.put(MailboxColumns.UI_SYNC_STATUS, UIProvider.SyncStatus.NO_SYNC);
        }

        // Set basic flags
        int flags = 0;
        if (mailboxType <= Mailbox.TYPE_NOT_EMAIL) {
            flags |= Mailbox.FLAG_HOLDS_MAIL + Mailbox.FLAG_SUPPORTS_SETTINGS;
        }
        // Outbox, Drafts, and Sent don't allow mail to be moved to them
        if (mailboxType == Mailbox.TYPE_MAIL || mailboxType == Mailbox.TYPE_TRASH ||
                mailboxType == Mailbox.TYPE_JUNK || mailboxType == Mailbox.TYPE_INBOX) {
            flags |= Mailbox.FLAG_ACCEPTS_MOVED_MAIL;
        }
        cv.put(MailboxColumns.FLAGS, flags);

        // Make boxes like Contacts and Calendar invisible in the folder list
        cv.put(MailboxColumns.FLAG_VISIBLE, (mailboxType < Mailbox.TYPE_NOT_EMAIL));

        mOperations.add(
                ContentProviderOperation.newInsert(Mailbox.CONTENT_URI).withValues(cv).build());

        mCreatedFolderTypes.put(mailboxType, true);
    }

    /**
     * Add the appropriate {@link ContentProviderOperation} to {@link #mOperations} for an Add
     * change in the FolderSync response.
     * @throws IOException
     */
    private void addParser() throws IOException {
        String name = null;
        String serverId = null;
        String parentId = null;
        int type = 0;

        while (nextTag(Tags.FOLDER_ADD) != END) {
            switch (tag) {
                case Tags.FOLDER_DISPLAY_NAME: {
                    name = getValue();
                    break;
                }
                case Tags.FOLDER_TYPE: {
                    type = getValueInt();
                    break;
                }
                case Tags.FOLDER_PARENT_ID: {
                    parentId = getValue();
                    break;
                }
                case Tags.FOLDER_SERVER_ID: {
                    serverId = getValue();
                    break;
                }
                default:
                    skipTag();
            }
        }
        if (name != null && serverId != null && parentId != null) {
            final int mailboxType = MAILBOX_TYPE_MAP.get(type, Mailbox.TYPE_NONE);
            if (mailboxType != Mailbox.TYPE_NONE) {
                if (type == Eas.MAILBOX_TYPE_CALENDAR && !name.contains(mAccount.mEmailAddress)) {
                    name = mAccount.mEmailAddress;
                }
                addMailboxOp(name, serverId, parentId, mailboxType, true);
            }
        }
    }

    /**
     * Add the appropriate {@link ContentProviderOperation} to {@link #mOperations} for an Update
     * change in the FolderSync response.
     * @throws IOException
     */
    private void updateParser() throws IOException {
        String serverId = null;
        String displayName = null;
        String parentId = null;
        while (nextTag(Tags.FOLDER_UPDATE) != END) {
            switch (tag) {
                case Tags.FOLDER_SERVER_ID:
                    serverId = getValue();
                    break;
                case Tags.FOLDER_DISPLAY_NAME:
                    displayName = getValue();
                    break;
                case Tags.FOLDER_PARENT_ID:
                    parentId = getValue();
                    break;
                default:
                    skipTag();
                    break;
            }
        }
        // We'll make a change if one of parentId or displayName are specified
        // serverId is required, but let's be careful just the same
        if (serverId != null && (displayName != null || parentId != null)) {
            final Cursor c = getServerIdCursor(serverId);
            try {
                // If we find the mailbox (using serverId), make the change
                if (c.moveToFirst()) {
                    LogUtils.d(TAG, "Updating %s", serverId);
                    final ContentValues cv = new ContentValues();
                    // Store the new parent key.
                    cv.put(Mailbox.PARENT_SERVER_ID, parentId);
                    // Fix up old and new parents, as needed
                    if (!TextUtils.isEmpty(parentId)) {
                        mParentFixupsNeeded.add(parentId);
                    } else {
                        cv.put(Mailbox.PARENT_KEY, Mailbox.NO_MAILBOX);
                    }
                    final String oldParentId = c.getString(MAILBOX_ID_COLUMNS_PARENT_SERVER_ID);
                    if (!TextUtils.isEmpty(oldParentId)) {
                        mParentFixupsNeeded.add(oldParentId);
                    }
                    // Set display name if we've got one
                    if (displayName != null) {
                        cv.put(Mailbox.DISPLAY_NAME, displayName);
                    }
                    mOperations.add(ContentProviderOperation.newUpdate(
                            ContentUris.withAppendedId(Mailbox.CONTENT_URI,
                                    c.getLong(MAILBOX_ID_COLUMNS_ID))).withValues(cv).build());
                }
            } finally {
                c.close();
            }
        }
    }

    /**
     * Handle the Changes element of the FolderSync response. This is the container for Add, Delete,
     * and Update elements.
     * @throws IOException
     */
    private void changesParser() throws IOException {
        while (nextTag(Tags.FOLDER_CHANGES) != END) {
            if (tag == Tags.FOLDER_ADD) {
                addParser();
            } else if (tag == Tags.FOLDER_DELETE) {
                deleteParser();
            } else if (tag == Tags.FOLDER_UPDATE) {
                updateParser();
            } else if (tag == Tags.FOLDER_COUNT) {
                // TODO: Maybe we can make use of this count somehow.
                getValueInt();
            } else
                skipTag();
        }
    }

    /**
     * Commit the contents of {@link #mOperations} to the content provider.
     * @throws IOException
     */
    private void flushOperations() throws IOException {
        if (mOperations.isEmpty()) {
            return;
        }
        int transactionSize = mOperations.size();
        final ArrayList<ContentProviderOperation> subOps =
                new ArrayList<ContentProviderOperation>(transactionSize);
        while (!mOperations.isEmpty()) {
            subOps.clear();
            // If the original transaction is split into smaller transactions,
            // need to ensure the final transaction doesn't overrun the array.
            if (transactionSize > mOperations.size()) {
                transactionSize = mOperations.size();
            }
            subOps.addAll(mOperations.subList(0, transactionSize));
            // Try to apply the ops. If the transaction is too large, split it in half and try again
            // If some other error happens then throw an IOException up the stack.
            try {
                mContentResolver.applyBatch(EmailContent.AUTHORITY, subOps);
                mOperations.removeAll(subOps);
            } catch (final TransactionTooLargeException e) {
                // If the transaction is too large, try splitting it.
                if (transactionSize == 1) {
                    LogUtils.e(TAG, "Single operation transaction too large");
                    throw new IOException("Single operation transaction too large");
                }
                LogUtils.d(TAG, "Transaction operation count %d too large, halving...",
                        transactionSize);
                transactionSize = transactionSize / 2;
                if (transactionSize < 1) {
                    transactionSize = 1;
                }
            } catch (final RemoteException e) {
                LogUtils.e(TAG, "RemoteException in commit");
                throw new IOException("RemoteException in commit");
            } catch (final OperationApplicationException e) {
                LogUtils.e(TAG, "OperationApplicationException in commit");
                throw new IOException("OperationApplicationException in commit");
            }
        }
        mOperations.clear();
    }

    /**
     * Fix folder data for any folders whose parent or children changed during this sync.
     * Unfortunately this cannot be done in the same pass as the actual sync: newly synced folders
     * lack ids until they're committed to the content provider, so we can't set the parentKey
     * for their children.
     * During parsing, we only track the parents who have changed. We need to do a query for
     * children anyway (to determine whether a parent still has any) so it's simpler to not bother
     * tracking which folders have had their parents changed.
     * TODO: Figure out if we can avoid the two-pass.
     * @throws IOException
     */
    private void doParentFixups() throws IOException {
        if (mParentFixupsNeeded.isEmpty()) {
            return;
        }

        // These objects will be used in every loop iteration, so create them here for efficiency
        // and just reset the values inside the loop as necessary.
        final String[] bindArguments = new String[2];
        bindArguments[1] = mAccountIdAsString;
        final ContentValues cv = new ContentValues(1);

        for (final String parentServerId : mParentFixupsNeeded) {
            // Get info about this parent.
            bindArguments[0] = parentServerId;
            final Cursor parentCursor = mContentResolver.query(Mailbox.CONTENT_URI,
                    FIXUP_PARENT_PROJECTION, WHERE_SERVER_ID_AND_ACCOUNT, bindArguments, null);
            if (parentCursor == null) {
                // TODO: Error handling.
                continue;
            }
            final long parentId;
            final int parentFlags;
            try {
                if (parentCursor.moveToFirst()) {
                    parentId = parentCursor.getLong(FIXUP_PARENT_ID_COLUMN);
                    parentFlags = parentCursor.getInt(FIXUP_PARENT_FLAGS_COLUMN);
                } else {
                    // TODO: Error handling.
                    continue;
                }
            } finally {
                parentCursor.close();
            }

            // Fix any children for this parent.
            final Cursor childCursor = mContentResolver.query(Mailbox.CONTENT_URI,
                    FIXUP_CHILD_PROJECTION, WHERE_PARENT_SERVER_ID_AND_ACCOUNT, bindArguments,
                    null);
            boolean hasChildren = false;
            if (childCursor != null) {
                try {
                    // Clear the results of the last iteration.
                    cv.clear();
                    // All children in this loop share the same parentId.
                    cv.put(MailboxColumns.PARENT_KEY, parentId);
                    while (childCursor.moveToNext()) {
                        final long childId = childCursor.getLong(FIXUP_CHILD_ID_COLUMN);
                        mOperations.add(ContentProviderOperation.newUpdate(
                                ContentUris.withAppendedId(Mailbox.CONTENT_URI, childId)).
                                withValues(cv).build());
                        hasChildren = true;
                    }
                } finally {
                    childCursor.close();
                }
            }

            // Fix the parent's flags based on whether it now has children.
            final int newFlags;

            if (hasChildren) {
                newFlags = parentFlags | HAS_CHILDREN_FLAGS;
            } else {
                newFlags = parentFlags & ~HAS_CHILDREN_FLAGS;
            }
            if (newFlags != parentFlags) {
                cv.clear();
                cv.put(MailboxColumns.FLAGS, newFlags);
                mOperations.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                        Mailbox.CONTENT_URI, parentId)).withValues(cv).build());
            }
            flushOperations();
        }
    }

    @Override
    public void commandsParser() throws IOException {
    }

    @Override
    public void commit() throws IOException {
        // Set the account sync key.
        if (mSyncKeyChanged) {
            final ContentValues cv = new ContentValues(1);
            cv.put(AccountColumns.SYNC_KEY, mAccount.mSyncKey);
            mOperations.add(
                    ContentProviderOperation.newUpdate(mAccount.getUri()).withValues(cv).build());
        }

        // If this is the initial sync, make sure we have all the required folder types.
        if (mInitialSync) {
            for (final int requiredType : Mailbox.REQUIRED_FOLDER_TYPES) {
                if (!mCreatedFolderTypes.get(requiredType)) {
                    addMailboxOp(Mailbox.getSystemMailboxName(mContext, requiredType),
                            null, null, requiredType, false);
                }
            }
        }

        // Send all operations so far.
        flushOperations();

        // Now that new mailboxes are committed, let's do parent fixups.
        doParentFixups();

        // Look for sync issues and its children and delete them
        // I'm not aware of any other way to deal with this properly
        mBindArguments[0] = "Sync Issues";
        mBindArguments[1] = mAccountIdAsString;
        Cursor c = mContentResolver.query(Mailbox.CONTENT_URI,
                MAILBOX_ID_COLUMNS_PROJECTION, WHERE_DISPLAY_NAME_AND_ACCOUNT,
                mBindArguments, null);
        String parentServerId = null;
        long id = 0;
        try {
            if (c.moveToFirst()) {
                id = c.getLong(MAILBOX_ID_COLUMNS_ID);
                parentServerId = c.getString(MAILBOX_ID_COLUMNS_SERVER_ID);
            }
        } finally {
            c.close();
        }
        if (parentServerId != null) {
            mContentResolver.delete(ContentUris.withAppendedId(Mailbox.CONTENT_URI, id),
                    null, null);
            mBindArguments[0] = parentServerId;
            mContentResolver.delete(Mailbox.CONTENT_URI, WHERE_PARENT_SERVER_ID_AND_ACCOUNT,
                    mBindArguments);
        }

        // If we have saved options, restore them now
        if (mInitialSync) {
            restoreMailboxSyncOptions();
        }
    }

    @Override
    public void responsesParser() throws IOException {
    }

    @Override
    protected void wipe() {
        if (mAccountId == EmailContent.NOT_SAVED) {
            // This is a dummy account so we don't need to do anything yet.
            return;
        }

        // For real accounts, let's go ahead and wipe some data.
        EasSyncCalendar.wipeAccountFromContentProvider(mContext,
                mAccount.mEmailAddress);
        EasSyncContacts.wipeAccountFromContentProvider(mContext,
                mAccount.mEmailAddress);

        // Save away any mailbox sync information that is NOT default
        saveMailboxSyncOptions();
        // And only then, delete mailboxes
        mContentResolver.delete(Mailbox.CONTENT_URI, WHERE_ACCOUNT_KEY,
                new String[] {mAccountIdAsString});
        // Reset the sync key and save.
        mAccount.mSyncKey = "0";
        ContentValues cv = new ContentValues();
        cv.put(AccountColumns.SYNC_KEY, mAccount.mSyncKey);
        mContentResolver.update(ContentUris.withAppendedId(Account.CONTENT_URI,
                mAccount.mId), cv, null, null);
    }
}
