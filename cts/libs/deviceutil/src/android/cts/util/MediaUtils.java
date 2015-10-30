/*
 * Copyright 2014 The Android Open Source Project
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
package android.cts.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.VideoCapabilities;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Range;

import com.android.cts.util.ReportLog;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;

import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import java.util.Arrays;
import java.util.Map;
import android.util.Log;

import java.io.IOException;

public class MediaUtils {
    private static final String TAG = "MediaUtils";

    private static final int ALL_AV_TRACKS = -1;

    private static final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

    /**
     * Returns the test name (heuristically).
     *
     * Since it uses heuristics, this method has only been verified for media
     * tests. This centralizes the way to signal errors during a test.
     */
    public static String getTestName() {
        int bestScore = -1;
        String testName = "test???";
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : traces.entrySet()) {
            StackTraceElement[] stack = entry.getValue();
            for (int index = 0; index < stack.length; ++index) {
                // method name must start with "test"
                String methodName = stack[index].getMethodName();
                if (!methodName.startsWith("test")) {
                    continue;
                }

                int score = 0;
                // see if there is a public non-static void method that takes no argument
                Class<?> clazz;
                try {
                    clazz = Class.forName(stack[index].getClassName());
                    ++score;
                    for (final Method method : clazz.getDeclaredMethods()) {
                        if (method.getName().equals(methodName)
                                && isPublic(method.getModifiers())
                                && !isStatic(method.getModifiers())
                                && method.getParameterTypes().length == 0
                                && method.getReturnType().equals(Void.TYPE)) {
                            ++score;
                            break;
                        }
                    }
                    if (score == 1) {
                        // if we could read the class, but method is not public void, it is
                        // not a candidate
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                }

                // even if we cannot verify the method signature, there are signals in the stack

                // usually test method is invoked by reflection
                int depth = 1;
                while (index + depth < stack.length
                        && stack[index + depth].getMethodName().equals("invoke")
                        && stack[index + depth].getClassName().equals(
                                "java.lang.reflect.Method")) {
                    ++depth;
                }
                if (depth > 1) {
                    ++score;
                    // and usually test method is run by runMethod method in android.test package
                    if (index + depth < stack.length) {
                        if (stack[index + depth].getClassName().startsWith("android.test.")) {
                            ++score;
                        }
                        if (stack[index + depth].getMethodName().equals("runMethod")) {
                            ++score;
                        }
                    }
                }

                if (score > bestScore) {
                    bestScore = score;
                    testName = methodName;
                }
            }
        }
        return testName;
    }

    /**
     * Finds test name (heuristically) and prints out standard skip message.
     *
     * Since it uses heuristics, this method has only been verified for media
     * tests. This centralizes the way to signal a skipped test.
     */
    public static void skipTest(String tag, String reason) {
        Log.i(tag, "SKIPPING " + getTestName() + "(): " + reason);
    }

    /**
     * Finds test name (heuristically) and prints out standard skip message.
     *
     * Since it uses heuristics, this method has only been verified for media
     * tests.  This centralizes the way to signal a skipped test.
     */
    public static void skipTest(String reason) {
        skipTest(TAG, reason);
    }

    public static boolean check(boolean result, String message) {
        if (!result) {
            skipTest(message);
        }
        return result;
    }

    public static MediaCodec getDecoder(MediaFormat format) {
        String decoder = sMCL.findDecoderForFormat(format);
        if (decoder != null) {
            try {
                return MediaCodec.createByCodecName(decoder);
            } catch (IOException e) {
            }
        }
        return null;
    }

    public static boolean canDecode(MediaFormat format) {
        if (sMCL.findDecoderForFormat(format) == null) {
            Log.i(TAG, "no decoder for " + format);
            return false;
        }
        return true;
    }

    public static boolean supports(String codecName, String mime, int w, int h) {
        MediaCodec codec;
        try {
            codec = MediaCodec.createByCodecName(codecName);
        } catch (IOException e) {
            return false;
        }

        CodecCapabilities cap = null;
        try {
            cap = codec.getCodecInfo().getCapabilitiesForType(mime);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "not supported mime: " + mime);
            codec.release();
            return false;
        }

        VideoCapabilities vidCap = cap.getVideoCapabilities();
        if (vidCap == null) {
            Log.w(TAG, "not a video codec: " + codecName);
            codec.release();
            return false;
        }
        try {
            Range<Double> fps = vidCap.getSupportedFrameRatesFor(w, h);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "unsupported size " + w + "x" + h);
            codec.release();
            return false;
        }
        codec.release();
        return true;
    }

    public static boolean hasCodecForTrack(MediaExtractor ex, int track) {
        int count = ex.getTrackCount();
        if (track < 0 || track >= count) {
            throw new IndexOutOfBoundsException(track + " not in [0.." + (count - 1) + "]");
        }
        return canDecode(ex.getTrackFormat(track));
    }

    /**
     * return true iff all audio and video tracks are supported
     */
    public static boolean hasCodecsForMedia(MediaExtractor ex) {
        for (int i = 0; i < ex.getTrackCount(); ++i) {
            MediaFormat format = ex.getTrackFormat(i);
            // only check for audio and video codecs
            String mime = format.getString(MediaFormat.KEY_MIME).toLowerCase();
            if (!mime.startsWith("audio/") && !mime.startsWith("video/")) {
                continue;
            }
            if (!canDecode(format)) {
                return false;
            }
        }
        return true;
    }

    /**
     * return true iff any track starting with mimePrefix is supported
     */
    public static boolean hasCodecForMediaAndDomain(MediaExtractor ex, String mimePrefix) {
        mimePrefix = mimePrefix.toLowerCase();
        for (int i = 0; i < ex.getTrackCount(); ++i) {
            MediaFormat format = ex.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.toLowerCase().startsWith(mimePrefix)) {
                if (canDecode(format)) {
                    return true;
                }
                Log.i(TAG, "no decoder for " + format);
            }
        }
        return false;
    }

    private static boolean hasCodecsForResourceCombo(
            Context context, int resourceId, int track, String mimePrefix) {
        try {
            AssetFileDescriptor afd = null;
            MediaExtractor ex = null;
            try {
                afd = context.getResources().openRawResourceFd(resourceId);
                ex = new MediaExtractor();
                ex.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                if (mimePrefix != null) {
                    return hasCodecForMediaAndDomain(ex, mimePrefix);
                } else if (track == ALL_AV_TRACKS) {
                    return hasCodecsForMedia(ex);
                } else {
                    return hasCodecForTrack(ex, track);
                }
            } finally {
                if (ex != null) {
                    ex.release();
                }
                if (afd != null) {
                    afd.close();
                }
            }
        } catch (IOException e) {
            Log.i(TAG, "could not open resource");
        }
        return false;
    }

    /**
     * return true iff all audio and video tracks are supported
     */
    public static boolean hasCodecsForResource(Context context, int resourceId) {
        return hasCodecsForResourceCombo(context, resourceId, ALL_AV_TRACKS, null /* mimePrefix */);
    }

    public static boolean checkCodecsForResource(Context context, int resourceId) {
        return check(hasCodecsForResource(context, resourceId), "no decoder found");
    }

    /**
     * return true iff track is supported.
     */
    public static boolean hasCodecForResource(Context context, int resourceId, int track) {
        return hasCodecsForResourceCombo(context, resourceId, track, null /* mimePrefix */);
    }

    public static boolean checkCodecForResource(Context context, int resourceId, int track) {
        return check(hasCodecForResource(context, resourceId, track), "no decoder found");
    }

    /**
     * return true iff any track starting with mimePrefix is supported
     */
    public static boolean hasCodecForResourceAndDomain(
            Context context, int resourceId, String mimePrefix) {
        return hasCodecsForResourceCombo(context, resourceId, ALL_AV_TRACKS, mimePrefix);
    }

    /**
     * return true iff all audio and video tracks are supported
     */
    public static boolean hasCodecsForPath(Context context, String path) {
        MediaExtractor ex = null;
        try {
            ex = new MediaExtractor();
            Uri uri = Uri.parse(path);
            String scheme = uri.getScheme();
            if (scheme == null) { // file
                ex.setDataSource(path);
            } else if (scheme.equalsIgnoreCase("file")) {
                ex.setDataSource(uri.getPath());
            } else {
                ex.setDataSource(context, uri, null);
            }
            return hasCodecsForMedia(ex);
        } catch (IOException e) {
            Log.i(TAG, "could not open path " + path);
        } finally {
            if (ex != null) {
                ex.release();
            }
        }
        return false;
    }

    public static boolean checkCodecsForPath(Context context, String path) {
        return check(hasCodecsForPath(context, path), "no decoder found");
    }

    private static boolean hasCodecForMime(boolean encoder, String mime) {
        for (MediaCodecInfo info : sMCL.getCodecInfos()) {
            if (encoder != info.isEncoder()) {
                continue;
            }

            for (String type : info.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mime)) {
                    Log.i(TAG, "found codec " + info.getName() + " for mime " + mime);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCodecForMimes(boolean encoder, String[] mimes) {
        for (String mime : mimes) {
            if (!hasCodecForMime(encoder, mime)) {
                Log.i(TAG, "no " + (encoder ? "encoder" : "decoder") + " for mime " + mime);
                return false;
            }
        }
        return true;
    }


    public static boolean hasEncoder(String... mimes) {
        return hasCodecForMimes(true /* encoder */, mimes);
    }

    public static boolean hasDecoder(String... mimes) {
        return hasCodecForMimes(false /* encoder */, mimes);
    }

    public static boolean checkDecoder(String... mimes) {
        return check(hasCodecForMimes(false /* encoder */, mimes), "no decoder found");
    }

    public static boolean checkEncoder(String... mimes) {
        return check(hasCodecForMimes(true /* encoder */, mimes), "no encoder found");
    }

    public static boolean canDecodeVideo(String mime, int width, int height, float rate) {
        MediaFormat format = MediaFormat.createVideoFormat(mime, width, height);
        format.setFloat(MediaFormat.KEY_FRAME_RATE, rate);
        return canDecode(format);
    }

    public static boolean checkDecoderForFormat(MediaFormat format) {
        return check(canDecode(format), "no decoder for " + format);
    }

    public static MediaExtractor createMediaExtractorForMimeType(
            Context context, int resourceId, String mimeTypePrefix)
            throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(resourceId);
        try {
            extractor.setDataSource(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } finally {
            afd.close();
        }
        int trackIndex;
        for (trackIndex = 0; trackIndex < extractor.getTrackCount(); trackIndex++) {
            MediaFormat trackMediaFormat = extractor.getTrackFormat(trackIndex);
            if (trackMediaFormat.getString(MediaFormat.KEY_MIME).startsWith(mimeTypePrefix)) {
                extractor.selectTrack(trackIndex);
                break;
            }
        }
        if (trackIndex == extractor.getTrackCount()) {
            extractor.release();
            throw new IllegalStateException("couldn't get a track for " + mimeTypePrefix);
        }

        return extractor;
    }

    /**
     * return the average value of the passed array.
     */
    public static double getAverage(double[] data) {
        int num = data.length;
        if (num == 0) {
            return 0;
        }

        double sum = data[0];
        for (int i = 1; i < num; i++) {
            sum += data[i];
        }
        return sum / num;
    }

    /**
     * return the standard deviation value of the passed array
     */
    public static double getStdev(double[] data) {
        double average = getAverage(data);
        int num = data.length;
        if (num == 0) {
            return 0;
        }
        double variance = 0;
        for (int i = 0; i < num; ++i) {
            variance += (data[i] - average) * (data[i] - average);
        }
        variance /= num;
        return Math.sqrt(variance);
    }

    public static double[] calculateMovingAverage(double[] array, int n) {
        int num = array.length;
        if (num < n) {
            return null;
        }
        int avgsNum = num - n + 1;
        double[] avgs = new double[avgsNum];
        double sum = array[0];
        for (int i = 1; i < n; ++i) {
            sum += array[i];
        }
        avgs[0] = sum / n;

        for (int i = n; i < num; ++i) {
            sum = sum - array[i - n] + array[i];
            avgs[i - n + 1] = sum / n;
        }
        return avgs;
    }

    public static String logResults(ReportLog log, String prefix,
            double min, double max, double avg, double stdev) {
        String msg = prefix;
        msg += " min=" + Math.round(min / 1000) + " max=" + Math.round(max / 1000) +
                " avg=" + Math.round(avg / 1000) + " stdev=" + Math.round(stdev / 1000);
        log.printValue(msg, 1000000000 / min, ResultType.HIGHER_BETTER, ResultUnit.FPS);
        return msg;
    }

    public static VideoCapabilities getVideoCapabilities(String codecName, String mime) {
        for (MediaCodecInfo info : sMCL.getCodecInfos()) {
            if (!info.getName().equalsIgnoreCase(codecName)) {
                continue;
            }
            CodecCapabilities caps;
            try {
                caps = info.getCapabilitiesForType(mime);
            } catch (IllegalArgumentException e) {
                // mime is not supported
                continue;
            }
            return caps.getVideoCapabilities();
        }
        return null;
    }

    public static Range<Double> getAchievableFrameRatesFor(
            String codecName, String mimeType, int width, int height) {
        VideoCapabilities cap = getVideoCapabilities(codecName, mimeType);
        if (cap == null) {
            return null;
        }
        return cap.getAchievableFrameRatesFor(width, height);
    }

    private static final double FRAMERATE_TOLERANCE = Math.sqrt(12.1);
    public static boolean verifyResults(String name, String mime, int w, int h, double measured) {
        Range<Double> reported = getAchievableFrameRatesFor(name, mime, w, h);
        if (reported == null) {
            Log.d(TAG, "Failed to getAchievableFrameRatesFor " +
                    name + " " + mime + " " + w + "x" + h);
            return false;
        }
        double lowerBoundary1 = reported.getLower() / FRAMERATE_TOLERANCE;
        double upperBoundary1 = reported.getUpper() * FRAMERATE_TOLERANCE;
        double lowerBoundary2 = reported.getUpper() / Math.pow(FRAMERATE_TOLERANCE, 2);
        double upperBoundary2 = reported.getLower() * Math.pow(FRAMERATE_TOLERANCE, 2);
        Log.d(TAG, name + " " + mime + " " + w + "x" + h + " " +
                "lowerBoundary1 " + lowerBoundary1 + " upperBoundary1 " + upperBoundary1 +
                " lowerBoundary2 " + lowerBoundary2 + " upperBoundary2 " + upperBoundary2 +
                " measured " + measured);
        return (measured >= lowerBoundary1 && measured <= upperBoundary1 &&
                measured >= lowerBoundary2 && measured <= upperBoundary2);
    }

    public static String getErrorMessage(
            Range<Double> reportedRange, double[] measuredFps, String[] rawData) {
        String msg = "";
        if (reportedRange == null) {
            msg += "Failed to get achievable frame rate.\n";
        } else {
            msg += "Expected achievable frame rate range: " + reportedRange + ".\n";
        }
        msg += "Measured frame rate: " + Arrays.toString(measuredFps) + ".\n";
        msg += "Raw data: " + Arrays.toString(rawData) + ".\n";
        return msg;
    }
}
