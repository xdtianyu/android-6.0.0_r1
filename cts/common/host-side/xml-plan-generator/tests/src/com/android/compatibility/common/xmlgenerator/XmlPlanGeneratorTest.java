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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class XmlPlanGeneratorTest extends TestCase {

    private static final String JAR = "out/host/linux-x86/framework/compatibility-xml-plan-generator_v2.jar";
    private static final String PACKAGE_NAME = "com.android.test";
    private static final String NAME = "ValidTest";
    private static final String VALID_RESULT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<TestPackage appPackageName=\"com.android.test\" name=\"ValidTest\" version=\"1.0\">" +
        "<TestSuite name=\"com\">" +
        "<TestSuite name=\"android\">" +
        "<TestSuite name=\"test\">" +
        "<TestCase name=\"ValidTest\">" +
        "<Test name=\"testA\" />" +
        "</TestCase>" +
        "</TestSuite>" +
        "</TestSuite>" +
        "</TestSuite>" +
        "</TestPackage>";

    private static final String VALID =
        "suite:com.android.test\n" +
        "case:ValidTest\n" +
        "test:testA\n";

    private static final String INVALID_A = "";

    private static final String INVALID_B =
        "suite:com.android.test\n" +
        "case:InvalidTest\n";

    private static final String INVALID_C =
        "uh oh";

    private static final String INVALID_D =
        "test:testA\n" +
        "case:InvalidTest\n" +
        "suite:com.android.test\n";

    private static final String INVALID_E =
        "suite:com.android.test\n" +
        "test:testA\n" +
        "case:InvalidTest\n";

    public void testValid() throws Exception {
        assertEquals(VALID_RESULT, runGenerator(VALID));
    }

    public void testInvalidA() throws Exception {
        assertNull(runGenerator(INVALID_A));
    }

    public void testInvalidB() throws Exception {
        assertNull(runGenerator(INVALID_B));
    }

    public void testTestListParserInvalidFormat() throws Exception {
        runTestListParser(INVALID_C);
    }

    public void testTestListParserSuiteExpected() throws Exception {
        runTestListParser(INVALID_D);
    }

    public void testTestListParserCaseExpected() throws Exception {
        runTestListParser(INVALID_E);
    }

    private static String runGenerator(String input) throws Exception {
        ArrayList<String> args = new ArrayList<String>();
        args.add("java");
        args.add("-jar");
        args.add(JAR);
        args.add("-p");
        args.add(PACKAGE_NAME);
        args.add("-n");
        args.add(NAME);

        final Process p = new ProcessBuilder(args).start();
        final PrintWriter out = new PrintWriter(p.getOutputStream());
        out.print(input);
        out.flush();
        out.close();
        final StringBuilder output = new StringBuilder();
        final Scanner in = new Scanner(p.getInputStream());
        while (in.hasNextLine()) {
            output.append(in.nextLine());
        }
        int ret = p.waitFor();
        if (ret == 0) {
            return output.toString();
        }
        return null;
    }

    private static void runTestListParser(String input) throws Exception {
        try {
            final ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
            final HashMap<String, TestSuite> suites = TestListParser.parse(in);
            fail();
        } catch (RuntimeException e) {}
    }
}
