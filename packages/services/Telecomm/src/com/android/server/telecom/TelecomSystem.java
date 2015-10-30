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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;

/**
 * Top-level Application class for Telecom.
 */
public final class TelecomSystem {

    /**
     * This interface is implemented by system-instantiated components (e.g., Services and
     * Activity-s) that wish to use the TelecomSystem but would like to be testable. Such a
     * component should implement the getTelecomSystem() method to return the global singleton,
     * and use its own method. Tests can subclass the component to return a non-singleton.
     *
     * A refactoring goal for Telecom is to limit use of the TelecomSystem singleton to those
     * system-instantiated components, and have all other parts of the system just take all their
     * dependencies as explicit arguments to their constructor or other methods.
     */
    public interface Component {
        TelecomSystem getTelecomSystem();
    }


    /**
     * Tagging interface for the object used for synchronizing multi-threaded operations in
     * the Telecom system.
     */
    public interface SyncRoot {
    }

    private static final IntentFilter USER_SWITCHED_FILTER =
            new IntentFilter(Intent.ACTION_USER_SWITCHED);

    private static TelecomSystem INSTANCE = null;

    private final SyncRoot mLock = new SyncRoot() { };
    private final MissedCallNotifier mMissedCallNotifier;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final CallsManager mCallsManager;
    private final RespondViaSmsManager mRespondViaSmsManager;
    private final Context mContext;
    private final BluetoothPhoneServiceImpl mBluetoothPhoneServiceImpl;
    private final CallIntentProcessor mCallIntentProcessor;
    private final TelecomBroadcastIntentProcessor mTelecomBroadcastIntentProcessor;
    private final TelecomServiceImpl mTelecomServiceImpl;
    private final ContactsAsyncHelper mContactsAsyncHelper;

    private final BroadcastReceiver mUserSwitchedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userHandleId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 0);
            UserHandle currentUserHandle = new UserHandle(userHandleId);
            mPhoneAccountRegistrar.setCurrentUserHandle(currentUserHandle);
        }
    };

    public static TelecomSystem getInstance() {
        return INSTANCE;
    }

    public static void setInstance(TelecomSystem instance) {
        if (INSTANCE != null) {
            throw new RuntimeException("Attempt to set TelecomSystem.INSTANCE twice");
        }
        Log.i(TelecomSystem.class, "TelecomSystem.INSTANCE being set");
        INSTANCE = instance;
    }

    public TelecomSystem(
            Context context,
            MissedCallNotifier missedCallNotifier,
            CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory,
            HeadsetMediaButtonFactory headsetMediaButtonFactory,
            ProximitySensorManagerFactory proximitySensorManagerFactory,
            InCallWakeLockControllerFactory inCallWakeLockControllerFactory) {
        mContext = context.getApplicationContext();

        mMissedCallNotifier = missedCallNotifier;
        mPhoneAccountRegistrar = new PhoneAccountRegistrar(mContext);
        mContactsAsyncHelper = new ContactsAsyncHelper(mLock);

        mCallsManager = new CallsManager(
                mContext,
                mLock,
                mContactsAsyncHelper,
                callerInfoAsyncQueryFactory,
                mMissedCallNotifier,
                mPhoneAccountRegistrar,
                headsetMediaButtonFactory,
                proximitySensorManagerFactory,
                inCallWakeLockControllerFactory);

        mRespondViaSmsManager = new RespondViaSmsManager(mCallsManager, mLock);
        mCallsManager.setRespondViaSmsManager(mRespondViaSmsManager);

        mContext.registerReceiver(mUserSwitchedReceiver, USER_SWITCHED_FILTER);
        mBluetoothPhoneServiceImpl = new BluetoothPhoneServiceImpl(
                mContext, mLock, mCallsManager, mPhoneAccountRegistrar);
        mCallIntentProcessor = new CallIntentProcessor(mContext, mCallsManager);
        mTelecomBroadcastIntentProcessor = new TelecomBroadcastIntentProcessor(
                mContext, mCallsManager);
        mTelecomServiceImpl = new TelecomServiceImpl(
                mContext, mCallsManager, mPhoneAccountRegistrar, mLock);
    }

    @VisibleForTesting
    public PhoneAccountRegistrar getPhoneAccountRegistrar() {
        return mPhoneAccountRegistrar;
    }

    public BluetoothPhoneServiceImpl getBluetoothPhoneServiceImpl() {
        return mBluetoothPhoneServiceImpl;
    }

    public CallIntentProcessor getCallIntentProcessor() {
        return mCallIntentProcessor;
    }

    public TelecomBroadcastIntentProcessor getTelecomBroadcastIntentProcessor() {
        return mTelecomBroadcastIntentProcessor;
    }

    public TelecomServiceImpl getTelecomServiceImpl() {
        return mTelecomServiceImpl;
    }

    public Object getLock() {
        return mLock;
    }
}
