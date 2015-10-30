package com.android.exchange.eas;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.EventsEntity;
import android.provider.CalendarContract.ExtendedProperties;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;
import com.android.emailcommon.TrafficFlags;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.Eas;
import com.android.exchange.R;
import com.android.exchange.adapter.AbstractSyncParser;
import com.android.exchange.adapter.CalendarSyncParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.utility.CalendarUtilities;
import com.android.mail.utils.LogUtils;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Performs an Exchange Sync for a Calendar collection.
 */
public class EasSyncCalendar extends EasSyncCollectionTypeBase {
    private static final String TAG = Eas.LOG_TAG;

    // TODO: Some constants are copied from CalendarSyncAdapter and are still used by the parser.
    // These values need to stay in sync; when the parser is cleaned up, be sure to unify them.

    private static final int PIM_WINDOW_SIZE_CALENDAR = 10;

    /** Projection for getting a calendar id. */
    private static final String[] CALENDAR_ID_PROJECTION = { Calendars._ID };
    private static final int CALENDAR_ID_COLUMN = 0;

    /** Content selection for getting a calendar id for an account. */
    private static final String CALENDAR_SELECTION_ACCOUNT_AND_SYNC_ID =
            Calendars.ACCOUNT_NAME + "=? AND " +
            Calendars.ACCOUNT_TYPE + "=? AND " +
            Calendars._SYNC_ID + "=?";

    /** Content selection for getting a calendar id for an account. */
    private static final String CALENDAR_SELECTION_ACCOUNT_AND_NO_SYNC =
            Calendars.ACCOUNT_NAME + "=? AND " +
            Calendars.ACCOUNT_TYPE + "=? AND " +
            Calendars._SYNC_ID + " IS NULL";

    /** The column used to track the timezone of the event. */
    private static final String EVENT_SAVED_TIMEZONE_COLUMN = Events.SYNC_DATA1;

    /** Used to keep track of exception vs. parent event dirtiness. */
    private static final String EVENT_SYNC_MARK = Events.SYNC_DATA8;

    /** The column used to track the Event version sequence number. */
    private static final String EVENT_SYNC_VERSION = Events.SYNC_DATA4;

    /** Projection for getting info about changed events. */
    private static final String[] ORIGINAL_EVENT_PROJECTION = { Events.ORIGINAL_ID, Events._ID };
    private static final int ORIGINAL_EVENT_ORIGINAL_ID_COLUMN = 0;
    private static final int ORIGINAL_EVENT_ID_COLUMN = 1;

    /** Content selection for dirty calendar events. */
    private static final String DIRTY_EXCEPTION_IN_CALENDAR = Events.DIRTY + "=1 AND " +
            Events.ORIGINAL_ID + " NOTNULL AND " + Events.CALENDAR_ID + "=?";

    /** Where clause for updating dirty events. */
    private static final String EVENT_ID_AND_CALENDAR_ID = Events._ID + "=? AND " +
            Events.ORIGINAL_SYNC_ID + " ISNULL AND " + Events.CALENDAR_ID + "=?";

    /** Content selection for dirty or marked top level events. */
    private static final String DIRTY_OR_MARKED_TOP_LEVEL_IN_CALENDAR = "(" + Events.DIRTY +
            "=1 OR " + EVENT_SYNC_MARK + "= 1) AND " + Events.ORIGINAL_ID + " ISNULL AND " +
            Events.CALENDAR_ID + "=?";

    /** Content selection for getting events when handling exceptions. */
    private static final String ORIGINAL_EVENT_AND_CALENDAR = Events.ORIGINAL_SYNC_ID + "=? AND " +
            Events.CALENDAR_ID + "=?";

    private static final String CATEGORY_TOKENIZER_DELIMITER = "\\";
    private static final String ATTENDEE_TOKENIZER_DELIMITER = CATEGORY_TOKENIZER_DELIMITER;

    /** Used to indicate that upsyncs aren't allowed (we catch this in sendLocalChanges) */
    private static final String EXTENDED_PROPERTY_UPSYNC_PROHIBITED = "upsyncProhibited";

    private static final String EXTENDED_PROPERTY_USER_ATTENDEE_STATUS = "userAttendeeStatus";
    private static final String EXTENDED_PROPERTY_ATTENDEES = "attendees";
    private static final String EXTENDED_PROPERTY_CATEGORIES = "categories";

    private final android.accounts.Account mAndroidAccount;
    private final long mCalendarId;

    // The following lists are populated as part of upsync, and handled during cleanup.
    /** Ids of events that were deleted in this upsync. */
    private final ArrayList<Long> mDeletedIdList = new ArrayList<Long>();
    /** Ids of events that were changed in this upsync. */
    private final ArrayList<Long> mUploadedIdList = new ArrayList<Long>();
    /** Emails that need to be sent due to this upsync. */
    private final ArrayList<Message> mOutgoingMailList = new ArrayList<Message>();

    public EasSyncCalendar(final Context context, final Account account,
            final Mailbox mailbox) {
        super();
        mAndroidAccount = new android.accounts.Account(account.mEmailAddress,
            Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
        final ContentResolver cr = context.getContentResolver();
        final Cursor c = cr.query(Calendars.CONTENT_URI, CALENDAR_ID_PROJECTION,
                CALENDAR_SELECTION_ACCOUNT_AND_SYNC_ID,
                new String[] {
                        account.mEmailAddress,
                        Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE,
                        mailbox.mServerId,
                }, null);
        if (c == null) {
            mCalendarId = -1;
        } else {
            try {
                if (c.moveToFirst()) {
                    mCalendarId = c.getLong(CALENDAR_ID_COLUMN);
                } else {
                    long id = -1;
                    // Check if we have a calendar for this account with no server Id. If so, it was
                    // synced with an older version of the sync adapter before serverId's were
                    // supported.
                    final Cursor c1 = cr.query(Calendars.CONTENT_URI,
                            CALENDAR_ID_PROJECTION,
                            CALENDAR_SELECTION_ACCOUNT_AND_NO_SYNC,
                            new String[] {
                                    account.mEmailAddress,
                                    Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE,
                            }, null);
                    if (c1 != null) {
                        try {
                            if (c1.moveToFirst()) {
                                id = c1.getLong(CALENDAR_ID_COLUMN);
                                final ContentValues values = new ContentValues();
                                values.put(Calendars._SYNC_ID, mailbox.mServerId);
                                cr.update(
                                        ContentUris.withAppendedId(
                                                asSyncAdapter(Calendars.CONTENT_URI, account), id),
                                        values,
                                        null, /* where */
                                        null /* selectionArgs */);
                            }
                        } finally {
                            c1.close();
                        }
                    }

                    if (id >= 0) {
                        mCalendarId = id;
                    } else {
                        mCalendarId = CalendarUtilities.createCalendar(context, cr, account,
                            mailbox);
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    @Override
    public void setSyncOptions(final Context context, final Serializer s,
        final double protocolVersion, final Account account, final Mailbox mailbox,
        final boolean isInitialSync, final int numWindows) throws IOException {
        if (isInitialSync) {
            setInitialSyncOptions(s);
        } else {
            setNonInitialSyncOptions(s, numWindows, protocolVersion);
            setUpsyncCommands(context, account, protocolVersion, s);
        }
    }


    @Override
    public AbstractSyncParser getParser(final Context context, final Account account,
        final Mailbox mailbox, final InputStream is) throws IOException {
        return new CalendarSyncParser(context, context.getContentResolver(), is, mailbox, account,
            mAndroidAccount, mCalendarId);
    }

    @Override
    public int getTrafficFlag() {
        return TrafficFlags.DATA_CALENDAR;
    }

    /**
     * Adds params to a {@link Uri} to indicate that the caller is a sync adapter, and to add the
     * account info.
     * @param uri The {@link Uri} to which to add params.
     * @return The augmented {@link Uri}.
     */
    private static Uri asSyncAdapter(final Uri uri, final String emailAddress) {
        return uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(Calendars.ACCOUNT_NAME, emailAddress)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE)
                .build();
    }

    /**
     * Convenience wrapper to {@link #asSyncAdapter(android.net.Uri, String)}.
     */
    private Uri asSyncAdapter(final Uri uri, final Account account) {
        return asSyncAdapter(uri, account.mEmailAddress);
    }

    protected String getFolderClassName() {
        return "Calendar";
    }

    protected void setInitialSyncOptions(final Serializer s) throws IOException {
        // Nothing to do for Calendar.
    }

    protected void setNonInitialSyncOptions(final Serializer s, final int numWindows,
        final double protocolVersion) throws IOException {
        final int windowSize = numWindows * PIM_WINDOW_SIZE_CALENDAR;
        if (windowSize > MAX_WINDOW_SIZE  + PIM_WINDOW_SIZE_CALENDAR) {
            throw new IOException("Max window size reached and still no data");
        }
        setPimSyncOptions(s, Eas.FILTER_2_WEEKS, protocolVersion,
                windowSize < MAX_WINDOW_SIZE ? windowSize : MAX_WINDOW_SIZE);
    }

    /**
     * Find all dirty events for our calendar and mark their parents. Also delete any dirty events
     * that have no parents.
     * @param calendarIdString {@link #mCalendarId}, as a String.
     * @param calendarIdArgument calendarIdString, in a String array.
     */
    private void markParentsOfDirtyEvents(final Context context, final Account account,
            final String calendarIdString, final String[] calendarIdArgument) {
        final ContentResolver cr = context.getContentResolver();
        // We've got to handle exceptions as part of the parent when changes occur, so we need
        // to find new/changed exceptions and mark the parent dirty
        final ArrayList<Long> orphanedExceptions = new ArrayList<Long>();
        final Cursor c = cr.query(Events.CONTENT_URI,
                ORIGINAL_EVENT_PROJECTION, DIRTY_EXCEPTION_IN_CALENDAR, calendarIdArgument, null);
        if (c != null) {
            try {
                final ContentValues cv = new ContentValues(1);
                // We use _sync_mark here to distinguish dirty parents from parents with dirty
                // exceptions
                cv.put(EVENT_SYNC_MARK, "1");
                while (c.moveToNext()) {
                    // Mark the parents of dirty exceptions
                    final long parentId = c.getLong(ORIGINAL_EVENT_ORIGINAL_ID_COLUMN);
                    final int cnt = cr.update(asSyncAdapter(Events.CONTENT_URI, account), cv,
                            EVENT_ID_AND_CALENDAR_ID,
                            new String[] { Long.toString(parentId), calendarIdString });
                    // Keep track of any orphaned exceptions
                    if (cnt == 0) {
                        orphanedExceptions.add(c.getLong(ORIGINAL_EVENT_ID_COLUMN));
                    }
                }
            } finally {
                c.close();
            }
        }

        // Delete any orphaned exceptions
        for (final long orphan : orphanedExceptions) {
            LogUtils.d(TAG, "Deleted orphaned exception: %d", orphan);
            cr.delete(asSyncAdapter(
                    ContentUris.withAppendedId(Events.CONTENT_URI, orphan), account), null, null);
        }
    }

    /**
     * Get the version number of the current event, incrementing it if it's already there.
     * @param entityValues The {@link ContentValues} for this event.
     * @return The new version number for this event (i.e. 0 if it's a new event, or the old version
     *     number + 1).
     */
    private static String getEntityVersion(final ContentValues entityValues) {
        final String version = entityValues.getAsString(EVENT_SYNC_VERSION);
        // This should never be null, but catch this error anyway
        // Version should be "0" when we create the event, so use that
        if (version != null) {
            // Increment and save
            try {
                return Integer.toString((Integer.parseInt(version) + 1));
            } catch (final NumberFormatException e) {
                // Handle the case in which someone writes a non-integer here;
                // shouldn't happen, but we don't want to kill the sync for his
            }
        }
        return "0";
    }

    /**
     * Convenience method for sending an email to the organizer declining the meeting.
     * @param entity The {@link Entity} for this event.
     * @param clientId The client id for this event.
     */
    private void sendDeclinedEmail(final Context context, final Account account,
        final Entity entity, final String clientId) {
        final Message msg =
                CalendarUtilities.createMessageForEntity(context, entity,
                        Message.FLAG_OUTGOING_MEETING_DECLINE, clientId, account);
        if (msg != null) {
            LogUtils.d(TAG, "Queueing declined response to %s", msg.mTo);
            mOutgoingMailList.add(msg);
        }
    }

    /**
     * Get an integer value from a {@link ContentValues}, or 0 if the value isn't there.
     * @param cv The {@link ContentValues} to find the value in.
     * @param column The name of the column in cv to get.
     * @return The appropriate value as an integer, or 0 if it's not there.
     */
    private static int getInt(final ContentValues cv, final String column) {
        final Integer i = cv.getAsInteger(column);
        if (i == null) return 0;
        return i;
    }

    /**
     * Convert {@link Events} visibility values to EAS visibility values.
     * @param visibility The {@link Events} visibility value.
     * @return The corresponding EAS visibility value.
     */
    private static String decodeVisibility(final int visibility) {
        final int easVisibility;
        switch(visibility) {
            case Events.ACCESS_DEFAULT:
                easVisibility = 0;
                break;
            case Events.ACCESS_PUBLIC:
                easVisibility = 1;
                break;
            case Events.ACCESS_PRIVATE:
                easVisibility = 2;
                break;
            case Events.ACCESS_CONFIDENTIAL:
                easVisibility = 3;
                break;
            default:
                easVisibility = 0;
                break;
        }
        return Integer.toString(easVisibility);
    }

    /**
     * Write an event to the {@link Serializer} for this upsync.
     * @param entity The {@link Entity} for this event.
     * @param clientId The client id for this event.
     * @param s The {@link Serializer} for this Sync request.
     * @throws IOException
     * TODO: This can probably be refactored/cleaned up more.
     */
    private void sendEvent(final Context context, final Account account, final Entity entity,
        final String clientId, final double protocolVersion, final Serializer s)
            throws IOException {
        // Serialize for EAS here
        // Set uid with the client id we created
        // 1) Serialize the top-level event
        // 2) Serialize attendees and reminders from subvalues
        // 3) Look for exceptions and serialize with the top-level event
        final ContentResolver cr = context.getContentResolver();
        final ContentValues entityValues = entity.getEntityValues();
        final boolean isException = (clientId == null);
        boolean hasAttendees = false;
        final boolean isChange = entityValues.containsKey(Events._SYNC_ID);
        final boolean allDay =
                CalendarUtilities.getIntegerValueAsBoolean(entityValues, Events.ALL_DAY);
        final TimeZone localTimeZone = TimeZone.getDefault();

        // NOTE: Exchange 2003 (EAS 2.5) seems to require the "exception deleted" and "exception
        // start time" data before other data in exceptions.  Failure to do so results in a
        // status 6 error during sync
        if (isException) {
            // Send exception deleted flag if necessary
            final Integer deleted = entityValues.getAsInteger(Events.DELETED);
            final boolean isDeleted = deleted != null && deleted == 1;
            final Integer eventStatus = entityValues.getAsInteger(Events.STATUS);
            final boolean isCanceled =
                    eventStatus != null && eventStatus.equals(Events.STATUS_CANCELED);
            if (isDeleted || isCanceled) {
                s.data(Tags.CALENDAR_EXCEPTION_IS_DELETED, "1");
                // If we're deleted, the UI will continue to show this exception until we mark
                // it canceled, so we'll do that here...
                if (isDeleted && !isCanceled) {
                    final long eventId = entityValues.getAsLong(Events._ID);
                    final ContentValues cv = new ContentValues(1);
                    cv.put(Events.STATUS, Events.STATUS_CANCELED);
                    cr.update(asSyncAdapter(
                        ContentUris.withAppendedId(Events.CONTENT_URI, eventId), account),
                            cv, null, null);
                }
            } else {
                s.data(Tags.CALENDAR_EXCEPTION_IS_DELETED, "0");
            }

            // TODO Add reminders to exceptions (allow them to be specified!)
            Long originalTime = entityValues.getAsLong(Events.ORIGINAL_INSTANCE_TIME);
            if (originalTime != null) {
                final boolean originalAllDay =
                        CalendarUtilities.getIntegerValueAsBoolean(entityValues,
                                Events.ORIGINAL_ALL_DAY);
                if (originalAllDay) {
                    // For all day events, we need our local all-day time
                    originalTime =
                        CalendarUtilities.getLocalAllDayCalendarTime(originalTime, localTimeZone);
                }
                s.data(Tags.CALENDAR_EXCEPTION_START_TIME,
                        CalendarUtilities.millisToEasDateTime(originalTime));
            } else {
                // Illegal; what should we do?
            }
        }

        if (!isException) {
            // A time zone is required in all EAS events; we'll use the default if none is set
            // Exchange 2003 seems to require this first... :-)
            String timeZoneName = entityValues.getAsString(
                    allDay ? EVENT_SAVED_TIMEZONE_COLUMN : Events.EVENT_TIMEZONE);
            if (timeZoneName == null) {
                timeZoneName = localTimeZone.getID();
            }
            s.data(Tags.CALENDAR_TIME_ZONE,
                    CalendarUtilities.timeZoneToTziString(TimeZone.getTimeZone(timeZoneName)));
        }

        s.data(Tags.CALENDAR_ALL_DAY_EVENT, allDay ? "1" : "0");

        // DTSTART is always supplied
        long startTime = entityValues.getAsLong(Events.DTSTART);
        // Determine endTime; it's either provided as DTEND or we calculate using DURATION
        // If no DURATION is provided, we default to one hour
        long endTime;
        if (entityValues.containsKey(Events.DTEND)) {
            endTime = entityValues.getAsLong(Events.DTEND);
        } else {
            long durationMillis = DateUtils.HOUR_IN_MILLIS;
            if (entityValues.containsKey(Events.DURATION)) {
                final Duration duration = new Duration();
                try {
                    duration.parse(entityValues.getAsString(Events.DURATION));
                    durationMillis = duration.getMillis();
                } catch (DateException e) {
                    // Can't do much about this; use the default (1 hour)
                }
            }
            endTime = startTime + durationMillis;
        }
        if (allDay) {
            startTime = CalendarUtilities.getLocalAllDayCalendarTime(startTime, localTimeZone);
            endTime = CalendarUtilities.getLocalAllDayCalendarTime(endTime, localTimeZone);
        }
        s.data(Tags.CALENDAR_START_TIME, CalendarUtilities.millisToEasDateTime(startTime));
        s.data(Tags.CALENDAR_END_TIME, CalendarUtilities.millisToEasDateTime(endTime));

        s.data(Tags.CALENDAR_DTSTAMP,
                CalendarUtilities.millisToEasDateTime(System.currentTimeMillis()));

        String loc = entityValues.getAsString(Events.EVENT_LOCATION);
        if (!TextUtils.isEmpty(loc)) {
            if (protocolVersion < Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
                // EAS 2.5 doesn't like bare line feeds
                loc = Utility.replaceBareLfWithCrlf(loc);
            }
            s.data(Tags.CALENDAR_LOCATION, loc);
        }
        s.writeStringValue(entityValues, Events.TITLE, Tags.CALENDAR_SUBJECT);

        if (protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
            s.start(Tags.BASE_BODY);
            s.data(Tags.BASE_TYPE, "1");
            s.writeStringValue(entityValues, Events.DESCRIPTION, Tags.BASE_DATA);
            s.end();
        } else {
            // EAS 2.5 doesn't like bare line feeds
            s.writeStringValue(entityValues, Events.DESCRIPTION, Tags.CALENDAR_BODY);
        }

        if (!isException) {
            // For Exchange 2003, only upsync if the event is new
            if ((protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) || !isChange) {
                s.writeStringValue(entityValues, Events.ORGANIZER, Tags.CALENDAR_ORGANIZER_EMAIL);
            }

            final String rrule = entityValues.getAsString(Events.RRULE);
            if (rrule != null) {
                CalendarUtilities.recurrenceFromRrule(rrule, startTime, localTimeZone, s);
            }
        }
        // Handle associated data EXCEPT for attendees, which have to be grouped
        final ArrayList<Entity.NamedContentValues> subValues = entity.getSubValues();
        // The earliest of the reminders for this Event; we can only send one reminder...
        int earliestReminder = -1;
        for (final Entity.NamedContentValues ncv: subValues) {
            final Uri ncvUri = ncv.uri;
            final ContentValues ncvValues = ncv.values;
            if (ncvUri.equals(ExtendedProperties.CONTENT_URI)) {
                final String propertyName = ncvValues.getAsString(ExtendedProperties.NAME);
                final String propertyValue = ncvValues.getAsString(ExtendedProperties.VALUE);
                if (TextUtils.isEmpty(propertyValue)) {
                    continue;
                }
                if (propertyName.equals(EXTENDED_PROPERTY_CATEGORIES)) {
                    // Send all the categories back to the server
                    // We've saved them as a String of delimited tokens
                    final StringTokenizer st =
                            new StringTokenizer(propertyValue, CATEGORY_TOKENIZER_DELIMITER);
                    if (st.countTokens() > 0) {
                        s.start(Tags.CALENDAR_CATEGORIES);
                        while (st.hasMoreTokens()) {
                            s.data(Tags.CALENDAR_CATEGORY, st.nextToken());
                        }
                        s.end();
                    }
                }
            } else if (ncvUri.equals(Reminders.CONTENT_URI)) {
                Integer mins = ncvValues.getAsInteger(Reminders.MINUTES);
                if (mins != null) {
                    // -1 means "default", which for Exchange, is 30
                    if (mins < 0) {
                        mins = 30;
                    }
                    // Save this away if it's the earliest reminder (greatest minutes)
                    if (mins > earliestReminder) {
                        earliestReminder = mins;
                    }
                }
            }
        }

        // If we have a reminder, send it to the server
        if (earliestReminder >= 0) {
            s.data(Tags.CALENDAR_REMINDER_MINS_BEFORE, Integer.toString(earliestReminder));
        }

        // We've got to send a UID, unless this is an exception.  If the event is new, we've
        // generated one; if not, we should have gotten one from extended properties.
        if (clientId != null) {
            s.data(Tags.CALENDAR_UID, clientId);
        }

        // Handle attendee data here; keep track of organizer and stream it afterward
        String organizerName = null;
        String organizerEmail = null;
        for (final Entity.NamedContentValues ncv: subValues) {
            final Uri ncvUri = ncv.uri;
            final ContentValues ncvValues = ncv.values;
            if (ncvUri.equals(Attendees.CONTENT_URI)) {
                final Integer relationship =
                        ncvValues.getAsInteger(Attendees.ATTENDEE_RELATIONSHIP);
                final String attendeeEmail =
                        ncvValues.getAsString(Attendees.ATTENDEE_EMAIL);
                // If there's no relationship, we can't create this for EAS
                // Similarly, we need an attendee email for each invitee
                if (relationship != null && !TextUtils.isEmpty(attendeeEmail)) {
                    // Organizer isn't among attendees in EAS
                    if (relationship == Attendees.RELATIONSHIP_ORGANIZER) {
                        organizerName = ncvValues.getAsString(Attendees.ATTENDEE_NAME);
                        organizerEmail = attendeeEmail;
                        continue;
                    }
                    if (!hasAttendees) {
                        s.start(Tags.CALENDAR_ATTENDEES);
                        hasAttendees = true;
                    }
                    s.start(Tags.CALENDAR_ATTENDEE);
                    String attendeeName = ncvValues.getAsString(Attendees.ATTENDEE_NAME);
                    if (attendeeName == null) {
                        attendeeName = attendeeEmail;
                    }
                    s.data(Tags.CALENDAR_ATTENDEE_NAME, attendeeName);
                    s.data(Tags.CALENDAR_ATTENDEE_EMAIL, attendeeEmail);
                    if (protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
                        s.data(Tags.CALENDAR_ATTENDEE_TYPE, "1"); // Required
                    }
                    s.end(); // Attendee
                }
            }
        }
        if (hasAttendees) {
            s.end();  // Attendees
        }

        // Get busy status from availability
        final int availability = entityValues.getAsInteger(Events.AVAILABILITY);
        final int busyStatus = CalendarUtilities.busyStatusFromAvailability(availability);
        s.data(Tags.CALENDAR_BUSY_STATUS, Integer.toString(busyStatus));

        // Meeting status, 0 = appointment, 1 = meeting, 3 = attendee
        // In JB, organizer won't be an attendee
        if (organizerEmail == null && entityValues.containsKey(Events.ORGANIZER)) {
            organizerEmail = entityValues.getAsString(Events.ORGANIZER);
        }
        if (account.mEmailAddress.equalsIgnoreCase(organizerEmail)) {
            s.data(Tags.CALENDAR_MEETING_STATUS, hasAttendees ? "1" : "0");
        } else {
            s.data(Tags.CALENDAR_MEETING_STATUS, "3");
        }

        // For Exchange 2003, only upsync if the event is new
        if (((protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) || !isChange) &&
                organizerName != null) {
            s.data(Tags.CALENDAR_ORGANIZER_NAME, organizerName);
        }

        // NOTE: Sensitivity must NOT be sent to the server for exceptions in Exchange 2003
        // The result will be a status 6 failure during sync
        final Integer visibility = entityValues.getAsInteger(Events.ACCESS_LEVEL);
        if (visibility != null) {
            s.data(Tags.CALENDAR_SENSITIVITY, decodeVisibility(visibility));
        } else {
            // Default to private if not set
            s.data(Tags.CALENDAR_SENSITIVITY, "1");
        }
    }

    /**
     * Handle exceptions to an event's recurrance pattern.
     * @param s The {@link Serializer} for this upsync.
     * @param entity The {@link Entity} for this event.
     * @param entityValues The {@link ContentValues} for entity.
     * @param serverId The server side id for this event.
     * @param clientId The client side id for this event.
     * @param calendarIdString The calendar id, as a {@link String}.
     * @param selfOrganizer Whether the user is the organizer of this event.
     * @throws IOException
     */
    private void handleExceptionsToRecurrenceRules(final Serializer s, final Context context,
            final Account account,final Entity entity, final ContentValues entityValues,
            final String serverId, final String clientId, final String calendarIdString,
            final boolean selfOrganizer, final double protocolVersion) throws IOException {
        final ContentResolver cr = context.getContentResolver();
        final Cursor cursor = cr.query(asSyncAdapter(Events.CONTENT_URI, account), null,
                ORIGINAL_EVENT_AND_CALENDAR, new String[] { serverId, calendarIdString }, null);
        if (cursor == null) {
            return;
        }
        final EntityIterator exIterator = EventsEntity.newEntityIterator(cursor, cr);
        boolean exFirst = true;
        while (exIterator.hasNext()) {
            final Entity exEntity = exIterator.next();
            if (exFirst) {
                s.start(Tags.CALENDAR_EXCEPTIONS);
                exFirst = false;
            }
            s.start(Tags.CALENDAR_EXCEPTION);
            sendEvent(context, account, exEntity, null, protocolVersion, s);
            final ContentValues exValues = exEntity.getEntityValues();
            if (getInt(exValues, Events.DIRTY) == 1) {
                // This is a new/updated exception, so we've got to notify our
                // attendees about it
                final long exEventId = exValues.getAsLong(Events._ID);

                final int flag;
                if ((getInt(exValues, Events.DELETED) == 1) ||
                        (getInt(exValues, Events.STATUS) == Events.STATUS_CANCELED)) {
                    flag = Message.FLAG_OUTGOING_MEETING_CANCEL;
                    if (!selfOrganizer) {
                        // Send a cancellation notice to the organizer
                        // Since CalendarProvider2 sets the organizer of exceptions
                        // to the user, we have to reset it first to the original
                        // organizer
                        exValues.put(Events.ORGANIZER, entityValues.getAsString(Events.ORGANIZER));
                        sendDeclinedEmail(context, account, exEntity, clientId);
                    }
                } else {
                    flag = Message.FLAG_OUTGOING_MEETING_INVITE;
                }
                // Add the eventId of the exception to the uploaded id list, so that
                // the dirty/mark bits are cleared
                mUploadedIdList.add(exEventId);

                // Copy version so the ics attachment shows the proper sequence #
                exValues.put(EVENT_SYNC_VERSION,
                        entityValues.getAsString(EVENT_SYNC_VERSION));
                // Copy location so that it's included in the outgoing email
                if (entityValues.containsKey(Events.EVENT_LOCATION)) {
                    exValues.put(Events.EVENT_LOCATION,
                            entityValues.getAsString(Events.EVENT_LOCATION));
                }

                if (selfOrganizer) {
                    final Message msg = CalendarUtilities.createMessageForEntity(context, exEntity,
                            flag, clientId, account);
                    if (msg != null) {
                        LogUtils.d(TAG, "Queueing exception update to %s", msg.mTo);
                        mOutgoingMailList.add(msg);
                    }

                    // Also send out a cancellation email to removed attendees
                    final Entity removedEntity = new Entity(exValues);
                    final Set<String> exAttendeeEmails = Sets.newHashSet();
                    // Find all the attendees from the updated event
                    for (final Entity.NamedContentValues ncv: exEntity.getSubValues()) {
                        if (ncv.uri.equals(Attendees.CONTENT_URI)) {
                            exAttendeeEmails.add(ncv.values.getAsString(Attendees.ATTENDEE_EMAIL));
                        }
                    }
                    // Find the ones left out from the previous event and add them to the new entity
                    for (final Entity.NamedContentValues ncv: entity.getSubValues()) {
                        if (ncv.uri.equals(Attendees.CONTENT_URI)) {
                            final String attendeeEmail =
                                    ncv.values.getAsString(Attendees.ATTENDEE_EMAIL);
                            if (!exAttendeeEmails.contains(attendeeEmail)) {
                                removedEntity.addSubValue(ncv.uri, ncv.values);
                            }
                        }
                    }

                    // Now send a cancellation email
                    final Message removedMessage =
                            CalendarUtilities.createMessageForEntity(context, removedEntity,
                                    Message.FLAG_OUTGOING_MEETING_CANCEL, clientId, account);
                    if (removedMessage != null) {
                        LogUtils.d(TAG, "Queueing cancellation for removed attendees");
                        mOutgoingMailList.add(removedMessage);
                    }
                }
            }
            s.end(); // EXCEPTION
        }
        if (!exFirst) {
            s.end(); // EXCEPTIONS
        }
    }

    /**
     * Update the event properties with the attendee list, and send mail as appropriate.
     * @param entity The {@link Entity} for this event.
     * @param entityValues The {@link ContentValues} for entity.
     * @param selfOrganizer Whether the user is the organizer of this event.
     * @param eventId The id for this event.
     * @param clientId The client side id for this event.
     */
    private void updateAttendeesAndSendMail(final Context context, final Account account,
            final Entity entity, final ContentValues entityValues, final boolean selfOrganizer,
            final long eventId, final String clientId) {
        // Go through the extended properties of this Event and pull out our tokenized
        // attendees list and the user attendee status; we will need them later
        final ContentResolver cr = context.getContentResolver();
        String attendeeString = null;
        long attendeeStringId = -1;
        String userAttendeeStatus = null;
        long userAttendeeStatusId = -1;
        for (final Entity.NamedContentValues ncv: entity.getSubValues()) {
            if (ncv.uri.equals(ExtendedProperties.CONTENT_URI)) {
                final ContentValues ncvValues = ncv.values;
                final String propertyName = ncvValues.getAsString(ExtendedProperties.NAME);
                if (propertyName.equals(EXTENDED_PROPERTY_ATTENDEES)) {
                    attendeeString = ncvValues.getAsString(ExtendedProperties.VALUE);
                    attendeeStringId = ncvValues.getAsLong(ExtendedProperties._ID);
                } else if (propertyName.equals(EXTENDED_PROPERTY_USER_ATTENDEE_STATUS)) {
                    userAttendeeStatus = ncvValues.getAsString(ExtendedProperties.VALUE);
                    userAttendeeStatusId = ncvValues.getAsLong(ExtendedProperties._ID);
                }
            }
        }

        // Send the meeting invite if there are attendees and we're the organizer AND
        // if the Event itself is dirty (we might be syncing only because an exception
        // is dirty, in which case we DON'T send email about the Event)
        if (selfOrganizer && (getInt(entityValues, Events.DIRTY) == 1)) {
            final Message msg =
                CalendarUtilities.createMessageForEventId(context, eventId,
                        Message.FLAG_OUTGOING_MEETING_INVITE, clientId, account);
            if (msg != null) {
                LogUtils.d(TAG, "Queueing invitation to %s", msg.mTo);
                mOutgoingMailList.add(msg);
            }
            // Make a list out of our tokenized attendees, if we have any
            final ArrayList<String> originalAttendeeList = new ArrayList<String>();
            if (attendeeString != null) {
                final StringTokenizer st =
                    new StringTokenizer(attendeeString, ATTENDEE_TOKENIZER_DELIMITER);
                while (st.hasMoreTokens()) {
                    originalAttendeeList.add(st.nextToken());
                }
            }
            final StringBuilder newTokenizedAttendees = new StringBuilder();
            // See if any attendees have been dropped and while we're at it, build
            // an updated String with tokenized attendee addresses
            for (final Entity.NamedContentValues ncv: entity.getSubValues()) {
                if (ncv.uri.equals(Attendees.CONTENT_URI)) {
                    final String attendeeEmail = ncv.values.getAsString(Attendees.ATTENDEE_EMAIL);
                    // Remove all found attendees
                    originalAttendeeList.remove(attendeeEmail);
                    newTokenizedAttendees.append(attendeeEmail);
                    newTokenizedAttendees.append(ATTENDEE_TOKENIZER_DELIMITER);
                }
            }
            // Update extended properties with the new attendee list, if we have one
            // Otherwise, create one (this would be the case for Events created on
            // device or "legacy" events (before this code was added)
            final ContentValues cv = new ContentValues();
            cv.put(ExtendedProperties.VALUE, newTokenizedAttendees.toString());
            if (attendeeString != null) {
                cr.update(asSyncAdapter(ContentUris.withAppendedId(
                        ExtendedProperties.CONTENT_URI, attendeeStringId), account),
                        cv, null, null);
            } else {
                // If there wasn't an "attendees" property, insert one
                cv.put(ExtendedProperties.NAME, EXTENDED_PROPERTY_ATTENDEES);
                cv.put(ExtendedProperties.EVENT_ID, eventId);
                cr.insert(asSyncAdapter(ExtendedProperties.CONTENT_URI, account), cv);
            }
            // Whoever is left has been removed from the attendee list; send them
            // a cancellation
            for (final String removedAttendee: originalAttendeeList) {
                // Send a cancellation message to each of them
                final Message cancelMsg = CalendarUtilities.createMessageForEventId(context,
                        eventId, Message.FLAG_OUTGOING_MEETING_CANCEL, clientId, account,
                        removedAttendee);
                if (cancelMsg != null) {
                    // Just send it to the removed attendee
                    LogUtils.d(TAG, "Queueing cancellation to removed attendee %s", cancelMsg.mTo);
                    mOutgoingMailList.add(cancelMsg);
                }
            }
        } else if (!selfOrganizer) {
            // If we're not the organizer, see if we've changed our attendee status
            // Our last synced attendee status is in ExtendedProperties, and we've
            // retrieved it above as userAttendeeStatus
            final int currentStatus = entityValues.getAsInteger(Events.SELF_ATTENDEE_STATUS);
            int syncStatus = Attendees.ATTENDEE_STATUS_NONE;
            if (userAttendeeStatus != null) {
                try {
                    syncStatus = Integer.parseInt(userAttendeeStatus);
                } catch (NumberFormatException e) {
                    // Just in case somebody else mucked with this and it's not Integer
                }
            }
            if ((currentStatus != syncStatus) &&
                    (currentStatus != Attendees.ATTENDEE_STATUS_NONE)) {
                // If so, send a meeting reply
                final int messageFlag;
                switch (currentStatus) {
                    case Attendees.ATTENDEE_STATUS_ACCEPTED:
                        messageFlag = Message.FLAG_OUTGOING_MEETING_ACCEPT;
                        break;
                    case Attendees.ATTENDEE_STATUS_DECLINED:
                        messageFlag = Message.FLAG_OUTGOING_MEETING_DECLINE;
                        break;
                    case Attendees.ATTENDEE_STATUS_TENTATIVE:
                        messageFlag = Message.FLAG_OUTGOING_MEETING_TENTATIVE;
                        break;
                    default:
                        messageFlag = 0;
                        break;
                }
                // Make sure we have a valid status (messageFlag should never be zero)
                if (messageFlag != 0 && userAttendeeStatusId >= 0) {
                    // Save away the new status
                    final ContentValues cv = new ContentValues(1);
                    cv.put(ExtendedProperties.VALUE, Integer.toString(currentStatus));
                    cr.update(asSyncAdapter(ContentUris.withAppendedId(
                            ExtendedProperties.CONTENT_URI, userAttendeeStatusId), account),
                            cv, null, null);
                    // Send mail to the organizer advising of the new status
                    final Message msg = CalendarUtilities.createMessageForEventId(context, eventId,
                            messageFlag, clientId, account);
                    if (msg != null) {
                        LogUtils.d(TAG, "Queueing invitation reply to %s", msg.mTo);
                        mOutgoingMailList.add(msg);
                    }
                }
            }
        }
    }

    /**
     * Process a single event, adding to the {@link Serializer} as necessary.
     * @param s The {@link Serializer} for this Sync request.
     * @param entity The {@link Entity} for this event.
     * @param calendarIdString The calendar's id, as a {@link String}.
     * @param first Whether this would be the first event added to s.
     * @return Whether this function added anything to s.
     * @throws IOException
     */
    private boolean handleEntity(final Serializer s, final Context context, final Account account,
            final Entity entity, final String calendarIdString, final boolean first,
            final double protocolVersion) throws IOException {
        // For each of these entities, create the change commands
        final ContentResolver cr = context.getContentResolver();
        final ContentValues entityValues = entity.getEntityValues();
        // We first need to check whether we can upsync this event; our test for this
        // is currently the value of EXTENDED_PROPERTY_ATTENDEES_REDACTED
        // If this is set to "1", we can't upsync the event
        for (final Entity.NamedContentValues ncv: entity.getSubValues()) {
            if (ncv.uri.equals(ExtendedProperties.CONTENT_URI)) {
                final ContentValues ncvValues = ncv.values;
                if (ncvValues.getAsString(ExtendedProperties.NAME).equals(
                        EXTENDED_PROPERTY_UPSYNC_PROHIBITED)) {
                    if ("1".equals(ncvValues.getAsString(ExtendedProperties.VALUE))) {
                        // Make sure we mark this to clear the dirty flag
                        mUploadedIdList.add(entityValues.getAsLong(Events._ID));
                        return false;
                    }
                }
            }
        }

        // EAS 2.5 needs: BusyStatus DtStamp EndTime Sensitivity StartTime TimeZone UID
        // We can generate all but what we're testing for below
        final String organizerEmail = entityValues.getAsString(Events.ORGANIZER);
        if (organizerEmail == null || !entityValues.containsKey(Events.DTSTART) ||
                (!entityValues.containsKey(Events.DURATION)
                        && !entityValues.containsKey(Events.DTEND))) {
            return false;
        }

        if (first) {
            s.start(Tags.SYNC_COMMANDS);
            LogUtils.d(TAG, "Sending Calendar changes to the server");
        }

        final boolean selfOrganizer = organizerEmail.equalsIgnoreCase(account.mEmailAddress);
        // Find our uid in the entity; otherwise create one
        String clientId = entityValues.getAsString(Events.SYNC_DATA2);
        if (clientId == null) {
            clientId = UUID.randomUUID().toString();
        }
        final String serverId = entityValues.getAsString(Events._SYNC_ID);
        final long eventId = entityValues.getAsLong(Events._ID);
        if (serverId == null) {
            // This is a new event; create a clientId
            LogUtils.d(TAG, "Creating new event with clientId: %s", clientId);
            s.start(Tags.SYNC_ADD).data(Tags.SYNC_CLIENT_ID, clientId);
            // And save it in the Event as the local id
            final ContentValues cv = new ContentValues(2);
            cv.put(Events.SYNC_DATA2, clientId);
            cv.put(EVENT_SYNC_VERSION, "0");
            cr.update(
                    asSyncAdapter(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), account),
                    cv, null, null);
        } else if (entityValues.getAsInteger(Events.DELETED) == 1) {
            LogUtils.d(TAG, "Deleting event with serverId: %s", serverId);
            s.start(Tags.SYNC_DELETE).data(Tags.SYNC_SERVER_ID, serverId).end();
            mDeletedIdList.add(eventId);
            if (selfOrganizer) {
                final Message msg = CalendarUtilities.createMessageForEventId(context,
                        eventId, Message.FLAG_OUTGOING_MEETING_CANCEL, null, account);
                if (msg != null) {
                    LogUtils.d(TAG, "Queueing cancellation to %s", msg.mTo);
                    mOutgoingMailList.add(msg);
                }
            } else {
                sendDeclinedEmail(context, account, entity, clientId);
            }
            // For deletions, we don't need to add application data, so just bail here.
            return true;
        } else {
            LogUtils.d(TAG, "Upsync change to event with serverId: %s", serverId);
            s.start(Tags.SYNC_CHANGE).data(Tags.SYNC_SERVER_ID, serverId);
            // Save to the ContentResolver.
            final String version = getEntityVersion(entityValues);
            final ContentValues cv = new ContentValues(1);
            cv.put(EVENT_SYNC_VERSION, version);
            cr.update( asSyncAdapter(ContentUris.withAppendedId(Events.CONTENT_URI, eventId),
                    account), cv, null, null);
            // Also save in entityValues so that we send it this time around
            entityValues.put(EVENT_SYNC_VERSION, version);
        }
        s.start(Tags.SYNC_APPLICATION_DATA);
        sendEvent(context, account, entity, clientId, protocolVersion, s);

        // Now, the hard part; find exceptions for this event
        if (serverId != null) {
            handleExceptionsToRecurrenceRules(s, context, account, entity, entityValues, serverId,
                    clientId, calendarIdString, selfOrganizer, protocolVersion);
        }

        s.end().end();  // ApplicationData & Add/Change
        mUploadedIdList.add(eventId);
        updateAttendeesAndSendMail(context, account, entity, entityValues, selfOrganizer, eventId,
            clientId);
        return true;
    }

    protected void setUpsyncCommands(Context context, final Account account,
            final double protocolVersion, final Serializer s) throws IOException {
        final ContentResolver cr = context.getContentResolver();
        final String calendarIdString = Long.toString(mCalendarId);
        final String[] calendarIdArgument = { calendarIdString };

        markParentsOfDirtyEvents(context, account, calendarIdString, calendarIdArgument);

        // Now go through dirty/marked top-level events and send them back to the server
        final Cursor cursor = cr.query(asSyncAdapter(Events.CONTENT_URI, account), null,
                DIRTY_OR_MARKED_TOP_LEVEL_IN_CALENDAR, calendarIdArgument, null);
        if (cursor == null) {
            return;
        }
        final EntityIterator eventIterator = EventsEntity.newEntityIterator(cursor, cr);

        try {
            boolean first = true;
            while (eventIterator.hasNext()) {
                final boolean addedCommand =
                        handleEntity(s, context, account, eventIterator.next(), calendarIdString,
                            first, protocolVersion);
                if (addedCommand) {
                    first = false;
                }
            }
            if (!first) {
                s.end();  // Commands
            }
        } finally {
            eventIterator.close();
        }
    }

    @Override
    public void cleanup(final Context context, final Account account) {
        final ContentResolver cr = context.getContentResolver();
        // Clear dirty and mark flags for updates sent to server
        if (!mUploadedIdList.isEmpty()) {
            final ContentValues cv = new ContentValues(2);
            cv.put(Events.DIRTY, 0);
            cv.put(EVENT_SYNC_MARK, "0");
            for (final long eventId : mUploadedIdList) {
                cr.update(asSyncAdapter(ContentUris.withAppendedId(
                        Events.CONTENT_URI, eventId), account), cv, null, null);
            }
        }
        // Delete events marked for deletion
        if (!mDeletedIdList.isEmpty()) {
            for (final long eventId : mDeletedIdList) {
                cr.delete(asSyncAdapter(ContentUris.withAppendedId(
                        Events.CONTENT_URI, eventId), account), null, null);
            }
        }
        // Send all messages that were created during this sync.
        for (final Message msg : mOutgoingMailList) {
            sendMessage(context, account, msg);
        }

        mDeletedIdList.clear();
        mUploadedIdList.clear();
        mOutgoingMailList.clear();
    }

    /**
     * Convenience method for adding a Message to an account's outbox
     * @param account The {@link Account} from which to send the message.
     * @param msg The message to send
     */
    protected void sendMessage(final Context context, final Account account,
        final EmailContent.Message msg) {
        long mailboxId = Mailbox.findMailboxOfType(context, account.mId, Mailbox.TYPE_OUTBOX);
        // TODO: Improve system mailbox handling.
        if (mailboxId == Mailbox.NO_MAILBOX) {
            LogUtils.d(TAG, "No outbox for account %d, creating it", account.mId);
            final Mailbox outbox =
                    Mailbox.newSystemMailbox(context, account.mId, Mailbox.TYPE_OUTBOX);
            outbox.save(context);
            mailboxId = outbox.mId;
        }
        msg.mMailboxKey = mailboxId;
        msg.mAccountKey = account.mId;
        msg.save(context);
        requestSyncForMailbox(EmailContent.AUTHORITY, mailboxId);
    }

    /**
     * Issue a {@link android.content.ContentResolver#requestSync} for a specific mailbox.
     * @param authority The authority for the mailbox that needs to sync.
     * @param mailboxId The id of the mailbox that needs to sync.
     */
    protected void requestSyncForMailbox(final String authority, final long mailboxId) {
        final Bundle extras = Mailbox.createSyncBundle(mailboxId);
        ContentResolver.requestSync(mAndroidAccount, authority, extras);
        LogUtils.d(TAG, "requestSync EasServerConnection requestSyncForMailbox %s, %s",
                mAndroidAccount.toString(), extras.toString());
    }


    /**
     * Delete an account from the Calendar provider.
     * @param context Our {@link Context}
     * @param emailAddress The email address of the account we wish to delete
     */
    public static void wipeAccountFromContentProvider(final Context context,
            final String emailAddress) {
        try {
            context.getContentResolver().delete(asSyncAdapter(Calendars.CONTENT_URI, emailAddress),
                    Calendars.ACCOUNT_NAME + "=" + DatabaseUtils.sqlEscapeString(emailAddress)
                    + " AND " + Calendars.ACCOUNT_TYPE + "="+ DatabaseUtils.sqlEscapeString(
                            context.getString(R.string.account_manager_type_exchange)), null);
        } catch (IllegalArgumentException e) {
            LogUtils.e(TAG, "CalendarProvider disabled; unable to wipe account.");
        }
    }
}
