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

package android.hardware.cts;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.TestSensorEnvironment;
import android.hardware.cts.helpers.sensoroperations.TestSensorOperation;
import android.hardware.cts.helpers.sensorverification.FifoLengthVerification;

/**
 * Checks the minimum Hardware FIFO length for each of the Hardware sensor.
 * Further verifies if the advertised FIFO (Sensor.getFifoMaxEventCount()) is actually allocated
 * for the sensor.
 *
 */
public class SensorBatchingFifoTest extends SensorTestCase {
    private static final int ACCELEROMETER_MIN_FIFO_LENGTH = 3000;
    private static final int UNCAL_MAGNETOMETER_MIN_FIFO_LENGTH = 600;
    private static final int PRESSURE_MIN_FIFO_LENGTH = 300;
    private static final int GAME_ROTATION_VECTOR_MIN_FIFO_LENGTH = 300;
    private static final int PROXIMITY_SENSOR_MIN_FIFO_LENGTH = 300;
    private static final int STEP_DETECTOR_MIN_FIFO_LENGTH = 100;

    private static final int SAMPLING_INTERVAL = 1000; /* every 1ms */
    private static final String TAG = "batching_fifo_test";

    private SensorManager mSensorManager;
    private boolean mHasHifiSensors;
    @Override
    protected void setUp() throws Exception {
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mHasHifiSensors = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_HIFI_SENSORS);
    }

    public void testAccelerometerFifoLength() throws Throwable {
        if (!mHasHifiSensors) return;
        runBatchingSensorFifoTest(
                Sensor.TYPE_ACCELEROMETER,
                checkMinFifoLength(Sensor.TYPE_ACCELEROMETER, ACCELEROMETER_MIN_FIFO_LENGTH));
    }

    public void testUncalMagnetometerFifoLength() throws Throwable {
        if (!mHasHifiSensors) return;
        runBatchingSensorFifoTest(
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
                checkMinFifoLength(
                        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
                        UNCAL_MAGNETOMETER_MIN_FIFO_LENGTH));
    }

    public void testPressureFifoLength() throws Throwable {
        if (!mHasHifiSensors) return;
        runBatchingSensorFifoTest(
                Sensor.TYPE_PRESSURE,
                checkMinFifoLength(Sensor.TYPE_PRESSURE, PRESSURE_MIN_FIFO_LENGTH));
    }

    public void testGameRotationVectorFifoLength() throws Throwable {
        if (!mHasHifiSensors) return;
        runBatchingSensorFifoTest(
                Sensor.TYPE_GAME_ROTATION_VECTOR,
                checkMinFifoLength(
                        Sensor.TYPE_GAME_ROTATION_VECTOR, GAME_ROTATION_VECTOR_MIN_FIFO_LENGTH));
    }

    public void testProximityFifoLength() throws Throwable {
        if (!mHasHifiSensors) return;
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (sensor != null) {
            assertTrue(sensor.getFifoReservedEventCount() <= PROXIMITY_SENSOR_MIN_FIFO_LENGTH);
        }
    }

    public void testStepDetectorFifoLength() throws Throwable {
        if (!mHasHifiSensors) return;
        checkMinFifoLength(Sensor.TYPE_STEP_DETECTOR, STEP_DETECTOR_MIN_FIFO_LENGTH);
    }

    private int checkMinFifoLength(int sensorType, int minRequiredLength) {
        Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
        assertTrue(String.format("sensor of type=%d (null)", sensorType), sensor != null);
        int maxFifoLength = sensor.getFifoReservedEventCount();
        assertTrue(String.format("Sensor=%s, min required fifo length=%d actual=%d",
                    sensor.getName(), minRequiredLength, maxFifoLength),
                    maxFifoLength >= minRequiredLength);
        return maxFifoLength;
    }

    private void runBatchingSensorFifoTest(int sensorType, int fifoLength) throws Throwable {
        if (fifoLength == 0) {
            return;
        }
        Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
        TestSensorEnvironment environment =  new TestSensorEnvironment(getContext(),
                sensor,
                false,
                sensor.getMinDelay(),
                Integer.MAX_VALUE);

        TestSensorOperation op = TestSensorOperation.createOperation(environment,
                sensor.getFifoReservedEventCount() * 2);
        op.addVerification(FifoLengthVerification.getDefault(environment));
        op.execute(getCurrentTestNode());
        op.getStats().log(TAG);
    }
}
