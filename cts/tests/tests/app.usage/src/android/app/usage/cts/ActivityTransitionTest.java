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
package android.app.usage.cts;

import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.app.usage.cts.R;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import java.util.List;

public class ActivityTransitionTest extends
        ActivityInstrumentationTestCase2<ActivityTransitionActivity> {
    protected ActivityTransitionActivity mActivity;

    private int mNumArrivedCalls;
    private PassInfo mReceiver;
    private long mExitTime;
    private long mExitTimeReady;
    private long mReenterTime;
    private long mReenterTimeReady;

    public ActivityTransitionTest() {
        super(ActivityTransitionActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
        mNumArrivedCalls = 0;
    }

    public void testOnSharedElementsArrived() throws Throwable {
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mReceiver = new PassInfo(new Handler());
                mActivity.setExitSharedElementCallback(new SharedElementCallback() {
                    @Override
                    public void onSharedElementsArrived(List<String> sharedElementNames,
                            List<View> sharedElements,
                            final OnSharedElementsReadyListener listener) {
                        mNumArrivedCalls++;
                        final boolean isExiting = mExitTimeReady == 0;
                        if (isExiting) {
                            mExitTime = SystemClock.uptimeMillis();
                        } else {
                            mReenterTime = SystemClock.uptimeMillis();
                        }
                        mActivity.getWindow().getDecorView().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (isExiting) {
                                    mExitTimeReady = SystemClock.uptimeMillis();
                                } else {
                                    mReenterTimeReady = SystemClock.uptimeMillis();
                                }
                                listener.onSharedElementsReady();
                            }
                        }, 60);
                    }
                });

                Bundle options = ActivityOptions.makeSceneTransitionAnimation(mActivity,
                        mActivity.findViewById(R.id.hello), "target").toBundle();
                Intent intent = new Intent(mActivity, ActivityTransitionActivity.class);
                intent.putExtra(ActivityTransitionActivity.TEST,
                        ActivityTransitionActivity.TEST_ARRIVE);
                intent.putExtra(ActivityTransitionActivity.LAYOUT_ID, R.layout.end);
                intent.putExtra(ActivityTransitionActivity.RESULT_RECEIVER, mReceiver);
                mActivity.startActivityForResult(intent, 0, options);
            }
        });

        long endTime = SystemClock.uptimeMillis() + 1500;
        synchronized (mActivity) {
            while (mActivity.resultCode == 0) {
                long waitTime = endTime - SystemClock.uptimeMillis();
                if (waitTime <= 0) {
                    fail("Activity didn't finish!");
                }
                mActivity.wait(waitTime);
            }
        }
        assertNotNull(mReceiver.resultData);
        assertEquals(2, mReceiver.resultData.getInt(
                ActivityTransitionActivity.ARRIVE_COUNT, -1));
        assertEquals(2, mNumArrivedCalls);
        assertNotSame(View.VISIBLE, mReceiver.resultData.getInt(
                ActivityTransitionActivity.ARRIVE_ENTER_START_VISIBILITY));
        assertNotSame(View.VISIBLE, mReceiver.resultData.getInt(
                ActivityTransitionActivity.ARRIVE_ENTER_DELAY_VISIBILITY));
        long enterTimeReady = mReceiver.resultData.getLong(
                ActivityTransitionActivity.ARRIVE_ENTER_TIME_READY);
        long returnTimeReady = mReceiver.resultData.getLong(
                ActivityTransitionActivity.ARRIVE_RETURN_TIME_READY);
        long enterTime = mReceiver.resultData.getLong(
                ActivityTransitionActivity.ARRIVE_ENTER_TIME);
        long returnTime = mReceiver.resultData.getLong(
                ActivityTransitionActivity.ARRIVE_RETURN_TIME);

        assertTrue(mExitTime < mExitTimeReady);
        assertTrue(mExitTimeReady <= enterTime);
        assertTrue(enterTime < enterTimeReady);
        assertTrue(enterTimeReady <= returnTime);
        assertTrue(returnTime < returnTimeReady);
        assertTrue(returnTimeReady <= mReenterTime);
        assertTrue(mReenterTime < mReenterTimeReady);
    }

    public static class PassInfo extends ResultReceiver {
        public int resultCode;
        public Bundle resultData;

        public PassInfo(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            this.resultCode = resultCode;
            this.resultData = resultData;
        }
    }
}
