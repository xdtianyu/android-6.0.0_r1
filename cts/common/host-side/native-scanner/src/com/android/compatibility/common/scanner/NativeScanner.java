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

package com.android.compatibility.common.scanner;

import com.android.compatibility.common.util.KeyValueArgsParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Passes the gtest output and outputs a list of test classes and methods.
 */
public final class NativeScanner {

    private static final String TEST_SUITE_ARG = "t";
    private static final String USAGE = "Usage: compatibility-native-scanner -t TEST_SUITE"
        + "  This code reads from stdin the list of tests."
        + "  The format expected:"
        + "    TEST_CASE_NAME."
        + "      TEST_NAME";

    /**
     * @return An {@link ArrayList} of suites, classes and method names.
     */
    static ArrayList<String> getTestNames(BufferedReader reader, String testSuite)
            throws IOException {
        ArrayList<String> testNames = new ArrayList<String>();
        testNames.add("suite:" + testSuite);

        String testCaseName = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            if (line.charAt(0) == ' ') {
                if (testCaseName == null) {
                    throw new RuntimeException("TEST_CASE_NAME not defined before first test.");
                }
                testNames.add("test:" + line.trim());
            } else {
                testCaseName = line.trim();
                if (testCaseName.endsWith(".")) {
                    testCaseName = testCaseName.substring(0, testCaseName.length()-1);
                }
                testNames.add("case:" + testCaseName);
            }
        }
        return testNames;
    }

    /** Lookup test suite argument and scan {@code System.in} for test cases */
    public static void main(String[] args) throws IOException {
        HashMap<String, String> argMap = KeyValueArgsParser.parse(args);
        if (!argMap.containsKey(TEST_SUITE_ARG)) {
            System.err.println(USAGE);
            System.exit(1);
        }

        String testSuite = argMap.get(TEST_SUITE_ARG);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (String name : getTestNames(reader, testSuite)) {
            System.out.println(name);
        }
    }
}
