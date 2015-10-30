/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.cts.verifier.sensors.helpers;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.concurrent.CountDownLatch;

/**
 * OpenCV library loader class
 */
public class OpenCVLibrary {

    private static String TAG = "OpenCVLibraryProbe";
    private static boolean mLoaded = false;

    /**
     * Load OpenCV Library in async mode
     * @param context Activity context
     */
    public static void loadAsync(Context context) {
        // only need to load once
        if (isLoaded())  return;

        // Load the library through loader
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, context,
                new BaseLoaderCallback(context) {
                    @Override
                    public void onManagerConnected(int status) {
                        Log.v(TAG, "New Loading status: "+status);
                        switch (status) {
                            case LoaderCallbackInterface.SUCCESS: {
                                mLoaded = true;
                            }
                            break;
                            default: {
                                super.onManagerConnected(status);
                            }
                            break;
                        }
                    }
                });
    }

    /**
     * Test if the library is loaded
     * @return a boolean indicates whether the OpenCV library is loaded.
     */
    public static boolean isLoaded() {
        return mLoaded;
    }
}
