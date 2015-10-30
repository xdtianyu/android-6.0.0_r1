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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Parser of test lists in the form;
 *
 * suite:android.sample
 * case:SampleTest
 * test:testA
 * test:testB
 * suite:android.sample.ui
 * case:SampleUiTest
 * test:testA
 * test:testB
 */
public class TestListParser {

    private TestListParser() {}

    public static HashMap<String, TestSuite> parse(InputStream input) {
        final HashMap<String, TestSuite> suites = new HashMap<String, TestSuite>();
        TestSuite currentSuite = null;
        TestCase currentCase = null;
        Scanner in = null;
        try {
            in = new Scanner(input);
            while (in.hasNextLine()) {
                final String line = in.nextLine();
                final String[] parts = line.split(":");
                if (parts.length != 2) {
                    throw new RuntimeException("Invalid Format: " + line);
                }
                final String key = parts[0];
                final String value = parts[1];
                if (currentSuite == null) {
                    if (!"suite".equals(key)) {
                        throw new RuntimeException("TestSuite Expected");
                    }
                    final String[] names = value.split("\\.");
                    for (int i = 0; i < names.length; i++) {
                        final String name = names[i];
                        if (currentSuite != null) {
                            if (currentSuite.hasTestSuite(name)) {
                                currentSuite = currentSuite.getTestSuite(name);
                            } else {
                                final TestSuite newSuite = new TestSuite(name);
                                currentSuite.addTestSuite(newSuite);
                                currentSuite = newSuite;
                            }
                        } else if (suites.containsKey(name)) {
                            currentSuite = suites.get(name);
                        } else {
                            currentSuite = new TestSuite(name);
                            suites.put(name, currentSuite);
                        }
                    }
                } else if (currentCase == null) {
                    if (!"case".equals(key)) {
                        throw new RuntimeException("TestCase Expected");
                    }
                    currentCase = new TestCase(value);
                    currentSuite.addTestCase(currentCase);
                } else {
                    if (!"test".equals(key)) {
                        throw new RuntimeException("Test Expected");
                    }
                    currentCase.addTest(new Test(value));
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return suites;
    }
}
