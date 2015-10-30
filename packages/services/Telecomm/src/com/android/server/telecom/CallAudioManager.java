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
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.CallAudioState;

import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * This class manages audio modes, streams and other properties.
 */
final class CallAudioManager extends CallsManagerListenerBase
        implements WiredHeadsetManager.Listener, DockManager.Listener {
    private static final int STREAM_NONE = -1;

    private static final String STREAM_DESCRIPTION_NONE = "STEAM_NONE";
    private static final String STREAM_DESCRIPTION_ALARM = "STEAM_ALARM";
    private static final String STREAM_DESCRIPTION_BLUETOOTH_SCO = "STREAM_BLUETOOTH_SCO";
    private static final String STREAM_DESCRIPTION_DTMF = "STREAM_DTMF";
    private static final String STREAM_DESCRIPTION_MUSIC = "STREAM_MUSIC";
    private static final String STREAM_DESCRIPTION_NOTIFICATION = "STREAM_NOTIFICATION";
    private static final String STREAM_DESCRIPTION_RING = "STREAM_RING";
    private static final String STREAM_DESCRIPTION_SYSTEM = "STREAM_SYSTEM";
    private static final String STREAM_DESCRIPTION_VOICE_CALL = "STREAM_VOICE_CALL";

    private static final String MODE_DESCRIPTION_INVALID = "MODE_INVALID";
    private static final String MODE_DESCRIPTION_CURRENT = "MODE_CURRENT";
    private static final String MODE_DESCRIPTION_NORMAL = "MODE_NORMAL";
    private static final String MODE_DESCRIPTION_RINGTONE = "MODE_RINGTONE";
    private static final String MODE_DESCRIPTION_IN_CALL = "MODE_IN_CALL";
    private static final String MODE_DESCRIPTION_IN_COMMUNICATION = "MODE_IN_COMMUNICATION";

    private static final int MSG_AUDIO_MANAGER_INITIALIZE = 0;
    private static final int MSG_AUDIO_MANAGER_TURN_ON_SPEAKER = 1;
    private static final int MSG_AUDIO_MANAGER_ABANDON_AUDIO_FOCUS_FOR_CALL = 2;
    private static final int MSG_AUDIO_MANAGER_SET_MICROPHONE_MUTE = 3;
    private static final int MSG_AUDIO_MANAGER_REQUEST_AUDIO_FOCUS_FOR_CALL = 4;
    private static final int MSG_AUDIO_MANAGER_SET_MODE = 5;

    private final Handler mAudioManagerHandler = new Handler(Looper.getMainLooper()) {

        private AudioManager mAudioManager;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AUDIO_MANAGER_INITIALIZE: {
                    mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    break;
                }
                case MSG_AUDIO_MANAGER_TURN_ON_SPEAKER: {
                    boolean on = (msg.arg1 != 0);
                    // Wired headset and earpiece work the same way
                    if (mAudioManager.isSpeakerphoneOn() != on) {
                        Log.i(this, "turning speaker phone %s", on);
                        mAudioManager.setSpeakerphoneOn(on);
                    }
                    break;
                }
                case MSG_AUDIO_MANAGER_ABANDON_AUDIO_FOCUS_FOR_CALL: {
                    mAudioManager.abandonAudioFocusForCall();
                    break;
                }
                case MSG_AUDIO_MANAGER_SET_MICROPHONE_MUTE: {
                    boolean mute = (msg.arg1 != 0);
                    if (mute != mAudioManager.isMicrophoneMute()) {
                        Log.i(this, "changing microphone mute state to: %b", mute);
                        mAudioManager.setMicrophoneMute(mute);
                    }
                    break;
                }
                case MSG_AUDIO_MANAGER_REQUEST_AUDIO_FOCUS_FOR_CALL: {
                    int stream = msg.arg1;
                    mAudioManager.requestAudioFocusForCall(
                            stream,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    break;
                }
                case MSG_AUDIO_MANAGER_SET_MODE: {
                    int newMode = msg.arg1;
                    int oldMode = mAudioManager.getMode();
                    Log.v(this, "Request to change audio mode from %s to %s", modeToString(oldMode),
                            modeToString(newMode));

                    if (oldMode != newMode) {
                        if (oldMode == AudioManager.MODE_IN_CALL &&
                                newMode == AudioManager.MODE_RINGTONE) {
                            Log.i(this, "Transition from IN_CALL -> RINGTONE."
                                    + "  Resetting to NORMAL first.");
                            mAudioManager.setMode(AudioManager.MODE_NORMAL);
                        }
                        mAudioManager.setMode(newMode);
                        synchronized (mLock) {
                            mMostRecentlyUsedMode = newMode;
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    private final Context mContext;
    private final TelecomSystem.SyncRoot mLock;
    private final StatusBarNotifier mStatusBarNotifier;
    private final BluetoothManager mBluetoothManager;
    private final WiredHeadsetManager mWiredHeadsetManager;
    private final DockManager mDockManager;
    private final CallsManager mCallsManager;

    private CallAudioState mCallAudioState;
    private int mAudioFocusStreamType;
    private boolean mIsRinging;
    private boolean mIsTonePlaying;
    private boolean mWasSpeakerOn;
    private int mMostRecentlyUsedMode = AudioManager.MODE_IN_CALL;
    private Call mCallToSpeedUpMTAudio = null;

    CallAudioManager(
            Context context,
            TelecomSystem.SyncRoot lock,
            StatusBarNotifier statusBarNotifier,
            WiredHeadsetManager wiredHeadsetManager,
            DockManager dockManager,
            CallsManager callsManager) {
        mContext = context;
        mLock = lock;
        mAudioManagerHandler.obtainMessage(MSG_AUDIO_MANAGER_INITIALIZE, 0, 0).sendToTarget();
        mStatusBarNotifier = statusBarNotifier;
        mBluetoothManager = new BluetoothManager(context, this);
        mWiredHeadsetManager = wiredHeadsetManager;
        mCallsManager = callsManager;

        mWiredHeadsetManager.addListener(this);
        mDockManager = dockManager;
        mDockManager.addListener(this);

        saveAudioState(getInitialAudioState(null));
        mAudioFocusStreamType = STREAM_NONE;
    }

    CallAudioState getCallAudioState() {
        return mCallAudioState;
    }

    @Override
    public void onCallAdded(Call call) {
        Log.v(this, "onCallAdded");
        onCallUpdated(call);

        if (hasFocus() && getForegroundCall() == call) {
            if (!call.isIncoming()) {
                // Unmute new outgoing call.
                setSystemAudioState(false, mCallAudioState.getRoute(),
                        mCallAudioState.getSupportedRouteMask());
            }
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        Log.v(this, "onCallRemoved");
        // If we didn't already have focus, there's nothing to do.
        if (hasFocus()) {
            if (mCallsManager.getCalls().isEmpty()) {
                Log.v(this, "all calls removed, resetting system audio to default state");
                setInitialAudioState(null, false /* force */);
                mWasSpeakerOn = false;
            }
            updateAudioStreamAndMode(call);
        }
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        Log.v(this, "onCallStateChanged : oldState = %d, newState = %d", oldState, newState);
        onCallUpdated(call);
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        Log.v(this, "onIncomingCallAnswered");
        int route = mCallAudioState.getRoute();

        // We do two things:
        // (1) If this is the first call, then we can to turn on bluetooth if available.
        // (2) Unmute the audio for the new incoming call.
        boolean isOnlyCall = mCallsManager.getCalls().size() == 1;
        if (isOnlyCall && mBluetoothManager.isBluetoothAvailable()) {
            mBluetoothManager.connectBluetoothAudio();
            route = CallAudioState.ROUTE_BLUETOOTH;
        }

        setSystemAudioState(false /* isMute */, route, mCallAudioState.getSupportedRouteMask());

        if (call.can(android.telecom.Call.Details.CAPABILITY_SPEED_UP_MT_AUDIO)) {
            Log.v(this, "Speed up audio setup for IMS MT call.");
            mCallToSpeedUpMTAudio = call;
            updateAudioStreamAndMode(call);
        }
    }

    @Override
    public void onForegroundCallChanged(Call oldForegroundCall, Call newForegroundCall) {
        onCallUpdated(newForegroundCall);
        // Ensure that the foreground call knows about the latest audio state.
        updateAudioForForegroundCall();
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        updateAudioStreamAndMode(call);
    }

    /**
      * Updates the audio route when the headset plugged in state changes. For example, if audio is
      * being routed over speakerphone and a headset is plugged in then switch to wired headset.
      */
    @Override
    public void onWiredHeadsetPluggedInChanged(boolean oldIsPluggedIn, boolean newIsPluggedIn) {
        // This can happen even when there are no calls and we don't have focus.
        if (!hasFocus()) {
            return;
        }

        boolean isCurrentlyWiredHeadset = mCallAudioState.getRoute()
                == CallAudioState.ROUTE_WIRED_HEADSET;

        int newRoute = mCallAudioState.getRoute();  // start out with existing route
        if (newIsPluggedIn) {
            newRoute = CallAudioState.ROUTE_WIRED_HEADSET;
        } else if (isCurrentlyWiredHeadset) {
            Call call = getForegroundCall();
            boolean hasLiveCall = call != null && call.isAlive();

            if (hasLiveCall) {
                // In order of preference when a wireless headset is unplugged.
                if (mWasSpeakerOn) {
                    newRoute = CallAudioState.ROUTE_SPEAKER;
                } else {
                    newRoute = CallAudioState.ROUTE_EARPIECE;
                }

                // We don't automatically connect to bluetooth when user unplugs their wired headset
                // and they were previously using the wired. Wired and earpiece are effectively the
                // same choice in that they replace each other as an option when wired headsets
                // are plugged in and out. This means that keeping it earpiece is a bit more
                // consistent with the status quo.  Bluetooth also has more danger associated with
                // choosing it in the wrong curcumstance because bluetooth devices can be
                // semi-public (like in a very-occupied car) where earpiece doesn't carry that risk.
            }
        }

        // We need to call this every time even if we do not change the route because the supported
        // routes changed either to include or not include WIRED_HEADSET.
        setSystemAudioState(mCallAudioState.isMuted(), newRoute, calculateSupportedRoutes());
    }

    @Override
    public void onDockChanged(boolean isDocked) {
        // This can happen even when there are no calls and we don't have focus.
        if (!hasFocus()) {
            return;
        }

        if (isDocked) {
            // Device just docked, turn to speakerphone. Only do so if the route is currently
            // earpiece so that we dont switch out of a BT headset or a wired headset.
            if (mCallAudioState.getRoute() == CallAudioState.ROUTE_EARPIECE) {
                setAudioRoute(CallAudioState.ROUTE_SPEAKER);
            }
        } else {
            // Device just undocked, remove from speakerphone if possible.
            if (mCallAudioState.getRoute() == CallAudioState.ROUTE_SPEAKER) {
                setAudioRoute(CallAudioState.ROUTE_WIRED_OR_EARPIECE);
            }
        }
    }

    void toggleMute() {
        mute(!mCallAudioState.isMuted());
    }

    void mute(boolean shouldMute) {
        if (!hasFocus()) {
            return;
        }

        Log.v(this, "mute, shouldMute: %b", shouldMute);

        // Don't mute if there are any emergency calls.
        if (mCallsManager.hasEmergencyCall()) {
            shouldMute = false;
            Log.v(this, "ignoring mute for emergency call");
        }

        if (mCallAudioState.isMuted() != shouldMute) {
            // We user CallsManager's foreground call so that we dont ignore ringing calls
            // for logging purposes
            Log.event(mCallsManager.getForegroundCall(), Log.Events.MUTE,
                    shouldMute ? "on" : "off");

            setSystemAudioState(shouldMute, mCallAudioState.getRoute(),
                    mCallAudioState.getSupportedRouteMask());
        }
    }

    /**
     * Changed the audio route, for example from earpiece to speaker phone.
     *
     * @param route The new audio route to use. See {@link CallAudioState}.
     */
    void setAudioRoute(int route) {
        // This can happen even when there are no calls and we don't have focus.
        if (!hasFocus()) {
            return;
        }

        Log.v(this, "setAudioRoute, route: %s", CallAudioState.audioRouteToString(route));

        // Change ROUTE_WIRED_OR_EARPIECE to a single entry.
        int newRoute = selectWiredOrEarpiece(route, mCallAudioState.getSupportedRouteMask());

        // If route is unsupported, do nothing.
        if ((mCallAudioState.getSupportedRouteMask() | newRoute) == 0) {
            Log.wtf(this, "Asking to set to a route that is unsupported: %d", newRoute);
            return;
        }

        if (mCallAudioState.getRoute() != newRoute) {
            // Remember the new speaker state so it can be restored when the user plugs and unplugs
            // a headset.
            mWasSpeakerOn = newRoute == CallAudioState.ROUTE_SPEAKER;
            setSystemAudioState(mCallAudioState.isMuted(), newRoute,
                    mCallAudioState.getSupportedRouteMask());
        }
    }

    /**
     * Sets the audio stream and mode based on whether a call is ringing.
     *
     * @param call The call which changed ringing state.
     * @param isRinging {@code true} if the call is ringing, {@code false} otherwise.
     */
    void setIsRinging(Call call, boolean isRinging) {
        if (mIsRinging != isRinging) {
            Log.i(this, "setIsRinging %b -> %b (call = %s)", mIsRinging, isRinging, call);
            mIsRinging = isRinging;
            updateAudioStreamAndMode(call);
        }
    }

    /**
     * Sets the tone playing status. Some tones can play even when there are no live calls and this
     * status indicates that we should keep audio focus even for tones that play beyond the life of
     * calls.
     *
     * @param isPlayingNew The status to set.
     */
    void setIsTonePlaying(boolean isPlayingNew) {
        if (mIsTonePlaying != isPlayingNew) {
            Log.v(this, "mIsTonePlaying %b -> %b.", mIsTonePlaying, isPlayingNew);
            mIsTonePlaying = isPlayingNew;
            updateAudioStreamAndMode();
        }
    }

    /**
     * Updates the audio routing according to the bluetooth state.
     */
    void onBluetoothStateChange(BluetoothManager bluetoothManager) {
        // This can happen even when there are no calls and we don't have focus.
        if (!hasFocus()) {
            return;
        }

        int supportedRoutes = calculateSupportedRoutes();
        int newRoute = mCallAudioState.getRoute();
        if (bluetoothManager.isBluetoothAudioConnectedOrPending()) {
            newRoute = CallAudioState.ROUTE_BLUETOOTH;
        } else if (mCallAudioState.getRoute() == CallAudioState.ROUTE_BLUETOOTH) {
            newRoute = selectWiredOrEarpiece(CallAudioState.ROUTE_WIRED_OR_EARPIECE,
                    supportedRoutes);
            // Do not switch to speaker when bluetooth disconnects.
            mWasSpeakerOn = false;
        }

        setSystemAudioState(mCallAudioState.isMuted(), newRoute, supportedRoutes);
    }

    boolean isBluetoothAudioOn() {
        return mBluetoothManager.isBluetoothAudioConnected();
    }

    boolean isBluetoothDeviceAvailable() {
        return mBluetoothManager.isBluetoothAvailable();
    }

    private void saveAudioState(CallAudioState callAudioState) {
        mCallAudioState = callAudioState;
        mStatusBarNotifier.notifyMute(mCallAudioState.isMuted());
        mStatusBarNotifier.notifySpeakerphone(mCallAudioState.getRoute()
                == CallAudioState.ROUTE_SPEAKER);
    }

    private void onCallUpdated(Call call) {
        updateAudioStreamAndMode(call);
        if (call != null && call.getState() == CallState.ACTIVE &&
                            call == mCallToSpeedUpMTAudio) {
            mCallToSpeedUpMTAudio = null;
        }
    }

    private void setSystemAudioState(boolean isMuted, int route, int supportedRouteMask) {
        setSystemAudioState(false /* force */, isMuted, route, supportedRouteMask);
    }

    private void setSystemAudioState(
            boolean force, boolean isMuted, int route, int supportedRouteMask) {
        if (!hasFocus()) {
            return;
        }

        CallAudioState oldAudioState = mCallAudioState;
        saveAudioState(new CallAudioState(isMuted, route, supportedRouteMask));
        if (!force && Objects.equals(oldAudioState, mCallAudioState)) {
            return;
        }

        Log.i(this, "setSystemAudioState: changing from %s to %s", oldAudioState, mCallAudioState);
        Log.event(mCallsManager.getForegroundCall(), Log.Events.AUDIO_ROUTE,
                CallAudioState.audioRouteToString(mCallAudioState.getRoute()));

        mAudioManagerHandler.obtainMessage(
                MSG_AUDIO_MANAGER_SET_MICROPHONE_MUTE,
                mCallAudioState.isMuted() ? 1 : 0,
                0)
                .sendToTarget();

        // Audio route.
        if (mCallAudioState.getRoute() == CallAudioState.ROUTE_BLUETOOTH) {
            turnOnSpeaker(false);
            turnOnBluetooth(true);
        } else if (mCallAudioState.getRoute() == CallAudioState.ROUTE_SPEAKER) {
            turnOnBluetooth(false);
            turnOnSpeaker(true);
        } else if (mCallAudioState.getRoute() == CallAudioState.ROUTE_EARPIECE ||
                mCallAudioState.getRoute() == CallAudioState.ROUTE_WIRED_HEADSET) {
            turnOnBluetooth(false);
            turnOnSpeaker(false);
        }

        if (!oldAudioState.equals(mCallAudioState)) {
            mCallsManager.onCallAudioStateChanged(oldAudioState, mCallAudioState);
            updateAudioForForegroundCall();
        }
    }

    private void turnOnSpeaker(boolean on) {
        mAudioManagerHandler.obtainMessage(MSG_AUDIO_MANAGER_TURN_ON_SPEAKER, on ? 1 : 0, 0)
                .sendToTarget();
    }

    private void turnOnBluetooth(boolean on) {
        if (mBluetoothManager.isBluetoothAvailable()) {
            boolean isAlreadyOn = mBluetoothManager.isBluetoothAudioConnectedOrPending();
            if (on != isAlreadyOn) {
                Log.i(this, "connecting bluetooth %s", on);
                if (on) {
                    mBluetoothManager.connectBluetoothAudio();
                } else {
                    mBluetoothManager.disconnectBluetoothAudio();
                }
            }
        }
    }

    private void updateAudioStreamAndMode() {
        updateAudioStreamAndMode(null /* call */);
    }

    private void updateAudioStreamAndMode(Call callToUpdate) {
        Log.i(this, "updateAudioStreamAndMode :  mIsRinging: %b, mIsTonePlaying: %b, call: %s",
                mIsRinging, mIsTonePlaying, callToUpdate);

        boolean wasVoiceCall = mAudioFocusStreamType == AudioManager.STREAM_VOICE_CALL;
        if (mIsRinging) {
            Log.i(this, "updateAudioStreamAndMode : ringing");
            requestAudioFocusAndSetMode(AudioManager.STREAM_RING, AudioManager.MODE_RINGTONE);
        } else {
            Call foregroundCall = getForegroundCall();
            Call waitingForAccountSelectionCall = mCallsManager
                    .getFirstCallWithState(CallState.SELECT_PHONE_ACCOUNT);
            Call call = mCallsManager.getForegroundCall();
            if (foregroundCall == null && call != null && call == mCallToSpeedUpMTAudio) {
                Log.v(this, "updateAudioStreamAndMode : no foreground, speeding up MT audio.");
                requestAudioFocusAndSetMode(AudioManager.STREAM_VOICE_CALL,
                                                         AudioManager.MODE_IN_CALL);
            } else if (foregroundCall != null && waitingForAccountSelectionCall == null) {
                // In the case where there is a call that is waiting for account selection,
                // this will fall back to abandonAudioFocus() below, which temporarily exits
                // the in-call audio mode. This is to allow TalkBack to speak the "Call with"
                // dialog information at media volume as opposed to through the earpiece.
                // Once exiting the "Call with" dialog, the audio focus will return to an in-call
                // audio mode when this method (updateAudioStreamAndMode) is called again.
                int mode = foregroundCall.getIsVoipAudioMode() ?
                        AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_IN_CALL;
                Log.v(this, "updateAudioStreamAndMode : foreground");
                requestAudioFocusAndSetMode(AudioManager.STREAM_VOICE_CALL, mode);
            } else if (mIsTonePlaying) {
                // There is no call, however, we are still playing a tone, so keep focus.
                // Since there is no call from which to determine the mode, use the most
                // recently used mode instead.
                Log.v(this, "updateAudioStreamAndMode : tone playing");
                requestAudioFocusAndSetMode(
                        AudioManager.STREAM_VOICE_CALL, mMostRecentlyUsedMode);
            } else if (!hasRingingForegroundCall()) {
                Log.v(this, "updateAudioStreamAndMode : no ringing call");
                abandonAudioFocus();
            } else {
                // mIsRinging is false, but there is a foreground ringing call present. Don't
                // abandon audio focus immediately to prevent audio focus from getting lost between
                // the time it takes for the foreground call to transition from RINGING to ACTIVE/
                // DISCONNECTED. When the call eventually transitions to the next state, audio
                // focus will be correctly abandoned by the if clause above.
            }
        }

        boolean isVoiceCall = mAudioFocusStreamType == AudioManager.STREAM_VOICE_CALL;

        // If we transition from not a voice call to a voice call, we need to set an initial audio
        // state for the call.
        if (!wasVoiceCall && isVoiceCall) {
            setInitialAudioState(callToUpdate, true /* force */);
        }
    }

    private void requestAudioFocusAndSetMode(int stream, int mode) {
        Log.v(this, "requestAudioFocusAndSetMode : stream: %s -> %s, mode: %s",
                streamTypeToString(mAudioFocusStreamType), streamTypeToString(stream),
                modeToString(mode));
        Preconditions.checkState(stream != STREAM_NONE);

        // Even if we already have focus, if the stream is different we update audio manager to give
        // it a hint about the purpose of our focus.
        if (mAudioFocusStreamType != stream) {
            Log.i(this, "requestAudioFocusAndSetMode : requesting stream: %s -> %s",
                    streamTypeToString(mAudioFocusStreamType), streamTypeToString(stream));
            mAudioManagerHandler.obtainMessage(
                    MSG_AUDIO_MANAGER_REQUEST_AUDIO_FOCUS_FOR_CALL,
                    stream,
                    0)
                    .sendToTarget();
        }
        mAudioFocusStreamType = stream;

        setMode(mode);
    }

    private void abandonAudioFocus() {
        if (hasFocus()) {
            setMode(AudioManager.MODE_NORMAL);
            Log.v(this, "abandoning audio focus");
            mAudioManagerHandler.obtainMessage(MSG_AUDIO_MANAGER_ABANDON_AUDIO_FOCUS_FOR_CALL, 0, 0)
                    .sendToTarget();
            mAudioFocusStreamType = STREAM_NONE;
            mCallToSpeedUpMTAudio = null;
        }
    }

    /**
     * Sets the audio mode.
     *
     * @param newMode Mode constant from AudioManager.MODE_*.
     */
    private void setMode(int newMode) {
        Preconditions.checkState(hasFocus());
        mAudioManagerHandler.obtainMessage(MSG_AUDIO_MANAGER_SET_MODE, newMode, 0).sendToTarget();
    }

    private int selectWiredOrEarpiece(int route, int supportedRouteMask) {
        // Since they are mutually exclusive and one is ALWAYS valid, we allow a special input of
        // ROUTE_WIRED_OR_EARPIECE so that callers dont have to make a call to check which is
        // supported before calling setAudioRoute.
        if (route == CallAudioState.ROUTE_WIRED_OR_EARPIECE) {
            route = CallAudioState.ROUTE_WIRED_OR_EARPIECE & supportedRouteMask;
            if (route == 0) {
                Log.wtf(this, "One of wired headset or earpiece should always be valid.");
                // assume earpiece in this case.
                route = CallAudioState.ROUTE_EARPIECE;
            }
        }
        return route;
    }

    private int calculateSupportedRoutes() {
        int routeMask = CallAudioState.ROUTE_SPEAKER;

        if (mWiredHeadsetManager.isPluggedIn()) {
            routeMask |= CallAudioState.ROUTE_WIRED_HEADSET;
        } else {
            routeMask |= CallAudioState.ROUTE_EARPIECE;
        }

        if (mBluetoothManager.isBluetoothAvailable()) {
            routeMask |=  CallAudioState.ROUTE_BLUETOOTH;
        }

        return routeMask;
    }

    private CallAudioState getInitialAudioState(Call call) {
        int supportedRouteMask = calculateSupportedRoutes();
        int route = selectWiredOrEarpiece(
                CallAudioState.ROUTE_WIRED_OR_EARPIECE, supportedRouteMask);

        // We want the UI to indicate that "bluetooth is in use" in two slightly different cases:
        // (a) The obvious case: if a bluetooth headset is currently in use for an ongoing call.
        // (b) The not-so-obvious case: if an incoming call is ringing, and we expect that audio
        //     *will* be routed to a bluetooth headset once the call is answered. In this case, just
        //     check if the headset is available. Note this only applies when we are dealing with
        //     the first call.
        if (call != null && mBluetoothManager.isBluetoothAvailable()) {
            switch(call.getState()) {
                case CallState.ACTIVE:
                case CallState.ON_HOLD:
                case CallState.DIALING:
                case CallState.CONNECTING:
                case CallState.RINGING:
                    route = CallAudioState.ROUTE_BLUETOOTH;
                    break;
                default:
                    break;
            }
        }

        return new CallAudioState(false, route, supportedRouteMask);
    }

    private void setInitialAudioState(Call call, boolean force) {
        CallAudioState audioState = getInitialAudioState(call);
        Log.i(this, "setInitialAudioState : audioState = %s, call = %s", audioState, call);
        setSystemAudioState(
                force, audioState.isMuted(), audioState.getRoute(),
                audioState.getSupportedRouteMask());
    }

    private void updateAudioForForegroundCall() {
        Call call = mCallsManager.getForegroundCall();
        if (call != null && call.getConnectionService() != null) {
            call.getConnectionService().onCallAudioStateChanged(call, mCallAudioState);
        }
    }

    /**
     * Returns the current foreground call in order to properly set the audio mode.
     */
    private Call getForegroundCall() {
        Call call = mCallsManager.getForegroundCall();

        // We ignore any foreground call that is in the ringing state because we deal with ringing
        // calls exclusively through the mIsRinging variable set by {@link Ringer}.
        if (call != null && call.getState() == CallState.RINGING) {
            return null;
        }

        return call;
    }

    private boolean hasRingingForegroundCall() {
        Call call = mCallsManager.getForegroundCall();
        return call != null && call.getState() == CallState.RINGING;
    }

    private boolean hasFocus() {
        return mAudioFocusStreamType != STREAM_NONE;
    }

    /**
     * Translates an {@link AudioManager} stream type to a human-readable string description.
     *
     * @param streamType The stream type.
     * @return Human readable description.
     */
    private String streamTypeToString(int streamType) {
        switch (streamType) {
            case STREAM_NONE:
                return STREAM_DESCRIPTION_NONE;
            case AudioManager.STREAM_ALARM:
                return STREAM_DESCRIPTION_ALARM;
            case AudioManager.STREAM_BLUETOOTH_SCO:
                return STREAM_DESCRIPTION_BLUETOOTH_SCO;
            case AudioManager.STREAM_DTMF:
                return STREAM_DESCRIPTION_DTMF;
            case AudioManager.STREAM_MUSIC:
                return STREAM_DESCRIPTION_MUSIC;
            case AudioManager.STREAM_NOTIFICATION:
                return STREAM_DESCRIPTION_NOTIFICATION;
            case AudioManager.STREAM_RING:
                return STREAM_DESCRIPTION_RING;
            case AudioManager.STREAM_SYSTEM:
                return STREAM_DESCRIPTION_SYSTEM;
            case AudioManager.STREAM_VOICE_CALL:
                return STREAM_DESCRIPTION_VOICE_CALL;
            default:
                return "STEAM_OTHER_" + streamType;
        }
    }

    /**
     * Translates an {@link AudioManager} mode into a human readable string.
     *
     * @param mode The mode.
     * @return The string.
     */
    private String modeToString(int mode) {
        switch (mode) {
            case AudioManager.MODE_INVALID:
                return MODE_DESCRIPTION_INVALID;
            case AudioManager.MODE_CURRENT:
                return MODE_DESCRIPTION_CURRENT;
            case AudioManager.MODE_NORMAL:
                return MODE_DESCRIPTION_NORMAL;
            case AudioManager.MODE_RINGTONE:
                return MODE_DESCRIPTION_RINGTONE;
            case AudioManager.MODE_IN_CALL:
                return MODE_DESCRIPTION_IN_CALL;
            case AudioManager.MODE_IN_COMMUNICATION:
                return MODE_DESCRIPTION_IN_COMMUNICATION;
            default:
                return "MODE_OTHER_" + mode;
        }
    }

    /**
     * Dumps the state of the {@link CallAudioManager}.
     *
     * @param pw The {@code IndentingPrintWriter} to write the state to.
     */
    public void dump(IndentingPrintWriter pw) {
        pw.println("mAudioState: " + mCallAudioState);
        pw.println("mBluetoothManager:");
        pw.increaseIndent();
        mBluetoothManager.dump(pw);
        pw.decreaseIndent();
        if (mWiredHeadsetManager != null) {
            pw.println("mWiredHeadsetManager:");
            pw.increaseIndent();
            mWiredHeadsetManager.dump(pw);
            pw.decreaseIndent();
        } else {
            pw.println("mWiredHeadsetManager: null");
        }
        pw.println("mAudioFocusStreamType: " + streamTypeToString(mAudioFocusStreamType));
        pw.println("mIsRinging: " + mIsRinging);
        pw.println("mIsTonePlaying: " + mIsTonePlaying);
        pw.println("mWasSpeakerOn: " + mWasSpeakerOn);
        pw.println("mMostRecentlyUsedMode: " + modeToString(mMostRecentlyUsedMode));
    }
}
