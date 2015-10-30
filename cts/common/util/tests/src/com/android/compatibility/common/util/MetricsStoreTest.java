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

import junit.framework.TestCase;

/**
 * Unit tests for {@link MetricsStore}
 */
public class MetricsStoreTest extends TestCase {

    private static final String DEVICE_SERIAL = "DEVICE_SERIAL";
    private static final String ABI = "ABI";
    private static final String CLASSMETHOD_NAME = "CLASSMETHOD_NAME";

    private static final double[] VALUES = new double[] {1, 11, 21, 1211, 111221};

    private ReportLog mReportLog;

    @Override
    protected void setUp() throws Exception {
        this.mReportLog = new ReportLog();
    }

    public void testStoreAndRemove() {
        mReportLog.setSummary("Sample Summary", 1.0, ResultType.HIGHER_BETTER, ResultUnit.BYTE);
        mReportLog.addValues("Details", VALUES, ResultType.NEUTRAL, ResultUnit.FPS);
        MetricsStore.storeResult(DEVICE_SERIAL, ABI, CLASSMETHOD_NAME, mReportLog);

        ReportLog reportLog = MetricsStore.removeResult(DEVICE_SERIAL, ABI, CLASSMETHOD_NAME);
        assertSame(mReportLog, reportLog);
        assertNull(MetricsStore.removeResult("blah", ABI, CLASSMETHOD_NAME));
    }

}
