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

package com.android.server.telecom;

import com.android.internal.annotations.VisibleForTesting;

import android.content.Context;
import android.os.PowerManager;

/**
 * Handles acquisition and release of wake locks relating to call state.
 */
@VisibleForTesting
public class InCallWakeLockController extends CallsManagerListenerBase {

    private static final String TAG = "InCallWakeLockContoller";

    private final Context mContext;
    private final PowerManager.WakeLock mFullWakeLock;
    private final CallsManager mCallsManager;

    @VisibleForTesting
    public InCallWakeLockController(Context context, CallsManager callsManager) {
        mContext = context;
        mCallsManager = callsManager;

        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mFullWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);

        callsManager.addListener(this);
    }

    @Override
    public void onCallAdded(Call call) {
        handleWakeLock();
    }

    @Override
    public void onCallRemoved(Call call) {
        handleWakeLock();
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        handleWakeLock();
    }

    private void handleWakeLock() {
        // We grab a full lock as long as there exists a ringing call.
        Call ringingCall = mCallsManager.getRingingCall();
        if (ringingCall != null) {
            mFullWakeLock.acquire();
            Log.i(this, "Acquiring full wake lock");
        } else if (mFullWakeLock.isHeld()) {
            mFullWakeLock.release();
            Log.i(this, "Releasing full wake lock");
        }
    }
}
