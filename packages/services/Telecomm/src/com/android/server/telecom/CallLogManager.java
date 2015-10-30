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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog.Calls;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.telephony.PhoneNumberUtils;

// TODO: Needed for move to system service: import com.android.internal.R;
import com.android.internal.telephony.CallerInfo;

/**
 * Helper class that provides functionality to write information about calls and their associated
 * caller details to the call log. All logging activity will be performed asynchronously in a
 * background thread to avoid blocking on the main thread.
 */
final class CallLogManager extends CallsManagerListenerBase {
    /**
     * Parameter object to hold the arguments to add a call in the call log DB.
     */
    private static class AddCallArgs {
        /**
         * @param callerInfo Caller details.
         * @param number The phone number to be logged.
         * @param presentation Number presentation of the phone number to be logged.
         * @param callType The type of call (e.g INCOMING_TYPE). @see
         *     {@link android.provider.CallLog} for the list of values.
         * @param features The features of the call (e.g. FEATURES_VIDEO). @see
         *     {@link android.provider.CallLog} for the list of values.
         * @param creationDate Time when the call was created (milliseconds since epoch).
         * @param durationInMillis Duration of the call (milliseconds).
         * @param dataUsage Data usage in bytes, or null if not applicable.
         */
        public AddCallArgs(Context context, CallerInfo callerInfo, String number,
                int presentation, int callType, int features, PhoneAccountHandle accountHandle,
                long creationDate, long durationInMillis, Long dataUsage) {
            this.context = context;
            this.callerInfo = callerInfo;
            this.number = number;
            this.presentation = presentation;
            this.callType = callType;
            this.features = features;
            this.accountHandle = accountHandle;
            this.timestamp = creationDate;
            this.durationInSec = (int)(durationInMillis / 1000);
            this.dataUsage = dataUsage;
        }
        // Since the members are accessed directly, we don't use the
        // mXxxx notation.
        public final Context context;
        public final CallerInfo callerInfo;
        public final String number;
        public final int presentation;
        public final int callType;
        public final int features;
        public final PhoneAccountHandle accountHandle;
        public final long timestamp;
        public final int durationInSec;
        public final Long dataUsage;
    }

    private static final String TAG = CallLogManager.class.getSimpleName();

    private final Context mContext;
    private static final String ACTION_CALLS_TABLE_ADD_ENTRY =
                "com.android.server.telecom.intent.action.CALLS_ADD_ENTRY";
    private static final String PERMISSION_PROCESS_CALLLOG_INFO =
                "android.permission.PROCESS_CALLLOG_INFO";
    private static final String CALL_TYPE = "callType";
    private static final String CALL_DURATION = "duration";

    public CallLogManager(Context context) {
        mContext = context;
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        int disconnectCause = call.getDisconnectCause().getCode();
        boolean isNewlyDisconnected =
                newState == CallState.DISCONNECTED || newState == CallState.ABORTED;
        boolean isCallCanceled = isNewlyDisconnected && disconnectCause == DisconnectCause.CANCELED;

        // Log newly disconnected calls only if:
        // 1) It was not in the "choose account" phase when disconnected
        // 2) It is a conference call
        // 3) Call was not explicitly canceled
        if (isNewlyDisconnected &&
                (oldState != CallState.SELECT_PHONE_ACCOUNT &&
                 !call.isConference() &&
                 !isCallCanceled)) {
            int type;
            if (!call.isIncoming()) {
                type = Calls.OUTGOING_TYPE;
            } else if (disconnectCause == DisconnectCause.MISSED) {
                type = Calls.MISSED_TYPE;
            } else {
                type = Calls.INCOMING_TYPE;
            }
            logCall(call, type);
        }
    }

    /**
     * Logs a call to the call log based on the {@link Call} object passed in.
     *
     * @param call The call object being logged
     * @param callLogType The type of call log entry to log this call as. See:
     *     {@link android.provider.CallLog.Calls#INCOMING_TYPE}
     *     {@link android.provider.CallLog.Calls#OUTGOING_TYPE}
     *     {@link android.provider.CallLog.Calls#MISSED_TYPE}
     */
    void logCall(Call call, int callLogType) {
        final long creationTime = call.getCreationTimeMillis();
        final long age = call.getAgeMillis();

        final String logNumber = getLogNumber(call);

        Log.d(TAG, "logNumber set to: %s", Log.pii(logNumber));

        final PhoneAccountHandle emergencyAccountHandle =
                TelephonyUtil.getDefaultEmergencyPhoneAccount().getAccountHandle();

        PhoneAccountHandle accountHandle = call.getTargetPhoneAccount();
        if (emergencyAccountHandle.equals(accountHandle)) {
            accountHandle = null;
        }

        // TODO(vt): Once data usage is available, wire it up here.
        int callFeatures = getCallFeatures(call.getVideoStateHistory());
        logCall(call.getCallerInfo(), logNumber, call.getHandlePresentation(),
                callLogType, callFeatures, accountHandle, creationTime, age, null);
    }

    /**
     * Inserts a call into the call log, based on the parameters passed in.
     *
     * @param callerInfo Caller details.
     * @param number The number the call was made to or from.
     * @param presentation
     * @param callType The type of call.
     * @param features The features of the call.
     * @param start The start time of the call, in milliseconds.
     * @param duration The duration of the call, in milliseconds.
     * @param dataUsage The data usage for the call, null if not applicable.
     */
    private void logCall(
            CallerInfo callerInfo,
            String number,
            int presentation,
            int callType,
            int features,
            PhoneAccountHandle accountHandle,
            long start,
            long duration,
            Long dataUsage) {
        boolean isEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(mContext, number);

        // On some devices, to avoid accidental redialing of emergency numbers, we *never* log
        // emergency calls to the Call Log.  (This behavior is set on a per-product basis, based
        // on carrier requirements.)
        final boolean okToLogEmergencyNumber =
                mContext.getResources().getBoolean(R.bool.allow_emergency_numbers_in_call_log);

        // Don't log emergency numbers if the device doesn't allow it.
        final boolean isOkToLogThisCall = !isEmergencyNumber || okToLogEmergencyNumber;

        sendAddCallBroadcast(callType, duration);

        if (isOkToLogThisCall) {
            Log.d(TAG, "Logging Calllog entry: " + callerInfo + ", "
                    + Log.pii(number) + "," + presentation + ", " + callType
                    + ", " + start + ", " + duration);
            AddCallArgs args = new AddCallArgs(mContext, callerInfo, number, presentation,
                    callType, features, accountHandle, start, duration, dataUsage);
            logCallAsync(args);
        } else {
          Log.d(TAG, "Not adding emergency call to call log.");
        }
    }

    /**
     * Based on the video state of the call, determines the call features applicable for the call.
     *
     * @param videoState The video state.
     * @return The call features.
     */
    private static int getCallFeatures(int videoState) {
        if (VideoProfile.isVideo(videoState)) {
            return Calls.FEATURES_VIDEO;
        }
        return 0;
    }

    /**
     * Retrieve the phone number from the call, and then process it before returning the
     * actual number that is to be logged.
     *
     * @param call The phone connection.
     * @return the phone number to be logged.
     */
    private String getLogNumber(Call call) {
        Uri handle = call.getOriginalHandle();

        if (handle == null) {
            return null;
        }

        String handleString = handle.getSchemeSpecificPart();
        if (!PhoneNumberUtils.isUriNumber(handleString)) {
            handleString = PhoneNumberUtils.stripSeparators(handleString);
        }
        return handleString;
    }

    /**
     * Adds the call defined by the parameters in the provided AddCallArgs to the CallLogProvider
     * using an AsyncTask to avoid blocking the main thread.
     *
     * @param args Prepopulated call details.
     * @return A handle to the AsyncTask that will add the call to the call log asynchronously.
     */
    public AsyncTask<AddCallArgs, Void, Uri[]> logCallAsync(AddCallArgs args) {
        return new LogCallAsyncTask().execute(args);
    }

    /**
     * Helper AsyncTask to access the call logs database asynchronously since database operations
     * can take a long time depending on the system's load. Since it extends AsyncTask, it uses
     * its own thread pool.
     */
    private class LogCallAsyncTask extends AsyncTask<AddCallArgs, Void, Uri[]> {
        @Override
        protected Uri[] doInBackground(AddCallArgs... callList) {
            int count = callList.length;
            Uri[] result = new Uri[count];
            for (int i = 0; i < count; i++) {
                AddCallArgs c = callList[i];

                try {
                    // May block.
                    result[i] = Calls.addCall(c.callerInfo, c.context, c.number, c.presentation,
                            c.callType, c.features, c.accountHandle, c.timestamp, c.durationInSec,
                            c.dataUsage, true /* addForAllUsers */);
                } catch (Exception e) {
                    // This is very rare but may happen in legitimate cases.
                    // E.g. If the phone is encrypted and thus write request fails, it may cause
                    // some kind of Exception (right now it is IllegalArgumentException, but this
                    // might change).
                    //
                    // We don't want to crash the whole process just because of that, so just log
                    // it instead.
                    Log.e(TAG, e, "Exception raised during adding CallLog entry.");
                    result[i] = null;
                }
            }
            return result;
        }

        /**
         * Performs a simple sanity check to make sure the call was written in the database.
         * Typically there is only one result per call so it is easy to identify which one failed.
         */
        @Override
        protected void onPostExecute(Uri[] result) {
            for (Uri uri : result) {
                if (uri == null) {
                    Log.w(TAG, "Failed to write call to the log.");
                }
            }
        }
    }

    private void sendAddCallBroadcast(int callType, long duration) {
        Intent callAddIntent = new Intent(ACTION_CALLS_TABLE_ADD_ENTRY);
        callAddIntent.putExtra(CALL_TYPE, callType);
        callAddIntent.putExtra(CALL_DURATION, duration);
        mContext.sendBroadcast(callAddIntent, PERMISSION_PROCESS_CALLLOG_INFO);
    }
}
