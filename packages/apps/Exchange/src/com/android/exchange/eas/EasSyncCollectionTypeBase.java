package com.android.exchange.eas;

import android.content.Context;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.Eas;
import com.android.exchange.adapter.AbstractSyncParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract base class that handles the details of syncing a specific collection type.
 * These details include:
 * - Forming the request options. Contacts, Calendar, and Mail set this up differently.
 * - Getting the appropriate parser for this collection type.
 */
public abstract class EasSyncCollectionTypeBase {

    public static final int MAX_WINDOW_SIZE = 512;

    /**
     * Get the flag for traffic bookkeeping for this sync type.
     * @return The appropriate value from {@link com.android.emailcommon.TrafficFlags} for this
     *         sync.
     */
    public abstract int getTrafficFlag();

    /**
     * Write the contents of a Collection node in an EAS sync request appropriate for our mailbox.
     * See http://msdn.microsoft.com/en-us/library/gg650891(v=exchg.80).aspx for documentation on
     * the contents of this sync request element.
     * @param context
     * @param s The {@link Serializer} for the current request. This should be within a
     *          {@link com.android.exchange.adapter.Tags#SYNC_COLLECTION} element.
     * @param protocolVersion
     * @param account
     * @param mailbox
     * @param isInitialSync
     * @param numWindows
     * @throws IOException
     */
    public abstract void setSyncOptions(final Context context, final Serializer s,
            final double protocolVersion, final Account account, final Mailbox mailbox,
            final boolean isInitialSync, final int numWindows) throws IOException;

    /**
     * Create a parser for the current response data, appropriate for this collection type.
     * @param context
     * @param account
     * @param mailbox
     * @param is The {@link InputStream} for the server response we're processing.
     * @return An appropriate parser for this input.
     * @throws IOException
     */
    public abstract AbstractSyncParser getParser(final Context context, final Account account,
            final Mailbox mailbox, final InputStream is) throws IOException;

    /**
     * After every successful sync iteration, this function gets called to cleanup any state to
     * match the sync result (e.g., to clean up an external ContentProvider for PIM data).
     * @param context
     * @param account
     */
    public void cleanup(final Context context, final Account account) {}

    /**
     * Shared non-initial sync options for PIM (contacts & calendar) objects.
     *
     * @param s The {@link com.android.exchange.adapter.Serializer} for this sync request.
     * @param filter The lookback to use, or null if no lookback is desired.
     * @param protocolVersion The EAS protocol version for this request, as a double.
     * @param windowSize
     * @throws IOException
     */
    protected static void setPimSyncOptions(final Serializer s, final String filter,
            final double protocolVersion, int windowSize) throws IOException {
        s.tag(Tags.SYNC_DELETES_AS_MOVES);
        s.tag(Tags.SYNC_GET_CHANGES);
        s.data(Tags.SYNC_WINDOW_SIZE, String.valueOf(windowSize));
        s.start(Tags.SYNC_OPTIONS);
        // Set the filter (lookback), if provided
        if (filter != null) {
            s.data(Tags.SYNC_FILTER_TYPE, filter);
        }
        // Set the truncation amount and body type
        if (protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
            s.start(Tags.BASE_BODY_PREFERENCE);
            // Plain text
            s.data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_TEXT);
            s.data(Tags.BASE_TRUNCATION_SIZE, Eas.EAS12_TRUNCATION_SIZE);
            s.end();
        } else {
            s.data(Tags.SYNC_TRUNCATION, Eas.EAS2_5_TRUNCATION_SIZE);
        }
        s.end();
    }
}
