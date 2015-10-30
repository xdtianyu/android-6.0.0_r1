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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * Collect device information on target device and write to a JSON file.
 */
public abstract class DeviceInfoActivity extends Activity {

    /** Device info result code: collector failed to complete. */
    private static final int DEVICE_INFO_RESULT_FAILED = -2;
    /** Device info result code: collector completed with error. */
    private static final int DEVICE_INFO_RESULT_ERROR = -1;
    /** Device info result code: collector has started but not completed. */
    private static final int DEVICE_INFO_RESULT_STARTED = 0;
    /** Device info result code: collector completed success. */
    private static final int DEVICE_INFO_RESULT_OK = 1;

    private static final int MAX_STRING_VALUE_LENGTH = 1000;
    private static final int MAX_ARRAY_LENGTH = 1000;

    private static final String LOG_TAG = "DeviceInfoActivity";

    private CountDownLatch mDone = new CountDownLatch(1);
    private JsonWriter mJsonWriter = null;
    private String mResultFilePath = null;
    private String mErrorMessage = "Collector has started.";
    private int mResultCode = DEVICE_INFO_RESULT_STARTED;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (createFilePath()) {
            createJsonWriter();
            startJsonWriter();
            collectDeviceInfo();
            closeJsonWriter();

            if (mResultCode == DEVICE_INFO_RESULT_STARTED) {
                mResultCode = DEVICE_INFO_RESULT_OK;
            }
        }

        Intent data = new Intent();
        if (mResultCode == DEVICE_INFO_RESULT_OK) {
            data.setData(Uri.parse(mResultFilePath));
            setResult(RESULT_OK, data);
        } else {
            data.setData(Uri.parse(mErrorMessage));
            setResult(RESULT_CANCELED, data);
        }

        mDone.countDown();
        finish();
    }

    /**
     * Method to collect device information.
     */
    protected abstract void collectDeviceInfo();

    void waitForActivityToFinish() {
        try {
            mDone.await();
        } catch (Exception e) {
            failed("Exception while waiting for activity to finish: " + e.getMessage());
        }
    }

    /**
     * Returns the error message if collector did not complete successfully.
     */
    String getErrorMessage() {
        if (mResultCode == DEVICE_INFO_RESULT_OK) {
            return null;
        }
        return mErrorMessage;
    }

    /**
     * Returns the path to the json file if collector completed successfully.
     */
    String getResultFilePath() {
        if (mResultCode == DEVICE_INFO_RESULT_OK) {
            return mResultFilePath;
        }
        return null;
    }

    private void error(String message) {
        mResultCode = DEVICE_INFO_RESULT_ERROR;
        mErrorMessage = message;
        Log.e(LOG_TAG, message);
    }

    private void failed(String message) {
        mResultCode = DEVICE_INFO_RESULT_FAILED;
        mErrorMessage = message;
        Log.e(LOG_TAG, message);
    }

    private boolean createFilePath() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            failed("External storage is not mounted");
            return false;
        }
        final File dir = new File(Environment.getExternalStorageDirectory(), "device-info-files");
        if (!dir.mkdirs() && !dir.isDirectory()) {
            failed("Cannot create directory for device info files");
            return false;
        }

        // Create file at /sdcard/device-info-files/<class_name>.deviceinfo.json
        final File jsonFile = new File(dir, getClass().getSimpleName() + ".deviceinfo.json");
        try {
            jsonFile.createNewFile();
        } catch (Exception e) {
            failed("Cannot create file to collect device info");
            return false;
        }
        mResultFilePath = jsonFile.getAbsolutePath();
        return true;
    }

    private void createJsonWriter() {
        try {
            FileOutputStream out = new FileOutputStream(mResultFilePath);
            mJsonWriter = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            // TODO(agathaman): remove to make json output less pretty
            mJsonWriter.setIndent("  ");
        } catch (Exception e) {
            failed("Failed to create JSON writer: " + e.getMessage());
        }
    }

    private void startJsonWriter() {
        try {
            mJsonWriter.beginObject();
        } catch (Exception e) {
            failed("Failed to begin JSON object: " + e.getMessage());
        }
    }

    private void closeJsonWriter() {
        try {
            mJsonWriter.endObject();
            mJsonWriter.close();
        } catch (Exception e) {
            failed("Failed to close JSON object: " + e.getMessage());
        }
    }

    /**
     * Start a new group of result.
     */
    public void startGroup() {
        try {
            mJsonWriter.beginObject();
        } catch (Exception e) {
            error("Failed to begin JSON group: " + e.getMessage());
        }
    }

    /**
     * Start a new group of result with specified name.
     */
    public void startGroup(String name) {
        try {
            mJsonWriter.name(name);
            mJsonWriter.beginObject();
        } catch (Exception e) {
            error("Failed to begin JSON group: " + e.getMessage());
        }
    }

    /**
     * Complete adding result to the last started group.
     */
    public void endGroup() {
        try {
            mJsonWriter.endObject();
        } catch (Exception e) {
            error("Failed to end JSON group: " + e.getMessage());
        }
    }

    /**
     * Start a new array of result.
     */
    public void startArray() {
        try {
            mJsonWriter.beginArray();
        } catch (Exception e) {
            error("Failed to begin JSON array: " + e.getMessage());
        }
    }

    /**
     * Start a new array of result with specified name.
     */
    public void startArray(String name) {
        checkName(name);
        try {
            mJsonWriter.name(name);
            mJsonWriter.beginArray();
        } catch (Exception e) {
            error("Failed to begin JSON array: " + e.getMessage());
        }
    }

    /**
     * Complete adding result to the last started array.
     */
    public void endArray() {
        try {
            mJsonWriter.endArray();
        } catch (Exception e) {
            error("Failed to end JSON group: " + e.getMessage());
        }
    }

    /**
     * Add a double value result.
     */
    public void addResult(String name, double value) {
        checkName(name);
        try {
            mJsonWriter.name(name).value(value);
        } catch (Exception e) {
            error("Failed to add result for type double: " + e.getMessage());
        }
    }

    /**
    * Add a long value result.
    */
    public void addResult(String name, long value) {
        checkName(name);
        try {
            mJsonWriter.name(name).value(value);
        } catch (Exception e) {
            error("Failed to add result for type long: " + e.getMessage());
        }
    }

    /**
     * Add an int value result.
     */
    public void addResult(String name, int value) {
        checkName(name);
        try {
            mJsonWriter.name(name).value((Number) value);
        } catch (Exception e) {
            error("Failed to add result for type int: " + e.getMessage());
        }
    }

    /**
     * Add a boolean value result.
     */
    public void addResult(String name, boolean value) {
        checkName(name);
        try {
            mJsonWriter.name(name).value(value);
        } catch (Exception e) {
            error("Failed to add result for type boolean: " + e.getMessage());
        }
    }

    /**
     * Add a String value result.
     */
    public void addResult(String name, String value) {
        checkName(name);
        try {
            mJsonWriter.name(name).value(checkString(value));
        } catch (Exception e) {
            error("Failed to add result for type String: " + e.getMessage());
        }
    }

    /**
     * Add a double array result.
     */
    public void addArray(String name, double[] list) {
        checkName(name);
        try {
            mJsonWriter.name(name);
            mJsonWriter.beginArray();
            for (double value : checkArray(list)) {
                mJsonWriter.value(value);
            }
            mJsonWriter.endArray();
        } catch (Exception e) {
            error("Failed to add result array for type double: " + e.getMessage());
        }
    }

    /**
     * Add a long array result.
     */
    public void addArray(String name, long[] list) {
        checkName(name);
        try {
        mJsonWriter.name(name);
        mJsonWriter.beginArray();
        for (long value : checkArray(list)) {
            mJsonWriter.value(value);
        }
            mJsonWriter.endArray();
        } catch (Exception e) {
            error("Failed to add result array for type long: " + e.getMessage());
        }
    }

    /**
     * Add an int array result.
     */
    public void addArray(String name, int[] list) {
        checkName(name);
        try {
            mJsonWriter.name(name);
            mJsonWriter.beginArray();
            for (int value : checkArray(list)) {
                mJsonWriter.value((Number) value);
            }
            mJsonWriter.endArray();
        } catch (Exception e) {
            error("Failed to add result array for type int: " + e.getMessage());
        }
    }

    /**
     * Add a boolean array result.
     */
    public void addArray(String name, boolean[] list) {
        checkName(name);
        try {
            mJsonWriter.name(name);
            mJsonWriter.beginArray();
            for (boolean value : checkArray(list)) {
                mJsonWriter.value(value);
            }
            mJsonWriter.endArray();
        } catch (Exception e) {
            error("Failed to add result array for type boolean: " + e.getMessage());
        }
    }

    /**
     * Add a String array result.
     */
    public void addArray(String name, String[] list) {
        checkName(name);
        try {
            mJsonWriter.name(name);
            mJsonWriter.beginArray();
            for (String value : checkArray(list)) {
                mJsonWriter.value(checkString(value));
            }
            mJsonWriter.endArray();
        } catch (Exception e) {
            error("Failed to add result array for type Sting: " + e.getMessage());
        }
    }

    private static boolean[] checkArray(boolean[] values) {
        if (values.length > MAX_ARRAY_LENGTH) {
            return Arrays.copyOf(values, MAX_ARRAY_LENGTH);
        } else {
            return values;
        }
    }

    private static double[] checkArray(double[] values) {
        if (values.length > MAX_ARRAY_LENGTH) {
            return Arrays.copyOf(values, MAX_ARRAY_LENGTH);
        } else {
            return values;
        }
    }

    private static int[] checkArray(int[] values) {
        if (values.length > MAX_ARRAY_LENGTH) {
            return Arrays.copyOf(values, MAX_ARRAY_LENGTH);
        } else {
            return values;
        }
    }

    private static long[] checkArray(long[] values) {
        if (values.length > MAX_ARRAY_LENGTH) {
            return Arrays.copyOf(values, MAX_ARRAY_LENGTH);
        } else {
            return values;
        }
    }

    private static String[] checkArray(String[] values) {
        if (values.length > MAX_ARRAY_LENGTH) {
            return Arrays.copyOf(values, MAX_ARRAY_LENGTH);
        } else {
            return values;
        }
    }

    private static String checkString(String value) {
        if (value.length() > MAX_STRING_VALUE_LENGTH) {
            return value.substring(0, MAX_STRING_VALUE_LENGTH);
        }
        return value;
    }

    private static String checkName(String value) {
        if (TextUtils.isEmpty(value)) {
            throw new NullPointerException();
        }
        return value;
    }
}

