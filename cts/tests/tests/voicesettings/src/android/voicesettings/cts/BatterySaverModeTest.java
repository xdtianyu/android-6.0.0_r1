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

package android.voicesettings.cts;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import common.src.android.voicesettings.common.Utils;

public class BatterySaverModeTest extends VoiceSettingsTestBase {
    static final String TAG = "BatterySaverModeTest";

    public BatterySaverModeTest() {
        super();
    }

    public void testAll() throws Exception {
        startTestActivity("BATTERYSAVER_MODE");
        boolean modeIsOn = isModeOn();
        Log.i(TAG, "Before testing, BATTERYSAVER_MODE is set to: " + modeIsOn);
        if (modeIsOn) {
            // mode is currently ON.
            // run a test to turn it off.
            // After successful run of the test, run a test to turn it back on.
            if (!runTest(Utils.TestcaseType.BATTERYSAVER_MODE_OFF, false)) {
                // the test failed. don't test the next one.
                return;
            }
            runTest(Utils.TestcaseType.BATTERYSAVER_MODE_ON, true);
        } else {
            // mode is currently OFF.
            // run a test to turn it on.
            // After successful run of the test, run a test to turn it back off.
            if (!runTest(Utils.TestcaseType.BATTERYSAVER_MODE_ON, true)) {
                // the test failed. don't test the next one.
                return;
            }
            runTest(Utils.TestcaseType.BATTERYSAVER_MODE_OFF, false);
        }
    }

    private boolean runTest(Utils.TestcaseType test, boolean expectedMode) throws Exception {
        if (!startTestAndWaitForBroadcast(test)) {
            return false;
        }

        // Verify the test results
        // Since CTS test needs the device to be connected to the host computer via USB,
        // Batter Saver mode can't be turned on/off.
        // The most we can do is that the broadcast frmo MainInteractionSession is received
        // because that signals the firing and completion of BatterySaverModeVoiceActivity
        // caused by the intent to set Battery Saver mode.
        return true;
    }

    private boolean isModeOn() {
        PowerManager powerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        return powerManager.isPowerSaveMode();
    }
}
