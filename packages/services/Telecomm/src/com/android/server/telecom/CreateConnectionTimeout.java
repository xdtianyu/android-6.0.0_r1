/*
 * Copyright 2015, The Android Open Source Project
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;

import java.util.Collection;
import java.util.Objects;

/**
 * Registers a timeout for a call and disconnects the call when the timeout expires.
 */
final class CreateConnectionTimeout extends PhoneStateListener implements Runnable {
    private final Context mContext;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final ConnectionServiceWrapper mConnectionService;
    private final Call mCall;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsRegistered;
    private boolean mIsCallTimedOut;

    CreateConnectionTimeout(Context context, PhoneAccountRegistrar phoneAccountRegistrar,
            ConnectionServiceWrapper service, Call call) {
        super(Looper.getMainLooper());
        mContext = context;
        mPhoneAccountRegistrar = phoneAccountRegistrar;
        mConnectionService = service;
        mCall = call;
    }

    boolean isTimeoutNeededForCall(Collection<PhoneAccountHandle> accounts,
            PhoneAccountHandle currentAccount) {
        // Non-emergency calls timeout automatically at the radio layer. No need for a timeout here.
        if (!TelephonyUtil.shouldProcessAsEmergency(mContext, mCall.getHandle())) {
            return false;
        }

        // If there's no connection manager to fallback on then there's no point in having a
        // timeout.
        PhoneAccountHandle connectionManager = mPhoneAccountRegistrar.getSimCallManager();
        if (!accounts.contains(connectionManager)) {
            return false;
        }

        // No need to add a timeout if the current attempt is over the connection manager.
        if (Objects.equals(connectionManager, currentAccount)) {
            return false;
        }

        // To reduce the number of scenarios where a timeout is needed, only use a timeout if
        // we're connected to Wi-Fi. This ensures that the fallback connection manager has an
        // alternate route to place the call. TODO: remove this condition or allow connection
        // managers to specify transports. See http://b/19199181.
        if (!isConnectedToWifi()) {
            return false;
        }

        Log.d(this, "isTimeoutNeededForCall, returning true");
        return true;
    }

    void registerTimeout() {
        Log.d(this, "registerTimeout");
        mIsRegistered = true;
        // First find out the cellular service state. Based on the state we decide whether a timeout
        // will actually be enforced and if so how long it should be.
        TelephonyManager telephonyManager =
            (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(this, PhoneStateListener.LISTEN_SERVICE_STATE);
        telephonyManager.listen(this, 0);
    }

    void unregisterTimeout() {
        Log.d(this, "unregisterTimeout");
        mIsRegistered = false;
        mHandler.removeCallbacksAndMessages(null);
    }

    boolean isCallTimedOut() {
        return mIsCallTimedOut;
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        long timeoutLengthMillis = getTimeoutLengthMillis(serviceState);
        if (!mIsRegistered) {
            Log.d(this, "onServiceStateChanged, timeout no longer registered, skipping");
        } else if (timeoutLengthMillis  <= 0) {
            Log.d(this, "onServiceStateChanged, timeout set to %d, skipping", timeoutLengthMillis);
        } else if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
            // If cellular service is available then don't bother with a timeout.
            Log.d(this, "onServiceStateChanged, cellular service available, skipping");
        } else {
            mHandler.postDelayed(this, timeoutLengthMillis);
        }
    }

    @Override
    public void run() {
        if (mIsRegistered && isCallBeingPlaced(mCall)) {
            Log.d(this, "run, call timed out, calling disconnect");
            mIsCallTimedOut = true;
            mConnectionService.disconnect(mCall);
        }
    }

    static boolean isCallBeingPlaced(Call call) {
        int state = call.getState();
        return state == CallState.NEW
            || state == CallState.CONNECTING
            || state == CallState.DIALING;
    }

    private long getTimeoutLengthMillis(ServiceState serviceState) {
        // If the radio is off then use a longer timeout. This gives us more time to power on the
        // radio.
        if (serviceState.getState() == ServiceState.STATE_POWER_OFF) {
            return Timeouts.getEmergencyCallTimeoutRadioOffMillis(
                    mContext.getContentResolver());
        } else {
            return Timeouts.getEmergencyCallTimeoutMillis(mContext.getContentResolver());
        }
    }

    private boolean isConnectedToWifi() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
            Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
          NetworkInfo ni = cm.getActiveNetworkInfo();
          return ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }
}
