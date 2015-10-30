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

package com.android.tv.settings.accessories;

import android.annotation.DrawableRes;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.util.Set;
import java.util.UUID;

public class BluetoothAccessoryActivity extends SettingsLayoutActivity {

    public static final String EXTRA_ACCESSORY_ADDRESS = "accessory_address";
    public static final String EXTRA_ACCESSORY_NAME = "accessory_name";
    public static final String EXTRA_ACCESSORY_ICON_ID = "accessory_icon_res";

    private static final String SAVE_STATE_UNPAIRING = "BluetoothAccessoryActivity.unpairing";

    private static final int MSG_UNPAIR_TIMEOUT = 1;
    private static final int UNPAIR_TIMEOUT = 5000;

    private static final UUID GATT_BATTERY_SERVICE_UUID =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID GATT_BATTERY_LEVEL_CHARACTERISTIC_UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private static final int ACTION_UNPAIR_OK = 100;
    private static final int ACTION_UNPAIR_CANCEL = 101;

    private static final String TAG = "BTAccSett";
    private static final boolean DEBUG = false;

    private BluetoothDevice mDevice;
    private BluetoothGatt mDeviceGatt;
    protected String mDeviceAddress;
    protected String mDeviceName;
    protected @DrawableRes int mDeviceImgId;
    protected boolean mUnpairing;
    private int mBatteryLevel = -1;

    private Layout.LayoutGetter mLayoutGetter;

    public static Intent getIntent(Context context, String deviceAddress,
            String deviceName, @DrawableRes int iconId) {
        Intent i = new Intent(context, BluetoothAccessoryActivity.class);
        i.putExtra(EXTRA_ACCESSORY_ADDRESS, deviceAddress);
        i.putExtra(EXTRA_ACCESSORY_NAME, deviceName);
        i.putExtra(EXTRA_ACCESSORY_ICON_ID, iconId);
        return i;
    }

    // Broadcast Receiver for Bluetooth related events
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (mUnpairing) {
                if (mDevice.equals(device)) {
                    // Done removing device, finish the activity
                    mMsgHandler.removeMessages(MSG_UNPAIR_TIMEOUT);
                    finish();
                }
            }
        }
    };

    // Internal message handler
    private final Handler mMsgHandler = new Handler() {
            @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UNPAIR_TIMEOUT:
                    finish();
                    break;
                default:
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mDeviceAddress = bundle.getString(EXTRA_ACCESSORY_ADDRESS);
            mDeviceName = bundle.getString(EXTRA_ACCESSORY_NAME);
            mDeviceImgId = bundle.getInt(EXTRA_ACCESSORY_ICON_ID);
        } else {
            mDeviceName = getString(R.string.accessory_options);
            mDeviceImgId = R.drawable.ic_qs_bluetooth_not_connected;
        }

        super.onCreate(savedInstanceState);

        mUnpairing = savedInstanceState != null
                && savedInstanceState.getBoolean(SAVE_STATE_UNPAIRING);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                if (mDeviceAddress.equals(device.getAddress())) {
                    mDevice = device;
                    break;
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDevice != null &&
                (mDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE ||
                mDevice.getType() == BluetoothDevice.DEVICE_TYPE_DUAL)) {
            // Only LE devices support GATT
            mDeviceGatt = mDevice.connectGatt(this, true, new GattBatteryCallbacks());
        }
    }

    private class GattBatteryCallbacks extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (DEBUG) {
                Log.d(TAG, "Connection status:" + status + " state:" + newState);
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (DEBUG) {
                    Log.e(TAG, "Service discovery failure on " + gatt);
                }
                return;
            }

            final BluetoothGattService battService = gatt.getService(GATT_BATTERY_SERVICE_UUID);
            if (battService == null) {
                if (DEBUG) {
                    Log.d(TAG, "No battery service");
                }
                return;
            }

            final BluetoothGattCharacteristic battLevel =
                    battService.getCharacteristic(GATT_BATTERY_LEVEL_CHARACTERISTIC_UUID);
            if (battLevel == null) {
                if (DEBUG) {
                    Log.d(TAG, "No battery level");
                }
                return;
            }

            gatt.readCharacteristic(battLevel);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (DEBUG) {
                    Log.e(TAG, "Read characteristic failure on " + gatt + " " + characteristic);
                }
                return;
            }

            if (GATT_BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                mBatteryLevel =
                        characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (mLayoutGetter != null) {
                    mLayoutGetter.refreshView();
                }
            }
        }
    }

    @Override
    public void onResume() {
        // Set a broadcast receiver to let us know when the device has been removed
        IntentFilter adapterIntentFilter = new IntentFilter();
        adapterIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, adapterIntentFilter);
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(SAVE_STATE_UNPAIRING, mUnpairing);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDeviceGatt != null) {
            mDeviceGatt.close();
        }
    }

    @Override
    public Layout createLayout() {
        final Resources res = getResources();

        mLayoutGetter = new Layout.LayoutGetter() {
            @Override
            public Layout get() {
                final Resources res = getResources();
                if (mUnpairing) {
                    return new Layout()
                            .add(new Layout.Status.Builder(res)
                                    .title(R.string.accessory_unpairing)
                                    .build());
                } else {
                    final Layout layout = new Layout();
                    layout.add(new Layout.Header.Builder(res)
                            .title(R.string.accessory_unpair)
                            .build()
                            .add(new Layout.Action.Builder(res, ACTION_UNPAIR_OK)
                                    .title(R.string.settings_ok)
                                    .build())
                            .add(new Layout.Action.Builder(res, ACTION_UNPAIR_CANCEL)
                                    .title(R.string.settings_cancel)
                                    .build()));

                    if (mBatteryLevel != -1) {
                        layout.add(new Layout.Status.Builder(res)
                                .title(getString(R.string.accessory_battery, mBatteryLevel))
                                .build());
                    }

                    return layout;
                }
            }
        };

        return new Layout()
                .breadcrumb(getString(R.string.header_category_accessories))
                .add(new Layout.Header.Builder(res)
                        .title(mDeviceName)
                        .icon(mDeviceImgId)
                        .build()
                        .add(mLayoutGetter));
    }

    @Override
    public void onActionClicked(Layout.Action action) {
        switch (action.getId()) {
            case ACTION_UNPAIR_OK:
                unpairDevice();
                break;
            case ACTION_UNPAIR_CANCEL:
                onBackPressed();
                break;
        }
    }

    void unpairDevice() {
        if (mDevice != null) {
            int state = mDevice.getBondState();

            if (state == BluetoothDevice.BOND_BONDING) {
                mDevice.cancelBondProcess();
            }

            if (state != BluetoothDevice.BOND_NONE) {
                mUnpairing = true;
                // Set a timeout, just in case we don't receive the unpair notification we
                // use to finish the activity
                mMsgHandler.sendEmptyMessageDelayed(MSG_UNPAIR_TIMEOUT, UNPAIR_TIMEOUT);
                final boolean successful = mDevice.removeBond();
                if (successful) {
                    if (DEBUG) {
                        Log.d(TAG, "Bluetooth device successfully unpaired.");
                    }
                    // set the dialog to a waiting state
                    if (mLayoutGetter != null) {
                        mLayoutGetter.refreshView();
                    }
                } else {
                    Log.e(TAG, "Failed to unpair Bluetooth Device: " + mDevice.getName());
                }
            }
        } else {
            Log.e(TAG, "Bluetooth device not found. Address = " + mDeviceAddress);
        }
    }
}
