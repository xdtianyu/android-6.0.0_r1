package com.android.exchange.adapter;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.android.emailcommon.internet.MimeMessage;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.mail.MeetingInfo;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.mail.PackedString;
import com.android.emailcommon.mail.Part;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.Policy;
import com.android.emailcommon.provider.ProviderUnavailableException;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.emailcommon.utility.ConversionUtilities;
import com.android.emailcommon.utility.TextUtilities;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.utility.CalendarUtilities;
import com.android.mail.utils.LogUtils;
import com.google.common.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for Sync on an email collection.
 */
public class EmailSyncParser extends AbstractSyncParser {
    private static final String TAG = Eas.LOG_TAG;

    private static final String WHERE_SERVER_ID_AND_MAILBOX_KEY = SyncColumns.SERVER_ID
            + "=? and " + MessageColumns.MAILBOX_KEY + "=?";

    private final String mMailboxIdAsString;

    private final ArrayList<EmailContent.Message>
            newEmails = new ArrayList<EmailContent.Message>();
    private final ArrayList<EmailContent.Message> fetchedEmails =
            new ArrayList<EmailContent.Message>();
    private final ArrayList<Long> deletedEmails = new ArrayList<Long>();
    private final ArrayList<ServerChange> changedEmails = new ArrayList<ServerChange>();

    private static final int MESSAGE_ID_SUBJECT_ID_COLUMN = 0;
    private static final int MESSAGE_ID_SUBJECT_SUBJECT_COLUMN = 1;
    private static final String[] MESSAGE_ID_SUBJECT_PROJECTION =
            new String[] { MessageColumns._ID, MessageColumns.SUBJECT };

    @VisibleForTesting
    static final int LAST_VERB_REPLY = 1;
    @VisibleForTesting
    static final int LAST_VERB_REPLY_ALL = 2;
    @VisibleForTesting
    static final int LAST_VERB_FORWARD = 3;

    private final Policy mPolicy;

    // Max times to retry when we get a TransactionTooLargeException exception
    private static final int MAX_RETRIES = 10;

    // Max number of ops per batch. It could end up more than this but once we detect we are at or
    // above this number, we flush.
    private static final int MAX_OPS_PER_BATCH = 50;

    private boolean mFetchNeeded = false;

    private final Map<String, Integer> mMessageUpdateStatus = new HashMap();

    public EmailSyncParser(final Context context, final ContentResolver resolver,
            final InputStream in, final Mailbox mailbox, final Account account)
            throws IOException {
        super(context, resolver, in, mailbox, account);
        mMailboxIdAsString = Long.toString(mMailbox.mId);
        if (mAccount.mPolicyKey != 0) {
            mPolicy = Policy.restorePolicyWithId(mContext, mAccount.mPolicyKey);
        } else {
            mPolicy = null;
        }
    }

    public EmailSyncParser(final Parser parser, final Context context,
            final ContentResolver resolver, final Mailbox mailbox, final Account account)
                    throws IOException {
        super(parser, context, resolver, mailbox, account);
        mMailboxIdAsString = Long.toString(mMailbox.mId);
        if (mAccount.mPolicyKey != 0) {
            mPolicy = Policy.restorePolicyWithId(mContext, mAccount.mPolicyKey);
        } else {
            mPolicy = null;
        }
    }

    public EmailSyncParser(final Context context, final InputStream in, final Mailbox mailbox,
            final Account account) throws IOException {
        this(context, context.getContentResolver(), in, mailbox, account);
    }

    public boolean fetchNeeded() {
        return mFetchNeeded;
    }

    public Map<String, Integer> getMessageStatuses() {
        return mMessageUpdateStatus;
    }

    public void addData(EmailContent.Message msg, int endingTag) throws IOException {
        ArrayList<EmailContent.Attachment> atts = new ArrayList<EmailContent.Attachment>();
        boolean truncated = false;

        while (nextTag(endingTag) != END) {
            switch (tag) {
                case Tags.EMAIL_ATTACHMENTS:
                case Tags.BASE_ATTACHMENTS: // BASE_ATTACHMENTS is used in EAS 12.0 and up
                    attachmentsParser(atts, msg, tag);
                    break;
                case Tags.EMAIL_TO:
                    msg.mTo = Address.toString(Address.parse(getValue()));
                    break;
                case Tags.EMAIL_FROM:
                    Address[] froms = Address.parse(getValue());
                    if (froms != null && froms.length > 0) {
                        msg.mDisplayName = froms[0].toFriendly();
                    }
                    msg.mFrom = Address.toString(froms);
                    break;
                case Tags.EMAIL_CC:
                    msg.mCc = Address.toString(Address.parse(getValue()));
                    break;
                case Tags.EMAIL_REPLY_TO:
                    msg.mReplyTo = Address.toString(Address.parse(getValue()));
                    break;
                case Tags.EMAIL_DATE_RECEIVED:
                    try {
                        msg.mTimeStamp = Utility.parseEmailDateTimeToMillis(getValue());
                    } catch (ParseException e) {
                        LogUtils.w(TAG, "Parse error for EMAIL_DATE_RECEIVED tag.", e);
                    }
                    break;
                case Tags.EMAIL_SUBJECT:
                    msg.mSubject = getValue();
                    break;
                case Tags.EMAIL_READ:
                    msg.mFlagRead = getValueInt() == 1;
                    break;
                case Tags.BASE_BODY:
                    bodyParser(msg);
                    break;
                case Tags.EMAIL_FLAG:
                    msg.mFlagFavorite = flagParser();
                    break;
                case Tags.EMAIL_MIME_TRUNCATED:
                    truncated = getValueInt() == 1;
                    break;
                case Tags.EMAIL_MIME_DATA:
                    // We get MIME data for EAS 2.5.  First we parse it, then we take the
                    // html and/or plain text data and store it in the message
                    if (truncated) {
                        // If the MIME data is truncated, don't bother parsing it, because
                        // it will take time and throw an exception anyway when EOF is reached
                        // In this case, we will load the body separately by tagging the message
                        // "partially loaded".
                        // Get the data (and ignore it)
                        getValue();
                        userLog("Partially loaded: ", msg.mServerId);
                        msg.mFlagLoaded = EmailContent.Message.FLAG_LOADED_PARTIAL;
                        mFetchNeeded = true;
                    } else {
                        mimeBodyParser(msg, getValue());
                    }
                    break;
                case Tags.EMAIL_BODY:
                    String text = getValue();
                    msg.mText = text;
                    break;
                case Tags.EMAIL_MESSAGE_CLASS:
                    String messageClass = getValue();
                    if (messageClass.equals("IPM.Schedule.Meeting.Request")) {
                        msg.mFlags |= EmailContent.Message.FLAG_INCOMING_MEETING_INVITE;
                    } else if (messageClass.equals("IPM.Schedule.Meeting.Canceled")) {
                        msg.mFlags |= EmailContent.Message.FLAG_INCOMING_MEETING_CANCEL;
                    }
                    break;
                case Tags.EMAIL_MEETING_REQUEST:
                    meetingRequestParser(msg);
                    break;
                case Tags.EMAIL_THREAD_TOPIC:
                    msg.mThreadTopic = getValue();
                    break;
                case Tags.RIGHTS_LICENSE:
                    skipParser(tag);
                    break;
                case Tags.EMAIL2_CONVERSATION_ID:
                    msg.mServerConversationId =
                            Base64.encodeToString(getValueBytes(), Base64.URL_SAFE);
                    break;
                case Tags.EMAIL2_CONVERSATION_INDEX:
                    // Ignore this byte array since we're not constructing a tree.
                    getValueBytes();
                    break;
                case Tags.EMAIL2_LAST_VERB_EXECUTED:
                    int val = getValueInt();
                    if (val == LAST_VERB_REPLY || val == LAST_VERB_REPLY_ALL) {
                        // We aren't required to distinguish between reply and reply all here
                        msg.mFlags |= EmailContent.Message.FLAG_REPLIED_TO;
                    } else if (val == LAST_VERB_FORWARD) {
                        msg.mFlags |= EmailContent.Message.FLAG_FORWARDED;
                    }
                    break;
                default:
                    skipTag();
            }
        }

        if (atts.size() > 0) {
            msg.mAttachments = atts;
        }

        if ((msg.mFlags & EmailContent.Message.FLAG_INCOMING_MEETING_MASK) != 0) {
            String text = TextUtilities.makeSnippetFromHtmlText(
                    msg.mText != null ? msg.mText : msg.mHtml);
            if (TextUtils.isEmpty(text)) {
                // Create text for this invitation
                String meetingInfo = msg.mMeetingInfo;
                if (!TextUtils.isEmpty(meetingInfo)) {
                    PackedString ps = new PackedString(meetingInfo);
                    ContentValues values = new ContentValues();
                    putFromMeeting(ps, MeetingInfo.MEETING_LOCATION, values,
                            CalendarContract.Events.EVENT_LOCATION);
                    String dtstart = ps.get(MeetingInfo.MEETING_DTSTART);
                    if (!TextUtils.isEmpty(dtstart)) {
                        try {
                            final long startTime =
                                Utility.parseEmailDateTimeToMillis(dtstart);
                            values.put(CalendarContract.Events.DTSTART, startTime);
                        } catch (ParseException e) {
                            LogUtils.w(TAG, "Parse error for MEETING_DTSTART tag.", e);
                        }
                    }
                    putFromMeeting(ps, MeetingInfo.MEETING_ALL_DAY, values,
                            CalendarContract.Events.ALL_DAY);
                    msg.mText = CalendarUtilities.buildMessageTextFromEntityValues(
                            mContext, values, null);
                    msg.mHtml = Html.toHtml(new SpannedString(msg.mText));
                }
            }
        }
    }

    private static void putFromMeeting(PackedString ps, String field, ContentValues values,
            String column) {
        String val = ps.get(field);
        if (!TextUtils.isEmpty(val)) {
            values.put(column, val);
        }
    }

    /**
     * Set up the meetingInfo field in the message with various pieces of information gleaned
     * from MeetingRequest tags.  This information will be used later to generate an appropriate
     * reply email if the user chooses to respond
     * @param msg the Message being built
     * @throws IOException
     */
    private void meetingRequestParser(EmailContent.Message msg) throws IOException {
        PackedString.Builder packedString = new PackedString.Builder();
        while (nextTag(Tags.EMAIL_MEETING_REQUEST) != END) {
            switch (tag) {
                case Tags.EMAIL_DTSTAMP:
                    packedString.put(MeetingInfo.MEETING_DTSTAMP, getValue());
                    break;
                case Tags.EMAIL_START_TIME:
                    packedString.put(MeetingInfo.MEETING_DTSTART, getValue());
                    break;
                case Tags.EMAIL_END_TIME:
                    packedString.put(MeetingInfo.MEETING_DTEND, getValue());
                    break;
                case Tags.EMAIL_ORGANIZER:
                    packedString.put(MeetingInfo.MEETING_ORGANIZER_EMAIL, getValue());
                    break;
                case Tags.EMAIL_LOCATION:
                    packedString.put(MeetingInfo.MEETING_LOCATION, getValue());
                    break;
                case Tags.EMAIL_GLOBAL_OBJID:
                    packedString.put(MeetingInfo.MEETING_UID,
                            CalendarUtilities.getUidFromGlobalObjId(getValue()));
                    break;
                case Tags.EMAIL_CATEGORIES:
                    skipParser(tag);
                    break;
                case Tags.EMAIL_RECURRENCES:
                    recurrencesParser();
                    break;
                case Tags.EMAIL_RESPONSE_REQUESTED:
                    packedString.put(MeetingInfo.MEETING_RESPONSE_REQUESTED, getValue());
                    break;
                case Tags.EMAIL_ALL_DAY_EVENT:
                    if (getValueInt() == 1) {
                        packedString.put(MeetingInfo.MEETING_ALL_DAY, "1");
                    }
                    break;
                default:
                    skipTag();
            }
        }
        if (msg.mSubject != null) {
            packedString.put(MeetingInfo.MEETING_TITLE, msg.mSubject);
        }
        msg.mMeetingInfo = packedString.toString();
    }

    private void recurrencesParser() throws IOException {
        while (nextTag(Tags.EMAIL_RECURRENCES) != END) {
            switch (tag) {
                case Tags.EMAIL_RECURRENCE:
                    skipParser(tag);
                    break;
                default:
                    skipTag();
            }
        }
    }

    /**
     * Parse a message from the server stream.
     * @return the parsed Message
     * @throws IOException
     */
    private EmailContent.Message addParser(final int endingTag) throws IOException, CommandStatusException {
        EmailContent.Message msg = new EmailContent.Message();
        msg.mAccountKey = mAccount.mId;
        msg.mMailboxKey = mMailbox.mId;
        msg.mFlagLoaded = EmailContent.Message.FLAG_LOADED_COMPLETE;
        // Default to 1 (success) in case we don't get this tag
        int status = 1;

        while (nextTag(endingTag) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    msg.mServerId = getValue();
                    break;
                case Tags.SYNC_STATUS:
                    status = getValueInt();
                    break;
                case Tags.SYNC_APPLICATION_DATA:
                    addData(msg, tag);
                    break;
                default:
                    skipTag();
            }
        }
        // For sync, status 1 = success
        if (status != 1) {
            throw new CommandStatusException(status, msg.mServerId);
        }
        return msg;
    }

    // For now, we only care about the "active" state
    private Boolean flagParser() throws IOException {
        Boolean state = false;
        while (nextTag(Tags.EMAIL_FLAG) != END) {
            switch (tag) {
                case Tags.EMAIL_FLAG_STATUS:
                    state = getValueInt() == 2;
                    break;
                default:
                    skipTag();
            }
        }
        return state;
    }

    private void bodyParser(EmailContent.Message msg) throws IOException {
        String bodyType = Eas.BODY_PREFERENCE_TEXT;
        String body = "";
        while (nextTag(Tags.BASE_BODY) != END) {
            switch (tag) {
                case Tags.BASE_TYPE:
                    bodyType = getValue();
                    break;
                case Tags.BASE_DATA:
                    body = getValue();
                    break;
                default:
                    skipTag();
            }
        }
        // We always ask for TEXT or HTML; there's no third option
        if (bodyType.equals(Eas.BODY_PREFERENCE_HTML)) {
            msg.mHtml = body;
        } else {
            msg.mText = body;
        }
    }

    /**
     * Parses untruncated MIME data, saving away the text parts
     * @param msg the message we're building
     * @param mimeData the MIME data we've received from the server
     * @throws IOException
     */
    private static void mimeBodyParser(EmailContent.Message msg, String mimeData)
            throws IOException {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(mimeData.getBytes());
            // The constructor parses the message
            MimeMessage mimeMessage = new MimeMessage(in);
            // Now process body parts & attachments
            ArrayList<Part> viewables = new ArrayList<Part>();
            // We'll ignore the attachments, as we'll get them directly from EAS
            ArrayList<Part> attachments = new ArrayList<Part>();
            MimeUtility.collectParts(mimeMessage, viewables, attachments);
            // parseBodyFields fills in the content fields of the Body
            ConversionUtilities.BodyFieldData data =
                    ConversionUtilities.parseBodyFields(viewables);
            // But we need them in the message itself for handling during commit()
            msg.setFlags(data.isQuotedReply, data.isQuotedForward);
            msg.mSnippet = data.snippet;
            msg.mHtml = data.htmlContent;
            msg.mText = data.textContent;
        } catch (MessagingException e) {
            // This would most likely indicate a broken stream
            throw new IOException(e);
        }
    }

    private void attachmentsParser(final ArrayList<EmailContent.Attachment> atts,
            final EmailContent.Message msg, final int endingTag) throws IOException {
        while (nextTag(endingTag) != END) {
            switch (tag) {
                case Tags.EMAIL_ATTACHMENT:
                case Tags.BASE_ATTACHMENT:  // BASE_ATTACHMENT is used in EAS 12.0 and up
                    attachmentParser(atts, msg, tag);
                    break;
                default:
                    skipTag();
            }
        }
    }

    private void attachmentParser(final ArrayList<EmailContent.Attachment> atts,
            final EmailContent.Message msg, final int endingTag) throws IOException {
        String fileName = null;
        String length = null;
        String location = null;
        boolean isInline = false;
        String contentId = null;

        while (nextTag(endingTag) != END) {
            switch (tag) {
                // We handle both EAS 2.5 and 12.0+ attachments here
                case Tags.EMAIL_DISPLAY_NAME:
                case Tags.BASE_DISPLAY_NAME:
                    fileName = getValue();
                    break;
                case Tags.EMAIL_ATT_NAME:
                case Tags.BASE_FILE_REFERENCE:
                    location = getValue();
                    break;
                case Tags.EMAIL_ATT_SIZE:
                case Tags.BASE_ESTIMATED_DATA_SIZE:
                    length = getValue();
                    break;
                case Tags.BASE_IS_INLINE:
                    isInline = getValueInt() == 1;
                    break;
                case Tags.BASE_CONTENT_ID:
                    contentId = getValue();
                    break;
                default:
                    skipTag();
            }
        }

        if ((fileName != null) && (length != null) && (location != null)) {
            EmailContent.Attachment att = new EmailContent.Attachment();
            att.mEncoding = "base64";
            att.mSize = Long.parseLong(length);
            att.mFileName = fileName;
            att.mLocation = location;
            att.mMimeType = getMimeTypeFromFileName(fileName);
            att.mAccountKey = mAccount.mId;
            // Save away the contentId, if we've got one (for inline images); note that the
            // EAS docs appear to be wrong about the tags used; inline images come with
            // contentId rather than contentLocation, when sent from Ex03, Ex07, and Ex10
            if (isInline && !TextUtils.isEmpty(contentId)) {
                att.mContentId = contentId;
            }
            // Check if this attachment can't be downloaded due to an account policy
            if (mPolicy != null) {
                if (mPolicy.mDontAllowAttachments ||
                        (mPolicy.mMaxAttachmentSize > 0 &&
                                (att.mSize > mPolicy.mMaxAttachmentSize))) {
                    att.mFlags = EmailContent.Attachment.FLAG_POLICY_DISALLOWS_DOWNLOAD;
                }
            }
            atts.add(att);
            msg.mFlagAttachment = true;
        }
    }

    /**
     * Returns an appropriate mimetype for the given file name's extension. If a mimetype
     * cannot be determined, {@code application/<<x>>} [where @{code <<x>> is the extension,
     * if it exists or {@code application/octet-stream}].
     * At the moment, this is somewhat lame, since many file types aren't recognized
     * @param fileName the file name to ponder
     */
    // Note: The MimeTypeMap method currently uses a very limited set of mime types
    // A bug has been filed against this issue.
    public String getMimeTypeFromFileName(String fileName) {
        String mimeType;
        int lastDot = fileName.lastIndexOf('.');
        String extension = null;
        if ((lastDot > 0) && (lastDot < fileName.length() - 1)) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        if (extension == null) {
            // A reasonable default for now.
            mimeType = "application/octet-stream";
        } else {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType == null) {
                mimeType = "application/" + extension;
            }
        }
        return mimeType;
    }

    private Cursor getServerIdCursor(String serverId, String[] projection) {
        Cursor c = mContentResolver.query(EmailContent.Message.CONTENT_URI, projection,
                WHERE_SERVER_ID_AND_MAILBOX_KEY, new String[] {serverId, mMailboxIdAsString},
                null);
        if (c == null) throw new ProviderUnavailableException();
        if (c.getCount() > 1) {
            userLog("Multiple messages with the same serverId/mailbox: " + serverId);
        }
        return c;
    }

    @VisibleForTesting
    void deleteParser(ArrayList<Long> deletes, int entryTag) throws IOException {
        while (nextTag(entryTag) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    String serverId = getValue();
                    // Find the message in this mailbox with the given serverId
                    Cursor c = getServerIdCursor(serverId, MESSAGE_ID_SUBJECT_PROJECTION);
                    try {
                        if (c.moveToFirst()) {
                            deletes.add(c.getLong(MESSAGE_ID_SUBJECT_ID_COLUMN));
                            if (Eas.USER_LOG) {
                                userLog("Deleting ", serverId + ", "
                                        + c.getString(MESSAGE_ID_SUBJECT_SUBJECT_COLUMN));
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

    @VisibleForTesting
    class ServerChange {
        final long id;
        final Boolean read;
        final Boolean flag;
        final Integer flags;

        ServerChange(long _id, Boolean _read, Boolean _flag, Integer _flags) {
            id = _id;
            read = _read;
            flag = _flag;
            flags = _flags;
        }
    }

    @VisibleForTesting
    void changeParser(ArrayList<ServerChange> changes) throws IOException {
        String serverId = null;
        Boolean oldRead = false;
        Boolean oldFlag = false;
        int flags = 0;
        long id = 0;
        while (nextTag(Tags.SYNC_CHANGE) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    serverId = getValue();
                    Cursor c = getServerIdCursor(serverId, EmailContent.Message.LIST_PROJECTION);
                    try {
                        if (c.moveToFirst()) {
                            userLog("Changing ", serverId);
                            oldRead = c.getInt(EmailContent.Message.LIST_READ_COLUMN)
                                    == EmailContent.Message.READ;
                            oldFlag = c.getInt(EmailContent.Message.LIST_FAVORITE_COLUMN) == 1;
                            flags = c.getInt(EmailContent.Message.LIST_FLAGS_COLUMN);
                            id = c.getLong(EmailContent.Message.LIST_ID_COLUMN);
                        }
                    } finally {
                        c.close();
                    }
                    break;
                case Tags.SYNC_APPLICATION_DATA:
                    changeApplicationDataParser(changes, oldRead, oldFlag, flags, id);
                    break;
                default:
                    skipTag();
            }
        }
    }

    private void changeApplicationDataParser(ArrayList<ServerChange> changes, Boolean oldRead,
            Boolean oldFlag, int oldFlags, long id) throws IOException {
        Boolean read = null;
        Boolean flag = null;
        Integer flags = null;
        while (nextTag(Tags.SYNC_APPLICATION_DATA) != END) {
            switch (tag) {
                case Tags.EMAIL_READ:
                    read = getValueInt() == 1;
                    break;
                case Tags.EMAIL_FLAG:
                    flag = flagParser();
                    break;
                case Tags.EMAIL2_LAST_VERB_EXECUTED:
                    int val = getValueInt();
                    // Clear out the old replied/forward flags and add in the new flag
                    flags = oldFlags & ~(EmailContent.Message.FLAG_REPLIED_TO
                            | EmailContent.Message.FLAG_FORWARDED);
                    if (val == LAST_VERB_REPLY || val == LAST_VERB_REPLY_ALL) {
                        // We aren't required to distinguish between reply and reply all here
                        flags |= EmailContent.Message.FLAG_REPLIED_TO;
                    } else if (val == LAST_VERB_FORWARD) {
                        flags |= EmailContent.Message.FLAG_FORWARDED;
                    }
                    break;
                default:
                    skipTag();
            }
        }
        // See if there are flag changes re: read, flag (favorite) or replied/forwarded
        if (((read != null) && !oldRead.equals(read)) ||
                ((flag != null) && !oldFlag.equals(flag)) || (flags != null)) {
            changes.add(new ServerChange(id, read, flag, flags));
        }
    }

    /* (non-Javadoc)
     * @see com.android.exchange.adapter.EasContentParser#commandsParser()
     */
    @Override
    public void commandsParser() throws IOException, CommandStatusException {
        while (nextTag(Tags.SYNC_COMMANDS) != END) {
            if (tag == Tags.SYNC_ADD) {
                newEmails.add(addParser(tag));
            } else if (tag == Tags.SYNC_DELETE || tag == Tags.SYNC_SOFT_DELETE) {
                deleteParser(deletedEmails, tag);
            } else if (tag == Tags.SYNC_CHANGE) {
                changeParser(changedEmails);
            } else
                skipTag();
        }
    }

    // EAS values for status element of sync responses.
    // TODO: Not all are used yet, but I wanted to transcribe all possible values.
    public static final int EAS_SYNC_STATUS_SUCCESS = 1;
    public static final int EAS_SYNC_STATUS_BAD_SYNC_KEY = 3;
    public static final int EAS_SYNC_STATUS_PROTOCOL_ERROR = 4;
    public static final int EAS_SYNC_STATUS_SERVER_ERROR = 5;
    public static final int EAS_SYNC_STATUS_BAD_CLIENT_DATA = 6;
    public static final int EAS_SYNC_STATUS_CONFLICT = 7;
    public static final int EAS_SYNC_STATUS_OBJECT_NOT_FOUND = 8;
    public static final int EAS_SYNC_STATUS_CANNOT_COMPLETE = 9;
    public static final int EAS_SYNC_STATUS_FOLDER_SYNC_NEEDED = 12;
    public static final int EAS_SYNC_STATUS_INCOMPLETE_REQUEST = 13;
    public static final int EAS_SYNC_STATUS_BAD_HEARTBEAT_VALUE = 14;
    public static final int EAS_SYNC_STATUS_TOO_MANY_COLLECTIONS = 15;
    public static final int EAS_SYNC_STATUS_RETRY = 16;

    public static boolean shouldRetry(final int status) {
        return status == EAS_SYNC_STATUS_SERVER_ERROR || status == EAS_SYNC_STATUS_RETRY;
    }

    /**
     * Parse the status for a single message update.
     * @param endTag the tag we end with
     * @throws IOException
     */
    public void messageUpdateParser(int endTag) throws IOException {
        // We get serverId and status in the responses
        String serverId = null;
        int status = -1;
        while (nextTag(endTag) != END) {
            if (tag == Tags.SYNC_STATUS) {
                status = getValueInt();
            } else if (tag == Tags.SYNC_SERVER_ID) {
                serverId = getValue();
            } else {
                skipTag();
            }
        }
        if (serverId != null && status != -1) {
            mMessageUpdateStatus.put(serverId, status);
        }
    }

    @Override
    public void responsesParser() throws IOException {
        while (nextTag(Tags.SYNC_RESPONSES) != END) {
            if (tag == Tags.SYNC_ADD || tag == Tags.SYNC_CHANGE || tag == Tags.SYNC_DELETE) {
                messageUpdateParser(tag);
            } else if (tag == Tags.SYNC_FETCH) {
                try {
                    fetchedEmails.add(addParser(tag));
                } catch (CommandStatusException sse) {
                    if (sse.mStatus == 8) {
                        // 8 = object not found; delete the message from EmailProvider
                        // No other status should be seen in a fetch response, except, perhaps,
                        // for some temporary server failure
                        mContentResolver.delete(EmailContent.Message.CONTENT_URI,
                                WHERE_SERVER_ID_AND_MAILBOX_KEY,
                                new String[] {sse.mItemId, mMailboxIdAsString});
                    }
                }
            }
        }
    }

    @Override
    protected void wipe() {
        LogUtils.i(TAG, "Wiping mailbox %s", mMailbox);
        Mailbox.resyncMailbox(mContentResolver, new android.accounts.Account(mAccount.mEmailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE), mMailbox.mId);
    }

    @Override
    public boolean parse() throws IOException, CommandStatusException {
        final boolean result = super.parse();
        return result || fetchNeeded();
    }

    /**
     * Commit all changes. This results in a Binder IPC call which has constraint on the size of
     * the data, the docs say it currently 1MB. We set a limit to the size of the message we fetch
     * with {@link Eas#EAS12_TRUNCATION_SIZE} & {@link Eas#EAS12_TRUNCATION_SIZE} which are at 200k
     * or bellow. As long as these limits are bellow 500k, we should be able to apply a single
     * message (the transaction size is about double the message size because Java strings are 16
     * bit.
     * <b/>
     * We first try to apply the changes in normal chunk size {@link #MAX_OPS_PER_BATCH}. If we get
     * a {@link TransactionTooLargeException} we try again with but this time, we apply each change
     * immediately.
     */
    @Override
    public void commit() throws RemoteException, OperationApplicationException {
        try {
            commitImpl(MAX_OPS_PER_BATCH);
        } catch (TransactionTooLargeException e1) {
            // Try again but apply batch after every message. The max message size defined in
            // Eas.EAS12_TRUNCATION_SIZE or Eas.EAS2_5_TRUNCATION_SIZE is small enough to fit
            // in a single Binder call.
            LogUtils.w(TAG, e1, "Transaction too large, retrying in single mode");
            try {
                commitImpl(1);
            } catch (TransactionTooLargeException e2) {
                LogUtils.wtf(TAG, e2, "Transaction too large with batch size one");
            }
        }
    }

    public void commitImpl(int maxOpsPerBatch)
            throws RemoteException, OperationApplicationException {
        // Use a batch operation to handle the changes
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        // Maximum size of message text per fetch
        int numFetched = fetchedEmails.size();
        LogUtils.d(TAG, "commitImpl: maxOpsPerBatch=%d numFetched=%d numNew=%d "
                + "numDeleted=%d numChanged=%d",
                maxOpsPerBatch,
                numFetched,
                newEmails.size(),
                deletedEmails.size(),
                changedEmails.size());
        for (EmailContent.Message msg: fetchedEmails) {
            // Find the original message's id (by serverId and mailbox)
            Cursor c = getServerIdCursor(msg.mServerId, EmailContent.ID_PROJECTION);
            String id = null;
            try {
                if (c.moveToFirst()) {
                    id = c.getString(EmailContent.ID_PROJECTION_COLUMN);
                    while (c.moveToNext()) {
                        // This shouldn't happen, but clean up if it does
                        Long dupId =
                                Long.parseLong(c.getString(EmailContent.ID_PROJECTION_COLUMN));
                        userLog("Delete duplicate with id: " + dupId);
                        deletedEmails.add(dupId);
                    }
                }
            } finally {
                c.close();
            }

            // If we find one, we do two things atomically: 1) set the body text for the
            // message, and 2) mark the message loaded (i.e. completely loaded)
            if (id != null) {
                LogUtils.i(TAG, "Fetched body successfully for %s", id);
                final String[] bindArgument = new String[] {id};
                ops.add(ContentProviderOperation.newUpdate(EmailContent.Body.CONTENT_URI)
                        .withSelection(EmailContent.Body.SELECTION_BY_MESSAGE_KEY, bindArgument)
                        .withValue(EmailContent.BodyColumns.TEXT_CONTENT, msg.mText)
                        .build());
                ops.add(ContentProviderOperation.newUpdate(EmailContent.Message.CONTENT_URI)
                        .withSelection(MessageColumns._ID + "=?", bindArgument)
                        .withValue(MessageColumns.FLAG_LOADED,
                                EmailContent.Message.FLAG_LOADED_COMPLETE)
                        .build());
            }
            applyBatchIfNeeded(ops, maxOpsPerBatch, false);
        }

        for (EmailContent.Message msg: newEmails) {
            msg.addSaveOps(ops);
            applyBatchIfNeeded(ops, maxOpsPerBatch, false);
        }

        for (Long id : deletedEmails) {
            ops.add(ContentProviderOperation.newDelete(
                    ContentUris.withAppendedId(EmailContent.Message.CONTENT_URI, id)).build());
            AttachmentUtilities.deleteAllAttachmentFiles(mContext, mAccount.mId, id);
            applyBatchIfNeeded(ops, maxOpsPerBatch, false);
        }

        if (!changedEmails.isEmpty()) {
            // Server wins in a conflict...
            for (ServerChange change : changedEmails) {
                ContentValues cv = new ContentValues();
                if (change.read != null) {
                    cv.put(EmailContent.MessageColumns.FLAG_READ, change.read);
                }
                if (change.flag != null) {
                    cv.put(EmailContent.MessageColumns.FLAG_FAVORITE, change.flag);
                }
                if (change.flags != null) {
                    cv.put(EmailContent.MessageColumns.FLAGS, change.flags);
                }
                ops.add(ContentProviderOperation.newUpdate(
                        ContentUris.withAppendedId(EmailContent.Message.CONTENT_URI, change.id))
                        .withValues(cv)
                        .build());
            }
            applyBatchIfNeeded(ops, maxOpsPerBatch, false);
        }

        // We only want to update the sync key here
        ContentValues mailboxValues = new ContentValues();
        mailboxValues.put(Mailbox.SYNC_KEY, mMailbox.mSyncKey);
        ops.add(ContentProviderOperation.newUpdate(
                ContentUris.withAppendedId(Mailbox.CONTENT_URI, mMailbox.mId))
                .withValues(mailboxValues).build());

        applyBatchIfNeeded(ops, maxOpsPerBatch, true);
        userLog(mMailbox.mDisplayName, " SyncKey saved as: ", mMailbox.mSyncKey);
    }

    // Check if there at least MAX_OPS_PER_BATCH ops in queue and flush if there are.
    // If force is true, flush regardless of size.
    private void applyBatchIfNeeded(ArrayList<ContentProviderOperation> ops, int maxOpsPerBatch,
            boolean force)
            throws RemoteException, OperationApplicationException {
        if (force ||  ops.size() >= maxOpsPerBatch) {
            mContentResolver.applyBatch(EmailContent.AUTHORITY, ops);
            ops.clear();
        }
    }
}
