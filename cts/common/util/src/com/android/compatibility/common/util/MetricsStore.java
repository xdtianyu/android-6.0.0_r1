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
 * limitations under the License
 */

package com.android.compatibility.common.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory store for metrics results. This should be used for hostside metrics reporting.
 */
public class MetricsStore {

    // needs concurrent version as there can be multiple client accessing this.
    // But there is no additional protection for the same key as that should not happen.
    private static final ConcurrentHashMap<String, ReportLog> mMap =
            new ConcurrentHashMap<String, ReportLog>();

    /**
     * Stores a result. Existing result with the same key will be replaced.
     * Note that key is generated in the form of device_serial#class#method name.
     * So there should be no concurrent test for the same (serial, class, method).
     * @param deviceSerial
     * @param abi
     * @param classMethodName
     * @param reportLog Contains the result to be stored
     */
    public static void storeResult(
            String deviceSerial, String abi, String classMethodName, ReportLog reportLog) {
        mMap.put(generateTestKey(deviceSerial, abi, classMethodName), reportLog);
    }

    /**
     * retrieves a metric result for the given condition and remove it from the internal
     * storage. If there is no result for the given condition, it will return null.
     */
    public static ReportLog removeResult(String deviceSerial, String abi, String classMethodName) {
        return mMap.remove(generateTestKey(deviceSerial, abi, classMethodName));
    }

    /**
     * @return test key in the form of device_serial#abi#class_name#method_name
     */
    private static String generateTestKey(String deviceSerial, String abi, String classMethodName) {
        return String.format("%s#%s#%s", deviceSerial, abi, classMethodName);
    }
}
