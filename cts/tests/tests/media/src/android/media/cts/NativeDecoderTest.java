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

package android.media.cts;

import com.android.cts.media.R;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.cts.util.MediaUtils;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;
import android.webkit.cts.CtsTestServer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NativeDecoderTest extends MediaPlayerTestBase {
    private static final String TAG = "DecoderTest";

    private static final int RESET_MODE_NONE = 0;
    private static final int RESET_MODE_RECONFIGURE = 1;
    private static final int RESET_MODE_FLUSH = 2;
    private static final int RESET_MODE_EOS_FLUSH = 3;

    private static final String[] CSD_KEYS = new String[] { "csd-0", "csd-1" };

    private static final int CONFIG_MODE_NONE = 0;
    private static final int CONFIG_MODE_QUEUE = 1;

    private static Resources mResources;
    short[] mMasterBuffer;

    /** Load jni on initialization */
    static {
        Log.i("@@@", "before loadlibrary");
        System.loadLibrary("ctsmediacodec_jni");
        Log.i("@@@", "after loadlibrary");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();

    }

    // check that native extractor behavior matches java extractor

    public void testExtractor() throws Exception {
        testExtractor(R.raw.sinesweepogg);
        testExtractor(R.raw.sinesweepmp3lame);
        testExtractor(R.raw.sinesweepmp3smpb);
        testExtractor(R.raw.sinesweepm4a);
        testExtractor(R.raw.sinesweepflac);
        testExtractor(R.raw.sinesweepwav);

        testExtractor(R.raw.video_1280x720_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz);
        testExtractor(R.raw.video_1280x720_webm_vp8_333kbps_25fps_vorbis_stereo_128kbps_44100hz);
        testExtractor(R.raw.video_1280x720_webm_vp9_309kbps_25fps_vorbis_stereo_128kbps_48000hz);
        testExtractor(R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_mono_24kbps_11025hz);
        testExtractor(R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz);

        CtsTestServer foo = new CtsTestServer(mContext);
        testExtractor(foo.getAssetUrl("noiseandchirps.ogg"));
        testExtractor(foo.getAssetUrl("ringer.mp3"));
        testExtractor(foo.getRedirectingAssetUrl("ringer.mp3"));
    }

    private void testExtractor(String path) throws Exception {
        int[] jsizes = getSampleSizes(path);
        int[] nsizes = getSampleSizesNativePath(path);

        //Log.i("@@@", Arrays.toString(jsizes));
        assertTrue("different samplesizes", Arrays.equals(jsizes, nsizes));
    }

    private void testExtractor(int res) throws Exception {
        AssetFileDescriptor fd = mResources.openRawResourceFd(res);

        int[] jsizes = getSampleSizes(
                fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        int[] nsizes = getSampleSizesNative(
                fd.getParcelFileDescriptor().getFd(), fd.getStartOffset(), fd.getLength());

        fd.close();
        //Log.i("@@@", Arrays.toString(jsizes));
        assertTrue("different samplesizes", Arrays.equals(jsizes, nsizes));
    }

    private static int[] getSampleSizes(String path) throws IOException {
        MediaExtractor ex = new MediaExtractor();
        ex.setDataSource(path);
        return getSampleSizes(ex);
    }

    private static int[] getSampleSizes(FileDescriptor fd, long offset, long size)
            throws IOException {
        MediaExtractor ex = new MediaExtractor();
        ex.setDataSource(fd, offset, size);
        return getSampleSizes(ex);
    }

    private static int[] getSampleSizes(MediaExtractor ex) {
        ArrayList<Integer> foo = new ArrayList<Integer>();
        ByteBuffer buf = ByteBuffer.allocate(1024*1024);
        int numtracks = ex.getTrackCount();
        assertTrue("no tracks", numtracks > 0);
        foo.add(numtracks);
        for (int i = 0; i < numtracks; i++) {
            MediaFormat format = ex.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                foo.add(0);
                foo.add(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                foo.add(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                foo.add((int)format.getLong(MediaFormat.KEY_DURATION));
            } else if (mime.startsWith("video/")) {
                foo.add(1);
                foo.add(format.getInteger(MediaFormat.KEY_WIDTH));
                foo.add(format.getInteger(MediaFormat.KEY_HEIGHT));
                foo.add((int)format.getLong(MediaFormat.KEY_DURATION));
            } else {
                fail("unexpected mime type: " + mime);
            }
            ex.selectTrack(i);
        }
        while(true) {
            int n = ex.readSampleData(buf, 0);
            if (n < 0) {
                break;
            }
            foo.add(n);
            foo.add(ex.getSampleTrackIndex());
            foo.add(ex.getSampleFlags());
            foo.add((int)ex.getSampleTime()); // just the low bits should be OK
            ex.advance();
        }

        int [] ret = new int[foo.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = foo.get(i);
        }
        return ret;
    }

    private static native int[] getSampleSizesNative(int fd, long offset, long size);
    private static native int[] getSampleSizesNativePath(String path);


    public void testDecoder() throws Exception {
        int testsRun =
            testDecoder(R.raw.sinesweepogg) +
            testDecoder(R.raw.sinesweepmp3lame) +
            testDecoder(R.raw.sinesweepmp3smpb) +
            testDecoder(R.raw.sinesweepm4a) +
            testDecoder(R.raw.sinesweepflac) +
            testDecoder(R.raw.sinesweepwav) +

            testDecoder(R.raw.video_1280x720_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz) +
            testDecoder(R.raw.video_1280x720_webm_vp8_333kbps_25fps_vorbis_stereo_128kbps_44100hz) +
            testDecoder(R.raw.video_1280x720_webm_vp9_309kbps_25fps_vorbis_stereo_128kbps_48000hz) +
            testDecoder(R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_mono_24kbps_11025hz) +
            testDecoder(R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz);
        if (testsRun == 0) {
            MediaUtils.skipTest("no decoders found");
        }
    }

    private int testDecoder(int res) throws Exception {
        if (!MediaUtils.hasCodecsForResource(mContext, res)) {
            return 0; // skip
        }

        AssetFileDescriptor fd = mResources.openRawResourceFd(res);

        int[] jdata = getDecodedData(
                fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        int[] ndata = getDecodedDataNative(
                fd.getParcelFileDescriptor().getFd(), fd.getStartOffset(), fd.getLength());

        fd.close();
        Log.i("@@@", Arrays.toString(jdata));
        Log.i("@@@", Arrays.toString(ndata));
        assertEquals("number of samples differs", jdata.length, ndata.length);
        assertTrue("different decoded data", Arrays.equals(jdata, ndata));
        return 1;
    }

    private static int[] getDecodedData(FileDescriptor fd, long offset, long size)
            throws IOException {
        MediaExtractor ex = new MediaExtractor();
        ex.setDataSource(fd, offset, size);
        return getDecodedData(ex);
    }
    private static int[] getDecodedData(MediaExtractor ex) throws IOException {
        int numtracks = ex.getTrackCount();
        assertTrue("no tracks", numtracks > 0);
        ArrayList<Integer>[] trackdata = new ArrayList[numtracks];
        MediaCodec[] codec = new MediaCodec[numtracks];
        MediaFormat[] format = new MediaFormat[numtracks];
        ByteBuffer[][] inbuffers = new ByteBuffer[numtracks][];
        ByteBuffer[][] outbuffers = new ByteBuffer[numtracks][];
        for (int i = 0; i < numtracks; i++) {
            format[i] = ex.getTrackFormat(i);
            String mime = format[i].getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/") || mime.startsWith("video/")) {
                codec[i] = MediaCodec.createDecoderByType(mime);
                codec[i].configure(format[i], null, null, 0);
                codec[i].start();
                inbuffers[i] = codec[i].getInputBuffers();
                outbuffers[i] = codec[i].getOutputBuffers();
                trackdata[i] = new ArrayList<Integer>();
            } else {
                fail("unexpected mime type: " + mime);
            }
            ex.selectTrack(i);
        }

        boolean[] sawInputEOS = new boolean[numtracks];
        boolean[] sawOutputEOS = new boolean[numtracks];
        int eosCount = 0;
        BufferInfo info = new BufferInfo();
        while(eosCount < numtracks) {
            int t = ex.getSampleTrackIndex();
            if (t >= 0) {
                assertFalse("saw input EOS twice", sawInputEOS[t]);
                int bufidx = codec[t].dequeueInputBuffer(5000);
                if (bufidx >= 0) {
                    Log.i("@@@@", "track " + t + " buffer " + bufidx);
                    ByteBuffer buf = inbuffers[t][bufidx];
                    int sampleSize = ex.readSampleData(buf, 0);
                    Log.i("@@@@", "read " + sampleSize);
                    if (sampleSize < 0) {
                        sampleSize = 0;
                        sawInputEOS[t] = true;
                        Log.i("@@@@", "EOS");
                        //break;
                    }
                    long presentationTimeUs = ex.getSampleTime();

                    codec[t].queueInputBuffer(bufidx, 0, sampleSize, presentationTimeUs,
                            sawInputEOS[t] ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    ex.advance();
                }
            } else {
                Log.i("@@@@", "no more input samples");
                for (int tt = 0; tt < codec.length; tt++) {
                    if (!sawInputEOS[tt]) {
                        // we ran out of samples without ever signaling EOS to the codec,
                        // so do that now
                        int bufidx = codec[tt].dequeueInputBuffer(5000);
                        if (bufidx >= 0) {
                            codec[tt].queueInputBuffer(bufidx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            sawInputEOS[tt] = true;
                        }
                    }
                }
            }

            // see if any of the codecs have data available
            for (int tt = 0; tt < codec.length; tt++) {
                if (!sawOutputEOS[tt]) {
                    int status = codec[tt].dequeueOutputBuffer(info, 1);
                    if (status >= 0) {
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.i("@@@@", "EOS on track " + tt);
                            sawOutputEOS[tt] = true;
                            eosCount++;
                        }
                        Log.i("@@@@", "got decoded buffer for track " + tt + ", size " + info.size);
                        if (info.size > 0) {
                            addSampleData(trackdata[tt], outbuffers[tt][status], info.size, format[tt]);
                        }
                        codec[tt].releaseOutputBuffer(status, false);
                    } else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        Log.i("@@@@", "output buffers changed for track " + tt);
                        outbuffers[tt] = codec[tt].getOutputBuffers();
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        format[tt] = codec[tt].getOutputFormat();
                        Log.i("@@@@", "format changed for track " + t + ": " + format[tt].toString());
                    } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.i("@@@@", "no buffer right now for track " + tt);
                    } else {
                        Log.i("@@@@", "unexpected info code for track " + tt + ": " + status);
                    }
                } else {
                    Log.i("@@@@", "already at EOS on track " + tt);
                }
            }
        }

        int totalsize = 0;
        for (int i = 0; i < numtracks; i++) {
            totalsize += trackdata[i].size();
        }
        int[] trackbytes = new int[totalsize];
        int idx = 0;
        for (int i = 0; i < numtracks; i++) {
            ArrayList<Integer> src = trackdata[i];
            int tracksize = src.size();
            for (int j = 0; j < tracksize; j++) {
                trackbytes[idx++] = src.get(j);
            }
        }

        for (int i = 0; i < codec.length; i++) {
            codec[i].release();
        }

        return trackbytes;
    }

    static void addSampleData(ArrayList<Integer> dst,
            ByteBuffer buf, int size, MediaFormat format) throws IOException{

        Log.i("@@@", "addsample " + dst.size() + "/" + size);
        int width = format.getInteger(MediaFormat.KEY_WIDTH, size);
        int stride = format.getInteger(MediaFormat.KEY_STRIDE, width);
        int height = format.getInteger(MediaFormat.KEY_HEIGHT, 1);
        byte[] bb = new byte[width * height];
        for (int i = 0; i < height; i++) {
            buf.position(i * stride);
            buf.get(bb, i * width, width);
        }
        // bb is filled with data
        long sum = adler32(bb);
        dst.add( (int) (sum & 0xffffffff));
    }

    // simple checksum computed over every decoded buffer
    static long adler32(byte[] input) {
        int a = 1;
        int b = 0;
        for (int i = 0; i < input.length; i++) {
            int unsignedval = input[i];
            if (unsignedval < 0) {
                unsignedval = 256 + unsignedval;
            }
            a += unsignedval;
            b += a;
        }
        a = a % 65521;
        b = b % 65521;
        long ret = b * 65536 + a;
        Log.i("@@@", "adler " + input.length + "/" + ret);
        return ret;
    }

    private static native int[] getDecodedDataNative(int fd, long offset, long size)
            throws IOException;

    public void testVideoPlayback() throws Exception {
        int testsRun =
            testVideoPlayback(
                    R.raw.video_1280x720_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz) +
            testVideoPlayback(
                    R.raw.video_1280x720_webm_vp8_333kbps_25fps_vorbis_stereo_128kbps_44100hz) +
            testVideoPlayback(
                    R.raw.video_1280x720_webm_vp9_309kbps_25fps_vorbis_stereo_128kbps_48000hz) +
            testVideoPlayback(
                    R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_mono_24kbps_11025hz) +
            testVideoPlayback(
                    R.raw.video_480x360_mp4_mpeg4_860kbps_25fps_aac_stereo_128kbps_44100hz);
        if (testsRun == 0) {
            MediaUtils.skipTest("no decoders found");
        }
    }

    private int testVideoPlayback(int res) throws Exception {
        if (!MediaUtils.checkCodecsForResource(mContext, res)) {
            return 0; // skip
        }

        AssetFileDescriptor fd = mResources.openRawResourceFd(res);

        boolean ret = testPlaybackNative(mActivity.getSurfaceHolder().getSurface(),
                fd.getParcelFileDescriptor().getFd(), fd.getStartOffset(), fd.getLength());
        assertTrue("native playback error", ret);
        return 1;
    }

    private static native boolean testPlaybackNative(Surface surface,
            int fd, long startOffset, long length);

    public void testMuxer() throws Exception {
        testMuxer(R.raw.video_1280x720_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz, false);
    }

    private void testMuxer(int res, boolean webm) throws Exception {
        if (!MediaUtils.checkCodecsForResource(mContext, res)) {
            return; // skip
        }

        AssetFileDescriptor infd = mResources.openRawResourceFd(res);

        File base = mContext.getExternalFilesDir(null);
        String tmpFile = base.getPath() + "/tmp.dat";
        Log.i("@@@", "using tmp file " + tmpFile);
        new File(tmpFile).delete();
        ParcelFileDescriptor out = ParcelFileDescriptor.open(new File(tmpFile),
                ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE);

        assertTrue("muxer failed", testMuxerNative(
                infd.getParcelFileDescriptor().getFd(), infd.getStartOffset(), infd.getLength(),
                out.getFd(), webm));

        // compare the original with the remuxed
        MediaExtractor org = new MediaExtractor();
        org.setDataSource(infd.getFileDescriptor(),
                infd.getStartOffset(), infd.getLength());

        MediaExtractor remux = new MediaExtractor();
        remux.setDataSource(out.getFileDescriptor());

        assertEquals("mismatched numer of tracks", org.getTrackCount(), remux.getTrackCount());
        for (int i = 0; i < 2; i++) {
            MediaFormat format1 = org.getTrackFormat(i);
            MediaFormat format2 = remux.getTrackFormat(i);
            Log.i("@@@", "org: " + format1);
            Log.i("@@@", "remux: " + format2);
            assertTrue("different formats", compareFormats(format1, format2));
        }

        org.release();
        remux.release();

        MediaPlayer player1 = MediaPlayer.create(mContext, res);
        MediaPlayer player2 = MediaPlayer.create(mContext, Uri.parse("file://" + tmpFile));
        assertEquals("duration is different", player1.getDuration(), player2.getDuration());
        player1.release();
        player2.release();
        new File(tmpFile).delete();
    }

    boolean compareFormats(MediaFormat f1, MediaFormat f2) {
        // there's no good way to compare two MediaFormats, so compare their string
        // representation
        return f1.toString().equals(f2.toString());
    }

    private static native boolean testMuxerNative(int in, long inoffset, long insize,
            int out, boolean webm);

    public void testFormat() throws Exception {
        assertTrue("media format fail, see log for details", testFormatNative());
    }

    private static native boolean testFormatNative();

    public void testPssh() throws Exception {
        testPssh(R.raw.psshtest);
    }

    private void testPssh(int res) throws Exception {
        AssetFileDescriptor fd = mResources.openRawResourceFd(res);

        MediaExtractor ex = new MediaExtractor();
        ex.setDataSource(fd.getParcelFileDescriptor().getFileDescriptor(),
                fd.getStartOffset(), fd.getLength());
        testPssh(ex);
        ex.release();

        boolean ret = testPsshNative(
                fd.getParcelFileDescriptor().getFd(), fd.getStartOffset(), fd.getLength());
        assertTrue("native pssh error", ret);
    }

    private static void testPssh(MediaExtractor ex) {
        Map<UUID, byte[]> map = ex.getPsshInfo();
        Set<UUID> keys = map.keySet();
        for (UUID uuid: keys) {
            Log.i("@@@", "uuid: " + uuid + ", data size " +
                    map.get(uuid).length);
        }
    }

    private static native boolean testPsshNative(int fd, long offset, long size);

    public void testCryptoInfo() throws Exception {
        assertTrue("native cryptoinfo failed, see log for details", testCryptoInfoNative());
    }

    private static native boolean testCryptoInfoNative();
}

