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

package android.app.usage.cts;

import android.app.Activity;
import android.app.SharedElementCallback;
import android.app.usage.cts.R;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.transition.ChangeBounds;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.Transition.TransitionListener;
import android.view.View;

import java.util.List;

/**
 * A simple activity containing the start state for an Activity Transition
 */
public class ActivityTransitionActivity extends Activity {
    private static final long DURATION = 50;
    private static final long SHARED_ELEMENT_READY_DELAY = 50;
    public static final String LAYOUT_ID = "layoutId";
    public static final String TEST = "test";
    public static final String RESULT_RECEIVER = "resultReceiver";

    public static final int NO_TEST = 0;
    public static final int TEST_ARRIVE = 1;

    public static final String ARRIVE_COUNT = "numArrived";
    public static final String ARRIVE_ENTER_START_VISIBILITY = "arriveEnterStartVisibility";
    public static final String ARRIVE_ENTER_DELAY_VISIBILITY = "arriveEnterDelayVisibility";
    public static final String ARRIVE_ENTER_TIME_READY = "arriveEnterTimeReady";
    public static final String ARRIVE_ENTER_TIME = "arriveEnterTime";
    public static final String ARRIVE_RETURN_TIME_READY = "arriveReturnTimeReady";
    public static final String ARRIVE_RETURN_TIME = "arriveReturnTime";

    private int mLayoutId;
    private int mTest;
    private ResultReceiver mResultReceiver;
    private int mNumSharedElementsArrivedCalled = 0;
    private boolean mEntering = true;

    public int resultCode = 0;
    public Bundle result = new Bundle();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().setSharedElementEnterTransition(new ChangeBounds().setDuration(DURATION));
        getWindow().setSharedElementReturnTransition(new ChangeBounds().setDuration(DURATION));
        getWindow().setEnterTransition(new Explode().setDuration(DURATION));
        getWindow().setReturnTransition(new Explode().setDuration(DURATION));
        getWindow().setExitTransition(new Fade().setDuration(DURATION));
        mLayoutId = 0;
        if (icicle != null) {
            mLayoutId =  icicle.getInt(LAYOUT_ID);
            mTest = icicle.getInt(TEST);
            mResultReceiver = icicle.getParcelable(RESULT_RECEIVER);
        }

        if (mLayoutId == 0) {
            Intent intent = getIntent();
            mLayoutId = intent.getIntExtra(LAYOUT_ID, R.layout.start);
            mTest = intent.getIntExtra(TEST, 0);
            mResultReceiver = intent.getParcelableExtra(RESULT_RECEIVER);
        }

        setContentView(mLayoutId);

        startTest();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(LAYOUT_ID, mLayoutId);
        outState.putInt(TEST, mTest);
        outState.putParcelable(RESULT_RECEIVER, mResultReceiver);
    }

    private void startTest() {
        if (mTest == TEST_ARRIVE) {
            setEnterSharedElementCallback(new SharedElementCallback() {
                @Override
                public void onSharedElementsArrived(List<String> sharedElementNames,
                        final List<View> sharedElements,
                        final OnSharedElementsReadyListener listener) {
                    mNumSharedElementsArrivedCalled++;
                    result.putInt(ARRIVE_COUNT, mNumSharedElementsArrivedCalled);
                    if (mEntering) {
                        result.putInt(ARRIVE_ENTER_START_VISIBILITY, sharedElements.get(0).getVisibility());
                        result.putLong(ARRIVE_ENTER_TIME, SystemClock.uptimeMillis());
                    } else {
                        result.putLong(ARRIVE_RETURN_TIME, SystemClock.uptimeMillis());
                    }

                    getWindow().getDecorView().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mEntering) {
                                result.putInt(ARRIVE_ENTER_DELAY_VISIBILITY,
                                        sharedElements.get(0).getVisibility());
                                result.putLong(ARRIVE_ENTER_TIME_READY, SystemClock.uptimeMillis());
                            } else {
                                result.putLong(ARRIVE_RETURN_TIME_READY,
                                        SystemClock.uptimeMillis());
                                mResultReceiver.send(RESULT_OK, result);
                            }
                            listener.onSharedElementsReady();
                        }
                    }, SHARED_ELEMENT_READY_DELAY);
                }
            });
            getWindow().getEnterTransition().addListener(new TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    mEntering = false;
                    setResult(RESULT_OK);
                    getWindow().getDecorView().post(new Runnable() {
                        @Override
                        public void run() {
                            finishAfterTransition();
                        }
                    });
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                }

                @Override
                public void onTransitionPause(Transition transition) {
                }

                @Override
                public void onTransitionResume(Transition transition) {
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        synchronized (this) {
            super.onActivityResult(requestCode, resultCode, data);
            this.resultCode = resultCode;
            this.notifyAll();
        }
    }
}
