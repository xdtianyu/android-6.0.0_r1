package com.android.cts.verifier.jobscheduler;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import com.android.cts.verifier.R;

/**
 *  This activity runs the following tests:
 *     - Ask the tester to unplug the phone, and verify that jobs with charging constraints will
 *      not run.
 *     - Ask the tester to ensure the phone is plugged in, and verify that jobs with charging
 *      constraints are run.
 */
@TargetApi(21)
public class ChargingConstraintTestActivity extends ConstraintTestActivity {

    private static final int ON_CHARGING_JOB_ID =
            ChargingConstraintTestActivity.class.hashCode() + 0;
    private static final int OFF_CHARGING_JOB_ID =
            ChargingConstraintTestActivity.class.hashCode() + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI.
        setContentView(R.layout.js_charging);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.js_charging_test, R.string.js_charging_instructions, -1);
        mStartButton = (Button) findViewById(R.id.js_charging_start_test_button);

        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        // Register receiver for connected/disconnected power events.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        registerReceiver(mChargingChangedReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mChargingChangedReceiver);
    }

    @Override
    public void startTestImpl() {
        new TestDeviceUnpluggedConstraint().execute();
    }

    private BroadcastReceiver mChargingChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
                mDeviceUnpluggedTestPassed = false;
                mStartButton.setEnabled(true);
            } else if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                mStartButton.setEnabled(false);
                if (mDeviceUnpluggedTestPassed) {
                    continueTest();
                }
            }
        }
    };

    /** Simple state boolean we use to determine whether to continue with the second test. */
    private boolean mDeviceUnpluggedTestPassed = false;

    /**
     * After the first test has passed, and preconditions are met, this will kick off the second
     * test.
     * See {@link #startTest(android.view.View)}.
     */
    private void continueTest() {
        new TestDevicePluggedInConstraint().execute();
    }

    /**
     * Test blocks and can't be run on the main thread.
     */
    private void testChargingConstraintFails_notCharging() {
        mTestEnvironment.setUp();

        mTestEnvironment.setExpectedExecutions(0);
        JobInfo runOnCharge = new JobInfo.Builder(OFF_CHARGING_JOB_ID, mMockComponent)
                .setRequiresCharging(true)
                .build();
        mJobScheduler.schedule(runOnCharge);

        // Send intent to kick off any jobs. This will be a no-op as the device is not plugged in;
        // the JobScheduler tracks charging state independently.
        sendBroadcastAndBlockForResult(EXPEDITE_STABLE_CHARGING);

        boolean testPassed;
        try {
            testPassed = mTestEnvironment.awaitTimeout();
        } catch (InterruptedException e) {
            testPassed = false;
        }
        mDeviceUnpluggedTestPassed = testPassed;
        runOnUiThread(new ChargingConstraintTestResultRunner(OFF_CHARGING_JOB_ID, testPassed));
    }

    /**
     * Test blocks and can't be run on the main thread.
     */
    private void testChargingConstraintExecutes_onCharging() {
        mTestEnvironment.setUp();

        JobInfo delayConstraintAndUnexpiredDeadline =
                new JobInfo.Builder(ON_CHARGING_JOB_ID, mMockComponent)
                        .setRequiresCharging(true)
                        .build();

        mTestEnvironment.setExpectedExecutions(1);
        mJobScheduler.schedule(delayConstraintAndUnexpiredDeadline);

        // Force the JobScheduler to consider any jobs that have charging constraints.
        sendBroadcast(EXPEDITE_STABLE_CHARGING);

        boolean testPassed;
        try {
            testPassed = mTestEnvironment.awaitExecution();
        } catch (InterruptedException e) {
            testPassed = false;
        }
        runOnUiThread(new ChargingConstraintTestResultRunner(ON_CHARGING_JOB_ID, testPassed));
    }

    /** Run test for when the <bold>device is not connected to power.</bold>. */
    private class TestDeviceUnpluggedConstraint extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            testChargingConstraintFails_notCharging();

            // Do not call notifyTestCompleted here, as we're still waiting for the user to put
            // the device back on charge to continue with TestDevicePluggedInConstraint.
            return null;
        }
    }

    /** Run test for when the <bold>device is connected to power.</bold> */
    private class TestDevicePluggedInConstraint extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            testChargingConstraintExecutes_onCharging();

            notifyTestCompleted();
            return null;
        }
    }

    private class ChargingConstraintTestResultRunner extends TestResultRunner {
        ChargingConstraintTestResultRunner(int jobId, boolean testPassed) {
            super(jobId, testPassed);
        }

        @Override
        public void run() {
            ImageView view;
            if (mJobId == OFF_CHARGING_JOB_ID) {
                view = (ImageView) findViewById(R.id.charging_off_test_image);
            } else if (mJobId == ON_CHARGING_JOB_ID) {
                view = (ImageView) findViewById(R.id.charging_on_test_image);
            } else {
                noteInvalidTest();
                return;
            }
            view.setImageResource(mTestPassed ? R.drawable.fs_good : R.drawable.fs_error);
        }
    }
}
