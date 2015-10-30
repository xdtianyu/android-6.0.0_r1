/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.telecom;

import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages logging for the entire module.
 */
@VisibleForTesting
public class Log {

    /**
     * Stores the various events associated with {@link Call}s. Also stores all request-response
     * pairs amongst the events.
     */
    public final static class Events {
        public static final String CREATED = "CREATED";
        public static final String DESTROYED = "DESTROYED";
        public static final String SET_NEW = "SET_NEW";
        public static final String SET_CONNECTING = "SET_CONNECTING";
        public static final String SET_DIALING = "SET_DIALING";
        public static final String SET_ACTIVE = "SET_ACTIVE";
        public static final String SET_HOLD = "SET_HOLD";
        public static final String SET_RINGING = "SET_RINGING";
        public static final String SET_DISCONNECTED = "SET_DISCONNECTED";
        public static final String SET_DISCONNECTING = "SET_DISCONNECTING";
        public static final String SET_SELECT_PHONE_ACCOUNT = "SET_SELECT_PHONE_ACCOUNT";
        public static final String REQUEST_HOLD = "REQUEST_HOLD";
        public static final String REQUEST_UNHOLD = "REQUEST_UNHOLD";
        public static final String REQUEST_DISCONNECT = "REQUEST_DISCONNECT";
        public static final String REQUEST_ACCEPT = "REQUEST_ACCEPT";
        public static final String REQUEST_REJECT = "REQUEST_REJECT";
        public static final String START_DTMF = "START_DTMF";
        public static final String STOP_DTMF = "STOP_DTMF";
        public static final String START_RINGER = "START_RINGER";
        public static final String STOP_RINGER = "STOP_RINGER";
        public static final String START_CALL_WAITING_TONE = "START_CALL_WAITING_TONE";
        public static final String STOP_CALL_WAITING_TONE = "STOP_CALL_WAITING_TONE";
        public static final String START_CONNECTION = "START_CONNECTION";
        public static final String BIND_CS = "BIND_CS";
        public static final String CS_BOUND = "CS_BOUND";
        public static final String CONFERENCE_WITH = "CONF_WITH";
        public static final String SPLIT_CONFERENCE = "CONF_SPLIT";
        public static final String SWAP = "SWAP";
        public static final String ADD_CHILD = "ADD_CHILD";
        public static final String REMOVE_CHILD = "REMOVE_CHILD";
        public static final String SET_PARENT = "SET_PARENT";
        public static final String MUTE = "MUTE";
        public static final String AUDIO_ROUTE = "AUDIO_ROUTE";
        public static final String ERROR_LOG = "ERROR";

        /**
         * Maps from a request to a response.  The same event could be listed as the
         * response for multiple requests (e.g. REQUEST_ACCEPT and REQUEST_UNHOLD both map to the
         * SET_ACTIVE response). This map is used to print out the amount of time it takes between
         * a request and a response.
         */
        public static final Map<String, String> requestResponsePairs =
                new HashMap<String, String>() {{
                    put(REQUEST_ACCEPT, SET_ACTIVE);
                    put(REQUEST_REJECT, SET_DISCONNECTED);
                    put(REQUEST_DISCONNECT, SET_DISCONNECTED);
                    put(REQUEST_HOLD, SET_HOLD);
                    put(REQUEST_UNHOLD, SET_ACTIVE);
                    put(START_CONNECTION, SET_DIALING);
                    put(BIND_CS, CS_BOUND);
                }};
    }

    public static class CallEvent {
        public String eventId;
        public long time;
        public Object data;

        public CallEvent(String eventId, long time, Object data) {
            this.eventId = eventId;
            this.time = time;
            this.data = data;
        }
    }

    public static class CallEventRecord {
        private static final DateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        private static int sNextId = 1;
        private final List<CallEvent> mEvents = new LinkedList<>();
        private final Call mCall;
        private final int mId;

        public CallEventRecord(Call call) {
            mCall = call;
            mId = ++sNextId;
        }

        public Call getCall() {
            return mCall;
        }

        public void addEvent(String event, Object data) {
            mEvents.add(new CallEvent(event, System.currentTimeMillis(), data));
            Log.i("Event", "Call %d: %s, %s", mId, event, data);
        }

        public void dump(IndentingPrintWriter pw) {
            Map<String, CallEvent> pendingResponses = new HashMap<>();

            pw.print("Call ");
            pw.print(mId);
            pw.print(" [");
            pw.print(sDateFormat.format(new Date(mCall.getCreationTimeMillis())));
            pw.print("]");
            pw.println(mCall.isIncoming() ? "(MT - incoming)" : "(MO - outgoing)");

            pw.increaseIndent();
            pw.println("To address: " + piiHandle(mCall.getHandle()));

            for (CallEvent event : mEvents) {

                // We print out events in chronological order. During that process we look at each
                // event and see if it maps to a request on the Request-Response pairs map. If it
                // does, then we effectively start 'listening' for the response. We do that by
                // storing the response event ID in {@code pendingResponses}. When we find the
                // response in a later iteration of the loop, we grab the original request and
                // calculate the time it took to get a response.
                if (Events.requestResponsePairs.containsKey(event.eventId)) {
                    // This event expects a response, so add that response to the maps
                    // of pending events.
                    String pendingResponse = Events.requestResponsePairs.get(event.eventId);
                    pendingResponses.put(pendingResponse, event);
                }

                pw.print(sDateFormat.format(new Date(event.time)));
                pw.print(" - ");
                pw.print(event.eventId);
                if (event.data != null) {
                    pw.print(" (");
                    Object data = event.data;

                    if (data instanceof Call) {
                        // If the data is another call, then change the data to the call's CallEvent
                        // ID instead.
                        CallEventRecord record = mCallEventRecordMap.get(data);
                        if (record != null) {
                            data = "Call " + record.mId;
                        }
                    }

                    pw.print(data);
                    pw.print(")");
                }

                // If this event is a response event that we've been waiting for, calculate the time
                // it took for the response to complete and print that out as well.
                CallEvent requestEvent = pendingResponses.remove(event.eventId);
                if (requestEvent != null) {
                    pw.print(", time since ");
                    pw.print(requestEvent.eventId);
                    pw.print(": ");
                    pw.print(event.time - requestEvent.time);
                    pw.print(" ms");
                }
                pw.println();
            }
            pw.decreaseIndent();
        }
    }

    public static final int MAX_CALLS_TO_CACHE = 5;  // Arbitrarily chosen.

    // Generic tag for all In Call logging
    @VisibleForTesting
    public static String TAG = "Telecom";

    public static final boolean FORCE_LOGGING = false; /* STOP SHIP if true */
    public static final boolean SYSTRACE_DEBUG = false; /* STOP SHIP if true */
    public static final boolean DEBUG = isLoggable(android.util.Log.DEBUG);
    public static final boolean INFO = isLoggable(android.util.Log.INFO);
    public static final boolean VERBOSE = isLoggable(android.util.Log.VERBOSE);
    public static final boolean WARN = isLoggable(android.util.Log.WARN);
    public static final boolean ERROR = isLoggable(android.util.Log.ERROR);

    private static final Map<Call, CallEventRecord> mCallEventRecordMap = new HashMap<>();
    private static final LinkedBlockingQueue<CallEventRecord> mCallEventRecords =
            new LinkedBlockingQueue<CallEventRecord>(MAX_CALLS_TO_CACHE);

    private Log() {}

    @VisibleForTesting
    public static void setTag(String tag) {
        TAG = tag;
    }

    public static void event(Call call, String event) {
        event(call, event, null);
    }

    public static void event(Call call, String event, Object data) {
        if (call == null) {
            Log.i(TAG, "Non-call EVENT: %s, %s", event, data);
            return;
        }
        synchronized (mCallEventRecords) {
            if (!mCallEventRecordMap.containsKey(call)) {
                // First remove the oldest entry if no new ones exist.
                if (mCallEventRecords.remainingCapacity() == 0) {
                    CallEventRecord record = mCallEventRecords.poll();
                    if (record != null) {
                        mCallEventRecordMap.remove(record.getCall());
                    }
                }

                // Now add a new entry
                CallEventRecord newRecord = new CallEventRecord(call);
                mCallEventRecords.add(newRecord);
                mCallEventRecordMap.put(call, newRecord);
            }

            CallEventRecord record = mCallEventRecordMap.get(call);
            record.addEvent(event, data);
        }
    }

    public static boolean isLoggable(int level) {
        return FORCE_LOGGING || android.util.Log.isLoggable(TAG, level);
    }

    public static void d(String prefix, String format, Object... args) {
        if (DEBUG) {
            android.util.Slog.d(TAG, buildMessage(prefix, format, args));
        }
    }

    public static void d(Object objectPrefix, String format, Object... args) {
        if (DEBUG) {
            android.util.Slog.d(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void i(String prefix, String format, Object... args) {
        if (INFO) {
            android.util.Slog.i(TAG, buildMessage(prefix, format, args));
        }
    }

    public static void i(Object objectPrefix, String format, Object... args) {
        if (INFO) {
            android.util.Slog.i(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void v(String prefix, String format, Object... args) {
        if (VERBOSE) {
            android.util.Slog.v(TAG, buildMessage(prefix, format, args));
        }
    }

    public static void v(Object objectPrefix, String format, Object... args) {
        if (VERBOSE) {
            android.util.Slog.v(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void w(String prefix, String format, Object... args) {
        if (WARN) {
            android.util.Slog.w(TAG, buildMessage(prefix, format, args));
        }
    }

    public static void w(Object objectPrefix, String format, Object... args) {
        if (WARN) {
            android.util.Slog.w(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args));
        }
    }

    public static void e(String prefix, Throwable tr, String format, Object... args) {
        if (ERROR) {
            android.util.Slog.e(TAG, buildMessage(prefix, format, args), tr);
        }
    }

    public static void e(Object objectPrefix, Throwable tr, String format, Object... args) {
        if (ERROR) {
            android.util.Slog.e(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args),
                    tr);
        }
    }

    public static void wtf(String prefix, Throwable tr, String format, Object... args) {
        android.util.Slog.wtf(TAG, buildMessage(prefix, format, args), tr);
    }

    public static void wtf(Object objectPrefix, Throwable tr, String format, Object... args) {
        android.util.Slog.wtf(TAG, buildMessage(getPrefixFromObject(objectPrefix), format, args),
                tr);
    }

    public static void wtf(String prefix, String format, Object... args) {
        String msg = buildMessage(prefix, format, args);
        android.util.Slog.wtf(TAG, msg, new IllegalStateException(msg));
    }

    public static void wtf(Object objectPrefix, String format, Object... args) {
        String msg = buildMessage(getPrefixFromObject(objectPrefix), format, args);
        android.util.Slog.wtf(TAG, msg, new IllegalStateException(msg));
    }

    public static String piiHandle(Object pii) {
        if (pii == null || VERBOSE) {
            return String.valueOf(pii);
        }

        StringBuilder sb = new StringBuilder();
        if (pii instanceof Uri) {
            Uri uri = (Uri) pii;
            String scheme = uri.getScheme();

            if (!TextUtils.isEmpty(scheme)) {
                sb.append(scheme).append(":");
            }

            String textToObfuscate = uri.getSchemeSpecificPart();
            if (PhoneAccount.SCHEME_TEL.equals(scheme)) {
                for (int i = 0; i < textToObfuscate.length(); i++) {
                    char c = textToObfuscate.charAt(i);
                    sb.append(PhoneNumberUtils.isDialable(c) ? "*" : c);
                }
            } else if (PhoneAccount.SCHEME_SIP.equals(scheme)) {
                for (int i = 0; i < textToObfuscate.length(); i++) {
                    char c = textToObfuscate.charAt(i);
                    if (c != '@' && c != '.') {
                        c = '*';
                    }
                    sb.append(c);
                }
            } else {
                sb.append(pii(pii));
            }
        }

        return sb.toString();
    }

    /**
     * Redact personally identifiable information for production users.
     * If we are running in verbose mode, return the original string, otherwise
     * return a SHA-1 hash of the input string.
     */
    public static String pii(Object pii) {
        if (pii == null || VERBOSE) {
            return String.valueOf(pii);
        }
        return "[" + secureHash(String.valueOf(pii).getBytes()) + "]";
    }

    public static void dumpCallEvents(IndentingPrintWriter pw) {
        pw.println("Historical Calls:");
        pw.increaseIndent();
        for (CallEventRecord callEventRecord : mCallEventRecords) {
            callEventRecord.dump(pw);
        }
        pw.decreaseIndent();
    }

    private static String secureHash(byte[] input) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        messageDigest.update(input);
        byte[] result = messageDigest.digest();
        return encodeHex(result);
    }

    private static String encodeHex(byte[] bytes) {
        StringBuffer hex = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            int byteIntValue = bytes[i] & 0xff;
            if (byteIntValue < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toString(byteIntValue, 16));
        }

        return hex.toString();
    }

    private static String getPrefixFromObject(Object obj) {
        return obj == null ? "<null>" : obj.getClass().getSimpleName();
    }

    private static String buildMessage(String prefix, String format, Object... args) {
        String msg;
        try {
            msg = (args == null || args.length == 0) ? format
                    : String.format(Locale.US, format, args);
        } catch (IllegalFormatException ife) {
            e("Log", ife, "IllegalFormatException: formatString='%s' numArgs=%d", format,
                    args.length);
            msg = format + " (An error occurred while formatting the message.)";
        }
        return String.format(Locale.US, "%s: %s", prefix, msg);
    }
}
