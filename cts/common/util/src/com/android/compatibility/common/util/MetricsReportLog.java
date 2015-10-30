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

/**
 * A {@link ReportLog} that can be used with the in memory metrics store used for host side metrics.
 */
public final class MetricsReportLog extends ReportLog {
    private final String mDeviceSerial;
    private final String mAbi;
    private final String mClassMethodName;

    /**
     * @param deviceSerial serial number of the device
     * @param abi abi the test was run on
     * @param classMethodName class name and method name of the test in class#method format.
     *        Note that ReportLog.getClassMethodNames() provide this.
     */
    public MetricsReportLog(String deviceSerial, String abi, String classMethodName) {
        mDeviceSerial = deviceSerial;
        mAbi = abi;
        mClassMethodName = classMethodName;
    }

    public void submit() {
        MetricsStore.storeResult(mDeviceSerial, mAbi, mClassMethodName, this);
    }
}
