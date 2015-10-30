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

package com.android.server.telecom.tests;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import android.content.Context;
import android.os.PowerManager;

import com.android.server.telecom.Call;
import com.android.server.telecom.CallState;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.InCallWakeLockController;

import org.mockito.Mock;

/**
 * TODO: The tests here are disabled because they depend on classes {@code PowerManager} and
 * {@code PowerManager.WakeLock}, which are both {@code final} and therefore cannot easily be
 * mocked.
 *
 * At the moment, we are using an {@link com.android.server.telecom.InCallWakeLockControllerFactory}
 * in the system under test to abstract out this class, and are assuming it's simple enough that it
 * is not the highest priority to test.
 */
public class InCallWakeLockControllerTest extends TelecomTestCase {

    @Mock Context mContext;
    @Mock PowerManager mPowerManager;
    @Mock PowerManager.WakeLock mWakeLock;
    @Mock CallsManager mCallsManager;
    @Mock Call mCall;

    private InCallWakeLockController mInCallWakeLockController;

    @Override
    public void setUp() throws Exception {
        /*
        super.setUp();

        when(mContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mPowerManager);
        when(mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "InCallWakeLockController"))
                .thenReturn(mWakeLock);
        mInCallWakeLockController = new InCallWakeLockController(mContext, mCallsManager);
        */
    }

    @Override
    public void tearDown() throws Exception {
        /*
        super.tearDown();
        */
    }

    public void DONT_test_RingingCallAdded() throws Exception {
        when(mCallsManager.getRingingCall()).thenReturn(mCall);
        mInCallWakeLockController.onCallAdded(mCall);
        verify(mWakeLock).acquire();
    }

    public void DONT_test_NonRingingCallAdded() throws Exception {
        when(mCallsManager.getRingingCall()).thenReturn(null);
        when(mWakeLock.isHeld()).thenReturn(false);

        mInCallWakeLockController.onCallAdded(mCall);
        verify(mWakeLock, never()).acquire();
    }

    public void DONT_test_RingingCallTransition() throws Exception {
        when(mCallsManager.getRingingCall()).thenReturn(mCall);
        mInCallWakeLockController.onCallStateChanged(mCall, CallState.NEW, CallState.RINGING);
        verify(mWakeLock).acquire();
    }

    public void DONT_test_RingingCallRemoved() throws Exception {
        when(mCallsManager.getRingingCall()).thenReturn(null);
        when(mWakeLock.isHeld()).thenReturn(false);

        mInCallWakeLockController.onCallRemoved(mCall);
        verify(mWakeLock, never()).acquire();
    }

    public void DONT_test_WakeLockReleased() throws Exception {
        when(mCallsManager.getRingingCall()).thenReturn(null);
        when(mWakeLock.isHeld()).thenReturn(true);

        mInCallWakeLockController.onCallRemoved(mCall);
        verify(mWakeLock).release();
    }
}
