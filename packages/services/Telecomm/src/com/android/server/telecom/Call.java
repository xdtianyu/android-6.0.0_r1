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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.Trace;
import android.provider.ContactsContract.Contacts;
import android.telecom.DisconnectCause;
import android.telecom.Connection;
import android.telecom.GatewayInfo;
import android.telecom.InCallService.VideoCall;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.Response;
import android.telecom.StatusHints;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery.OnQueryCompleteListener;
import com.android.internal.telephony.SmsApplication;
import com.android.server.telecom.ContactsAsyncHelper.OnImageLoadCompleteListener;
import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Encapsulates all aspects of a given phone call throughout its lifecycle, starting
 *  from the time the call intent was received by Telecom (vs. the time the call was
 *  connected etc).
 */
@VisibleForTesting
public class Call implements CreateConnectionResponse {
    /**
     * Listener for events on the call.
     */
    interface Listener {
        void onSuccessfulOutgoingCall(Call call, int callState);
        void onFailedOutgoingCall(Call call, DisconnectCause disconnectCause);
        void onSuccessfulIncomingCall(Call call);
        void onFailedIncomingCall(Call call);
        void onSuccessfulUnknownCall(Call call, int callState);
        void onFailedUnknownCall(Call call);
        void onRingbackRequested(Call call, boolean ringbackRequested);
        void onPostDialWait(Call call, String remaining);
        void onPostDialChar(Call call, char nextChar);
        void onConnectionCapabilitiesChanged(Call call);
        void onParentChanged(Call call);
        void onChildrenChanged(Call call);
        void onCannedSmsResponsesLoaded(Call call);
        void onVideoCallProviderChanged(Call call);
        void onCallerInfoChanged(Call call);
        void onIsVoipAudioModeChanged(Call call);
        void onStatusHintsChanged(Call call);
        void onExtrasChanged(Call call);
        void onHandleChanged(Call call);
        void onCallerDisplayNameChanged(Call call);
        void onVideoStateChanged(Call call);
        void onTargetPhoneAccountChanged(Call call);
        void onConnectionManagerPhoneAccountChanged(Call call);
        void onPhoneAccountChanged(Call call);
        void onConferenceableCallsChanged(Call call);
        boolean onCanceledViaNewOutgoingCallBroadcast(Call call);
    }

    public abstract static class ListenerBase implements Listener {
        @Override
        public void onSuccessfulOutgoingCall(Call call, int callState) {}
        @Override
        public void onFailedOutgoingCall(Call call, DisconnectCause disconnectCause) {}
        @Override
        public void onSuccessfulIncomingCall(Call call) {}
        @Override
        public void onFailedIncomingCall(Call call) {}
        @Override
        public void onSuccessfulUnknownCall(Call call, int callState) {}
        @Override
        public void onFailedUnknownCall(Call call) {}
        @Override
        public void onRingbackRequested(Call call, boolean ringbackRequested) {}
        @Override
        public void onPostDialWait(Call call, String remaining) {}
        @Override
        public void onPostDialChar(Call call, char nextChar) {}
        @Override
        public void onConnectionCapabilitiesChanged(Call call) {}
        @Override
        public void onParentChanged(Call call) {}
        @Override
        public void onChildrenChanged(Call call) {}
        @Override
        public void onCannedSmsResponsesLoaded(Call call) {}
        @Override
        public void onVideoCallProviderChanged(Call call) {}
        @Override
        public void onCallerInfoChanged(Call call) {}
        @Override
        public void onIsVoipAudioModeChanged(Call call) {}
        @Override
        public void onStatusHintsChanged(Call call) {}
        @Override
        public void onExtrasChanged(Call call) {}
        @Override
        public void onHandleChanged(Call call) {}
        @Override
        public void onCallerDisplayNameChanged(Call call) {}
        @Override
        public void onVideoStateChanged(Call call) {}
        @Override
        public void onTargetPhoneAccountChanged(Call call) {}
        @Override
        public void onConnectionManagerPhoneAccountChanged(Call call) {}
        @Override
        public void onPhoneAccountChanged(Call call) {}
        @Override
        public void onConferenceableCallsChanged(Call call) {}
        @Override
        public boolean onCanceledViaNewOutgoingCallBroadcast(Call call) {
            return false;
        }
    }

    private final OnQueryCompleteListener mCallerInfoQueryListener =
            new OnQueryCompleteListener() {
                /** ${inheritDoc} */
                @Override
                public void onQueryComplete(int token, Object cookie, CallerInfo callerInfo) {
                    synchronized (mLock) {
                        if (cookie != null) {
                            ((Call) cookie).setCallerInfo(callerInfo, token);
                        }
                    }
                }
            };

    private final OnImageLoadCompleteListener mPhotoLoadListener =
            new OnImageLoadCompleteListener() {
                /** ${inheritDoc} */
                @Override
                public void onImageLoadComplete(
                        int token, Drawable photo, Bitmap photoIcon, Object cookie) {
                    synchronized (mLock) {
                        if (cookie != null) {
                            ((Call) cookie).setPhoto(photo, photoIcon, token);
                        }
                    }
                }
            };

    private final Runnable mDirectToVoicemailRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                processDirectToVoicemail();
            }
        }
    };

    /** True if this is an incoming call. */
    private final boolean mIsIncoming;

    /** True if this is a currently unknown call that was not previously tracked by CallsManager,
     *  and did not originate via the regular incoming/outgoing call code paths.
     */
    private boolean mIsUnknown;

    /**
     * The time this call was created. Beyond logging and such, may also be used for bookkeeping
     * and specifically for marking certain call attempts as failed attempts.
     */
    private long mCreationTimeMillis = System.currentTimeMillis();

    /** The time this call was made active. */
    private long mConnectTimeMillis = 0;

    /** The time this call was disconnected. */
    private long mDisconnectTimeMillis = 0;

    /** The gateway information associated with this call. This stores the original call handle
     * that the user is attempting to connect to via the gateway, the actual handle to dial in
     * order to connect the call via the gateway, as well as the package name of the gateway
     * service. */
    private GatewayInfo mGatewayInfo;

    private PhoneAccountHandle mConnectionManagerPhoneAccountHandle;

    private PhoneAccountHandle mTargetPhoneAccountHandle;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final List<Call> mConferenceableCalls = new ArrayList<>();

    /** The state of the call. */
    private int mState;

    /** The handle with which to establish this call. */
    private Uri mHandle;

    /**
     * The presentation requirements for the handle. See {@link TelecomManager} for valid values.
     */
    private int mHandlePresentation;

    /** The caller display name (CNAP) set by the connection service. */
    private String mCallerDisplayName;

    /**
     * The presentation requirements for the handle. See {@link TelecomManager} for valid values.
     */
    private int mCallerDisplayNamePresentation;

    /**
     * The connection service which is attempted or already connecting this call.
     */
    private ConnectionServiceWrapper mConnectionService;

    private boolean mIsEmergencyCall;

    private boolean mSpeakerphoneOn;

    /**
     * Tracks the video states which were applicable over the duration of a call.
     * See {@link VideoProfile} for a list of valid video states.
     * <p>
     * Video state history is tracked when the call is active, and when a call is rejected or
     * missed.
     */
    private int mVideoStateHistory;

    private int mVideoState;

    /**
     * Disconnect cause for the call. Only valid if the state of the call is STATE_DISCONNECTED.
     * See {@link android.telecom.DisconnectCause}.
     */
    private DisconnectCause mDisconnectCause = new DisconnectCause(DisconnectCause.UNKNOWN);

    private Bundle mIntentExtras = new Bundle();

    /** Set of listeners on this call.
     *
     * ConcurrentHashMap constructor params: 8 is initial table size, 0.9f is
     * load factor before resizing, 1 means we only expect a single thread to
     * access the map so make only a single shard
     */
    private final Set<Listener> mListeners = Collections.newSetFromMap(
            new ConcurrentHashMap<Listener, Boolean>(8, 0.9f, 1));

    private CreateConnectionProcessor mCreateConnectionProcessor;

    /** Caller information retrieved from the latest contact query. */
    private CallerInfo mCallerInfo;

    /** The latest token used with a contact info query. */
    private int mQueryToken = 0;

    /** Whether this call is requesting that Telecom play the ringback tone on its behalf. */
    private boolean mRingbackRequested = false;

    /** Whether direct-to-voicemail query is pending. */
    private boolean mDirectToVoicemailQueryPending;

    private int mConnectionCapabilities;

    private boolean mIsConference = false;

    private Call mParentCall = null;

    private List<Call> mChildCalls = new LinkedList<>();

    /** Set of text message responses allowed for this call, if applicable. */
    private List<String> mCannedSmsResponses = Collections.EMPTY_LIST;

    /** Whether an attempt has been made to load the text message responses. */
    private boolean mCannedSmsResponsesLoadingStarted = false;

    private IVideoProvider mVideoProvider;
    private VideoProviderProxy mVideoProviderProxy;

    private boolean mIsVoipAudioMode;
    private StatusHints mStatusHints;
    private Bundle mExtras;
    private final ConnectionServiceRepository mRepository;
    private final ContactsAsyncHelper mContactsAsyncHelper;
    private final Context mContext;
    private final CallsManager mCallsManager;
    private final TelecomSystem.SyncRoot mLock;
    private final CallerInfoAsyncQueryFactory mCallerInfoAsyncQueryFactory;

    private boolean mWasConferencePreviouslyMerged = false;

    // For conferences which support merge/swap at their level, we retain a notion of an active
    // call. This is used for BluetoothPhoneService.  In order to support hold/merge, it must have
    // the notion of the current "active" call within the conference call. This maintains the
    // "active" call and switches every time the user hits "swap".
    private Call mConferenceLevelActiveCall = null;

    private boolean mIsLocallyDisconnecting = false;

    /**
     * Persists the specified parameters and initializes the new instance.
     *
     * @param context The context.
     * @param repository The connection service repository.
     * @param handle The handle to dial.
     * @param gatewayInfo Gateway information to use for the call.
     * @param connectionManagerPhoneAccountHandle Account to use for the service managing the call.
     *         This account must be one that was registered with the
     *         {@link PhoneAccount#CAPABILITY_CONNECTION_MANAGER} flag.
     * @param targetPhoneAccountHandle Account information to use for the call. This account must be
     *         one that was registered with the {@link PhoneAccount#CAPABILITY_CALL_PROVIDER} flag.
     * @param isIncoming True if this is an incoming call.
     */
    public Call(
            Context context,
            CallsManager callsManager,
            TelecomSystem.SyncRoot lock,
            ConnectionServiceRepository repository,
            ContactsAsyncHelper contactsAsyncHelper,
            CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory,
            Uri handle,
            GatewayInfo gatewayInfo,
            PhoneAccountHandle connectionManagerPhoneAccountHandle,
            PhoneAccountHandle targetPhoneAccountHandle,
            boolean isIncoming,
            boolean isConference) {
        mState = isConference ? CallState.ACTIVE : CallState.NEW;
        mContext = context;
        mCallsManager = callsManager;
        mLock = lock;
        mRepository = repository;
        mContactsAsyncHelper = contactsAsyncHelper;
        mCallerInfoAsyncQueryFactory = callerInfoAsyncQueryFactory;
        setHandle(handle);
        setHandle(handle, TelecomManager.PRESENTATION_ALLOWED);
        mGatewayInfo = gatewayInfo;
        setConnectionManagerPhoneAccount(connectionManagerPhoneAccountHandle);
        setTargetPhoneAccount(targetPhoneAccountHandle);
        mIsIncoming = isIncoming;
        mIsConference = isConference;
        maybeLoadCannedSmsResponses();

        Log.event(this, Log.Events.CREATED);
    }

    /**
     * Persists the specified parameters and initializes the new instance.
     *
     * @param context The context.
     * @param repository The connection service repository.
     * @param handle The handle to dial.
     * @param gatewayInfo Gateway information to use for the call.
     * @param connectionManagerPhoneAccountHandle Account to use for the service managing the call.
     *         This account must be one that was registered with the
     *         {@link PhoneAccount#CAPABILITY_CONNECTION_MANAGER} flag.
     * @param targetPhoneAccountHandle Account information to use for the call. This account must be
     *         one that was registered with the {@link PhoneAccount#CAPABILITY_CALL_PROVIDER} flag.
     * @param isIncoming True if this is an incoming call.
     * @param connectTimeMillis The connection time of the call.
     */
    Call(
            Context context,
            CallsManager callsManager,
            TelecomSystem.SyncRoot lock,
            ConnectionServiceRepository repository,
            ContactsAsyncHelper contactsAsyncHelper,
            CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory,
            Uri handle,
            GatewayInfo gatewayInfo,
            PhoneAccountHandle connectionManagerPhoneAccountHandle,
            PhoneAccountHandle targetPhoneAccountHandle,
            boolean isIncoming,
            boolean isConference,
            long connectTimeMillis) {
        this(context, callsManager, lock, repository, contactsAsyncHelper,
                callerInfoAsyncQueryFactory, handle, gatewayInfo,
                connectionManagerPhoneAccountHandle, targetPhoneAccountHandle, isIncoming,
                isConference);

        mConnectTimeMillis = connectTimeMillis;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        if (listener != null) {
            mListeners.remove(listener);
        }
    }

    public void destroy() {
        Log.event(this, Log.Events.DESTROYED);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String component = null;
        if (mConnectionService != null && mConnectionService.getComponentName() != null) {
            component = mConnectionService.getComponentName().flattenToShortString();
        }



        return String.format(Locale.US, "[%s, %s, %s, %s, %s, childs(%d), has_parent(%b), [%s]]",
                System.identityHashCode(this),
                CallState.toString(mState),
                component,
                Log.piiHandle(mHandle),
                getVideoStateDescription(getVideoState()),
                getChildCalls().size(),
                getParentCall() != null,
                Connection.capabilitiesToString(getConnectionCapabilities()));
    }

    /**
     * Builds a debug-friendly description string for a video state.
     * <p>
     * A = audio active, T = video transmission active, R = video reception active, P = video
     * paused.
     *
     * @param videoState The video state.
     * @return A string indicating which bits are set in the video state.
     */
    private String getVideoStateDescription(int videoState) {
        StringBuilder sb = new StringBuilder();
        sb.append("A");

        if (VideoProfile.isTransmissionEnabled(videoState)) {
            sb.append("T");
        }

        if (VideoProfile.isReceptionEnabled(videoState)) {
            sb.append("R");
        }

        if (VideoProfile.isPaused(videoState)) {
            sb.append("P");
        }

        return sb.toString();
    }

    int getState() {
        return mState;
    }

    private boolean shouldContinueProcessingAfterDisconnect() {
        // Stop processing once the call is active.
        if (!CreateConnectionTimeout.isCallBeingPlaced(this)) {
            return false;
        }

        // Make sure that there are additional connection services to process.
        if (mCreateConnectionProcessor == null
            || !mCreateConnectionProcessor.isProcessingComplete()
            || !mCreateConnectionProcessor.hasMorePhoneAccounts()) {
            return false;
        }

        if (mDisconnectCause == null) {
            return false;
        }

        // Continue processing if the current attempt failed or timed out.
        return mDisconnectCause.getCode() == DisconnectCause.ERROR ||
            mCreateConnectionProcessor.isCallTimedOut();
    }

    /**
     * Sets the call state. Although there exists the notion of appropriate state transitions
     * (see {@link CallState}), in practice those expectations break down when cellular systems
     * misbehave and they do this very often. The result is that we do not enforce state transitions
     * and instead keep the code resilient to unexpected state changes.
     */
    public void setState(int newState, String tag) {
        if (mState != newState) {
            Log.v(this, "setState %s -> %s", mState, newState);

            if (newState == CallState.DISCONNECTED && shouldContinueProcessingAfterDisconnect()) {
                Log.w(this, "continuing processing disconnected call with another service");
                mCreateConnectionProcessor.continueProcessingIfPossible(this, mDisconnectCause);
                return;
            }

            mState = newState;
            maybeLoadCannedSmsResponses();

            if (mState == CallState.ACTIVE || mState == CallState.ON_HOLD) {
                if (mConnectTimeMillis == 0) {
                    // We check to see if mConnectTime is already set to prevent the
                    // call from resetting active time when it goes in and out of
                    // ACTIVE/ON_HOLD
                    mConnectTimeMillis = System.currentTimeMillis();
                }

                // Video state changes are normally tracked against history when a call is active.
                // When the call goes active we need to be sure we track the history in case the
                // state never changes during the duration of the call -- we want to ensure we
                // always know the state at the start of the call.
                mVideoStateHistory = mVideoStateHistory | mVideoState;

                // We're clearly not disconnected, so reset the disconnected time.
                mDisconnectTimeMillis = 0;
            } else if (mState == CallState.DISCONNECTED) {
                mDisconnectTimeMillis = System.currentTimeMillis();
                setLocallyDisconnecting(false);
                fixParentAfterDisconnect();
            }
            if (mState == CallState.DISCONNECTED &&
                    mDisconnectCause.getCode() == DisconnectCause.MISSED) {
                // Ensure when an incoming call is missed that the video state history is updated.
                mVideoStateHistory |= mVideoState;
            }

            // Log the state transition event
            String event = null;
            Object data = null;
            switch (newState) {
                case CallState.ACTIVE:
                    event = Log.Events.SET_ACTIVE;
                    break;
                case CallState.CONNECTING:
                    event = Log.Events.SET_CONNECTING;
                    break;
                case CallState.DIALING:
                    event = Log.Events.SET_DIALING;
                    break;
                case CallState.DISCONNECTED:
                    event = Log.Events.SET_DISCONNECTED;
                    data = getDisconnectCause();
                    break;
                case CallState.DISCONNECTING:
                    event = Log.Events.SET_DISCONNECTING;
                    break;
                case CallState.ON_HOLD:
                    event = Log.Events.SET_HOLD;
                    break;
                case CallState.SELECT_PHONE_ACCOUNT:
                    event = Log.Events.SET_SELECT_PHONE_ACCOUNT;
                    break;
                case CallState.RINGING:
                    event = Log.Events.SET_RINGING;
                    break;
            }
            if (event != null) {
                // The string data should be just the tag.
                String stringData = tag;
                if (data != null) {
                    // If data exists, add it to tag.  If no tag, just use data.toString().
                    stringData = stringData == null ? data.toString() : stringData + "> " + data;
                }
                Log.event(this, event, stringData);
            }
        }
    }

    void setRingbackRequested(boolean ringbackRequested) {
        mRingbackRequested = ringbackRequested;
        for (Listener l : mListeners) {
            l.onRingbackRequested(this, mRingbackRequested);
        }
    }

    boolean isRingbackRequested() {
        return mRingbackRequested;
    }

    boolean isConference() {
        return mIsConference;
    }

    public Uri getHandle() {
        return mHandle;
    }

    int getHandlePresentation() {
        return mHandlePresentation;
    }


    void setHandle(Uri handle) {
        setHandle(handle, TelecomManager.PRESENTATION_ALLOWED);
    }

    public void setHandle(Uri handle, int presentation) {
        if (!Objects.equals(handle, mHandle) || presentation != mHandlePresentation) {
            mHandlePresentation = presentation;
            if (mHandlePresentation == TelecomManager.PRESENTATION_RESTRICTED ||
                    mHandlePresentation == TelecomManager.PRESENTATION_UNKNOWN) {
                mHandle = null;
            } else {
                mHandle = handle;
                if (mHandle != null && !PhoneAccount.SCHEME_VOICEMAIL.equals(mHandle.getScheme())
                        && TextUtils.isEmpty(mHandle.getSchemeSpecificPart())) {
                    // If the number is actually empty, set it to null, unless this is a
                    // SCHEME_VOICEMAIL uri which always has an empty number.
                    mHandle = null;
                }
            }

            mIsEmergencyCall = mHandle != null && PhoneNumberUtils.isLocalEmergencyNumber(mContext,
                    mHandle.getSchemeSpecificPart());
            startCallerInfoLookup();
            for (Listener l : mListeners) {
                l.onHandleChanged(this);
            }
        }
    }

    String getCallerDisplayName() {
        return mCallerDisplayName;
    }

    int getCallerDisplayNamePresentation() {
        return mCallerDisplayNamePresentation;
    }

    void setCallerDisplayName(String callerDisplayName, int presentation) {
        if (!TextUtils.equals(callerDisplayName, mCallerDisplayName) ||
                presentation != mCallerDisplayNamePresentation) {
            mCallerDisplayName = callerDisplayName;
            mCallerDisplayNamePresentation = presentation;
            for (Listener l : mListeners) {
                l.onCallerDisplayNameChanged(this);
            }
        }
    }

    public String getName() {
        return mCallerInfo == null ? null : mCallerInfo.name;
    }

    public Bitmap getPhotoIcon() {
        return mCallerInfo == null ? null : mCallerInfo.cachedPhotoIcon;
    }

    public Drawable getPhoto() {
        return mCallerInfo == null ? null : mCallerInfo.cachedPhoto;
    }

    /**
     * @param disconnectCause The reason for the disconnection, represented by
     *         {@link android.telecom.DisconnectCause}.
     */
    public void setDisconnectCause(DisconnectCause disconnectCause) {
        // TODO: Consider combining this method with a setDisconnected() method that is totally
        // separate from setState.
        mDisconnectCause = disconnectCause;
    }

    public DisconnectCause getDisconnectCause() {
        return mDisconnectCause;
    }

    boolean isEmergencyCall() {
        return mIsEmergencyCall;
    }

    /**
     * @return The original handle this call is associated with. In-call services should use this
     * handle when indicating in their UI the handle that is being called.
     */
    public Uri getOriginalHandle() {
        if (mGatewayInfo != null && !mGatewayInfo.isEmpty()) {
            return mGatewayInfo.getOriginalAddress();
        }
        return getHandle();
    }

    GatewayInfo getGatewayInfo() {
        return mGatewayInfo;
    }

    void setGatewayInfo(GatewayInfo gatewayInfo) {
        mGatewayInfo = gatewayInfo;
    }

    PhoneAccountHandle getConnectionManagerPhoneAccount() {
        return mConnectionManagerPhoneAccountHandle;
    }

    void setConnectionManagerPhoneAccount(PhoneAccountHandle accountHandle) {
        if (!Objects.equals(mConnectionManagerPhoneAccountHandle, accountHandle)) {
            mConnectionManagerPhoneAccountHandle = accountHandle;
            for (Listener l : mListeners) {
                l.onConnectionManagerPhoneAccountChanged(this);
            }
        }

    }

    PhoneAccountHandle getTargetPhoneAccount() {
        return mTargetPhoneAccountHandle;
    }

    void setTargetPhoneAccount(PhoneAccountHandle accountHandle) {
        if (!Objects.equals(mTargetPhoneAccountHandle, accountHandle)) {
            mTargetPhoneAccountHandle = accountHandle;
            for (Listener l : mListeners) {
                l.onTargetPhoneAccountChanged(this);
            }
        }
    }

    boolean isIncoming() {
        return mIsIncoming;
    }

    /**
     * @return The "age" of this call object in milliseconds, which typically also represents the
     *     period since this call was added to the set pending outgoing calls, see
     *     mCreationTimeMillis.
     */
    long getAgeMillis() {
        if (mState == CallState.DISCONNECTED &&
                (mDisconnectCause.getCode() == DisconnectCause.REJECTED ||
                 mDisconnectCause.getCode() == DisconnectCause.MISSED)) {
            // Rejected and missed calls have no age. They're immortal!!
            return 0;
        } else if (mConnectTimeMillis == 0) {
            // Age is measured in the amount of time the call was active. A zero connect time
            // indicates that we never went active, so return 0 for the age.
            return 0;
        } else if (mDisconnectTimeMillis == 0) {
            // We connected, but have not yet disconnected
            return System.currentTimeMillis() - mConnectTimeMillis;
        }

        return mDisconnectTimeMillis - mConnectTimeMillis;
    }

    /**
     * @return The time when this call object was created and added to the set of pending outgoing
     *     calls.
     */
    public long getCreationTimeMillis() {
        return mCreationTimeMillis;
    }

    public void setCreationTimeMillis(long time) {
        mCreationTimeMillis = time;
    }

    long getConnectTimeMillis() {
        return mConnectTimeMillis;
    }

    int getConnectionCapabilities() {
        return mConnectionCapabilities;
    }

    void setConnectionCapabilities(int connectionCapabilities) {
        setConnectionCapabilities(connectionCapabilities, false /* forceUpdate */);
    }

    void setConnectionCapabilities(int connectionCapabilities, boolean forceUpdate) {
        Log.v(this, "setConnectionCapabilities: %s", Connection.capabilitiesToString(
                connectionCapabilities));
        if (forceUpdate || mConnectionCapabilities != connectionCapabilities) {
           mConnectionCapabilities = connectionCapabilities;
            for (Listener l : mListeners) {
                l.onConnectionCapabilitiesChanged(this);
            }
        }
    }

    Call getParentCall() {
        return mParentCall;
    }

    List<Call> getChildCalls() {
        return mChildCalls;
    }

    boolean wasConferencePreviouslyMerged() {
        return mWasConferencePreviouslyMerged;
    }

    Call getConferenceLevelActiveCall() {
        return mConferenceLevelActiveCall;
    }

    ConnectionServiceWrapper getConnectionService() {
        return mConnectionService;
    }

    /**
     * Retrieves the {@link Context} for the call.
     *
     * @return The {@link Context}.
     */
    Context getContext() {
        return mContext;
    }

    void setConnectionService(ConnectionServiceWrapper service) {
        Preconditions.checkNotNull(service);

        clearConnectionService();

        service.incrementAssociatedCallCount();
        mConnectionService = service;
        mConnectionService.addCall(this);
    }

    /**
     * Clears the associated connection service.
     */
    void clearConnectionService() {
        if (mConnectionService != null) {
            ConnectionServiceWrapper serviceTemp = mConnectionService;
            mConnectionService = null;
            serviceTemp.removeCall(this);

            // Decrementing the count can cause the service to unbind, which itself can trigger the
            // service-death code.  Since the service death code tries to clean up any associated
            // calls, we need to make sure to remove that information (e.g., removeCall()) before
            // we decrement. Technically, invoking removeCall() prior to decrementing is all that is
            // necessary, but cleaning up mConnectionService prior to triggering an unbind is good
            // to do.
            decrementAssociatedCallCount(serviceTemp);
        }
    }

    private void processDirectToVoicemail() {
        if (mDirectToVoicemailQueryPending) {
            if (mCallerInfo != null && mCallerInfo.shouldSendToVoicemail) {
                Log.i(this, "Directing call to voicemail: %s.", this);
                // TODO: Once we move State handling from CallsManager to Call, we
                // will not need to set STATE_RINGING state prior to calling reject.
                setState(CallState.RINGING, "directing to voicemail");
                reject(false, null);
            } else {
                // TODO: Make this class (not CallsManager) responsible for changing
                // the call state to STATE_RINGING.

                // TODO: Replace this with state transition to STATE_RINGING.
                for (Listener l : mListeners) {
                    l.onSuccessfulIncomingCall(this);
                }
            }

            mDirectToVoicemailQueryPending = false;
        }
    }

    /**
     * Starts the create connection sequence. Upon completion, there should exist an active
     * connection through a connection service (or the call will have failed).
     *
     * @param phoneAccountRegistrar The phone account registrar.
     */
    void startCreateConnection(PhoneAccountRegistrar phoneAccountRegistrar) {
        Preconditions.checkState(mCreateConnectionProcessor == null);
        mCreateConnectionProcessor = new CreateConnectionProcessor(this, mRepository, this,
                phoneAccountRegistrar, mContext);
        mCreateConnectionProcessor.process();
    }

    @Override
    public void handleCreateConnectionSuccess(
            CallIdMapper idMapper,
            ParcelableConnection connection) {
        Log.v(this, "handleCreateConnectionSuccessful %s", connection);
        setTargetPhoneAccount(connection.getPhoneAccount());
        setHandle(connection.getHandle(), connection.getHandlePresentation());
        setCallerDisplayName(
                connection.getCallerDisplayName(), connection.getCallerDisplayNamePresentation());
        setConnectionCapabilities(connection.getConnectionCapabilities());
        setVideoProvider(connection.getVideoProvider());
        setVideoState(connection.getVideoState());
        setRingbackRequested(connection.isRingbackRequested());
        setIsVoipAudioMode(connection.getIsVoipAudioMode());
        setStatusHints(connection.getStatusHints());
        setExtras(connection.getExtras());

        mConferenceableCalls.clear();
        for (String id : connection.getConferenceableConnectionIds()) {
            mConferenceableCalls.add(idMapper.getCall(id));
        }

        if (mIsUnknown) {
            for (Listener l : mListeners) {
                l.onSuccessfulUnknownCall(this, getStateFromConnectionState(connection.getState()));
            }
        } else if (mIsIncoming) {
            // We do not handle incoming calls immediately when they are verified by the connection
            // service. We allow the caller-info-query code to execute first so that we can read the
            // direct-to-voicemail property before deciding if we want to show the incoming call to
            // the user or if we want to reject the call.
            mDirectToVoicemailQueryPending = true;

            // Timeout the direct-to-voicemail lookup execution so that we dont wait too long before
            // showing the user the incoming call screen.
            mHandler.postDelayed(mDirectToVoicemailRunnable, Timeouts.getDirectToVoicemailMillis(
                    mContext.getContentResolver()));
        } else {
            for (Listener l : mListeners) {
                l.onSuccessfulOutgoingCall(this,
                        getStateFromConnectionState(connection.getState()));
            }
        }
    }

    @Override
    public void handleCreateConnectionFailure(DisconnectCause disconnectCause) {
        clearConnectionService();
        setDisconnectCause(disconnectCause);
        mCallsManager.markCallAsDisconnected(this, disconnectCause);

        if (mIsUnknown) {
            for (Listener listener : mListeners) {
                listener.onFailedUnknownCall(this);
            }
        } else if (mIsIncoming) {
            for (Listener listener : mListeners) {
                listener.onFailedIncomingCall(this);
            }
        } else {
            for (Listener listener : mListeners) {
                listener.onFailedOutgoingCall(this, disconnectCause);
            }
        }
    }

    /**
     * Plays the specified DTMF tone.
     */
    void playDtmfTone(char digit) {
        if (mConnectionService == null) {
            Log.w(this, "playDtmfTone() request on a call without a connection service.");
        } else {
            Log.i(this, "Send playDtmfTone to connection service for call %s", this);
            mConnectionService.playDtmfTone(this, digit);
            Log.event(this, Log.Events.START_DTMF, Log.pii(digit));
        }
    }

    /**
     * Stops playing any currently playing DTMF tone.
     */
    void stopDtmfTone() {
        if (mConnectionService == null) {
            Log.w(this, "stopDtmfTone() request on a call without a connection service.");
        } else {
            Log.i(this, "Send stopDtmfTone to connection service for call %s", this);
            Log.event(this, Log.Events.STOP_DTMF);
            mConnectionService.stopDtmfTone(this);
        }
    }

    void disconnect() {
        disconnect(false);
    }

    /**
     * Attempts to disconnect the call through the connection service.
     */
    void disconnect(boolean wasViaNewOutgoingCallBroadcaster) {
        Log.event(this, Log.Events.REQUEST_DISCONNECT);

        // Track that the call is now locally disconnecting.
        setLocallyDisconnecting(true);

        if (mState == CallState.NEW || mState == CallState.SELECT_PHONE_ACCOUNT ||
                mState == CallState.CONNECTING) {
            Log.v(this, "Aborting call %s", this);
            abort(wasViaNewOutgoingCallBroadcaster);
        } else if (mState != CallState.ABORTED && mState != CallState.DISCONNECTED) {
            if (mConnectionService == null) {
                Log.e(this, new Exception(), "disconnect() request on a call without a"
                        + " connection service.");
            } else {
                Log.i(this, "Send disconnect to connection service for call: %s", this);
                // The call isn't officially disconnected until the connection service
                // confirms that the call was actually disconnected. Only then is the
                // association between call and connection service severed, see
                // {@link CallsManager#markCallAsDisconnected}.
                mConnectionService.disconnect(this);
            }
        }
    }

    void abort(boolean wasViaNewOutgoingCallBroadcaster) {
        if (mCreateConnectionProcessor != null &&
                !mCreateConnectionProcessor.isProcessingComplete()) {
            mCreateConnectionProcessor.abort();
        } else if (mState == CallState.NEW || mState == CallState.SELECT_PHONE_ACCOUNT
                || mState == CallState.CONNECTING) {
            if (wasViaNewOutgoingCallBroadcaster) {
                // If the cancelation was from NEW_OUTGOING_CALL, then we do not automatically
                // destroy the call.  Instead, we announce the cancelation and CallsManager handles
                // it through a timer. Since apps often cancel calls through NEW_OUTGOING_CALL and
                // then re-dial them quickly using a gateway, allowing the first call to end
                // causes jank. This timeout allows CallsManager to transition the first call into
                // the second call so that in-call only ever sees a single call...eliminating the
                // jank altogether.
                for (Listener listener : mListeners) {
                    if (listener.onCanceledViaNewOutgoingCallBroadcast(this)) {
                        // The first listener to handle this wins. A return value of true means that
                        // the listener will handle the disconnection process later and so we
                        // should not continue it here.
                        setLocallyDisconnecting(false);
                        return;
                    }
                }
            }

            handleCreateConnectionFailure(new DisconnectCause(DisconnectCause.CANCELED));
        } else {
            Log.v(this, "Cannot abort a call which is neither SELECT_PHONE_ACCOUNT or CONNECTING");
        }
    }

    /**
     * Answers the call if it is ringing.
     *
     * @param videoState The video state in which to answer the call.
     */
    void answer(int videoState) {
        Preconditions.checkNotNull(mConnectionService);

        // Check to verify that the call is still in the ringing state. A call can change states
        // between the time the user hits 'answer' and Telecom receives the command.
        if (isRinging("answer")) {
            // At this point, we are asking the connection service to answer but we don't assume
            // that it will work. Instead, we wait until confirmation from the connectino service
            // that the call is in a non-STATE_RINGING state before changing the UI. See
            // {@link ConnectionServiceAdapter#setActive} and other set* methods.
            mConnectionService.answer(this, videoState);
            Log.event(this, Log.Events.REQUEST_ACCEPT);
        }
    }

    /**
     * Rejects the call if it is ringing.
     *
     * @param rejectWithMessage Whether to send a text message as part of the call rejection.
     * @param textMessage An optional text message to send as part of the rejection.
     */
    void reject(boolean rejectWithMessage, String textMessage) {
        Preconditions.checkNotNull(mConnectionService);

        // Check to verify that the call is still in the ringing state. A call can change states
        // between the time the user hits 'reject' and Telecomm receives the command.
        if (isRinging("reject")) {
            // Ensure video state history tracks video state at time of rejection.
            mVideoStateHistory |= mVideoState;

            mConnectionService.reject(this);
            Log.event(this, Log.Events.REQUEST_REJECT);
        }
    }

    /**
     * Puts the call on hold if it is currently active.
     */
    void hold() {
        Preconditions.checkNotNull(mConnectionService);

        if (mState == CallState.ACTIVE) {
            mConnectionService.hold(this);
            Log.event(this, Log.Events.REQUEST_HOLD);
        }
    }

    /**
     * Releases the call from hold if it is currently active.
     */
    void unhold() {
        Preconditions.checkNotNull(mConnectionService);

        if (mState == CallState.ON_HOLD) {
            mConnectionService.unhold(this);
            Log.event(this, Log.Events.REQUEST_UNHOLD);
        }
    }

    /** Checks if this is a live call or not. */
    boolean isAlive() {
        switch (mState) {
            case CallState.NEW:
            case CallState.RINGING:
            case CallState.DISCONNECTED:
            case CallState.ABORTED:
                return false;
            default:
                return true;
        }
    }

    boolean isActive() {
        return mState == CallState.ACTIVE;
    }

    Bundle getExtras() {
        return mExtras;
    }

    void setExtras(Bundle extras) {
        mExtras = extras;
        for (Listener l : mListeners) {
            l.onExtrasChanged(this);
        }
    }

    Bundle getIntentExtras() {
        return mIntentExtras;
    }

    void setIntentExtras(Bundle extras) {
        mIntentExtras = extras;
    }

    /**
     * @return the uri of the contact associated with this call.
     */
    Uri getContactUri() {
        if (mCallerInfo == null || !mCallerInfo.contactExists) {
            return getHandle();
        }
        return Contacts.getLookupUri(mCallerInfo.contactIdOrZero, mCallerInfo.lookupKey);
    }

    Uri getRingtone() {
        return mCallerInfo == null ? null : mCallerInfo.contactRingtoneUri;
    }

    void onPostDialWait(String remaining) {
        for (Listener l : mListeners) {
            l.onPostDialWait(this, remaining);
        }
    }

    void onPostDialChar(char nextChar) {
        for (Listener l : mListeners) {
            l.onPostDialChar(this, nextChar);
        }
    }

    void postDialContinue(boolean proceed) {
        mConnectionService.onPostDialContinue(this, proceed);
    }

    void conferenceWith(Call otherCall) {
        if (mConnectionService == null) {
            Log.w(this, "conference requested on a call without a connection service.");
        } else {
            Log.event(this, Log.Events.CONFERENCE_WITH, otherCall);
            mConnectionService.conference(this, otherCall);
        }
    }

    void splitFromConference() {
        if (mConnectionService == null) {
            Log.w(this, "splitting from conference call without a connection service");
        } else {
            Log.event(this, Log.Events.SPLIT_CONFERENCE);
            mConnectionService.splitFromConference(this);
        }
    }

    void mergeConference() {
        if (mConnectionService == null) {
            Log.w(this, "merging conference calls without a connection service.");
        } else if (can(Connection.CAPABILITY_MERGE_CONFERENCE)) {
            Log.event(this, Log.Events.CONFERENCE_WITH);
            mConnectionService.mergeConference(this);
            mWasConferencePreviouslyMerged = true;
        }
    }

    void swapConference() {
        if (mConnectionService == null) {
            Log.w(this, "swapping conference calls without a connection service.");
        } else if (can(Connection.CAPABILITY_SWAP_CONFERENCE)) {
            Log.event(this, Log.Events.SWAP);
            mConnectionService.swapConference(this);
            switch (mChildCalls.size()) {
                case 1:
                    mConferenceLevelActiveCall = mChildCalls.get(0);
                    break;
                case 2:
                    // swap
                    mConferenceLevelActiveCall = mChildCalls.get(0) == mConferenceLevelActiveCall ?
                            mChildCalls.get(1) : mChildCalls.get(0);
                    break;
                default:
                    // For anything else 0, or 3+, set it to null since it is impossible to tell.
                    mConferenceLevelActiveCall = null;
                    break;
            }
        }
    }

    void setParentCall(Call parentCall) {
        if (parentCall == this) {
            Log.e(this, new Exception(), "setting the parent to self");
            return;
        }
        if (parentCall == mParentCall) {
            // nothing to do
            return;
        }
        Preconditions.checkState(parentCall == null || mParentCall == null);

        Call oldParent = mParentCall;
        if (mParentCall != null) {
            mParentCall.removeChildCall(this);
        }
        mParentCall = parentCall;
        if (mParentCall != null) {
            mParentCall.addChildCall(this);
        }

        Log.event(this, Log.Events.SET_PARENT, mParentCall);
        for (Listener l : mListeners) {
            l.onParentChanged(this);
        }
    }

    void setConferenceableCalls(List<Call> conferenceableCalls) {
        mConferenceableCalls.clear();
        mConferenceableCalls.addAll(conferenceableCalls);

        for (Listener l : mListeners) {
            l.onConferenceableCallsChanged(this);
        }
    }

    List<Call> getConferenceableCalls() {
        return mConferenceableCalls;
    }

    boolean can(int capability) {
        return (mConnectionCapabilities & capability) == capability;
    }

    private void addChildCall(Call call) {
        if (!mChildCalls.contains(call)) {
            // Set the pseudo-active call to the latest child added to the conference.
            // See definition of mConferenceLevelActiveCall for more detail.
            mConferenceLevelActiveCall = call;
            mChildCalls.add(call);

            Log.event(this, Log.Events.ADD_CHILD, call);

            for (Listener l : mListeners) {
                l.onChildrenChanged(this);
            }
        }
    }

    private void removeChildCall(Call call) {
        if (mChildCalls.remove(call)) {
            Log.event(this, Log.Events.REMOVE_CHILD, call);
            for (Listener l : mListeners) {
                l.onChildrenChanged(this);
            }
        }
    }

    /**
     * Return whether the user can respond to this {@code Call} via an SMS message.
     *
     * @return true if the "Respond via SMS" feature should be enabled
     * for this incoming call.
     *
     * The general rule is that we *do* allow "Respond via SMS" except for
     * the few (relatively rare) cases where we know for sure it won't
     * work, namely:
     *   - a bogus or blank incoming number
     *   - a call from a SIP address
     *   - a "call presentation" that doesn't allow the number to be revealed
     *
     * In all other cases, we allow the user to respond via SMS.
     *
     * Note that this behavior isn't perfect; for example we have no way
     * to detect whether the incoming call is from a landline (with most
     * networks at least), so we still enable this feature even though
     * SMSes to that number will silently fail.
     */
    boolean isRespondViaSmsCapable() {
        if (mState != CallState.RINGING) {
            return false;
        }

        if (getHandle() == null) {
            // No incoming number known or call presentation is "PRESENTATION_RESTRICTED", in
            // other words, the user should not be able to see the incoming phone number.
            return false;
        }

        if (PhoneNumberUtils.isUriNumber(getHandle().toString())) {
            // The incoming number is actually a URI (i.e. a SIP address),
            // not a regular PSTN phone number, and we can't send SMSes to
            // SIP addresses.
            // (TODO: That might still be possible eventually, though. Is
            // there some SIP-specific equivalent to sending a text message?)
            return false;
        }

        // Is there a valid SMS application on the phone?
        if (SmsApplication.getDefaultRespondViaMessageApplication(mContext,
                true /*updateIfNeeded*/) == null) {
            return false;
        }

        // TODO: with some carriers (in certain countries) you *can* actually
        // tell whether a given number is a mobile phone or not. So in that
        // case we could potentially return false here if the incoming call is
        // from a land line.

        // If none of the above special cases apply, it's OK to enable the
        // "Respond via SMS" feature.
        return true;
    }

    List<String> getCannedSmsResponses() {
        return mCannedSmsResponses;
    }

    /**
     * We need to make sure that before we move a call to the disconnected state, it no
     * longer has any parent/child relationships.  We want to do this to ensure that the InCall
     * Service always has the right data in the right order.  We also want to do it in telecom so
     * that the insurance policy lives in the framework side of things.
     */
    private void fixParentAfterDisconnect() {
        setParentCall(null);
    }

    /**
     * @return True if the call is ringing, else logs the action name.
     */
    private boolean isRinging(String actionName) {
        if (mState == CallState.RINGING) {
            return true;
        }

        Log.i(this, "Request to %s a non-ringing call %s", actionName, this);
        return false;
    }

    @SuppressWarnings("rawtypes")
    private void decrementAssociatedCallCount(ServiceBinder binder) {
        if (binder != null) {
            binder.decrementAssociatedCallCount();
        }
    }

    /**
     * Looks up contact information based on the current handle.
     */
    private void startCallerInfoLookup() {
        final String number = mHandle == null ? null : mHandle.getSchemeSpecificPart();

        mQueryToken++;  // Updated so that previous queries can no longer set the information.
        mCallerInfo = null;
        if (!TextUtils.isEmpty(number)) {
            Log.v(this, "Looking up information for: %s.", Log.piiHandle(number));
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallerInfoAsyncQueryFactory.startQuery(
                            mQueryToken,
                            mContext,
                            number,
                            mCallerInfoQueryListener,
                            Call.this);
                }
            });
        }
    }

    /**
     * Saves the specified caller info if the specified token matches that of the last query
     * that was made.
     *
     * @param callerInfo The new caller information to set.
     * @param token The token used with this query.
     */
    private void setCallerInfo(CallerInfo callerInfo, int token) {
        Trace.beginSection("setCallerInfo");
        Preconditions.checkNotNull(callerInfo);

        if (mQueryToken == token) {
            mCallerInfo = callerInfo;
            Log.i(this, "CallerInfo received for %s: %s", Log.piiHandle(mHandle), callerInfo);

            if (mCallerInfo.contactDisplayPhotoUri != null) {
                Log.d(this, "Searching person uri %s for call %s",
                        mCallerInfo.contactDisplayPhotoUri, this);
                mContactsAsyncHelper.startObtainPhotoAsync(
                        token,
                        mContext,
                        mCallerInfo.contactDisplayPhotoUri,
                        mPhotoLoadListener,
                        this);
                // Do not call onCallerInfoChanged yet in this case.  We call it in setPhoto().
            } else {
                for (Listener l : mListeners) {
                    l.onCallerInfoChanged(this);
                }
            }

            processDirectToVoicemail();
        }
        Trace.endSection();
    }

    CallerInfo getCallerInfo() {
        return mCallerInfo;
    }

    /**
     * Saves the specified photo information if the specified token matches that of the last query.
     *
     * @param photo The photo as a drawable.
     * @param photoIcon The photo as a small icon.
     * @param token The token used with this query.
     */
    private void setPhoto(Drawable photo, Bitmap photoIcon, int token) {
        if (mQueryToken == token) {
            mCallerInfo.cachedPhoto = photo;
            mCallerInfo.cachedPhotoIcon = photoIcon;

            for (Listener l : mListeners) {
                l.onCallerInfoChanged(this);
            }
        }
    }

    private void maybeLoadCannedSmsResponses() {
        if (mIsIncoming && isRespondViaSmsCapable() && !mCannedSmsResponsesLoadingStarted) {
            Log.d(this, "maybeLoadCannedSmsResponses: starting task to load messages");
            mCannedSmsResponsesLoadingStarted = true;
            mCallsManager.getRespondViaSmsManager().loadCannedTextMessages(
                    new Response<Void, List<String>>() {
                        @Override
                        public void onResult(Void request, List<String>... result) {
                            if (result.length > 0) {
                                Log.d(this, "maybeLoadCannedSmsResponses: got %s", result[0]);
                                mCannedSmsResponses = result[0];
                                for (Listener l : mListeners) {
                                    l.onCannedSmsResponsesLoaded(Call.this);
                                }
                            }
                        }

                        @Override
                        public void onError(Void request, int code, String msg) {
                            Log.w(Call.this, "Error obtaining canned SMS responses: %d %s", code,
                                    msg);
                        }
                    },
                    mContext
            );
        } else {
            Log.d(this, "maybeLoadCannedSmsResponses: doing nothing");
        }
    }

    /**
     * Sets speakerphone option on when call begins.
     */
    public void setStartWithSpeakerphoneOn(boolean startWithSpeakerphone) {
        mSpeakerphoneOn = startWithSpeakerphone;
    }

    /**
     * Returns speakerphone option.
     *
     * @return Whether or not speakerphone should be set automatically when call begins.
     */
    public boolean getStartWithSpeakerphoneOn() {
        return mSpeakerphoneOn;
    }

    /**
     * Sets a video call provider for the call.
     */
    public void setVideoProvider(IVideoProvider videoProvider) {
        Log.v(this, "setVideoProvider");

        if (videoProvider != null ) {
            try {
                mVideoProviderProxy = new VideoProviderProxy(mLock, videoProvider, this);
            } catch (RemoteException ignored) {
                // Ignore RemoteException.
            }
        } else {
            mVideoProviderProxy = null;
        }

        mVideoProvider = videoProvider;

        for (Listener l : mListeners) {
            l.onVideoCallProviderChanged(Call.this);
        }
    }

    /**
     * @return The {@link Connection.VideoProvider} binder.
     */
    public IVideoProvider getVideoProvider() {
        if (mVideoProviderProxy == null) {
            return null;
        }

        return mVideoProviderProxy.getInterface();
    }

    /**
     * @return The {@link VideoProviderProxy} for this call.
     */
    public VideoProviderProxy getVideoProviderProxy() {
        return mVideoProviderProxy;
    }

    /**
     * The current video state for the call.
     * See {@link VideoProfile} for a list of valid video states.
     */
    public int getVideoState() {
        return mVideoState;
    }

    /**
     * Returns the video states which were applicable over the duration of a call.
     * See {@link VideoProfile} for a list of valid video states.
     *
     * @return The video states applicable over the duration of the call.
     */
    public int getVideoStateHistory() {
        return mVideoStateHistory;
    }

    /**
     * Determines the current video state for the call.
     * For an outgoing call determines the desired video state for the call.
     * Valid values: see {@link VideoProfile}
     *
     * @param videoState The video state for the call.
     */
    public void setVideoState(int videoState) {
        // Track which video states were applicable over the duration of the call.
        // Only track the call state when the call is active or disconnected.  This ensures we do
        // not include the video state when:
        // - Call is incoming (but not answered).
        // - Call it outgoing (but not answered).
        // We include the video state when disconnected to ensure that rejected calls reflect the
        // appropriate video state.
        if (isActive() || getState() == CallState.DISCONNECTED) {
            mVideoStateHistory = mVideoStateHistory | videoState;
        }

        mVideoState = videoState;
        for (Listener l : mListeners) {
            l.onVideoStateChanged(this);
        }
    }

    public boolean getIsVoipAudioMode() {
        return mIsVoipAudioMode;
    }

    public void setIsVoipAudioMode(boolean audioModeIsVoip) {
        mIsVoipAudioMode = audioModeIsVoip;
        for (Listener l : mListeners) {
            l.onIsVoipAudioModeChanged(this);
        }
    }

    public StatusHints getStatusHints() {
        return mStatusHints;
    }

    public void setStatusHints(StatusHints statusHints) {
        mStatusHints = statusHints;
        for (Listener l : mListeners) {
            l.onStatusHintsChanged(this);
        }
    }

    public boolean isUnknown() {
        return mIsUnknown;
    }

    public void setIsUnknown(boolean isUnknown) {
        mIsUnknown = isUnknown;
    }

    /**
     * Determines if this call is in a disconnecting state.
     *
     * @return {@code true} if this call is locally disconnecting.
     */
    public boolean isLocallyDisconnecting() {
        return mIsLocallyDisconnecting;
    }

    /**
     * Sets whether this call is in a disconnecting state.
     *
     * @param isLocallyDisconnecting {@code true} if this call is locally disconnecting.
     */
    private void setLocallyDisconnecting(boolean isLocallyDisconnecting) {
        mIsLocallyDisconnecting = isLocallyDisconnecting;
    }

    static int getStateFromConnectionState(int state) {
        switch (state) {
            case Connection.STATE_INITIALIZING:
                return CallState.CONNECTING;
            case Connection.STATE_ACTIVE:
                return CallState.ACTIVE;
            case Connection.STATE_DIALING:
                return CallState.DIALING;
            case Connection.STATE_DISCONNECTED:
                return CallState.DISCONNECTED;
            case Connection.STATE_HOLDING:
                return CallState.ON_HOLD;
            case Connection.STATE_NEW:
                return CallState.NEW;
            case Connection.STATE_RINGING:
                return CallState.RINGING;
        }
        return CallState.DISCONNECTED;
    }
}
