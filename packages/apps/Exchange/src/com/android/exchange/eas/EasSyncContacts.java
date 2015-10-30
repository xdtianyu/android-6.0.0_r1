package com.android.exchange.eas;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Base64;

import com.android.emailcommon.TrafficFlags;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.Eas;
import com.android.exchange.adapter.AbstractSyncParser;
import com.android.exchange.adapter.ContactsSyncParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Performs an Exchange sync for contacts.
 * Contact state is in the contacts provider, not in our DB (and therefore not in e.g. mMailbox).
 * The Mailbox in the Email DB is only useful for serverId and syncInterval.
 */
public class EasSyncContacts extends EasSyncCollectionTypeBase {
    private static final String TAG = Eas.LOG_TAG;

    public static final int PIM_WINDOW_SIZE_CONTACTS = 10;

    private static final String MIMETYPE_GROUP_MEMBERSHIP_AND_ID_EQUALS =
            ContactsContract.Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE + "' AND " +
                    GroupMembership.GROUP_ROW_ID + "=?";

    private static final String[] GROUP_TITLE_PROJECTION =
            new String[] {Groups.TITLE};
    private static final String[] GROUPS_ID_PROJECTION = new String[] {Groups._ID};

    /** The maximum number of IMs we can send for one contact. */
    private static final int MAX_IM_ROWS = 3;
    /** The tags to use for IMs in an upsync. */
    private static final int[] IM_TAGS = new int[] {Tags.CONTACTS2_IM_ADDRESS,
            Tags.CONTACTS2_IM_ADDRESS_2, Tags.CONTACTS2_IM_ADDRESS_3};

    /** The maximum number of email addresses we can send for one contact. */
    private static final int MAX_EMAIL_ROWS = 3;
    /** The tags to use for the emails in an upsync. */
    private static final int[] EMAIL_TAGS = new int[] {Tags.CONTACTS_EMAIL1_ADDRESS,
            Tags.CONTACTS_EMAIL2_ADDRESS, Tags.CONTACTS_EMAIL3_ADDRESS};

    /** The maximum number of phone numbers of each type we can send for one contact. */
    private static final int MAX_PHONE_ROWS = 2;
    /** The tags to use for work phone numbers. */
    private static final int[] WORK_PHONE_TAGS = new int[] {Tags.CONTACTS_BUSINESS_TELEPHONE_NUMBER,
            Tags.CONTACTS_BUSINESS2_TELEPHONE_NUMBER};
    /** The tags to use for home phone numbers. */
    private static final int[] HOME_PHONE_TAGS = new int[] {Tags.CONTACTS_HOME_TELEPHONE_NUMBER,
            Tags.CONTACTS_HOME2_TELEPHONE_NUMBER};

    /** The tags to use for different parts of a home address. */
    private static final int[] HOME_ADDRESS_TAGS = new int[] {Tags.CONTACTS_HOME_ADDRESS_CITY,
            Tags.CONTACTS_HOME_ADDRESS_COUNTRY,
            Tags.CONTACTS_HOME_ADDRESS_POSTAL_CODE,
            Tags.CONTACTS_HOME_ADDRESS_STATE,
            Tags.CONTACTS_HOME_ADDRESS_STREET};

    /** The tags to use for different parts of a work address. */
    private static final int[] WORK_ADDRESS_TAGS = new int[] {Tags.CONTACTS_BUSINESS_ADDRESS_CITY,
            Tags.CONTACTS_BUSINESS_ADDRESS_COUNTRY,
            Tags.CONTACTS_BUSINESS_ADDRESS_POSTAL_CODE,
            Tags.CONTACTS_BUSINESS_ADDRESS_STATE,
            Tags.CONTACTS_BUSINESS_ADDRESS_STREET};

    /** The tags to use for different parts of an "other" address. */
    private static final int[] OTHER_ADDRESS_TAGS = new int[] {Tags.CONTACTS_HOME_ADDRESS_CITY,
            Tags.CONTACTS_OTHER_ADDRESS_COUNTRY,
            Tags.CONTACTS_OTHER_ADDRESS_POSTAL_CODE,
            Tags.CONTACTS_OTHER_ADDRESS_STATE,
            Tags.CONTACTS_OTHER_ADDRESS_STREET};

    private final android.accounts.Account mAccountManagerAccount;

    private final ArrayList<Long> mDeletedContacts = new ArrayList<Long>();
    private final ArrayList<Long> mUpdatedContacts = new ArrayList<Long>();

    // We store the parser so that we can ask it later isGroupsUsed.
    // TODO: Can we do this more cleanly?
    private ContactsSyncParser mParser = null;

    private static final class EasChildren {
        private EasChildren() {}

        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/eas_children";
        public static final int MAX_CHILDREN = 8;
        public static final String[] ROWS =
            new String[] {"data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9"};
    }

    // Classes for each type of contact.
    // These are copied from ContactSyncAdapter, with unused fields and methods removed, but the
    // parser hasn't been moved over yet. When that happens, the variables and functions may also
    // need to be copied over.

    /**
     * Data and constants for a Personal contact.
     */
    private static final class EasPersonal {
        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/eas_personal";
        public static final String ANNIVERSARY = "data2";
        public static final String FILE_AS = "data4";
    }

    /**
     * Data and constants for a Business contact.
     */
    private static final class EasBusiness {
        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/eas_business";
        public static final String CUSTOMER_ID = "data6";
        public static final String GOVERNMENT_ID = "data7";
        public static final String ACCOUNT_NAME = "data8";
    }

    public EasSyncContacts(final String emailAddress) {
        mAccountManagerAccount = new android.accounts.Account(emailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
    }

    @Override
    public int getTrafficFlag() {
        return TrafficFlags.DATA_CONTACTS;
    }

    @Override
    public void setSyncOptions(final Context context, final Serializer s,
            final double protocolVersion, final Account account, final Mailbox mailbox,
            final boolean isInitialSync, final int numWindows) throws IOException {
        if (isInitialSync) {
            setInitialSyncOptions(s);
            return;
        }

        final int windowSize = numWindows * PIM_WINDOW_SIZE_CONTACTS;
        if (windowSize > MAX_WINDOW_SIZE  + PIM_WINDOW_SIZE_CONTACTS) {
            throw new IOException("Max window size reached and still no data");
        }
        setPimSyncOptions(s, null, protocolVersion,
                windowSize < MAX_WINDOW_SIZE ? windowSize : MAX_WINDOW_SIZE);

        setUpsyncCommands(s, context.getContentResolver(), account, mailbox, protocolVersion);
    }

    @Override
    public AbstractSyncParser getParser(final Context context, final Account account,
            final Mailbox mailbox, final InputStream is) throws IOException {
        mParser = new ContactsSyncParser(context, context.getContentResolver(), is, mailbox,
                account, mAccountManagerAccount);
        return mParser;
    }

    private void setInitialSyncOptions(final Serializer s) throws IOException {
        // These are the tags we support for upload; whenever we add/remove support
        // (in addData), we need to update this list
        s.start(Tags.SYNC_SUPPORTED);
        s.tag(Tags.CONTACTS_FIRST_NAME);
        s.tag(Tags.CONTACTS_LAST_NAME);
        s.tag(Tags.CONTACTS_MIDDLE_NAME);
        s.tag(Tags.CONTACTS_SUFFIX);
        s.tag(Tags.CONTACTS_COMPANY_NAME);
        s.tag(Tags.CONTACTS_JOB_TITLE);
        s.tag(Tags.CONTACTS_EMAIL1_ADDRESS);
        s.tag(Tags.CONTACTS_EMAIL2_ADDRESS);
        s.tag(Tags.CONTACTS_EMAIL3_ADDRESS);
        s.tag(Tags.CONTACTS_BUSINESS2_TELEPHONE_NUMBER);
        s.tag(Tags.CONTACTS_BUSINESS_TELEPHONE_NUMBER);
        s.tag(Tags.CONTACTS2_MMS);
        s.tag(Tags.CONTACTS_BUSINESS_FAX_NUMBER);
        s.tag(Tags.CONTACTS2_COMPANY_MAIN_PHONE);
        s.tag(Tags.CONTACTS_HOME_FAX_NUMBER);
        s.tag(Tags.CONTACTS_HOME_TELEPHONE_NUMBER);
        s.tag(Tags.CONTACTS_HOME2_TELEPHONE_NUMBER);
        s.tag(Tags.CONTACTS_MOBILE_TELEPHONE_NUMBER);
        s.tag(Tags.CONTACTS_CAR_TELEPHONE_NUMBER);
        s.tag(Tags.CONTACTS_RADIO_TELEPHONE_NUMBER);
        s.tag(Tags.CONTACTS_PAGER_NUMBER);
        s.tag(Tags.CONTACTS_ASSISTANT_TELEPHONE_NUMBER);
        s.tag(Tags.CONTACTS2_IM_ADDRESS);
        s.tag(Tags.CONTACTS2_IM_ADDRESS_2);
        s.tag(Tags.CONTACTS2_IM_ADDRESS_3);
        s.tag(Tags.CONTACTS_BUSINESS_ADDRESS_CITY);
        s.tag(Tags.CONTACTS_BUSINESS_ADDRESS_COUNTRY);
        s.tag(Tags.CONTACTS_BUSINESS_ADDRESS_POSTAL_CODE);
        s.tag(Tags.CONTACTS_BUSINESS_ADDRESS_STATE);
        s.tag(Tags.CONTACTS_BUSINESS_ADDRESS_STREET);
        s.tag(Tags.CONTACTS_HOME_ADDRESS_CITY);
        s.tag(Tags.CONTACTS_HOME_ADDRESS_COUNTRY);
        s.tag(Tags.CONTACTS_HOME_ADDRESS_POSTAL_CODE);
        s.tag(Tags.CONTACTS_HOME_ADDRESS_STATE);
        s.tag(Tags.CONTACTS_HOME_ADDRESS_STREET);
        s.tag(Tags.CONTACTS_OTHER_ADDRESS_CITY);
        s.tag(Tags.CONTACTS_OTHER_ADDRESS_COUNTRY);
        s.tag(Tags.CONTACTS_OTHER_ADDRESS_POSTAL_CODE);
        s.tag(Tags.CONTACTS_OTHER_ADDRESS_STATE);
        s.tag(Tags.CONTACTS_OTHER_ADDRESS_STREET);
        s.tag(Tags.CONTACTS_YOMI_COMPANY_NAME);
        s.tag(Tags.CONTACTS_YOMI_FIRST_NAME);
        s.tag(Tags.CONTACTS_YOMI_LAST_NAME);
        s.tag(Tags.CONTACTS2_NICKNAME);
        s.tag(Tags.CONTACTS_ASSISTANT_NAME);
        s.tag(Tags.CONTACTS2_MANAGER_NAME);
        s.tag(Tags.CONTACTS_SPOUSE);
        s.tag(Tags.CONTACTS_DEPARTMENT);
        s.tag(Tags.CONTACTS_TITLE);
        s.tag(Tags.CONTACTS_OFFICE_LOCATION);
        s.tag(Tags.CONTACTS2_CUSTOMER_ID);
        s.tag(Tags.CONTACTS2_GOVERNMENT_ID);
        s.tag(Tags.CONTACTS2_ACCOUNT_NAME);
        s.tag(Tags.CONTACTS_ANNIVERSARY);
        s.tag(Tags.CONTACTS_BIRTHDAY);
        s.tag(Tags.CONTACTS_WEBPAGE);
        s.tag(Tags.CONTACTS_PICTURE);
        s.tag(Tags.CONTACTS_FILE_AS);
        s.end(); // SYNC_SUPPORTED
    }

    /**
     * Add account info and the "caller is syncadapter" param to a URI.
     * @param uri The {@link Uri} to add to.
     * @param emailAddress The email address to add to uri.
     * @return
     */
    private static Uri uriWithAccountAndIsSyncAdapter(final Uri uri, final String emailAddress) {
        return uri.buildUpon()
            .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, emailAddress)
            .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE,
                    Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE)
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build();
    }

    /**
     * Add the "caller is syncadapter" param to a URI.
     * @param uri The {@link Uri} to add to.
     * @return
     */
    private static Uri addCallerIsSyncAdapterParameter(final Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
    }

    /**
     * Mark contacts in dirty groups as dirty.
     */
    private void dirtyContactsWithinDirtyGroups(final ContentResolver cr, final Account account) {
        final String emailAddress = account.mEmailAddress;
        final Cursor c = cr.query( uriWithAccountAndIsSyncAdapter(Groups.CONTENT_URI, emailAddress),
                GROUPS_ID_PROJECTION, Groups.DIRTY + "=1", null, null);
        if (c == null) {
            return;
        }
        try {
            if (c.getCount() > 0) {
                final String[] updateArgs = new String[1];
                final ContentValues updateValues = new ContentValues();
                while (c.moveToNext()) {
                    // For each, "touch" all data rows with this group id; this will mark contacts
                    // in this group as dirty (per ContactsContract).  We will then know to upload
                    // them to the server with the modified group information
                    final long id = c.getLong(0);
                    updateValues.put(GroupMembership.GROUP_ROW_ID, id);
                    updateArgs[0] = Long.toString(id);
                    cr.update(ContactsContract.Data.CONTENT_URI, updateValues,
                            MIMETYPE_GROUP_MEMBERSHIP_AND_ID_EQUALS, updateArgs);
                }
                // Really delete groups that are marked deleted
                cr.delete(uriWithAccountAndIsSyncAdapter(Groups.CONTENT_URI, emailAddress),
                        Groups.DELETED + "=1", null);
                // Clear the dirty flag for all of our groups
                updateValues.clear();
                updateValues.put(Groups.DIRTY, 0);
                cr.update(uriWithAccountAndIsSyncAdapter(Groups.CONTENT_URI, emailAddress),
                        updateValues, null, null);
            }
        } finally {
            c.close();
        }
    }

    /**
     * Helper function to safely extract a string from a content value.
     * @param cv The {@link ContentValues} that contains the values
     * @param column The column name in cv for the data
     * @return The data in the column or null if it doesn't exist or is empty.
     * @throws IOException
     */
    public static String tryGetStringData(final ContentValues cv, final String column)
            throws IOException {
        if ((cv == null) || (column == null)) {
            return null;
        }
        if (cv.containsKey(column)) {
            final String value = cv.getAsString(column);
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Helper to add a string to the upsync.
     * @param s The {@link Serializer} for this sync request
     * @param cv The {@link ContentValues} with the data for this string.
     * @param column The column name in cv to find the string.
     * @param tag The tag to use when adding to s.
     * @return Whether or not the field was actually set.
     * @throws IOException
     */
    private static boolean sendStringData(final Serializer s, final ContentValues cv,
            final String column, final int tag) throws IOException {
        final String dataValue = tryGetStringData(cv, column);
        if (dataValue != null) {
            s.data(tag, dataValue);
            return true;
        }
        return false;
    }


    // This is to catch when the contacts provider has a date in this particular wrong format.
    private static final SimpleDateFormat SHORT_DATE_FORMAT;
    // Array of formats we check when parsing dates from the contacts provider.
    private static final DateFormat[] DATE_FORMATS;
    static {
        SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SHORT_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        //TODO: We only handle two formatting types. The default contacts app will work with this
        // but any other contacts apps might not. We can try harder to handle those guys too.
        DATE_FORMATS = new DateFormat[] { Eas.DATE_FORMAT, SHORT_DATE_FORMAT };
    }

    /**
     * Helper to add a date to the upsync. It reads the date as a string from the
     * {@link ContentValues} that we got from the provider, tries to parse it using various formats,
     * and formats it correctly to send to the server. If it can't parse it, it will omit the date
     * in the upsync; since Birthdays (the only date currently supported by this class) can be
     * ghosted, this means that any date changes on the client will NOT be reflected on the server.
     * @param s The {@link Serializer} for this sync request
     * @param cv The {@link ContentValues} with the data for this string.
     * @param column The column name in cv to find the string.
     * @param tag The tag to use when adding to s.
     * @throws IOException
     */
    private static void sendDateData(final Serializer s, final ContentValues cv,
            final String column, final int tag) throws IOException {
        if (cv.containsKey(column)) {
            final String value = cv.getAsString(column);
            if (!TextUtils.isEmpty(value)) {
                Date date;
                // Check all the formats we know about to see if one of them works.
                for (final DateFormat format : DATE_FORMATS) {
                    try {
                        date = format.parse(value);
                        if (date != null) {
                            // We got a legit date for this format, so send it up.
                            s.data(tag, Eas.DATE_FORMAT.format(date));
                            return;
                        }
                    } catch (final ParseException e) {
                        // The date didn't match this particular format; keep looping.
                    }
                }
            }
        }
    }


    /**
     * Add a nickname to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this nickname.
     * @throws IOException
     */
    private static void sendNickname(final Serializer s, final ContentValues cv)
            throws IOException {
        sendStringData(s, cv, Nickname.NAME, Tags.CONTACTS2_NICKNAME);
    }

    /**
     * Add children data to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for a set of children.
     * @throws IOException
     */
    private static void sendChildren(final Serializer s, final ContentValues cv)
            throws IOException {
        boolean first = true;
        for (int i = 0; i < EasChildren.MAX_CHILDREN; i++) {
            final String row = EasChildren.ROWS[i];
            if (cv.containsKey(row)) {
                if (first) {
                    s.start(Tags.CONTACTS_CHILDREN);
                    first = false;
                }
                s.data(Tags.CONTACTS_CHILD, cv.getAsString(row));
            }
        }
        if (!first) {
            s.end();
        }
    }

    /**
     * Add business contact info to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this business contact.
     * @throws IOException
     */
    private static void sendBusiness(final Serializer s, final ContentValues cv)
            throws IOException {
        sendStringData(s, cv, EasBusiness.ACCOUNT_NAME, Tags.CONTACTS2_ACCOUNT_NAME);
        sendStringData(s, cv, EasBusiness.CUSTOMER_ID, Tags.CONTACTS2_CUSTOMER_ID);
        sendStringData(s, cv, EasBusiness.GOVERNMENT_ID, Tags.CONTACTS2_GOVERNMENT_ID);
    }

    /**
     * Add a webpage info to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this webpage.
     * @throws IOException
     */
    private static void sendWebpage(final Serializer s, final ContentValues cv) throws IOException {
        sendStringData(s, cv, Website.URL, Tags.CONTACTS_WEBPAGE);
    }

    /**
     * Add personal contact info to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this personal contact.
     * @throws IOException
     */
    private static void sendPersonal(final Serializer s, final ContentValues cv)
            throws IOException {
        sendStringData(s, cv, EasPersonal.ANNIVERSARY, Tags.CONTACTS_ANNIVERSARY);
    }

    /**
     * Add contact file_as info to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this personal contact.
     * @throws IOException
     */
    private static boolean trySendFileAs(final Serializer s, final ContentValues cv)
            throws IOException {
        return sendStringData(s, cv, EasPersonal.FILE_AS, Tags.CONTACTS_FILE_AS);
    }

    /**
     * Add a phone number to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this phone number.
     * @param workCount The number of work phone numbers already added.
     * @param homeCount The number of home phone numbers already added.
     * @throws IOException
     */
    private static void sendPhone(final Serializer s, final ContentValues cv, final int workCount,
            final int homeCount) throws IOException {
        final String value = cv.getAsString(Phone.NUMBER);
        if (value == null || !cv.containsKey(Phone.TYPE)) {
            return;
        }
        switch (cv.getAsInteger(Phone.TYPE)) {
            case Phone.TYPE_WORK:
                if (workCount < MAX_PHONE_ROWS) {
                    s.data(WORK_PHONE_TAGS[workCount], value);
                }
                break;
            case Phone.TYPE_MMS:
                s.data(Tags.CONTACTS2_MMS, value);
                break;
            case Phone.TYPE_ASSISTANT:
                s.data(Tags.CONTACTS_ASSISTANT_TELEPHONE_NUMBER, value);
                break;
            case Phone.TYPE_FAX_WORK:
                s.data(Tags.CONTACTS_BUSINESS_FAX_NUMBER, value);
                break;
            case Phone.TYPE_COMPANY_MAIN:
                s.data(Tags.CONTACTS2_COMPANY_MAIN_PHONE, value);
                break;
            case Phone.TYPE_HOME:
                if (homeCount < MAX_PHONE_ROWS) {
                    s.data(HOME_PHONE_TAGS[homeCount], value);
                }
                break;
            case Phone.TYPE_MOBILE:
                s.data(Tags.CONTACTS_MOBILE_TELEPHONE_NUMBER, value);
                break;
            case Phone.TYPE_CAR:
                s.data(Tags.CONTACTS_CAR_TELEPHONE_NUMBER, value);
                break;
            case Phone.TYPE_PAGER:
                s.data(Tags.CONTACTS_PAGER_NUMBER, value);
                break;
            case Phone.TYPE_RADIO:
                s.data(Tags.CONTACTS_RADIO_TELEPHONE_NUMBER, value);
                break;
            case Phone.TYPE_FAX_HOME:
                s.data(Tags.CONTACTS_HOME_FAX_NUMBER, value);
                break;
            default:
                break;
        }
    }

    /**
     * Add a relation to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this relation.
     * @throws IOException
     */
    private static void sendRelation(final Serializer s, final ContentValues cv)
            throws IOException {
        final String value = cv.getAsString(Relation.DATA);
        if (value == null || !cv.containsKey(Relation.TYPE)) {
            return;
        }
        switch (cv.getAsInteger(Relation.TYPE)) {
            case Relation.TYPE_ASSISTANT:
                s.data(Tags.CONTACTS_ASSISTANT_NAME, value);
                break;
            case Relation.TYPE_MANAGER:
                s.data(Tags.CONTACTS2_MANAGER_NAME, value);
                break;
            case Relation.TYPE_SPOUSE:
                s.data(Tags.CONTACTS_SPOUSE, value);
                break;
            default:
                break;
        }
    }

    /**
     * Add a name to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this name.
     * @throws IOException
     */
    // TODO: This used to return a displayName, but it was always null. Figure out what it really
    // wanted to return.
    private static void sendStructuredName(final Serializer s, final ContentValues cv)
            throws IOException {
        sendStringData(s, cv, StructuredName.FAMILY_NAME, Tags.CONTACTS_LAST_NAME);
        sendStringData(s, cv, StructuredName.GIVEN_NAME, Tags.CONTACTS_FIRST_NAME);
        sendStringData(s, cv, StructuredName.MIDDLE_NAME, Tags.CONTACTS_MIDDLE_NAME);
        sendStringData(s, cv, StructuredName.SUFFIX, Tags.CONTACTS_SUFFIX);
        sendStringData(s, cv, StructuredName.PHONETIC_GIVEN_NAME, Tags.CONTACTS_YOMI_FIRST_NAME);
        sendStringData(s, cv, StructuredName.PHONETIC_FAMILY_NAME, Tags.CONTACTS_YOMI_LAST_NAME);
        sendStringData(s, cv, StructuredName.PREFIX, Tags.CONTACTS_TITLE);
    }

    /**
     * Add an address of a particular type to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this address.
     * @param fieldNames The field names for this address type.
     * @throws IOException
     */
    private static void sendOnePostal(final Serializer s, final ContentValues cv,
            final int[] fieldNames) throws IOException{
        sendStringData(s, cv, StructuredPostal.CITY, fieldNames[0]);
        sendStringData(s, cv, StructuredPostal.COUNTRY, fieldNames[1]);
        sendStringData(s, cv, StructuredPostal.POSTCODE, fieldNames[2]);
        sendStringData(s, cv, StructuredPostal.REGION, fieldNames[3]);
        sendStringData(s, cv, StructuredPostal.STREET, fieldNames[4]);
    }

    /**
     * Add an address to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this address.
     * @throws IOException
     */
    private static void sendStructuredPostal(final Serializer s, final ContentValues cv)
        throws IOException {
        if (!cv.containsKey(StructuredPostal.TYPE)) {
            return;
        }
        switch (cv.getAsInteger(StructuredPostal.TYPE)) {
            case StructuredPostal.TYPE_HOME:
                sendOnePostal(s, cv, HOME_ADDRESS_TAGS);
                break;
            case StructuredPostal.TYPE_WORK:
                sendOnePostal(s, cv, WORK_ADDRESS_TAGS);
                break;
            case StructuredPostal.TYPE_OTHER:
                sendOnePostal(s, cv, OTHER_ADDRESS_TAGS);
                break;
            default:
                break;
        }
    }

    /**
     * Add an organization to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this organization.
     * @throws IOException
     */
    private static void sendOrganization(final Serializer s, final ContentValues cv)
            throws IOException {
        sendStringData(s, cv, Organization.TITLE, Tags.CONTACTS_JOB_TITLE);
        sendStringData(s, cv, Organization.COMPANY, Tags.CONTACTS_COMPANY_NAME);
        sendStringData(s, cv, Organization.DEPARTMENT, Tags.CONTACTS_DEPARTMENT);
        sendStringData(s, cv, Organization.OFFICE_LOCATION, Tags.CONTACTS_OFFICE_LOCATION);
    }

    /**
     * Add an IM to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this IM.
     * @throws IOException
     */
     private static void sendIm(final Serializer s, final ContentValues cv, final int count)
             throws IOException {
        final String value = cv.getAsString(Im.DATA);
        if (value == null) return;
        if (count < MAX_IM_ROWS) {
            s.data(IM_TAGS[count], value);
        }
    }

    /**
     * Add a birthday to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this birthday.
     * @throws IOException
     */
    private static void sendBirthday(final Serializer s, final ContentValues cv)
            throws IOException {
        sendDateData(s, cv, Event.START_DATE, Tags.CONTACTS_BIRTHDAY);
    }

    /**
     * Add a note to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this note.
     * @param protocolVersion
     * @throws IOException
     */
    private void sendNote(final Serializer s, final ContentValues cv, final double protocolVersion)
            throws IOException {
        // Even when there is no local note, we must explicitly upsync an empty note,
        // which is the only way to force the server to delete any pre-existing note.
        String note = "";
        if (cv.containsKey(Note.NOTE)) {
            // EAS won't accept note data with raw newline characters
            note = cv.getAsString(Note.NOTE).replaceAll("\n", "\r\n");
        }
        // Format of upsync data depends on protocol version
        if (protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
            s.start(Tags.BASE_BODY);
            s.data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_TEXT).data(Tags.BASE_DATA, note);
            s.end();
        } else {
            s.data(Tags.CONTACTS_BODY, note);
        }
    }

    /**
     * Add a photo to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this photo.
     * @throws IOException
     */
    private static void sendPhoto(final Serializer s, final ContentValues cv) throws IOException {
        if (cv.containsKey(Photo.PHOTO)) {
            final byte[] bytes = cv.getAsByteArray(Photo.PHOTO);
            final String pic = Base64.encodeToString(bytes, Base64.NO_WRAP);
            s.data(Tags.CONTACTS_PICTURE, pic);
        } else {
            // Send an empty tag, which signals the server to delete any pre-existing photo
            s.tag(Tags.CONTACTS_PICTURE);
        }
    }

    /**
     * Add an email address to the upsync.
     * @param s The {@link Serializer} for this sync request.
     * @param cv The {@link ContentValues} with the data for this email address.
     * @param count The number of email addresses that have already been added.
     * @param displayName The display name for this contact.
     * @param protocolVersion
     * @throws IOException
     */
    private void sendEmail(final Serializer s, final ContentValues cv, final int count,
            final String displayName, final double protocolVersion) throws IOException {
        // Get both parts of the email address (a newly created one in the UI won't have a name)
        final String addr = cv.getAsString(Email.DATA);
        String name = cv.getAsString(Email.DISPLAY_NAME);
        if (name == null) {
            if (displayName != null) {
                name = displayName;
            } else {
                name = addr;
            }
        }
        // Compose address from name and addr
        if (addr != null) {
            final String value;
            // Only send the raw email address for EAS 2.5 (Hotmail, in particular, chokes on
            // an RFC822 address)
            if (protocolVersion < Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
                value = addr;
            } else {
                value = '\"' + name + "\" <" + addr + '>';
            }
            if (count < MAX_EMAIL_ROWS) {
                s.data(EMAIL_TAGS[count], value);
            }
        }
    }

    /**
     * Generate a default fileAs string for this contact using name and email data.
     * Note that the user can change this in Outlook/OWA if it is not correct for them but
     * we need to send something or else Exchange will not display a name with the contact.
     * @param nameValues Name information to use in generating the fileAs string
     * @param emailValues Email information to use to generate the fileAs string
     * @return A valid fileAs string or null
     */
    public static String generateFileAs(final ContentValues nameValues,
            final ArrayList<ContentValues> emailValues) throws IOException {
        // TODO: Is there a better way of generating a default file_as that will make people
        // happy everywhere in the world? Should we read the sort settings of the People app?
        final String firstName = tryGetStringData(nameValues, StructuredName.GIVEN_NAME);
        final String lastName = tryGetStringData(nameValues, StructuredName.FAMILY_NAME);;
        final String middleName = tryGetStringData(nameValues, StructuredName.MIDDLE_NAME);;
        final String nameSuffix = tryGetStringData(nameValues, StructuredName.SUFFIX);

        if (firstName == null && lastName == null) {
            if (emailValues == null) {
                // Bad name, bad email list...not much we can do about it.
                return null;
            }
            // The name fields didn't yield anything valuable, let's generate a file as
            // via the email addresses that were passed in.
            for (final ContentValues cv : emailValues) {
                final String emailAddr = tryGetStringData(cv, Email.DATA);
                if (emailAddr != null) {
                    return emailAddr;
                }
            }
            return null;
        }
        // Let's try to construct this with the name only. The format is this:
        // LastName nameSuffix, FirstName MiddleName
        // nameSuffix is only applied if lastName exists.
        final StringBuilder builder = new StringBuilder();
        if (lastName != null) {
            builder.append(lastName);
            if (nameSuffix != null) {
                builder.append(" " + nameSuffix);
            }
            builder.append(", ");
        }
        if (firstName != null) {
            builder.append(firstName + " ");
        }
        if (middleName != null) {
            builder.append(middleName);
        }
        // We might leave a trailing space, so let's trim the string here.
        return builder.toString().trim();
    }


    private void setUpsyncCommands(final Serializer s, final ContentResolver cr,
            final Account account, final Mailbox mailbox, final double protocolVersion)
            throws IOException {
        // Find any groups of ours that are dirty and dirty those groups' members
        dirtyContactsWithinDirtyGroups(cr, account);

        // First, let's find Contacts that have changed.
        final Uri uri = uriWithAccountAndIsSyncAdapter(
                ContactsContract.RawContactsEntity.CONTENT_URI, account.mEmailAddress);

        // Get them all atomically
        final Cursor cursor = cr.query(uri, null, ContactsContract.RawContacts.DIRTY + "=1",
                null, null);
        if (cursor == null) {
            return;
        }
        final EntityIterator ei = ContactsContract.RawContacts.newEntityIterator(cursor);
        final ContentValues cidValues = new ContentValues();
        boolean hasSetFileAs = false;
        try {
            boolean first = true;
            final Uri rawContactUri = addCallerIsSyncAdapterParameter(
                    ContactsContract.RawContacts.CONTENT_URI);
            while (ei.hasNext()) {
                final Entity entity = ei.next();
                // For each of these entities, create the change commands
                final ContentValues entityValues = entity.getEntityValues();
                String serverId = entityValues.getAsString(ContactsContract.RawContacts.SOURCE_ID);
                final ArrayList<Integer> groupIds = new ArrayList<Integer>();
                if (first) {
                    s.start(Tags.SYNC_COMMANDS);
                    LogUtils.d(TAG, "Sending Contacts changes to the server");
                    first = false;
                }
                if (serverId == null) {
                    // This is a new contact; create a clientId
                    final String clientId =
                            "new_" + mailbox.mId + '_' + System.currentTimeMillis();
                    // We need to server id to look up the fileAs string.
                    serverId = clientId;
                    LogUtils.d(TAG, "Creating new contact with clientId: %s", clientId);
                    s.start(Tags.SYNC_ADD).data(Tags.SYNC_CLIENT_ID, clientId);
                    // And save it in the raw contact
                    cidValues.put(ContactsContract.RawContacts.SYNC1, clientId);
                    cr.update(ContentUris.withAppendedId(rawContactUri,
                            entityValues.getAsLong(ContactsContract.RawContacts._ID)),
                            cidValues, null, null);
                } else {
                    if (entityValues.getAsInteger(ContactsContract.RawContacts.DELETED) == 1) {
                        LogUtils.d(TAG, "Deleting contact with serverId: %s", serverId);
                        s.start(Tags.SYNC_DELETE).data(Tags.SYNC_SERVER_ID, serverId).end();
                        mDeletedContacts.add(
                                entityValues.getAsLong(ContactsContract.RawContacts._ID));
                        continue;
                    }
                    LogUtils.d(TAG, "Upsync change to contact with serverId: %s", serverId);
                    s.start(Tags.SYNC_CHANGE).data(Tags.SYNC_SERVER_ID, serverId);
                    // We don't need to set the file has because it is not a new contact
                    // i.e. it should have the file_as if it needs one.
                    hasSetFileAs = true;
                }
                s.start(Tags.SYNC_APPLICATION_DATA);
                // Write out the data here
                int imCount = 0;
                int emailCount = 0;
                int homePhoneCount = 0;
                int workPhoneCount = 0;
                // TODO: How is this name supposed to be formed?
                String displayName = null;
                final ArrayList<ContentValues> emailValues = new ArrayList<ContentValues>();
                ContentValues nameValues = null;
                for (final Entity.NamedContentValues ncv: entity.getSubValues()) {
                    final ContentValues cv = ncv.values;
                    final String mimeType = cv.getAsString(ContactsContract.Data.MIMETYPE);
                    if (TextUtils.isEmpty(mimeType)) {
                        LogUtils.i(TAG, "Contacts upsync, unknown data: no mimetype set");
                        continue;
                    }

                    if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                        emailValues.add(cv);
                    } else if (mimeType.equals(Nickname.CONTENT_ITEM_TYPE)) {
                        sendNickname(s, cv);
                    } else if (mimeType.equals(EasChildren.CONTENT_ITEM_TYPE)) {
                        sendChildren(s, cv);
                    } else if (mimeType.equals(EasBusiness.CONTENT_ITEM_TYPE)) {
                        sendBusiness(s, cv);
                    } else if (mimeType.equals(Website.CONTENT_ITEM_TYPE)) {
                        sendWebpage(s, cv);
                    } else if (mimeType.equals(EasPersonal.CONTENT_ITEM_TYPE)) {
                        sendPersonal(s, cv);
                        hasSetFileAs = trySendFileAs(s, cv);
                    } else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                        sendPhone(s, cv, workPhoneCount, homePhoneCount);
                        if (cv.containsKey(Phone.TYPE)) {
                            final int type = cv.getAsInteger(Phone.TYPE);
                            if (type == Phone.TYPE_HOME) {
                                homePhoneCount++;
                            } else if (type == Phone.TYPE_WORK) {
                                workPhoneCount++;
                            }
                        }
                    } else if (mimeType.equals(Relation.CONTENT_ITEM_TYPE)) {
                        sendRelation(s, cv);
                    } else if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                        sendStructuredName(s, cv);
                        // Stash names here
                        nameValues = cv;
                    } else if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
                        sendStructuredPostal(s, cv);
                    } else if (mimeType.equals(Organization.CONTENT_ITEM_TYPE)) {
                        sendOrganization(s, cv);
                    } else if (mimeType.equals(Im.CONTENT_ITEM_TYPE)) {
                        sendIm(s, cv, imCount++);
                    } else if (mimeType.equals(Event.CONTENT_ITEM_TYPE)) {
                        if (cv.containsKey(Event.TYPE)) {
                            final Integer eventType = cv.getAsInteger(Event.TYPE);
                            if (eventType != null &&
                                    eventType.equals(Event.TYPE_BIRTHDAY)) {
                                sendBirthday(s, cv);
                            }
                        }
                    } else if (mimeType.equals(GroupMembership.CONTENT_ITEM_TYPE)) {
                        // We must gather these, and send them together (below)
                        groupIds.add(cv.getAsInteger(GroupMembership.GROUP_ROW_ID));
                    } else if (mimeType.equals(Note.CONTENT_ITEM_TYPE)) {
                        sendNote(s, cv, protocolVersion);
                    } else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE)) {
                        sendPhoto(s, cv);
                    } else {
                        LogUtils.i(TAG, "Contacts upsync, unknown data: %s", mimeType);
                    }
                }
                // We do the email rows last, because we need to make sure we've found the
                // displayName (if one exists); this would be in a StructuredName rnow
                for (final ContentValues cv: emailValues) {
                    sendEmail(s, cv, emailCount++, displayName, protocolVersion);
                }
                // For Exchange, we need to make sure that we provide a fileAs string because
                // it is used as the display name for the contact in some views.
                if (!hasSetFileAs) {
                    String fileAs = null;
                    // Let's go grab the display_name_alt info for this contact and use
                    // that as the default fileAs.
                    final Cursor c = cr.query(ContactsContract.RawContacts.CONTENT_URI,
                            new String[]{ContactsContract.RawContacts.DISPLAY_NAME_ALTERNATIVE},
                            ContactsContract.RawContacts.SYNC1 + "=?",
                            new String[]{String.valueOf(serverId)}, null);
                    try {
                        while (c.moveToNext()) {
                            final String contentValue = c.getString(0);
                            if ((contentValue != null) && (!TextUtils.isEmpty(contentValue))) {
                                fileAs = contentValue;
                                break;
                            }
                        }
                    } finally {
                        c.close();
                    }
                    if (fileAs == null) {
                        // Just in case that property did not exist, we can generate our own
                        // rudimentary string that uses a combination of structured name fields or
                        // email addresses depending on what is available.
                        fileAs = generateFileAs(nameValues, emailValues);
                    }
                    s.data(Tags.CONTACTS_FILE_AS, fileAs);
                }
                // Now, we'll send up groups, if any
                if (!groupIds.isEmpty()) {
                    boolean groupFirst = true;
                    for (final int id: groupIds) {
                        // Since we get id's from the provider, we need to find their names
                        final Cursor c = cr.query(ContentUris.withAppendedId(Groups.CONTENT_URI,
                                id), GROUP_TITLE_PROJECTION, null, null, null);
                        try {
                            // Presumably, this should always succeed, but ...
                            if (c.moveToFirst()) {
                                if (groupFirst) {
                                    s.start(Tags.CONTACTS_CATEGORIES);
                                    groupFirst = false;
                                }
                                s.data(Tags.CONTACTS_CATEGORY, c.getString(0));
                            }
                        } finally {
                            c.close();
                        }
                    }
                    if (!groupFirst) {
                        s.end();
                    }
                }
                s.end().end(); // ApplicationData & Change
                mUpdatedContacts.add(entityValues.getAsLong(ContactsContract.RawContacts._ID));
            }
            if (!first) {
                s.end(); // Commands
            }
        } finally {
            ei.close();
        }

    }

    @Override
    public void cleanup(final Context context, final Account account) {
        final ContentResolver cr = context.getContentResolver();

        // Mark the changed contacts dirty = 0
        // Permanently delete the user deletions
        ContactsSyncParser.ContactOperations ops = new ContactsSyncParser.ContactOperations();
        for (final Long id: mUpdatedContacts) {
            ops.add(ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,
                            id).buildUpon()
                            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                            .build())
                    .withValue(ContactsContract.RawContacts.DIRTY, 0).build());
        }
        for (final Long id: mDeletedContacts) {
            ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(
                    ContactsContract.RawContacts.CONTENT_URI, id).buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build())
                    .build());
        }
        ops.execute(context);
        if (mParser != null && mParser.isGroupsUsed()) {
            // Make sure the title column is set for all of our groups
            // And that all of our groups are visible
            // TODO Perhaps the visible part should only happen when the group is created, but
            // this is fine for now.
            final Uri groupsUri = uriWithAccountAndIsSyncAdapter(Groups.CONTENT_URI,
                    account.mEmailAddress);
            final Cursor c = cr.query(groupsUri, new String[] {Groups.SOURCE_ID, Groups.TITLE},
                    Groups.TITLE + " IS NULL", null, null);
            final ContentValues values = new ContentValues();
            values.put(Groups.GROUP_VISIBLE, 1);
            try {
                while (c.moveToNext()) {
                    final String sourceId = c.getString(0);
                    values.put(Groups.TITLE, sourceId);
                    cr.update(uriWithAccountAndIsSyncAdapter(groupsUri,
                            account.mEmailAddress), values, Groups.SOURCE_ID + "=?",
                            new String[] {sourceId});
                }
            } finally {
                c.close();
            }
        }
    }

    /**
     * Delete an account from the Contacts provider.
     * @param context Our {@link Context}
     * @param emailAddress The email address of the account we wish to delete
     */
    public static void wipeAccountFromContentProvider(final Context context,
            final String emailAddress) {
        try {
            context.getContentResolver().delete(uriWithAccountAndIsSyncAdapter(
                            ContactsContract.RawContacts.CONTENT_URI, emailAddress), null, null);
        } catch (IllegalArgumentException e) {
            LogUtils.e(TAG, "ContactsProvider disabled; unable to wipe account.");
        }
    }
}
