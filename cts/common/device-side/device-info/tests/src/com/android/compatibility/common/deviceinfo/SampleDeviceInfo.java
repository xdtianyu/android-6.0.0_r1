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

import android.os.Bundle;

import java.lang.StringBuilder;

/**
 * Sample device info collector.
 */
public class SampleDeviceInfo extends DeviceInfoActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void collectDeviceInfo() {
        boolean[] booleans = {Boolean.TRUE, Boolean.FALSE};
        double[] doubles = {Double.MAX_VALUE, Double.MIN_VALUE};
        int[] ints = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        long[] longs = {Long.MAX_VALUE, Long.MIN_VALUE};

        // Group Foo
        startGroup("foo");
        addResult("foo_boolean", Boolean.TRUE);

        // Group Bar
        startGroup("bar");
        addArray("bar_string", new String[] {
                "bar-string-1",
                "bar-string-2",
                "bar-string-3"});

        addArray("bar_boolean", booleans);
        addArray("bar_double", doubles);
        addArray("bar_int", ints);
        addArray("bar_long", longs);
        endGroup(); // bar

        addResult("foo_double", Double.MAX_VALUE);
        addResult("foo_int", Integer.MAX_VALUE);
        addResult("foo_long", Long.MAX_VALUE);
        addResult("foo_string", "foo-string");

        StringBuilder sb = new StringBuilder();
        int[] arr = new int[1001];
        for (int i = 0; i < 1001; i++) {
            sb.append("a");
            arr[i] = i;
        }
        addResult("long_string", sb.toString());
        addArray("long_int_array", arr);

        endGroup(); // foo
    }
}

