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

import java.util.Arrays;

/**
 * Unit tests for {@link ReportLog}
 */
public class ReportLogTest extends TestCase {

    private static final double[] VALUES = new double[] {1, 11, 21, 1211, 111221};

    private static final String EXPECTED_ENCODED_REPORT_LOG =
            "com.android.compatibility.common.util.ReportLogTest#testEncodeDecode:44|" +
            "Sample Summary| |HIGHER_BETTER|BYTE|1.0 ++++" +
            "com.android.compatibility.common.util.ReportLogTest#testEncodeDecode:45|" +
            "Details| |NEUTRAL|FPS|1.0 11.0 21.0 1211.0 111221.0 ";
    private ReportLog reportLog;

    @Override
    protected void setUp() throws Exception {
        this.reportLog = new ReportLog();
    }

    public void testEncodeDecode() {

        reportLog.setSummary("Sample Summary", 1.0, ResultType.HIGHER_BETTER, ResultUnit.BYTE);
        reportLog.addValues("Details", VALUES, ResultType.NEUTRAL, ResultUnit.FPS);

        String encodedReportLog = reportLog.toEncodedString();
        assertEquals(EXPECTED_ENCODED_REPORT_LOG, encodedReportLog);

        ReportLog decodedReportLog = ReportLog.fromEncodedString(encodedReportLog);
        ReportLog.Result summary = reportLog.getSummary();
        assertEquals("Sample Summary", summary.getMessage());
        assertFalse(summary.getLocation().isEmpty());
        assertEquals(ResultType.HIGHER_BETTER, summary.getType());
        assertEquals(ResultUnit.BYTE, summary.getUnit());
        assertTrue(Arrays.equals(new double[] {1.0}, summary.getValues()));

        assertEquals(1, decodedReportLog.getDetailedMetrics().size());
        ReportLog.Result detail = decodedReportLog.getDetailedMetrics().get(0);
        assertEquals("Details", detail.getMessage());
        assertFalse(detail.getLocation().isEmpty());
        assertEquals(ResultType.NEUTRAL, detail.getType());
        assertEquals(ResultUnit.FPS, detail.getUnit());
        assertTrue(Arrays.equals(VALUES, detail.getValues()));

        assertEquals(encodedReportLog, decodedReportLog.toEncodedString());
    }
}
