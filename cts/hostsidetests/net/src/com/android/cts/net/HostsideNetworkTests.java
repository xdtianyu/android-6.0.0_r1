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

package com.android.cts.net;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.ddmlib.testrunner.TestResult;
import com.android.ddmlib.testrunner.TestResult.TestStatus;
import com.android.ddmlib.testrunner.TestRunResult;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.testtype.IAbiReceiver;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.CollectingTestListener;

import java.util.Map;

public class HostsideNetworkTests extends DeviceTestCase implements IAbiReceiver, IBuildReceiver {
    private static final String TEST_PKG = "com.android.cts.net.hostside";
    private static final String TEST_APK = "CtsHostsideNetworkTestsApp.apk";

    private IAbi mAbi;
    private CtsBuildHelper mCtsBuild;

    @Override
    public void setAbi(IAbi abi) {
        mAbi = abi;
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        assertNotNull(mAbi);
        assertNotNull(mCtsBuild);

        getDevice().uninstallPackage(TEST_PKG);

        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(TEST_APK), false));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        getDevice().uninstallPackage(TEST_PKG);
    }

    public void testVpn() throws Exception {
        runDeviceTests(TEST_PKG, ".VpnTest");
    }

    public void runDeviceTests(String packageName, String testClassName)
           throws DeviceNotAvailableException {
        RemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(packageName,
                "android.support.test.runner.AndroidJUnitRunner", getDevice().getIDevice());

        final CollectingTestListener listener = new CollectingTestListener();
        getDevice().runInstrumentationTests(testRunner, listener);

        final TestRunResult result = listener.getCurrentRunResults();
        if (result.isRunFailure()) {
            throw new AssertionError("Failed to successfully run device tests for "
                    + result.getName() + ": " + result.getRunFailureMessage());
        }

        if (result.hasFailedTests()) {
            // build a meaningful error message
            StringBuilder errorBuilder = new StringBuilder("on-device tests failed:\n");
            for (Map.Entry<TestIdentifier, TestResult> resultEntry :
                result.getTestResults().entrySet()) {
                if (!resultEntry.getValue().getStatus().equals(TestStatus.PASSED)) {
                    errorBuilder.append(resultEntry.getKey().toString());
                    errorBuilder.append(":\n");
                    errorBuilder.append(resultEntry.getValue().getStackTrace());
                }
            }
            throw new AssertionError(errorBuilder.toString());
        }
    }
}
