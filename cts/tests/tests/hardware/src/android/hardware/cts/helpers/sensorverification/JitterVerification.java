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

package android.hardware.cts.helpers.sensorverification;

import junit.framework.Assert;

import android.content.Context;
import android.content.pm.PackageManager;

import android.util.Log;
import android.hardware.Sensor;
import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorStats;
import android.hardware.cts.helpers.TestSensorEnvironment;
import android.hardware.cts.helpers.TestSensorEvent;
import android.util.SparseIntArray;

import com.android.cts.util.StatisticsUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link ISensorVerification} which verifies that the sensor jitter is in an acceptable range.
 */
public class JitterVerification extends AbstractSensorVerification {
    public static final String PASSED_KEY = "jitter_passed";

    // sensorType: threshold (% of expected period)
    private static final SparseIntArray DEFAULTS = new SparseIntArray(12);
    // Max allowed jitter (in percentage).
    private static final int GRACE_FACTOR = 2;
    private static final int THRESHOLD_PERCENT_FOR_HIFI_SENSORS = 1 * GRACE_FACTOR;
    static {
        // Use a method so that the @deprecation warning can be set for that method only
        setDefaults();
    }

    private final int mThresholdAsPercentage;
    private final List<Long> mTimestamps = new LinkedList<Long>();

    /**
     * Construct a {@link JitterVerification}
     *
     * @param thresholdAsPercentage the acceptable margin of error as a percentage
     */
    public JitterVerification(int thresholdAsPercentage) {
        mThresholdAsPercentage = thresholdAsPercentage;
    }

    /**
     * Get the default {@link JitterVerification} for a sensor.
     *
     * @param environment the test environment
     * @return the verification or null if the verification does not apply to the sensor.
     */
    public static JitterVerification getDefault(TestSensorEnvironment environment) {
        int sensorType = environment.getSensor().getType();
        int threshold = DEFAULTS.get(sensorType, -1);
        if (threshold == -1) {
            return null;
        }
        boolean hasHifiSensors = environment.getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_HIFI_SENSORS);
        if (hasHifiSensors) {
           threshold = THRESHOLD_PERCENT_FOR_HIFI_SENSORS;
        }
        return new JitterVerification(threshold);
    }

    /**
     * Verify that the 95th percentile of the jitter is in the acceptable range. Add
     * {@value #PASSED_KEY} and {@value SensorStats#JITTER_95_PERCENTILE_PERCENT_KEY} keys to
     * {@link SensorStats}.
     *
     * @throws AssertionError if the verification failed.
     */
    @Override
    public void verify(TestSensorEnvironment environment, SensorStats stats) {
        int timestampsCount = mTimestamps.size();
        if (timestampsCount < 2 || environment.isSensorSamplingRateOverloaded()) {
            // the verification is not reliable in environments under load
            stats.addValue(PASSED_KEY, true);
            return;
        }

        List<Double> jitters = getJitterValues();
        double jitter95PercentileNs = SensorCtsHelper.get95PercentileValue(jitters);
        long firstTimestamp = mTimestamps.get(0);
        long lastTimestamp = mTimestamps.get(timestampsCount - 1);
        long measuredPeriodNs = (lastTimestamp - firstTimestamp) / (timestampsCount - 1);
        double jitter95PercentilePercent = (jitter95PercentileNs * 100.0) / measuredPeriodNs;
        stats.addValue(SensorStats.JITTER_95_PERCENTILE_PERCENT_KEY, jitter95PercentilePercent);

        boolean success = (jitter95PercentilePercent < mThresholdAsPercentage);
        stats.addValue(PASSED_KEY, success);

        if (!success) {
            String message = String.format(
                    "Jitter out of range: measured period=%dns, jitter(95th percentile)=%.2f%%"
                            + " (expected < %d%%)",
                    measuredPeriodNs,
                    jitter95PercentilePercent,
                    mThresholdAsPercentage);
            Assert.fail(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JitterVerification clone() {
        return new JitterVerification(mThresholdAsPercentage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addSensorEventInternal(TestSensorEvent event) {
        mTimestamps.add(event.timestamp);
    }

    /**
     * Get the list of all jitter values. Exposed for unit testing.
     */
    List<Double> getJitterValues() {
        List<Long> deltas = new ArrayList<Long>(mTimestamps.size() - 1);
        for (int i = 1; i < mTimestamps.size(); i++) {
            deltas.add(mTimestamps.get(i) - mTimestamps.get(i - 1));
        }
        double deltaMean = StatisticsUtils.getMean(deltas);
        List<Double> jitters = new ArrayList<Double>(deltas.size());
        for (long delta : deltas) {
            jitters.add(Math.abs(delta - deltaMean));
        }
        return jitters;
    }

    @SuppressWarnings("deprecation")
    private static void setDefaults() {
        DEFAULTS.put(Sensor.TYPE_ACCELEROMETER, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_MAGNETIC_FIELD, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GYROSCOPE, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_ORIENTATION, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_PRESSURE, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GRAVITY, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_LINEAR_ACCELERATION, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_ROTATION_VECTOR, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GAME_ROTATION_VECTOR, Integer.MAX_VALUE);
        DEFAULTS.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, Integer.MAX_VALUE);
    }
}
