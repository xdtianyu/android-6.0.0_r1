/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.Context;
import android.os.RemoteException;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.emailcommon.service.IEmailServiceCallback;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.ItemOperationsParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.service.EasService;
import com.android.exchange.utility.UriCodec;
import com.android.mail.utils.LogUtils;

import org.apache.http.HttpEntity;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class performs the heavy lifting of loading attachments from the Exchange server to the
 * device in a local file.
 * TODO: Add ability to call back to UI when this failed, and generally better handle error cases.
 */
public final class EasLoadAttachment extends EasOperation {

    public static final int RESULT_SUCCESS = 0;

    /** Attachment Loading Errors **/
    public static final int RESULT_LOAD_ATTACHMENT_INFO_ERROR = -100;
    public static final int RESULT_ATTACHMENT_NO_LOCATION_ERROR = -101;
    public static final int RESULT_ATTACHMENT_LOAD_MESSAGE_ERROR = -102;
    public static final int RESULT_ATTACHMENT_INTERNAL_HANDLING_ERROR = -103;
    public static final int RESULT_ATTACHMENT_RESPONSE_PARSING_ERROR = -104;

    private final IEmailServiceCallback mCallback;
    private final long mAttachmentId;

    // These members are set in a future point in time outside of the constructor.
    private Attachment mAttachment;

    /**
     * Constructor for use with {@link EasService} when performing an actual sync.
     * @param context Our {@link Context}.
     * @param account The account we're loading the attachment for.
     * @param attachmentId The local id of the attachment (i.e. its id in the database).
     * @param callback The callback for any status updates.
     */
    public EasLoadAttachment(final Context context, final Account account, final long attachmentId,
            final IEmailServiceCallback callback) {
        // The account is loaded before performOperation but it is not guaranteed to be available
        // before then.
        super(context, account);
        mCallback = callback;
        mAttachmentId = attachmentId;
    }

    /**
     * Helper function that makes a callback for us within our implementation.
     */
    private static void doStatusCallback(final IEmailServiceCallback callback,
            final long messageKey, final long attachmentId, final int status, final int progress) {
        if (callback != null) {
            try {
                // loadAttachmentStatus is mart of IEmailService interface.
                callback.loadAttachmentStatus(messageKey, attachmentId, status, progress);
            } catch (final RemoteException e) {
                LogUtils.e(LOG_TAG, "RemoteException in loadAttachment: %s", e.getMessage());
            }
        }
    }

    /**
     * Helper class that is passed to other objects to perform callbacks for us.
     */
    public static class ProgressCallback {
        private final IEmailServiceCallback mCallback;
        private final EmailContent.Attachment mAttachment;

        public ProgressCallback(final IEmailServiceCallback callback,
                final EmailContent.Attachment attachment) {
            mCallback = callback;
            mAttachment = attachment;
        }

        public void doCallback(final int progress) {
            doStatusCallback(mCallback, mAttachment.mMessageKey, mAttachment.mId,
                    EmailServiceStatus.IN_PROGRESS, progress);
        }
    }

    /**
     * Encoder for Exchange 2003 attachment names.  They come from the server partially encoded,
     * but there are still possible characters that need to be encoded (Why, MSFT, why?)
     */
    private static class AttachmentNameEncoder extends UriCodec {
        @Override
        protected boolean isRetained(final char c) {
            // These four characters are commonly received in EAS 2.5 attachment names and are
            // valid (verified by testing); we won't encode them
            return c == '_' || c == ':' || c == '/' || c == '.';
        }
    }

    /**
     * Finish encoding attachment names for Exchange 2003.
     * @param str A partially encoded string.
     * @return The fully encoded version of str.
     */
    private static String encodeForExchange2003(final String str) {
        final AttachmentNameEncoder enc = new AttachmentNameEncoder();
        final StringBuilder sb = new StringBuilder(str.length() + 16);
        enc.appendPartiallyEncoded(sb, str);
        return sb.toString();
    }

    /**
     * Finish encoding attachment names for Exchange 2003.
     * @return A {@link EmailServiceStatus} code that indicates the result of the operation.
     */
    @Override
    public int performOperation() {
        mAttachment = EmailContent.Attachment.restoreAttachmentWithId(mContext, mAttachmentId);
        if (mAttachment == null) {
            LogUtils.e(LOG_TAG, "Could not load attachment %d", mAttachmentId);
            doStatusCallback(mCallback, -1, mAttachmentId, EmailServiceStatus.ATTACHMENT_NOT_FOUND,
                    0);
            return RESULT_LOAD_ATTACHMENT_INFO_ERROR;
        }
        if (mAttachment.mLocation == null) {
            LogUtils.e(LOG_TAG, "Attachment %d lacks a location", mAttachmentId);
            doStatusCallback(mCallback, -1, mAttachmentId, EmailServiceStatus.ATTACHMENT_NOT_FOUND,
                    0);
            return RESULT_ATTACHMENT_NO_LOCATION_ERROR;
        }
        final EmailContent.Message message = EmailContent.Message
                .restoreMessageWithId(mContext, mAttachment.mMessageKey);
        if (message == null) {
            LogUtils.e(LOG_TAG, "Could not load message %d", mAttachment.mMessageKey);
            doStatusCallback(mCallback, mAttachment.mMessageKey, mAttachmentId,
                    EmailServiceStatus.MESSAGE_NOT_FOUND, 0);
            return RESULT_ATTACHMENT_LOAD_MESSAGE_ERROR;
        }

        // First callback to let the client know that we have started the attachment load.
        doStatusCallback(mCallback, mAttachment.mMessageKey, mAttachmentId,
                EmailServiceStatus.IN_PROGRESS, 0);

        final int result = super.performOperation();

        // Last callback to report results.
        if (result < 0) {
            // We had an error processing an attachment, let's report a {@link EmailServiceStatus}
            // connection error in this case
            LogUtils.d(LOG_TAG, "Invoking callback for attachmentId: %d with CONNECTION_ERROR",
                    mAttachmentId);
            doStatusCallback(mCallback, mAttachment.mMessageKey, mAttachmentId,
                    EmailServiceStatus.CONNECTION_ERROR, 0);
        } else {
            LogUtils.d(LOG_TAG, "Invoking callback for attachmentId: %d with SUCCESS",
                    mAttachmentId);
            doStatusCallback(mCallback, mAttachment.mMessageKey, mAttachmentId,
                    EmailServiceStatus.SUCCESS, 0);
        }
        return result;
    }

    @Override
    protected String getCommand() {
        if (mAttachment == null) {
            LogUtils.wtf(LOG_TAG, "Error, mAttachment is null");
        }

        final String cmd;
        if (getProtocolVersion() >= Eas.SUPPORTED_PROTOCOL_EX2010_DOUBLE) {
            // The operation is different in EAS 14.0 than in earlier versions
            cmd = "ItemOperations";
        } else {
            final String location;
            // For Exchange 2003 (EAS 2.5), we have to look for illegal chars in the file name
            // that EAS sent to us!
            if (getProtocolVersion() < Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
                location = encodeForExchange2003(mAttachment.mLocation);
            } else {
                location = mAttachment.mLocation;
            }
            cmd = "GetAttachment&AttachmentName=" + location;
        }
        return cmd;
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        if (mAttachment == null) {
            LogUtils.wtf(LOG_TAG, "Error, mAttachment is null");
        }

        final HttpEntity entity;
        final Serializer s = new Serializer();
        if (getProtocolVersion() >= Eas.SUPPORTED_PROTOCOL_EX2010_DOUBLE) {
            s.start(Tags.ITEMS_ITEMS).start(Tags.ITEMS_FETCH);
            s.data(Tags.ITEMS_STORE, "Mailbox");
            s.data(Tags.BASE_FILE_REFERENCE, mAttachment.mLocation);
            s.end().end().done(); // ITEMS_FETCH, ITEMS_ITEMS
            entity = makeEntity(s);
        } else {
            // Older versions of the protocol have the attachment location in the command.
            entity = null;
        }
        return entity;
    }

    /**
     * Close, ignoring errors (as during cleanup)
     * @param c a Closeable
     */
    private static void close(final Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
            LogUtils.e(LOG_TAG, "IOException while cleaning up attachment: %s", e.getMessage());
        }
    }

    /**
     * Save away the contentUri for this Attachment and notify listeners
     */
    private boolean finishLoadAttachment(final EmailContent.Attachment attachment, final File file) {
        final InputStream in;
        try {
            in = new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            // Unlikely, as we just created it successfully, but log it.
            LogUtils.e(LOG_TAG, "Could not open attachment file: %s", e.getMessage());
            return false;
        }
        AttachmentUtilities.saveAttachment(mContext, in, attachment);
        close(in);
        return true;
    }

    /**
     * Read the {@link EasResponse} and extract the attachment data, saving it to the provider.
     * @param response The (successful) {@link EasResponse} containing the attachment data.
     * @return A status code, 0 is a success, anything negative is an error outlined by constants
     *         in this class or its base class.
     */
    @Override
    protected int handleResponse(final EasResponse response) {
        // Some very basic error checking on the response object first.
        // Our base class should be responsible for checking these errors but if the error
        // checking is done in the override functions, we can be more specific about
        // the errors that are being returned to the caller of performOperation().
        if (response.isEmpty()) {
            LogUtils.e(LOG_TAG, "Error, empty response.");
            return RESULT_NETWORK_PROBLEM;
        }

        // This is a 2 step process.
        // 1. Grab what came over the wire and write it to a temp file on disk.
        // 2. Move the attachment to its final location.
        final File tmpFile;
        try {
            tmpFile = File.createTempFile("eas_", "tmp", mContext.getCacheDir());
        } catch (final IOException e) {
            LogUtils.e(LOG_TAG, "Could not open temp file: %s", e.getMessage());
            return RESULT_NETWORK_PROBLEM;
        }

        try {
            final OutputStream os;
            try {
                os = new FileOutputStream(tmpFile);
            } catch (final FileNotFoundException e) {
                LogUtils.e(LOG_TAG, "Temp file not found: %s", e.getMessage());
                return RESULT_ATTACHMENT_INTERNAL_HANDLING_ERROR;
            }
            try {
                final InputStream is = response.getInputStream();
                try {
                    // TODO: Right now we are explictly loading this from a class
                    // that will be deprecated when we move over to EasService. When we start using
                    // our internal class instead, there will be rippling side effect changes that
                    // need to be made when this time comes.
                    final ProgressCallback callback = new ProgressCallback(mCallback, mAttachment);
                    final boolean success;
                    if (getProtocolVersion() >= Eas.SUPPORTED_PROTOCOL_EX2010_DOUBLE) {
                        final ItemOperationsParser parser = new ItemOperationsParser(is, os,
                                mAttachment.mSize, callback);
                        parser.parse();
                        success = (parser.getStatusCode() == 1);
                    } else {
                        final int length = response.getLength();
                        if (length != 0) {
                            // len > 0 means that Content-Length was set in the headers
                            // len < 0 means "chunked" transfer-encoding
                            ItemOperationsParser.readChunked(is, os,
                                    (length < 0) ? mAttachment.mSize : length, callback);
                        }
                        success = true;
                    }
                    // Check that we successfully grabbed what came over the wire...
                    if (!success) {
                        LogUtils.e(LOG_TAG, "Error parsing server response");
                        return RESULT_ATTACHMENT_RESPONSE_PARSING_ERROR;
                    }
                    // Now finish the process and save to the final destination.
                    final boolean loadResult = finishLoadAttachment(mAttachment, tmpFile);
                    if (!loadResult) {
                        LogUtils.e(LOG_TAG, "Error post processing attachment file.");
                        return RESULT_ATTACHMENT_INTERNAL_HANDLING_ERROR;
                    }
                } catch (final IOException e) {
                    LogUtils.e(LOG_TAG, "Error handling attachment: %s", e.getMessage());
                    return RESULT_ATTACHMENT_INTERNAL_HANDLING_ERROR;
                } finally {
                    close(is);
                }
            } finally {
                close(os);
            }
        } finally {
            tmpFile.delete();
        }
        return RESULT_SUCCESS;
    }
}
