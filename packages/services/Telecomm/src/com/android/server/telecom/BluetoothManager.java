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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

import com.android.internal.util.IndentingPrintWriter;

import java.util.List;

/**
 * Listens to and caches bluetooth headset state.  Used By the CallAudioManager for maintaining
 * overall audio state. Also provides method for connecting the bluetooth headset to the phone call.
 */
public class BluetoothManager {

    private final BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    mBluetoothHeadset = (BluetoothHeadset) proxy;
                    Log.v(this, "- Got BluetoothHeadset: " + mBluetoothHeadset);
                    updateBluetoothState();
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    mBluetoothHeadset = null;
                    Log.v(this, "Lost BluetoothHeadset: " + mBluetoothHeadset);
                    updateBluetoothState();
                }
           };

    /**
     * Receiver for misc intent broadcasts the BluetoothManager cares about.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                int bluetoothHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                                          BluetoothHeadset.STATE_DISCONNECTED);
                Log.d(this, "mReceiver: HEADSET_STATE_CHANGED_ACTION");
                Log.d(this, "==> new state: %s ", bluetoothHeadsetState);
                updateBluetoothState();
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                int bluetoothHeadsetAudioState =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                           BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                Log.d(this, "mReceiver: HEADSET_AUDIO_STATE_CHANGED_ACTION");
                Log.d(this, "==> new state: %s", bluetoothHeadsetAudioState);
                updateBluetoothState();
            }
        }
    };

    private final BluetoothAdapter mBluetoothAdapter;
    private final CallAudioManager mCallAudioManager;

    private BluetoothHeadset mBluetoothHeadset;
    private boolean mBluetoothConnectionPending = false;
    private long mBluetoothConnectionRequestTime;


    public BluetoothManager(Context context, CallAudioManager callAudioManager) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mCallAudioManager = callAudioManager;

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.getProfileProxy(context, mBluetoothProfileServiceListener,
                                    BluetoothProfile.HEADSET);
        }

        // Register for misc other intent broadcasts.
        IntentFilter intentFilter =
                new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        context.registerReceiver(mReceiver, intentFilter);
    }

    //
    // Bluetooth helper methods.
    //
    // - BluetoothAdapter is the Bluetooth system service.  If
    //   getDefaultAdapter() returns null
    //   then the device is not BT capable.  Use BluetoothDevice.isEnabled()
    //   to see if BT is enabled on the device.
    //
    // - BluetoothHeadset is the API for the control connection to a
    //   Bluetooth Headset.  This lets you completely connect/disconnect a
    //   headset (which we don't do from the Phone UI!) but also lets you
    //   get the address of the currently active headset and see whether
    //   it's currently connected.

    /**
     * @return true if the Bluetooth on/off switch in the UI should be
     *         available to the user (i.e. if the device is BT-capable
     *         and a headset is connected.)
     */
    boolean isBluetoothAvailable() {
        Log.v(this, "isBluetoothAvailable()...");

        // There's no need to ask the Bluetooth system service if BT is enabled:
        //
        //    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        //    if ((adapter == null) || !adapter.isEnabled()) {
        //        Log.d(this, "  ==> FALSE (BT not enabled)");
        //        return false;
        //    }
        //    Log.d(this, "  - BT enabled!  device name " + adapter.getName()
        //                 + ", address " + adapter.getAddress());
        //
        // ...since we already have a BluetoothHeadset instance.  We can just
        // call isConnected() on that, and assume it'll be false if BT isn't
        // enabled at all.

        // Check if there's a connected headset, using the BluetoothHeadset API.
        boolean isConnected = false;
        if (mBluetoothHeadset != null) {
            List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

            if (deviceList.size() > 0) {
                isConnected = true;
                for (int i = 0; i < deviceList.size(); i++) {
                    BluetoothDevice device = deviceList.get(i);
                    Log.v(this, "state = " + mBluetoothHeadset.getConnectionState(device)
                            + "for headset: " + device);
                }
            }
        }

        Log.v(this, "  ==> " + isConnected);
        return isConnected;
    }

    /**
     * @return true if a BT Headset is available, and its audio is currently connected.
     */
    boolean isBluetoothAudioConnected() {
        if (mBluetoothHeadset == null) {
            Log.v(this, "isBluetoothAudioConnected: ==> FALSE (null mBluetoothHeadset)");
            return false;
        }
        List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

        if (deviceList.isEmpty()) {
            return false;
        }
        for (int i = 0; i < deviceList.size(); i++) {
            BluetoothDevice device = deviceList.get(i);
            boolean isAudioOn = mBluetoothHeadset.isAudioConnected(device);
            Log.v(this, "isBluetoothAudioConnected: ==> isAudioOn = " + isAudioOn
                    + "for headset: " + device);
            if (isAudioOn) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method used to control the onscreen "Bluetooth" indication;
     *
     * @return true if a BT device is available and its audio is currently connected,
     *              <b>or</b> if we issued a BluetoothHeadset.connectAudio()
     *              call within the last 5 seconds (which presumably means
     *              that the BT audio connection is currently being set
     *              up, and will be connected soon.)
     */
    /* package */ boolean isBluetoothAudioConnectedOrPending() {
        if (isBluetoothAudioConnected()) {
            Log.v(this, "isBluetoothAudioConnectedOrPending: ==> TRUE (really connected)");
            return true;
        }

        // If we issued a connectAudio() call "recently enough", even
        // if BT isn't actually connected yet, let's still pretend BT is
        // on.  This makes the onscreen indication more responsive.
        if (mBluetoothConnectionPending) {
            long timeSinceRequest =
                    SystemClock.elapsedRealtime() - mBluetoothConnectionRequestTime;
            if (timeSinceRequest < 5000 /* 5 seconds */) {
                Log.v(this, "isBluetoothAudioConnectedOrPending: ==> TRUE (requested "
                             + timeSinceRequest + " msec ago)");
                return true;
            } else {
                Log.v(this, "isBluetoothAudioConnectedOrPending: ==> FALSE (request too old: "
                             + timeSinceRequest + " msec ago)");
                mBluetoothConnectionPending = false;
                return false;
            }
        }

        Log.v(this, "isBluetoothAudioConnectedOrPending: ==> FALSE");
        return false;
    }

    /**
     * Notified audio manager of a change to the bluetooth state.
     */
    void updateBluetoothState() {
        mCallAudioManager.onBluetoothStateChange(this);
    }

    void connectBluetoothAudio() {
        Log.v(this, "connectBluetoothAudio()...");
        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.connectAudio();
        }

        // Watch out: The bluetooth connection doesn't happen instantly;
        // the connectAudio() call returns instantly but does its real
        // work in another thread.  The mBluetoothConnectionPending flag
        // is just a little trickery to ensure that the onscreen UI updates
        // instantly. (See isBluetoothAudioConnectedOrPending() above.)
        mBluetoothConnectionPending = true;
        mBluetoothConnectionRequestTime = SystemClock.elapsedRealtime();
    }

    void disconnectBluetoothAudio() {
        Log.v(this, "disconnectBluetoothAudio()...");
        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.disconnectAudio();
        }
        mBluetoothConnectionPending = false;
    }

    /**
     * Dumps the state of the {@link BluetoothManager}.
     *
     * @param pw The {@code IndentingPrintWriter} to write the state to.
     */
    public void dump(IndentingPrintWriter pw) {
        pw.println("isBluetoothAvailable: " + isBluetoothAvailable());
        pw.println("isBluetoothAudioConnected: " + isBluetoothAudioConnected());
        pw.println("isBluetoothAudioConnectedOrPending: " + isBluetoothAudioConnectedOrPending());

        if (mBluetoothAdapter != null) {
            if (mBluetoothHeadset != null) {
                List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

                if (deviceList.size() > 0) {
                    BluetoothDevice device = deviceList.get(0);
                    pw.println("BluetoothHeadset.getCurrentDevice: " + device);
                    pw.println("BluetoothHeadset.State: "
                            + mBluetoothHeadset.getConnectionState(device));
                    pw.println("BluetoothHeadset audio connected: " +
                            mBluetoothHeadset.isAudioConnected(device));
                }
            } else {
                pw.println("mBluetoothHeadset is null");
            }
        } else {
            pw.println("mBluetoothAdapter is null; device is not BT capable");
        }
    }
}
