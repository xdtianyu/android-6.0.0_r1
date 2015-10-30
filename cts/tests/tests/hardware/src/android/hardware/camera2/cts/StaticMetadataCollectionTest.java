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

package android.hardware.camera2.cts;

import android.content.pm.PackageManager;
import android.cts.util.DeviceReportLog;
import android.hardware.camera2.cts.testcases.Camera2SurfaceViewTestCase;
import android.hardware.camera2.cts.helpers.CameraMetadataGetter;
import android.util.Log;

import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * This test collects camera2 API static metadata and reports to device report.
 *
 */
public class StaticMetadataCollectionTest extends Camera2SurfaceViewTestCase {
    private static final String TAG = "StaticMetadataCollectionTest";

    private DeviceReportLog mReportLog;

    @Override
    protected void setUp() throws Exception {
        mReportLog = new DeviceReportLog();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        // Deliver the report to host will automatically clear the report log.
        mReportLog.deliverReportToHost(getInstrumentation());
        super.tearDown();
    }

    public void testDataCollection() {
        if (hasCameraFeature()) {
            CameraMetadataGetter cameraInfoGetter = new CameraMetadataGetter(mCameraManager);
            for (String id : mCameraIds) {
                // Gather camera info
                JSONObject cameraInfo = cameraInfoGetter.getCameraInfo(id);
                dumpJsonObjectAsCtsResult(String.format("camera2_id%s_static_info", id), cameraInfo);
                dumpDoubleAsCtsResult(String.format("camera2_id%s_static_info:", id)
                        + cameraInfo.toString(), 0);

                JSONObject[] templates = cameraInfoGetter.getCaptureRequestTemplates(id);
                for (int i = 0; i < templates.length; i++) {
                    dumpJsonObjectAsCtsResult(String.format("camera2_id%s_capture_template%d",
                            id, CameraMetadataGetter.TEMPLATE_IDS[i]), templates[i]);
                    if (templates[i] != null) {
                        dumpDoubleAsCtsResult(String.format("camera2_id%s_capture_template%d:",
                                id, CameraMetadataGetter.TEMPLATE_IDS[i])
                                + templates[i].toString(), 0);
                    }
                }
            }

            try {
                cameraInfoGetter.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to close camera info getter " + e.getMessage());
            }

            mReportLog.printSummary("Camera data collection for static info and capture request"
                    + " templates",
                    0.0, ResultType.NEUTRAL, ResultUnit.NONE);
        }

    }

    private void dumpDoubleAsCtsResult(String name, double value) {
        mReportLog.printValue(name, value, ResultType.NEUTRAL, ResultUnit.NONE);
    }

    public void dumpDoubleArrayAsCtsResult(String name, double[] values) {
        mReportLog.printArray(name, values, ResultType.NEUTRAL, ResultUnit.NONE);
    }

    private double getJsonValueAsDouble(String name, Object obj) throws Exception {
        if (obj == null) {
            Log.e(TAG, "Null value: " + name);
            throw new Exception();
        } else if (obj instanceof Double) {
            return ((Double)obj).doubleValue();
        } else if (obj instanceof Float) {
            return ((Float)obj).floatValue();
        } else if (obj instanceof Long) {
            return ((Long)obj).longValue();
        } else if (obj instanceof Integer) {
            return ((Integer)obj).intValue();
        } else if (obj instanceof Byte) {
            return ((Byte)obj).intValue();
        } else if (obj instanceof Short) {
            return ((Short)obj).intValue();
        } else if (obj instanceof Boolean) {
            return ((Boolean)obj) ? 1 : 0;
        } else {
            Log.e(TAG, "Unsupported value type: " + name);
            throw new Exception();
        }
    }

    private void dumpJsonArrayAsCtsResult(String name, JSONArray arr) throws Exception {
        if (arr == null || arr.length() == 0) {
            dumpDoubleAsCtsResult(name + "[]", 0);
        } else if (arr.get(0) instanceof JSONObject) {
            for (int i = 0; i < arr.length(); i++) {
                dumpJsonObjectAsCtsResult(name+String.format("[%04d]",i),(JSONObject)arr.get(i));
            }
        } else if (arr.get(0) instanceof JSONArray) {
            for (int i = 0; i < arr.length(); i++) {
                dumpJsonArrayAsCtsResult(name+String.format("[%04d]",i),(JSONArray)arr.get(i));
            }
        } else if (!(arr.get(0) instanceof String)) {
            double[] values = new double[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                values[i] = getJsonValueAsDouble(name + "[]", arr.get(i));
            }
            dumpDoubleArrayAsCtsResult(name + "[]", values);
        } else if (arr.get(0) instanceof String) {
            for (int i = 0; i < arr.length(); i++) {
                dumpDoubleAsCtsResult(
                        name+String.format("[%04d]",i)+" = "+(String)arr.get(i), 0);
            }
        } else {
            Log.e(TAG, "Unsupported array value type: " + name);
            throw new Exception();
        }
    }

    private void dumpJsonObjectAsCtsResult(String name, JSONObject obj) {
        if (obj == null) {
            dumpDoubleAsCtsResult(name + "{}", 0);
            return;
        }
        Iterator<?> keys = obj.keys();
        while (keys.hasNext()) {
            try {
                String key = (String)keys.next();
                if (obj.get(key) instanceof JSONObject) {
                    dumpJsonObjectAsCtsResult(name+"."+key, (JSONObject)obj.get(key));
                } else if (obj.get(key) instanceof JSONArray) {
                    dumpJsonArrayAsCtsResult(name+"."+key, (JSONArray)obj.get(key));
                } else if (!(obj.get(key) instanceof String)) {
                    dumpDoubleAsCtsResult(name+"."+key,
                            getJsonValueAsDouble(name+"."+key, obj.get(key)));
                } else if (obj.get(key) instanceof String) {
                    dumpDoubleAsCtsResult(name+"."+key + " = " + (String)obj.get(key), 0);
                } else {
                    Log.e(TAG, "Unsupported object field type: " + name + "." + key);
                }
            } catch (Exception e) {
                // Swallow
            }
        }
    }

    private boolean hasCameraFeature() {
        PackageManager packageManager = getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }
}
