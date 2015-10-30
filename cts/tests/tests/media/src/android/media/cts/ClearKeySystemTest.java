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

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.media.MediaFormat;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tests of MediaPlayer streaming capabilities.
 */
public class ClearKeySystemTest extends MediaPlayerTestBase {
    private static final String TAG = ClearKeySystemTest.class.getSimpleName();

    // Add additional keys here if the content has more keys.
    private static final byte[] CLEAR_KEY =
        { 0x1a, (byte)0x8a, 0x20, (byte)0x95, (byte)0xe4, (byte)0xde, (byte)0xb2, (byte)0xd2,
          (byte)0x9e, (byte)0xc8, 0x16, (byte)0xac, 0x7b, (byte)0xae, 0x20, (byte)0x82 };

    private static final int SLEEP_TIME_MS = 1000;
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;
    private static final long PLAY_TIME_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
    private static final String MIME_VIDEO_AVC = MediaFormat.MIMETYPE_VIDEO_AVC;

    private static final Uri AUDIO_URL = Uri.parse(
            "http://yt-dash-mse-test.commondatastorage.googleapis.com/media/car_cenc-20120827-8c.mp4");
    private static final Uri VIDEO_URL = Uri.parse(
            "http://yt-dash-mse-test.commondatastorage.googleapis.com/media/car_cenc-20120827-88.mp4");

    private static final UUID CLEARKEY_SCHEME_UUID =
            new UUID(0x1077efecc0b24d02L, 0xace33c1e52e2fb4bL);

    private byte[] mDrmInitData;
    private byte[] mSessionId;
    private Context mContext;
    private final List<byte[]> mClearKeys = new ArrayList<byte[]>() {
        {
            add(CLEAR_KEY);
            // add additional keys here
        }
    };
    private Looper mLooper;
    private MediaCodecCencPlayer mMediaCodecPlayer;
    private MediaDrm mDrm;
    private Object mLock = new Object();
    private SurfaceHolder mSurfaceHolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (false == deviceHasMediaDrm()) {
            tearDown();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private boolean deviceHasMediaDrm() {
        // ClearKey is introduced after KitKat.
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.KITKAT) {
            Log.i(TAG, "This test is designed to work after Android KitKat.");
            return false;
        }
        return true;
    }

    /**
     * Extracts key ids from the pssh blob returned by getKeyRequest() and
     * places it in keyIds.
     * keyRequestBlob format (section 5.1.3.1):
     * https://dvcs.w3.org/hg/html-media/raw-file/default/encrypted-media/encrypted-media.html#clear-key
     *
     * @return size of keyIds vector that contains the key ids, 0 for error
     */
    private int getKeyIds(byte[] keyRequestBlob, Vector<String> keyIds) {
        if (0 == keyRequestBlob.length || keyIds == null)
            return 0;

        String jsonLicenseRequest = new String(keyRequestBlob);
        keyIds.clear();

        try {
            JSONObject license = new JSONObject(jsonLicenseRequest);
            final JSONArray ids = license.getJSONArray("kids");
            for (int i = 0; i < ids.length(); ++i) {
                keyIds.add(ids.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Invalid JSON license = " + jsonLicenseRequest);
            return 0;
        }
        return keyIds.size();
    }

    /**
     * Creates the JSON Web Key string.
     *
     * @return JSON Web Key string.
     */
    private String createJsonWebKeySet(Vector<String> keyIds, Vector<String> keys) {
        String jwkSet = "{\"keys\":[";
        for (int i = 0; i < keyIds.size(); ++i) {
            String id = new String(keyIds.get(i).getBytes(Charset.forName("UTF-8")));
            String key = new String(keys.get(i).getBytes(Charset.forName("UTF-8")));

            jwkSet += "{\"kty\":\"oct\",\"kid\":\"" + id +
                    "\",\"k\":\"" + key + "\"}";
        }
        jwkSet += "]}";
        return jwkSet;
    }

    /**
     * Retrieves clear key ids from getKeyRequest(), create JSON Web Key
     * set and send it to the CDM via provideKeyResponse().
     */
    private void getKeys(MediaDrm drm, byte[] sessionId, byte[] drmInitData) {
        MediaDrm.KeyRequest drmRequest = null;;
        try {
            drmRequest = drm.getKeyRequest(sessionId, drmInitData, "cenc",
                    MediaDrm.KEY_TYPE_STREAMING, null);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Failed to get key request: " + e.toString());
        }
        if (drmRequest == null) {
            Log.e(TAG, "Failed getKeyRequest");
            return;
        }

        Vector<String> keyIds = new Vector<String>();
        if (0 == getKeyIds(drmRequest.getData(), keyIds)) {
            Log.e(TAG, "No key ids found in initData");
            return;
        }

        if (mClearKeys.size() != keyIds.size()) {
            Log.e(TAG, "Mismatch number of key ids and keys: ids=" +
                    keyIds.size() + ", keys=" + mClearKeys.size());
            return;
        }

        // Base64 encodes clearkeys. Keys are known to the application.
        Vector<String> keys = new Vector<String>();
        for (int i = 0; i < mClearKeys.size(); ++i) {
            String clearKey = Base64.encodeToString(mClearKeys.get(i),
                    Base64.NO_PADDING | Base64.NO_WRAP);
            keys.add(clearKey);
        }

        String jwkSet = createJsonWebKeySet(keyIds, keys);
        byte[] jsonResponse = jwkSet.getBytes(Charset.forName("UTF-8"));

        try {
            try {
                drm.provideKeyResponse(sessionId, jsonResponse);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to provide key response: " + e.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to provide key response: " + e.toString());
        }
    }

    private MediaDrm startDrm() {
        new Thread() {
            @Override
            public void run() {
                // Set up a looper to handle events
                Looper.prepare();

                // Save the looper so that we can terminate this thread
                // after we are done with it.
                mLooper = Looper.myLooper();

                try {
                    mDrm = new MediaDrm(CLEARKEY_SCHEME_UUID);
                } catch (MediaDrmException e) {
                    Log.e(TAG, "Failed to create MediaDrm: " + e.getMessage());
                    return;
                }

                synchronized(mLock) {
                    mDrm.setOnEventListener(new MediaDrm.OnEventListener() {
                            @Override
                            public void onEvent(MediaDrm md, byte[] sessionId, int event,
                                    int extra, byte[] data) {
                                if (event == MediaDrm.EVENT_KEY_REQUIRED) {
                                    Log.i(TAG, "MediaDrm event: Key required");
                                    getKeys(mDrm, mSessionId, mDrmInitData);
                                } else if (event == MediaDrm.EVENT_KEY_EXPIRED) {
                                    Log.i(TAG, "MediaDrm event: Key expired");
                                    getKeys(mDrm, mSessionId, mDrmInitData);
                                } else {
                                    Log.e(TAG, "Events not supported" + event);
                                }
                            }
                        });
                    mLock.notify();
                }
                Looper.loop();  // Blocks forever until Looper.quit() is called.
            }
        }.start();

        // wait for mDrm to be created
        synchronized(mLock) {
            try {
                mLock.wait(1000);
            } catch (Exception e) {
            }
        }
        return mDrm;
    }

    private void stopDrm(MediaDrm drm) {
        if (drm != mDrm) {
            Log.e(TAG, "invalid drm specified in stopDrm");
        }
        mLooper.quit();
    }

    private byte[] openSession(MediaDrm drm) {
        byte[] mSessionId = null;
        boolean mRetryOpen;
        do {
            try {
                mRetryOpen = false;
                mSessionId = drm.openSession();
            } catch (Exception e) {
                mRetryOpen = true;
            }
        } while (mRetryOpen);
        return mSessionId;
    }

    private void closeSession(MediaDrm drm, byte[] sessionId) {
        drm.closeSession(sessionId);
    }

    public boolean isResolutionSupported(int videoWidth, int videoHeight) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            if  (videoHeight <= 144) {
                return CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QCIF);
            } else if (videoHeight <= 240) {
                return CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QVGA);
            } else if (videoHeight <= 288) {
                return CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF);
            } else if (videoHeight <= 480) {
                return CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P);
            } else if (videoHeight <= 720) {
                return CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P);
            } else if (videoHeight <= 1080) {
                return CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P);
            } else {
                return false;
            }
        }

        MediaFormat format = MediaFormat.createVideoFormat(
                MIME_VIDEO_AVC, videoWidth, videoHeight);
        // using secure codec even though it is clear key DRM
        format.setFeatureEnabled(CodecCapabilities.FEATURE_SecurePlayback, true);
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);
        if (mcl.findDecoderForFormat(format) == null) {
            Log.i(TAG, "could not find codec for " + format);
            return false;
        }
        return true;
    }

    /**
     * Tests clear key system playback.
     */
    public void testClearKeyPlayback() throws Exception {

        MediaDrm drm = startDrm();
        if (null == drm) {
            throw new Error("Failed to create drm.");
        }

        if (!drm.isCryptoSchemeSupported(CLEARKEY_SCHEME_UUID)) {
            stopDrm(drm);
            throw new Error("Crypto scheme is not supported.");
        }

        if (!isResolutionSupported(VIDEO_WIDTH, VIDEO_HEIGHT)) {
            Log.i(TAG, "Device does not support " +
                    VIDEO_WIDTH + "x" + VIDEO_HEIGHT + "resolution.");
            return;
        }

        mSessionId = openSession(drm);
        mMediaCodecPlayer = new MediaCodecCencPlayer(
                getActivity().getSurfaceHolder(), mSessionId);

        mMediaCodecPlayer.setAudioDataSource(AUDIO_URL, null, false);
        mMediaCodecPlayer.setVideoDataSource(VIDEO_URL, null, true);
        mMediaCodecPlayer.start();
        mMediaCodecPlayer.prepare();
        mDrmInitData = mMediaCodecPlayer.getPsshInfo().get(CLEARKEY_SCHEME_UUID);

        getKeys(mDrm, mSessionId, mDrmInitData);
        // starts video playback
        mMediaCodecPlayer.startThread();

        long timeOut = System.currentTimeMillis() + PLAY_TIME_MS * 4;
        while (timeOut > System.currentTimeMillis() && !mMediaCodecPlayer.isEnded()) {
            Thread.sleep(SLEEP_TIME_MS);
            if (mMediaCodecPlayer.getCurrentPosition() >= mMediaCodecPlayer.getDuration() ) {
                Log.d(TAG, "current pos = " + mMediaCodecPlayer.getCurrentPosition() +
                        ">= duration = " + mMediaCodecPlayer.getDuration());
                break;
            }
        }

        Log.d(TAG, "playVideo player.reset()");
        mMediaCodecPlayer.reset();
        closeSession(drm, mSessionId);
        stopDrm(drm);
    }
}
