package com.android.exchange.eas;

import android.content.Context;
import android.net.TrafficStats;
import android.text.format.DateUtils;

import com.android.emailcommon.TrafficFlags;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.AbstractSyncParser;
import com.android.exchange.adapter.Parser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.mail.utils.LogUtils;

import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * Performs an EAS sync operation for one folder (excluding mail upsync).
 * TODO: Merge with {@link EasSync}, which currently handles mail upsync.
 */
public class EasSyncBase extends EasOperation {

    private static final String TAG = Eas.LOG_TAG;

    public static final int RESULT_DONE = 0;
    public static final int RESULT_MORE_AVAILABLE = 1;

    private boolean mInitialSync;
    private final Mailbox mMailbox;
    private EasSyncCollectionTypeBase mCollectionTypeHandler;

    private int mNumWindows;

    // TODO: Convert to accountId when ready to convert to EasService.
    public EasSyncBase(final Context context, final Account account, final Mailbox mailbox) {
        super(context, account);
        mMailbox = mailbox;
    }

    /**
     * Get the sync key for this mailbox.
     * @return The sync key for the object being synced. "0" means this is the first sync. If
     *      there is an error in getting the sync key, this function returns null.
     */
    protected String getSyncKey() {
        if (mMailbox == null) {
            return null;
        }
        if (mMailbox.mSyncKey == null) {
            mMailbox.mSyncKey = "0";
        }
        return mMailbox.mSyncKey;
    }

    @Override
    protected String getCommand() {
        return "Sync";
    }

    @Override
    public boolean init() {
        mCollectionTypeHandler = getCollectionTypeHandler(mMailbox.mType);
        if (mCollectionTypeHandler == null) {
            return false;
        }
        // Set up traffic stats bookkeeping.
        final int trafficFlags = TrafficFlags.getSyncFlags(mContext, mAccount);
        TrafficStats.setThreadStatsTag(trafficFlags | mCollectionTypeHandler.getTrafficFlag());
        return true;
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        final String className = Eas.getFolderClass(mMailbox.mType);
        final String syncKey = getSyncKey();
        LogUtils.d(TAG, "Syncing account %d mailbox %d (class %s) with syncKey %s", mAccount.mId,
                mMailbox.mId, className, syncKey);
        mInitialSync = EmailContent.isInitialSyncKey(syncKey);
        final Serializer s = new Serializer();
        s.start(Tags.SYNC_SYNC);
        s.start(Tags.SYNC_COLLECTIONS);
        s.start(Tags.SYNC_COLLECTION);
        // The "Class" element is removed in EAS 12.1 and later versions
        if (getProtocolVersion() < Eas.SUPPORTED_PROTOCOL_EX2007_SP1_DOUBLE) {
            s.data(Tags.SYNC_CLASS, className);
        }
        s.data(Tags.SYNC_SYNC_KEY, syncKey);
        s.data(Tags.SYNC_COLLECTION_ID, mMailbox.mServerId);
        mCollectionTypeHandler.setSyncOptions(mContext, s, getProtocolVersion(), mAccount, mMailbox,
                mInitialSync, mNumWindows);
        s.end().end().end().done();

        return makeEntity(s);
    }

    @Override
    protected int handleResponse(final EasResponse response)
            throws IOException, CommandStatusException {
        try {
            final AbstractSyncParser parser = mCollectionTypeHandler.getParser(mContext, mAccount,
                    mMailbox, response.getInputStream());
            final boolean moreAvailable = parser.parse();
            if (moreAvailable) {
                return RESULT_MORE_AVAILABLE;
            }
        } catch (final Parser.EmptyStreamException e) {
            // This indicates a compressed response which was empty, which is OK.
        }
        return RESULT_DONE;
    }

    @Override
    public int performOperation() {
        int result = RESULT_MORE_AVAILABLE;
        mNumWindows = 1;
        final String key = getSyncKey();
        while (result == RESULT_MORE_AVAILABLE) {
            result = super.performOperation();
            if (result == RESULT_MORE_AVAILABLE || result == RESULT_DONE) {
                mCollectionTypeHandler.cleanup(mContext, mAccount);
            }
            // TODO: Clear pending request queue.
            final String newKey = getSyncKey();
            if (result == RESULT_MORE_AVAILABLE && key.equals(newKey)) {
                LogUtils.e(TAG,
                        "Server has more data but we have the same key: %s numWindows: %d",
                        key, mNumWindows);
                mNumWindows++;
            } else {
                mNumWindows = 1;
            }
        }
        return result;
    }

    @Override
    protected long getTimeout() {
        if (mInitialSync) {
            return 120 * DateUtils.SECOND_IN_MILLIS;
        }
        return super.getTimeout();
    }

    /**
     * Get an instance of the correct {@link EasSyncCollectionTypeBase} for a specific collection
     * type.
     * @param type The type of the {@link Mailbox} that we're trying to sync.
     * @return An {@link EasSyncCollectionTypeBase} appropriate for this type.
     */
    private EasSyncCollectionTypeBase getCollectionTypeHandler(final int type) {
        switch (type) {
            case Mailbox.TYPE_MAIL:
            case Mailbox.TYPE_INBOX:
            case Mailbox.TYPE_DRAFTS:
            case Mailbox.TYPE_SENT:
            case Mailbox.TYPE_TRASH:
            case Mailbox.TYPE_JUNK:
                return new EasSyncMail();
            case Mailbox.TYPE_CALENDAR: {
                return new EasSyncCalendar(mContext, mAccount, mMailbox);
            }
            case Mailbox.TYPE_CONTACTS:
                return new EasSyncContacts(mAccount.mEmailAddress);
            default:
                LogUtils.e(LOG_TAG, "unexpected collectiontype %d", type);
                return null;
        }
    }
}
