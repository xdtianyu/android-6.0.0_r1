/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.phone.vvm.omtp.imap;

import android.content.Context;
import android.net.Network;
import android.telecom.PhoneAccountHandle;
import android.telecom.Voicemail;
import android.telephony.TelephonyManager;
import android.util.Base64;

import com.android.phone.PhoneUtils;
import com.android.phone.common.mail.Address;
import com.android.phone.common.mail.Body;
import com.android.phone.common.mail.BodyPart;
import com.android.phone.common.mail.FetchProfile;
import com.android.phone.common.mail.Flag;
import com.android.phone.common.mail.Message;
import com.android.phone.common.mail.MessagingException;
import com.android.phone.common.mail.Multipart;
import com.android.phone.common.mail.TempDirectory;
import com.android.phone.common.mail.internet.MimeMessage;
import com.android.phone.common.mail.store.ImapFolder;
import com.android.phone.common.mail.store.ImapStore;
import com.android.phone.common.mail.store.imap.ImapConstants;
import com.android.phone.common.mail.utils.LogUtils;
import com.android.phone.settings.VisualVoicemailSettingsUtil;
import com.android.phone.vvm.omtp.OmtpConstants;
import com.android.phone.vvm.omtp.OmtpVvmCarrierConfigHelper;
import com.android.phone.vvm.omtp.fetch.VoicemailFetchedCallback;

import libcore.io.IoUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A helper interface to abstract commands sent across IMAP interface for a given account.
 */
public class ImapHelper {
    private final String TAG = "ImapHelper";

    private ImapFolder mFolder;
    private ImapStore mImapStore;
    private Context mContext;
    private PhoneAccountHandle mPhoneAccount;

    public ImapHelper(Context context, PhoneAccountHandle phoneAccount, Network network) {
        try {
            mContext = context;
            mPhoneAccount = phoneAccount;
            TempDirectory.setTempDirectory(context);

            String username = VisualVoicemailSettingsUtil.getVisualVoicemailCredentials(context,
                    OmtpConstants.IMAP_USER_NAME, phoneAccount);
            String password = VisualVoicemailSettingsUtil.getVisualVoicemailCredentials(context,
                    OmtpConstants.IMAP_PASSWORD, phoneAccount);
            String serverName = VisualVoicemailSettingsUtil.getVisualVoicemailCredentials(context,
                    OmtpConstants.SERVER_ADDRESS, phoneAccount);
            int port = Integer.parseInt(
                    VisualVoicemailSettingsUtil.getVisualVoicemailCredentials(context,
                            OmtpConstants.IMAP_PORT, phoneAccount));
            int auth = ImapStore.FLAG_NONE;

            OmtpVvmCarrierConfigHelper carrierConfigHelper = new OmtpVvmCarrierConfigHelper(context,
                    PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccount));
            if (TelephonyManager.VVM_TYPE_CVVM.equals(carrierConfigHelper.getVvmType())) {
                // TODO: move these into the carrier config app
                port = 993;
                auth = ImapStore.FLAG_SSL;
            }

            mImapStore = new ImapStore(
                    context, username, password, port, serverName, auth, network);
        } catch (NumberFormatException e) {
            LogUtils.w(TAG, "Could not parse port number");
        }
    }

    /**
     * If mImapStore is null, this means that there was a missing or badly formatted port number,
     * which means there aren't sufficient credentials for login. If mImapStore is succcessfully
     * initialized, then ImapHelper is ready to go.
     */
    public boolean isSuccessfullyInitialized() {
        return mImapStore != null;
    }

    /** The caller thread will block until the method returns. */
    public boolean markMessagesAsRead(List<Voicemail> voicemails) {
        return setFlags(voicemails, Flag.SEEN);
    }

    /** The caller thread will block until the method returns. */
    public boolean markMessagesAsDeleted(List<Voicemail> voicemails) {
        return setFlags(voicemails, Flag.DELETED);
    }

    /**
     * Set flags on the server for a given set of voicemails.
     *
     * @param voicemails The voicemails to set flags for.
     * @param flags The flags to set on the voicemails.
     * @return {@code true} if the operation completes successfully, {@code false} otherwise.
     */
    private boolean setFlags(List<Voicemail> voicemails, String... flags) {
        if (voicemails.size() == 0) {
            return false;
        }
        try {
            mFolder = openImapFolder(ImapFolder.MODE_READ_WRITE);
            if (mFolder != null) {
                mFolder.setFlags(convertToImapMessages(voicemails), flags, true);
                return true;
            }
            return false;
        } catch (MessagingException e) {
            LogUtils.e(TAG, e, "Messaging exception");
            return false;
        } finally {
            closeImapFolder();
        }
    }

    /**
     * Fetch a list of voicemails from the server.
     *
     * @return A list of voicemail objects containing data about voicemails stored on the server.
     */
    public List<Voicemail> fetchAllVoicemails() {
        List<Voicemail> result = new ArrayList<Voicemail>();
        Message[] messages;
        try {
            mFolder = openImapFolder(ImapFolder.MODE_READ_WRITE);
            if (mFolder == null) {
                // This means we were unable to successfully open the folder.
                return null;
            }

            // This method retrieves lightweight messages containing only the uid of the message.
            messages = mFolder.getMessages(null);

            for (Message message : messages) {
                // Get the voicemail details.
                Voicemail voicemail = fetchVoicemail(message);
                if (voicemail != null) {
                    result.add(voicemail);
                }
            }
            return result;
        } catch (MessagingException e) {
            LogUtils.e(TAG, e, "Messaging Exception");
            return null;
        } finally {
            closeImapFolder();
        }
    }

    /**
     * Fetches the structure of the given message and returns the voicemail parsed from it.
     *
     * @throws MessagingException if fetching the structure of the message fails
     */
    private Voicemail fetchVoicemail(Message message)
            throws MessagingException {
        LogUtils.d(TAG, "Fetching message structure for " + message.getUid());

        MessageStructureFetchedListener listener = new MessageStructureFetchedListener();

        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.addAll(Arrays.asList(FetchProfile.Item.FLAGS, FetchProfile.Item.ENVELOPE,
                FetchProfile.Item.STRUCTURE));

        // The IMAP folder fetch method will call "messageRetrieved" on the listener when the
        // message is successfully retrieved.
        mFolder.fetch(new Message[] {message}, fetchProfile, listener);
        return listener.getVoicemail();
    }


    public boolean fetchVoicemailPayload(VoicemailFetchedCallback callback, final String uid) {
        Message message;
        try {
            mFolder = openImapFolder(ImapFolder.MODE_READ_WRITE);
            if (mFolder == null) {
                // This means we were unable to successfully open the folder.
                return false;
            }
            message = mFolder.getMessage(uid);
            VoicemailPayload voicemailPayload = fetchVoicemailPayload(message);

            if (voicemailPayload == null) {
                return false;
            }

            callback.setVoicemailContent(voicemailPayload);
            return true;
        } catch (MessagingException e) {
        } finally {
            closeImapFolder();
        }
        return false;
    }

    /**
     * Fetches the body of the given message and returns the parsed voicemail payload.
     *
     * @throws MessagingException if fetching the body of the message fails
     */
    private VoicemailPayload fetchVoicemailPayload(Message message)
            throws MessagingException {
        LogUtils.d(TAG, "Fetching message body for " + message.getUid());

        MessageBodyFetchedListener listener = new MessageBodyFetchedListener();

        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.BODY);

        mFolder.fetch(new Message[] {message}, fetchProfile, listener);
        return listener.getVoicemailPayload();
    }

    /**
     * Listener for the message structure being fetched.
     */
    private final class MessageStructureFetchedListener
            implements ImapFolder.MessageRetrievalListener {
        private Voicemail mVoicemail;

        public MessageStructureFetchedListener() {
        }

        public Voicemail getVoicemail() {
            return mVoicemail;
        }

        @Override
        public void messageRetrieved(Message message) {
            LogUtils.d(TAG, "Fetched message structure for " + message.getUid());
            LogUtils.d(TAG, "Message retrieved: " + message);
            try {
                mVoicemail = getVoicemailFromMessage(message);
                if (mVoicemail == null) {
                    LogUtils.d(TAG, "This voicemail does not have an attachment...");
                    return;
                }
            } catch (MessagingException e) {
                LogUtils.e(TAG, e, "Messaging Exception");
                closeImapFolder();
            }
        }

        /**
         * Convert an IMAP message to a voicemail object.
         *
         * @param message The IMAP message.
         * @return The voicemail object corresponding to an IMAP message.
         * @throws MessagingException
         */
        private Voicemail getVoicemailFromMessage(Message message) throws MessagingException {
            if (!message.getMimeType().startsWith("multipart/")) {
                LogUtils.w(TAG, "Ignored non multi-part message");
                return null;
            }

            Multipart multipart = (Multipart) message.getBody();
            for (int i = 0; i < multipart.getCount(); ++i) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String bodyPartMimeType = bodyPart.getMimeType().toLowerCase();
                LogUtils.d(TAG, "bodyPart mime type: " + bodyPartMimeType);

                if (bodyPartMimeType.startsWith("audio/")) {
                    // Found an audio attachment, this is a valid voicemail.
                    long time = message.getSentDate().getTime();
                    String number = getNumber(message.getFrom());
                    boolean isRead = Arrays.asList(message.getFlags()).contains(Flag.SEEN);

                    return Voicemail.createForInsertion(time, number)
                            .setPhoneAccount(mPhoneAccount)
                            .setSourcePackage(mContext.getPackageName())
                            .setSourceData(message.getUid())
                            .setIsRead(isRead)
                            .build();
                }
            }
            // No attachment found, this is not a voicemail.
            return null;
        }

        /**
         * The "from" field of a visual voicemail IMAP message is the number of the caller who left
         * the message. Extract this number from the list of "from" addresses.
         *
         * @param fromAddresses A list of addresses that comprise the "from" line.
         * @return The number of the voicemail sender.
         */
        private String getNumber(Address[] fromAddresses) {
            if (fromAddresses != null && fromAddresses.length > 0) {
                if (fromAddresses.length != 1) {
                    LogUtils.w(TAG, "More than one from addresses found. Using the first one.");
                }
                String sender = fromAddresses[0].getAddress();
                int atPos = sender.indexOf('@');
                if (atPos != -1) {
                    // Strip domain part of the address.
                    sender = sender.substring(0, atPos);
                }
                return sender;
            }
            return null;
        }
    }

    /**
     * Listener for the message body being fetched.
     */
    private final class MessageBodyFetchedListener implements ImapFolder.MessageRetrievalListener {
        private VoicemailPayload mVoicemailPayload;

        /** Returns the fetch voicemail payload. */
        public VoicemailPayload getVoicemailPayload() {
            return mVoicemailPayload;
        }

        @Override
        public void messageRetrieved(Message message) {
            LogUtils.d(TAG, "Fetched message body for " + message.getUid());
            LogUtils.d(TAG, "Message retrieved: " + message);
            try {
                mVoicemailPayload = getVoicemailPayloadFromMessage(message);
            } catch (MessagingException e) {
                LogUtils.e(TAG, "Messaging Exception:", e);
            } catch (IOException e) {
                LogUtils.e(TAG, "IO Exception:", e);
            }
        }

        private VoicemailPayload getVoicemailPayloadFromMessage(Message message)
                throws MessagingException, IOException {
            Multipart multipart = (Multipart) message.getBody();
            for (int i = 0; i < multipart.getCount(); ++i) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String bodyPartMimeType = bodyPart.getMimeType().toLowerCase();
                LogUtils.d(TAG, "bodyPart mime type: " + bodyPartMimeType);

                if (bodyPartMimeType.startsWith("audio/")) {
                    byte[] bytes = getAudioDataFromBody(bodyPart.getBody());
                    LogUtils.d(TAG, String.format("Fetched %s bytes of data", bytes.length));
                    return new VoicemailPayload(bodyPartMimeType, bytes);
                }
            }
            LogUtils.e(TAG, "No audio attachment found on this voicemail");
            return null;
        }

        private byte[] getAudioDataFromBody(Body body) throws IOException, MessagingException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BufferedOutputStream bufferedOut = new BufferedOutputStream(out);
            try {
                body.writeTo(bufferedOut);
            } finally {
                IoUtils.closeQuietly(bufferedOut);
            }
            return Base64.decode(out.toByteArray(), Base64.DEFAULT);
        }
    }

    private ImapFolder openImapFolder(String modeReadWrite) {
        try {
            if (mImapStore == null) {
                return null;
            }
            ImapFolder folder = new ImapFolder(mImapStore, ImapConstants.INBOX);
            folder.open(modeReadWrite);
            return folder;
        } catch (MessagingException e) {
            LogUtils.e(TAG, e, "Messaging Exception");
        }
        return null;
    }

    private Message[] convertToImapMessages(List<Voicemail> voicemails) {
        Message[] messages = new Message[voicemails.size()];
        for (int i = 0; i < voicemails.size(); ++i) {
            messages[i] = new MimeMessage();
            messages[i].setUid(voicemails.get(i).getSourceData());
        }
        return messages;
    }

    private void closeImapFolder() {
        if (mFolder != null) {
            mFolder.close(true);
        }
    }
}