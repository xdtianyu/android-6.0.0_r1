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

package com.android.cts.util;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for the {@link StatisticsUtils} class.
 */
public class StatisticsUtilsTest extends TestCase {

    /**
     * Test {@link StatisticsUtils#get95PercentileValue(Collection)}.
     */
    public void testGet95PercentileValue() {
        Collection<Integer> values = new HashSet<Integer>();
        for (int i = 0; i < 100; i++) {
            values.add(i);
        }
        assertEquals(95, (int) StatisticsUtils.get95PercentileValue(values));

        values = new HashSet<Integer>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }
        assertEquals(950, (int) StatisticsUtils.get95PercentileValue(values));

        values = new HashSet<Integer>();
        for (int i = 0; i < 100; i++) {
            values.add(i * i);
        }
        assertEquals(95 * 95, (int) StatisticsUtils.get95PercentileValue(values));
    }

    /**
     * Test {@link StatisticsUtils#getMean(Collection)}.
     */
    public void testGetMean() {
        List<Integer> values = Arrays.asList(0, 1, 2, 3, 4);
        double mean = StatisticsUtils.getMean(values);
        assertEquals(2.0, mean, 0.00001);

        values = Arrays.asList(1, 2, 3, 4, 5);
        mean = StatisticsUtils.getMean(values);
        assertEquals(3.0, mean, 0.00001);

        values = Arrays.asList(0, 1, 4, 9, 16);
        mean = StatisticsUtils.getMean(values);
        assertEquals(6.0, mean, 0.00001);
    }

    /**
     * Test {@link StatisticsUtils#getVariance(Collection)}.
     */
    public void testGetVariance() {
        List<Integer> values = Arrays.asList(0, 1, 2, 3, 4);
        double variance = StatisticsUtils.getVariance(values);
        assertEquals(2.5, variance, 0.00001);

        values = Arrays.asList(1, 2, 3, 4, 5);
        variance = StatisticsUtils.getVariance(values);
        assertEquals(2.5, variance, 0.00001);

        values = Arrays.asList(0, 2, 4, 6, 8);
        variance = StatisticsUtils.getVariance(values);
        assertEquals(10.0, variance, 0.00001);
    }

    /**
     * Test {@link StatisticsUtils#getStandardDeviation(Collection)}.
     */
    public void testGetStandardDeviation() {
        List<Integer> values = Arrays.asList(0, 1, 2, 3, 4);
        double stddev = StatisticsUtils.getStandardDeviation(values);
        assertEquals(Math.sqrt(2.5), stddev, 0.00001);

        values = Arrays.asList(1, 2, 3, 4, 5);
        stddev = StatisticsUtils.getStandardDeviation(values);
        assertEquals(Math.sqrt(2.5), stddev, 0.00001);

        values = Arrays.asList(0, 2, 4, 6, 8);
        stddev = StatisticsUtils.getStandardDeviation(values);
        assertEquals(Math.sqrt(10.0), stddev, 0.00001);
    }


}
