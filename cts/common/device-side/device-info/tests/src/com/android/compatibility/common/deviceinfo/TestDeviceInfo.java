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
 * Collector for testing DeviceInfoActivity
 */
public class TestDeviceInfo extends DeviceInfoActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void collectDeviceInfo() {

        // Test primitive results
        addResult("test_boolean", true);
        addResult("test_double", 1.23456789);
        addResult("test_int", 123456789);
        addResult("test_long", Long.MAX_VALUE);
        addResult("test_string", "test string");
        addArray("test_strings", new String[] {
            "test string 1",
            "test string 2",
            "test string 3",
        });

        // Test group
        startGroup("test_group");
        addResult("test_boolean", false);
        addResult("test_double", 9.87654321);
        addResult("test_int", 987654321);
        addResult("test_long", Long.MAX_VALUE);
        addResult("test_string", "test group string");
        addArray("test_strings", new String[] {
            "test group string 1",
            "test group string 2",
            "test group string 3"
        });
        endGroup(); // test_group

        // Test array of groups
        startArray("test_groups");
        for (int i = 1; i < 4; i++) {
            startGroup();
            addResult("test_string", "test groups string " + i);
            addArray("test_strings", new String[] {
                "test groups string " + i + "-1",
                "test groups string " + i + "-2",
                "test groups string " + i + "-3"
            });
            endGroup();
        }
        endArray(); // test_groups

        // Test max
        StringBuilder sb = new StringBuilder();
        int[] arr = new int[1001];
        for (int i = 0; i < 1001; i++) {
            sb.append("a");
            arr[i] = i;
        }
        addResult("max_length_string", sb.toString());
        addArray("max_num_ints", arr);
    }
}
