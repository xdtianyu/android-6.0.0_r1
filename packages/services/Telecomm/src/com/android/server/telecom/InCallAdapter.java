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

import android.os.Binder;
import android.telecom.PhoneAccountHandle;

import com.android.internal.telecom.IInCallAdapter;

/**
 * Receives call commands and updates from in-call app and passes them through to CallsManager.
 * {@link InCallController} creates an instance of this class and passes it to the in-call app after
 * binding to it. This adapter can receive commands and updates until the in-call app is unbound.
 */
class InCallAdapter extends IInCallAdapter.Stub {
    private final CallsManager mCallsManager;
    private final CallIdMapper mCallIdMapper;
    private final TelecomSystem.SyncRoot mLock;

    /** Persists the specified parameters. */
    public InCallAdapter(CallsManager callsManager, CallIdMapper callIdMapper,
            TelecomSystem.SyncRoot lock) {
        mCallsManager = callsManager;
        mCallIdMapper = callIdMapper;
        mLock = lock;
    }

    @Override
    public void answerCall(String callId, int videoState) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                Log.d(this, "answerCall(%s,%d)", callId, videoState);
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.answerCall(call, videoState);
                    } else {
                        Log.w(this, "answerCall, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void rejectCall(String callId, boolean rejectWithMessage, String textMessage) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                Log.d(this, "rejectCall(%s,%b,%s)", callId, rejectWithMessage, textMessage);
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.rejectCall(call, rejectWithMessage, textMessage);
                    } else {
                        Log.w(this, "setRingback, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void playDtmfTone(String callId, char digit) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                Log.d(this, "playDtmfTone(%s,%c)", callId, digit);
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.playDtmfTone(call, digit);
                    } else {
                        Log.w(this, "playDtmfTone, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void stopDtmfTone(String callId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                Log.d(this, "stopDtmfTone(%s)", callId);
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.stopDtmfTone(call);
                    } else {
                        Log.w(this, "stopDtmfTone, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void postDialContinue(String callId, boolean proceed) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                Log.d(this, "postDialContinue(%s)", callId);
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.postDialContinue(call, proceed);
                    } else {
                        Log.w(this, "postDialContinue, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void disconnectCall(String callId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                Log.v(this, "disconnectCall: %s", callId);
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.disconnectCall(call);
                    } else {
                        Log.w(this, "disconnectCall, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void holdCall(String callId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.holdCall(call);
                    } else {
                        Log.w(this, "holdCall, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void unholdCall(String callId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.unholdCall(call);
                    } else {
                        Log.w(this, "unholdCall, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void phoneAccountSelected(String callId, PhoneAccountHandle accountHandle,
            boolean setDefault) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.phoneAccountSelected(call, accountHandle, setDefault);
                    } else {
                        Log.w(this, "phoneAccountSelected, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void mute(boolean shouldMute) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                mCallsManager.mute(shouldMute);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void setAudioRoute(int route) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                mCallsManager.setAudioRoute(route);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void conference(String callId, String otherCallId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                if (mCallIdMapper.isValidCallId(callId) &&
                        mCallIdMapper.isValidCallId(otherCallId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    Call otherCall = mCallIdMapper.getCall(otherCallId);
                    if (call != null && otherCall != null) {
                        mCallsManager.conference(call, otherCall);
                    } else {
                        Log.w(this, "conference, unknown call id: %s or %s", callId, otherCallId);
                    }

                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void splitFromConference(String callId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        call.splitFromConference();
                    } else {
                        Log.w(this, "splitFromConference, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void mergeConference(String callId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        call.mergeConference();
                    } else {
                        Log.w(this, "mergeConference, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void swapConference(String callId) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                if (mCallIdMapper.isValidCallId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        call.swapConference();
                    } else {
                        Log.w(this, "swapConference, unknown call id: %s", callId);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void turnOnProximitySensor() {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                mCallsManager.turnOnProximitySensor();
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void turnOffProximitySensor(boolean screenOnImmediately) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (mLock) {
                mCallsManager.turnOffProximitySensor(screenOnImmediately);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
