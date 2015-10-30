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

package android.media.tv.cts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.media.PlaybackParams;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvView;
import android.media.tv.cts.TvInputServiceTest.CountingTvInputService.CountingSession;
import android.net.Uri;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

import com.android.cts.tv.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Test {@link android.media.tv.TvInputService}.
 */
public class TvInputServiceTest extends ActivityInstrumentationTestCase2<TvViewStubActivity> {
    /** The maximum time to wait for an operation. */
    private static final long TIME_OUT = 15000L;
    private static final String mDummyTrackId = "dummyTrackId";
    private static final TvTrackInfo mDummyTrack =
            new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, mDummyTrackId)
            .setVideoWidth(1920).setVideoHeight(1080).setLanguage("und").build();

    private TvView mTvView;
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private TvInputManager mManager;
    private TvInputInfo mStubInfo;
    private final StubCallback mCallback = new StubCallback();
    private final StubTimeShiftPositionCallback mTimeShiftPositionCallback =
            new StubTimeShiftPositionCallback();

    private static class StubCallback extends TvView.TvInputCallback {
        private int mChannelRetunedCount;
        private int mVideoAvailableCount;
        private int mVideoUnavailableCount;
        private int mTrackSelectedCount;
        private int mTrackChangedCount;
        private int mVideoSizeChanged;
        private int mContentAllowedCount;
        private int mContentBlockedCount;
        private int mTimeShiftStatusChangedCount;

        @Override
        public void onChannelRetuned(String inputId, Uri channelUri) {
            mChannelRetunedCount++;
        }

        @Override
        public void onVideoAvailable(String inputId) {
            mVideoAvailableCount++;
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            mVideoUnavailableCount++;
        }

        @Override
        public void onTrackSelected(String inputId, int type, String trackId) {
            mTrackSelectedCount++;
        }

        @Override
        public void onTracksChanged(String inputId, List<TvTrackInfo> trackList) {
            mTrackChangedCount++;
        }

        @Override
        public void onVideoSizeChanged(String inputId, int width, int height) {
            mVideoSizeChanged++;
        }

        @Override
        public void onContentAllowed(String inputId) {
            mContentAllowedCount++;
        }

        @Override
        public void onContentBlocked(String inputId, TvContentRating rating) {
            mContentBlockedCount++;
        }

        @Override
        public void onTimeShiftStatusChanged(String inputId, int status) {
            mTimeShiftStatusChangedCount++;
        }

        public void resetCounts() {
            mChannelRetunedCount = 0;
            mVideoAvailableCount = 0;
            mVideoUnavailableCount = 0;
            mTrackSelectedCount = 0;
            mTrackChangedCount = 0;
            mContentAllowedCount = 0;
            mContentBlockedCount = 0;
            mTimeShiftStatusChangedCount = 0;
        }
    }

    private static class StubTimeShiftPositionCallback extends TvView.TimeShiftPositionCallback {
        private int mTimeShiftStartPositionChanged;
        private int mTimeShiftCurrentPositionChanged;

        @Override
        public void onTimeShiftStartPositionChanged(String inputId, long timeMs) {
            mTimeShiftStartPositionChanged++;
        }

        @Override
        public void onTimeShiftCurrentPositionChanged(String inputId, long timeMs) {
            mTimeShiftCurrentPositionChanged++;
        }

        public void resetCounts() {
            mTimeShiftStartPositionChanged = 0;
            mTimeShiftCurrentPositionChanged = 0;
        }
    }

    public TvInputServiceTest() {
        super(TvViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mTvView = (TvView) mActivity.findViewById(R.id.tvview);
        mManager = (TvInputManager) mActivity.getSystemService(Context.TV_INPUT_SERVICE);
        for (TvInputInfo info : mManager.getTvInputList()) {
            if (info.getServiceInfo().name.equals(CountingTvInputService.class.getName())) {
                mStubInfo = info;
                break;
            }
        }
        assertNotNull(mStubInfo);
        mTvView.setCallback(mCallback);

        CountingTvInputService.sSession = null;
    }

    public void testTvInputService() throws Throwable {
        if (!Utils.hasTvInputFramework(getActivity())) {
            return;
        }
        verifyCommandTune();
        verifyCommandSetStreamVolume();
        verifyCommandSetCaptionEnabled();
        verifyCommandSelectTrack();
        verifyCommandDispatchKeyDown();
        verifyCommandDispatchKeyMultiple();
        verifyCommandDispatchKeyUp();
        verifyCommandDispatchTouchEvent();
        verifyCommandDispatchTrackballEvent();
        verifyCommandDispatchGenericMotionEvent();
        verifyCommandTimeShiftPause();
        verifyCommandTimeShiftResume();
        verifyCommandTimeShiftSeekTo();
        verifyCommandTimeShiftSetPlaybackParams();
        verifyCommandSetTimeShiftPositionCallback();
        verifyCommandOverlayViewSizeChanged();
        verifyCallbackChannelRetuned();
        verifyCallbackVideoAvailable();
        verifyCallbackVideoUnavailable();
        verifyCallbackTracksChanged();
        verifyCallbackTrackSelected();
        verifyCallbackContentAllowed();
        verifyCallbackContentBlocked();
        verifyCallbackTimeShiftStatusChanged();
        verifyCallbackLayoutSurface();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvView.reset();
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    public void verifyCommandTune() {
        Uri fakeChannelUri = TvContract.buildChannelUri(0);
        mTvView.tune(mStubInfo.getId(), fakeChannelUri);
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mTuneCount > 0 && session.mCreateOverlayView > 0;
            }
        }.run();
    }

    public void verifyCommandSetStreamVolume() {
        resetCounts();
        mTvView.setStreamVolume(1.0f);
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mSetStreamVolumeCount > 0;
            }
        }.run();
    }

    public void verifyCommandSetCaptionEnabled() {
        resetCounts();
        mTvView.setCaptionEnabled(true);
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mSetCaptionEnabledCount > 0;
            }
        }.run();
    }

    public void verifyCommandSelectTrack() {
        resetCounts();
        verifyCallbackTracksChanged();
        mTvView.selectTrack(mDummyTrack.getType(), mDummyTrack.getId());
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mSelectTrackCount > 0;
            }
        }.run();
    }

    public void verifyCommandDispatchKeyDown() {
        resetCounts();
        mTvView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_K));
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mKeyDownCount > 0;
            }
        }.run();
    }

    public void verifyCommandDispatchKeyMultiple() {
        resetCounts();
        mTvView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_K));
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mKeyMultipleCount > 0;
            }
        }.run();
    }

    public void verifyCommandDispatchKeyUp() {
        resetCounts();
        mTvView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_K));
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mKeyUpCount > 0;
            }
        }.run();
    }

    public void verifyCommandDispatchTouchEvent() {
        resetCounts();
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 1.0f, 1.0f,
                1.0f, 1.0f, 0, 1.0f, 1.0f, 0, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        mTvView.dispatchTouchEvent(event);
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mTouchEventCount > 0;
            }
        }.run();
    }

    public void verifyCommandDispatchTrackballEvent() {
        resetCounts();
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 1.0f, 1.0f,
                1.0f, 1.0f, 0, 1.0f, 1.0f, 0, 0);
        event.setSource(InputDevice.SOURCE_TRACKBALL);
        mTvView.dispatchTouchEvent(event);
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mTrackballEventCount > 0;
            }
        }.run();
    }

    public void verifyCommandDispatchGenericMotionEvent() {
        resetCounts();
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 1.0f, 1.0f,
                1.0f, 1.0f, 0, 1.0f, 1.0f, 0, 0);
        mTvView.dispatchGenericMotionEvent(event);
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mGenricMotionEventCount > 0;
            }
        }.run();
    }

    public void verifyCommandTimeShiftPause() {
        resetCounts();
        mTvView.timeShiftPause();
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mTimeShiftPauseCount > 0;
            }
        }.run();
    }

    public void verifyCommandTimeShiftResume() {
        resetCounts();
        mTvView.timeShiftResume();
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mTimeShiftResumeCount > 0;
            }
        }.run();
    }

    public void verifyCommandTimeShiftSeekTo() {
        resetCounts();
        mTvView.timeShiftSeekTo(0);
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mTimeShiftSeekToCount > 0;
            }
        }.run();
    }

    public void verifyCommandTimeShiftSetPlaybackParams() {
        resetCounts();
        mTvView.timeShiftSetPlaybackParams(new PlaybackParams().setSpeed(2.0f)
                .setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT));
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mTimeShiftSetPlaybackParamsCount > 0;
            }
        }.run();
    }

    public void verifyCommandSetTimeShiftPositionCallback() {
        resetCounts();
        mTvView.setTimeShiftPositionCallback(mTimeShiftPositionCallback);
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mTimeShiftPositionCallback.mTimeShiftCurrentPositionChanged > 0
                        && mTimeShiftPositionCallback.mTimeShiftStartPositionChanged > 0;
            }
        }.run();
    }

    public void verifyCommandOverlayViewSizeChanged() {
        resetCounts();
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTvView.setLayoutParams(new LinearLayout.LayoutParams(10, 20));
            }
        });
        mInstrumentation.waitForIdleSync();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                CountingSession session = CountingTvInputService.sSession;
                return session != null && session.mOverlayViewSizeChangedCount > 0;
            }
        }.run();
    }

    public void verifyCallbackChannelRetuned() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        Uri fakeChannelUri = TvContract.buildChannelUri(0);
        session.notifyChannelRetuned(fakeChannelUri);
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mChannelRetunedCount > 0;
            }
        }.run();
    }

    public void verifyCallbackVideoAvailable() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        session.notifyVideoAvailable();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mVideoAvailableCount > 0;
            }
        }.run();
    }

    public void verifyCallbackVideoUnavailable() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        session.notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mVideoUnavailableCount > 0;
            }
        }.run();
    }

    public void verifyCallbackTracksChanged() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        ArrayList<TvTrackInfo> tracks = new ArrayList<>();
        tracks.add(mDummyTrack);
        session.notifyTracksChanged(tracks);
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mTrackChangedCount > 0;
            }
        }.run();
    }

    public void verifyCallbackVideoSizeChanged() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        ArrayList<TvTrackInfo> tracks = new ArrayList<>();
        tracks.add(mDummyTrack);
        session.notifyTracksChanged(tracks);
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mVideoSizeChanged > 0;
            }
        }.run();
    }

    public void verifyCallbackTrackSelected() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        session.notifyTrackSelected(mDummyTrack.getType(), mDummyTrack.getId());
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mTrackSelectedCount > 0;
            }
        }.run();
    }

    public void verifyCallbackContentAllowed() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        session.notifyContentAllowed();
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mContentAllowedCount > 0;
            }
        }.run();
    }

    public void verifyCallbackContentBlocked() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        TvContentRating rating = TvContentRating.createRating("android.media.tv", "US_TVPG",
                "US_TVPG_TV_MA", "US_TVPG_S", "US_TVPG_V");
        session.notifyContentBlocked(rating);
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mContentBlockedCount > 0;
            }
        }.run();
    }

    public void verifyCallbackTimeShiftStatusChanged() {
        resetCounts();
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        session.notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mCallback.mTimeShiftStatusChangedCount > 0;
            }
        }.run();
    }

    public void verifyCallbackLayoutSurface() {
        resetCounts();
        final int left = 10;
        final int top = 20;
        final int right = 30;
        final int bottom = 40;
        CountingSession session = CountingTvInputService.sSession;
        assertNotNull(session);
        session.layoutSurface(left, top, right, bottom);
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                int childCount = mTvView.getChildCount();
                for (int i = 0; i < childCount; ++i) {
                    View v = mTvView.getChildAt(i);
                    if (v instanceof SurfaceView) {
                        return v.getLeft() == left && v.getTop() == top && v.getRight() == right
                                && v.getBottom() == bottom;
                    }
                }
                return false;
            }
        }.run();
    }

    private void resetCounts() {
        if (CountingTvInputService.sSession != null) {
            CountingTvInputService.sSession.resetCounts();
        }
        mCallback.resetCounts();
        mTimeShiftPositionCallback.resetCounts();
    }

    public static class CountingTvInputService extends StubTvInputService {
        static CountingSession sSession;

        @Override
        public Session onCreateSession(String inputId) {
            sSession = new CountingSession(this);
            sSession.setOverlayViewEnabled(true);
            return sSession;
        }

        public static class CountingSession extends Session {
            public volatile int mTuneCount;
            public volatile int mSetStreamVolumeCount;
            public volatile int mSetCaptionEnabledCount;
            public volatile int mSelectTrackCount;
            public volatile int mCreateOverlayView;
            public volatile int mKeyDownCount;
            public volatile int mKeyLongPressCount;
            public volatile int mKeyMultipleCount;
            public volatile int mKeyUpCount;
            public volatile int mTouchEventCount;
            public volatile int mTrackballEventCount;
            public volatile int mGenricMotionEventCount;
            public volatile int mOverlayViewSizeChangedCount;
            public volatile int mTimeShiftPauseCount;
            public volatile int mTimeShiftResumeCount;
            public volatile int mTimeShiftSeekToCount;
            public volatile int mTimeShiftSetPlaybackParamsCount;
            public volatile long mTimeShiftGetCurrentPositionCount;
            public volatile long mTimeShiftGetStartPositionCount;

            CountingSession(Context context) {
                super(context);
            }

            public void resetCounts() {
                mTuneCount = 0;
                mSetStreamVolumeCount = 0;
                mSetCaptionEnabledCount = 0;
                mSelectTrackCount = 0;
                mCreateOverlayView = 0;
                mKeyDownCount = 0;
                mKeyLongPressCount = 0;
                mKeyMultipleCount = 0;
                mKeyUpCount = 0;
                mTouchEventCount = 0;
                mTrackballEventCount = 0;
                mGenricMotionEventCount = 0;
                mOverlayViewSizeChangedCount = 0;
                mTimeShiftPauseCount = 0;
                mTimeShiftResumeCount = 0;
                mTimeShiftSeekToCount = 0;
                mTimeShiftSetPlaybackParamsCount = 0;
                mTimeShiftGetCurrentPositionCount = 0;
                mTimeShiftGetStartPositionCount = 0;
            }

            @Override
            public void onRelease() {
            }

            @Override
            public boolean onSetSurface(Surface surface) {
                return false;
            }

            @Override
            public boolean onTune(Uri channelUri) {
                mTuneCount++;
                return false;
            }

            @Override
            public void onSetStreamVolume(float volume) {
                mSetStreamVolumeCount++;
            }

            @Override
            public void onSetCaptionEnabled(boolean enabled) {
                mSetCaptionEnabledCount++;
            }

            @Override
            public boolean onSelectTrack(int type, String id) {
                mSelectTrackCount++;
                return false;
            }

            @Override
            public View onCreateOverlayView() {
                mCreateOverlayView++;
                return null;
            }

            @Override
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                mKeyDownCount++;
                return false;
            }

            @Override
            public boolean onKeyLongPress(int keyCode, KeyEvent event) {
                mKeyLongPressCount++;
                return false;
            }

            @Override
            public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
                mKeyMultipleCount++;
                return false;
            }

            @Override
            public boolean onKeyUp(int keyCode, KeyEvent event) {
                mKeyUpCount++;
                return false;
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                mTouchEventCount++;
                return false;
            }

            @Override
            public boolean onTrackballEvent(MotionEvent event) {
                mTrackballEventCount++;
                return false;
            }

            @Override
            public boolean onGenericMotionEvent(MotionEvent event) {
                mGenricMotionEventCount++;
                return false;
            }

            @Override
            public void onTimeShiftPause() {
                mTimeShiftPauseCount++;
            }

            @Override
            public void onTimeShiftResume() {
                mTimeShiftResumeCount++;
            }

            @Override
            public void onTimeShiftSeekTo(long timeMs) {
                mTimeShiftSeekToCount++;
            }

            @Override
            public void onTimeShiftSetPlaybackParams(PlaybackParams param) {
                mTimeShiftSetPlaybackParamsCount++;
            }

            @Override
            public long onTimeShiftGetCurrentPosition() {
                return ++mTimeShiftGetCurrentPositionCount;
            }

            @Override
            public long onTimeShiftGetStartPosition() {
                return ++mTimeShiftGetStartPositionCount;
            }

            @Override
            public void onOverlayViewSizeChanged(int width, int height) {
                mOverlayViewSizeChangedCount++;
            }
        }
    }
}
