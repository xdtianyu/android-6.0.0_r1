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

import java.util.concurrent.TimeUnit;

/**
 * Test min-max frequency, max range parameters for sensors.
 *
 * <p>To execute these test cases, the following command can be used:</p>
 * <pre>
 * adb shell am instrument -e class android.hardware.cts.SensorParameterRangeTest \
 *     -w com.android.cts.hardware/android.test.AndroidJUnitRunner
 * </pre>
 */
public class SensorParameterRangeTest extends SensorTestCase {

    private static final double ACCELEROMETER_MAX_RANGE = 8 * 9.80; // 8G minus a slop
    private static final double ACCELEROMETER_MIN_FREQUENCY = 12.50;
    private static final int ACCELEROMETER_MAX_FREQUENCY = 200;

    private static final double GYRO_MAX_RANGE = 1000/57.295 - 1.0; // 1000 degrees per sec minus a slop
    private static final double GYRO_MIN_FREQUENCY = 12.50;
    private static final double GYRO_MAX_FREQUENCY = 200.0;

    private static final int MAGNETOMETER_MAX_RANGE = 900;   // micro telsa
    private static final double MAGNETOMETER_MIN_FREQUENCY = 5.0;
    private static final double MAGNETOMETER_MAX_FREQUENCY = 50.0;

    private static final double PRESSURE_MAX_RANGE = 1100.0;     // hecto-pascal
    private static final double PRESSURE_MIN_FREQUENCY = 1.0;
    private static final double PRESSURE_MAX_FREQUENCY = 10.0;

    private boolean mHasHifiSensors;
    private SensorManager mSensorManager;

    @Override
    public void setUp() {
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mHasHifiSensors = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_HIFI_SENSORS);
    }

    public void testAccelerometerRange() {
        checkSensorRangeAndFrequency(
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                ACCELEROMETER_MAX_RANGE,
                ACCELEROMETER_MIN_FREQUENCY,
                ACCELEROMETER_MAX_FREQUENCY);
  }

  public void testGyroscopeRange() {
        checkSensorRangeAndFrequency(
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                GYRO_MAX_RANGE,
                GYRO_MIN_FREQUENCY,
                GYRO_MAX_FREQUENCY);
  }

    public void testMagnetometerRange() {
        checkSensorRangeAndFrequency(
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                MAGNETOMETER_MAX_RANGE,
                MAGNETOMETER_MIN_FREQUENCY,
                MAGNETOMETER_MAX_FREQUENCY);
    }

    public void testPressureRange() {
        checkSensorRangeAndFrequency(
                mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
                PRESSURE_MAX_RANGE,
                PRESSURE_MIN_FREQUENCY,
                PRESSURE_MAX_FREQUENCY);
    }

    private void checkSensorRangeAndFrequency(
          Sensor sensor, double maxRange, double minFrequency, double maxFrequency) {
        if (!mHasHifiSensors) return;
        assertTrue(String.format("%s Range actual=%.2f expected=%.2f %s",
                    sensor.getName(), sensor.getMaximumRange(), maxRange,
                    SensorCtsHelper.getUnitsForSensor(sensor)),
                sensor.getMaximumRange() >= maxRange);
        double actualMinFrequency = SensorCtsHelper.getFrequency(sensor.getMaxDelay(),
                TimeUnit.MICROSECONDS);
        assertTrue(String.format("%s Min Frequency actual=%.2f expected=%dHz",
                    sensor.getName(), actualMinFrequency, minFrequency), actualMinFrequency <=
                minFrequency + 0.1);

        double actualMaxFrequency = SensorCtsHelper.getFrequency(sensor.getMinDelay(),
                TimeUnit.MICROSECONDS);
        assertTrue(String.format("%s Max Frequency actual=%.2f expected=%dHz",
                    sensor.getName(), actualMaxFrequency, maxFrequency), actualMaxFrequency >=
                maxFrequency - 0.1);
    }
}
