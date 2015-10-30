package com.android.cts.verifier.sensors;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.android.cts.verifier.R;
import com.android.cts.verifier.sensors.base.SensorCtsVerifierTestActivity;
import com.android.cts.verifier.sensors.helpers.SensorTestScreenManipulator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.hardware.cts.helpers.MovementDetectorHelper;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.SensorTestStateNotSupportedException;
import android.hardware.cts.helpers.TestSensorEnvironment;
import android.hardware.cts.helpers.TestSensorEvent;
import android.hardware.cts.helpers.TestSensorEventListener;
import android.hardware.cts.helpers.TestSensorManager;
import android.hardware.cts.helpers.sensoroperations.TestSensorOperation;
import android.hardware.cts.helpers.SensorNotSupportedException;
import android.hardware.cts.helpers.sensorverification.BatchArrivalVerification;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import junit.framework.Assert;

public class DeviceSuspendTestActivity
            extends SensorCtsVerifierTestActivity {
        public DeviceSuspendTestActivity() {
            super(DeviceSuspendTestActivity.class);
        }

        private SensorTestScreenManipulator mScreenManipulator;
        private PowerManager.WakeLock mDeviceSuspendLock;
        private PendingIntent mPendingIntent;
        private AlarmManager mAlarmManager;
        private static String ACTION_ALARM = "DeviceSuspendTestActivity.ACTION_ALARM";
        private static String TAG = "DeviceSuspendSensorTest";
        private SensorManager mSensorManager;

        @Override
        protected void activitySetUp() throws InterruptedException {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mScreenManipulator = new SensorTestScreenManipulator(this);
            mScreenManipulator.initialize(this);
            LocalBroadcastManager.getInstance(this).registerReceiver(myBroadCastReceiver,
                                            new IntentFilter(ACTION_ALARM));

            Intent intent = new Intent(this, AlarmReceiver.class);
            mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

            mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mDeviceSuspendLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                "DeviceSuspendTestActivity");
            mDeviceSuspendLock.acquire();
            SensorTestLogger logger = getTestLogger();
            logger.logInstructions(R.string.snsr_device_suspend_test_instr);
            waitForUserToBegin();
        }

        @Override
        protected void activityCleanUp() {
            mScreenManipulator.turnScreenOn();
            try {
                playSound();
            } catch(InterruptedException e) {
              // Ignore.
            }
            LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadCastReceiver);
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (mScreenManipulator != null) {
                mScreenManipulator.releaseScreenOn();
                mScreenManipulator.close();
            }
            if (mDeviceSuspendLock.isHeld()) {
                mDeviceSuspendLock.release();
            }
        }

        public static class AlarmReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent alarm_intent = new Intent(context, DeviceSuspendTestActivity.class);
                alarm_intent.setAction(DeviceSuspendTestActivity.ACTION_ALARM);
                LocalBroadcastManager.getInstance(context).sendBroadcastSync(alarm_intent);
            }
        }

        public BroadcastReceiver myBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!mDeviceSuspendLock.isHeld()) {
                    mDeviceSuspendLock.acquire();
                }
            }
        };

        public String testAPWakeUpWhenReportLatencyExpiresAccel() throws Throwable {
            Sensor wakeUpSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true);
            if (wakeUpSensor == null) {
                throw new SensorNotSupportedException(Sensor.TYPE_ACCELEROMETER, true);
            }
            return runAPWakeUpWhenReportLatencyExpires(wakeUpSensor);
        }

        public String testAPWakeUpWhenReportLatencyExpiresGyro() throws Throwable {
            Sensor wakeUpSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE, true);
            if (wakeUpSensor == null) {
                throw new SensorNotSupportedException(Sensor.TYPE_GYROSCOPE, true);
            }
            return runAPWakeUpWhenReportLatencyExpires(wakeUpSensor);
        }

        public String testAPWakeUpWhenReportLatencyExpiresMag() throws Throwable {
            Sensor wakeUpSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD,true);
            if (wakeUpSensor == null) {
                throw new SensorNotSupportedException(Sensor.TYPE_MAGNETIC_FIELD, true);
            }
            return runAPWakeUpWhenFIFOFull(wakeUpSensor);
        }

        public String testAPWakeUpWhenFIFOFullAccel() throws Throwable {
            Sensor wakeUpSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true);
            if (wakeUpSensor == null) {
                throw new SensorNotSupportedException(Sensor.TYPE_ACCELEROMETER, true);
            }
            return runAPWakeUpWhenFIFOFull(wakeUpSensor);
        }

        public String testAPWakeUpWhenFIFOFullGyro() throws Throwable {
            Sensor wakeUpSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE, true);
            if (wakeUpSensor == null) {
                throw new SensorNotSupportedException(Sensor.TYPE_GYROSCOPE, true);
            }
            return runAPWakeUpWhenFIFOFull(wakeUpSensor);
        }

        public String testAPWakeUpWhenFIFOFullMag() throws Throwable {
            Sensor wakeUpSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD,true);
            if (wakeUpSensor == null) {
                throw new SensorNotSupportedException(Sensor.TYPE_MAGNETIC_FIELD, true);
            }
            return runAPWakeUpWhenFIFOFull(wakeUpSensor);
        }

        public String testAccelBatchingInAPSuspendLargeReportLatency() throws Throwable {
            Sensor accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accel == null) {
                throw new SensorNotSupportedException(Sensor.TYPE_ACCELEROMETER, false);
            }
            return runAPWakeUpByAlarmNonWakeSensor(accel, (int)TimeUnit.SECONDS.toMicros(1000));
        }

        public String testAccelBatchingInAPSuspendZeroReportLatency() throws Throwable {
            Sensor accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
           if (accel == null) {
                throw new SensorNotSupportedException(Sensor.TYPE_ACCELEROMETER, false);
            }
            return runAPWakeUpByAlarmNonWakeSensor(accel, 0);
        }

        public String runAPWakeUpWhenReportLatencyExpires(Sensor sensor) throws Throwable {
            int fifoMaxEventCount = sensor.getFifoMaxEventCount();
            if (fifoMaxEventCount == 0) {
                throw new SensorTestStateNotSupportedException("Batching not supported.");
            }
            int maximumExpectedSamplingPeriodUs = sensor.getMaxDelay();
            if (maximumExpectedSamplingPeriodUs == 0) {
                // If maxDelay is not defined, set the value for 5 Hz.
                maximumExpectedSamplingPeriodUs = 200000;
            }
            int fifoBasedReportLatencyUs = fifoMaxEventCount * maximumExpectedSamplingPeriodUs;

            // Ensure that FIFO based report latency is at least 20 seconds, we need at least 10
            // seconds of time to allow the device to be in suspend state.
            if (fifoBasedReportLatencyUs < 20000000L) {
                throw new SensorTestStateNotSupportedException("FIFO too small to test reliably");
            }

            final int MAX_REPORT_LATENCY_US = 15000000; // 15 seconds
            TestSensorEnvironment environment = new TestSensorEnvironment(
                    this,
                    sensor,
                    false,
                    maximumExpectedSamplingPeriodUs,
                    MAX_REPORT_LATENCY_US,
                    true /*isDeviceSuspendTest*/);

            TestSensorOperation op = TestSensorOperation.createOperation(environment,
                                                                          mDeviceSuspendLock,
                                                                          false);
            final int ALARM_WAKE_UP_DELAY_MS = MAX_REPORT_LATENCY_US/1000 +
                (int)TimeUnit.SECONDS.toMillis(10);
            op.addVerification(BatchArrivalVerification.getDefault(environment));
            mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                    SystemClock.elapsedRealtime() + ALARM_WAKE_UP_DELAY_MS,
                                    mPendingIntent);
            try {
                Log.i(TAG, "Running .. " + getCurrentTestNode().getName() + " " + sensor.getName());
                op.execute(getCurrentTestNode());
            } finally {
                mAlarmManager.cancel(mPendingIntent);
            }
            return null;
        }

        public String runAPWakeUpWhenFIFOFull(Sensor sensor) throws Throwable {
            int fifoMaxEventCount = sensor.getFifoMaxEventCount();
            if (fifoMaxEventCount == 0) {
                throw new SensorTestStateNotSupportedException("Batching not supported.");
            }
            // Try to fill the FIFO at the fastest rate and check if the time is enough to run
            // the manual test.
            int maximumExpectedSamplingPeriodUs = sensor.getMinDelay();
            int fifoBasedReportLatencyUs = fifoMaxEventCount * maximumExpectedSamplingPeriodUs;

            final int MIN_LATENCY_US = (int)TimeUnit.SECONDS.toMicros(20);
            // Ensure that FIFO based report latency is at least 20 seconds, we need at least 10
            // seconds of time to allow the device to be in suspend state.
            if (fifoBasedReportLatencyUs < MIN_LATENCY_US) {
                maximumExpectedSamplingPeriodUs = MIN_LATENCY_US/fifoMaxEventCount;
                fifoBasedReportLatencyUs = MIN_LATENCY_US;
            }

            final int MAX_REPORT_LATENCY_US = Integer.MAX_VALUE;
            final int ALARM_WAKE_UP_DELAY_MS = fifoBasedReportLatencyUs/1000 +
                (int)TimeUnit.SECONDS.toMillis(10);
            TestSensorEnvironment environment = new TestSensorEnvironment(
                    this,
                    sensor,
                    false,
                    maximumExpectedSamplingPeriodUs,
                    MAX_REPORT_LATENCY_US,
                    true /*isDeviceSuspendTest*/);

           TestSensorOperation op = TestSensorOperation.createOperation(environment,
                                                                        mDeviceSuspendLock,
                                                                        true);
            mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                    SystemClock.elapsedRealtime() + ALARM_WAKE_UP_DELAY_MS,
                                    mPendingIntent);
            op.addDefaultVerifications();
            try {
                Log.i(TAG, "Running .. " + getCurrentTestNode().getName() + " " + sensor.getName());
                op.execute(getCurrentTestNode());
            } finally {
                mAlarmManager.cancel(mPendingIntent);
            }
            return null;
        }

        public String runAPWakeUpByAlarmNonWakeSensor(Sensor sensor, int maxReportLatencyUs)
            throws  Throwable {
            int fifoMaxEventCount = sensor.getFifoMaxEventCount();
            if (fifoMaxEventCount == 0) {
                throw new SensorTestStateNotSupportedException("Batching not supported.");
            }
            int maximumExpectedSamplingPeriodUs = sensor.getMaxDelay();
            if (maximumExpectedSamplingPeriodUs == 0 ||
                    maximumExpectedSamplingPeriodUs > 200000) {
                // If maxDelay is not defined, set the value for 5 Hz.
                maximumExpectedSamplingPeriodUs = 200000;
            }
            int fifoBasedReportLatencyUs = fifoMaxEventCount * maximumExpectedSamplingPeriodUs;

            // Ensure that FIFO based report latency is at least 20 seconds, we need at least 10
            // seconds of time to allow the device to be in suspend state.
            if (fifoBasedReportLatencyUs < 20000000L) {
                throw new SensorTestStateNotSupportedException("FIFO too small to test reliably");
            }

            TestSensorEnvironment environment = new TestSensorEnvironment(
                    this,
                    sensor,
                    false,
                    maximumExpectedSamplingPeriodUs,
                    maxReportLatencyUs,
                    true /*isDeviceSuspendTest*/);

            final int ALARM_WAKE_UP_DELAY_MS = 20000;
            TestSensorOperation op = TestSensorOperation.createOperation(environment,
                                                                         mDeviceSuspendLock,
                                                                         true);
            mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                    SystemClock.elapsedRealtime() + ALARM_WAKE_UP_DELAY_MS,
                                    mPendingIntent);
            try {
                Log.i(TAG, "Running .. " + getCurrentTestNode().getName() + " " + sensor.getName());
                op.execute(getCurrentTestNode());
            } finally {
                mAlarmManager.cancel(mPendingIntent);
            }
            return null;
        }
}
