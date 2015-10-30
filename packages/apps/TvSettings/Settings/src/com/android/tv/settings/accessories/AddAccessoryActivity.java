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

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.DialogActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Activity for detecting and adding (pairing) new bluetooth devices.
 */
public class AddAccessoryActivity extends DialogActivity
        implements ActionAdapter.Listener,
        BluetoothDevicePairer.EventListener {

    private static final boolean DEBUG = false;
    private static final String TAG = "AddAccessoryActivity";

    private static final String ACTION_CONNECT_INPUT =
            "com.google.android.intent.action.CONNECT_INPUT";

    private static final String INTENT_EXTRA_NO_INPUT_MODE = "no_input_mode";

    private static final String KEY_BT_DEVICE = "selected_bt_device";

    private static final String ADDRESS_NONE = "NONE";

    private static final int AUTOPAIR_COUNT = 10;

    private static final int MSG_UPDATE_VIEW = 1;
    private static final int MSG_REMOVE_CANCELED = 2;
    private static final int MSG_PAIRING_COMPLETE = 3;
    private static final int MSG_OP_TIMEOUT = 4;
    private static final int MSG_RESTART = 5;
    private static final int MSG_TRIGGER_SELECT_DOWN = 6;
    private static final int MSG_TRIGGER_SELECT_UP = 7;
    private static final int MSG_AUTOPAIR_TICK = 8;
    private static final int MSG_START_AUTOPAIR_COUNTDOWN = 9;
    private static final int MSG_MULTIPAIR_BLINK = 10;

    private static final int CANCEL_MESSAGE_TIMEOUT = 3000;
    private static final int DONE_MESSAGE_TIMEOUT = 3000;
    private static final int PAIR_OPERATION_TIMEOUT = 120000;
    private static final int CONNECT_OPERATION_TIMEOUT = 15000;
    private static final int RESTART_DELAY = 3000;
    private static final int LONG_PRESS_DURATION = 3000;
    private static final int KEY_DOWN_TIME = 150;
    private static final int TIME_TO_START_AUTOPAIR_COUNT = 5000;
    private static final int BLINK_START = 1000;
    private static final int EXIT_TIMEOUT_MILLIS = 90 * 1000;

    private ActionFragment mActionFragment;
    private ArrayList<Action> mActions;
    private AddAccessoryContentFragment mContentFragment;

    // members related to Bluetooth pairing
    private BluetoothDevicePairer mBtPairer;
    private int mPreviousStatus = BluetoothDevicePairer.STATUS_NONE;
    private boolean mPairingSuccess = false;
    private boolean mPairingBluetooth = false;
    private ArrayList<BluetoothDevice> mBtDevices;
    private String mCancelledAddress = ADDRESS_NONE;
    private String mCurrentTargetAddress = ADDRESS_NONE;
    private String mCurrentTargetStatus = "";
    private boolean mPairingInBackground = false;

    private boolean mActionsVisible = false;
    private ViewGroup mTopLayout;
    private View mActionView;
    private View mContentView;
    private boolean mShowingMultiFragment;
    private TextView mAutoPairText;
    private AnimationDrawable mAnimation;
    private int mViewOffset = 0;
    private static final int ANIMATE_IN_DELAY = 1500;
    private static long mStartTime;
    private boolean mAnimateOnStart = true;
    private boolean mDone = false;
    private final Object mLock = new Object();

    private FragmentManager mFragmentManager;

    private IDreamManager mDreamManager;
    private boolean mHwKeyDown;
    private boolean mHwKeyDidSelect;
    private boolean mNoInputMode;
    private boolean mActionsAnimationDone;
    private boolean mFragmentTransactionPending;

    // Internal message handler
    private final MessageHandler mMsgHandler = new MessageHandler();

    private static class MessageHandler extends Handler {

        private WeakReference<AddAccessoryActivity> mActivityRef = new WeakReference<>(null);

        public void setActivity(AddAccessoryActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final AddAccessoryActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case MSG_UPDATE_VIEW:
                    activity.updateView();
                    break;
                case MSG_REMOVE_CANCELED:
                    activity.mCancelledAddress = ADDRESS_NONE;
                    activity.updateView();
                    break;
                case MSG_PAIRING_COMPLETE:
                    activity.finish();
                    break;
                case MSG_OP_TIMEOUT:
                    activity.handlePairingTimeout();
                    break;
                case MSG_RESTART:
                    if (activity.mBtPairer != null) {
                        activity.mBtPairer.start();
                        activity.mBtPairer.cancelPairing();
                    }
                    break;
                case MSG_TRIGGER_SELECT_DOWN:
                    activity.sendKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER, true);
                    activity.mHwKeyDidSelect = true;
                    sendEmptyMessageDelayed(MSG_TRIGGER_SELECT_UP, KEY_DOWN_TIME);
                    activity.cancelPairingCountdown();
                    break;
                case MSG_TRIGGER_SELECT_UP:
                    activity.sendKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER, false);
                    break;
                case MSG_START_AUTOPAIR_COUNTDOWN:
                    activity.mAutoPairText.setVisibility(View.VISIBLE);
                    activity.mAutoPairText.setText(String.format(
                            activity.getString(R.string.accessories_autopair_msg), AUTOPAIR_COUNT));
                    sendMessageDelayed(obtainMessage(MSG_AUTOPAIR_TICK,
                            AUTOPAIR_COUNT, 0, null), 1000);
                    break;
                case MSG_AUTOPAIR_TICK:
                    int countToAutoPair = msg.arg1 - 1;
                    if (activity.mAutoPairText != null) {
                        if (countToAutoPair <= 0) {
                            activity.mAutoPairText.setVisibility(View.GONE);
                            // AutoPair
                            activity.startAutoPairing();
                        } else {
                            activity.mAutoPairText.setText(String.format(
                                    activity.getString(R.string.accessories_autopair_msg),
                                    countToAutoPair));
                            sendMessageDelayed(obtainMessage(MSG_AUTOPAIR_TICK,
                                    countToAutoPair, 0, null), 1000);
                        }
                    }
                    break;
                case MSG_MULTIPAIR_BLINK:
                    // Kick off the blinking animation
                    ImageView backImage = (ImageView) activity.findViewById(R.id.back_panel_image);
                    if (backImage != null) {
                        activity.mAnimation = (AnimationDrawable) backImage.getDrawable();
                        if (activity.mAnimation != null) {
                            activity.mAnimation.start();
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private final Handler mAutoExitHandler = new Handler();

    private final Runnable mAutoExitRunnable = new Runnable() {
        @Override
        public void run() {
            stopActivity();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMsgHandler.setActivity(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.checkService(
                DreamService.DREAM_SERVICE));

        mFragmentManager = getFragmentManager();

        mBtDevices = new ArrayList<>();

        mActions = new ArrayList<>();

        mNoInputMode = getIntent().getBooleanExtra(INTENT_EXTRA_NO_INPUT_MODE, false);
        mHwKeyDown = false;

        mActions.clear();

        mActionFragment = ActionFragment.newInstance(mActions);
        mContentFragment = AddAccessoryContentFragment.newInstance(false);
        setContentAndActionFragments(mContentFragment, mActionFragment);
        mShowingMultiFragment = false;

        mActionsAnimationDone = false;
        mFragmentTransactionPending = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (DEBUG) {
            Log.d(TAG, "onStart() mPairingInBackground = " + mPairingInBackground);
        }

        // Only do the following if we are not coming back to this activity from
        // the Secure Pairing activity.
        if (!mPairingInBackground) {
            if (mAnimateOnStart) {
                mAnimateOnStart = false;
                ViewGroup contentView = (ViewGroup) findViewById(android.R.id.content);
                mTopLayout = (ViewGroup) contentView.getChildAt(0);

                // Fade out the old activity, and fade in the new activity.
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                // Set the activity background
                int bgColor = getColor(R.color.dialog_activity_background);
                getBackgroundDrawable().setColor(bgColor);
                mTopLayout.setBackground(getBackgroundDrawable());

                // Delay the rest of the changes until the first layout event
                mTopLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                mTopLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                // set the Action and Content fragments to their start offsets
                                mActionView = findViewById(R.id.action_fragment);
                                mContentView = findViewById(R.id.content_fragment);
                                if (mActionView != null) {
                                    mViewOffset = mActionView.getMeasuredWidth();
                                    int offset = (ViewCompat.getLayoutDirection(mActionView) ==
                                            ViewCompat.LAYOUT_DIRECTION_RTL) ?
                                            -mViewOffset : mViewOffset;
                                    mActionView.setTranslationX(offset);
                                    mContentView.setTranslationX(offset / 2);
                                }
                                mAutoPairText = (TextView) findViewById(R.id.autopair_message);
                                if (mAutoPairText != null) {
                                    mAutoPairText.setVisibility(View.GONE);
                                }
                                updateView();
                            }
                        });
            }

            startBluetoothPairer();

            mStartTime = SystemClock.elapsedRealtime();
        }

        mPairingInBackground = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.d(TAG, "stopping auto-exit timer");
        mAutoExitHandler.removeCallbacks(mAutoExitRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNoInputMode) {
            // Start timer count down for exiting activity.
            if (DEBUG) Log.d(TAG, "starting auto-exit timer");
            mAutoExitHandler.postDelayed(mAutoExitRunnable, EXIT_TIMEOUT_MILLIS);
        }
    }

    @Override
    public void onStop() {
        if (DEBUG) {
            Log.d(TAG, "onStop()");
        }
        if (!mPairingBluetooth) {
            stopActivity();
        } else {
            // allow activity to remain in the background while we perform the
            // BT Secure pairing.
            mPairingInBackground = true;
        }

        super.onStop();
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            if (mPairingBluetooth && !mDone) {
                cancelBtPairing();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (ACTION_CONNECT_INPUT.equals(intent.getAction()) &&
                (intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == 0) {
            // We were the front most app and we got a new intent.
            // If screen saver is going, stop it.
            try {
                if (mDreamManager != null && mDreamManager.isDreaming()) {
                    mDreamManager.awaken();
                }
            } catch (RemoteException e) {
                // Do nothing.
            }

            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_PAIRING) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    onHwKeyEvent(false);
                } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    onHwKeyEvent(true);
                }
            }
        } else {
            setIntent(intent);
        }
    }

    @Override
    protected void onIntroAnimationFinished() {
        mActionsAnimationDone = true;
        if (mFragmentTransactionPending) {
            mFragmentTransactionPending = false;
            switchToMultipleDevicesFragment();
        }
    }

    @Override
    public void onActionClicked(Action action) {
        cancelPairingCountdown();
        if (!mDone) {
            String key = action.getKey();

            if (KEY_BT_DEVICE.equals(key)) {
                btDeviceClicked(action.getDescription());
            }
        }
    }

    // Events related to a device HW key
    protected void onHwKeyEvent(boolean keyDown) {
        if (!mHwKeyDown) {
            // HW key was in UP state before
            if (keyDown) {
                // Back key pressed down
                mHwKeyDown = true;
                mHwKeyDidSelect = false;
                mMsgHandler.sendEmptyMessageDelayed(MSG_TRIGGER_SELECT_DOWN, LONG_PRESS_DURATION);
            }
        } else {
            // HW key was in DOWN state before
            if (!keyDown) {
                // HW key released
                mHwKeyDown = false;
                mMsgHandler.removeMessages(MSG_TRIGGER_SELECT_DOWN);
                if (!mHwKeyDidSelect) {
                    // key wasn't pressed long enough for selection, move selection
                    // to next item.
                    int selectedIndex = mActionFragment.getSelectedItemPosition() + 1;
                    if (selectedIndex >= mActions.size()) {
                        selectedIndex = 0;
                    }
                    mActionFragment.setSelectionSmooth(selectedIndex);
                }
                mHwKeyDidSelect = false;
            }
        }
    }

    private void sendKeyEvent(int keyCode, boolean down) {
        InputManager iMgr = (InputManager) getSystemService(INPUT_SERVICE);
        if (iMgr != null) {
            long time = SystemClock.uptimeMillis();
            KeyEvent evt = new KeyEvent(time, time,
                    down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP,
                    keyCode, 0);
            iMgr.injectInputEvent(evt, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    }

    protected void updateView() {
        if (mActionView == null || mStartTime == 0) {
            // view not yet ready, update will happen on first layout event
            return;
        }

        synchronized (mLock) {
            int prevNumDevices = mActions.size();
            mActions.clear();

            if (mActionFragment != null && mBtPairer != null) {
                // Add entries for the discovered Bluetooth devices
                for (BluetoothDevice bt : mBtDevices) {
                    String title = bt.getName();
                    String desc;
                    if (mCurrentTargetAddress.equalsIgnoreCase(bt.getAddress()) &&
                            !mCurrentTargetStatus.isEmpty()) {
                        desc = mCurrentTargetStatus;
                    } else if (mCancelledAddress.equalsIgnoreCase(bt.getAddress())) {
                        desc = getString(R.string.accessory_state_canceled);
                    } else {
                        desc = bt.getAddress();
                    }
                    mActions.add(new Action.Builder()
                            .key(KEY_BT_DEVICE)
                            .title(title)
                            .description(desc.toUpperCase())
                            .drawableResource(AccessoryUtils.getImageIdForDevice(bt))
                            .build());
                }
            }

            // Update the main fragment.
            ActionAdapter adapter = (ActionAdapter) mActionFragment.getAdapter();
            if (adapter != null) {
                adapter.setActions(mActions);
            }

            if (!mActionsVisible && mActions.size() > 0) {
                mActionsVisible = true;
                long delay = ANIMATE_IN_DELAY - (SystemClock.elapsedRealtime() - mStartTime);
                if (delay > 0) {
                    // Make sure we have a little bit of time after the activity
                    // fades in
                    // before we animate the actions in
                    mActionView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animateActionsIn();
                        }
                    }, delay);
                } else {
                    animateActionsIn();
                }
            }

            if (mNoInputMode) {
                if (DEBUG) Log.d(TAG, "stopping auto-exit timer");
                mAutoExitHandler.removeCallbacks(mAutoExitRunnable);
                if (mActions.size() == 1 && prevNumDevices == 0) {
                    // first device added, start counter for autopair
                    mMsgHandler.sendEmptyMessageDelayed(MSG_START_AUTOPAIR_COUNTDOWN,
                            TIME_TO_START_AUTOPAIR_COUNT);
                } else {

                    // Start timer count down for exiting activity.
                    if (DEBUG) Log.d(TAG, "starting auto-exit timer");
                    mAutoExitHandler.postDelayed(mAutoExitRunnable, EXIT_TIMEOUT_MILLIS);

                    if (mActions.size() > 1) {
                        // More than one device found, cancel auto pair
                        cancelPairingCountdown();

                        if (!mShowingMultiFragment && !mFragmentTransactionPending) {
                            if (mActionsAnimationDone) {
                                switchToMultipleDevicesFragment();
                            } else {
                                mFragmentTransactionPending = true;
                            }
                        }
                    }
               }
            }
        }
    }

    private void cancelPairingCountdown() {
        // Cancel countdown
        mMsgHandler.removeMessages(MSG_AUTOPAIR_TICK);
        mMsgHandler.removeMessages(MSG_START_AUTOPAIR_COUNTDOWN);
        if (mAutoPairText != null) {
            mAutoPairText.setVisibility(View.GONE);
        }
    }

    protected void switchToMultipleDevicesFragment() {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        mContentFragment = AddAccessoryContentFragment.newInstance(true);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.content_fragment, mContentFragment);
        ft.disallowAddToBackStack();

        ft.commit();
        mMsgHandler.sendEmptyMessageDelayed(MSG_MULTIPAIR_BLINK, BLINK_START);
        mShowingMultiFragment = true;
    }

    private void setTimeout(int timeout) {
        cancelTimeout();
        mMsgHandler.sendEmptyMessageDelayed(MSG_OP_TIMEOUT, timeout);
    }

    private void cancelTimeout() {
        mMsgHandler.removeMessages(MSG_OP_TIMEOUT);
    }

    private void animateActionsIn() {
        prepareAndAnimateView(mContentView, 1f, mViewOffset / 2, 0, ANIMATE_IN_DURATION,
                new DecelerateInterpolator(1.0f), true);
        prepareAndAnimateView(mActionView, 1f, mViewOffset, 0, ANIMATE_IN_DURATION,
                new DecelerateInterpolator(1.0f), false);
    }

    protected void startAutoPairing() {
        if (mActions.size() > 0) {
            onActionClicked(mActions.get(0));
        }
    }

    private void btDeviceClicked(String clickedAddress) {
        if (mBtPairer != null && !mBtPairer.isInProgress()) {
            if (mBtPairer.getStatus() == BluetoothDevicePairer.STATUS_WAITING_TO_PAIR &&
                    mBtPairer.getTargetDevice() != null) {
                cancelBtPairing();
            } else {
                if (DEBUG) {
                    Log.d(TAG, "Looking for " + clickedAddress +
                            " in available devices to start pairing");
                }
                for (BluetoothDevice target : mBtDevices) {
                    if (target.getAddress().equalsIgnoreCase(clickedAddress)) {
                        if (DEBUG) {
                            Log.d(TAG, "Found it!");
                        }
                        mCancelledAddress = ADDRESS_NONE;
                        setPairingBluetooth(true);
                        mBtPairer.startPairing(target);
                        break;
                    }
                }
            }
        }
    }

    private void cancelBtPairing() {
        // cancel current request to pair
        if (mBtPairer != null) {
            if (mBtPairer.getTargetDevice() != null) {
                mCancelledAddress = mBtPairer.getTargetDevice().getAddress();
            } else {
                mCancelledAddress = ADDRESS_NONE;
            }
            mBtPairer.cancelPairing();
        }
        mPairingSuccess = false;
        setPairingBluetooth(false);
        mMsgHandler.sendEmptyMessageDelayed(MSG_REMOVE_CANCELED,
                CANCEL_MESSAGE_TIMEOUT);
    }

    private void setPairingBluetooth(boolean pairing) {
        if (mPairingBluetooth != pairing) {
            mPairingBluetooth = pairing;
        }
    }

    private void startBluetoothPairer() {
        stopBluetoothPairer();
        mBtPairer = new BluetoothDevicePairer(this, this);
        mBtPairer.start();

        mBtPairer.disableAutoPairing();

        mPairingSuccess = false;
        statusChanged();
    }

    private void stopBluetoothPairer() {
        if (mBtPairer != null) {
            mBtPairer.setListener(null);
            mBtPairer.dispose();
            mBtPairer = null;
        }
    }

    private String getMessageForStatus(int status) {
        final int msgId;
        String msg;

        switch (status) {
            case BluetoothDevicePairer.STATUS_WAITING_TO_PAIR:
            case BluetoothDevicePairer.STATUS_PAIRING:
                msgId = R.string.accessory_state_pairing;
                break;
            case BluetoothDevicePairer.STATUS_CONNECTING:
                msgId = R.string.accessory_state_connecting;
                break;
            case BluetoothDevicePairer.STATUS_ERROR:
                msgId = R.string.accessory_state_error;
                break;
            default:
                return "";
        }

        msg = getString(msgId);

        return msg;
    }

    @Override
    public void statusChanged() {
        synchronized (mLock) {
            if (mBtPairer == null) return;

            int numDevices = mBtPairer.getAvailableDevices().size();
            int status = mBtPairer.getStatus();
            int oldStatus = mPreviousStatus;
            mPreviousStatus = status;

            String address = mBtPairer.getTargetDevice() == null ? ADDRESS_NONE :
                    mBtPairer.getTargetDevice().getAddress();

            if (DEBUG) {
                String state = "?";
                switch (status) {
                    case BluetoothDevicePairer.STATUS_NONE:
                        state = "BluetoothDevicePairer.STATUS_NONE";
                        break;
                    case BluetoothDevicePairer.STATUS_SCANNING:
                        state = "BluetoothDevicePairer.STATUS_SCANNING";
                        break;
                    case BluetoothDevicePairer.STATUS_WAITING_TO_PAIR:
                        state = "BluetoothDevicePairer.STATUS_WAITING_TO_PAIR";
                        break;
                    case BluetoothDevicePairer.STATUS_PAIRING:
                        state = "BluetoothDevicePairer.STATUS_PAIRING";
                        break;
                    case BluetoothDevicePairer.STATUS_CONNECTING:
                        state = "BluetoothDevicePairer.STATUS_CONNECTING";
                        break;
                    case BluetoothDevicePairer.STATUS_ERROR:
                        state = "BluetoothDevicePairer.STATUS_ERROR";
                        break;
                }
                long time = mBtPairer.getNextStageTime() - SystemClock.elapsedRealtime();
                Log.d(TAG, "Update received, number of devices:" + numDevices + " state: " +
                        state + " target device: " + address + " time to next event: " + time);
            }

            mBtDevices.clear();
            for (BluetoothDevice device : mBtPairer.getAvailableDevices()) {
                mBtDevices.add(device);
            }

            cancelTimeout();

            switch (status) {
                case BluetoothDevicePairer.STATUS_NONE:
                    // if we just connected to something or just tried to connect
                    // to something, restart scanning just in case the user wants
                    // to pair another device.
                    if (oldStatus == BluetoothDevicePairer.STATUS_CONNECTING) {
                        if (mPairingSuccess) {
                            // Pairing complete
                            mCurrentTargetStatus = getString(R.string.accessory_state_paired);
                            mMsgHandler.sendEmptyMessage(MSG_UPDATE_VIEW);
                            mMsgHandler.sendEmptyMessageDelayed(MSG_PAIRING_COMPLETE,
                                    DONE_MESSAGE_TIMEOUT);
                            mDone = true;
                            if (mAnimation != null) {
                                mAnimation.setOneShot(true);
                            }

                            // Done, return here and just wait for the message
                            // to close the activity
                            return;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "Invalidating and restarting.");
                        }

                        mBtPairer.invalidateDevice(mBtPairer.getTargetDevice());
                        mBtPairer.start();
                        mBtPairer.cancelPairing();
                        setPairingBluetooth(false);

                        // if this looks like a successful connection run, reflect
                        // this in the UI, otherwise use the default message
                        if (!mPairingSuccess && BluetoothDevicePairer.hasValidInputDevice(this)) {
                            mPairingSuccess = true;
                        }
                    }
                    break;
                case BluetoothDevicePairer.STATUS_SCANNING:
                    mPairingSuccess = false;
                    break;
                case BluetoothDevicePairer.STATUS_WAITING_TO_PAIR:
                    break;
                case BluetoothDevicePairer.STATUS_PAIRING:
                    // reset the pairing success value since this is now a new
                    // pairing run
                    mPairingSuccess = true;
                    setTimeout(PAIR_OPERATION_TIMEOUT);
                    break;
                case BluetoothDevicePairer.STATUS_CONNECTING:
                    setTimeout(CONNECT_OPERATION_TIMEOUT);
                    break;
                case BluetoothDevicePairer.STATUS_ERROR:
                    mPairingSuccess = false;
                    setPairingBluetooth(false);
                    if (mNoInputMode) {
                        clearDeviceList();
                    }
                    break;
            }

            mCurrentTargetAddress = address;
            mCurrentTargetStatus = getMessageForStatus(status);
            mMsgHandler.sendEmptyMessage(MSG_UPDATE_VIEW);
        }
    }

    private void clearDeviceList() {
        mBtDevices.clear();
        mBtPairer.clearDeviceList();
    }

    private void stopActivity() {
        stopBluetoothPairer();
        mMsgHandler.removeCallbacksAndMessages(null);
        mAnimateOnStart = true;

        // Forcing this activity to finish in OnStop, to make sure it always gets created
        // fresh, since it has different behavior depending on the intent that launched
        // it (Settings vs HW button press).
        Log.d(TAG, "Calling finish() on activity.onStop().");
        finish();
    }

    private void handlePairingTimeout() {
        if (mPairingInBackground) {
            stopActivity();
        } else {
            // Either Pairing or Connecting timeout out.
            // Display error message and post delayed message to the scanning process.
            mPairingSuccess = false;
            if (mBtPairer != null) {
                mBtPairer.cancelPairing();
            }
            mCurrentTargetStatus = getString(R.string.accessory_state_error);
            mMsgHandler.sendEmptyMessage(MSG_UPDATE_VIEW);
            mMsgHandler.sendEmptyMessageDelayed(MSG_RESTART, RESTART_DELAY);
        }
    }

}
