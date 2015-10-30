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
package com.android.compatibility.common.deviceinfo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An instrumentation that runs all activities that extends DeviceInfoActivity.
 */
public class DeviceInfoInstrument extends Instrumentation {

    private static final String LOG_TAG = "ExtendedDeviceInfo";
    private static final String COLLECTOR = "collector";

    // List of collectors to run. If null or empty, all collectors will run.
    private Set<String> mCollectorSet = new HashSet<String>();

    // Results sent to the caller when this istrumentation completes.
    private Bundle mBundle = new Bundle();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String collectorList = savedInstanceState.getString(COLLECTOR);
            if (!TextUtils.isEmpty(collectorList)) {
                for (String collector : TextUtils.split(collectorList, ",")) {
                  if (!TextUtils.isEmpty(collector)) {
                    mCollectorSet.add(collector);
                  }
                }
            }
        }
        start();
    }

    @Override
    public void onStart() {
        try {
            Context context = getContext();
            ActivityInfo[] activities = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_ACTIVITIES).activities;
            for (ActivityInfo activityInfo : activities) {
                runActivity(activityInfo.name);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception occurred while running activities.", e);
            // Returns INSTRUMENTATION_CODE: 0
            finish(Activity.RESULT_CANCELED, mBundle);
        }
        // Returns INSTRUMENTATION_CODE: -1
        finish(Activity.RESULT_OK, mBundle);
    }

    /**
     * Returns true if the activity meets the criteria to run; otherwise, false.
     */
    private boolean isActivityRunnable(Class activityClass) {
        // Don't run the base DeviceInfoActivity class.
        if (DeviceInfoActivity.class == activityClass) {
            return false;
        }
        // Don't run anything that doesn't extends DeviceInfoActivity.
        if (!DeviceInfoActivity.class.isAssignableFrom(activityClass)) {
            return false;
        }
        // Only run activity if mCollectorSet is empty or contains it.
        if (mCollectorSet != null && mCollectorSet.size() > 0) {
            return mCollectorSet.contains(activityClass.getName());
        }
        // Run anything that makes it here.
        return true;
    }

    /**
     * Runs a device info activity and return the file path where the results are written to.
     */
    private void runActivity(String activityName) throws Exception {
        Class activityClass = null;
        try {
            activityClass = Class.forName(activityName);
        } catch (ClassNotFoundException e) {
            return;
        }

        if (activityClass == null || !isActivityRunnable(activityClass)) {
            return;
        }

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(this.getContext(), activityName);

        DeviceInfoActivity activity = (DeviceInfoActivity) startActivitySync(intent);
        waitForIdleSync();
        activity.waitForActivityToFinish();

        String className = activityClass.getSimpleName();
        String errorMessage = activity.getErrorMessage();
        if (TextUtils.isEmpty(errorMessage)) {
            mBundle.putString(className, activity.getResultFilePath());
        } else {
            mBundle.putString(className, errorMessage);
            throw new Exception(errorMessage);
        }
    }
}

