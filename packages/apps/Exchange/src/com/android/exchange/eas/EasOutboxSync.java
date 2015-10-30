package com.android.exchange.eas;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.internet.Rfc822Output;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.CommandStatusException.CommandStatus;
import com.android.exchange.adapter.SendMailParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.adapter.Parser.EmptyStreamException;
import com.android.mail.utils.LogUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class EasOutboxSync extends EasOperation {

    // Value for a message's server id when sending fails.
    public static final int SEND_FAILED = 1;
    // This needs to be long enough to send the longest reasonable message, without being so long
    // as to effectively "hang" sending of mail.  The standard 30 second timeout isn't long enough
    // for pictures and the like.  For now, we'll use 15 minutes, in the knowledge that any socket
    // failure would probably generate an Exception before timing out anyway
    public static final long SEND_MAIL_TIMEOUT = 15 * DateUtils.MINUTE_IN_MILLIS;

    public static final int RESULT_OK = 1;
    public static final int RESULT_IO_ERROR = -100;
    public static final int RESULT_ITEM_NOT_FOUND = -101;
    public static final int RESULT_SEND_FAILED = -102;

    private final Message mMessage;
    private boolean mIsEas14;
    private final File mCacheDir;
    private final SmartSendInfo mSmartSendInfo;
    private final int mModeTag;
    private File mTmpFile;
    private FileInputStream mFileStream;

    public EasOutboxSync(final Context context, final Account account, final Message message,
            final boolean useSmartSend) {
        super(context, account);
        mMessage = message;
        initEas14();
        mCacheDir = context.getCacheDir();
        if (useSmartSend) {
            mSmartSendInfo = SmartSendInfo.getSmartSendInfo(mContext, mAccount, mMessage);
        } else {
            mSmartSendInfo = null;
        }
        mModeTag = getModeTag(mSmartSendInfo);
    }

    /**
     * Have to override EasOperation::init because it reloads mAccount, so we
     * need to reset any derived values (eg, mIsEas14).
     */
    @Override
    public boolean init() {
        initEas14();
        return true;
    }

    private void initEas14() {
        mIsEas14 = Eas.isProtocolEas14(mAccount.mProtocolVersion);
    }

    @Override
    protected String getCommand() {
        String cmd = "SendMail";
        if (mSmartSendInfo != null) {
            // In EAS 14, we don't send itemId and collectionId in the command
            if (mIsEas14) {
                cmd = mSmartSendInfo.isForward() ? "SmartForward" : "SmartReply";
            } else {
                cmd = mSmartSendInfo.generateSmartSendCmd();
            }
        }
        // If we're not EAS 14, add our save-in-sent setting here
        if (!mIsEas14) {
            cmd += "&SaveInSent=T";
        }
        return cmd;
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException, MessageInvalidException {
        try {
            mTmpFile = File.createTempFile("eas_", "tmp", mCacheDir);
        } catch (final IOException e) {
            LogUtils.w(LOG_TAG, "IO error creating temp file");
            throw new IllegalStateException("Failure creating temp file");
        }

        if (!writeMessageToTempFile(mTmpFile, mMessage, mSmartSendInfo)) {
            // There are several reasons this could happen, possibly the message is corrupt (e.g.
            // the To header is null) or the disk is too full to handle the temporary message.
            // We can't send this message, but we don't want to abort the entire sync. Returning
            // this error code will let the caller recognize that this operation failed, but we
            // should continue on with the rest of the sync.
            LogUtils.w(LOG_TAG, "IO error writing to temp file");
            throw new MessageInvalidException("Failure writing to temp file");
        }

        try {
            mFileStream = new FileInputStream(mTmpFile);
        } catch (final FileNotFoundException e) {
            LogUtils.w(LOG_TAG, "IO error creating fileInputStream");
            throw new IllegalStateException("Failure creating fileInputStream");
        }
          final long fileLength = mTmpFile.length();
          final HttpEntity entity;
          if (mIsEas14) {
              entity = new SendMailEntity(mFileStream, fileLength, mModeTag, mMessage,
                      mSmartSendInfo);
          } else {
              entity = new InputStreamEntity(mFileStream, fileLength);
          }

          return entity;
    }

    @Override
    protected int handleHttpError(int httpStatus) {
        if (httpStatus == HttpStatus.SC_INTERNAL_SERVER_ERROR && mSmartSendInfo != null) {
            // Let's retry without "smart" commands.
            return RESULT_ITEM_NOT_FOUND;
        } else {
            return RESULT_OTHER_FAILURE;
        }
    }

    /**
     * This routine is called in a finally block in EasOperation.performOperation,
     * so the request may have failed part way through and there is no guarantee
     * what state we're in.
     */
    @Override
    protected void onRequestMade() {
        if (mFileStream != null) {
            try {
                mFileStream.close();
            } catch (IOException e) {
                LogUtils.w(LOG_TAG, "IOException closing fileStream %s", e);
            }
            mFileStream = null;
        }
        if (mTmpFile != null) {
            if (mTmpFile.exists()) {
                mTmpFile.delete();
            }
            mTmpFile = null;
        }
    }

    @Override
    protected int handleResponse(EasResponse response) throws IOException, CommandStatusException {
        if (mIsEas14) {
            try {
                // Try to parse the result
                final SendMailParser p = new SendMailParser(response.getInputStream(), mModeTag);
                // If we get here, the SendMail failed; go figure
                p.parse();
                // The parser holds the status
                final int status = p.getStatus();
                if (CommandStatus.isNeedsProvisioning(status)) {
                    LogUtils.w(LOG_TAG, "Needs provisioning before sending message: %d",
                            mMessage.mId);
                    return RESULT_PROVISIONING_ERROR;
                } else if (status == CommandStatus.ITEM_NOT_FOUND && mSmartSendInfo != null) {
                    // Let's retry without "smart" commands.
                    LogUtils.w(LOG_TAG, "ITEM_NOT_FOUND smart sending message: %d", mMessage.mId);
                    return RESULT_ITEM_NOT_FOUND;
                }
                // TODO: Set syncServerId = SEND_FAILED in DB?
                LogUtils.w(LOG_TAG, "General failure sending message: %d", mMessage.mId);
                return RESULT_SEND_FAILED;
            } catch (final EmptyStreamException e) {
                // This is actually fine; an empty stream means SendMail succeeded
                LogUtils.d(LOG_TAG, "Empty response sending message: %d", mMessage.mId);
                // Don't return here, fall through so that we'll delete the sent message.
            } catch (final IOException e) {
                // Parsing failed in some other way.
                LogUtils.e(LOG_TAG, e, "IOException sending message: %d", mMessage.mId);
                return RESULT_IO_ERROR;
            }
        } else {
            // FLAG: Do we need to parse results for earlier versions?
        }
        LogUtils.d(LOG_TAG, "Returning RESULT_OK after sending: %d", mMessage.mId);
        mContext.getContentResolver().delete(
            ContentUris.withAppendedId(Message.CONTENT_URI, mMessage.mId), null, null);
        return RESULT_OK;
    }

    /**
     * Writes message to the temp file.
     * @param tmpFile The temp file to use.
     * @param message The {@link Message} to write.
     * @param smartSendInfo The {@link SmartSendInfo} for this message send attempt.
     * @return Whether we could successfully write the file.
     */
    private boolean writeMessageToTempFile(final File tmpFile, final Message message,
            final SmartSendInfo smartSendInfo) {
        final FileOutputStream fileStream;
        try {
            fileStream = new FileOutputStream(tmpFile);
            Log.d(LogUtils.TAG, "created outputstream");
        } catch (final FileNotFoundException e) {
            Log.e(LogUtils.TAG, "Failed to create message file", e);
            return false;
        }
        try {
            final boolean smartSend = smartSendInfo != null;
            final ArrayList<Attachment> attachments =
                    smartSend ? smartSendInfo.mRequiredAtts : null;
            Rfc822Output.writeTo(mContext, message, fileStream, smartSend, true, attachments);
        } catch (final Exception e) {
            Log.e(LogUtils.TAG, "Failed to write message file", e);
            return false;
        } finally {
            try {
                fileStream.close();
            } catch (final IOException e) {
                // should not happen
                Log.e(LogUtils.TAG, "Failed to close file - should not happen", e);
            }
        }
        return true;
    }

    private int getModeTag(final SmartSendInfo smartSendInfo) {
        if (mIsEas14) {
            if (smartSendInfo == null) {
                return Tags.COMPOSE_SEND_MAIL;
            } else if (smartSendInfo.isForward()) {
                return Tags.COMPOSE_SMART_FORWARD;
            } else {
                return Tags.COMPOSE_SMART_REPLY;
            }
        }
        return 0;
    }

    /**
     * Information needed for SmartReply/SmartForward.
     */
    private static class SmartSendInfo {
        final String mItemId;
        final String mCollectionId;
        final boolean mIsReply;
        final ArrayList<Attachment> mRequiredAtts;

        private SmartSendInfo(final String itemId, final String collectionId,
                final boolean isReply,ArrayList<Attachment> requiredAtts) {
            mItemId = itemId;
            mCollectionId = collectionId;
            mIsReply = isReply;
            mRequiredAtts = requiredAtts;
        }

        public String generateSmartSendCmd() {
            final StringBuilder sb = new StringBuilder();
            sb.append(isForward() ? "SmartForward" : "SmartReply");
            sb.append("&ItemId=");
            sb.append(Uri.encode(mItemId, ":"));
            sb.append("&CollectionId=");
            sb.append(Uri.encode(mCollectionId, ":"));
            return sb.toString();
        }

        public boolean isForward() {
            return !mIsReply;
        }

        /**
         * See if a given attachment is among an array of attachments; it is if the locations of
         * both are the same (we're looking to see if they represent the same attachment on the
         * server. Note that an attachment that isn't on the server (e.g. an outbound attachment
         * picked from the  gallery) won't have a location, so the result will always be false.
         *
         * @param att the attachment to test
         * @param atts the array of attachments to look in
         * @return whether the test attachment is among the array of attachments
         */
        private static boolean amongAttachments(final Attachment att, final Attachment[] atts) {
            final String location = att.mLocation;
            if (location == null) return false;
            for (final Attachment a: atts) {
                if (location.equals(a.mLocation)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * If this message should use SmartReply or SmartForward, return an object with the data
         * for the smart send.
         *
         * @param context the caller's context
         * @param account the Account we're sending from
         * @param message the Message being sent
         * @return an object to support smart sending, or null if not applicable.
         */
        public static SmartSendInfo getSmartSendInfo(final Context context,
                final Account account, final Message message) {
            final int flags = message.mFlags;
            // We only care about the original message if we include quoted text.
            if ((flags & Message.FLAG_NOT_INCLUDE_QUOTED_TEXT) != 0) {
                return null;
            }
            final boolean reply = (flags & Message.FLAG_TYPE_REPLY) != 0;
            final boolean forward = (flags & Message.FLAG_TYPE_FORWARD) != 0;
            // We also only care for replies or forwards.
            if (!reply && !forward) {
                return null;
            }
            // Just a sanity check here, since we assume that reply and forward are mutually
            // exclusive throughout this class.
            if (reply && forward) {
                return null;
            }
            // If we don't support SmartForward, then don't proceed.
            // TODO: For now, we assume that if we do not support Smart Forward, we also don't
            // support Smart Reply. At some point, perhaps these should be separate flags.
            if ((account.mFlags & Account.FLAGS_SUPPORTS_SMART_FORWARD) == 0) {
                return null;
            }

            // Note: itemId and collectionId are the terms used by EAS to refer to the serverId and
            // mailboxId of a Message
            String itemId = null;
            String collectionId = null;

            // First, we need to get the id of the reply/forward message, 0 is the default value
            // so we are looking for something greater than 0.
            final long refId = Body.restoreBodySourceKey(context, message.mId);
            LogUtils.d(LOG_TAG, "getSmartSendInfo - found refId: %d for %d", refId, message.mId);
            if (refId > 0) {
                // Then, we need the serverId and mailboxKey of the message
                final String[] colsMailboxKey = Utility.getRowColumns(context, Message.CONTENT_URI,
                        refId, SyncColumns.SERVER_ID, MessageColumns.MAILBOX_KEY,
                        MessageColumns.PROTOCOL_SEARCH_INFO);
                if (colsMailboxKey != null) {
                    itemId = colsMailboxKey[0];
                    final long boxId = Long.parseLong(colsMailboxKey[1]);
                    // Then, we need the serverId of the mailbox
                    final String[] colsServerId = Utility.getRowColumns(context,
                            Mailbox.CONTENT_URI, boxId, MailboxColumns.SERVER_ID);
                    if (colsServerId != null) {
                        collectionId = colsServerId[0];
                    }
                }
            }
            // We need either a longId or both itemId (serverId) and collectionId (mailboxId) to
            // process a smart reply or a smart forward
            if (itemId != null && collectionId != null) {
                final ArrayList<Attachment> requiredAtts;
                if (forward) {
                    // See if we can really smart forward (all reference attachments must be sent)
                    final Attachment[] outAtts =
                            Attachment.restoreAttachmentsWithMessageId(context, message.mId);
                    final Attachment[] refAtts =
                            Attachment.restoreAttachmentsWithMessageId(context, refId);
                    for (final Attachment refAtt: refAtts) {
                        // If an original attachment isn't among what's going out, we can't be smart
                        if (!amongAttachments(refAtt, outAtts)) {
                            return null;
                        }
                    }
                    requiredAtts = new ArrayList<Attachment>();
                    for (final Attachment outAtt: outAtts) {
                        // If an outgoing attachment isn't in original message, we must send it
                        if (!amongAttachments(outAtt, refAtts)) {
                            requiredAtts.add(outAtt);
                        }
                    }
                } else {
                    requiredAtts = null;
                }
                return new SmartSendInfo(itemId, collectionId, reply, requiredAtts);
            } else {
                LogUtils.w(LOG_TAG,
                        "getSmartSendInfo - Skipping SmartSend, could not find IDs for: %d",
                        message.mId);
            }
            return null;
        }
    }

    @Override
    public String getRequestContentType() {
        // When using older protocols, we need to use a different MIME type for sending messages.
        if (getProtocolVersion() < Eas.SUPPORTED_PROTOCOL_EX2010_DOUBLE) {
            return MimeUtility.MIME_TYPE_RFC822;
        } else {
            return super.getRequestContentType();
        }
    }

    /**
     * Our own HttpEntity subclass that is able to insert opaque data (in this case the MIME
     * representation of the message body as stored in a temporary file) into the serializer stream
     */
    private static class SendMailEntity extends InputStreamEntity {
        private final FileInputStream mFileStream;
        private final long mFileLength;
        private final int mSendTag;
        private final Message mMessage;
        private final SmartSendInfo mSmartSendInfo;

        public SendMailEntity(final FileInputStream instream, final long length, final int tag,
                final Message message, final SmartSendInfo smartSendInfo) {
            super(instream, length);
            mFileStream = instream;
            mFileLength = length;
            mSendTag = tag;
            mMessage = message;
            mSmartSendInfo = smartSendInfo;
        }

        /**
         * We always return -1 because we don't know the actual length of the POST data (this
         * causes HttpClient to send the data in "chunked" mode)
         */
        @Override
        public long getContentLength() {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                // Calculate the overhead for the WBXML data
                writeTo(baos, false);
                // Return the actual size that will be sent
                return baos.size() + mFileLength;
            } catch (final IOException e) {
                // Just return -1 (unknown)
            } finally {
                try {
                    baos.close();
                } catch (final IOException e) {
                    // Ignore
                }
            }
            return -1;
        }

        @Override
        public void writeTo(final OutputStream outstream) throws IOException {
            writeTo(outstream, true);
        }

        /**
         * Write the message to the output stream
         * @param outstream the output stream to write
         * @param withData whether or not the actual data is to be written; true when sending
         *   mail; false when calculating size only
         * @throws IOException
         */
        public void writeTo(final OutputStream outstream, final boolean withData)
                throws IOException {
            // Not sure if this is possible; the check is taken from the superclass
            if (outstream == null) {
                throw new IllegalArgumentException("Output stream may not be null");
            }

            // We'll serialize directly into the output stream
            final Serializer s = new Serializer(outstream);
            // Send the appropriate initial tag
            s.start(mSendTag);
            // The Message-Id for this message (note that we cannot use the messageId stored in
            // the message, as EAS 14 limits the length to 40 chars and we use 70+)
            s.data(Tags.COMPOSE_CLIENT_ID, "SendMail-" + System.nanoTime());
            // We always save sent mail
            s.tag(Tags.COMPOSE_SAVE_IN_SENT_ITEMS);

            // If we're using smart reply/forward, we need info about the original message
            if (mSendTag != Tags.COMPOSE_SEND_MAIL) {
                if (mSmartSendInfo != null) {
                    s.start(Tags.COMPOSE_SOURCE);
                    // For search results, use the long id (stored in mProtocolSearchInfo); else,
                    // use folder id/item id combo
                    if (mMessage.mProtocolSearchInfo != null) {
                        s.data(Tags.COMPOSE_LONG_ID, mMessage.mProtocolSearchInfo);
                    } else {
                        s.data(Tags.COMPOSE_ITEM_ID, mSmartSendInfo.mItemId);
                        s.data(Tags.COMPOSE_FOLDER_ID, mSmartSendInfo.mCollectionId);
                    }
                    s.end();  // Tags.COMPOSE_SOURCE
                }
            }

            // Start the MIME tag; this is followed by "opaque" data (byte array)
            s.start(Tags.COMPOSE_MIME);
            // Send opaque data from the file stream
            if (withData) {
                s.opaque(mFileStream, (int) mFileLength);
            } else {
                s.writeOpaqueHeader((int) mFileLength);
            }
            // And we're done
            s.end().end().done();
        }
    }
}
