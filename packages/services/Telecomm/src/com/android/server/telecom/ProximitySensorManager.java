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
 * limitations under the License
 */

package com.android.server.telecom;

import android.content.Context;
import android.os.PowerManager;

/**
 * This class manages the proximity sensor and allows callers to turn it on and off.
 */
public class ProximitySensorManager extends CallsManagerListenerBase {
    private static final String TAG = ProximitySensorManager.class.getSimpleName();

    private final PowerManager.WakeLock mProximityWakeLock;
    private final CallsManager mCallsManager;

    public ProximitySensorManager(Context context, CallsManager callsManager) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            mProximityWakeLock = pm.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
        } else {
            mProximityWakeLock = null;
        }

        mCallsManager = callsManager;
        Log.d(this, "onCreate: mProximityWakeLock: ", mProximityWakeLock);
    }

    @Override
    public void onCallRemoved(Call call) {
        if (mCallsManager.getCalls().isEmpty()) {
            Log.i(this, "All calls removed, resetting proximity sensor to default state");
            turnOff(true);
        }
        super.onCallRemoved(call);
    }

    /**
     * Turn the proximity sensor on.
     */
    void turnOn() {
        if (mCallsManager.getCalls().isEmpty()) {
            Log.w(this, "Asking to turn on prox sensor without a call? I don't think so.");
            return;
        }

        if (mProximityWakeLock == null) {
            return;
        }
        if (!mProximityWakeLock.isHeld()) {
            Log.i(this, "Acquiring proximity wake lock");
            mProximityWakeLock.acquire();
        } else {
            Log.i(this, "Proximity wake lock already acquired");
        }
    }

    /**
     * Turn the proximity sensor off.
     * @param screenOnImmediately
     */
    void turnOff(boolean screenOnImmediately) {
        if (mProximityWakeLock == null) {
            return;
        }
        if (mProximityWakeLock.isHeld()) {
            Log.i(this, "Releasing proximity wake lock");
            int flags =
                (screenOnImmediately ? 0 : PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
            mProximityWakeLock.release(flags);
        } else {
            Log.i(this, "Proximity wake lock already released");
        }
    }
}
