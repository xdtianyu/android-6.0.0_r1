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
 * limitations under the License.
 */

package com.android.cts.verifier.audio;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.content.Context;

import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import android.os.Bundle;
import android.os.Handler;

import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.TextView;

/**
 * Tests AudioTrack and AudioRecord (re)Routing messages.
 */
public class AudioRoutingNotificationsActivity extends PassFailButtons.Activity {
    private static final String TAG = "AudioRoutingNotificationsActivity";

    Context mContext;

    OnBtnClickListener mBtnClickListener = new OnBtnClickListener();

    int mNumTrackNotifications = 0;
    int mNumRecordNotifications = 0;

    TrivialPlayer mAudioPlayer = new TrivialPlayer();
    TrivialRecorder mAudioRecorder = new TrivialRecorder();

    private class OnBtnClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.audio_routingnotification_playBtn:
                    Log.i(TAG, "audio_routingnotification_playBtn");
                    mAudioPlayer.start();
                    break;

                case R.id.audio_routingnotification_playStopBtn:
                    Log.i(TAG, "audio_routingnotification_playStopBtn");
                    mAudioPlayer.stop();
                    break;

                case R.id.audio_routingnotification_recordBtn:
                    break;

                case R.id.audio_routingnotification_recordStopBtn:
                    break;
            }
        }
    }

    private class AudioTrackRoutingChangeListener implements AudioTrack.OnRoutingChangedListener {
        public void onRoutingChanged(AudioTrack audioTrack) {
            mNumTrackNotifications++;
            TextView textView =
                (TextView)findViewById(R.id.audio_routingnotification_audioTrack_change);
            String msg = mContext.getResources().getString(
                    R.string.audio_routingnotification_trackRoutingMsg);
            AudioDeviceInfo routedDevice = audioTrack.getRoutedDevice();
            CharSequence deviceName = routedDevice != null ? routedDevice.getProductName() : "none";
            int deviceType = routedDevice != null ? routedDevice.getType() : -1;
            textView.setText(msg + " - " +
                             deviceName + " [0x" + Integer.toHexString(deviceType) + "]" +
                             " - " + mNumTrackNotifications);
        }
    }

    private class AudioRecordRoutingChangeListener implements AudioRecord.OnRoutingChangedListener {
        public void onRoutingChanged(AudioRecord audioRecord) {
            mNumRecordNotifications++;
            TextView textView =
                    (TextView)findViewById(R.id.audio_routingnotification_audioRecord_change);
            String msg = mContext.getResources().getString(
                    R.string.audio_routingnotification_recordRoutingMsg);
            AudioDeviceInfo routedDevice = audioRecord.getRoutedDevice();
            CharSequence deviceName = routedDevice != null ? routedDevice.getProductName() : "none";
            int deviceType = routedDevice != null ? routedDevice.getType() : -1;
            textView.setText(msg + " - " +
                             deviceName + " [0x" + Integer.toHexString(deviceType) + "]" +
                             " - " + mNumRecordNotifications);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_routingnotifications_test);

        Button btn;
        btn = (Button)findViewById(R.id.audio_routingnotification_playBtn);
        btn.setOnClickListener(mBtnClickListener);
        btn = (Button)findViewById(R.id.audio_routingnotification_playStopBtn);
        btn.setOnClickListener(mBtnClickListener);
        btn = (Button)findViewById(R.id.audio_routingnotification_recordBtn);
        btn.setOnClickListener(mBtnClickListener);
        btn = (Button)findViewById(R.id.audio_routingnotification_recordStopBtn);
        btn.setOnClickListener(mBtnClickListener);

        mContext = this;

        AudioTrack audioTrack = mAudioPlayer.getAudioTrack();
        audioTrack.addOnRoutingChangedListener(
            new AudioTrackRoutingChangeListener(), new Handler());

        AudioRecord audioRecord = mAudioRecorder.getAudioRecord();
        audioRecord.addOnRoutingChangedListener(
            new AudioRecordRoutingChangeListener(), new Handler());

        setPassFailButtonClickListeners();
    }

    @Override
    public void onBackPressed () {
        mAudioPlayer.shutDown();
        mAudioRecorder.shutDown();
        super.onBackPressed();
    }
}
