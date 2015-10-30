/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.google.common.base.Predicate;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.ParcelableCall;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.TelephonyManager;

import com.android.internal.telecom.IInCallAdapter;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.HeadsetMediaButton;
import com.android.server.telecom.HeadsetMediaButtonFactory;
import com.android.server.telecom.InCallWakeLockController;
import com.android.server.telecom.InCallWakeLockControllerFactory;
import com.android.server.telecom.Log;
import com.android.server.telecom.MissedCallNotifier;
import com.android.server.telecom.ProximitySensorManager;
import com.android.server.telecom.ProximitySensorManagerFactory;
import com.android.server.telecom.TelecomSystem;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class TelecomSystemTest extends TelecomTestCase {

    static final int TEST_POLL_INTERVAL = 10;  // milliseconds
    static final int TEST_TIMEOUT = 1000;  // milliseconds

    @Mock MissedCallNotifier mMissedCallNotifier;
    @Mock HeadsetMediaButton mHeadsetMediaButton;
    @Mock ProximitySensorManager mProximitySensorManager;
    @Mock InCallWakeLockController mInCallWakeLockController;

    final ComponentName mInCallServiceComponentNameX =
            new ComponentName(
                    "incall-service-package-X",
                    "incall-service-class-X");
    final ComponentName mInCallServiceComponentNameY =
            new ComponentName(
                    "incall-service-package-Y",
                    "incall-service-class-Y");

    InCallServiceFixture mInCallServiceFixtureX;
    InCallServiceFixture mInCallServiceFixtureY;

    final ComponentName mConnectionServiceComponentNameA =
            new ComponentName(
                    "connection-service-package-A",
                    "connection-service-class-A");
    final ComponentName mConnectionServiceComponentNameB =
            new ComponentName(
                    "connection-service-package-B",
                    "connection-service-class-B");

    final PhoneAccount mPhoneAccountA0 =
            PhoneAccount.builder(
                    new PhoneAccountHandle(
                            mConnectionServiceComponentNameA,
                            "id A 0"),
                    "Phone account service A ID 0")
                    .addSupportedUriScheme("tel")
                    .setCapabilities(
                            PhoneAccount.CAPABILITY_CALL_PROVIDER |
                            PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
                    .build();
    final PhoneAccount mPhoneAccountA1 =
            PhoneAccount.builder(
                    new PhoneAccountHandle(
                            mConnectionServiceComponentNameA,
                            "id A 1"),
                    "Phone account service A ID 1")
                    .addSupportedUriScheme("tel")
                    .setCapabilities(
                            PhoneAccount.CAPABILITY_CALL_PROVIDER |
                            PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
                    .build();
    final PhoneAccount mPhoneAccountB0 =
            PhoneAccount.builder(
                    new PhoneAccountHandle(
                            mConnectionServiceComponentNameB,
                            "id B 0"),
                    "Phone account service B ID 0")
                    .addSupportedUriScheme("tel")
                    .setCapabilities(
                            PhoneAccount.CAPABILITY_CALL_PROVIDER |
                            PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
                    .build();

    ConnectionServiceFixture mConnectionServiceFixtureA;
    ConnectionServiceFixture mConnectionServiceFixtureB;

    CallerInfoAsyncQueryFactoryFixture mCallerInfoAsyncQueryFactoryFixture;

    TelecomSystem mTelecomSystem;

    class IdPair {
        final String mConnectionId;
        final String mCallId;

        public IdPair(String connectionId, String callId) {
            this.mConnectionId = connectionId;
            this.mCallId = callId;
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // First set up information about the In-Call services in the mock Context, since
        // Telecom will search for these as soon as it is instantiated
        setupInCallServices();

        // Next, create the TelecomSystem, our system under test
        setupTelecomSystem();

        // Finally, register the ConnectionServices with the PhoneAccountRegistrar of the
        // now-running TelecomSystem
        setupConnectionServices();
    }

    @Override
    public void tearDown() throws Exception {
        mTelecomSystem = null;
        super.tearDown();
    }

    private void setupTelecomSystem() throws Exception {
        HeadsetMediaButtonFactory headsetMediaButtonFactory =
                mock(HeadsetMediaButtonFactory.class);
        ProximitySensorManagerFactory proximitySensorManagerFactory =
                mock(ProximitySensorManagerFactory.class);
        InCallWakeLockControllerFactory inCallWakeLockControllerFactory =
                mock(InCallWakeLockControllerFactory.class);

        mCallerInfoAsyncQueryFactoryFixture = new CallerInfoAsyncQueryFactoryFixture();

        when(headsetMediaButtonFactory.create(
                any(Context.class),
                any(CallsManager.class),
                any(TelecomSystem.SyncRoot.class)))
                .thenReturn(mHeadsetMediaButton);
        when(proximitySensorManagerFactory.create(
                any(Context.class),
                any(CallsManager.class)))
                .thenReturn(mProximitySensorManager);
        when(inCallWakeLockControllerFactory.create(
                any(Context.class),
                any(CallsManager.class)))
                .thenReturn(mInCallWakeLockController);

        mTelecomSystem = new TelecomSystem(
                mComponentContextFixture.getTestDouble(),
                mMissedCallNotifier,
                mCallerInfoAsyncQueryFactoryFixture.getTestDouble(),
                headsetMediaButtonFactory,
                proximitySensorManagerFactory,
                inCallWakeLockControllerFactory);

        verify(headsetMediaButtonFactory).create(
                eq(mComponentContextFixture.getTestDouble().getApplicationContext()),
                any(CallsManager.class),
                any(TelecomSystem.SyncRoot.class));
        verify(proximitySensorManagerFactory).create(
                eq(mComponentContextFixture.getTestDouble().getApplicationContext()),
                any(CallsManager.class));
        verify(inCallWakeLockControllerFactory).create(
                eq(mComponentContextFixture.getTestDouble().getApplicationContext()),
                any(CallsManager.class));
    }

    private void setupConnectionServices() throws Exception {
        mConnectionServiceFixtureA = new ConnectionServiceFixture();
        mConnectionServiceFixtureB = new ConnectionServiceFixture();

        mComponentContextFixture.addConnectionService(
                mConnectionServiceComponentNameA,
                mConnectionServiceFixtureA.getTestDouble());
        mComponentContextFixture.addConnectionService(
                mConnectionServiceComponentNameB,
                mConnectionServiceFixtureB.getTestDouble());

        mTelecomSystem.getPhoneAccountRegistrar().registerPhoneAccount(mPhoneAccountA0);
        mTelecomSystem.getPhoneAccountRegistrar().registerPhoneAccount(mPhoneAccountA1);
        mTelecomSystem.getPhoneAccountRegistrar().registerPhoneAccount(mPhoneAccountB0);

        mTelecomSystem.getPhoneAccountRegistrar().setUserSelectedOutgoingPhoneAccount(
                mPhoneAccountA0.getAccountHandle());
    }

    private void setupInCallServices() throws Exception {
        mComponentContextFixture.putResource(
                com.android.server.telecom.R.string.ui_default_package,
                mInCallServiceComponentNameX.getPackageName());
        mComponentContextFixture.putResource(
                com.android.server.telecom.R.string.incall_default_class,
                mInCallServiceComponentNameX.getClassName());

        mInCallServiceFixtureX = new InCallServiceFixture();
        mInCallServiceFixtureY = new InCallServiceFixture();

        mComponentContextFixture.addInCallService(
                mInCallServiceComponentNameX,
                mInCallServiceFixtureX.getTestDouble());
        mComponentContextFixture.addInCallService(
                mInCallServiceComponentNameY,
                mInCallServiceFixtureY.getTestDouble());
    }

    private IdPair startOutgoingPhoneCall(
            String number,
            PhoneAccountHandle phoneAccountHandle,
            ConnectionServiceFixture connectionServiceFixture) throws Exception {
        reset(
                connectionServiceFixture.getTestDouble(),
                mInCallServiceFixtureX.getTestDouble(),
                mInCallServiceFixtureY.getTestDouble());

        assertEquals(
                mInCallServiceFixtureX.mCallById.size(),
                mInCallServiceFixtureY.mCallById.size());
        assertEquals(
                (mInCallServiceFixtureX.mInCallAdapter != null),
                (mInCallServiceFixtureY.mInCallAdapter != null));

        int startingNumConnections = connectionServiceFixture.mConnectionById.size();
        int startingNumCalls = mInCallServiceFixtureX.mCallById.size();
        boolean hasInCallAdapter = mInCallServiceFixtureX.mInCallAdapter != null;

        Intent actionCallIntent = new Intent();
        actionCallIntent.setData(Uri.parse("tel:" + number));
        actionCallIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
        actionCallIntent.setAction(Intent.ACTION_CALL);
        if (phoneAccountHandle != null) {
            actionCallIntent.putExtra(
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    phoneAccountHandle);
        }

        mTelecomSystem.getCallIntentProcessor().processIntent(actionCallIntent);

        if (!hasInCallAdapter) {
            verify(mInCallServiceFixtureX.getTestDouble())
                    .setInCallAdapter(
                            any(IInCallAdapter.class));
            verify(mInCallServiceFixtureY.getTestDouble())
                    .setInCallAdapter(
                            any(IInCallAdapter.class));
        }

        ArgumentCaptor<Intent> newOutgoingCallIntent =
                ArgumentCaptor.forClass(Intent.class);
        ArgumentCaptor<BroadcastReceiver> newOutgoingCallReceiver =
                ArgumentCaptor.forClass(BroadcastReceiver.class);

        verify(mComponentContextFixture.getTestDouble().getApplicationContext())
                .sendOrderedBroadcastAsUser(
                        newOutgoingCallIntent.capture(),
                        any(UserHandle.class),
                        anyString(),
                        anyInt(),
                        newOutgoingCallReceiver.capture(),
                        any(Handler.class),
                        anyInt(),
                        anyString(),
                        any(Bundle.class));

        // Pass on the new outgoing call Intent
        // Set a dummy PendingResult so the BroadcastReceiver agrees to accept onReceive()
        newOutgoingCallReceiver.getValue().setPendingResult(
                new BroadcastReceiver.PendingResult(0, "", null, 0, true, false, null, 0, 0));
        newOutgoingCallReceiver.getValue().setResultData(
                newOutgoingCallIntent.getValue().getStringExtra(Intent.EXTRA_PHONE_NUMBER));
        newOutgoingCallReceiver.getValue().onReceive(
                mComponentContextFixture.getTestDouble(),
                newOutgoingCallIntent.getValue());

        assertEquals(startingNumConnections + 1, connectionServiceFixture.mConnectionById.size());

        verify(connectionServiceFixture.getTestDouble()).createConnection(
                eq(phoneAccountHandle),
                anyString(),
                any(ConnectionRequest.class),
                anyBoolean(),
                anyBoolean());

        connectionServiceFixture.sendHandleCreateConnectionComplete(
                connectionServiceFixture.mLatestConnectionId);

        assertEquals(startingNumCalls + 1, mInCallServiceFixtureX.mCallById.size());
        assertEquals(startingNumCalls + 1, mInCallServiceFixtureY.mCallById.size());

        assertEquals(
                mInCallServiceFixtureX.mLatestCallId,
                mInCallServiceFixtureY.mLatestCallId);

        return new IdPair(
                connectionServiceFixture.mLatestConnectionId,
                mInCallServiceFixtureX.mLatestCallId);
    }

    private IdPair startIncomingPhoneCall(
            String number,
            PhoneAccountHandle phoneAccountHandle,
            final ConnectionServiceFixture connectionServiceFixture) throws Exception {
        reset(
                connectionServiceFixture.getTestDouble(),
                mInCallServiceFixtureX.getTestDouble(),
                mInCallServiceFixtureY.getTestDouble());

        assertEquals(
                mInCallServiceFixtureX.mCallById.size(),
                mInCallServiceFixtureY.mCallById.size());
        assertEquals(
                (mInCallServiceFixtureX.mInCallAdapter != null),
                (mInCallServiceFixtureY.mInCallAdapter != null));

        final int startingNumConnections = connectionServiceFixture.mConnectionById.size();
        final int startingNumCalls = mInCallServiceFixtureX.mCallById.size();
        boolean hasInCallAdapter = mInCallServiceFixtureX.mInCallAdapter != null;

        Bundle extras = new Bundle();
        extras.putParcelable(
                TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null));
        mTelecomSystem.getTelecomServiceImpl().getBinder()
                .addNewIncomingCall(phoneAccountHandle, extras);

        verify(connectionServiceFixture.getTestDouble()).createConnection(
                any(PhoneAccountHandle.class),
                anyString(),
                any(ConnectionRequest.class),
                eq(true),
                eq(false));

        connectionServiceFixture.sendHandleCreateConnectionComplete(
                connectionServiceFixture.mLatestConnectionId);
        connectionServiceFixture.sendSetRinging(
                connectionServiceFixture.mLatestConnectionId);

        // For the case of incoming calls, Telecom connecting the InCall services and adding the
        // Call is triggered by the async completion of the CallerInfoAsyncQuery. Once the Call
        // is added, future interactions as triggered by the ConnectionService, through the various
        // test fixtures, will be synchronous.

        if (!hasInCallAdapter) {
            verify(
                    mInCallServiceFixtureX.getTestDouble(),
                    timeout(TEST_TIMEOUT))
                    .setInCallAdapter(
                            any(IInCallAdapter.class));
            verify(
                    mInCallServiceFixtureY.getTestDouble(),
                    timeout(TEST_TIMEOUT))
                    .setInCallAdapter(
                            any(IInCallAdapter.class));
        }

        // Give the InCallService time to respond

        assertTrueWithTimeout(new Predicate<Void>() {
            @Override
            public boolean apply(Void v) {
                return mInCallServiceFixtureX.mInCallAdapter != null;
            }
        });

        assertTrueWithTimeout(new Predicate<Void>() {
            @Override
            public boolean apply(Void v) {
                return mInCallServiceFixtureY.mInCallAdapter != null;
            }
        });

        verify(
                mInCallServiceFixtureX.getTestDouble(),
                timeout(TEST_TIMEOUT))
                .addCall(
                        any(ParcelableCall.class));
        verify(
                mInCallServiceFixtureY.getTestDouble(),
                timeout(TEST_TIMEOUT))
                .addCall(
                        any(ParcelableCall.class));

        // Give the InCallService time to respond

        assertTrueWithTimeout(new Predicate<Void>() {
            @Override
            public boolean apply(Void v) {
                return startingNumConnections + 1 ==
                        connectionServiceFixture.mConnectionById.size();
            }
        });
        assertTrueWithTimeout(new Predicate<Void>() {
            @Override
            public boolean apply(Void v) {
                return startingNumCalls + 1 == mInCallServiceFixtureX.mCallById.size();
            }
        });
        assertTrueWithTimeout(new Predicate<Void>() {
            @Override
            public boolean apply(Void v) {
                return startingNumCalls + 1 == mInCallServiceFixtureY.mCallById.size();
            }
        });

        assertEquals(
                mInCallServiceFixtureX.mLatestCallId,
                mInCallServiceFixtureY.mLatestCallId);

        return new IdPair(
                connectionServiceFixture.mLatestConnectionId,
                mInCallServiceFixtureX.mLatestCallId);
    }

    private void rapidFire(Runnable... tasks) {
        final CyclicBarrier barrier = new CyclicBarrier(tasks.length);
        final CountDownLatch latch = new CountDownLatch(tasks.length);
        for (int i = 0; i < tasks.length; i++) {
            final Runnable task = tasks[i];
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                        task.run();
                    } catch (InterruptedException | BrokenBarrierException e){
                        Log.e(TelecomSystemTest.this, e, "Unexpectedly interrupted");
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TelecomSystemTest.this, e, "Unexpectedly interrupted");
        }
    }

    // A simple outgoing call, verifying that the appropriate connection service is contacted,
    // the proper lifecycle is followed, and both In-Call Services are updated correctly.
    private IdPair startAndMakeActiveOutgoingCall(
            String number,
            PhoneAccountHandle phoneAccountHandle,
            ConnectionServiceFixture connectionServiceFixture) throws Exception {
        IdPair ids = startOutgoingPhoneCall(number, phoneAccountHandle, connectionServiceFixture);

        connectionServiceFixture.sendSetDialing(ids.mConnectionId);
        assertEquals(Call.STATE_DIALING, mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_DIALING, mInCallServiceFixtureY.getCall(ids.mCallId).getState());

        connectionServiceFixture.sendSetActive(ids.mConnectionId);
        assertEquals(Call.STATE_ACTIVE, mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_ACTIVE, mInCallServiceFixtureY.getCall(ids.mCallId).getState());

        return ids;
    }

    public void testSingleOutgoingCallLocalDisconnect() throws Exception {
        IdPair ids = startAndMakeActiveOutgoingCall(
                "650-555-1212",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);

        mInCallServiceFixtureX.mInCallAdapter.disconnectCall(ids.mCallId);
        assertEquals(Call.STATE_ACTIVE, mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_ACTIVE, mInCallServiceFixtureY.getCall(ids.mCallId).getState());

        mConnectionServiceFixtureA.sendSetDisconnected(ids.mConnectionId, DisconnectCause.LOCAL);
        assertEquals(Call.STATE_DISCONNECTED,
                mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_DISCONNECTED,
                mInCallServiceFixtureY.getCall(ids.mCallId).getState());
    }

    public void testSingleOutgoingCallRemoteDisconnect() throws Exception {
        IdPair ids = startAndMakeActiveOutgoingCall(
                "650-555-1212",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);

        mConnectionServiceFixtureA.sendSetDisconnected(ids.mConnectionId, DisconnectCause.LOCAL);
        assertEquals(Call.STATE_DISCONNECTED,
                mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_DISCONNECTED,
                mInCallServiceFixtureY.getCall(ids.mCallId).getState());
    }

    // A simple incoming call, similar in scope to the previous test
    private IdPair startAndMakeActiveIncomingCall(
            String number,
            PhoneAccountHandle phoneAccountHandle,
            ConnectionServiceFixture connectionServiceFixture) throws Exception {
        IdPair ids = startIncomingPhoneCall(number, phoneAccountHandle, connectionServiceFixture);

        assertEquals(Call.STATE_RINGING, mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_RINGING, mInCallServiceFixtureY.getCall(ids.mCallId).getState());

        mInCallServiceFixtureX.mInCallAdapter
                .answerCall(ids.mCallId, VideoProfile.STATE_AUDIO_ONLY);

        verify(connectionServiceFixture.getTestDouble())
                .answer(ids.mConnectionId);

        connectionServiceFixture.sendSetActive(ids.mConnectionId);
        assertEquals(Call.STATE_ACTIVE, mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_ACTIVE, mInCallServiceFixtureY.getCall(ids.mCallId).getState());

        return ids;
    }

    public void testSingleIncomingCallLocalDisconnect() throws Exception {
        IdPair ids = startAndMakeActiveIncomingCall(
                "650-555-1212",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);

        mInCallServiceFixtureX.mInCallAdapter.disconnectCall(ids.mCallId);
        assertEquals(Call.STATE_ACTIVE, mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_ACTIVE, mInCallServiceFixtureY.getCall(ids.mCallId).getState());

        mConnectionServiceFixtureA.sendSetDisconnected(ids.mConnectionId, DisconnectCause.LOCAL);
        assertEquals(Call.STATE_DISCONNECTED,
                mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_DISCONNECTED,
                mInCallServiceFixtureY.getCall(ids.mCallId).getState());
    }

    public void testSingleIncomingCallRemoteDisconnect() throws Exception {
        IdPair ids = startAndMakeActiveIncomingCall(
                "650-555-1212",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);

        mConnectionServiceFixtureA.sendSetDisconnected(ids.mConnectionId, DisconnectCause.LOCAL);
        assertEquals(Call.STATE_DISCONNECTED,
                mInCallServiceFixtureX.getCall(ids.mCallId).getState());
        assertEquals(Call.STATE_DISCONNECTED,
                mInCallServiceFixtureY.getCall(ids.mCallId).getState());
    }

    public void do_testDeadlockOnOutgoingCall() throws Exception {
        final IdPair ids = startOutgoingPhoneCall(
                "650-555-1212",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);
        rapidFire(
                new Runnable() {
                    @Override
                    public void run() {
                        while (mCallerInfoAsyncQueryFactoryFixture.mRequests.size() > 0) {
                            mCallerInfoAsyncQueryFactoryFixture.mRequests.remove(0).reply();
                        }
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mConnectionServiceFixtureA.sendSetActive(ids.mConnectionId);
                        } catch (Exception e) {
                            Log.e(this, e, "");
                        }
                    }
                });
    }

    public void testDeadlockOnOutgoingCall() throws Exception {
        for (int i = 0; i < 100; i++) {
            TelecomSystemTest test = new TelecomSystemTest();
            test.setContext(getContext());
            test.setTestContext(getTestContext());
            test.setName(getName());
            test.setUp();
            test.do_testDeadlockOnOutgoingCall();
            test.tearDown();
        }
    }

    public void testIncomingThenOutgoingCalls() throws Exception {
        // TODO: We have to use the same PhoneAccount for both; see http://b/18461539
        IdPair incoming = startAndMakeActiveIncomingCall(
                "650-555-2323",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);
        IdPair outgoing = startAndMakeActiveOutgoingCall(
                "650-555-1212",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);
    }

    public void testOutgoingThenIncomingCalls() throws Exception {
        // TODO: We have to use the same PhoneAccount for both; see http://b/18461539
        IdPair outgoing = startAndMakeActiveOutgoingCall(
                "650-555-1212",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);
        IdPair incoming = startAndMakeActiveIncomingCall(
                "650-555-2323",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);
        verify(mConnectionServiceFixtureA.getTestDouble())
                .hold(outgoing.mConnectionId);
        mConnectionServiceFixtureA.mConnectionById.get(outgoing.mConnectionId).state =
                Connection.STATE_HOLDING;
        mConnectionServiceFixtureA.sendSetOnHold(outgoing.mConnectionId);
        assertEquals(
                Call.STATE_HOLDING,
                mInCallServiceFixtureX.getCall(outgoing.mCallId).getState());
        assertEquals(
                Call.STATE_HOLDING,
                mInCallServiceFixtureY.getCall(outgoing.mCallId).getState());
    }

    public void testAudioManagerOperations() throws Exception {
        AudioManager audioManager = (AudioManager) mComponentContextFixture.getTestDouble()
                .getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        IdPair outgoing = startAndMakeActiveOutgoingCall(
                "650-555-1212",
                mPhoneAccountA0.getAccountHandle(),
                mConnectionServiceFixtureA);

        verify(audioManager, timeout(TEST_TIMEOUT))
                .requestAudioFocusForCall(anyInt(), anyInt());
        verify(audioManager, timeout(TEST_TIMEOUT).atLeastOnce())
                .setMode(AudioManager.MODE_IN_CALL);

        mInCallServiceFixtureX.mInCallAdapter.mute(true);
        verify(audioManager, timeout(TEST_TIMEOUT))
                .setMicrophoneMute(true);
        mInCallServiceFixtureX.mInCallAdapter.mute(false);
        verify(audioManager, timeout(TEST_TIMEOUT))
                .setMicrophoneMute(false);

        mInCallServiceFixtureX.mInCallAdapter.setAudioRoute(CallAudioState.ROUTE_SPEAKER);
        verify(audioManager, timeout(TEST_TIMEOUT))
                .setSpeakerphoneOn(true);
        mInCallServiceFixtureX.mInCallAdapter.setAudioRoute(CallAudioState.ROUTE_EARPIECE);
        verify(audioManager, timeout(TEST_TIMEOUT))
                .setSpeakerphoneOn(false);

        mConnectionServiceFixtureA.
                sendSetDisconnected(outgoing.mConnectionId, DisconnectCause.REMOTE);

        verify(audioManager, timeout(TEST_TIMEOUT))
                .abandonAudioFocusForCall();
        verify(audioManager, timeout(TEST_TIMEOUT).atLeastOnce())
                .setMode(AudioManager.MODE_NORMAL);
    }

    protected static void assertTrueWithTimeout(Predicate<Void> predicate) {
        int elapsed = 0;
        while (elapsed < TEST_TIMEOUT) {
            if (predicate.apply(null)) {
                return;
            } else {
                try {
                    Thread.sleep(TEST_POLL_INTERVAL);
                    elapsed += TEST_POLL_INTERVAL;
                } catch (InterruptedException e) {
                    fail(e.toString());
                }
            }
        }
        fail("Timeout in assertTrueWithTimeout");
    }
}
