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
import android.test.AndroidTestCase;

/**
 * Checks if Hifi sensors are supported. When supported, checks individual support for
 * Accelerometer, Gyroscope, Gyroscope_uncal, GeoMagneticField, MagneticField_uncal
 * Pressure, RotationVector, SignificantMotion, StepDetector, StepCounter, TiltDetector.
 *
 * <p>To execute these test cases, the following command can be used:</p>
 * <pre>
 * adb shell am instrument -e class android.hardware.cts.SensorSupportTest \
 *     -w com.android.cts.hardware/android.test.AndroidJUnitRunner
 * </pre>
 */
public class SensorSupportTest extends AndroidTestCase {
    private SensorManager mSensorManager;
    private boolean mAreHifiSensorsSupported;

    @Override
    public void setUp() {
        // Tests will only run if HIFI_SENSORS are supported.
        mAreHifiSensorsSupported = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_HIFI_SENSORS);
        if (mAreHifiSensorsSupported) {
            mSensorManager =
                    (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        }
    }

    public void testSupportsAccelerometer() {
        checkSupportsSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void testSupportsGyroscope() {
        checkSupportsSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void testSupportsGyroscopeUncalibrated() {
        checkSupportsSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
    }

    public void testSupportsGeoMagneticField() {
        checkSupportsSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
    }

    public void testSupportsMagneticFieldUncalibrated() {
        checkSupportsSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
    }

    public void testSupportsPressure() {
        checkSupportsSensor(Sensor.TYPE_PRESSURE);
    }

    public void testSupportsRotationVector() {
        checkSupportsSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public void testSupportsSignificantMotion() {
        checkSupportsSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
    }

    public void testSupportsStepDetector() {
        checkSupportsSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    public void testSupportsStepCounter() {
        checkSupportsSensor(Sensor.TYPE_STEP_COUNTER);
    }

    public void testSupportsTiltDetector() {
        final int TYPE_TILT_DETECTOR = 22;
        checkSupportsSensor(TYPE_TILT_DETECTOR);
    }

    private void checkSupportsSensor(int sensorType) {
        if (mAreHifiSensorsSupported) {
            assertTrue(mSensorManager.getDefaultSensor(sensorType) != null);
        }
    }
}
