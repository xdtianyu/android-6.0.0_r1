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

package com.android.cts.verifier.jobscheduler;

import com.android.cts.verifier.R;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

/**
 *  Idle constraints:
 *      The framework doesn't support turning idle mode off. Use the manual tester to ensure that
 *      the device is not in idle mode (by turning the screen off and then back on) before running
 *      the tests.
 */
@TargetApi(21)
public class IdleConstraintTestActivity extends ConstraintTestActivity {
    private static final String TAG = "IdleModeTestActivity";
    /**
     * It takes >1hr for idle mode to be triggered. We'll use this secret broadcast to force the
     * scheduler into idle. It's not a protected broadcast so that's alright.
     */
    private static final String ACTION_EXPEDITE_IDLE_MODE =
            "com.android.server.task.controllers.IdleController.ACTION_TRIGGER_IDLE";

    /**
     * Id for the job that we schedule when the device is not in idle mode. This job is expected
     * to not execute. Executing means that the verifier test should fail.
     */
    private static final int IDLE_OFF_JOB_ID = IdleConstraintTestActivity.class.hashCode() + 0;
    /**
     * Id for the job that we schedule when the device *is* in idle mode. This job is expected to
     * execute. Not executing means that the verifier test should fail.
     */
    private static final int IDLE_ON_JOB_ID = IdleConstraintTestActivity.class.hashCode() + 1;

    /**
     * Listens for idle mode off/on events, namely {@link #ACTION_EXPEDITE_IDLE_MODE} and
     * {@link Intent#ACTION_SCREEN_ON}.
     * On ACTION_EXPEDITE_IDLE_MODE, we will disable the {@link #mStartButton}, and on
     * ACTION_SCREEN_ON we enable it. This is to avoid the start button being clicked when the
     * device is in idle mode.
     */
    private BroadcastReceiver mIdleChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mStartButton.setEnabled(true);
            } else if (ACTION_EXPEDITE_IDLE_MODE.equals(intent.getAction())) {
                mStartButton.setEnabled(false);
            } else {
                Log.e(TAG, "Invalid broadcast received, was expecting SCREEN_ON");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI.
        setContentView(R.layout.js_idle);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.js_idle_test, R.string.js_idle_instructions, -1);
        mStartButton = (Button) findViewById(R.id.js_idle_start_test_button);

        // Register receiver for idle off/on events.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(ACTION_EXPEDITE_IDLE_MODE);

        registerReceiver(mIdleChangedReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIdleChangedReceiver);
    }

    @Override
    protected void startTestImpl() {
        new TestIdleModeTask().execute();
    }

    /** Background task that will run the actual test. */
    private class TestIdleModeTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            testIdleConstraintFails_notIdle();


            // Send the {@link #ACTION_EXPEDITE_IDLE_MODE} broadcast as an ordered broadcast, this
            // function will block until all receivers have processed the broadcast.
            if (!sendBroadcastAndBlockForResult(new Intent(ACTION_EXPEDITE_IDLE_MODE))) {
                // Fail the test if the broadcast wasn't processed.
                runOnUiThread(new IdleTestResultRunner(IDLE_ON_JOB_ID, false));
            }

            testIdleConstraintExecutes_onIdle();

            notifyTestCompleted();
            return null;
        }

    }

    /**
     * The user has just pressed the "Start Test" button, so we know that the device can't be idle.
     * Schedule a job with an idle constraint and verify that it doesn't execute.
     */
    private void testIdleConstraintFails_notIdle() {
        mTestEnvironment.setUp();
        mJobScheduler.cancelAll();

        mTestEnvironment.setExpectedExecutions(0);

        mJobScheduler.schedule(
                new JobInfo.Builder(IDLE_OFF_JOB_ID, mMockComponent)
                        .setRequiresDeviceIdle(true)
                        .build());

        boolean testPassed;
        try {
            testPassed = mTestEnvironment.awaitTimeout();
        } catch (InterruptedException e) {
            // We'll just indicate that it failed, not why.
            testPassed = false;
        }
        runOnUiThread(new IdleTestResultRunner(IDLE_OFF_JOB_ID, testPassed));
    }

    private void testIdleConstraintExecutes_onIdle() {
        mTestEnvironment.setUp();
        mJobScheduler.cancelAll();

        mTestEnvironment.setExpectedExecutions(1);

        mJobScheduler.schedule(
                new JobInfo.Builder(IDLE_ON_JOB_ID, mMockComponent)
                .setRequiresDeviceIdle(true)
                .build());

        boolean testPassed;
        try {
            testPassed = mTestEnvironment.awaitExecution();
        } catch (InterruptedException e) {
            // We'll just indicate that it failed, not why.
            testPassed = false;
        }
        runOnUiThread(new IdleTestResultRunner(IDLE_ON_JOB_ID, testPassed));
    }

    /**
     * Runnable to update the UI with the outcome of the test. This class only runs two tests, so
     * the argument passed into the constructor will indicate which of the tests we are reporting
     * for.
     */
    protected class IdleTestResultRunner extends TestResultRunner {

        IdleTestResultRunner(int jobId, boolean testPassed) {
            super(jobId, testPassed);
        }

        @Override
        public void run() {
            ImageView view;
            if (mJobId == IDLE_OFF_JOB_ID) {
                view = (ImageView) findViewById(R.id.idle_off_test_image);
            } else if (mJobId == IDLE_ON_JOB_ID) {
                view = (ImageView) findViewById(R.id.idle_on_test_image);
            } else {
                noteInvalidTest();
                return;
            }
            view.setImageResource(mTestPassed ? R.drawable.fs_good : R.drawable.fs_error);
        }
    }
}
