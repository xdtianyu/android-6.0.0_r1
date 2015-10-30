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
 * limitations under the License
 */

package android.telecom.cts;

import static android.telecom.cts.TestUtils.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.net.Uri;
import android.telecom.Call;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.InCallService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.StatusHints;
import android.telecom.TelecomManager;

import com.android.cts.telecom.R;

/**
 * Suites of tests that verifies the various Call details.
 */
public class CallDetailsTest extends BaseTelecomTestWithMockServices {

    public static final int CONNECTION_CAPABILITIES =
            Connection.CAPABILITY_HOLD | Connection.CAPABILITY_MUTE |
            /**
             * CAPABILITY_HIGH_DEF_AUDIO & CAPABILITY_WIFI are hidden, so
             * hardcoding the values for now.
             */
            0x00008000 | 0x00010000;
    public static final int CALL_CAPABILITIES =
            Call.Details.CAPABILITY_HOLD | Call.Details.CAPABILITY_MUTE;
    public static final int CALL_PROPERTIES =
            Call.Details.PROPERTY_HIGH_DEF_AUDIO | Call.Details.PROPERTY_WIFI;
    public static final String CALLER_DISPLAY_NAME = "CTS test";
    public static final int CALLER_DISPLAY_NAME_PRESENTATION = TelecomManager.PRESENTATION_ALLOWED;

    private StatusHints mStatusHints;
    private Bundle mExtras = new Bundle();

    private MockInCallService mInCallService;
    private Call mCall;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (mShouldTestTelecom) {
            PhoneAccount account = setupConnectionService(
                    new MockConnectionService() {
                        @Override
                        public Connection onCreateOutgoingConnection(
                                PhoneAccountHandle connectionManagerPhoneAccount,
                                ConnectionRequest request) {
                            Connection connection = super.onCreateOutgoingConnection(
                                    connectionManagerPhoneAccount,
                                    request);
                            // Modify the connection object created with local values.
                            connection.setConnectionCapabilities(CONNECTION_CAPABILITIES);
                            connection.setCallerDisplayName(
                                    CALLER_DISPLAY_NAME,
                                    CALLER_DISPLAY_NAME_PRESENTATION);
                            connection.setExtras(mExtras);
                            mStatusHints = new StatusHints(
                                    "CTS test",
                                    Icon.createWithResource(
                                            getInstrumentation().getContext(),
                                            R.drawable.ic_phone_24dp),
                                            null);
                            connection.setStatusHints(mStatusHints);
                            lock.release();
                            return connection;
                        }
                    }, FLAG_REGISTER | FLAG_ENABLE);

            /** Place a call as a part of the setup before we test the various
             *  Call details.
             */
            placeAndVerifyCall();
            verifyConnectionForOutgoingCall();

            mInCallService = mInCallCallbacks.getService();
            mCall = mInCallService.getLastCall();

            assertCallState(mCall, Call.STATE_DIALING);
        }
    }

    /**
     * Tests whether the getAccountHandle() getter returns the correct object.
     */
    public void testAccountHandle() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getAccountHandle(), is(PhoneAccountHandle.class));
        assertEquals(TEST_PHONE_ACCOUNT_HANDLE, mCall.getDetails().getAccountHandle());
    }

    /**
     * Tests whether the getCallCapabilities() getter returns the correct object.
     */
    public void testCallCapabilities() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getCallCapabilities(), is(Integer.class));
        assertEquals(CALL_CAPABILITIES, mCall.getDetails().getCallCapabilities());
        assertTrue(mCall.getDetails().can(Call.Details.CAPABILITY_HOLD));
        assertTrue(mCall.getDetails().can(Call.Details.CAPABILITY_MUTE));
        assertFalse(mCall.getDetails().can(Call.Details.CAPABILITY_MANAGE_CONFERENCE));
        assertFalse(mCall.getDetails().can(Call.Details.CAPABILITY_RESPOND_VIA_TEXT));
    }

    /**
     * Tests whether the getCallerDisplayName() getter returns the correct object.
     */
    public void testCallerDisplayName() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getCallerDisplayName(), is(String.class));
        assertEquals(CALLER_DISPLAY_NAME, mCall.getDetails().getCallerDisplayName());
    }

    /**
     * Tests whether the getCallerDisplayNamePresentation() getter returns the correct object.
     */
    public void testCallerDisplayNamePresentation() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getCallerDisplayNamePresentation(), is(Integer.class));
        assertEquals(CALLER_DISPLAY_NAME_PRESENTATION, mCall.getDetails().getCallerDisplayNamePresentation());
    }

    /**
     * Tests whether the getCallProperties() getter returns the correct object.
     */
    public void testCallProperties() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getCallProperties(), is(Integer.class));
        assertEquals(CALL_PROPERTIES, mCall.getDetails().getCallProperties());
    }

    /**
     * Tests whether the getConnectTimeMillis() getter returns the correct object.
     */
    public void testConnectTimeMillis() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getConnectTimeMillis(), is(Long.class));
    }

    /**
     * Tests whether the getDisconnectCause() getter returns the correct object.
     */
    public void testDisconnectCause() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getDisconnectCause(), is(DisconnectCause.class));
    }

    /**
     * Tests whether the getExtras() getter returns the correct object.
     */
    public void testExtras() {
        if (!mShouldTestTelecom) {
            return;
        }

        if (mCall.getDetails().getExtras() != null) {
            assertThat(mCall.getDetails().getExtras(), is(Bundle.class));
        }
    }

    /**
     * Tests whether the getIntentExtras() getter returns the correct object.
     */
    public void testIntentExtras() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getIntentExtras(), is(Bundle.class));
    }

    /**
     * Tests whether the getGatewayInfo() getter returns the correct object.
     */
    public void testGatewayInfo() {
        if (!mShouldTestTelecom) {
            return;
        }

        if (mCall.getDetails().getGatewayInfo() != null) {
            assertThat(mCall.getDetails().getGatewayInfo(), is(GatewayInfo.class));
        }
    }

    /**
     * Tests whether the getHandle() getter returns the correct object.
     */
    public void testHandle() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getHandle(), is(Uri.class));
        assertEquals(getTestNumber(), mCall.getDetails().getHandle());
    }

    /**
     * Tests whether the getHandlePresentation() getter returns the correct object.
     */
    public void testHandlePresentation() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getHandlePresentation(), is(Integer.class));
        assertEquals(MockConnectionService.CONNECTION_PRESENTATION, mCall.getDetails().getHandlePresentation());
    }

    /**
     * Tests whether the getStatusHints() getter returns the correct object.
     */
    public void testStatusHints() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getStatusHints(), is(StatusHints.class));
        assertEquals(mStatusHints.getLabel(), mCall.getDetails().getStatusHints().getLabel());
        assertEquals(
                mStatusHints.getIcon().toString(),
                mCall.getDetails().getStatusHints().getIcon().toString());
        assertEquals(mStatusHints.getExtras(), mCall.getDetails().getStatusHints().getExtras());
    }

    /**
     * Tests whether the getVideoState() getter returns the correct object.
     */
    public void testVideoState() {
        if (!mShouldTestTelecom) {
            return;
        }

        assertThat(mCall.getDetails().getVideoState(), is(Integer.class));
    }
}
