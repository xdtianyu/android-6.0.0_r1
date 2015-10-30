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

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.telecom.AudioState;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DefaultDialerManager;
import android.telecom.InCallService;
import android.telecom.ParcelableCall;
import android.telecom.TelecomManager;
import android.telecom.VideoCallImpl;
import android.util.ArrayMap;

// TODO: Needed for move to system service: import com.android.internal.R;
import com.android.internal.telecom.IInCallService;
import com.android.internal.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Binds to {@link IInCallService} and provides the service to {@link CallsManager} through which it
 * can send updates to the in-call app. This class is created and owned by CallsManager and retains
 * a binding to the {@link IInCallService} (implemented by the in-call app).
 */
public final class InCallController extends CallsManagerListenerBase {
    /**
     * Used to bind to the in-call app and triggers the start of communication between
     * this class and in-call app.
     */
    private class InCallServiceConnection implements ServiceConnection {
        /** {@inheritDoc} */
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(this, "onServiceConnected: %s", name);
            onConnected(name, service);
        }

        /** {@inheritDoc} */
        @Override public void onServiceDisconnected(ComponentName name) {
            Log.d(this, "onDisconnected: %s", name);
            onDisconnected(name);
        }
    }

    private final Call.Listener mCallListener = new Call.ListenerBase() {
        @Override
        public void onConnectionCapabilitiesChanged(Call call) {
            updateCall(call);
        }

        @Override
        public void onCannedSmsResponsesLoaded(Call call) {
            updateCall(call);
        }

        @Override
        public void onVideoCallProviderChanged(Call call) {
            updateCall(call, true /* videoProviderChanged */);
        }

        @Override
        public void onStatusHintsChanged(Call call) {
            updateCall(call);
        }

        @Override
        public void onExtrasChanged(Call call) {
            updateCall(call);
        }

        @Override
        public void onHandleChanged(Call call) {
            updateCall(call);
        }

        @Override
        public void onCallerDisplayNameChanged(Call call) {
            updateCall(call);
        }

        @Override
        public void onVideoStateChanged(Call call) {
            updateCall(call);
        }

        @Override
        public void onTargetPhoneAccountChanged(Call call) {
            updateCall(call);
        }

        @Override
        public void onConferenceableCallsChanged(Call call) {
            updateCall(call);
        }
    };

    /**
     * Maintains a binding connection to the in-call app(s).
     * ConcurrentHashMap constructor params: 8 is initial table size, 0.9f is
     * load factor before resizing, 1 means we only expect a single thread to
     * access the map so make only a single shard
     */
    private final Map<ComponentName, InCallServiceConnection> mServiceConnections =
            new ConcurrentHashMap<ComponentName, InCallServiceConnection>(8, 0.9f, 1);

    /** The in-call app implementations, see {@link IInCallService}. */
    private final Map<ComponentName, IInCallService> mInCallServices = new ArrayMap<>();

    /**
     * The {@link ComponentName} of the bound In-Call UI Service.
     */
    private ComponentName mInCallUIComponentName;

    private final CallIdMapper mCallIdMapper = new CallIdMapper("InCall");

    /** The {@link ComponentName} of the default InCall UI. */
    private final ComponentName mSystemInCallComponentName;

    private final Context mContext;
    private final TelecomSystem.SyncRoot mLock;
    private final CallsManager mCallsManager;

    public InCallController(
            Context context, TelecomSystem.SyncRoot lock, CallsManager callsManager) {
        mContext = context;
        mLock = lock;
        mCallsManager = callsManager;
        Resources resources = mContext.getResources();

        mSystemInCallComponentName = new ComponentName(
                resources.getString(R.string.ui_default_package),
                resources.getString(R.string.incall_default_class));
    }

    @Override
    public void onCallAdded(Call call) {
        if (!isBoundToServices()) {
            bindToServices(call);
        } else {
            adjustServiceBindingsForEmergency();

            Log.i(this, "onCallAdded: %s", call);
            // Track the call if we don't already know about it.
            addCall(call);

            for (Map.Entry<ComponentName, IInCallService> entry : mInCallServices.entrySet()) {
                ComponentName componentName = entry.getKey();
                IInCallService inCallService = entry.getValue();
                ParcelableCall parcelableCall = toParcelableCall(call,
                        true /* includeVideoProvider */);
                try {
                    inCallService.addCall(parcelableCall);
                } catch (RemoteException ignored) {
                }
            }
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        Log.i(this, "onCallRemoved: %s", call);
        if (mCallsManager.getCalls().isEmpty()) {
            /** Let's add a 2 second delay before we send unbind to the services to hopefully
             *  give them enough time to process all the pending messages.
             */
            Handler handler = new Handler(Looper.getMainLooper());
            final Runnable runnableUnbind = new Runnable() {
                @Override
                public void run() {
                    synchronized (mLock) {
                        // Check again to make sure there are no active calls.
                        if (mCallsManager.getCalls().isEmpty()) {
                            unbindFromServices();
                        }
                    }
                }
            };
            handler.postDelayed(
                    runnableUnbind,
                    Timeouts.getCallRemoveUnbindInCallServicesDelay(
                            mContext.getContentResolver()));
        }
        call.removeListener(mCallListener);
        mCallIdMapper.removeCall(call);
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        updateCall(call);
    }

    @Override
    public void onConnectionServiceChanged(
            Call call,
            ConnectionServiceWrapper oldService,
            ConnectionServiceWrapper newService) {
        updateCall(call);
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState oldCallAudioState,
            CallAudioState newCallAudioState) {
        if (!mInCallServices.isEmpty()) {
            Log.i(this, "Calling onAudioStateChanged, audioState: %s -> %s", oldCallAudioState,
                    newCallAudioState);
            for (IInCallService inCallService : mInCallServices.values()) {
                try {
                    inCallService.onCallAudioStateChanged(newCallAudioState);
                } catch (RemoteException ignored) {
                }
            }
        }
    }

    @Override
    public void onCanAddCallChanged(boolean canAddCall) {
        if (!mInCallServices.isEmpty()) {
            Log.i(this, "onCanAddCallChanged : %b", canAddCall);
            for (IInCallService inCallService : mInCallServices.values()) {
                try {
                    inCallService.onCanAddCallChanged(canAddCall);
                } catch (RemoteException ignored) {
                }
            }
        }
    }

    void onPostDialWait(Call call, String remaining) {
        if (!mInCallServices.isEmpty()) {
            Log.i(this, "Calling onPostDialWait, remaining = %s", remaining);
            for (IInCallService inCallService : mInCallServices.values()) {
                try {
                    inCallService.setPostDialWait(mCallIdMapper.getCallId(call), remaining);
                } catch (RemoteException ignored) {
                }
            }
        }
    }

    @Override
    public void onIsConferencedChanged(Call call) {
        Log.d(this, "onIsConferencedChanged %s", call);
        updateCall(call);
    }

    void bringToForeground(boolean showDialpad) {
        if (!mInCallServices.isEmpty()) {
            for (IInCallService inCallService : mInCallServices.values()) {
                try {
                    inCallService.bringToForeground(showDialpad);
                } catch (RemoteException ignored) {
                }
            }
        } else {
            Log.w(this, "Asking to bring unbound in-call UI to foreground.");
        }
    }

    /**
     * Unbinds an existing bound connection to the in-call app.
     */
    private void unbindFromServices() {
        Iterator<Map.Entry<ComponentName, InCallServiceConnection>> iterator =
            mServiceConnections.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<ComponentName, InCallServiceConnection> entry = iterator.next();
            Log.i(this, "Unbinding from InCallService %s", entry.getKey());
            try {
                mContext.unbindService(entry.getValue());
            } catch (Exception e) {
                Log.e(this, e, "Exception while unbinding from InCallService");
            }
            iterator.remove();
        }
        mInCallServices.clear();
    }

    /**
     * Binds to all the UI-providing InCallService as well as system-implemented non-UI
     * InCallServices. Method-invoker must check {@link #isBoundToServices()} before invoking.
     *
     * @param call The newly added call that triggered the binding to the in-call services.
     */
    private void bindToServices(Call call) {
        PackageManager packageManager = mContext.getPackageManager();
        Intent serviceIntent = new Intent(InCallService.SERVICE_INTERFACE);

        List<ComponentName> inCallControlServices = new ArrayList<>();
        ComponentName inCallUIService = null;

        for (ResolveInfo entry :
                packageManager.queryIntentServices(serviceIntent, PackageManager.GET_META_DATA)) {
            ServiceInfo serviceInfo = entry.serviceInfo;
            if (serviceInfo != null) {
                boolean hasServiceBindPermission = serviceInfo.permission != null &&
                        serviceInfo.permission.equals(
                                Manifest.permission.BIND_INCALL_SERVICE);
                if (!hasServiceBindPermission) {
                    Log.w(this, "InCallService does not have BIND_INCALL_SERVICE permission: " +
                            serviceInfo.packageName);
                    continue;
                }

                boolean hasControlInCallPermission = packageManager.checkPermission(
                        Manifest.permission.CONTROL_INCALL_EXPERIENCE,
                        serviceInfo.packageName) == PackageManager.PERMISSION_GRANTED;
                boolean isDefaultDialerPackage = Objects.equals(serviceInfo.packageName,
                        DefaultDialerManager.getDefaultDialerApplication(mContext));
                if (!hasControlInCallPermission && !isDefaultDialerPackage) {
                    Log.w(this, "Service does not have CONTROL_INCALL_EXPERIENCE permission: %s"
                            + " and is not system or default dialer.", serviceInfo.packageName);
                    continue;
                }

                boolean isUIService = serviceInfo.metaData != null &&
                        serviceInfo.metaData.getBoolean(
                                TelecomManager.METADATA_IN_CALL_SERVICE_UI, false);
                ComponentName componentName = new ComponentName(serviceInfo.packageName,
                        serviceInfo.name);
                if (isUIService) {
                    // For the main UI service, we always prefer the default dialer.
                    if (isDefaultDialerPackage) {
                        inCallUIService = componentName;
                        Log.i(this, "Found default-dialer's In-Call UI: %s", componentName);
                    }
                } else {
                    // for non-UI services that have passed our checks, add them to the list of
                    // service to bind to.
                    inCallControlServices.add(componentName);
                }

            }
        }

        // Attempt to bind to the default-dialer InCallService first.
        if (inCallUIService != null) {
            // skip default dialer if we have an emergency call or if it failed binding.
            if (mCallsManager.hasEmergencyCall()) {
                Log.i(this, "Skipping default-dialer because of emergency call");
                inCallUIService = null;
            } else if (!bindToInCallService(inCallUIService, call, "def-dialer")) {
                Log.event(call, Log.Events.ERROR_LOG,
                        "InCallService UI failed binding: " + inCallUIService);
                inCallUIService = null;
            }
        }

        if (inCallUIService == null) {
            // We failed to connect to the default-dialer service, or none was provided. Switch to
            // the system built-in InCallService UI.
            inCallUIService = mSystemInCallComponentName;
            if (!bindToInCallService(inCallUIService, call, "system")) {
                Log.event(call, Log.Events.ERROR_LOG,
                        "InCallService system UI failed binding: " + inCallUIService);
            }
        }
        mInCallUIComponentName = inCallUIService;

        // Bind to the control InCallServices
        for (ComponentName componentName : inCallControlServices) {
            bindToInCallService(componentName, call, "control");
        }
    }

    /**
     * Binds to the specified InCallService.
     */
    private boolean bindToInCallService(ComponentName componentName, Call call, String tag) {
        if (mInCallServices.containsKey(componentName)) {
            Log.i(this, "An InCallService already exists: %s", componentName);
            return true;
        }

        if (mServiceConnections.containsKey(componentName)) {
            Log.w(this, "The service is already bound for this component %s", componentName);
            return true;
        }

        Intent intent = new Intent(InCallService.SERVICE_INTERFACE);
        intent.setComponent(componentName);
        if (call != null && !call.isIncoming()){
            intent.putExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS,
                    call.getIntentExtras());
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    call.getTargetPhoneAccount());
        }

        Log.i(this, "Attempting to bind to [%s] InCall %s, with %s", tag, componentName, intent);
        InCallServiceConnection inCallServiceConnection = new InCallServiceConnection();
        if (mContext.bindServiceAsUser(intent, inCallServiceConnection,
                    Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE,
                    UserHandle.CURRENT)) {
            mServiceConnections.put(componentName, inCallServiceConnection);
            return true;
        }

        return false;
    }

    private void adjustServiceBindingsForEmergency() {
        if (!Objects.equals(mInCallUIComponentName, mSystemInCallComponentName)) {
            // The connected UI is not the system UI, so lets check if we should switch them
            // if there exists an emergency number.
            if (mCallsManager.hasEmergencyCall()) {
                // Lets fake a failure here in order to trigger the switch to the system UI.
                onInCallServiceFailure(mInCallUIComponentName, "emergency adjust");
            }
        }
    }

    /**
     * Persists the {@link IInCallService} instance and starts the communication between
     * this class and in-call app by sending the first update to in-call app. This method is
     * called after a successful binding connection is established.
     *
     * @param componentName The service {@link ComponentName}.
     * @param service The {@link IInCallService} implementation.
     */
    private void onConnected(ComponentName componentName, IBinder service) {
        Trace.beginSection("onConnected: " + componentName);
        Log.i(this, "onConnected to %s", componentName);

        IInCallService inCallService = IInCallService.Stub.asInterface(service);
        mInCallServices.put(componentName, inCallService);

        try {
            inCallService.setInCallAdapter(
                    new InCallAdapter(
                            mCallsManager,
                            mCallIdMapper,
                            mLock));
        } catch (RemoteException e) {
            Log.e(this, e, "Failed to set the in-call adapter.");
            Trace.endSection();
            onInCallServiceFailure(componentName, "setInCallAdapter");
            return;
        }

        // Upon successful connection, send the state of the world to the service.
        Collection<Call> calls = mCallsManager.getCalls();
        if (!calls.isEmpty()) {
            Log.i(this, "Adding %s calls to InCallService after onConnected: %s", calls.size(),
                    componentName);
            for (Call call : calls) {
                try {
                    // Track the call if we don't already know about it.
                    addCall(call);
                    inCallService.addCall(toParcelableCall(call, true /* includeVideoProvider */));
                } catch (RemoteException ignored) {
                }
            }
            onCallAudioStateChanged(
                    null,
                    mCallsManager.getAudioState());
            onCanAddCallChanged(mCallsManager.canAddCall());
        } else {
            unbindFromServices();
        }
        Trace.endSection();
    }

    /**
     * Cleans up an instance of in-call app after the service has been unbound.
     *
     * @param disconnectedComponent The {@link ComponentName} of the service which disconnected.
     */
    private void onDisconnected(ComponentName disconnectedComponent) {
        Log.i(this, "onDisconnected from %s", disconnectedComponent);

        mInCallServices.remove(disconnectedComponent);
        if (mServiceConnections.containsKey(disconnectedComponent)) {
            // One of the services that we were bound to has unexpectedly disconnected.
            onInCallServiceFailure(disconnectedComponent, "onDisconnect");
        }
    }

    /**
     * Handles non-recoverable failures by the InCallService. This method performs cleanup and
     * special handling when the failure is to the UI InCallService.
     */
    private void onInCallServiceFailure(ComponentName componentName, String tag) {
        Log.i(this, "Cleaning up a failed InCallService [%s]: %s", tag, componentName);

        // We always clean up the connections here. Even in the case where we rebind to the UI
        // because binding is count based and we could end up double-bound.
        mInCallServices.remove(componentName);
        InCallServiceConnection serviceConnection = mServiceConnections.remove(componentName);
        if (serviceConnection != null) {
            // We still need to call unbind even though it disconnected.
            mContext.unbindService(serviceConnection);
        }

        if (Objects.equals(mInCallUIComponentName, componentName)) {
            if (!mCallsManager.hasAnyCalls()) {
                // No calls are left anyway. Lets just disconnect all of them.
                unbindFromServices();
                return;
            }

            // Whenever the UI crashes, we automatically revert to the System UI for the
            // remainder of the active calls.
            mInCallUIComponentName = mSystemInCallComponentName;
            bindToInCallService(mInCallUIComponentName, null, "reconnecting");
        }
    }

    /**
     * Informs all {@link InCallService} instances of the updated call information.
     *
     * @param call The {@link Call}.
     */
    private void updateCall(Call call) {
        updateCall(call, false /* videoProviderChanged */);
    }

    /**
     * Informs all {@link InCallService} instances of the updated call information.
     *
     * @param call The {@link Call}.
     * @param videoProviderChanged {@code true} if the video provider changed, {@code false}
     *      otherwise.
     */
    private void updateCall(Call call, boolean videoProviderChanged) {
        if (!mInCallServices.isEmpty()) {
            ParcelableCall parcelableCall = toParcelableCall(call,
                    videoProviderChanged /* includeVideoProvider */);
            Log.i(this, "Sending updateCall %s ==> %s", call, parcelableCall);
            List<ComponentName> componentsUpdated = new ArrayList<>();
            for (Map.Entry<ComponentName, IInCallService> entry : mInCallServices.entrySet()) {
                ComponentName componentName = entry.getKey();
                IInCallService inCallService = entry.getValue();
                componentsUpdated.add(componentName);
                try {
                    inCallService.updateCall(parcelableCall);
                } catch (RemoteException ignored) {
                }
            }
            Log.i(this, "Components updated: %s", componentsUpdated);
        }
    }

    /**
     * Parcels all information for a {@link Call} into a new {@link ParcelableCall} instance.
     *
     * @param call The {@link Call} to parcel.
     * @param includeVideoProvider {@code true} if the video provider should be parcelled with the
     *      {@link Call}, {@code false} otherwise.  Since the {@link ParcelableCall#getVideoCall()}
     *      method creates a {@link VideoCallImpl} instance on access it is important for the
     *      recipient of the {@link ParcelableCall} to know if the video provider changed.
     * @return The {@link ParcelableCall} containing all call information from the {@link Call}.
     */
    private ParcelableCall toParcelableCall(Call call, boolean includeVideoProvider) {
        String callId = mCallIdMapper.getCallId(call);

        int state = getParcelableState(call);
        int capabilities = convertConnectionToCallCapabilities(call.getConnectionCapabilities());
        int properties = convertConnectionToCallProperties(call.getConnectionCapabilities());
        if (call.isConference()) {
            properties |= android.telecom.Call.Details.PROPERTY_CONFERENCE;
        }

        // If this is a single-SIM device, the "default SIM" will always be the only SIM.
        boolean isDefaultSmsAccount =
                mCallsManager.getPhoneAccountRegistrar()
                        .isUserSelectedSmsPhoneAccount(call.getTargetPhoneAccount());
        if (call.isRespondViaSmsCapable() && isDefaultSmsAccount) {
            capabilities |= android.telecom.Call.Details.CAPABILITY_RESPOND_VIA_TEXT;
        }

        if (call.isEmergencyCall()) {
            capabilities = removeCapability(
                    capabilities, android.telecom.Call.Details.CAPABILITY_MUTE);
        }

        if (state == android.telecom.Call.STATE_DIALING) {
            capabilities = removeCapability(capabilities,
                    android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL);
            capabilities = removeCapability(capabilities,
                    android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL);
        }

        String parentCallId = null;
        Call parentCall = call.getParentCall();
        if (parentCall != null) {
            parentCallId = mCallIdMapper.getCallId(parentCall);
        }

        long connectTimeMillis = call.getConnectTimeMillis();
        List<Call> childCalls = call.getChildCalls();
        List<String> childCallIds = new ArrayList<>();
        if (!childCalls.isEmpty()) {
            long childConnectTimeMillis = Long.MAX_VALUE;
            for (Call child : childCalls) {
                if (child.getConnectTimeMillis() > 0) {
                    childConnectTimeMillis = Math.min(child.getConnectTimeMillis(),
                            childConnectTimeMillis);
                }
                childCallIds.add(mCallIdMapper.getCallId(child));
            }

            if (childConnectTimeMillis != Long.MAX_VALUE) {
                connectTimeMillis = childConnectTimeMillis;
            }
        }

        Uri handle = call.getHandlePresentation() == TelecomManager.PRESENTATION_ALLOWED ?
                call.getHandle() : null;
        String callerDisplayName = call.getCallerDisplayNamePresentation() ==
                TelecomManager.PRESENTATION_ALLOWED ?  call.getCallerDisplayName() : null;

        List<Call> conferenceableCalls = call.getConferenceableCalls();
        List<String> conferenceableCallIds = new ArrayList<String>(conferenceableCalls.size());
        for (Call otherCall : conferenceableCalls) {
            String otherId = mCallIdMapper.getCallId(otherCall);
            if (otherId != null) {
                conferenceableCallIds.add(otherId);
            }
        }

        return new ParcelableCall(
                callId,
                state,
                call.getDisconnectCause(),
                call.getCannedSmsResponses(),
                capabilities,
                properties,
                connectTimeMillis,
                handle,
                call.getHandlePresentation(),
                callerDisplayName,
                call.getCallerDisplayNamePresentation(),
                call.getGatewayInfo(),
                call.getTargetPhoneAccount(),
                includeVideoProvider,
                includeVideoProvider ? call.getVideoProvider() : null,
                parentCallId,
                childCallIds,
                call.getStatusHints(),
                call.getVideoState(),
                conferenceableCallIds,
                call.getIntentExtras(),
                call.getExtras());
    }

    private static int getParcelableState(Call call) {
        int state = CallState.NEW;
        switch (call.getState()) {
            case CallState.ABORTED:
            case CallState.DISCONNECTED:
                state = android.telecom.Call.STATE_DISCONNECTED;
                break;
            case CallState.ACTIVE:
                state = android.telecom.Call.STATE_ACTIVE;
                break;
            case CallState.CONNECTING:
                state = android.telecom.Call.STATE_CONNECTING;
                break;
            case CallState.DIALING:
                state = android.telecom.Call.STATE_DIALING;
                break;
            case CallState.DISCONNECTING:
                state = android.telecom.Call.STATE_DISCONNECTING;
                break;
            case CallState.NEW:
                state = android.telecom.Call.STATE_NEW;
                break;
            case CallState.ON_HOLD:
                state = android.telecom.Call.STATE_HOLDING;
                break;
            case CallState.RINGING:
                state = android.telecom.Call.STATE_RINGING;
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                state = android.telecom.Call.STATE_SELECT_PHONE_ACCOUNT;
                break;
        }

        // If we are marked as 'locally disconnecting' then mark ourselves as disconnecting instead.
        // Unless we're disconnect*ED*, in which case leave it at that.
        if (call.isLocallyDisconnecting() &&
                (state != android.telecom.Call.STATE_DISCONNECTED)) {
            state = android.telecom.Call.STATE_DISCONNECTING;
        }
        return state;
    }

    private static final int[] CONNECTION_TO_CALL_CAPABILITY = new int[] {
        Connection.CAPABILITY_HOLD,
        android.telecom.Call.Details.CAPABILITY_HOLD,

        Connection.CAPABILITY_SUPPORT_HOLD,
        android.telecom.Call.Details.CAPABILITY_SUPPORT_HOLD,

        Connection.CAPABILITY_MERGE_CONFERENCE,
        android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE,

        Connection.CAPABILITY_SWAP_CONFERENCE,
        android.telecom.Call.Details.CAPABILITY_SWAP_CONFERENCE,

        Connection.CAPABILITY_RESPOND_VIA_TEXT,
        android.telecom.Call.Details.CAPABILITY_RESPOND_VIA_TEXT,

        Connection.CAPABILITY_MUTE,
        android.telecom.Call.Details.CAPABILITY_MUTE,

        Connection.CAPABILITY_MANAGE_CONFERENCE,
        android.telecom.Call.Details.CAPABILITY_MANAGE_CONFERENCE,

        Connection.CAPABILITY_SUPPORTS_VT_LOCAL_RX,
        android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_RX,

        Connection.CAPABILITY_SUPPORTS_VT_LOCAL_TX,
        android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_TX,

        Connection.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL,
        android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL,

        Connection.CAPABILITY_SUPPORTS_VT_REMOTE_RX,
        android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_RX,

        Connection.CAPABILITY_SUPPORTS_VT_REMOTE_TX,
        android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_TX,

        Connection.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL,
        android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL,

        Connection.CAPABILITY_SEPARATE_FROM_CONFERENCE,
        android.telecom.Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE,

        Connection.CAPABILITY_DISCONNECT_FROM_CONFERENCE,
        android.telecom.Call.Details.CAPABILITY_DISCONNECT_FROM_CONFERENCE,

        Connection.CAPABILITY_CAN_UPGRADE_TO_VIDEO,
        android.telecom.Call.Details.CAPABILITY_CAN_UPGRADE_TO_VIDEO,

        Connection.CAPABILITY_CAN_PAUSE_VIDEO,
        android.telecom.Call.Details.CAPABILITY_CAN_PAUSE_VIDEO
    };

    private static int convertConnectionToCallCapabilities(int connectionCapabilities) {
        int callCapabilities = 0;
        for (int i = 0; i < CONNECTION_TO_CALL_CAPABILITY.length; i += 2) {
            if ((CONNECTION_TO_CALL_CAPABILITY[i] & connectionCapabilities) != 0) {
                callCapabilities |= CONNECTION_TO_CALL_CAPABILITY[i + 1];
            }
        }
        return callCapabilities;
    }

    private static final int[] CONNECTION_TO_CALL_PROPERTIES = new int[] {
        Connection.CAPABILITY_HIGH_DEF_AUDIO,
        android.telecom.Call.Details.PROPERTY_HIGH_DEF_AUDIO,

        Connection.CAPABILITY_WIFI,
        android.telecom.Call.Details.PROPERTY_WIFI,

        Connection.CAPABILITY_GENERIC_CONFERENCE,
        android.telecom.Call.Details.PROPERTY_GENERIC_CONFERENCE,

        Connection.CAPABILITY_SHOW_CALLBACK_NUMBER,
        android.telecom.Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE,
    };

    private static int convertConnectionToCallProperties(int connectionCapabilities) {
        int callProperties = 0;
        for (int i = 0; i < CONNECTION_TO_CALL_PROPERTIES.length; i += 2) {
            if ((CONNECTION_TO_CALL_PROPERTIES[i] & connectionCapabilities) != 0) {
                callProperties |= CONNECTION_TO_CALL_PROPERTIES[i + 1];
            }
        }
        return callProperties;
    }

    /**
     * Adds the call to the list of calls tracked by the {@link InCallController}.
     * @param call The call to add.
     */
    private void addCall(Call call) {
        if (mCallIdMapper.getCallId(call) == null) {
            mCallIdMapper.addCall(call);
            call.addListener(mCallListener);
        }
    }

    private boolean isBoundToServices() {
        return !mInCallServices.isEmpty();
    }

    /**
     * Removes the specified capability from the set of capabilities bits and returns the new set.
     */
    private static int removeCapability(int capabilities, int capability) {
        return capabilities & ~capability;
    }

    /**
     * Dumps the state of the {@link InCallController}.
     *
     * @param pw The {@code IndentingPrintWriter} to write the state to.
     */
    public void dump(IndentingPrintWriter pw) {
        pw.println("mInCallServices (InCalls registered):");
        pw.increaseIndent();
        for (ComponentName componentName : mInCallServices.keySet()) {
            pw.println(componentName);
        }
        pw.decreaseIndent();

        pw.println("mServiceConnections (InCalls bound):");
        pw.increaseIndent();
        for (ComponentName componentName : mServiceConnections.keySet()) {
            pw.println(componentName);
        }
        pw.decreaseIndent();
    }
}
