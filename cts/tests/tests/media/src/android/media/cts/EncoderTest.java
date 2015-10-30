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
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.test.AndroidTestCase;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class EncoderTest extends AndroidTestCase {
    private static final String TAG = "EncoderTest";
    private static final boolean VERBOSE = false;

    private static final int kNumInputBytes = 256 * 1024;
    private static final long kTimeoutUs = 10000;

    @Override
    public void setContext(Context context) {
        super.setContext(context);
    }

    public void testAMRNBEncoders() {
        LinkedList<MediaFormat> formats = new LinkedList<MediaFormat>();

        final int kBitRates[] =
            { 4750, 5150, 5900, 6700, 7400, 7950, 10200, 12200 };

        for (int j = 0; j < kBitRates.length; ++j) {
            MediaFormat format  = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AMR_NB);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 8000);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[j]);
            formats.push(format);
        }

        testEncoderWithFormats(MediaFormat.MIMETYPE_AUDIO_AMR_NB, formats);
    }

    public void testAMRWBEncoders() {
        LinkedList<MediaFormat> formats = new LinkedList<MediaFormat>();

        final int kBitRates[] =
            { 6600, 8850, 12650, 14250, 15850, 18250, 19850, 23050, 23850 };

        for (int j = 0; j < kBitRates.length; ++j) {
            MediaFormat format  = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AMR_WB);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 16000);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[j]);
            formats.push(format);
        }

        testEncoderWithFormats(MediaFormat.MIMETYPE_AUDIO_AMR_WB, formats);
    }

    public void testAACEncoders() {
        LinkedList<MediaFormat> formats = new LinkedList<MediaFormat>();

        final int kAACProfiles[] = {
            2 /* OMX_AUDIO_AACObjectLC */,
            5 /* OMX_AUDIO_AACObjectHE */,
            39 /* OMX_AUDIO_AACObjectELD */
        };

        final int kSampleRates[] = { 8000, 11025, 22050, 44100, 48000 };
        final int kBitRates[] = { 64000, 128000 };

        for (int k = 0; k < kAACProfiles.length; ++k) {
            for (int i = 0; i < kSampleRates.length; ++i) {
                if (kAACProfiles[k] == 5 && kSampleRates[i] < 22050) {
                    // Is this right? HE does not support sample rates < 22050Hz?
                    continue;
                }
                for (int j = 0; j < kBitRates.length; ++j) {
                    for (int ch = 1; ch <= 2; ++ch) {
                        MediaFormat format  = new MediaFormat();
                        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);

                        format.setInteger(
                                MediaFormat.KEY_AAC_PROFILE, kAACProfiles[k]);

                        format.setInteger(
                                MediaFormat.KEY_SAMPLE_RATE, kSampleRates[i]);

                        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, ch);
                        format.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[j]);
                        formats.push(format);
                    }
                }
            }
        }

        testEncoderWithFormats(MediaFormat.MIMETYPE_AUDIO_AAC, formats);
    }

    private void testEncoderWithFormats(
            String mime, List<MediaFormat> formats) {
        List<String> componentNames = getEncoderNamesForType(mime);

        for (String componentName : componentNames) {
            Log.d(TAG, "testing component '" + componentName + "'");
            for (MediaFormat format : formats) {
                Log.d(TAG, "  testing format '" + format + "'");
                assertEquals(mime, format.getString(MediaFormat.KEY_MIME));
                testEncoder(componentName, format);
            }
        }
    }

    private List<String> getEncoderNamesForType(String mime) {
        LinkedList<String> names = new LinkedList<String>();

        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info : mcl.getCodecInfos()) {
            if (!info.isEncoder()) {
                continue;
            }
            for (String type : info.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mime)) {
                    names.push(info.getName());
                    break;
                }
            }
        }

        return names;
    }

    private int queueInputBuffer(
            MediaCodec codec, ByteBuffer[] inputBuffers, int index) {
        ByteBuffer buffer = inputBuffers[index];
        buffer.clear();

        int size = buffer.limit();

        byte[] zeroes = new byte[size];
        buffer.put(zeroes);

        codec.queueInputBuffer(index, 0 /* offset */, size, 0 /* timeUs */, 0);

        return size;
    }

    private void dequeueOutputBuffer(
            MediaCodec codec, ByteBuffer[] outputBuffers,
            int index, MediaCodec.BufferInfo info) {
        codec.releaseOutputBuffer(index, false /* render */);
    }

    private void testEncoder(String componentName, MediaFormat format) {
        MediaCodec codec;
        try {
            codec = MediaCodec.createByCodecName(componentName);
        } catch (Exception e) {
            fail("codec '" + componentName + "' failed construction.");
            return; /* does not get here, but avoids warning */
        }
        try {
            codec.configure(
                    format,
                    null /* surface */,
                    null /* crypto */,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IllegalStateException e) {
            fail("codec '" + componentName + "' failed configuration.");
        }

        codec.start();
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        int numBytesSubmitted = 0;
        boolean doneSubmittingInput = false;
        int numBytesDequeued = 0;

        while (true) {
            int index;

            if (!doneSubmittingInput) {
                index = codec.dequeueInputBuffer(kTimeoutUs /* timeoutUs */);

                if (index != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (numBytesSubmitted >= kNumInputBytes) {
                        codec.queueInputBuffer(
                                index,
                                0 /* offset */,
                                0 /* size */,
                                0 /* timeUs */,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                        if (VERBOSE) {
                            Log.d(TAG, "queued input EOS.");
                        }

                        doneSubmittingInput = true;
                    } else {
                        int size = queueInputBuffer(
                                codec, codecInputBuffers, index);

                        numBytesSubmitted += size;

                        if (VERBOSE) {
                            Log.d(TAG, "queued " + size + " bytes of input data.");
                        }
                    }
                }
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            index = codec.dequeueOutputBuffer(info, kTimeoutUs /* timeoutUs */);

            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
            } else {
                dequeueOutputBuffer(codec, codecOutputBuffers, index, info);

                numBytesDequeued += info.size;

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "dequeued output EOS.");
                    }
                    break;
                }

                if (VERBOSE) {
                    Log.d(TAG, "dequeued " + info.size + " bytes of output data.");
                }
            }
        }

        if (VERBOSE) {
            Log.d(TAG, "queued a total of " + numBytesSubmitted + "bytes, "
                    + "dequeued " + numBytesDequeued + " bytes.");
        }

        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int inBitrate = sampleRate * channelCount * 16;  // bit/sec
        int outBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);

        float desiredRatio = (float)outBitrate / (float)inBitrate;
        float actualRatio = (float)numBytesDequeued / (float)numBytesSubmitted;

        if (actualRatio < 0.9 * desiredRatio || actualRatio > 1.1 * desiredRatio) {
            Log.w(TAG, "desiredRatio = " + desiredRatio
                    + ", actualRatio = " + actualRatio);
        }

        codec.release();
        codec = null;
    }
}

