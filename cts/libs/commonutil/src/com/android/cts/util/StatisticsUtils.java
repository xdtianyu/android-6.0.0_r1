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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Set of static helper methods for CTS tests.
 */
public class StatisticsUtils {


    /**
     * Private constructor for static class.
     */
    private StatisticsUtils() {}

    /**
     * Get the value of the 95th percentile using nearest rank algorithm.
     *
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <TValue extends Comparable<? super TValue>> TValue get95PercentileValue(
            Collection<TValue> collection) {
        validateCollection(collection);

        List<TValue> arrayCopy = new ArrayList<TValue>(collection);
        Collections.sort(arrayCopy);

        // zero-based array index
        int arrayIndex = (int) Math.round(arrayCopy.size() * 0.95 + .5) - 1;

        return arrayCopy.get(arrayIndex);
    }

    /**
     * Calculate the mean of a collection.
     *
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <TValue extends Number> double getMean(Collection<TValue> collection) {
        validateCollection(collection);

        double sum = 0.0;
        for(TValue value : collection) {
            sum += value.doubleValue();
        }
        return sum / collection.size();
    }

    /**
     * Calculate the bias-corrected sample variance of a collection.
     *
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <TValue extends Number> double getVariance(Collection<TValue> collection) {
        validateCollection(collection);

        double mean = getMean(collection);
        ArrayList<Double> squaredDiffs = new ArrayList<Double>();
        for(TValue value : collection) {
            double difference = mean - value.doubleValue();
            squaredDiffs.add(Math.pow(difference, 2));
        }

        double sum = 0.0;
        for (Double value : squaredDiffs) {
            sum += value;
        }
        return sum / (squaredDiffs.size() - 1);
    }

    /**
     * Calculate the bias-corrected standard deviation of a collection.
     *
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <TValue extends Number> double getStandardDeviation(
            Collection<TValue> collection) {
        return Math.sqrt(getVariance(collection));
    }

    /**
     * Validate that a collection is not null or empty.
     *
     * @throws IllegalStateException if collection is null or empty.
     */
    private static <T> void validateCollection(Collection<T> collection) {
        if(collection == null || collection.size() == 0) {
            throw new IllegalStateException("Collection cannot be null or empty");
        }
    }

}
