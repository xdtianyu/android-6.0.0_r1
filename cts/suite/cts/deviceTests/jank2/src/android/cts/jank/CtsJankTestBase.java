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
 * limitations under the License.
 */

package android.cts.jank;

import android.cts.util.DeviceReportLog;
import android.os.Bundle;
import android.support.test.jank.JankTestBase;
import android.support.test.jank.WindowContentFrameStatsMonitor;
import android.support.test.uiautomator.UiDevice;

import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;

public abstract class CtsJankTestBase extends JankTestBase {

    private UiDevice mDevice;
    private DeviceReportLog mLog;

    @Override
    public void afterTest(Bundle metrics) {
        String source = String.format("%s#%s", getClass().getCanonicalName(), getName());
        mLog.printValue(source, WindowContentFrameStatsMonitor.KEY_AVG_FPS,
                metrics.getDouble(WindowContentFrameStatsMonitor.KEY_AVG_FPS),
                ResultType.HIGHER_BETTER, ResultUnit.FPS);
        mLog.printValue(source, WindowContentFrameStatsMonitor.KEY_AVG_LONGEST_FRAME,
                metrics.getDouble(WindowContentFrameStatsMonitor.KEY_AVG_LONGEST_FRAME),
                ResultType.LOWER_BETTER, ResultUnit.MS);
        mLog.printValue(source, WindowContentFrameStatsMonitor.KEY_MAX_NUM_JANKY,
                metrics.getInt(WindowContentFrameStatsMonitor.KEY_MAX_NUM_JANKY),
                ResultType.LOWER_BETTER, ResultUnit.COUNT);
        mLog.printSummary(WindowContentFrameStatsMonitor.KEY_AVG_NUM_JANKY,
                metrics.getDouble(WindowContentFrameStatsMonitor.KEY_AVG_NUM_JANKY),
                ResultType.LOWER_BETTER, ResultUnit.COUNT);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mLog = new DeviceReportLog();
        // fix device orientation
        mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.setOrientationNatural();
    }

    @Override
    protected void tearDown() throws Exception {
        mLog.deliverReportToHost(getInstrumentation());
        // restore device orientation
        mDevice.unfreezeRotation();
        super.tearDown();
    }

    protected UiDevice getUiDevice() {
        return mDevice;
    }
}
