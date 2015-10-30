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

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.util.HostReportLog;
import com.android.cts.util.MeasureRun;
import com.android.cts.util.MeasureTime;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;
import com.android.cts.util.ReportLog;
import com.android.cts.util.Stat;
import com.android.ddmlib.IDevice;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.testtype.IAbiReceiver;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.RunUtil;

import java.io.File;
import java.lang.Exception;

/**
 * Test to measure the transfer time of a file from the host to the device.
 */
public class SampleHostResultTest extends DeviceTestCase implements IAbiReceiver, IBuildReceiver {

    private static final String TAG = SampleHostResultTest.class.getSimpleName();

    /**
     * The number of times to repeat the test.
     */
    private static final int REPEAT = 5;

    /**
     * The name of the plan to transfer.
     *
     * In this case we will transfer the CTS.xml file.
     */
    private static final String PLAN_NAME = "CTS";

    /**
     * A reference to the build.
     */
    private CtsBuildHelper mBuild;

    /**
     * A reference to the device under test.
     */
    private ITestDevice mDevice;

    /**
     * A reference to the ABI under test.
     */
    private IAbi mAbi;

    @Override
    public void setAbi(IAbi abi) {
        mAbi = abi;
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        // Get the build, this is used to access the APK.
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Get the device, this gives a handle to run commands and install APKs.
        mDevice = getDevice();
    }

    /**
     * Measures the time taken to transfer a file to the device and then back.
     *
     * The workload is repeated several times and the report is populated with the result.
     *
     * @throws Exception
     */
    public void testTransferTime() throws Exception {
        final ITestDevice device = mDevice;
        // Get the external storage location and ensure its not null.
        final String externalStorePath = mDevice.getMountPoint(IDevice.MNT_EXTERNAL_STORAGE);
        assertNotNull("External storage location no found", externalStorePath);
        // Create the device side path where the file will be transfered.
        final String devicePath = String.format("%s/%s", externalStorePath, "tmp_testPushPull.txt");
        // Get the file from the build.
        final File testFile = mBuild.getTestPlanFile(PLAN_NAME);
        double[] result = MeasureTime.measure(REPEAT, new MeasureRun() {
            @Override
            public void prepare(int i) throws Exception {
                device.executeShellCommand(String.format("rm %s", devicePath));
            }
            @Override
            public void run(int i) throws Exception {
                // Create a temporary file to compare with.
                File tmpFile = FileUtil.createTempFile("tmp", "txt");
                try {
                    // Push the file across and ensure it exists.
                    assertTrue("Could not push file", device.pushFile(testFile, devicePath));
                    assertTrue("Unsuccessful transfer", device.doesFileExist(devicePath));
                    // Pull the file back and ensure it is the same.
                    assertTrue("Could not pull file", device.pullFile(devicePath, tmpFile));
                    assertFilesAreEqual(testFile, tmpFile);
                } finally {
                    // Clean up.
                    tmpFile.delete();
                    device.executeShellCommand(String.format("rm %s", devicePath));
                }
            }
        });
        // Compute the stats.
        Stat.StatResult stat = Stat.getStat(result);
        // Get the report for this test and add the results to record.
        HostReportLog report = new HostReportLog(mDevice.getSerialNumber(), mAbi.getName(),
                ReportLog.getClassMethodNames());
        report.printArray("Times", result, ResultType.LOWER_BETTER, ResultUnit.MS);
        report.printValue("Min", stat.mMin, ResultType.LOWER_BETTER, ResultUnit.MS);
        report.printValue("Max", stat.mMax, ResultType.LOWER_BETTER, ResultUnit.MS);
        // Every report must have a summary,
        report.printSummary("Average", stat.mAverage, ResultType.LOWER_BETTER, ResultUnit.MS);
        // Send the report to Tradefed.
        report.deliverReportToHost();
    }

    /**
     * Asserts the two given files are equal using the diff utility.
     *
     * @throws Exception
     */
    private static void assertFilesAreEqual(File first, File second) throws Exception {
        CommandResult result = RunUtil.getDefault().runTimedCmd(5000, "diff",
                first.getAbsolutePath(), second.getAbsolutePath());
        assertTrue("Diff failed to run", result.getStatus() == CommandStatus.SUCCESS);
        assertTrue("Files are not equivalent", "".equals(result.getStdout()));
    }

}
