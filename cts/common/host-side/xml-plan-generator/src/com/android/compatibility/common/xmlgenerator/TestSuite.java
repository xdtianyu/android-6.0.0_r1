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

package com.android.compatibility.common.xmlgenerator;

import java.util.ArrayList;
import java.util.HashMap;

public class TestSuite {

    private final String mName;
    private final HashMap<String, TestSuite> mTestSuites = new HashMap<String, TestSuite>();
    private final ArrayList<TestCase> mTestCases = new ArrayList<TestCase>();

    public TestSuite(String name) {
        mName = name;
    }

    public boolean hasTestSuite(String name) {
        return mTestSuites.containsKey(name);
    }

    public TestSuite getTestSuite(String name) {
        return mTestSuites.get(name);
    }

    public void addTestSuite(TestSuite testSuite) {
        mTestSuites.put(testSuite.getName(), testSuite);
    }

    public void addTestCase(TestCase testCase) {
        mTestCases.add(testCase);
    }

    public String getName() {
        return mName;
    }

    public HashMap<String, TestSuite> getTestSuites() {
        return mTestSuites;
    }

    public ArrayList<TestCase> getTestCases() {
        return mTestCases;
    }
}
