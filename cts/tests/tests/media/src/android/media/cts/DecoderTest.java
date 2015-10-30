/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.cts.media.R;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.cts.util.MediaUtils;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.concurrent.TimeUnit;

public class DecoderTest extends MediaPlayerTestBase {
    private static final String TAG = "DecoderTest";

    private static final int RESET_MODE_NONE = 0;
    private static final int RESET_MODE_RECONFIGURE = 1;
    private static final int RESET_MODE_FLUSH = 2;
    private static final int RESET_MODE_EOS_FLUSH = 3;

    private static final String[] CSD_KEYS = new String[] { "csd-0", "csd-1" };

    private static final int CONFIG_MODE_NONE = 0;
    private static final int CONFIG_MODE_QUEUE = 1;

    private Resources mResources;
    short[] mMasterBuffer;

    private MediaCodecTunneledPlayer mMediaCodecPlayer;
    private static final int SLEEP_TIME_MS = 1000;
    private static final long PLAY_TIME_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
    private static final Uri AUDIO_URL = Uri.parse(
            "http://redirector.c.youtube.com/videoplayback?id=c80658495af60617"
                + "&itag=18&source=youtube&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&sparams=ip,ipbits,expire,id,itag,source"
                + "&signature=46A04ED550CA83B79B60060BA80C79FDA5853D26."
                + "49582D382B4A9AFAA163DED38D2AE531D85603C0"
                + "&key=ik0&user=android-device-test");  // H.264 Base + AAC
    private static final Uri VIDEO_URL = Uri.parse(
            "http://redirector.c.youtube.com/videoplayback?id=c80658495af60617"
                + "&itag=18&source=youtube&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&sparams=ip,ipbits,expire,id,itag,source"
                + "&signature=46A04ED550CA83B79B60060BA80C79FDA5853D26."
                + "49582D382B4A9AFAA163DED38D2AE531D85603C0"
                + "&key=ik0&user=android-device-test");  // H.264 Base + AAC

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();

        // read master file into memory
        AssetFileDescriptor masterFd = mResources.openRawResourceFd(R.raw.sinesweepraw);
        long masterLength = masterFd.getLength();
        mMasterBuffer = new short[(int) (masterLength / 2)];
        InputStream is = masterFd.createInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        for (int i = 0; i < mMasterBuffer.length; i++) {
            int lo = bis.read();
            int hi = bis.read();
            if (hi >= 128) {
                hi -= 256;
            }
            int sample = hi * 256 + lo;
            mMasterBuffer[i] = (short) sample;
        }
        bis.close();
        masterFd.close();
    }

    // TODO: add similar tests for other audio and video formats
    public void testBug11696552() throws Exception {
        MediaCodec mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaFormat mFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, 48000 /* frequency */, 2 /* channels */);
        mFormat.setByteBuffer("csd-0", ByteBuffer.wrap( new byte [] {0x13, 0x10} ));
        mFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
        mMediaCodec.configure(mFormat, null, null, 0);
        mMediaCodec.start();
        int index = mMediaCodec.dequeueInputBuffer(250000);
        mMediaCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        mMediaCodec.dequeueOutputBuffer(info, 250000);
    }

    // The allowed errors in the following tests are the actual maximum measured
    // errors with the standard decoders, plus 10%.
    // This should allow for some variation in decoders, while still detecting
    // phase and delay errors, channel swap, etc.
    public void testDecodeMp3Lame() throws Exception {
        decode(R.raw.sinesweepmp3lame, 804.f);
        testTimeStampOrdering(R.raw.sinesweepmp3lame);
    }
    public void testDecodeMp3Smpb() throws Exception {
        decode(R.raw.sinesweepmp3smpb, 413.f);
        testTimeStampOrdering(R.raw.sinesweepmp3smpb);
    }
    public void testDecodeM4a() throws Exception {
        decode(R.raw.sinesweepm4a, 124.f);
        testTimeStampOrdering(R.raw.sinesweepm4a);
    }
    public void testDecodeOgg() throws Exception {
        decode(R.raw.sinesweepogg, 168.f);
        testTimeStampOrdering(R.raw.sinesweepogg);
    }
    public void testDecodeWav() throws Exception {
        decode(R.raw.sinesweepwav, 0.0f);
        testTimeStampOrdering(R.raw.sinesweepwav);
    }
    public void testDecodeFlac() throws Exception {
        decode(R.raw.sinesweepflac, 0.0f);
        testTimeStampOrdering(R.raw.sinesweepflac);
    }

    public void testDecodeMonoMp3() throws Exception {
        monoTest(R.raw.monotestmp3, 44100);
        testTimeStampOrdering(R.raw.monotestmp3);
    }

    public void testDecodeMonoM4a() throws Exception {
        monoTest(R.raw.monotestm4a, 44100);
        testTimeStampOrdering(R.raw.monotestm4a);
    }

    public void testDecodeMonoOgg() throws Exception {
        monoTest(R.raw.monotestogg, 44100);
        testTimeStampOrdering(R.raw.monotestogg);
    }

    public void testDecodeMonoGsm() throws Exception {
        if (MediaUtils.hasCodecsForResource(mContext, R.raw.monotestgsm)) {
            monoTest(R.raw.monotestgsm, 8000);
            testTimeStampOrdering(R.raw.monotestgsm);
        } else {
            MediaUtils.skipTest("not mandatory");
        }
    }

    public void testDecodeAacTs() throws Exception {
        testTimeStampOrdering(R.raw.sinesweeptsaac);
    }

    public void testDecode51M4a() throws Exception {
        decodeToMemory(R.raw.sinesweep51m4a, RESET_MODE_NONE, CONFIG_MODE_NONE, -1, null);
    }

    private void testTimeStampOrdering(int res) throws Exception {
        List<Long> timestamps = new ArrayList<Long>();
        decodeToMemory(res, RESET_MODE_NONE, CONFIG_MODE_NONE, -1, timestamps);
        Long lastTime = Long.MIN_VALUE;
        for (int i = 0; i < timestamps.size(); i++) {
            Long thisTime = timestamps.get(i);
            assertTrue("timetravel occurred: " + lastTime + " > " + thisTime, thisTime >= lastTime);
            lastTime = thisTime;
        }
    }

    public void testTrackSelection() throws Exception {
        testTrackSelection(R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz);
        testTrackSelection(
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz_fragmented);
        testTrackSelection(
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz_dash);
    }

    public void testBFrames() throws Exception {
        int testsRun =
            testBFrames(R.raw.video_h264_main_b_frames) +
            testBFrames(R.raw.video_h264_main_b_frames_frag);
        if (testsRun == 0) {
            MediaUtils.skipTest("no codec found");
        }
    }

    public int testBFrames(int res) throws Exception {
        AssetFileDescriptor fd = mResources.openRawResourceFd(res);
        MediaExtractor ex = new MediaExtractor();
        ex.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        MediaFormat format = ex.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        assertTrue("not a video track. Wrong test file?", mime.startsWith("video/"));
        if (!MediaUtils.canDecode(format)) {
            ex.release();
            fd.close();
            return 0; // skip
        }
        MediaCodec dec = MediaCodec.createDecoderByType(mime);
        Surface s = getActivity().getSurfaceHolder().getSurface();
        dec.configure(format, s, null, 0);
        dec.start();
        ByteBuffer[] buf = dec.getInputBuffers();
        ex.selectTrack(0);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long lastPresentationTimeUsFromExtractor = -1;
        long lastPresentationTimeUsFromDecoder = -1;
        boolean inputoutoforder = false;
        while(true) {
            int flags = ex.getSampleFlags();
            long time = ex.getSampleTime();
            if (time >= 0 && time < lastPresentationTimeUsFromExtractor) {
                inputoutoforder = true;
            }
            lastPresentationTimeUsFromExtractor = time;
            int bufidx = dec.dequeueInputBuffer(5000);
            if (bufidx >= 0) {
                int n = ex.readSampleData(buf[bufidx], 0);
                if (n < 0) {
                    flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                    time = 0;
                    n = 0;
                }
                dec.queueInputBuffer(bufidx, 0, n, time, flags);
                ex.advance();
            }
            int status = dec.dequeueOutputBuffer(info, 5000);
            if (status >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
                assertTrue("out of order timestamp from decoder",
                        info.presentationTimeUs > lastPresentationTimeUsFromDecoder);
                dec.releaseOutputBuffer(status, true);
                lastPresentationTimeUsFromDecoder = info.presentationTimeUs;
            }
        }
        assertTrue("extractor timestamps were ordered, wrong test file?", inputoutoforder);
        dec.release();
        ex.release();
        fd.close();
        return 1;
      }

    private void testTrackSelection(int resid) throws Exception {
        AssetFileDescriptor fd1 = null;
        try {
            fd1 = mResources.openRawResourceFd(resid);
            MediaExtractor ex1 = new MediaExtractor();
            ex1.setDataSource(fd1.getFileDescriptor(), fd1.getStartOffset(), fd1.getLength());

            ByteBuffer buf1 = ByteBuffer.allocate(1024*1024);
            ArrayList<Integer> vid = new ArrayList<Integer>();
            ArrayList<Integer> aud = new ArrayList<Integer>();

            // scan the file once and build lists of audio and video samples
            ex1.selectTrack(0);
            ex1.selectTrack(1);
            while(true) {
                int n1 = ex1.readSampleData(buf1, 0);
                if (n1 < 0) {
                    break;
                }
                int idx = ex1.getSampleTrackIndex();
                if (idx == 0) {
                    vid.add(n1);
                } else if (idx == 1) {
                    aud.add(n1);
                } else {
                    fail("unexpected track index: " + idx);
                }
                ex1.advance();
            }

            // read the video track once, then rewind and do it again, and
            // verify we get the right samples
            ex1.release();
            ex1 = new MediaExtractor();
            ex1.setDataSource(fd1.getFileDescriptor(), fd1.getStartOffset(), fd1.getLength());
            ex1.selectTrack(0);
            for (int i = 0; i < 2; i++) {
                ex1.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                int idx = 0;
                while(true) {
                    int n1 = ex1.readSampleData(buf1, 0);
                    if (n1 < 0) {
                        assertEquals(vid.size(), idx);
                        break;
                    }
                    assertEquals(vid.get(idx++).intValue(), n1);
                    ex1.advance();
                }
            }

            // read the audio track once, then rewind and do it again, and
            // verify we get the right samples
            ex1.release();
            ex1 = new MediaExtractor();
            ex1.setDataSource(fd1.getFileDescriptor(), fd1.getStartOffset(), fd1.getLength());
            ex1.selectTrack(1);
            for (int i = 0; i < 2; i++) {
                ex1.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                int idx = 0;
                while(true) {
                    int n1 = ex1.readSampleData(buf1, 0);
                    if (n1 < 0) {
                        assertEquals(aud.size(), idx);
                        break;
                    }
                    assertEquals(aud.get(idx++).intValue(), n1);
                    ex1.advance();
                }
            }

            // read the video track first, then rewind and get the audio track instead, and
            // verify we get the right samples
            ex1.release();
            ex1 = new MediaExtractor();
            ex1.setDataSource(fd1.getFileDescriptor(), fd1.getStartOffset(), fd1.getLength());
            for (int i = 0; i < 2; i++) {
                ex1.selectTrack(i);
                ex1.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                int idx = 0;
                while(true) {
                    int n1 = ex1.readSampleData(buf1, 0);
                    if (i == 0) {
                        if (n1 < 0) {
                            assertEquals(vid.size(), idx);
                            break;
                        }
                        assertEquals(vid.get(idx++).intValue(), n1);
                    } else if (i == 1) {
                        if (n1 < 0) {
                            assertEquals(aud.size(), idx);
                            break;
                        }
                        assertEquals(aud.get(idx++).intValue(), n1);
                    } else {
                        fail("unexpected track index: " + idx);
                    }
                    ex1.advance();
                }
                ex1.unselectTrack(i);
            }

            // read the video track first, then rewind, enable the audio track in addition
            // to the video track, and verify we get the right samples
            ex1.release();
            ex1 = new MediaExtractor();
            ex1.setDataSource(fd1.getFileDescriptor(), fd1.getStartOffset(), fd1.getLength());
            for (int i = 0; i < 2; i++) {
                ex1.selectTrack(i);
                ex1.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                int vididx = 0;
                int audidx = 0;
                while(true) {
                    int n1 = ex1.readSampleData(buf1, 0);
                    if (n1 < 0) {
                        // we should have read all audio and all video samples at this point
                        assertEquals(vid.size(), vididx);
                        if (i == 1) {
                            assertEquals(aud.size(), audidx);
                        }
                        break;
                    }
                    int trackidx = ex1.getSampleTrackIndex();
                    if (trackidx == 0) {
                        assertEquals(vid.get(vididx++).intValue(), n1);
                    } else if (trackidx == 1) {
                        assertEquals(aud.get(audidx++).intValue(), n1);
                    } else {
                        fail("unexpected track index: " + trackidx);
                    }
                    ex1.advance();
                }
            }

            // read both tracks from the start, then rewind and verify we get the right
            // samples both times
            ex1.release();
            ex1 = new MediaExtractor();
            ex1.setDataSource(fd1.getFileDescriptor(), fd1.getStartOffset(), fd1.getLength());
            for (int i = 0; i < 2; i++) {
                ex1.selectTrack(0);
                ex1.selectTrack(1);
                ex1.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                int vididx = 0;
                int audidx = 0;
                while(true) {
                    int n1 = ex1.readSampleData(buf1, 0);
                    if (n1 < 0) {
                        // we should have read all audio and all video samples at this point
                        assertEquals(vid.size(), vididx);
                        assertEquals(aud.size(), audidx);
                        break;
                    }
                    int trackidx = ex1.getSampleTrackIndex();
                    if (trackidx == 0) {
                        assertEquals(vid.get(vididx++).intValue(), n1);
                    } else if (trackidx == 1) {
                        assertEquals(aud.get(audidx++).intValue(), n1);
                    } else {
                        fail("unexpected track index: " + trackidx);
                    }
                    ex1.advance();
                }
            }

        } finally {
            if (fd1 != null) {
                fd1.close();
            }
        }
    }

    public void testDecodeFragmented() throws Exception {
        testDecodeFragmented(R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz,
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz_fragmented);
        testDecodeFragmented(R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz,
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz_dash);
    }

    private void testDecodeFragmented(int reference, int teststream) throws Exception {
        AssetFileDescriptor fd1 = null;
        AssetFileDescriptor fd2 = null;
        try {
            fd1 = mResources.openRawResourceFd(reference);
            MediaExtractor ex1 = new MediaExtractor();
            ex1.setDataSource(fd1.getFileDescriptor(), fd1.getStartOffset(), fd1.getLength());

            fd2 = mResources.openRawResourceFd(teststream);
            MediaExtractor ex2 = new MediaExtractor();
            ex2.setDataSource(fd2.getFileDescriptor(), fd2.getStartOffset(), fd2.getLength());

            assertEquals("different track count", ex1.getTrackCount(), ex2.getTrackCount());

            ByteBuffer buf1 = ByteBuffer.allocate(1024*1024);
            ByteBuffer buf2 = ByteBuffer.allocate(1024*1024);

            for (int i = 0; i < ex1.getTrackCount(); i++) {
                // note: this assumes the tracks are reported in the order in which they appear
                // in the file.
                ex1.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                ex1.selectTrack(i);
                ex2.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                ex2.selectTrack(i);

                while(true) {
                    int n1 = ex1.readSampleData(buf1, 0);
                    int n2 = ex2.readSampleData(buf2, 0);
                    assertEquals("different buffer size on track " + i, n1, n2);

                    if (n1 < 0) {
                        break;
                    }
                    // see bug 13008204
                    buf1.limit(n1);
                    buf2.limit(n2);
                    buf1.rewind();
                    buf2.rewind();

                    assertEquals("limit does not match return value on track " + i,
                            n1, buf1.limit());
                    assertEquals("limit does not match return value on track " + i,
                            n2, buf2.limit());

                    assertEquals("buffer data did not match on track " + i, buf1, buf2);

                    ex1.advance();
                    ex2.advance();
                }
                ex1.unselectTrack(i);
                ex2.unselectTrack(i);
            }
        } finally {
            if (fd1 != null) {
                fd1.close();
            }
            if (fd2 != null) {
                fd2.close();
            }
        }
    }


    private void monoTest(int res, int expectedLength) throws Exception {
        short [] mono = decodeToMemory(res, RESET_MODE_NONE, CONFIG_MODE_NONE, -1, null);
        if (mono.length == expectedLength) {
            // expected
        } else if (mono.length == expectedLength * 2) {
            // the decoder output 2 channels instead of 1, check that the left and right channel
            // are identical
            for (int i = 0; i < mono.length; i += 2) {
                assertEquals("mismatched samples at " + i, mono[i], mono[i+1]);
            }
        } else {
            fail("wrong number of samples: " + mono.length);
        }

        short [] mono2 = decodeToMemory(res, RESET_MODE_RECONFIGURE, CONFIG_MODE_NONE, -1, null);

        assertEquals("count different after reconfigure: ", mono.length, mono2.length);
        for (int i = 0; i < mono.length; i++) {
            assertEquals("samples at " + i + " don't match", mono[i], mono2[i]);
        }

        short [] mono3 = decodeToMemory(res, RESET_MODE_FLUSH, CONFIG_MODE_NONE, -1, null);

        assertEquals("count different after flush: ", mono.length, mono3.length);
        for (int i = 0; i < mono.length; i++) {
            assertEquals("samples at " + i + " don't match", mono[i], mono3[i]);
        }
    }

    /**
     * @param testinput the file to decode
     * @param maxerror the maximum allowed root mean squared error
     * @throws IOException
     */
    private void decode(int testinput, float maxerror) throws IOException {

        short[] decoded = decodeToMemory(testinput, RESET_MODE_NONE, CONFIG_MODE_NONE, -1, null);

        assertEquals("wrong data size", mMasterBuffer.length, decoded.length);

        long totalErrorSquared = 0;

        for (int i = 0; i < decoded.length; i++) {
            short sample = decoded[i];
            short mastersample = mMasterBuffer[i];
            int d = sample - mastersample;
            totalErrorSquared += d * d;
        }

        long avgErrorSquared = (totalErrorSquared / decoded.length);
        double rmse = Math.sqrt(avgErrorSquared);
        assertTrue("decoding error too big: " + rmse, rmse <= maxerror);

        int[] resetModes = new int[] { RESET_MODE_NONE, RESET_MODE_RECONFIGURE,
                RESET_MODE_FLUSH, RESET_MODE_EOS_FLUSH };
        int[] configModes = new int[] { CONFIG_MODE_NONE, CONFIG_MODE_QUEUE };

        for (int conf : configModes) {
            for (int reset : resetModes) {
                if (conf == CONFIG_MODE_NONE && reset == RESET_MODE_NONE) {
                    // default case done outside of loop
                    continue;
                }
                if (conf == CONFIG_MODE_QUEUE && !hasAudioCsd(testinput)) {
                    continue;
                }

                String params = String.format("(using reset: %d, config: %s)", reset, conf);
                short[] decoded2 = decodeToMemory(testinput, reset, conf, -1, null);
                assertEquals("count different with reconfigure" + params,
                        decoded.length, decoded2.length);
                for (int i = 0; i < decoded.length; i++) {
                    assertEquals("samples don't match" + params, decoded[i], decoded2[i]);
                }
            }
        }
    }

    private boolean hasAudioCsd(int testinput) throws IOException {
        AssetFileDescriptor fd = null;
        try {

            fd = mResources.openRawResourceFd(testinput);
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            MediaFormat format = extractor.getTrackFormat(0);

            return format.containsKey(CSD_KEYS[0]);

        } finally {
            if (fd != null) {
                fd.close();
            }
        }
    }

    private short[] decodeToMemory(int testinput, int resetMode, int configMode,
            int eossample, List<Long> timestamps) throws IOException {

        String localTag = TAG + "#decodeToMemory";
        Log.v(localTag, String.format("reset = %d; config: %s", resetMode, configMode));
        short [] decoded = new short[0];
        int decodedIdx = 0;

        AssetFileDescriptor testFd = mResources.openRawResourceFd(testinput);

        MediaExtractor extractor;
        MediaCodec codec;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        extractor = new MediaExtractor();
        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                testFd.getLength());
        testFd.close();

        assertEquals("wrong number of tracks", 1, extractor.getTrackCount());
        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        assertTrue("not an audio file", mime.startsWith("audio/"));

        MediaFormat configFormat = format;
        codec = MediaCodec.createDecoderByType(mime);
        if (configMode == CONFIG_MODE_QUEUE && format.containsKey(CSD_KEYS[0])) {
            configFormat = MediaFormat.createAudioFormat(mime,
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));

            configFormat.setLong(MediaFormat.KEY_DURATION,
                    format.getLong(MediaFormat.KEY_DURATION));
            String[] keys = new String[] { "max-input-size", "encoder-delay", "encoder-padding" };
            for (String k : keys) {
                if (format.containsKey(k)) {
                    configFormat.setInteger(k, format.getInteger(k));
                }
            }
        }
        Log.v(localTag, "configuring with " + configFormat);
        codec.configure(configFormat, null /* surface */, null /* crypto */, 0 /* flags */);

        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        if (resetMode == RESET_MODE_RECONFIGURE) {
            codec.stop();
            codec.configure(configFormat, null /* surface */, null /* crypto */, 0 /* flags */);
            codec.start();
            codecInputBuffers = codec.getInputBuffers();
            codecOutputBuffers = codec.getOutputBuffers();
        } else if (resetMode == RESET_MODE_FLUSH) {
            codec.flush();
        }

        extractor.selectTrack(0);

        if (configMode == CONFIG_MODE_QUEUE) {
            queueConfig(codec, format);
        }

        // start decoding
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int samplecounter = 0;
        while (!sawOutputEOS && noOutputCounter < 50) {
            noOutputCounter++;
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize =
                        extractor.readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0 && eossample > 0) {
                        fail("test is broken: never reached eos sample");
                    }
                    if (sampleSize < 0) {
                        Log.d(TAG, "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        if (samplecounter == eossample) {
                            sawInputEOS = true;
                        }
                        samplecounter++;
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
            }

            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {
                //Log.d(TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);

                if (info.size > 0) {
                    noOutputCounter = 0;
                    if (timestamps != null) {
                        timestamps.add(info.presentationTimeUs);
                    }
                }
                if (info.size > 0 &&
                        resetMode != RESET_MODE_NONE && resetMode != RESET_MODE_EOS_FLUSH) {
                    // once we've gotten some data out of the decoder, reset and start again
                    if (resetMode == RESET_MODE_RECONFIGURE) {
                        codec.stop();
                        codec.configure(configFormat, null /* surface */, null /* crypto */,
                                0 /* flags */);
                        codec.start();
                        codecInputBuffers = codec.getInputBuffers();
                        codecOutputBuffers = codec.getOutputBuffers();
                        if (configMode == CONFIG_MODE_QUEUE) {
                            queueConfig(codec, format);
                        }
                    } else /* resetMode == RESET_MODE_FLUSH */ {
                        codec.flush();
                    }
                    resetMode = RESET_MODE_NONE;
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    sawInputEOS = false;
                    samplecounter = 0;
                    if (timestamps != null) {
                        timestamps.clear();
                    }
                    continue;
                }

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                if (decodedIdx + (info.size / 2) >= decoded.length) {
                    decoded = Arrays.copyOf(decoded, decodedIdx + (info.size / 2));
                }

                buf.position(info.offset);
                for (int i = 0; i < info.size; i += 2) {
                    decoded[decodedIdx++] = buf.getShort();
                }

                codec.releaseOutputBuffer(outputBufIndex, false /* render */);

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "saw output EOS.");
                    if (resetMode == RESET_MODE_EOS_FLUSH) {
                        resetMode = RESET_MODE_NONE;
                        codec.flush();
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                        sawInputEOS = false;
                        samplecounter = 0;
                        decoded = new short[0];
                        decodedIdx = 0;
                        if (timestamps != null) {
                            timestamps.clear();
                        }
                    } else {
                        sawOutputEOS = true;
                    }
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();

                Log.d(TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();

                Log.d(TAG, "output format has changed to " + oformat);
            } else {
                Log.d(TAG, "dequeueOutputBuffer returned " + res);
            }
        }
        if (noOutputCounter >= 50) {
            fail("decoder stopped outputing data");
        }

        codec.stop();
        codec.release();
        return decoded;
    }

    private void queueConfig(MediaCodec codec, MediaFormat format) {
        for (String csdKey : CSD_KEYS) {
            if (!format.containsKey(csdKey)) {
                continue;
            }
            ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
            int inputBufIndex = codec.dequeueInputBuffer(-1);
            if (inputBufIndex < 0) {
                fail("failed to queue configuration buffer " + csdKey);
            } else {
                ByteBuffer csd = (ByteBuffer) format.getByteBuffer(csdKey).rewind();
                Log.v(TAG + "#queueConfig", String.format("queueing %s:%s", csdKey, csd));
                codecInputBuffers[inputBufIndex].put(csd);
                codec.queueInputBuffer(
                        inputBufIndex,
                        0 /* offset */,
                        csd.limit(),
                        0 /* presentation time (us) */,
                        MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
            }
        }
    }

    public void testDecodeWithEOSOnLastBuffer() throws Exception {
        testDecodeWithEOSOnLastBuffer(R.raw.sinesweepm4a);
        testDecodeWithEOSOnLastBuffer(R.raw.sinesweepmp3lame);
        testDecodeWithEOSOnLastBuffer(R.raw.sinesweepmp3smpb);
        testDecodeWithEOSOnLastBuffer(R.raw.sinesweepwav);
        testDecodeWithEOSOnLastBuffer(R.raw.sinesweepflac);
        testDecodeWithEOSOnLastBuffer(R.raw.sinesweepogg);
    }

    /* setting EOS on the last full input buffer should be equivalent to setting EOS on an empty
     * input buffer after all the full ones. */
    private void testDecodeWithEOSOnLastBuffer(int res) throws Exception {
        int numsamples = countSamples(res);
        assertTrue(numsamples != 0);

        List<Long> timestamps1 = new ArrayList<Long>();
        short[] decode1 = decodeToMemory(res, RESET_MODE_NONE, CONFIG_MODE_NONE, -1, timestamps1);

        List<Long> timestamps2 = new ArrayList<Long>();
        short[] decode2 = decodeToMemory(res, RESET_MODE_NONE, CONFIG_MODE_NONE, numsamples - 1,
                timestamps2);

        // check that the data and the timestamps are the same for EOS-on-last and EOS-after-last
        assertEquals(decode1.length, decode2.length);
        assertTrue(Arrays.equals(decode1, decode2));
        assertEquals(timestamps1.size(), timestamps2.size());
        assertTrue(timestamps1.equals(timestamps2));

        // ... and that this is also true when reconfiguring the codec
        timestamps2.clear();
        decode2 = decodeToMemory(res, RESET_MODE_RECONFIGURE, CONFIG_MODE_NONE, -1, timestamps2);
        assertTrue(Arrays.equals(decode1, decode2));
        assertTrue(timestamps1.equals(timestamps2));
        timestamps2.clear();
        decode2 = decodeToMemory(res, RESET_MODE_RECONFIGURE, CONFIG_MODE_NONE, numsamples - 1,
                timestamps2);
        assertEquals(decode1.length, decode2.length);
        assertTrue(Arrays.equals(decode1, decode2));
        assertTrue(timestamps1.equals(timestamps2));

        // ... and that this is also true when flushing the codec
        timestamps2.clear();
        decode2 = decodeToMemory(res, RESET_MODE_FLUSH, CONFIG_MODE_NONE, -1, timestamps2);
        assertTrue(Arrays.equals(decode1, decode2));
        assertTrue(timestamps1.equals(timestamps2));
        timestamps2.clear();
        decode2 = decodeToMemory(res, RESET_MODE_FLUSH, CONFIG_MODE_NONE, numsamples - 1,
                timestamps2);
        assertEquals(decode1.length, decode2.length);
        assertTrue(Arrays.equals(decode1, decode2));
        assertTrue(timestamps1.equals(timestamps2));
    }

    private int countSamples(int res) throws IOException {
        AssetFileDescriptor testFd = mResources.openRawResourceFd(res);

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                testFd.getLength());
        testFd.close();
        extractor.selectTrack(0);
        int numsamples = 0;
        while (extractor.advance()) {
            numsamples++;
        }
        return numsamples;
    }

    private void testDecode(int testVideo, int frameNum) throws Exception {
        if (!MediaUtils.checkCodecForResource(mContext, testVideo, 0 /* track */)) {
            return; // skip
        }

        // Decode to Surface.
        Surface s = getActivity().getSurfaceHolder().getSurface();
        int frames1 = countFrames(testVideo, RESET_MODE_NONE, -1 /* eosframe */, s);
        assertEquals("wrong number of frames decoded", frameNum, frames1);

        // Decode to buffer.
        int frames2 = countFrames(testVideo, RESET_MODE_NONE, -1 /* eosframe */, null);
        assertEquals("different number of frames when using Surface", frames1, frames2);
    }

    public void testCodecBasicH264() throws Exception {
        testDecode(R.raw.video_480x360_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz, 240);
    }

    public void testCodecBasicHEVC() throws Exception {
        testDecode(R.raw.video_1280x720_mp4_hevc_1150kbps_30fps_aac_stereo_128kbps_48000hz, 300);
    }

    public void testCodecBasicH263() throws Exception {
        testDecode(R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_22050hz, 122);
    }

    public void testCodecBasicMpeg4() throws Exception {
        testDecode(R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz, 249);
    }

    public void testCodecBasicVP8() throws Exception {
        testDecode(R.raw.video_480x360_webm_vp8_333kbps_25fps_vorbis_stereo_128kbps_48000hz, 240);
    }

    public void testCodecBasicVP9() throws Exception {
        testDecode(R.raw.video_480x360_webm_vp9_333kbps_25fps_vorbis_stereo_128kbps_48000hz, 240);
    }

    public void testH264Decode320x240() throws Exception {
        testDecode(R.raw.video_320x240_mp4_h264_800kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    public void testH264Decode720x480() throws Exception {
        testDecode(R.raw.video_720x480_mp4_h264_2048kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    public void testH264Decode30fps1280x720Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720, 30));
        }
    }

    public void testH264SecureDecode30fps1280x720Tv() throws Exception {
        if (checkTv()) {
            verifySecureVideoDecodeSupport(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720, 30);
        }
    }

    public void testH264Decode30fps1280x720() throws Exception {
        testDecode(R.raw.video_1280x720_mp4_h264_8192kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    public void testH264Decode60fps1280x720Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720, 60));
        }
    }

    public void testH264SecureDecode60fps1280x720Tv() throws Exception {
        if (checkTv()) {
            verifySecureVideoDecodeSupport(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720, 60);
        }
    }

    public void testH264Decode60fps1280x720() throws Exception {
        testDecode(R.raw.video_1280x720_mp4_h264_8192kbps_60fps_aac_stereo_128kbps_44100hz, 596);
    }

    public void testH264Decode30fps1920x1080Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080, 30));
        }
    }

    public void testH264SecureDecode30fps1920x1080Tv() throws Exception {
        if (checkTv()) {
            verifySecureVideoDecodeSupport(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080, 30);
        }
    }

    public void testH264Decode30fps1920x1080() throws Exception {
        testDecode(R.raw.video_1920x1080_mp4_h264_20480kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    public void testH264Decode60fps1920x1080Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080, 60));
        }
    }

    public void testH264SecureDecode60fps1920x1080Tv() throws Exception {
        if (checkTv()) {
            verifySecureVideoDecodeSupport(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080, 60);
        }
    }

    public void testH264Decode60fps1920x1080() throws Exception {
        testDecode(R.raw.video_1920x1080_mp4_h264_20480kbps_60fps_aac_stereo_128kbps_44100hz, 596);
    }

    public void testVP8Decode320x240() throws Exception {
        testDecode(R.raw.video_320x240_webm_vp8_800kbps_30fps_vorbis_stereo_128kbps_44100hz, 249);
    }

    public void testVP8Decode640x360() throws Exception {
        testDecode(R.raw.video_640x360_webm_vp8_2048kbps_30fps_vorbis_stereo_128kbps_48000hz, 249);
    }

    public void testVP8Decode30fps1280x720Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_VP8, 1280, 720, 30));
        }
    }

    public void testVP8Decode30fps1280x720() throws Exception {
        testDecode(R.raw.video_1280x720_webm_vp8_8192kbps_30fps_vorbis_stereo_128kbps_48000hz, 249);
    }

    public void testVP8Decode60fps1280x720Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_VP8, 1280, 720, 60));
        }
    }

    public void testVP8Decode60fps1280x720() throws Exception {
        testDecode(R.raw.video_1280x720_webm_vp8_8192kbps_60fps_vorbis_stereo_128kbps_48000hz, 249);
    }

    public void testVP8Decode30fps1920x1080Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_VP8, 1920, 1080, 30));
        }
    }

    public void testVP8Decode30fps1920x1080() throws Exception {
        testDecode(R.raw.video_1920x1080_webm_vp8_20480kbps_30fps_vorbis_stereo_128kbps_48000hz,
                249);
    }

    public void testVP8Decode60fps1920x1080Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_VP8, 1920, 1080, 60));
        }
    }

    public void testVP8Decode60fps1920x1080() throws Exception {
        testDecode(R.raw.video_1920x1080_webm_vp8_20480kbps_60fps_vorbis_stereo_128kbps_44100hz,
                249);
    }

    public void testVP9Decode320x240() throws Exception {
        testDecode(R.raw.video_320x240_webm_vp9_600kbps_30fps_vorbis_stereo_128kbps_48000hz, 249);
    }

    public void testVP9Decode640x360() throws Exception {
        testDecode(R.raw.video_640x360_webm_vp9_1600kbps_30fps_vorbis_stereo_128kbps_48000hz, 249);
    }

    public void testVP9Decode30fps1280x720Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_VP9, 1280, 720, 30));
        }
    }

    public void testVP9Decode30fps1280x720() throws Exception {
        testDecode(R.raw.video_1280x720_webm_vp9_4096kbps_30fps_vorbis_stereo_128kbps_44100hz, 249);
    }

    public void testVP9Decode30fps1920x1080() throws Exception {
        testDecode(R.raw.video_1920x1080_webm_vp9_10240kbps_30fps_vorbis_stereo_128kbps_48000hz,
                249);
    }

    public void testVP9Decode30fps3840x2160() throws Exception {
        testDecode(R.raw.video_3840x2160_webm_vp9_20480kbps_30fps_vorbis_stereo_128kbps_48000hz,
                249);
    }

    public void testHEVCDecode352x288() throws Exception {
        testDecode(R.raw.video_352x288_mp4_hevc_600kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    public void testHEVCDecode720x480() throws Exception {
        testDecode(R.raw.video_720x480_mp4_hevc_1638kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    public void testHEVCDecode30fps1280x720Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_HEVC, 1280, 720, 30));
        }
    }

    public void testHEVCDecode30fps1280x720() throws Exception {
        testDecode(R.raw.video_1280x720_mp4_hevc_4096kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    public void testHEVCDecode30fps1920x1080Tv() throws Exception {
        if (checkTv()) {
            assertTrue(MediaUtils.canDecodeVideo(MediaFormat.MIMETYPE_VIDEO_HEVC, 1920, 1080, 30));
        }
    }

    public void testHEVCDecode30fps1920x1080() throws Exception {
        testDecode(R.raw.video_1920x1080_mp4_hevc_10240kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    public void testHEVCDecode30fps3840x2160() throws Exception {
        testDecode(R.raw.video_3840x2160_mp4_hevc_20480kbps_30fps_aac_stereo_128kbps_44100hz, 299);
    }

    private void testCodecEarlyEOS(int resid, int eosFrame) throws Exception {
        if (!MediaUtils.checkCodecForResource(mContext, resid, 0 /* track */)) {
            return; // skip
        }
        Surface s = getActivity().getSurfaceHolder().getSurface();
        int frames1 = countFrames(resid, RESET_MODE_NONE, eosFrame, s);
        assertEquals("wrong number of frames decoded", eosFrame, frames1);
    }

    public void testCodecEarlyEOSH263() throws Exception {
        testCodecEarlyEOS(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_22050hz,
                64 /* eosframe */);
    }

    public void testCodecEarlyEOSH264() throws Exception {
        testCodecEarlyEOS(
                R.raw.video_480x360_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz,
                120 /* eosframe */);
    }

    public void testCodecEarlyEOSHEVC() throws Exception {
        testCodecEarlyEOS(
                R.raw.video_480x360_mp4_hevc_650kbps_30fps_aac_stereo_128kbps_48000hz,
                120 /* eosframe */);
    }

    public void testCodecEarlyEOSMpeg4() throws Exception {
        testCodecEarlyEOS(
                R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz,
                120 /* eosframe */);
    }

    public void testCodecEarlyEOSVP8() throws Exception {
        testCodecEarlyEOS(
                R.raw.video_480x360_webm_vp8_333kbps_25fps_vorbis_stereo_128kbps_48000hz,
                120 /* eosframe */);
    }

    public void testCodecEarlyEOSVP9() throws Exception {
        testCodecEarlyEOS(
                R.raw.video_480x360_webm_vp9_333kbps_25fps_vorbis_stereo_128kbps_48000hz,
                120 /* eosframe */);
    }

    public void testCodecResetsH264WithoutSurface() throws Exception {
        testCodecResets(
                R.raw.video_480x360_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz, null);
    }

    public void testCodecResetsH264WithSurface() throws Exception {
        Surface s = getActivity().getSurfaceHolder().getSurface();
        testCodecResets(
                R.raw.video_480x360_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz, s);
    }

    public void testCodecResetsHEVCWithoutSurface() throws Exception {
        testCodecResets(
                R.raw.video_1280x720_mp4_hevc_1150kbps_30fps_aac_stereo_128kbps_48000hz, null);
    }

    public void testCodecResetsHEVCWithSurface() throws Exception {
        Surface s = getActivity().getSurfaceHolder().getSurface();
        testCodecResets(
                R.raw.video_1280x720_mp4_hevc_1150kbps_30fps_aac_stereo_128kbps_48000hz, s);
    }

    public void testCodecResetsH263WithoutSurface() throws Exception {
        testCodecResets(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_22050hz, null);
    }

    public void testCodecResetsH263WithSurface() throws Exception {
        Surface s = getActivity().getSurfaceHolder().getSurface();
        testCodecResets(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_22050hz, s);
    }

    public void testCodecResetsMpeg4WithoutSurface() throws Exception {
        testCodecResets(
                R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz, null);
    }

    public void testCodecResetsMpeg4WithSurface() throws Exception {
        Surface s = getActivity().getSurfaceHolder().getSurface();
        testCodecResets(
                R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz, s);
    }

    public void testCodecResetsVP8WithoutSurface() throws Exception {
        testCodecResets(
                R.raw.video_480x360_webm_vp8_333kbps_25fps_vorbis_stereo_128kbps_48000hz, null);
    }

    public void testCodecResetsVP8WithSurface() throws Exception {
        Surface s = getActivity().getSurfaceHolder().getSurface();
        testCodecResets(
                R.raw.video_480x360_webm_vp8_333kbps_25fps_vorbis_stereo_128kbps_48000hz, s);
    }

    public void testCodecResetsVP9WithoutSurface() throws Exception {
        testCodecResets(
                R.raw.video_480x360_webm_vp9_333kbps_25fps_vorbis_stereo_128kbps_48000hz, null);
    }

    public void testCodecResetsVP9WithSurface() throws Exception {
        Surface s = getActivity().getSurfaceHolder().getSurface();
        testCodecResets(
                R.raw.video_480x360_webm_vp9_333kbps_25fps_vorbis_stereo_128kbps_48000hz, s);
    }

//    public void testCodecResetsOgg() throws Exception {
//        testCodecResets(R.raw.sinesweepogg, null);
//    }

    public void testCodecResetsMp3() throws Exception {
        testCodecReconfig(R.raw.sinesweepmp3lame);
        // NOTE: replacing testCodecReconfig call soon
//        testCodecResets(R.raw.sinesweepmp3lame, null);
    }

    public void testCodecResetsM4a() throws Exception {
        testCodecReconfig(R.raw.sinesweepm4a);
        // NOTE: replacing testCodecReconfig call soon
//        testCodecResets(R.raw.sinesweepm4a, null);
    }

    private void testCodecReconfig(int audio) throws Exception {
        int size1 = countSize(audio, RESET_MODE_NONE, -1 /* eosframe */);
        int size2 = countSize(audio, RESET_MODE_RECONFIGURE, -1 /* eosframe */);
        assertEquals("different output size when using reconfigured codec", size1, size2);
    }

    private void testCodecResets(int video, Surface s) throws Exception {
        if (!MediaUtils.checkCodecForResource(mContext, video, 0 /* track */)) {
            return; // skip
        }

        int frames1 = countFrames(video, RESET_MODE_NONE, -1 /* eosframe */, s);
        int frames2 = countFrames(video, RESET_MODE_RECONFIGURE, -1 /* eosframe */, s);
        int frames3 = countFrames(video, RESET_MODE_FLUSH, -1 /* eosframe */, s);
        assertEquals("different number of frames when using reconfigured codec", frames1, frames2);
        assertEquals("different number of frames when using flushed codec", frames1, frames3);
    }

    private static void verifySecureVideoDecodeSupport(String mime, int width, int height, float rate) {
        MediaFormat baseFormat = new MediaFormat();
        baseFormat.setString(MediaFormat.KEY_MIME, mime);
        baseFormat.setFeatureEnabled(CodecCapabilities.FEATURE_SecurePlayback, true);

        MediaFormat format = MediaFormat.createVideoFormat(mime, width, height);
        format.setFeatureEnabled(CodecCapabilities.FEATURE_SecurePlayback, true);
        format.setFloat(MediaFormat.KEY_FRAME_RATE, rate);

        MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);
        if (mcl.findDecoderForFormat(baseFormat) == null) {
            MediaUtils.skipTest("no secure decoder for " + mime);
            return;
        }
        assertNotNull("no decoder for " + format, mcl.findDecoderForFormat(format));
    }

    private static MediaCodec createDecoder(String mime) {
        try {
            if (false) {
                // change to force testing software codecs
                if (mime.contains("avc")) {
                    return MediaCodec.createByCodecName("OMX.google.h264.decoder");
                } else if (mime.contains("hevc")) {
                    return MediaCodec.createByCodecName("OMX.google.hevc.decoder");
                } else if (mime.contains("3gpp")) {
                    return MediaCodec.createByCodecName("OMX.google.h263.decoder");
                } else if (mime.contains("mp4v")) {
                    return MediaCodec.createByCodecName("OMX.google.mpeg4.decoder");
                } else if (mime.contains("vp8")) {
                    return MediaCodec.createByCodecName("OMX.google.vp8.decoder");
                } else if (mime.contains("vp9")) {
                    return MediaCodec.createByCodecName("OMX.google.vp9.decoder");
                }
            }
            return MediaCodec.createDecoderByType(mime);
        } catch (Exception e) {
            return null;
        }
    }

    private static MediaCodec createDecoder(MediaFormat format) {
        return MediaUtils.getDecoder(format);
    }

    // for video
    private int countFrames(int video, int resetMode, int eosframe, Surface s)
            throws Exception {
        AssetFileDescriptor testFd = mResources.openRawResourceFd(video);
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                testFd.getLength());
        extractor.selectTrack(0);

        int numframes = decodeWithChecks(extractor, CHECKFLAG_RETURN_OUTPUTFRAMES
                | CHECKFLAG_COMPAREINPUTOUTPUTPTSMATCH, resetMode, s,
                eosframe, null, null);

        extractor.release();
        testFd.close();
        return numframes;
    }

    // for audio
    private int countSize(int audio, int resetMode, int eosframe)
            throws Exception {
        AssetFileDescriptor testFd = mResources.openRawResourceFd(audio);
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                testFd.getLength());
        extractor.selectTrack(0);

        // fails CHECKFLAG_COMPAREINPUTOUTPUTPTSMATCH
        int outputSize = decodeWithChecks(extractor, CHECKFLAG_RETURN_OUTPUTSIZE, resetMode, null,
                eosframe, null, null);

        extractor.release();
        testFd.close();
        return outputSize;
    }

    private void testEOSBehavior(int movie, int stopatsample) throws Exception {
        testEOSBehavior(movie, new int[] {stopatsample});
    }

    private void testEOSBehavior(int movie, int[] stopAtSample) throws Exception {
        Surface s = null;
        AssetFileDescriptor testFd = mResources.openRawResourceFd(movie);
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                testFd.getLength());
        extractor.selectTrack(0); // consider variable looping on track
        MediaFormat format = extractor.getTrackFormat(0);
        if (!MediaUtils.checkDecoderForFormat(format)) {
            return; // skip
        }
        List<Long> outputChecksums = new ArrayList<Long>();
        List<Long> outputTimestamps = new ArrayList<Long>();
        Arrays.sort(stopAtSample);
        int last = stopAtSample.length - 1;

        // decode reference (longest sequence to stop at + 100) and
        // store checksums/pts in outputChecksums and outputTimestamps
        // (will fail CHECKFLAG_COMPAREINPUTOUTPUTSAMPLEMATCH)
        decodeWithChecks(extractor,
                CHECKFLAG_SETCHECKSUM | CHECKFLAG_SETPTS | CHECKFLAG_COMPAREINPUTOUTPUTPTSMATCH,
                RESET_MODE_NONE, s,
                stopAtSample[last] + 100, outputChecksums, outputTimestamps);

        // decode stopAtSample requests in reverse order (longest to
        // shortest) and compare to reference checksums/pts in
        // outputChecksums and outputTimestamps
        for (int i = last; i >= 0; --i) {
            if (true) { // reposition extractor
                extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
            } else { // create new extractor
                extractor.release();
                extractor = new MediaExtractor();
                extractor.setDataSource(testFd.getFileDescriptor(),
                        testFd.getStartOffset(), testFd.getLength());
                extractor.selectTrack(0); // consider variable looping on track
            }
            decodeWithChecks(extractor,
                    CHECKFLAG_COMPARECHECKSUM | CHECKFLAG_COMPAREPTS
                    | CHECKFLAG_COMPAREINPUTOUTPUTSAMPLEMATCH
                    | CHECKFLAG_COMPAREINPUTOUTPUTPTSMATCH,
                    RESET_MODE_NONE, s,
                    stopAtSample[i], outputChecksums, outputTimestamps);
        }
        extractor.release();
        testFd.close();
    }

    private static final int CHECKFLAG_SETCHECKSUM = 1 << 0;
    private static final int CHECKFLAG_COMPARECHECKSUM = 1 << 1;
    private static final int CHECKFLAG_SETPTS = 1 << 2;
    private static final int CHECKFLAG_COMPAREPTS = 1 << 3;
    private static final int CHECKFLAG_COMPAREINPUTOUTPUTSAMPLEMATCH = 1 << 4;
    private static final int CHECKFLAG_COMPAREINPUTOUTPUTPTSMATCH = 1 << 5;
    private static final int CHECKFLAG_RETURN_OUTPUTFRAMES = 1 << 6;
    private static final int CHECKFLAG_RETURN_OUTPUTSIZE = 1 << 7;

    /**
     * Decodes frames with parameterized checks and return values.
     * The integer return can be selected through the checkFlags variable.
     */
    private static int decodeWithChecks(MediaExtractor extractor, int checkFlags, int resetMode,
            Surface surface, int stopAtSample,
            List<Long> outputChecksums, List<Long> outputTimestamps)
            throws Exception {
        int trackIndex = extractor.getSampleTrackIndex();
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        String mime = format.getString(MediaFormat.KEY_MIME);
        boolean isAudio = mime.startsWith("audio/");
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        MediaCodec codec = createDecoder(format);
        Log.i("@@@@", "using codec: " + codec.getName());
        codec.configure(format, surface, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        if (resetMode == RESET_MODE_RECONFIGURE) {
            codec.stop();
            codec.configure(format, surface, null /* crypto */, 0 /* flags */);
            codec.start();
            codecInputBuffers = codec.getInputBuffers();
            codecOutputBuffers = codec.getOutputBuffers();
        } else if (resetMode == RESET_MODE_FLUSH) {
            codec.flush();
        }

        // start decode loop
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        final long kTimeOutUs = 5000; // 5ms timeout
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int deadDecoderCounter = 0;
        int samplenum = 0;
        int numframes = 0;
        int outputSize = 0;
        int width = 0;
        int height = 0;
        boolean dochecksum = false;
        ArrayList<Long> timestamps = new ArrayList<Long>();
        if ((checkFlags & CHECKFLAG_SETPTS) != 0) {
            outputTimestamps.clear();
        }
        if ((checkFlags & CHECKFLAG_SETCHECKSUM) != 0) {
            outputChecksums.clear();
        }
        while (!sawOutputEOS && deadDecoderCounter < 100) {
            // handle input
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);
                    long presentationTimeUs = extractor.getSampleTime();
                    boolean advanceDone = extractor.advance();
                    // int flags = extractor.getSampleFlags();
                    // Log.i("@@@@", "read sample " + samplenum + ":" +
                    // extractor.getSampleFlags()
                    // + " @ " + extractor.getSampleTime() + " size " +
                    // sampleSize);
                    assertEquals("extractor.advance() should match end of stream", sampleSize >= 0,
                            advanceDone);

                    if (sampleSize < 0) {
                        Log.d(TAG, "saw input EOS.");
                        sawInputEOS = true;
                        assertEquals("extractor.readSampleData() must return -1 at end of stream",
                                -1, sampleSize);
                        assertEquals("extractor.getSampleTime() must return -1 at end of stream",
                                -1, presentationTimeUs);
                        sampleSize = 0; // required otherwise queueInputBuffer
                                        // returns invalid.
                    } else {
                        timestamps.add(presentationTimeUs);
                        samplenum++; // increment before comparing with stopAtSample
                        if (samplenum == stopAtSample) {
                            Log.d(TAG, "saw input EOS (stop at sample).");
                            sawInputEOS = true; // tag this sample as EOS
                        }
                    }
                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                } else {
                    assertEquals(
                            "codec.dequeueInputBuffer() unrecognized return value: " + inputBufIndex,
                            MediaCodec.INFO_TRY_AGAIN_LATER, inputBufIndex);
                }
            }

            // handle output
            int outputBufIndex = codec.dequeueOutputBuffer(info, kTimeOutUs);

            deadDecoderCounter++;
            if (outputBufIndex >= 0) {
                if (info.size > 0) { // Disregard 0-sized buffers at the end.
                    deadDecoderCounter = 0;
                    if (resetMode != RESET_MODE_NONE) {
                        // once we've gotten some data out of the decoder, reset
                        // and start again
                        if (resetMode == RESET_MODE_RECONFIGURE) {
                            codec.stop();
                            codec.configure(format, surface /* surface */, null /* crypto */,
                                    0 /* flags */);
                            codec.start();
                            codecInputBuffers = codec.getInputBuffers();
                            codecOutputBuffers = codec.getOutputBuffers();
                        } else if (resetMode == RESET_MODE_FLUSH) {
                            codec.flush();
                        } else {
                            fail("unknown resetMode: " + resetMode);
                        }
                        // restart at beginning, clear resetMode
                        resetMode = RESET_MODE_NONE;
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                        sawInputEOS = false;
                        numframes = 0;
                        timestamps.clear();
                        if ((checkFlags & CHECKFLAG_SETPTS) != 0) {
                            outputTimestamps.clear();
                        }
                        if ((checkFlags & CHECKFLAG_SETCHECKSUM) != 0) {
                            outputChecksums.clear();
                        }
                        continue;
                    }
                    if ((checkFlags & CHECKFLAG_COMPAREPTS) != 0) {
                        assertTrue("number of frames (" + numframes
                                + ") exceeds number of reference timestamps",
                                numframes < outputTimestamps.size());
                        assertEquals("frame ts mismatch at frame " + numframes,
                                (long) outputTimestamps.get(numframes), info.presentationTimeUs);
                    } else if ((checkFlags & CHECKFLAG_SETPTS) != 0) {
                        outputTimestamps.add(info.presentationTimeUs);
                    }
                    if ((checkFlags & (CHECKFLAG_SETCHECKSUM | CHECKFLAG_COMPARECHECKSUM)) != 0) {
                        long sum = 0;   // note: checksum is 0 if buffer format unrecognized
                        if (dochecksum) {
                            Image image = codec.getOutputImage(outputBufIndex);
                            // use image to do crc if it's available
                            // fall back to buffer if image is not available
                            if (image != null) {
                                sum = checksum(image);
                            } else {
                                // TODO: add stride - right now just use info.size (as before)
                                //sum = checksum(codecOutputBuffers[outputBufIndex], width, height,
                                //        stride);
                                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufIndex);
                                outputBuffer.position(info.offset);
                                sum = checksum(outputBuffer, info.size);
                            }
                        }
                        if ((checkFlags & CHECKFLAG_COMPARECHECKSUM) != 0) {
                            assertTrue("number of frames (" + numframes
                                    + ") exceeds number of reference checksums",
                                    numframes < outputChecksums.size());
                            Log.d(TAG, "orig checksum: " + outputChecksums.get(numframes)
                                    + " new checksum: " + sum);
                            assertEquals("frame data mismatch at frame " + numframes,
                                    (long) outputChecksums.get(numframes), sum);
                        } else if ((checkFlags & CHECKFLAG_SETCHECKSUM) != 0) {
                            outputChecksums.add(sum);
                        }
                    }
                    if ((checkFlags & CHECKFLAG_COMPAREINPUTOUTPUTPTSMATCH) != 0) {
                        assertTrue("output timestamp " + info.presentationTimeUs
                                + " without corresponding input timestamp"
                                , timestamps.remove(info.presentationTimeUs));
                    }
                    outputSize += info.size;
                    numframes++;
                }
                // Log.d(TAG, "got frame, size " + info.size + "/" +
                // info.presentationTimeUs +
                // "/" + numframes + "/" + info.flags);
                codec.releaseOutputBuffer(outputBufIndex, true /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
                Log.d(TAG, "output buffers have changed.");
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();
                if (oformat.containsKey(MediaFormat.KEY_COLOR_FORMAT) &&
                        oformat.containsKey(MediaFormat.KEY_WIDTH) &&
                        oformat.containsKey(MediaFormat.KEY_HEIGHT)) {
                    int colorFormat = oformat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    width = oformat.getInteger(MediaFormat.KEY_WIDTH);
                    height = oformat.getInteger(MediaFormat.KEY_HEIGHT);
                    dochecksum = isRecognizedFormat(colorFormat); // only checksum known raw
                                                                  // buf formats
                    Log.d(TAG, "checksum fmt: " + colorFormat + " dim " + width + "x" + height);
                } else {
                    dochecksum = false; // check with audio later
                    width = height = 0;
                    Log.d(TAG, "output format has changed to (unknown video) " + oformat);
                }
            } else {
                assertEquals(
                        "codec.dequeueOutputBuffer() unrecognized return index: "
                                + outputBufIndex,
                        MediaCodec.INFO_TRY_AGAIN_LATER, outputBufIndex);
            }
        }
        codec.stop();
        codec.release();

        assertTrue("last frame didn't have EOS", sawOutputEOS);
        if ((checkFlags & CHECKFLAG_COMPAREINPUTOUTPUTSAMPLEMATCH) != 0) {
            assertEquals("I!=O", samplenum, numframes);
            if (stopAtSample != 0) {
                assertEquals("did not stop with right number of frames", stopAtSample, numframes);
            }
        }
        return (checkFlags & CHECKFLAG_RETURN_OUTPUTSIZE) != 0 ? outputSize :
                (checkFlags & CHECKFLAG_RETURN_OUTPUTFRAMES) != 0 ? numframes :
                        0;
    }

    public void testEOSBehaviorH264() throws Exception {
        // this video has an I frame at 44
        testEOSBehavior(R.raw.video_480x360_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz,
                new int[] {44, 45, 55});
    }
    public void testEOSBehaviorHEVC() throws Exception {
        testEOSBehavior(R.raw.video_480x360_mp4_hevc_650kbps_30fps_aac_stereo_128kbps_48000hz, 17);
        testEOSBehavior(R.raw.video_480x360_mp4_hevc_650kbps_30fps_aac_stereo_128kbps_48000hz, 23);
        testEOSBehavior(R.raw.video_480x360_mp4_hevc_650kbps_30fps_aac_stereo_128kbps_48000hz, 49);
    }

    public void testEOSBehaviorH263() throws Exception {
        // this video has an I frame every 12 frames.
        testEOSBehavior(R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_22050hz,
                new int[] {24, 25, 48, 50});
    }

    public void testEOSBehaviorMpeg4() throws Exception {
        // this video has an I frame every 12 frames
        testEOSBehavior(R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz,
                new int[] {24, 25, 48, 50, 2});
    }

    public void testEOSBehaviorVP8() throws Exception {
        // this video has an I frame at 46
        testEOSBehavior(R.raw.video_480x360_webm_vp8_333kbps_25fps_vorbis_stereo_128kbps_48000hz,
                new int[] {46, 47, 57, 45});
    }

    public void testEOSBehaviorVP9() throws Exception {
        // this video has an I frame at 44
        testEOSBehavior(R.raw.video_480x360_webm_vp9_333kbps_25fps_vorbis_stereo_128kbps_48000hz,
                new int[] {44, 45, 55, 43});
    }

    /* from EncodeDecodeTest */
    private static boolean isRecognizedFormat(int colorFormat) {
        // Log.d(TAG, "color format: " + String.format("0x%08x", colorFormat));
        switch (colorFormat) {
        // these are the formats we know how to handle for this test
            case CodecCapabilities.COLOR_FormatYUV420Planar:
            case CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
            case CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
                /*
                 * TODO: Check newer formats or ignore.
                 * OMX_SEC_COLOR_FormatNV12Tiled = 0x7FC00002
                 * OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka = 0x7FA30C03: N4/N7_2
                 * OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar32m = 0x7FA30C04: N5
                 */
                return true;
            default:
                return false;
        }
    }

    private static long checksum(ByteBuffer buf, int size) {
        int cap = buf.capacity();
        assertTrue("checksum() params are invalid: size = " + size + " cap = " + cap,
                size > 0 && size <= cap);
        CRC32 crc = new CRC32();
        if (buf.hasArray()) {
            crc.update(buf.array(), buf.position() + buf.arrayOffset(), size);
        } else {
            int pos = buf.position();
            final int rdsize = Math.min(4096, size);
            byte bb[] = new byte[rdsize];
            int chk;
            for (int i = 0; i < size; i += chk) {
                chk = Math.min(rdsize, size - i);
                buf.get(bb, 0, chk);
                crc.update(bb, 0, chk);
            }
            buf.position(pos);
        }
        return crc.getValue();
    }

    private static long checksum(ByteBuffer buf, int width, int height, int stride) {
        int cap = buf.capacity();
        assertTrue("checksum() params are invalid: w x h , s = "
                + width + " x " + height + " , " + stride + " cap = " + cap,
                width > 0 && width <= stride && height > 0 && height * stride <= cap);
        // YUV 4:2:0 should generally have a data storage height 1.5x greater
        // than the declared image height, representing the UV planes.
        //
        // We only check Y frame for now. Somewhat unknown with tiling effects.
        //
        //long tm = System.nanoTime();
        final int lineinterval = 1; // line sampling frequency
        CRC32 crc = new CRC32();
        if (buf.hasArray()) {
            byte b[] = buf.array();
            int offs = buf.arrayOffset();
            for (int i = 0; i < height; i += lineinterval) {
                crc.update(b, i * stride + offs, width);
            }
        } else { // almost always ends up here due to direct buffers
            int pos = buf.position();
            if (true) { // this {} is 80x times faster than else {} below.
                byte[] bb = new byte[width]; // local line buffer
                for (int i = 0; i < height; i += lineinterval) {
                    buf.position(pos + i * stride);
                    buf.get(bb, 0, width);
                    crc.update(bb, 0, width);
                }
            } else {
                for (int i = 0; i < height; i += lineinterval) {
                    buf.position(pos + i * stride);
                    for (int j = 0; j < width; ++j) {
                        crc.update(buf.get());
                    }
                }
            }
            buf.position(pos);
        }
        //tm = System.nanoTime() - tm;
        //Log.d(TAG, "checksum time " + tm);
        return crc.getValue();
    }

    private static long checksum(Image image) {
        int format = image.getFormat();
        assertEquals("unsupported image format", ImageFormat.YUV_420_888, format);

        CRC32 crc = new CRC32();

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        Image.Plane[] planes = image.getPlanes();
        for (int i = 0; i < planes.length; ++i) {
            ByteBuffer buf = planes[i].getBuffer();

            int width, height, rowStride, pixelStride, x, y;
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            if (i == 0) {
                width = imageWidth;
                height = imageHeight;
            } else {
                width = imageWidth / 2;
                height = imageHeight /2;
            }
            // local contiguous pixel buffer
            byte[] bb = new byte[width * height];
            if (buf.hasArray()) {
                byte b[] = buf.array();
                int offs = buf.arrayOffset();
                if (pixelStride == 1) {
                    for (y = 0; y < height; ++y) {
                        System.arraycopy(bb, y * width, b, y * rowStride + offs, width);
                    }
                } else {
                    // do it pixel-by-pixel
                    for (y = 0; y < height; ++y) {
                        int lineOffset = offs + y * rowStride;
                        for (x = 0; x < width; ++x) {
                            bb[y * width + x] = b[lineOffset + x * pixelStride];
                        }
                    }
                }
            } else { // almost always ends up here due to direct buffers
                int pos = buf.position();
                if (pixelStride == 1) {
                    for (y = 0; y < height; ++y) {
                        buf.position(pos + y * rowStride);
                        buf.get(bb, y * width, width);
                    }
                } else {
                    // local line buffer
                    byte[] lb = new byte[rowStride];
                    // do it pixel-by-pixel
                    for (y = 0; y < height; ++y) {
                        buf.position(pos + y * rowStride);
                        // we're only guaranteed to have pixelStride * (width - 1) + 1 bytes
                        buf.get(lb, 0, pixelStride * (width - 1) + 1);
                        for (x = 0; x < width; ++x) {
                            bb[y * width + x] = lb[x * pixelStride];
                        }
                    }
                }
                buf.position(pos);
            }
            crc.update(bb, 0, width * height);
        }

        return crc.getValue();
    }

    public void testFlush() throws Exception {
        testFlush(R.raw.loudsoftwav);
        testFlush(R.raw.loudsoftogg);
        testFlush(R.raw.loudsoftmp3);
        testFlush(R.raw.loudsoftaac);
        testFlush(R.raw.loudsoftfaac);
        testFlush(R.raw.loudsoftitunes);
    }

    private void testFlush(int resource) throws Exception {

        AssetFileDescriptor testFd = mResources.openRawResourceFd(resource);

        MediaExtractor extractor;
        MediaCodec codec;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        extractor = new MediaExtractor();
        extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                testFd.getLength());
        testFd.close();

        assertEquals("wrong number of tracks", 1, extractor.getTrackCount());
        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        assertTrue("not an audio file", mime.startsWith("audio/"));

        codec = MediaCodec.createDecoderByType(mime);
        assertNotNull("couldn't find codec " + mime, codec);

        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        extractor.selectTrack(0);

        // decode a bit of the first part of the file, and verify the amplitude
        short maxvalue1 = getAmplitude(extractor, codec);

        // flush the codec and seek the extractor a different position, then decode a bit more
        // and check the amplitude
        extractor.seekTo(8000000, 0);
        codec.flush();
        short maxvalue2 = getAmplitude(extractor, codec);

        assertTrue("first section amplitude too low", maxvalue1 > 20000);
        assertTrue("second section amplitude too high", maxvalue2 < 5000);
        codec.stop();
        codec.release();

    }

    private short getAmplitude(MediaExtractor extractor, MediaCodec codec) {
        short maxvalue = 0;
        int numBytesDecoded = 0;
        final long kTimeOutUs = 5000;
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while(numBytesDecoded < 44100 * 2) {
            int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                int sampleSize = extractor.readSampleData(dstBuf, 0 /* offset */);
                long presentationTimeUs = extractor.getSampleTime();

                codec.queueInputBuffer(
                        inputBufIndex,
                        0 /* offset */,
                        sampleSize,
                        presentationTimeUs,
                        0 /* flags */);

                extractor.advance();
            }
            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                buf.position(info.offset);
                for (int i = 0; i < info.size; i += 2) {
                    short sample = buf.getShort();
                    if (maxvalue < sample) {
                        maxvalue = sample;
                    }
                    int idx = (numBytesDecoded + i) / 2;
                }

                numBytesDecoded += info.size;

                codec.releaseOutputBuffer(outputBufIndex, false /* render */);
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();
            }
        }
        return maxvalue;
    }

    /* return true if a particular video feature is supported for the given mimetype */
    private boolean isVideoFeatureSupported(String mimeType, String feature) {
        MediaFormat format = MediaFormat.createVideoFormat( mimeType, 1920, 1080);
        format.setFeatureEnabled(feature, true);
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);
        String codecName = mcl.findDecoderForFormat(format);
        return (codecName == null) ? false : true;
    }


    /**
     * Test tunneled video playback mode if supported
     */
    public void testTunneledVideoPlayback() throws Exception {
        if (!isVideoFeatureSupported(MediaFormat.MIMETYPE_VIDEO_AVC,
                CodecCapabilities.FEATURE_TunneledPlayback)) {
            MediaUtils.skipTest(TAG, "No tunneled video playback codec found!");
            return;
        }

        AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mMediaCodecPlayer = new MediaCodecTunneledPlayer(
                getActivity().getSurfaceHolder(), true, am.generateAudioSessionId());

        mMediaCodecPlayer.setAudioDataSource(AUDIO_URL, null);
        mMediaCodecPlayer.setVideoDataSource(VIDEO_URL, null);
        assertTrue("MediaCodecPlayer.start() failed!", mMediaCodecPlayer.start());
        assertTrue("MediaCodecPlayer.prepare() failed!", mMediaCodecPlayer.prepare());

        // starts video playback
        mMediaCodecPlayer.startThread();

        long timeOut = System.currentTimeMillis() + 4*PLAY_TIME_MS;
        while (timeOut > System.currentTimeMillis() && !mMediaCodecPlayer.isEnded()) {
            Thread.sleep(SLEEP_TIME_MS);
            if (mMediaCodecPlayer.getCurrentPosition() >= mMediaCodecPlayer.getDuration() ) {
                Log.d(TAG, "testTunneledVideoPlayback -- current pos = " +
                        mMediaCodecPlayer.getCurrentPosition() +
                        ">= duration = " + mMediaCodecPlayer.getDuration());
                break;
            }
        }
        assertTrue("Tunneled video playback timeout exceeded!",
                timeOut > System.currentTimeMillis());

        Log.d(TAG, "playVideo player.reset()");
        mMediaCodecPlayer.reset();
    }

    /**
     * Test tunneled video playback flush if supported
     */
    public void testTunneledVideoFlush() throws Exception {
        if (!isVideoFeatureSupported(MediaFormat.MIMETYPE_VIDEO_AVC,
                CodecCapabilities.FEATURE_TunneledPlayback)) {
            MediaUtils.skipTest(TAG, "No tunneled video playback codec found!");
            return;
        }

        AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mMediaCodecPlayer = new MediaCodecTunneledPlayer(
                getActivity().getSurfaceHolder(), true, am.generateAudioSessionId());

        mMediaCodecPlayer.setAudioDataSource(AUDIO_URL, null);
        mMediaCodecPlayer.setVideoDataSource(VIDEO_URL, null);
        assertTrue("MediaCodecPlayer.start() failed!", mMediaCodecPlayer.start());
        assertTrue("MediaCodecPlayer.prepare() failed!", mMediaCodecPlayer.prepare());

        // starts video playback
        mMediaCodecPlayer.startThread();
        Thread.sleep(SLEEP_TIME_MS);
        mMediaCodecPlayer.pause();
        mMediaCodecPlayer.flush();
        Thread.sleep(SLEEP_TIME_MS);
        mMediaCodecPlayer.reset();
    }
}

