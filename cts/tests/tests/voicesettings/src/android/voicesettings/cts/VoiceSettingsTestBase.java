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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import common.src.android.voicesettings.common.Utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VoiceSettingsTestBase extends ActivityInstrumentationTestCase2<TestStartActivity> {
    static final String TAG = "VoiceSettingsTestBase";
    protected static final int TIMEOUT_MS = 20 * 1000;

    protected Context mContext;
    protected Bundle mResultExtras;
    private CountDownLatch mLatch;
    private ActivityDoneReceiver mActivityDoneReceiver = null;
    private TestStartActivity mActivity;
    private Utils.TestcaseType mTestCaseType;

    public VoiceSettingsTestBase() {
        super(TestStartActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
    }

    @Override
    protected void tearDown() throws Exception {
        mContext.unregisterReceiver(mActivityDoneReceiver);
        super.tearDown();
    }

    protected void startTestActivity(String intentSuffix) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.TEST_START_ACTIVITY_" + intentSuffix);
        intent.setComponent(new ComponentName(getInstrumentation().getContext(),
                TestStartActivity.class));
        setActivityIntent(intent);
        mActivity = getActivity();
    }

    protected void registerBroadcastReceiver(Utils.TestcaseType testCaseType) throws Exception {
        mTestCaseType = testCaseType;
        mLatch = new CountDownLatch(1);
        if (mActivityDoneReceiver != null) {
            mContext.unregisterReceiver(mActivityDoneReceiver);
        }
        mActivityDoneReceiver = new ActivityDoneReceiver();
        mContext.registerReceiver(mActivityDoneReceiver,
                new IntentFilter(Utils.BROADCAST_INTENT + testCaseType.toString()));
    }

    protected boolean startTestAndWaitForBroadcast(Utils.TestcaseType testCaseType)
            throws Exception {
        Log.i(TAG, "Begin Testing: " + testCaseType);
        registerBroadcastReceiver(testCaseType);
        mActivity.startTest(testCaseType.toString());
        if (!mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Failed to receive broadcast in " + TIMEOUT_MS + "msec");
            return false;
        }
        return true;
    }

    class ActivityDoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    Utils.BROADCAST_INTENT +
                        VoiceSettingsTestBase.this.mTestCaseType.toString())) {
                Bundle extras = intent.getExtras();
                Log.i(TAG, "received_broadcast for " + Utils.toBundleString(extras));
                VoiceSettingsTestBase.this.mResultExtras = extras;
                mLatch.countDown();
            }
        }
    }
}
