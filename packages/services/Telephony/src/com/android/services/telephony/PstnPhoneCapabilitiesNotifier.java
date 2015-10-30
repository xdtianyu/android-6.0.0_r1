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

package com.android.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.Preconditions;
import com.android.phone.PhoneUtils;

/**
 * Listens to phone's capabilities changed event and notifies Telecomm. One instance of these exists
 * for each of the telephony-based call services.
 */
final class PstnPhoneCapabilitiesNotifier {
    private static final int EVENT_VIDEO_CAPABILITIES_CHANGED = 1;

    /**
     * Listener called when video capabilities have changed.
     */
    public interface Listener {
        public void onVideoCapabilitiesChanged(boolean isVideoCapable);
    }

    private final PhoneProxy mPhoneProxy;
    private final Listener mListener;
    private Phone mPhoneBase;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_VIDEO_CAPABILITIES_CHANGED:
                    handleVideoCapabilitesChanged((AsyncResult) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    private final BroadcastReceiver mRatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED.equals(action)) {
                String newPhone = intent.getStringExtra(PhoneConstants.PHONE_NAME_KEY);
                Log.d(this, "Radio technology switched. Now %s is active.", newPhone);

                registerForNotifications();
            }
        }
    };

    /*package*/
    PstnPhoneCapabilitiesNotifier(PhoneProxy phoneProxy, Listener listener) {
        Preconditions.checkNotNull(phoneProxy);

        mPhoneProxy = phoneProxy;
        mListener = listener;

        registerForNotifications();

        IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        mPhoneProxy.getContext().registerReceiver(mRatReceiver, intentFilter);
    }

    /*package*/
    void teardown() {
        unregisterForNotifications();
        mPhoneProxy.getContext().unregisterReceiver(mRatReceiver);
    }

    private void registerForNotifications() {
        Phone newPhone = mPhoneProxy.getActivePhone();
        if (newPhone != mPhoneBase) {
            unregisterForNotifications();

            if (newPhone != null) {
                Log.d(this, "Registering: " + newPhone);
                mPhoneBase = newPhone;
                mPhoneBase.registerForVideoCapabilityChanged(
                        mHandler, EVENT_VIDEO_CAPABILITIES_CHANGED, null);
            }
        }
    }

    private void unregisterForNotifications() {
        if (mPhoneBase != null) {
            Log.d(this, "Unregistering: " + mPhoneBase);
            mPhoneBase.unregisterForVideoCapabilityChanged(mHandler);
        }
    }

    private void handleVideoCapabilitesChanged(AsyncResult ar) {
        try {
            boolean isVideoCapable = (Boolean) ar.result;
            Log.d(this, "handleVideoCapabilitesChanged. Video capability - " + isVideoCapable);
            PhoneAccountHandle accountHandle =
                    PhoneUtils.makePstnPhoneAccountHandle(mPhoneProxy);

            TelecomManager telecomMgr = TelecomManager.from(mPhoneProxy.getContext());
            PhoneAccount oldPhoneAccount = telecomMgr.getPhoneAccount(accountHandle);
            PhoneAccount.Builder builder = new PhoneAccount.Builder(oldPhoneAccount);

            int capabilites = newCapabilities(oldPhoneAccount.getCapabilities(),
                    PhoneAccount.CAPABILITY_VIDEO_CALLING, isVideoCapable);

            builder.setCapabilities(capabilites);
            telecomMgr.registerPhoneAccount(builder.build());
            mListener.onVideoCapabilitiesChanged(isVideoCapable);
        } catch (Exception e) {
            Log.d(this, "handleVideoCapabilitesChanged. Exception=" + e);
        }
    }

    private int newCapabilities(int capabilities, int capability, boolean set) {
        if (set) {
            capabilities |= capability;
        } else {
            capabilities &= ~capability;
        }
        return capabilities;
    }
}
