package com.android.exchange.adapter;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.Entity.NamedContentValues;
import android.content.EntityIterator;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
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
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.SyncState;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Base64;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.Eas;
import com.android.exchange.eas.EasSyncCollectionTypeBase;
import com.android.exchange.eas.EasSyncContacts;
import com.android.exchange.utility.CalendarUtilities;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class ContactsSyncParser extends AbstractSyncParser {
    private static final String TAG = Eas.LOG_TAG;

    private static final String SERVER_ID_SELECTION = RawContacts.SOURCE_ID + "=?";
    private static final String CLIENT_ID_SELECTION = RawContacts.SYNC1 + "=?";
    private static final String[] ID_PROJECTION = new String[] {RawContacts._ID};

    private static final ArrayList<NamedContentValues> EMPTY_ARRAY_NAMEDCONTENTVALUES
        = new ArrayList<NamedContentValues>();

    private static final String FOUND_DATA_ROW = "com.android.exchange.FOUND_ROW";

    private static final int MAX_IM_ROWS = 3;
    private static final int MAX_EMAIL_ROWS = 3;
    private static final int MAX_PHONE_ROWS = 2;
    private static final String COMMON_DATA_ROW = Im.DATA;  // Could have been Email.DATA, etc.
    private static final String COMMON_TYPE_ROW = Phone.TYPE; // Could have been any typed row

    String[] mBindArgument = new String[1];
    ContactOperations ops = new ContactOperations();
    private final android.accounts.Account mAccountManagerAccount;
    private final Uri mAccountUri;
    private boolean mGroupsUsed = false;

    public ContactsSyncParser(final Context context, final ContentResolver resolver,
            final InputStream in, final Mailbox mailbox, final Account account,
            final android.accounts.Account accountManagerAccount) throws IOException {
        super(context, resolver, in, mailbox, account);
        mAccountManagerAccount = accountManagerAccount;
        mAccountUri = uriWithAccountAndIsSyncAdapter(RawContacts.CONTENT_URI,
                mAccount.mEmailAddress);
    }

    public boolean isGroupsUsed() {
        return mGroupsUsed;
    }

    public void addData(String serverId, ContactOperations ops, Entity entity)
            throws IOException {
        String prefix = null;
        String firstName = null;
        String lastName = null;
        String middleName = null;
        String suffix = null;
        String companyName = null;
        String yomiFirstName = null;
        String yomiLastName = null;
        String yomiCompanyName = null;
        String title = null;
        String department = null;
        String officeLocation = null;
        Address home = new Address();
        Address work = new Address();
        Address other = new Address();
        EasBusiness business = new EasBusiness();
        EasPersonal personal = new EasPersonal();
        ArrayList<String> children = new ArrayList<String>();
        ArrayList<UntypedRow> emails = new ArrayList<UntypedRow>();
        ArrayList<UntypedRow> ims = new ArrayList<UntypedRow>();
        ArrayList<UntypedRow> homePhones = new ArrayList<UntypedRow>();
        ArrayList<UntypedRow> workPhones = new ArrayList<UntypedRow>();
        if (entity == null) {
            ops.newContact(serverId, mAccount.mEmailAddress);
        }

        while (nextTag(Tags.SYNC_APPLICATION_DATA) != END) {
            switch (tag) {
                case Tags.CONTACTS_FIRST_NAME:
                    firstName = getValue();
                    break;
                case Tags.CONTACTS_LAST_NAME:
                    lastName = getValue();
                    break;
                case Tags.CONTACTS_MIDDLE_NAME:
                    middleName = getValue();
                    break;
                case Tags.CONTACTS_SUFFIX:
                    suffix = getValue();
                    break;
                case Tags.CONTACTS_COMPANY_NAME:
                    companyName = getValue();
                    break;
                case Tags.CONTACTS_JOB_TITLE:
                    title = getValue();
                    break;
                case Tags.CONTACTS_EMAIL1_ADDRESS:
                case Tags.CONTACTS_EMAIL2_ADDRESS:
                case Tags.CONTACTS_EMAIL3_ADDRESS:
                    emails.add(new EmailRow(getValue()));
                    break;
                case Tags.CONTACTS_BUSINESS2_TELEPHONE_NUMBER:
                case Tags.CONTACTS_BUSINESS_TELEPHONE_NUMBER:
                    workPhones.add(new PhoneRow(getValue(), Phone.TYPE_WORK));
                    break;
                case Tags.CONTACTS2_MMS:
                    ops.addPhone(entity, Phone.TYPE_MMS, getValue());
                    break;
                case Tags.CONTACTS_BUSINESS_FAX_NUMBER:
                    ops.addPhone(entity, Phone.TYPE_FAX_WORK, getValue());
                    break;
                case Tags.CONTACTS2_COMPANY_MAIN_PHONE:
                    ops.addPhone(entity, Phone.TYPE_COMPANY_MAIN, getValue());
                    break;
                case Tags.CONTACTS_HOME_FAX_NUMBER:
                    ops.addPhone(entity, Phone.TYPE_FAX_HOME, getValue());
                    break;
                case Tags.CONTACTS_HOME_TELEPHONE_NUMBER:
                case Tags.CONTACTS_HOME2_TELEPHONE_NUMBER:
                    homePhones.add(new PhoneRow(getValue(), Phone.TYPE_HOME));
                    break;
                case Tags.CONTACTS_MOBILE_TELEPHONE_NUMBER:
                    ops.addPhone(entity, Phone.TYPE_MOBILE, getValue());
                    break;
                case Tags.CONTACTS_CAR_TELEPHONE_NUMBER:
                    ops.addPhone(entity, Phone.TYPE_CAR, getValue());
                    break;
                case Tags.CONTACTS_RADIO_TELEPHONE_NUMBER:
                    ops.addPhone(entity, Phone.TYPE_RADIO, getValue());
                    break;
                case Tags.CONTACTS_PAGER_NUMBER:
                    ops.addPhone(entity, Phone.TYPE_PAGER, getValue());
                    break;
                case Tags.CONTACTS_ASSISTANT_TELEPHONE_NUMBER:
                    ops.addPhone(entity, Phone.TYPE_ASSISTANT, getValue());
                    break;
                case Tags.CONTACTS2_IM_ADDRESS:
                case Tags.CONTACTS2_IM_ADDRESS_2:
                case Tags.CONTACTS2_IM_ADDRESS_3:
                    ims.add(new ImRow(getValue()));
                    break;
                case Tags.CONTACTS_BUSINESS_ADDRESS_CITY:
                    work.city = getValue();
                    break;
                case Tags.CONTACTS_BUSINESS_ADDRESS_COUNTRY:
                    work.country = getValue();
                    break;
                case Tags.CONTACTS_BUSINESS_ADDRESS_POSTAL_CODE:
                    work.code = getValue();
                    break;
                case Tags.CONTACTS_BUSINESS_ADDRESS_STATE:
                    work.state = getValue();
                    break;
                case Tags.CONTACTS_BUSINESS_ADDRESS_STREET:
                    work.street = getValue();
                    break;
                case Tags.CONTACTS_HOME_ADDRESS_CITY:
                    home.city = getValue();
                    break;
                case Tags.CONTACTS_HOME_ADDRESS_COUNTRY:
                    home.country = getValue();
                    break;
                case Tags.CONTACTS_HOME_ADDRESS_POSTAL_CODE:
                    home.code = getValue();
                    break;
                case Tags.CONTACTS_HOME_ADDRESS_STATE:
                    home.state = getValue();
                    break;
                case Tags.CONTACTS_HOME_ADDRESS_STREET:
                    home.street = getValue();
                    break;
                case Tags.CONTACTS_OTHER_ADDRESS_CITY:
                    other.city = getValue();
                    break;
                case Tags.CONTACTS_OTHER_ADDRESS_COUNTRY:
                    other.country = getValue();
                    break;
                case Tags.CONTACTS_OTHER_ADDRESS_POSTAL_CODE:
                    other.code = getValue();
                    break;
                case Tags.CONTACTS_OTHER_ADDRESS_STATE:
                    other.state = getValue();
                    break;
                case Tags.CONTACTS_OTHER_ADDRESS_STREET:
                    other.street = getValue();
                    break;

                case Tags.CONTACTS_CHILDREN:
                    childrenParser(children);
                    break;

                case Tags.CONTACTS_YOMI_COMPANY_NAME:
                    yomiCompanyName = getValue();
                    break;
                case Tags.CONTACTS_YOMI_FIRST_NAME:
                    yomiFirstName = getValue();
                    break;
                case Tags.CONTACTS_YOMI_LAST_NAME:
                    yomiLastName = getValue();
                    break;

                case Tags.CONTACTS2_NICKNAME:
                    ops.addNickname(entity, getValue());
                    break;

                case Tags.CONTACTS_ASSISTANT_NAME:
                    ops.addRelation(entity, Relation.TYPE_ASSISTANT, getValue());
                    break;
                case Tags.CONTACTS2_MANAGER_NAME:
                    ops.addRelation(entity, Relation.TYPE_MANAGER, getValue());
                    break;
                case Tags.CONTACTS_SPOUSE:
                    ops.addRelation(entity, Relation.TYPE_SPOUSE, getValue());
                    break;
                case Tags.CONTACTS_DEPARTMENT:
                    department = getValue();
                    break;
                case Tags.CONTACTS_TITLE:
                    prefix = getValue();
                    break;

                // EAS Business
                case Tags.CONTACTS_OFFICE_LOCATION:
                    officeLocation = getValue();
                    break;
                case Tags.CONTACTS2_CUSTOMER_ID:
                    business.customerId = getValue();
                    break;
                case Tags.CONTACTS2_GOVERNMENT_ID:
                    business.governmentId = getValue();
                    break;
                case Tags.CONTACTS2_ACCOUNT_NAME:
                    business.accountName = getValue();
                    break;

                // EAS Personal
                case Tags.CONTACTS_ANNIVERSARY:
                    personal.anniversary = getValue();
                    break;
                case Tags.CONTACTS_FILE_AS:
                    personal.fileAs = getValue();
                    break;
                case Tags.CONTACTS_BIRTHDAY:
                    ops.addBirthday(entity, getValue());
                    break;
                case Tags.CONTACTS_WEBPAGE:
                    ops.addWebpage(entity, getValue());
                    break;

                case Tags.CONTACTS_PICTURE:
                    ops.addPhoto(entity, getValue());
                    break;

                case Tags.BASE_BODY:
                    ops.addNote(entity, bodyParser());
                    break;
                case Tags.CONTACTS_BODY:
                    ops.addNote(entity, getValue());
                    break;

                case Tags.CONTACTS_CATEGORIES:
                    mGroupsUsed = true;
                    categoriesParser(ops, entity);
                    break;

                default:
                    skipTag();
            }
        }

        ops.addName(entity, prefix, firstName, lastName, middleName, suffix,
                yomiFirstName, yomiLastName);
        ops.addBusiness(entity, business);
        ops.addPersonal(entity, personal);

        ops.addUntyped(entity, emails, Email.CONTENT_ITEM_TYPE, -1, MAX_EMAIL_ROWS);
        ops.addUntyped(entity, ims, Im.CONTENT_ITEM_TYPE, -1, MAX_IM_ROWS);
        ops.addUntyped(entity, homePhones, Phone.CONTENT_ITEM_TYPE, Phone.TYPE_HOME,
                MAX_PHONE_ROWS);
        ops.addUntyped(entity, workPhones, Phone.CONTENT_ITEM_TYPE, Phone.TYPE_WORK,
                MAX_PHONE_ROWS);

        if (!children.isEmpty()) {
            ops.addChildren(entity, children);
        }

        if (work.hasData()) {
            ops.addPostal(entity, StructuredPostal.TYPE_WORK, work.street, work.city,
                    work.state, work.country, work.code);
        }
        if (home.hasData()) {
            ops.addPostal(entity, StructuredPostal.TYPE_HOME, home.street, home.city,
                    home.state, home.country, home.code);
        }
        if (other.hasData()) {
            ops.addPostal(entity, StructuredPostal.TYPE_OTHER, other.street, other.city,
                    other.state, other.country, other.code);
        }

        if (companyName != null) {
            ops.addOrganization(entity, Organization.TYPE_WORK, companyName, title, department,
                    yomiCompanyName, officeLocation);
        }

        if (entity != null) {
            // We've been removing rows from the list as they've been found in the xml
            // Any that are left must have been deleted on the server
            ArrayList<NamedContentValues> ncvList = entity.getSubValues();
            for (NamedContentValues ncv: ncvList) {
                // These rows need to be deleted...
                Uri u = dataUriFromNamedContentValues(ncv);
                ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterParameter(u))
                        .build());
            }
        }
    }

    private void categoriesParser(ContactOperations ops, Entity entity) throws IOException {
        while (nextTag(Tags.CONTACTS_CATEGORIES) != END) {
            switch (tag) {
                case Tags.CONTACTS_CATEGORY:
                    ops.addGroup(entity, getValue());
                    break;
                default:
                    skipTag();
            }
        }
    }

    private void childrenParser(ArrayList<String> children) throws IOException {
        while (nextTag(Tags.CONTACTS_CHILDREN) != END) {
            switch (tag) {
                case Tags.CONTACTS_CHILD:
                    if (children.size() < EasChildren.MAX_CHILDREN) {
                        children.add(getValue());
                    }
                    break;
                default:
                    skipTag();
            }
        }
    }

    private String bodyParser() throws IOException {
        String body = null;
        while (nextTag(Tags.BASE_BODY) != END) {
            switch (tag) {
                case Tags.BASE_DATA:
                    body = getValue();
                    break;
                default:
                    skipTag();
            }
        }
        return body;
    }

    public void addParser(ContactOperations ops) throws IOException {
        String serverId = null;
        while (nextTag(Tags.SYNC_ADD) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID: // same as
                    serverId = getValue();
                    break;
                case Tags.SYNC_APPLICATION_DATA:
                    addData(serverId, ops, null);
                    break;
                default:
                    skipTag();
            }
        }
    }

    private Cursor getServerIdCursor(String serverId) {
        mBindArgument[0] = serverId;
        return mContentResolver.query(mAccountUri, ID_PROJECTION, SERVER_ID_SELECTION,
                mBindArgument, null);
    }

    private Cursor getClientIdCursor(String clientId) {
        mBindArgument[0] = clientId;
        return mContentResolver.query(mAccountUri, ID_PROJECTION, CLIENT_ID_SELECTION,
                mBindArgument, null);
    }

    public void deleteParser(ContactOperations ops) throws IOException {
        while (nextTag(Tags.SYNC_DELETE) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    String serverId = getValue();
                    // Find the message in this mailbox with the given serverId
                    Cursor c = getServerIdCursor(serverId);
                    try {
                        if (c.moveToFirst()) {
                            userLog("Deleting ", serverId);
                            ops.delete(c.getLong(0));
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

    class ServerChange {
        long id;
        boolean read;

        ServerChange(long _id, boolean _read) {
            id = _id;
            read = _read;
        }
    }

    /**
     * Changes are handled row by row, and only changed/new rows are acted upon
     * @param ops the array of pending ContactProviderOperations.
     * @throws IOException
     */
    public void changeParser(ContactOperations ops) throws IOException {
        String serverId = null;
        Entity entity = null;
        while (nextTag(Tags.SYNC_CHANGE) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    serverId = getValue();
                    Cursor c = getServerIdCursor(serverId);
                    try {
                        if (c.moveToFirst()) {
                            // TODO Handle deleted individual rows...
                            Uri uri = ContentUris.withAppendedId(
                                    RawContacts.CONTENT_URI, c.getLong(0));
                            uri = Uri.withAppendedPath(
                                    uri, RawContacts.Entity.CONTENT_DIRECTORY);
                            final Cursor cursor = mContentResolver.query(uri,
                                    null, null, null, null);
                            if (cursor != null) {
                                final EntityIterator entityIterator =
                                    RawContacts.newEntityIterator(cursor);
                                if (entityIterator.hasNext()) {
                                    entity = entityIterator.next();
                                }
                                userLog("Changing contact ", serverId);
                            }
                        }
                    } finally {
                        c.close();
                    }
                    break;
                case Tags.SYNC_APPLICATION_DATA:
                    addData(serverId, ops, entity);
                    break;
                default:
                    skipTag();
            }
        }
    }

    @Override
    public void commandsParser() throws IOException {
        while (nextTag(Tags.SYNC_COMMANDS) != END) {
            if (tag == Tags.SYNC_ADD) {
                addParser(ops);
            } else if (tag == Tags.SYNC_DELETE) {
                deleteParser(ops);
            } else if (tag == Tags.SYNC_CHANGE) {
                changeParser(ops);
            } else
                skipTag();
        }
    }

    @Override
    public void commit() throws IOException {
       // Save the syncKey here, using the Helper provider by Contacts provider
        userLog("Contacts SyncKey saved as: ", mMailbox.mSyncKey);
        ops.add(SyncStateContract.Helpers.newSetOperation(SyncState.CONTENT_URI,
                mAccountManagerAccount, mMailbox.mSyncKey.getBytes()));

        // Execute these all at once...
        ops.execute(mContext);

        if (ops.mResults != null && ops.mResults.length > 0) {
            final ContentValues cv = new ContentValues();
            cv.put(RawContacts.DIRTY, 0);
            for (int i = 0; i < ops.mContactIndexCount; i++) {
                final int index = ops.mContactIndexArray[i];
                final Uri u = index < ops.mResults.length ? ops.mResults[index].uri : null;
                if (u != null) {
                    String idString = u.getLastPathSegment();
                    mContentResolver.update(
                            addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI), cv,
                            RawContacts._ID + "=" + idString, null);
                }
            }
        }
    }

    public void addResponsesParser() throws IOException {
        String serverId = null;
        String clientId = null;
        ContentValues cv = new ContentValues();
        while (nextTag(Tags.SYNC_ADD) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    serverId = getValue();
                    break;
                case Tags.SYNC_CLIENT_ID:
                    clientId = getValue();
                    break;
                case Tags.SYNC_STATUS:
                    getValue();
                    break;
                default:
                    skipTag();
            }
        }

        // This is theoretically impossible, but...
        if (clientId == null || serverId == null) return;

        Cursor c = getClientIdCursor(clientId);
        try {
            if (c.moveToFirst()) {
                cv.put(RawContacts.SOURCE_ID, serverId);
                cv.put(RawContacts.DIRTY, 0);
                ops.add(ContentProviderOperation.newUpdate(
                        ContentUris.withAppendedId(
                                addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI),
                                c.getLong(0)))
                        .withValues(cv)
                        .build());
                userLog("New contact " + clientId + " was given serverId: " + serverId);
            }
        } finally {
            c.close();
        }
    }

    public void changeResponsesParser() throws IOException {
        String serverId = null;
        String status = null;
        while (nextTag(Tags.SYNC_CHANGE) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    serverId = getValue();
                    break;
                case Tags.SYNC_STATUS:
                    status = getValue();
                    break;
                default:
                    skipTag();
            }
        }
        if (serverId != null && status != null) {
            userLog("Changed contact " + serverId + " failed with status: " + status);
        }
    }


    @Override
    public void responsesParser() throws IOException {
        // Handle server responses here (for Add and Change)
        while (nextTag(Tags.SYNC_RESPONSES) != END) {
            if (tag == Tags.SYNC_ADD) {
                addResponsesParser();
            } else if (tag == Tags.SYNC_CHANGE) {
                changeResponsesParser();
            } else
                skipTag();
        }
    }

    private static Uri uriWithAccountAndIsSyncAdapter(final Uri uri, final String emailAddress) {
        return uri.buildUpon()
            .appendQueryParameter(RawContacts.ACCOUNT_NAME, emailAddress)
            .appendQueryParameter(RawContacts.ACCOUNT_TYPE, Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE)
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build();
    }

    static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
    }

    /**
     * Generate the uri for the data row associated with this NamedContentValues object
     * @param ncv the NamedContentValues object
     * @return a uri that can be used to refer to this row
     */
    public static Uri dataUriFromNamedContentValues(NamedContentValues ncv) {
        long id = ncv.values.getAsLong(RawContacts._ID);
        Uri dataUri = ContentUris.withAppendedId(ncv.uri, id);
        return dataUri;
    }

    public static final class EasChildren {
        private EasChildren() {}

        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/eas_children";
        public static final int MAX_CHILDREN = 8;
        public static final String[] ROWS =
            new String[] {"data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9"};
    }

    public static final class EasPersonal {
        String anniversary;
        String fileAs;

            /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/eas_personal";
        public static final String ANNIVERSARY = "data2";
        public static final String FILE_AS = "data4";

        boolean hasData() {
            return anniversary != null || fileAs != null;
        }
    }

    public static final class EasBusiness {
        String customerId;
        String governmentId;
        String accountName;

        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/eas_business";
        public static final String CUSTOMER_ID = "data6";
        public static final String GOVERNMENT_ID = "data7";
        public static final String ACCOUNT_NAME = "data8";

        boolean hasData() {
            return customerId != null || governmentId != null || accountName != null;
        }
    }

    public static final class Address {
        String city;
        String country;
        String code;
        String street;
        String state;

        boolean hasData() {
            return city != null || country != null || code != null || state != null
                || street != null;
        }
    }

    interface UntypedRow {
        public void addValues(RowBuilder builder);
        public boolean isSameAs(int type, String value);
    }

    static class EmailRow implements UntypedRow {
        String email;
        String displayName;

        public EmailRow(String _email) {
            Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(_email);
            // Can't happen, but belt & suspenders
            if (tokens.length == 0) {
                email = "";
                displayName = "";
            } else {
                Rfc822Token token = tokens[0];
                email = token.getAddress();
                displayName = token.getName();
            }
        }

        @Override
        public void addValues(RowBuilder builder) {
            builder.withValue(Email.DATA, email);
            builder.withValue(Email.DISPLAY_NAME, displayName);
        }

        @Override
        public boolean isSameAs(int type, String value) {
            return email.equalsIgnoreCase(value);
        }
    }

    static class ImRow implements UntypedRow {
        String im;

        public ImRow(String _im) {
            im = _im;
        }

        @Override
        public void addValues(RowBuilder builder) {
            builder.withValue(Im.DATA, im);
        }

        @Override
        public boolean isSameAs(int type, String value) {
            return im.equalsIgnoreCase(value);
        }
    }

    static class PhoneRow implements UntypedRow {
        String phone;
        int type;

        public PhoneRow(String _phone, int _type) {
            phone = _phone;
            type = _type;
        }

        @Override
        public void addValues(RowBuilder builder) {
            builder.withValue(Im.DATA, phone);
            builder.withValue(Phone.TYPE, type);
        }

        @Override
        public boolean isSameAs(int _type, String value) {
            return type == _type && phone.equalsIgnoreCase(value);
        }
    }

    /**
     * RowBuilder is a wrapper for the Builder class that is used to create/update rows for a
     * ContentProvider.  It has, in addition to the Builder, ContentValues which, if present,
     * represent the current values of that row, that can be compared against current values to
     * see whether an update is even necessary.  The methods on SmartBuilder are delegated to
     * the Builder.
     */
    private static class RowBuilder {
        Builder builder;
        ContentValues cv;

        public RowBuilder(Builder _builder) {
            builder = _builder;
        }

        public RowBuilder(Builder _builder, NamedContentValues _ncv) {
            builder = _builder;
            cv = _ncv.values;
        }

        RowBuilder withValueBackReference(String key, int previousResult) {
            builder.withValueBackReference(key, previousResult);
            return this;
        }

        ContentProviderOperation build() {
            return builder.build();
        }

        RowBuilder withValue(String key, Object value) {
            builder.withValue(key, value);
            return this;
        }
    }
    public static class ContactOperations extends ArrayList<ContentProviderOperation> {
        private static final long serialVersionUID = 1L;
        private int mCount = 0;
        private int mContactBackValue = mCount;
        // Make an array big enough for the max possible window size.
        private final int[] mContactIndexArray = new int[EasSyncCollectionTypeBase.MAX_WINDOW_SIZE];
        private int mContactIndexCount = 0;
        private ContentProviderResult[] mResults = null;

        @Override
        public boolean add(ContentProviderOperation op) {
            super.add(op);
            mCount++;
            return true;
        }

        public void newContact(final String serverId, final String emailAddress) {
            Builder builder = ContentProviderOperation.newInsert(
                    uriWithAccountAndIsSyncAdapter(RawContacts.CONTENT_URI, emailAddress));
            ContentValues values = new ContentValues();
            values.put(RawContacts.SOURCE_ID, serverId);
            builder.withValues(values);
            mContactBackValue = mCount;
            mContactIndexArray[mContactIndexCount++] = mCount;
            add(builder.build());
        }

        public void delete(long id) {
            add(ContentProviderOperation
                    .newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id)
                            .buildUpon()
                            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                            .build())
                    .build());
        }

        public void execute(final Context context) {
            try {
                if (!isEmpty()) {
                    mResults = context.getContentResolver().applyBatch(
                            ContactsContract.AUTHORITY, this);
                }
            } catch (RemoteException e) {
                // There is nothing sensible to be done here
                LogUtils.e(TAG, "problem inserting contact during server update", e);
            } catch (OperationApplicationException e) {
                // There is nothing sensible to be done here
                LogUtils.e(TAG, "problem inserting contact during server update", e);
            } catch (IllegalArgumentException e) {
                // CP2 has been disabled
                LogUtils.e(TAG, "CP2 is disabled; unable to insert contact.");
            }
        }

        /**
         * Given the list of NamedContentValues for an entity, a mime type, and a subtype,
         * tries to find a match, returning it
         * @param list the list of NCV's from the contact entity
         * @param contentItemType the mime type we're looking for
         * @param type the subtype (e.g. HOME, WORK, etc.)
         * @return the matching NCV or null if not found
         */
        private static NamedContentValues findTypedData(ArrayList<NamedContentValues> list,
                String contentItemType, int type, String stringType) {
            NamedContentValues result = null;
            if (contentItemType == null) {
                return result;
            }

            // Loop through the ncv's, looking for an existing row
            for (NamedContentValues namedContentValues: list) {
                final Uri uri = namedContentValues.uri;
                final ContentValues cv = namedContentValues.values;
                if (Data.CONTENT_URI.equals(uri)) {
                    final String mimeType = cv.getAsString(Data.MIMETYPE);
                    if (TextUtils.equals(mimeType, contentItemType)) {
                        if (stringType != null) {
                            if (cv.getAsString(GroupMembership.GROUP_ROW_ID).equals(stringType)) {
                                result = namedContentValues;
                            }
                        // Note Email.TYPE could be ANY type column; they are all defined in
                        // the private CommonColumns class in ContactsContract
                        // We'll accept either type < 0 (don't care), cv doesn't have a type,
                        // or the types are equal
                        } else if (type < 0 || !cv.containsKey(Email.TYPE) ||
                                cv.getAsInteger(Email.TYPE) == type) {
                            result = namedContentValues;
                        }
                    }
                }
            }

            // If we've found an existing data row, we'll delete it.  Any rows left at the
            // end should be deleted...
            if (result != null) {
                list.remove(result);
            }

            // Return the row found (or null)
            return result;
        }

        /**
         * Given the list of NamedContentValues for an entity and a mime type
         * gather all of the matching NCV's, returning them
         * @param list the list of NCV's from the contact entity
         * @param contentItemType the mime type we're looking for
         * @param type the subtype (e.g. HOME, WORK, etc.)
         * @return the matching NCVs
         */
        private static ArrayList<NamedContentValues> findUntypedData(
                ArrayList<NamedContentValues> list, int type, String contentItemType) {
            final ArrayList<NamedContentValues> result = new ArrayList<NamedContentValues>();
            if (contentItemType == null) {
                return result;
            }

            // Loop through the ncv's, looking for an existing row
            for (NamedContentValues namedContentValues: list) {
                final Uri uri = namedContentValues.uri;
                final ContentValues cv = namedContentValues.values;
                if (Data.CONTENT_URI.equals(uri)) {
                    final String mimeType = cv.getAsString(Data.MIMETYPE);
                    if (TextUtils.equals(mimeType, contentItemType)) {
                        if (type != -1) {
                            final int subtype = cv.getAsInteger(Phone.TYPE);
                            if (type != subtype) {
                                continue;
                            }
                        }
                        result.add(namedContentValues);
                    }
                }
            }

            // If we've found an existing data row, we'll delete it.  Any rows left at the
            // end should be deleted...
            for (NamedContentValues values : result) {
                list.remove(values);
            }

            // Return the row found (or null)
            return result;
        }

        /**
         * Create a wrapper for a builder (insert or update) that also includes the NCV for
         * an existing row of this type.   If the SmartBuilder's cv field is not null, then
         * it represents the current (old) values of this field.  The caller can then check
         * whether the field is now different and needs to be updated; if it's not different,
         * the caller will simply return and not generate a new CPO.  Otherwise, the builder
         * should have its content values set, and the built CPO should be added to the
         * ContactOperations list.
         *
         * @param entity the contact entity (or null if this is a new contact)
         * @param mimeType the mime type of this row
         * @param type the subtype of this row
         * @param stringType for groups, the name of the group (type will be ignored), or null
         * @return the created SmartBuilder
         */
        public RowBuilder createBuilder(Entity entity, String mimeType, int type,
                String stringType) {
            RowBuilder builder = null;

            if (entity != null) {
                NamedContentValues ncv =
                    findTypedData(entity.getSubValues(), mimeType, type, stringType);
                if (ncv != null) {
                    builder = new RowBuilder(
                            ContentProviderOperation
                                .newUpdate(addCallerIsSyncAdapterParameter(
                                    dataUriFromNamedContentValues(ncv))),
                            ncv);
                }
            }

            if (builder == null) {
                builder = newRowBuilder(entity, mimeType);
            }

            // Return the appropriate builder (insert or update)
            // Caller will fill in the appropriate values; 4 MIMETYPE is already set
            return builder;
        }

        private RowBuilder typedRowBuilder(Entity entity, String mimeType, int type) {
            return createBuilder(entity, mimeType, type, null);
        }

        private RowBuilder untypedRowBuilder(Entity entity, String mimeType) {
            return createBuilder(entity, mimeType, -1, null);
        }

        private RowBuilder newRowBuilder(Entity entity, String mimeType) {
            // This is a new row; first get the contactId
            // If the Contact is new, use the saved back value; otherwise the value in the entity
            int contactId = mContactBackValue;
            if (entity != null) {
                contactId = entity.getEntityValues().getAsInteger(RawContacts._ID);
            }

            // Create an insert operation with the proper contactId reference
            RowBuilder builder =
                new RowBuilder(ContentProviderOperation.newInsert(
                        addCallerIsSyncAdapterParameter(Data.CONTENT_URI)));
            if (entity == null) {
                builder.withValueBackReference(Data.RAW_CONTACT_ID, contactId);
            } else {
                builder.withValue(Data.RAW_CONTACT_ID, contactId);
            }

            // Set the mime type of the row
            builder.withValue(Data.MIMETYPE, mimeType);
            return builder;
        }

        /**
         * Compare a column in a ContentValues with an (old) value, and see if they are the
         * same.  For this purpose, null and an empty string are considered the same.
         * @param cv a ContentValues object, from a NamedContentValues
         * @param column a column that might be in the ContentValues
         * @param oldValue an old value (or null) to check against
         * @return whether the column's value in the ContentValues matches oldValue
         */
        private static boolean cvCompareString(ContentValues cv, String column, String oldValue) {
            if (cv.containsKey(column)) {
                if (oldValue != null && cv.getAsString(column).equals(oldValue)) {
                    return true;
                }
            } else if (oldValue == null || oldValue.length() == 0) {
                return true;
            }
            return false;
        }

        public void addChildren(Entity entity, ArrayList<String> children) {
            RowBuilder builder = untypedRowBuilder(entity, EasChildren.CONTENT_ITEM_TYPE);
            int i = 0;
            for (String child: children) {
                builder.withValue(EasChildren.ROWS[i++], child);
            }
            add(builder.build());
        }

        public void addGroup(Entity entity, String group) {
            RowBuilder builder =
                createBuilder(entity, GroupMembership.CONTENT_ITEM_TYPE, -1, group);
            builder.withValue(GroupMembership.GROUP_SOURCE_ID, group);
            add(builder.build());
        }

        public void addBirthday(Entity entity, String birthday) {
            RowBuilder builder =
                    typedRowBuilder(entity, Event.CONTENT_ITEM_TYPE, Event.TYPE_BIRTHDAY);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, Event.START_DATE, birthday)) {
                return;
            }
            // TODO: Store the date in the format expected by EAS servers.
            final long millis;
            try {
                millis = Utility.parseEmailDateTimeToMillis(birthday);
            } catch (ParseException e) {
                LogUtils.w(TAG, "Parse error for birthday date field.", e);
                return;
            }
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            cal.setTimeInMillis(millis);
            if (cal.get(GregorianCalendar.HOUR_OF_DAY) >= 12) {
                cal.add(GregorianCalendar.DATE, 1);
            }
            String realBirthday = CalendarUtilities.calendarToBirthdayString(cal);
            builder.withValue(Event.START_DATE, realBirthday);
            builder.withValue(Event.TYPE, Event.TYPE_BIRTHDAY);
            add(builder.build());
        }

        public void addName(Entity entity, String prefix, String givenName, String familyName,
                String middleName, String suffix, String yomiFirstName, String yomiLastName) {
            RowBuilder builder = untypedRowBuilder(entity, StructuredName.CONTENT_ITEM_TYPE);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, StructuredName.GIVEN_NAME, givenName) &&
                    cvCompareString(cv, StructuredName.FAMILY_NAME, familyName) &&
                    cvCompareString(cv, StructuredName.MIDDLE_NAME, middleName) &&
                    cvCompareString(cv, StructuredName.PREFIX, prefix) &&
                    cvCompareString(cv, StructuredName.PHONETIC_GIVEN_NAME, yomiFirstName) &&
                    cvCompareString(cv, StructuredName.PHONETIC_FAMILY_NAME, yomiLastName) &&
                    cvCompareString(cv, StructuredName.SUFFIX, suffix)) {
                return;
            }
            builder.withValue(StructuredName.GIVEN_NAME, givenName);
            builder.withValue(StructuredName.FAMILY_NAME, familyName);
            builder.withValue(StructuredName.MIDDLE_NAME, middleName);
            builder.withValue(StructuredName.SUFFIX, suffix);
            builder.withValue(StructuredName.PHONETIC_GIVEN_NAME, yomiFirstName);
            builder.withValue(StructuredName.PHONETIC_FAMILY_NAME, yomiLastName);
            builder.withValue(StructuredName.PREFIX, prefix);
            add(builder.build());
        }

        public void addPersonal(Entity entity, EasPersonal personal) {
            RowBuilder builder = untypedRowBuilder(entity, EasPersonal.CONTENT_ITEM_TYPE);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, EasPersonal.ANNIVERSARY, personal.anniversary) &&
                    cvCompareString(cv, EasPersonal.FILE_AS , personal.fileAs)) {
                return;
            }
            if (!personal.hasData()) {
                return;
            }
            builder.withValue(EasPersonal.FILE_AS, personal.fileAs);
            builder.withValue(EasPersonal.ANNIVERSARY, personal.anniversary);
            add(builder.build());
        }

        public void addBusiness(Entity entity, EasBusiness business) {
            RowBuilder builder = untypedRowBuilder(entity, EasBusiness.CONTENT_ITEM_TYPE);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, EasBusiness.ACCOUNT_NAME, business.accountName) &&
                    cvCompareString(cv, EasBusiness.CUSTOMER_ID, business.customerId) &&
                    cvCompareString(cv, EasBusiness.GOVERNMENT_ID, business.governmentId)) {
                return;
            }
            if (!business.hasData()) {
                return;
            }
            builder.withValue(EasBusiness.ACCOUNT_NAME, business.accountName);
            builder.withValue(EasBusiness.CUSTOMER_ID, business.customerId);
            builder.withValue(EasBusiness.GOVERNMENT_ID, business.governmentId);
            add(builder.build());
        }

        public void addPhoto(Entity entity, String photo) {
            // We're always going to add this; it's not worth trying to figure out whether the
            // picture is the same as the one stored.
            final byte[] pic;
            try {
                pic = Base64.decode(photo, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                LogUtils.w(TAG, "Bad base-64 encoding; unable to decode photo.");
                return;
            }

            final RowBuilder builder = untypedRowBuilder(entity, Photo.CONTENT_ITEM_TYPE);
            builder.withValue(Photo.PHOTO, pic);
            add(builder.build());
        }

        public void addPhone(Entity entity, int type, String phone) {
            RowBuilder builder = typedRowBuilder(entity, Phone.CONTENT_ITEM_TYPE, type);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, Phone.NUMBER, phone)) {
                return;
            }
            builder.withValue(Phone.TYPE, type);
            builder.withValue(Phone.NUMBER, phone);
            add(builder.build());
        }

        public void addWebpage(Entity entity, String url) {
            RowBuilder builder = untypedRowBuilder(entity, Website.CONTENT_ITEM_TYPE);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, Website.URL, url)) {
                return;
            }
            builder.withValue(Website.TYPE, Website.TYPE_WORK);
            builder.withValue(Website.URL, url);
            add(builder.build());
        }

        public void addRelation(Entity entity, int type, String value) {
            RowBuilder builder = typedRowBuilder(entity, Relation.CONTENT_ITEM_TYPE, type);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, Relation.DATA, value)) {
                return;
            }
            builder.withValue(Relation.TYPE, type);
            builder.withValue(Relation.DATA, value);
            add(builder.build());
        }

        public void addNickname(Entity entity, String name) {
            RowBuilder builder =
                typedRowBuilder(entity, Nickname.CONTENT_ITEM_TYPE, Nickname.TYPE_DEFAULT);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, Nickname.NAME, name)) {
                return;
            }
            builder.withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT);
            builder.withValue(Nickname.NAME, name);
            add(builder.build());
        }

        public void addPostal(Entity entity, int type, String street, String city, String state,
                String country, String code) {
            RowBuilder builder = typedRowBuilder(entity, StructuredPostal.CONTENT_ITEM_TYPE,
                    type);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, StructuredPostal.CITY, city) &&
                    cvCompareString(cv, StructuredPostal.STREET, street) &&
                    cvCompareString(cv, StructuredPostal.COUNTRY, country) &&
                    cvCompareString(cv, StructuredPostal.POSTCODE, code) &&
                    cvCompareString(cv, StructuredPostal.REGION, state)) {
                return;
            }
            builder.withValue(StructuredPostal.TYPE, type);
            builder.withValue(StructuredPostal.CITY, city);
            builder.withValue(StructuredPostal.STREET, street);
            builder.withValue(StructuredPostal.COUNTRY, country);
            builder.withValue(StructuredPostal.POSTCODE, code);
            builder.withValue(StructuredPostal.REGION, state);
            add(builder.build());
        }

       /**
         * We now are dealing with up to maxRows typeless rows of mimeType data.  We need to try to
         * match them with existing rows; if there's a match, everything's great.  Otherwise, we
         * either need to add a new row for the data, or we have to replace an existing one
         * that no longer matches.  This is similar to the way Emails are handled.
         */
        public void addUntyped(Entity entity, ArrayList<UntypedRow> rows, String mimeType,
                int type, int maxRows) {
            // Make a list of all same type rows in the existing entity
            ArrayList<NamedContentValues> oldValues = EMPTY_ARRAY_NAMEDCONTENTVALUES;
            ArrayList<NamedContentValues> entityValues = EMPTY_ARRAY_NAMEDCONTENTVALUES;
            if (entity != null) {
                oldValues = findUntypedData(entityValues, type, mimeType);
                entityValues = entity.getSubValues();
            }

            // These will be rows needing replacement with new values
            ArrayList<UntypedRow> rowsToReplace = new ArrayList<UntypedRow>();

            // The count of existing rows
            int numRows = oldValues.size();
            for (UntypedRow row: rows) {
                boolean found = false;
                // If we already have this row, mark it
                for (NamedContentValues ncv: oldValues) {
                    ContentValues cv = ncv.values;
                    String data = cv.getAsString(COMMON_DATA_ROW);
                    int rowType = -1;
                    if (cv.containsKey(COMMON_TYPE_ROW)) {
                        rowType = cv.getAsInteger(COMMON_TYPE_ROW);
                    }
                    if (row.isSameAs(rowType, data)) {
                        cv.put(FOUND_DATA_ROW, true);
                        // Remove this to indicate it's still being used
                        entityValues.remove(ncv);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // If we don't, there are two possibilities
                    if (numRows < maxRows) {
                        // If there are available rows, add a new one
                        RowBuilder builder = newRowBuilder(entity, mimeType);
                        row.addValues(builder);
                        add(builder.build());
                        numRows++;
                    } else {
                        // Otherwise, say we need to replace a row with this
                        rowsToReplace.add(row);
                    }
                }
            }

            // Go through rows needing replacement
            for (UntypedRow row: rowsToReplace) {
                for (NamedContentValues ncv: oldValues) {
                    ContentValues cv = ncv.values;
                    // Find a row that hasn't been used (i.e. doesn't match current rows)
                    if (!cv.containsKey(FOUND_DATA_ROW)) {
                        // And update it
                        RowBuilder builder = new RowBuilder(
                                ContentProviderOperation
                                    .newUpdate(addCallerIsSyncAdapterParameter(
                                        dataUriFromNamedContentValues(ncv))),
                                ncv);
                        row.addValues(builder);
                        add(builder.build());
                    }
                }
            }
        }

        public void addOrganization(Entity entity, int type, String company, String title,
                String department, String yomiCompanyName, String officeLocation) {
            RowBuilder builder = typedRowBuilder(entity, Organization.CONTENT_ITEM_TYPE, type);
            ContentValues cv = builder.cv;
            if (cv != null && cvCompareString(cv, Organization.COMPANY, company) &&
                    cvCompareString(cv, Organization.PHONETIC_NAME, yomiCompanyName) &&
                    cvCompareString(cv, Organization.DEPARTMENT, department) &&
                    cvCompareString(cv, Organization.TITLE, title) &&
                    cvCompareString(cv, Organization.OFFICE_LOCATION, officeLocation)) {
                return;
            }
            builder.withValue(Organization.TYPE, type);
            builder.withValue(Organization.COMPANY, company);
            builder.withValue(Organization.TITLE, title);
            builder.withValue(Organization.DEPARTMENT, department);
            builder.withValue(Organization.PHONETIC_NAME, yomiCompanyName);
            builder.withValue(Organization.OFFICE_LOCATION, officeLocation);
            add(builder.build());
        }

        public void addNote(Entity entity, String note) {
            RowBuilder builder = typedRowBuilder(entity, Note.CONTENT_ITEM_TYPE, -1);
            ContentValues cv = builder.cv;
            if (note == null) return;
            note = note.replaceAll("\r\n", "\n");
            if (cv != null && cvCompareString(cv, Note.NOTE, note)) {
                return;
            }

            // Reject notes with nothing in them.  Often, we get something from Outlook when
            // nothing was ever entered.  Sigh.
            int len = note.length();
            int i = 0;
            for (; i < len; i++) {
                char c = note.charAt(i);
                if (!Character.isWhitespace(c)) {
                    break;
                }
            }
            if (i == len) return;

            builder.withValue(Note.NOTE, note);
            add(builder.build());
        }
    }

    @Override
    protected void wipe() {
        LogUtils.w(TAG, "Wiping contacts for account %d", mAccount.mId);
        EasSyncContacts.wipeAccountFromContentProvider(mContext,
                mAccount.mEmailAddress);
    }
}
