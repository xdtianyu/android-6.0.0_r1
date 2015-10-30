/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.media.cts;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.content.pm.PackageManager;
import android.cts.util.CtsAndroidTestCase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaSyncEvent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.cts.util.ReportLog;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;

public class AudioRecordTest extends CtsAndroidTestCase {
    private final static String TAG = "AudioRecordTest";
    private AudioRecord mAudioRecord;
    private int mHz = 44100;
    private boolean mIsOnMarkerReachedCalled;
    private boolean mIsOnPeriodicNotificationCalled;
    private boolean mIsHandleMessageCalled;
    private Looper mLooper;
    // For doTest
    private int mMarkerPeriodInFrames;
    private int mMarkerPosition;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            mIsHandleMessageCalled = true;
            super.handleMessage(msg);
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!hasMicrophone()) {
            return;
        }

        /*
         * InstrumentationTestRunner.onStart() calls Looper.prepare(), which creates a looper
         * for the current thread. However, since we don't actually call loop() in the test,
         * any messages queued with that looper will never be consumed. Therefore, we must
         * create the instance in another thread, either without a looper, so the main looper is
         * used, or with an active looper.
         */
        Thread t = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mLooper = Looper.myLooper();
                synchronized(this) {
                    mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, mHz,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            AudioRecord.getMinBufferSize(mHz,
                                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT) * 10);
                    this.notify();
                }
                Looper.loop();
            }
        };
        synchronized(t) {
            t.start(); // will block until we wait
            t.wait();
        }
        assertNotNull(mAudioRecord);
    }

    @Override
    protected void tearDown() throws Exception {
        if (hasMicrophone()) {
            mAudioRecord.release();
            mLooper.quit();
        }
        super.tearDown();
    }

    private void reset() {
        mIsOnMarkerReachedCalled = false;
        mIsOnPeriodicNotificationCalled = false;
        mIsHandleMessageCalled = false;
    }

    public void testAudioRecordProperties() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, mAudioRecord.getAudioFormat());
        assertEquals(MediaRecorder.AudioSource.DEFAULT, mAudioRecord.getAudioSource());
        assertEquals(1, mAudioRecord.getChannelCount());
        assertEquals(AudioFormat.CHANNEL_IN_MONO,
                mAudioRecord.getChannelConfiguration());
        assertEquals(AudioRecord.STATE_INITIALIZED, mAudioRecord.getState());
        assertEquals(mHz, mAudioRecord.getSampleRate());
        assertEquals(AudioRecord.RECORDSTATE_STOPPED, mAudioRecord.getRecordingState());

        int bufferSize = AudioRecord.getMinBufferSize(mHz,
                AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
        assertTrue(bufferSize > 0);
    }

    public void testAudioRecordOP() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        final int SLEEP_TIME = 10;
        final int RECORD_TIME = 10000;
        assertEquals(AudioRecord.STATE_INITIALIZED, mAudioRecord.getState());

        int markerInFrames = mAudioRecord.getSampleRate() / 2;
        assertEquals(AudioRecord.SUCCESS,
                mAudioRecord.setNotificationMarkerPosition(markerInFrames));
        assertEquals(markerInFrames, mAudioRecord.getNotificationMarkerPosition());
        int periodInFrames = mAudioRecord.getSampleRate();
        assertEquals(AudioRecord.SUCCESS,
                mAudioRecord.setPositionNotificationPeriod(periodInFrames));
        assertEquals(periodInFrames, mAudioRecord.getPositionNotificationPeriod());
        OnRecordPositionUpdateListener listener = new OnRecordPositionUpdateListener() {

            public void onMarkerReached(AudioRecord recorder) {
                mIsOnMarkerReachedCalled = true;
            }

            public void onPeriodicNotification(AudioRecord recorder) {
                mIsOnPeriodicNotificationCalled = true;
            }
        };
        mAudioRecord.setRecordPositionUpdateListener(listener);

        // use byte array as buffer
        final int BUFFER_SIZE = 102400;
        byte[] byteData = new byte[BUFFER_SIZE];
        long time = System.currentTimeMillis();
        mAudioRecord.startRecording();
        assertEquals(AudioRecord.RECORDSTATE_RECORDING, mAudioRecord.getRecordingState());
        while (System.currentTimeMillis() - time < RECORD_TIME) {
            Thread.sleep(SLEEP_TIME);
            mAudioRecord.read(byteData, 0, BUFFER_SIZE);
        }
        mAudioRecord.stop();
        assertEquals(AudioRecord.RECORDSTATE_STOPPED, mAudioRecord.getRecordingState());
        assertTrue(mIsOnMarkerReachedCalled);
        assertTrue(mIsOnPeriodicNotificationCalled);
        reset();

        // use short array as buffer
        short[] shortData = new short[BUFFER_SIZE];
        time = System.currentTimeMillis();
        mAudioRecord.startRecording();
        assertEquals(AudioRecord.RECORDSTATE_RECORDING, mAudioRecord.getRecordingState());
        while (System.currentTimeMillis() - time < RECORD_TIME) {
            Thread.sleep(SLEEP_TIME);
            mAudioRecord.read(shortData, 0, BUFFER_SIZE);
        }
        mAudioRecord.stop();
        assertEquals(AudioRecord.RECORDSTATE_STOPPED, mAudioRecord.getRecordingState());
        assertTrue(mIsOnMarkerReachedCalled);
        assertTrue(mIsOnPeriodicNotificationCalled);
        reset();

        // use ByteBuffer as buffer
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        time = System.currentTimeMillis();
        mAudioRecord.startRecording();
        assertEquals(AudioRecord.RECORDSTATE_RECORDING, mAudioRecord.getRecordingState());
        while (System.currentTimeMillis() - time < RECORD_TIME) {
            Thread.sleep(SLEEP_TIME);
            mAudioRecord.read(byteBuffer, BUFFER_SIZE);
        }
        mAudioRecord.stop();
        assertEquals(AudioRecord.RECORDSTATE_STOPPED, mAudioRecord.getRecordingState());
        assertTrue(mIsOnMarkerReachedCalled);
        assertTrue(mIsOnPeriodicNotificationCalled);
        reset();

        // use handler
        final Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                mIsHandleMessageCalled = true;
                super.handleMessage(msg);
            }
        };

        mAudioRecord.setRecordPositionUpdateListener(listener, handler);
        time = System.currentTimeMillis();
        mAudioRecord.startRecording();
        assertEquals(AudioRecord.RECORDSTATE_RECORDING, mAudioRecord.getRecordingState());
        while (System.currentTimeMillis() - time < RECORD_TIME) {
            Thread.sleep(SLEEP_TIME);
            mAudioRecord.read(byteData, 0, BUFFER_SIZE);
        }
        mAudioRecord.stop();
        assertEquals(AudioRecord.RECORDSTATE_STOPPED, mAudioRecord.getRecordingState());
        assertTrue(mIsOnMarkerReachedCalled);
        assertTrue(mIsOnPeriodicNotificationCalled);
        // The handler argument is only ever used for getting the associated Looper
        assertFalse(mIsHandleMessageCalled);

        mAudioRecord.release();
        assertEquals(AudioRecord.STATE_UNINITIALIZED, mAudioRecord.getState());
    }

    public void testAudioRecordResamplerMono8Bit() throws Exception {
        doTest("ResamplerResamplerMono8Bit", true /*localRecord*/, false /*customHandler*/,
                1 /*periodsPerSecond*/, 1 /*markerPeriodsPerSecond*/,
                false /*useByteBuffer*/,  false /*blocking*/,
                false /*auditRecording*/, false /*isChannelIndex*/, 88200 /*TEST_SR*/,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT);
    }

    public void testAudioRecordResamplerStereo8Bit() throws Exception {
        doTest("ResamplerStereo8Bit", true /*localRecord*/, false /*customHandler*/,
                0 /*periodsPerSecond*/, 3 /*markerPeriodsPerSecond*/,
                true /*useByteBuffer*/,  true /*blocking*/,
                false /*auditRecording*/, false /*isChannelIndex*/, 45000 /*TEST_SR*/,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_8BIT);
    }

    public void testAudioRecordLocalMono16Bit() throws Exception {
        doTest("LocalMono16Bit", true /*localRecord*/, false /*customHandler*/,
                30 /*periodsPerSecond*/, 2 /*markerPeriodsPerSecond*/,
                false /*useByteBuffer*/, true /*blocking*/,
                false /*auditRecording*/, false /*isChannelIndex*/, 8000 /*TEST_SR*/,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    public void testAudioRecordStereo16Bit() throws Exception {
        doTest("Stereo16Bit", false /*localRecord*/, false /*customHandler*/,
                2 /*periodsPerSecond*/, 2 /*markerPeriodsPerSecond*/,
                false /*useByteBuffer*/, false /*blocking*/,
                false /*auditRecording*/, false /*isChannelIndex*/, 17000 /*TEST_SR*/,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    }

    public void testAudioRecordMonoFloat() throws Exception {
        doTest("MonoFloat", false /*localRecord*/, true /*customHandler*/,
                30 /*periodsPerSecond*/, 2 /*markerPeriodsPerSecond*/,
                false /*useByteBuffer*/, true /*blocking*/,
                false /*auditRecording*/, false /*isChannelIndex*/, 32000 /*TEST_SR*/,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT);
    }

    public void testAudioRecordLocalNonblockingStereoFloat() throws Exception {
        doTest("LocalNonblockingStereoFloat", true /*localRecord*/, true /*customHandler*/,
                2 /*periodsPerSecond*/, 0 /*markerPeriodsPerSecond*/,
                false /*useByteBuffer*/, false /*blocking*/,
                false /*auditRecording*/, false /*isChannelIndex*/, 48000 /*TEST_SR*/,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_FLOAT);
    }

    // Audit modes work best with non-blocking mode
    public void testAudioRecordAuditByteBufferResamplerStereoFloat() throws Exception {
        doTest("AuditByteBufferResamplerStereoFloat",
                false /*localRecord*/, true /*customHandler*/,
                2 /*periodsPerSecond*/, 0 /*markerPeriodsPerSecond*/,
                true /*useByteBuffer*/, false /*blocking*/,
                true /*auditRecording*/, false /*isChannelIndex*/, 96000 /*TEST_SR*/,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_FLOAT);
    }

    public void testAudioRecordAuditChannelIndexMonoFloat() throws Exception {
        doTest("AuditChannelIndexMonoFloat", true /*localRecord*/, true /*customHandler*/,
                2 /*periodsPerSecond*/, 0 /*markerPeriodsPerSecond*/,
                false /*useByteBuffer*/, false /*blocking*/,
                true /*auditRecording*/, true /*isChannelIndex*/, 47000 /*TEST_SR*/,
                (1 << 0) /* 1 channel */, AudioFormat.ENCODING_PCM_FLOAT);
    }

    // Audit buffers can run out of space with high sample rate,
    // so keep the channels and pcm encoding low
    public void testAudioRecordAuditChannelIndex2() throws Exception {
        doTest("AuditChannelIndex2", true /*localRecord*/, true /*customHandler*/,
                2 /*periodsPerSecond*/, 0 /*markerPeriodsPerSecond*/,
                false /*useByteBuffer*/, false /*blocking*/,
                true /*auditRecording*/, true /*isChannelIndex*/, 192000 /*TEST_SR*/,
                (1 << 0) | (1 << 2) /* 2 channels, gap in middle */,
                AudioFormat.ENCODING_PCM_8BIT);
    }

    // Audit buffers can run out of space with high numbers of channels,
    // so keep the sample rate low.
    public void testAudioRecordAuditChannelIndex5() throws Exception {
        doTest("AuditChannelIndex5", true /*localRecord*/, true /*customHandler*/,
                2 /*periodsPerSecond*/, 0 /*markerPeriodsPerSecond*/,
                false /*useByteBuffer*/, false /*blocking*/,
                true /*auditRecording*/, true /*isChannelIndex*/, 16000 /*TEST_SR*/,
                (1 << 0) | (1 << 1) | (1 << 2) | (1 << 3) | (1 << 4)  /* 5 channels */,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    // Test AudioRecord.Builder to verify the observed configuration of an AudioRecord built with
    // an empty Builder matches the documentation / expected values
    public void testAudioRecordBuilderDefault() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        // constants for test
        final String TEST_NAME = "testAudioRecordBuilderDefault";
        // expected values below match the AudioRecord.Builder documentation
        final int expectedCapturePreset = MediaRecorder.AudioSource.DEFAULT;
        final String rateStr = new AudioManager(getContext())
                .getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        final int expectedRate = Integer.valueOf(rateStr).intValue();
        final int expectedChannel = AudioFormat.CHANNEL_IN_MONO;
        final int expectedEncoding = AudioFormat.ENCODING_PCM_16BIT;
        final int expectedState = AudioRecord.STATE_INITIALIZED;
        // use builder with default values
        final AudioRecord rec = new AudioRecord.Builder().build();
        // save results
        final int observedRate = rec.getSampleRate();
        final int observedSource = rec.getAudioSource();
        final int observedChannel = rec.getChannelConfiguration();
        final int observedEncoding = rec.getAudioFormat();
        final int observedState = rec.getState();
        // release recorder before the test exits (either successfully or with an exception)
        rec.release();
        // compare results
        assertEquals(TEST_NAME + ": default capture preset", expectedCapturePreset, observedSource);
        assertEquals(TEST_NAME + ": default rate", expectedRate, observedRate);
        assertEquals(TEST_NAME + ": default channel config", expectedChannel, observedChannel);
        assertEquals(TEST_NAME + ": default encoding", expectedEncoding, observedEncoding);
        assertEquals(TEST_NAME + ": state", expectedState, observedState);
    }

    // Test AudioRecord.Builder to verify the observed configuration of an AudioRecord built with
    // an incomplete AudioFormat matches the documentation / expected values
    public void testAudioRecordBuilderPartialFormat() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        // constants for test
        final String TEST_NAME = "testAudioRecordBuilderPartialFormat";
        final int expectedRate = 16000;
        final int expectedState = AudioRecord.STATE_INITIALIZED;
        // expected values below match the AudioRecord.Builder documentation
        final int expectedChannel = AudioFormat.CHANNEL_IN_MONO;
        final int expectedEncoding = AudioFormat.ENCODING_PCM_16BIT;
        // use builder with a partial audio format
        final AudioRecord rec = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(expectedRate).build())
                .build();
        // save results
        final int observedRate = rec.getSampleRate();
        final int observedChannel = rec.getChannelConfiguration();
        final int observedEncoding = rec.getAudioFormat();
        final int observedState = rec.getState();
        // release recorder before the test exits (either successfully or with an exception)
        rec.release();
        // compare results
        assertEquals(TEST_NAME + ": configured rate", expectedRate, observedRate);
        assertEquals(TEST_NAME + ": default channel config", expectedChannel, observedChannel);
        assertEquals(TEST_NAME + ": default encoding", expectedEncoding, observedEncoding);
        assertEquals(TEST_NAME + ": state", expectedState, observedState);
    }

    // Test AudioRecord.Builder to verify the observed configuration of an AudioRecord matches
    // the parameters used in the builder
    public void testAudioRecordBuilderParams() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        // constants for test
        final String TEST_NAME = "testAudioRecordBuilderParams";
        final int expectedRate = 8000;
        final int expectedChannel = AudioFormat.CHANNEL_IN_MONO;
        final int expectedChannelCount = 1;
        final int expectedEncoding = AudioFormat.ENCODING_PCM_16BIT;
        final int expectedSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        final int expectedState = AudioRecord.STATE_INITIALIZED;
        // use builder with expected parameters
        final AudioRecord rec = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(expectedRate)
                        .setChannelMask(expectedChannel)
                        .setEncoding(expectedEncoding)
                        .build())
                .setAudioSource(expectedSource)
                .build();
        // save results
        final int observedRate = rec.getSampleRate();
        final int observedChannel = rec.getChannelConfiguration();
        final int observedChannelCount = rec.getChannelCount();
        final int observedEncoding = rec.getAudioFormat();
        final int observedSource = rec.getAudioSource();
        final int observedState = rec.getState();
        // release recorder before the test exits (either successfully or with an exception)
        rec.release();
        // compare results
        assertEquals(TEST_NAME + ": configured rate", expectedRate, observedRate);
        assertEquals(TEST_NAME + ": configured channel config", expectedChannel, observedChannel);
        assertEquals(TEST_NAME + ": configured encoding", expectedEncoding, observedEncoding);
        assertEquals(TEST_NAME + ": implicit channel count", expectedChannelCount,
                observedChannelCount);
        assertEquals(TEST_NAME + ": configured source", expectedSource, observedSource);
        assertEquals(TEST_NAME + ": state", expectedState, observedState);
    }

    // Test AudioRecord to ensure we can build after a failure.
    public void testAudioRecordBufferSize() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        // constants for test
        final String TEST_NAME = "testAudioRecordBufferSize";

        // use builder with parameters that should fail
        final int superBigBufferSize = 1 << 28;
        try {
            final AudioRecord record = new AudioRecord.Builder()
                .setBufferSizeInBytes(superBigBufferSize)
                .build();
            record.release();
            fail(TEST_NAME + ": should throw exception on failure");
        } catch (UnsupportedOperationException e) {
            ;
        }

        // we should be able to create again with minimum buffer size
        final int verySmallBufferSize = 2 * 3 * 4; // frame size multiples
        final AudioRecord record2 = new AudioRecord.Builder()
                .setBufferSizeInBytes(verySmallBufferSize)
                .build();

        final int observedState2 = record2.getState();
        final int observedBufferSize2 = record2.getBufferSizeInFrames();
        record2.release();

        // succeeds for minimum buffer size
        assertEquals(TEST_NAME + ": state", AudioRecord.STATE_INITIALIZED, observedState2);
        // should force the minimum size buffer which is > 0
        assertTrue(TEST_NAME + ": buffer frame count", observedBufferSize2 > 0);
    }

    public void testSynchronizedRecord() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        final String TEST_NAME = "testSynchronizedRecord";
        AudioTrack track = null;
        AudioRecord record = null;

        try {
            // 1. create a static AudioTrack.
            final int PLAYBACK_TIME_IN_MS = 2000; /* ms duration. */
            final int PLAYBACK_SAMPLE_RATE = 8000; /* in hz */
            AudioFormat format = new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                    .setSampleRate(PLAYBACK_SAMPLE_RATE)
                    .build();
            final int frameCount = AudioHelper.frameCountFromMsec(PLAYBACK_TIME_IN_MS, format);
            final int frameSize = AudioHelper.frameSizeFromFormat(format);
            track = new AudioTrack.Builder()
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(frameCount * frameSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            // create float array and write it
            final int sampleCount = frameCount * format.getChannelCount();
            byte[] vab = AudioHelper.createSoundDataInByteArray(
                    sampleCount, PLAYBACK_SAMPLE_RATE, 600 /* frequency */, 0 /* sweep */);
            assertEquals(TEST_NAME, vab.length,
                    track.write(vab, 0 /* offsetInBytes */, vab.length,
                            AudioTrack.WRITE_NON_BLOCKING));
            final int trackSessionId = track.getAudioSessionId();

            // 2. create an AudioRecord to sync off of AudioTrack completion.
            final int RECORD_TIME_IN_MS = 2000;
            final int RECORD_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
            final int RECORD_CHANNEL_MASK = AudioFormat.CHANNEL_IN_STEREO;
            final int RECORD_SAMPLE_RATE = 44100;
            record = new AudioRecord.Builder()
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(RECORD_SAMPLE_RATE)
                            .setChannelMask(RECORD_CHANNEL_MASK)
                            .setEncoding(RECORD_ENCODING)
                            .build())
                    .build();
            // AudioRecord creation may have silently failed, check state now
            assertEquals(TEST_NAME, AudioRecord.STATE_INITIALIZED, record.getState());

            // 3. create a MediaSyncEvent
            // This MediaSyncEvent checks playback completion of an AudioTrack
            // (or MediaPlayer, or ToneGenerator) based on its audio session id.
            //
            // Note: when synchronizing record from a MediaSyncEvent
            // (1) You need to be "close" to the end of the associated AudioTrack.
            // If the track does not complete in 30 seconds, recording begins regardless.
            // (actual delay limit may vary).
            //
            // (2) Track completion may be triggered by pause() as well as stop()
            // or when a static AudioTrack completes playback.
            //
            final int eventType = MediaSyncEvent.SYNC_EVENT_PRESENTATION_COMPLETE;
            MediaSyncEvent event = MediaSyncEvent.createEvent(eventType)
                    .setAudioSessionId(trackSessionId);
            assertEquals(TEST_NAME, trackSessionId, event.getAudioSessionId());
            assertEquals(TEST_NAME, eventType, event.getType());

            // 4. now set the AudioTrack playing and start the recording synchronized
            track.play();
            // start recording.  Recording state turns to RECORDSTATE_RECORDING immediately
            // but the data read() only occurs after the AudioTrack completes.
            record.startRecording(event);
            assertEquals(TEST_NAME,
                    AudioRecord.RECORDSTATE_RECORDING, record.getRecordingState());
            long startTime = System.currentTimeMillis();

            // 5. get record data.
            // For our tests, we could set test duration by timed sleep or by # frames received.
            // Since we don't know *exactly* when AudioRecord actually begins recording,
            // we end the test by # frames read.
            final int numChannels =
                    AudioFormat.channelCountFromInChannelMask(RECORD_CHANNEL_MASK);
            final int bytesPerSample = AudioFormat.getBytesPerSample(RECORD_ENCODING);
            final int bytesPerFrame = numChannels * bytesPerSample;
            // careful about integer overflow in the formula below:
            final int targetSamples =
                    (int)((long)RECORD_TIME_IN_MS * RECORD_SAMPLE_RATE * numChannels / 1000);
            final int BUFFER_FRAMES = 512;
            final int BUFFER_SAMPLES = BUFFER_FRAMES * numChannels;

            // After starting, there is no guarantee when the first frame of data is read.
            long firstSampleTime = 0;
            int samplesRead = 0;

            // For 16 bit data, use shorts
            short[] shortData = new short[BUFFER_SAMPLES];
            while (samplesRead < targetSamples) {
                // the first time through, we read a single frame.
                // this sets the recording anchor position.
                int amount = samplesRead == 0 ? numChannels :
                    Math.min(BUFFER_SAMPLES, targetSamples - samplesRead);
                int ret = record.read(shortData, 0, amount);
                assertEquals(TEST_NAME, amount, ret);
                if (samplesRead == 0 && ret > 0) {
                    firstSampleTime = System.currentTimeMillis();
                }
                samplesRead += ret;
                // sanity check: elapsed time cannot be more than a second
                // than what we expect.
                assertTrue(System.currentTimeMillis() - startTime <=
                        PLAYBACK_TIME_IN_MS + RECORD_TIME_IN_MS + 1000);
            }

            // 6. We've read all the frames, now check the timing.
            final long endTime = System.currentTimeMillis();
            //Log.d(TEST_NAME, "first sample time " + (firstSampleTime - startTime)
            //        + " test time " + (endTime - firstSampleTime));
            //
            // Verify recording starts within 400 ms of AudioTrack completion (typical 180ms)
            // Verify recording completes within 50 ms of expected test time (typical 20ms)
            assertEquals(TEST_NAME, PLAYBACK_TIME_IN_MS, firstSampleTime - startTime, 400);
            assertEquals(TEST_NAME, RECORD_TIME_IN_MS, endTime - firstSampleTime, 50);

            record.stop();
            assertEquals(TEST_NAME, AudioRecord.RECORDSTATE_STOPPED, record.getRecordingState());
        } finally {
            if (record != null) {
                record.release();
                record = null;
            }
            if (track != null) {
                track.release();
                track = null;
            }
        }
    }

    private AudioRecord createAudioRecord(
            int audioSource, int sampleRateInHz,
            int channelConfig, int audioFormat, int bufferSizeInBytes,
            boolean auditRecording, boolean isChannelIndex) {
        final AudioRecord record;
        if (auditRecording) {
            record = new AudioHelper.AudioRecordAudit(
                    audioSource, sampleRateInHz, channelConfig,
                    audioFormat, bufferSizeInBytes, isChannelIndex);
        } else if (isChannelIndex) {
            record = new AudioRecord.Builder()
                    .setAudioFormat(new AudioFormat.Builder()
                            .setChannelIndexMask(channelConfig)
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRateInHz)
                            .build())
                    .setBufferSizeInBytes(bufferSizeInBytes)
                    .build();
        } else {
            record = new AudioRecord(audioSource, sampleRateInHz, channelConfig,
                    audioFormat, bufferSizeInBytes);
        }

        // did we get the AudioRecord we expected?
        final AudioFormat format = record.getFormat();
        assertEquals(isChannelIndex ? channelConfig : AudioFormat.CHANNEL_INVALID,
                format.getChannelIndexMask());
        assertEquals(isChannelIndex ? AudioFormat.CHANNEL_INVALID : channelConfig,
                format.getChannelMask());
        assertEquals(audioFormat, format.getEncoding());
        assertEquals(sampleRateInHz, format.getSampleRate());
        final int frameSize =
                format.getChannelCount() * AudioFormat.getBytesPerSample(audioFormat);
        // our native frame count cannot be smaller than our minimum buffer size request.
        assertTrue(record.getBufferSizeInFrames() * frameSize >= bufferSizeInBytes);
        return record;
    }

    private void doTest(String reportName, boolean localRecord, boolean customHandler,
            int periodsPerSecond, int markerPeriodsPerSecond,
            boolean useByteBuffer, boolean blocking,
            final boolean auditRecording, final boolean isChannelIndex,
            final int TEST_SR, final int TEST_CONF, final int TEST_FORMAT) throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        // audit recording plays back recorded audio, so use longer test timing
        final int TEST_TIME_MS = auditRecording ? 10000 : 2000;
        final int TEST_SOURCE = MediaRecorder.AudioSource.DEFAULT;
        mIsHandleMessageCalled = false;

        // For channelIndex use one frame in bytes for buffer size.
        // This is adjusted to the minimum buffer size by native code.
        final int bufferSizeInBytes = isChannelIndex ?
                (AudioFormat.getBytesPerSample(TEST_FORMAT)
                        * AudioFormat.channelCountFromInChannelMask(TEST_CONF)) :
                AudioRecord.getMinBufferSize(TEST_SR, TEST_CONF, TEST_FORMAT);
        assertTrue(bufferSizeInBytes > 0);

        final AudioRecord record;
        final AudioHelper
                .MakeSomethingAsynchronouslyAndLoop<AudioRecord> makeSomething;

        if (localRecord) {
            makeSomething = null;
            record = createAudioRecord(TEST_SOURCE, TEST_SR, TEST_CONF,
                    TEST_FORMAT, bufferSizeInBytes, auditRecording, isChannelIndex);
        } else {
            makeSomething =
                    new AudioHelper.MakeSomethingAsynchronouslyAndLoop<AudioRecord>(
                            new AudioHelper.MakesSomething<AudioRecord>() {
                                @Override
                                public AudioRecord makeSomething() {
                                    return createAudioRecord(TEST_SOURCE, TEST_SR, TEST_CONF,
                                            TEST_FORMAT, bufferSizeInBytes, auditRecording,
                                            isChannelIndex);
                                }
                            }
                            );
           // create AudioRecord on different thread's looper.
           record = makeSomething.make();
        }

        // AudioRecord creation may have silently failed, check state now
        assertEquals(AudioRecord.STATE_INITIALIZED, record.getState());

        final MockOnRecordPositionUpdateListener listener;
        if (customHandler) {
            listener = new MockOnRecordPositionUpdateListener(record, mHandler);
        } else {
            listener = new MockOnRecordPositionUpdateListener(record);
        }

        final int updatePeriodInFrames = (periodsPerSecond == 0)
                ? 0 : TEST_SR / periodsPerSecond;
        // After starting, there is no guarantee when the first frame of data is read.
        long firstSampleTime = 0;

        // blank final variables: all successful paths will initialize the times.
        final long endTime;
        final long startTime;
        final long stopRequestTime;
        final long stopTime;

        try {
            if (markerPeriodsPerSecond != 0) {
                mMarkerPeriodInFrames = TEST_SR / markerPeriodsPerSecond;
                mMarkerPosition = mMarkerPeriodInFrames;
                assertEquals(AudioRecord.SUCCESS,
                        record.setNotificationMarkerPosition(mMarkerPosition));
            } else {
                mMarkerPeriodInFrames = 0;
            }

            assertEquals(AudioRecord.SUCCESS,
                    record.setPositionNotificationPeriod(updatePeriodInFrames));

            listener.start(TEST_SR);
            record.startRecording();
            assertEquals(AudioRecord.RECORDSTATE_RECORDING, record.getRecordingState());
            startTime = System.currentTimeMillis();

            // For our tests, we could set test duration by timed sleep or by # frames received.
            // Since we don't know *exactly* when AudioRecord actually begins recording,
            // we end the test by # frames read.
            final int numChannels =  AudioFormat.channelCountFromInChannelMask(TEST_CONF);
            final int bytesPerSample = AudioFormat.getBytesPerSample(TEST_FORMAT);
            final int bytesPerFrame = numChannels * bytesPerSample;
            // careful about integer overflow in the formula below:
            final int targetSamples = (int)((long)TEST_TIME_MS * TEST_SR * numChannels / 1000);
            final int BUFFER_FRAMES = 512;
            final int BUFFER_SAMPLES = BUFFER_FRAMES * numChannels;
            // TODO: verify behavior when buffer size is not a multiple of frame size.

            int samplesRead = 0;
            if (useByteBuffer) {
                ByteBuffer byteBuffer =
                        ByteBuffer.allocateDirect(BUFFER_SAMPLES * bytesPerSample);
                while (samplesRead < targetSamples) {
                    // the first time through, we read a single frame.
                    // this sets the recording anchor position.
                    int amount = samplesRead == 0 ? numChannels :
                        Math.min(BUFFER_SAMPLES, targetSamples - samplesRead);
                    amount *= bytesPerSample;    // in bytes
                    // read always places data at the start of the byte buffer with
                    // position and limit are ignored.  test this by setting
                    // position and limit to arbitrary values here.
                    final int lastPosition = 7;
                    final int lastLimit = 13;
                    byteBuffer.position(lastPosition);
                    byteBuffer.limit(lastLimit);
                    int ret = blocking ? record.read(byteBuffer, amount) :
                        record.read(byteBuffer, amount, AudioRecord.READ_NON_BLOCKING);
                    // so long as amount requested in bytes is a multiple of the frame size
                    // we expect the byte buffer request to be filled.  Caution: the
                    // byte buffer data will be in native endian order, not Java order.
                    if (blocking) {
                        assertEquals(amount, ret);
                    } else {
                        assertTrue("0 <= " + ret + " <= " + amount,
                                0 <= ret && ret <= amount);
                    }
                    // position, limit are not changed by read().
                    assertEquals(lastPosition, byteBuffer.position());
                    assertEquals(lastLimit, byteBuffer.limit());
                    if (samplesRead == 0 && ret > 0) {
                        firstSampleTime = System.currentTimeMillis();
                    }
                    samplesRead += ret / bytesPerSample;
                }
            } else {
                switch (TEST_FORMAT) {
                case AudioFormat.ENCODING_PCM_8BIT: {
                    // For 8 bit data, use bytes
                    byte[] byteData = new byte[BUFFER_SAMPLES];
                    while (samplesRead < targetSamples) {
                        // the first time through, we read a single frame.
                        // this sets the recording anchor position.
                        int amount = samplesRead == 0 ? numChannels :
                            Math.min(BUFFER_SAMPLES, targetSamples - samplesRead);
                        int ret = blocking ? record.read(byteData, 0, amount) :
                            record.read(byteData, 0, amount, AudioRecord.READ_NON_BLOCKING);
                        if (blocking) {
                            assertEquals(amount, ret);
                        } else {
                            assertTrue("0 <= " + ret + " <= " + amount,
                                    0 <= ret && ret <= amount);
                        }
                        if (samplesRead == 0 && ret > 0) {
                            firstSampleTime = System.currentTimeMillis();
                        }
                        samplesRead += ret;
                    }
                } break;
                case AudioFormat.ENCODING_PCM_16BIT: {
                    // For 16 bit data, use shorts
                    short[] shortData = new short[BUFFER_SAMPLES];
                    while (samplesRead < targetSamples) {
                        // the first time through, we read a single frame.
                        // this sets the recording anchor position.
                        int amount = samplesRead == 0 ? numChannels :
                            Math.min(BUFFER_SAMPLES, targetSamples - samplesRead);
                        int ret = blocking ? record.read(shortData, 0, amount) :
                            record.read(shortData, 0, amount, AudioRecord.READ_NON_BLOCKING);
                        if (blocking) {
                            assertEquals(amount, ret);
                        } else {
                            assertTrue("0 <= " + ret + " <= " + amount,
                                    0 <= ret && ret <= amount);
                        }
                        if (samplesRead == 0 && ret > 0) {
                            firstSampleTime = System.currentTimeMillis();
                        }
                        samplesRead += ret;
                    }
                } break;
                case AudioFormat.ENCODING_PCM_FLOAT: {
                    float[] floatData = new float[BUFFER_SAMPLES];
                    while (samplesRead < targetSamples) {
                        // the first time through, we read a single frame.
                        // this sets the recording anchor position.
                        int amount = samplesRead == 0 ? numChannels :
                            Math.min(BUFFER_SAMPLES, targetSamples - samplesRead);
                        int ret = record.read(floatData, 0, amount, blocking ?
                                AudioRecord.READ_BLOCKING : AudioRecord.READ_NON_BLOCKING);
                        if (blocking) {
                            assertEquals(amount, ret);
                        } else {
                            assertTrue("0 <= " + ret + " <= " + amount,
                                    0 <= ret && ret <= amount);
                        }
                        if (samplesRead == 0 && ret > 0) {
                            firstSampleTime = System.currentTimeMillis();
                        }
                        samplesRead += ret;
                    }
                } break;
                }
            }

            // We've read all the frames, now check the record timing.
            endTime = System.currentTimeMillis();
            //Log.d(TAG, "first sample time " + (firstSampleTime - startTime)
            //        + " test time " + (endTime - firstSampleTime));
            // Verify recording starts within 200 ms of record.startRecording() (typical 100ms)
            // Verify recording completes within 50 ms of expected test time (typical 20ms)
            assertEquals(0, firstSampleTime - startTime, 200);
            assertEquals(TEST_TIME_MS, endTime - firstSampleTime, auditRecording ? 1000 : 50);

            // Even though we've read all the frames we want, the events may not be sent to
            // the listeners (events are handled through a separate internal callback thread).
            // One must sleep to make sure the last event(s) come in.
            Thread.sleep(30);

            stopRequestTime = System.currentTimeMillis();
            record.stop();
            assertEquals(AudioRecord.RECORDSTATE_STOPPED, record.getRecordingState());

            stopTime = System.currentTimeMillis();

            // stop listening - we should be done.
            // Caution M behavior and likely much earlier:
            // we assume no events can happen after stop(), but this may not
            // always be true as stop can take 100ms to complete (as it may disable
            // input recording on the hal); thus the event handler may be block with
            // valid events, issuing right after stop completes. Except for those events,
            // no other events should show up after stop.
            // This behavior may change in the future but we account for it here in testing.
            listener.stop();

            // clean up
            if (makeSomething != null) {
                makeSomething.join();
            }

        } finally {
            listener.release();
            // we must release the record immediately as it is a system-wide
            // resource needed for other tests.
            record.release();
        }
        if (auditRecording) { // don't check timing if auditing (messes up timing)
            return;
        }
        final int markerPeriods = markerPeriodsPerSecond * TEST_TIME_MS / 1000;
        final int updatePeriods = periodsPerSecond * TEST_TIME_MS / 1000;
        final int markerPeriodsMax =
                markerPeriodsPerSecond * (int)(stopTime - firstSampleTime) / 1000 + 1;
        final int updatePeriodsMax =
                periodsPerSecond * (int)(stopTime - firstSampleTime) / 1000 + 1;

        // collect statistics
        final ArrayList<Integer> markerList = listener.getMarkerList();
        final ArrayList<Integer> periodicList = listener.getPeriodicList();
        // verify count of markers and periodic notifications.
        // there could be an extra notification since we don't stop() immediately
        // rather wait for potential events to come in.
        //Log.d(TAG, "markerPeriods " + markerPeriods +
        //        " markerPeriodsReceived " + markerList.size());
        //Log.d(TAG, "updatePeriods " + updatePeriods +
        //        " updatePeriodsReceived " + periodicList.size());
        assertTrue(TAG + ": markerPeriods " + markerPeriods +
                " <= markerPeriodsReceived " + markerList.size() +
                " <= markerPeriodsMax " + markerPeriodsMax,
                markerPeriods <= markerList.size()
                && markerList.size() <= markerPeriodsMax);
        assertTrue(TAG + ": updatePeriods " + updatePeriods +
               " <= updatePeriodsReceived " + periodicList.size() +
               " <= updatePeriodsMax " + updatePeriodsMax,
                updatePeriods <= periodicList.size()
                && periodicList.size() <= updatePeriodsMax);

        // Since we don't have accurate positioning of the start time of the recorder,
        // and there is no record.getPosition(), we consider only differential timing
        // from the first marker or periodic event.
        final int toleranceInFrames = TEST_SR * 80 / 1000; // 80 ms
        final int testTimeInFrames = (int)((long)TEST_TIME_MS * TEST_SR / 1000);

        AudioHelper.Statistics markerStat = new AudioHelper.Statistics();
        for (int i = 1; i < markerList.size(); ++i) {
            final int expected = mMarkerPeriodInFrames * i;
            if (markerList.get(i) > testTimeInFrames) {
                break; // don't consider any notifications when we might be stopping.
            }
            final int actual = markerList.get(i) - markerList.get(0);
            //Log.d(TAG, "Marker: " + i + " expected(" + expected + ")  actual(" + actual
            //        + ")  diff(" + (actual - expected) + ")"
            //        + " tolerance " + toleranceInFrames);
            assertEquals(expected, actual, toleranceInFrames);
            markerStat.add((double)(actual - expected) * 1000 / TEST_SR);
        }

        AudioHelper.Statistics periodicStat = new AudioHelper.Statistics();
        for (int i = 1; i < periodicList.size(); ++i) {
            final int expected = updatePeriodInFrames * i;
            if (periodicList.get(i) > testTimeInFrames) {
                break; // don't consider any notifications when we might be stopping.
            }
            final int actual = periodicList.get(i) - periodicList.get(0);
            //Log.d(TAG, "Update: " + i + " expected(" + expected + ")  actual(" + actual
            //        + ")  diff(" + (actual - expected) + ")"
            //        + " tolerance " + toleranceInFrames);
            assertEquals(expected, actual, toleranceInFrames);
            periodicStat.add((double)(actual - expected) * 1000 / TEST_SR);
        }

        // report this
        ReportLog log = getReportLog();
        log.printValue(reportName + ": startRecording lag", firstSampleTime - startTime,
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": stop execution time", stopTime - stopRequestTime,
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Total record time expected", TEST_TIME_MS,
                ResultType.NEUTRAL, ResultUnit.MS);
        log.printValue(reportName + ": Total record time actual", endTime - firstSampleTime,
                ResultType.NEUTRAL, ResultUnit.MS);
        log.printValue(reportName + ": Total markers expected", markerPeriods,
                ResultType.NEUTRAL, ResultUnit.COUNT);
        log.printValue(reportName + ": Total markers actual", markerList.size(),
                ResultType.NEUTRAL, ResultUnit.COUNT);
        log.printValue(reportName + ": Total periods expected", updatePeriods,
                ResultType.NEUTRAL, ResultUnit.COUNT);
        log.printValue(reportName + ": Total periods actual", periodicList.size(),
                ResultType.NEUTRAL, ResultUnit.COUNT);
        log.printValue(reportName + ": Average Marker diff", markerStat.getAvg(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Maximum Marker abs diff", markerStat.getMaxAbs(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Average Marker abs diff", markerStat.getAvgAbs(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Average Periodic diff", periodicStat.getAvg(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Maximum Periodic abs diff", periodicStat.getMaxAbs(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue(reportName + ": Average Periodic abs diff", periodicStat.getAvgAbs(),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printSummary(reportName + ": Unified abs diff",
                (periodicStat.getAvgAbs() + markerStat.getAvgAbs()) / 2,
                ResultType.LOWER_BETTER, ResultUnit.MS);
    }

    private class MockOnRecordPositionUpdateListener
                                        implements OnRecordPositionUpdateListener {
        public MockOnRecordPositionUpdateListener(AudioRecord record) {
            mAudioRecord = record;
            record.setRecordPositionUpdateListener(this);
        }

        public MockOnRecordPositionUpdateListener(AudioRecord record, Handler handler) {
            mAudioRecord = record;
            record.setRecordPositionUpdateListener(this, handler);
        }

        public synchronized void onMarkerReached(AudioRecord record) {
            if (mIsTestActive) {
                int position = getPosition();
                mOnMarkerReachedCalled.add(position);
                mMarkerPosition += mMarkerPeriodInFrames;
                assertEquals(AudioRecord.SUCCESS,
                        mAudioRecord.setNotificationMarkerPosition(mMarkerPosition));
            } else {
                // stop() is not sufficient to end all notifications
                // as is not synchronous with the event handling thread
                // so we comment out the line below.
                // fail("onMarkerReached called when not active");
            }
        }

        public synchronized void onPeriodicNotification(AudioRecord record) {
            if (mIsTestActive) {
                int position = getPosition();
                mOnPeriodicNotificationCalled.add(position);
            } else {
                // see above comments about stop
                // fail("onPeriodicNotification called when not active");
            }
        }

        public synchronized void start(int sampleRate) {
            mIsTestActive = true;
            mSampleRate = sampleRate;
            mStartTime = System.currentTimeMillis();
        }

        public synchronized void stop() {
            mIsTestActive = false;
        }

        public ArrayList<Integer> getMarkerList() {
            return mOnMarkerReachedCalled;
        }

        public ArrayList<Integer> getPeriodicList() {
            return mOnPeriodicNotificationCalled;
        }

        public synchronized void release() {
            stop();
            mAudioRecord.setRecordPositionUpdateListener(null);
            mAudioRecord = null;
        }

        private int getPosition() {
            // we don't have mAudioRecord.getRecordPosition();
            // so we fake this by timing.
            long delta = System.currentTimeMillis() - mStartTime;
            return (int)(delta * mSampleRate / 1000);
        }

        private long mStartTime;
        private int mSampleRate;
        private boolean mIsTestActive = true;
        private AudioRecord mAudioRecord;
        private ArrayList<Integer> mOnMarkerReachedCalled = new ArrayList<Integer>();
        private ArrayList<Integer> mOnPeriodicNotificationCalled = new ArrayList<Integer>();
    }

    private boolean hasMicrophone() {
        return getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }
}
