package com.android.exchange.eas;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.service.EasService;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogUtils;

import org.apache.http.HttpEntity;

import java.io.IOException;
import java.util.Set;

public class EasFullSyncOperation extends EasOperation {
    private final static String TAG = LogUtils.TAG;

    private final static int RESULT_SUCCESS = 0;
    public final static int RESULT_SECURITY_HOLD = -100;

    public static final int SEND_FAILED = 1;
    public static final String MAILBOX_KEY_AND_NOT_SEND_FAILED =
            EmailContent.MessageColumns.MAILBOX_KEY + "=? and (" +
                    EmailContent.SyncColumns.SERVER_ID + " is null or " +
                    EmailContent.SyncColumns.SERVER_ID + "!=" + SEND_FAILED + ')';
    /**
     * The content authorities that can be synced for EAS accounts. Initialization must wait until
     * after we have a chance to call {@link EmailContent#init} (and, for future content types,
     * possibly other initializations) because that's how we can know what the email authority is.
     */
    private static String[] AUTHORITIES_TO_SYNC;

    static {
        // Statically initialize the authorities we'll sync.
        AUTHORITIES_TO_SYNC = new String[] {
                EmailContent.AUTHORITY,
                CalendarContract.AUTHORITY,
                ContactsContract.AUTHORITY
        };
    }

    final Bundle mSyncExtras;
    Set<String> mAuthsToSync;

    public EasFullSyncOperation(final Context context, final Account account,
                                final Bundle syncExtras) {
        super(context, account);
        mSyncExtras = syncExtras;
    }

    @Override
    protected String getCommand() {
        // This is really a container operation, its performOperation() actually just creates and
        // performs a bunch of other operations. It doesn't actually do any of its own
        // requests.
        // TODO: This is kind of ugly, maybe we need a simpler base class for EasOperation that
        // does not assume that it will perform a single network operation.
        LogUtils.e(TAG, "unexpected call to EasFullSyncOperation.getCommand");
        return null;
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        // This is really a container operation, its performOperation() actually just creates and
        // performs a bunch of other operations. It doesn't actually do any of its own
        // requests.
        LogUtils.e(TAG, "unexpected call to EasFullSyncOperation.getRequestEntity");
        return null;
    }

    @Override
    protected int handleResponse(final EasResponse response)
            throws IOException, CommandStatusException {
        // This is really a container operation, its performOperation() actually just creates and
        // performs a bunch of other operations. It doesn't actually do any of its own
        // requests.
        LogUtils.e(TAG, "unexpected call to EasFullSyncOperation.handleResponse");
        return RESULT_SUCCESS;
    }

    @Override
    public int performOperation() {
        if (!init()) {
            LogUtils.i(LOG_TAG, "Failed to initialize %d before sending request for operation %s",
                    getAccountId(), getCommand());
            return RESULT_INITIALIZATION_FAILURE;
        }

        final android.accounts.Account amAccount = new android.accounts.Account(
                mAccount.mEmailAddress, Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
        mAuthsToSync = EasService.getAuthoritiesToSync(amAccount, AUTHORITIES_TO_SYNC);

        // Figure out what we want to sync, based on the extras and our account sync status.
        final boolean isInitialSync = EmailContent.isInitialSyncKey(mAccount.mSyncKey);
        final long[] mailboxIds = Mailbox.getMailboxIdsFromBundle(mSyncExtras);
        final int mailboxType = mSyncExtras.getInt(Mailbox.SYNC_EXTRA_MAILBOX_TYPE,
                Mailbox.TYPE_NONE);

        final boolean isManual = mSyncExtras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        // Push only means this sync request should only refresh the ping (either because
        // settings changed, or we need to restart it for some reason).
        final boolean pushOnly = Mailbox.isPushOnlyExtras(mSyncExtras);
        // Account only means just do a FolderSync.
        final boolean accountOnly = Mailbox.isAccountOnlyExtras(mSyncExtras);
        final boolean hasCallbackMethod =
                mSyncExtras.containsKey(EmailServiceStatus.SYNC_EXTRAS_CALLBACK_METHOD);
        // A "full sync" means that we didn't request a more specific type of sync.
        // In this case we sync the folder list and all syncable folders.
        final boolean isFullSync = (!pushOnly && !accountOnly && mailboxIds == null &&
                mailboxType == Mailbox.TYPE_NONE);
        // A FolderSync is necessary for full sync, initial sync, and account only sync.
        final boolean isFolderSync = (isFullSync || isInitialSync || accountOnly);

        int result;

        // Now we will use a bunch of other EasOperations to actually do the sync. Note that
        // since we have overridden performOperation, this EasOperation does not have the
        // normal handling of errors and retrying that is built in. The handling of errors and
        // retries is done in each individual operation.

        // Perform a FolderSync if necessary.
        // TODO: We permit FolderSync even during security hold, because it's necessary to
        // resolve some holds. Ideally we would only do it for the holds that require it.
        if (isFolderSync) {
            final EasFolderSync folderSync = new EasFolderSync(mContext, mAccount);
            result = folderSync.performOperation();
            if (isFatal(result)) {
                // This is a failure, abort the sync.
                LogUtils.i(TAG, "Fatal result %d on folderSync", result);
                return result;
            }
        }

        // Do not permit further syncs if we're on security hold.
        if ((mAccount.mFlags & Account.FLAGS_SECURITY_HOLD) != 0) {
            LogUtils.d(TAG, "Account is on security hold %d", mAccount.getId());
            return RESULT_SECURITY_HOLD;
        }

        if (!isInitialSync) {
            EasMoveItems moveOp = new EasMoveItems(mContext, mAccount);
            result = moveOp.upsyncMovedMessages();
            if (isFatal(result)) {
                // This is a failure, abort the sync.
                LogUtils.i(TAG, "Fatal result %d on MoveItems", result);
                return result;
            }

            final EasSync upsync = new EasSync(mContext, mAccount);
            result = upsync.upsync();
            if (isFatal(result)) {
                // This is a failure, abort the sync.
                LogUtils.i(TAG, "Fatal result %d on upsync", result);
                return result;
            }
        }

        if (mailboxIds != null) {
            // Sync the mailbox that was explicitly requested.
            for (final long mailboxId : mailboxIds) {
                result = syncMailbox(mailboxId, hasCallbackMethod, isManual);
                if (isFatal(result)) {
                    // This is a failure, abort the sync.
                    LogUtils.i(TAG, "Fatal result %d on syncMailbox", result);
                    return result;
                }
            }
        } else if (!accountOnly && !pushOnly) {
           // We have to sync multiple folders.
            final Cursor c;
            if (isFullSync) {
                // Full account sync includes all mailboxes that participate in system sync.
                c = Mailbox.getMailboxIdsForSync(mContext.getContentResolver(), mAccount.mId);
            } else {
                // Type-filtered sync should only get the mailboxes of a specific type.
                c = Mailbox.getMailboxIdsForSyncByType(mContext.getContentResolver(),
                        mAccount.mId, mailboxType);
            }
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        result = syncMailbox(c.getLong(0), hasCallbackMethod, false);
                        if (isFatal(result)) {
                            // This is a failure, abort the sync.
                            LogUtils.i(TAG, "Fatal result %d on syncMailbox", result);
                            return result;
                        }
                    }
                } finally {
                    c.close();
                }
            }
        }

        return RESULT_SUCCESS;
    }

    private int syncMailbox(final long folderId, final boolean hasCallbackMethod,
                            final boolean isUserSync) {
        final Mailbox mailbox = Mailbox.restoreMailboxWithId(mContext, folderId);
        if (mailbox == null) {
            LogUtils.d(TAG, "Could not load folder %d", folderId);
            return EasSyncBase.RESULT_HARD_DATA_FAILURE;
        }

        if (mailbox.mAccountKey != mAccount.mId) {
            LogUtils.e(TAG, "Mailbox does not match account: mailbox %s, %s", mAccount.toString(),
                    mSyncExtras);
            return EasSyncBase.RESULT_HARD_DATA_FAILURE;
        }

        if (mAuthsToSync != null && !mAuthsToSync.contains(Mailbox.getAuthority(mailbox.mType))) {
            // We are asking for an account sync, but this mailbox type is not configured for
            // sync. Do NOT treat this as a sync error for ping backoff purposes.
            return EasSyncBase.RESULT_DONE;
        }

        if (mailbox.mType == Mailbox.TYPE_DRAFTS) {
            // TODO: Because we don't have bidirectional sync working, trying to downsync
            // the drafts folder is confusing. b/11158759
            // For now, just disable all syncing of DRAFTS type folders.
            // Automatic syncing should always be disabled, but we also stop it here to ensure
            // that we won't sync even if the user attempts to force a sync from the UI.
            // Do NOT treat as a sync error for ping backoff purposes.
            LogUtils.d(TAG, "Skipping sync of DRAFTS folder");
            return EmailServiceStatus.SUCCESS;
        }

        int syncResult = 0;
        // Non-mailbox syncs are whole account syncs initiated by the AccountManager and are
        // treated as background syncs.
        if (mailbox.mType == Mailbox.TYPE_OUTBOX || mailbox.isSyncable()) {
            final ContentValues cv = new ContentValues(2);
            final int syncStatus = isUserSync ?
                    EmailContent.SYNC_STATUS_USER : EmailContent.SYNC_STATUS_BACKGROUND;
            updateMailbox(mailbox, cv, syncStatus);
            try {
                if (mailbox.mType == Mailbox.TYPE_OUTBOX) {
                    return syncOutbox(mailbox.mId);
                }
                if (hasCallbackMethod) {
                    final int lastSyncResult = UIProvider.createSyncValue(syncStatus,
                            UIProvider.LastSyncResult.SUCCESS);
                    EmailServiceStatus.syncMailboxStatus(mContext.getContentResolver(), mSyncExtras,
                            mailbox.mId, EmailServiceStatus.IN_PROGRESS, 0, lastSyncResult);
                }
                final EasSyncBase operation = new EasSyncBase(mContext, mAccount, mailbox);
                LogUtils.d(TAG, "IEmailService.syncMailbox account %d", mAccount.mId);
                syncResult = operation.performOperation();
            } finally {
                updateMailbox(mailbox, cv, EmailContent.SYNC_STATUS_NONE);
                if (hasCallbackMethod) {
                    final int uiSyncResult = translateSyncResultToUiResult(syncResult);
                    final int lastSyncResult = UIProvider.createSyncValue(syncStatus, uiSyncResult);
                    EmailServiceStatus.syncMailboxStatus(mContext.getContentResolver(), mSyncExtras,
                            mailbox.mId, EmailServiceStatus.SUCCESS, 0, lastSyncResult);
                }
            }
        } else {
            // This mailbox is not syncable.
            LogUtils.d(TAG, "Skipping sync of non syncable folder");
        }

        return syncResult;
    }

    private int syncOutbox(final long mailboxId) {
        LogUtils.d(TAG, "syncOutbox %d", mAccount.mId);
        // Because syncing the outbox uses a single EasOperation for every message, we don't
        // want to use doOperation(). That would stop and restart the ping between each operation,
        // which is wasteful if we have several messages to send.
        final Cursor c = mContext.getContentResolver().query(EmailContent.Message.CONTENT_URI,
                EmailContent.Message.CONTENT_PROJECTION, MAILBOX_KEY_AND_NOT_SEND_FAILED,
                new String[] {Long.toString(mailboxId)}, null);
        try {
            // Loop through the messages, sending each one
            while (c.moveToNext()) {
                final Message message = new Message();
                message.restore(c);
                if (Utility.hasUnloadedAttachments(mContext, message.mId)) {
                    // We'll just have to wait on this...
                    // TODO: We should make sure that this attachment is queued for download here.
                    continue;
                }

                // TODO: Fix -- how do we want to signal to UI that we started syncing?
                // Note the entire callback mechanism here needs improving.
                //sendMessageStatus(message.mId, null, EmailServiceStatus.IN_PROGRESS, 0);

                EasOperation op = new EasOutboxSync(mContext, mAccount, message, true);

                int result = op.performOperation();
                if (result == EasOutboxSync.RESULT_ITEM_NOT_FOUND) {
                    // This can happen if we are using smartReply, and the message we are referring
                    // to has disappeared from the server. Try again with smartReply disabled.
                    // This should be a legitimate, but unusual case. Log a warning.
                    LogUtils.w(TAG, "WARNING: EasOutboxSync falling back from smartReply");
                    op = new EasOutboxSync(mContext, mAccount, message, false);
                    result = op.performOperation();
                }
                // If we got some connection error or other fatal error, terminate the sync.
                // If we get some non-fatal error, continue.
                if (result != EasOutboxSync.RESULT_OK &&
                        result != EasOutboxSync.RESULT_NON_FATAL_ERROR &&
                        result > EasOutboxSync.RESULT_OP_SPECIFIC_ERROR_RESULT) {
                    LogUtils.w(TAG, "Aborting outbox sync for error %d", result);
                    return result;
                } else if (result <= EasOutboxSync.RESULT_OP_SPECIFIC_ERROR_RESULT) {
                    // There are several different conditions that can cause outbox syncing to fail,
                    // but they shouldn't prevent us from continuing and trying to downsync
                    // other mailboxes.
                    LogUtils.i(TAG, "Outbox sync failed with result %d", result);
                }
            }
        } finally {
            // TODO: Some sort of sendMessageStatus() is needed here.
            c.close();
        }

        return EasOutboxSync.RESULT_OK;
    }


    /**
     * Update the mailbox's sync status with the provider and, if we're finished with the sync,
     * write the last sync time as well.
     * @param mailbox The mailbox whose sync status to update.
     * @param cv A {@link ContentValues} object to use for updating the provider.
     * @param syncStatus The status for the current sync.
     */
    private void updateMailbox(final Mailbox mailbox, final ContentValues cv,
                               final int syncStatus) {
        cv.put(Mailbox.UI_SYNC_STATUS, syncStatus);
        if (syncStatus == EmailContent.SYNC_STATUS_NONE) {
            cv.put(Mailbox.SYNC_TIME, System.currentTimeMillis());
        }
        mailbox.update(mContext, cv);
    }
}
