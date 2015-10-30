package com.android.exchange.adapter;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.utility.TextUtilities;
import com.android.exchange.Eas;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Parse the result of a Search command
 */
public class SearchParser extends Parser {
    private static final String LOG_TAG = Logging.LOG_TAG;
    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final Mailbox mMailbox;
    private final Account mAccount;
    private final String mQuery;
    private int mTotalResults;

    public SearchParser(final Context context, final ContentResolver resolver,
        final InputStream in, final Mailbox mailbox, final Account account,
        String query)
            throws IOException {
        super(in);
        mContext = context;
        mContentResolver = resolver;
        mMailbox = mailbox;
        mAccount = account;
        mQuery = query;
    }

    public int getTotalResults() {
        return mTotalResults;
    }

    @Override
    public boolean parse() throws IOException {
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.SEARCH_SEARCH) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.SEARCH_STATUS) {
                String status = getValue();
                if (Eas.USER_LOG) {
                    LogUtils.d(Logging.LOG_TAG, "Search status: " + status);
                }
            } else if (tag == Tags.SEARCH_RESPONSE) {
                parseResponse();
            } else {
                skipTag();
            }
        }
        return res;
    }

    private boolean parseResponse() throws IOException {
        boolean res = false;
        while (nextTag(Tags.SEARCH_RESPONSE) != END) {
            if (tag == Tags.SEARCH_STORE) {
                parseStore();
            } else {
                skipTag();
            }
        }
        return res;
    }

    private boolean parseStore() throws IOException {
        EmailSyncParser parser = new EmailSyncParser(this, mContext, mContentResolver,
                mMailbox, mAccount);
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        boolean res = false;

        while (nextTag(Tags.SEARCH_STORE) != END) {
            if (tag == Tags.SEARCH_STATUS) {
                getValue();
            } else if (tag == Tags.SEARCH_TOTAL) {
                mTotalResults = getValueInt();
            } else if (tag == Tags.SEARCH_RESULT) {
                parseResult(parser, ops);
            } else {
                skipTag();
            }
        }

        try {
            // FLAG: In EmailSyncParser.commit(), we have complicated logic to constrain the size
            // of the batch, and fall back to one op at a time if that fails. We don't have any
            // such logic here, but we probably should.
            mContentResolver.applyBatch(EmailContent.AUTHORITY, ops);
            LogUtils.d(Logging.LOG_TAG, "Saved %s search results", ops.size());
        } catch (RemoteException e) {
            LogUtils.d(Logging.LOG_TAG, "RemoteException while saving search results.");
        } catch (OperationApplicationException e) {
        }

        return res;
    }

    private boolean parseResult(EmailSyncParser parser,
            ArrayList<ContentProviderOperation> ops) throws IOException {
        boolean res = false;
        Message msg = new Message();
        while (nextTag(Tags.SEARCH_RESULT) != END) {
            if (tag == Tags.SYNC_CLASS) {
                getValue();
            } else if (tag == Tags.SYNC_COLLECTION_ID) {
                getValue();
            } else if (tag == Tags.SEARCH_LONG_ID) {
                msg.mProtocolSearchInfo = getValue();
            } else if (tag == Tags.SEARCH_PROPERTIES) {
                msg.mAccountKey = mAccount.mId;
                msg.mMailboxKey = mMailbox.mId;
                msg.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;
                // Delegate parsing of the properties to the EmailSyncParser.

                // We push a new <Properties> tag onto the EmailSyncParser. It will parse
                // until it consumes the </Properties>
                parser.pushTag(tag);
                // Since the EmailSyncParser is responsible for consuming the </Properties>
                // tag, we need to remove it from our stack or it will be double counted.
                pop();

                parser.addData(msg, tag);
                if (msg.mHtml != null) {
                    msg.mHtml = TextUtilities.highlightTermsInHtml(msg.mHtml, mQuery);
                }
                msg.addSaveOps(ops);
            } else {
                skipTag();
            }
        }
        return res;
    }
}
