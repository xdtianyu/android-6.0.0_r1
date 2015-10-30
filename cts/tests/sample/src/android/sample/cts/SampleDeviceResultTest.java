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
package android.sample.cts;

import com.android.cts.util.MeasureRun;
import com.android.cts.util.MeasureTime;
import com.android.cts.util.ReportLog;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;
import com.android.cts.util.Stat;

import android.cts.util.CtsAndroidTestCase;

/**
 * A simple compatibility test which includes results in the report.
 *
 * This test measures the time taken to run a workload and adds in the report.
 */
public class SampleDeviceResultTest extends CtsAndroidTestCase {

    /**
     * The number of times to repeat the test.
     */
    private static final int REPEAT = 5;

    /**
     * The input number for the factorial.
     */
    private static final int IN = 15;

    /**
     * The expected output number for the factorial.
     */
    private static final long OUT = 1307674368000L;

    /**
     * Measures the time taken to compute the factorial of 15 with a recursive method.
     *
     * @throws Exception
     */
    public void testFactorialRecursive() throws Exception {
        runTest(new MeasureRun() {
            @Override
            public void run(int i) throws Exception {
                // Compute the factorial and assert it is correct.
                assertEquals("Incorrect result", OUT, factorialRecursive(IN));
            }
        });
    }

    /**
     * Measures the time taken to compute the factorial of 15 with a iterative method.
     *
     * @throws Exception
     */
    public void testFactorialIterative() throws Exception {
        runTest(new MeasureRun() {
            @Override
            public void run(int i) throws Exception {
                // Compute the factorial and assert it is correct.
                assertEquals("Incorrect result", OUT, factorialIterative(IN));
            }
        });
    }

    /**
     * Computes the factorial of a number with a recursive method.
     *
     * @param num The number to compute the factorial of.
     */
    private static long factorialRecursive(int num) {
        if (num <= 0) {
            return 1;
        }
        return num * factorialRecursive(num - 1);
    }

    /**
     * Computes the factorial of a number with a iterative method.
     *
     * @param num The number to compute the factorial of.
     */
    private static long factorialIterative(int num) {
        long result = 1;
        for (int i = 2; i <= num; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Runs the workload and records the result to the report log.
     *
     * @param workload
     */
    private void runTest(MeasureRun workload) throws Exception {
        // MeasureTime runs the workload N times and records the time taken by each run.
        double[] result = MeasureTime.measure(REPEAT, workload);
        // Compute the stats.
        Stat.StatResult stat = Stat.getStat(result);
        // Get the report for this test and add the results to record.
        ReportLog log = getReportLog();
        log.printArray("Times", result, ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue("Min", stat.mMin, ResultType.LOWER_BETTER, ResultUnit.MS);
        log.printValue("Max", stat.mMax, ResultType.LOWER_BETTER, ResultUnit.MS);
        // Every report must have a summary,
        log.printSummary("Average", stat.mAverage, ResultType.LOWER_BETTER, ResultUnit.MS);
    }
}
