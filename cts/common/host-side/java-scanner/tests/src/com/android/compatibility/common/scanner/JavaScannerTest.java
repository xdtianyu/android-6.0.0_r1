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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import junit.framework.TestCase;

public class JavaScannerTest extends TestCase {

    private static final String JAR = "out/host/linux-x86/framework/compatibility-java-scanner_v2.jar";
    private static final String VALID_RESULT =
        "suite:com.android.test" +
        "case:ValidTest" +
        "test:testA";

    private static final String VALID_FILENAME = "ValidTest";
    private static final String VALID =
        "package com.android.test;" +
        "import junit.framework.TestCase;" +
        "public class ValidTest extends TestCase {" +
        "  public void testA() throws Exception {" +
        "    helper();" +
        "  }" +
        "  public void helper() {" +
        "    fail();" +
        "  }" +
        "}";

    // TestCases must have TestCase in their hierarchy
    private static final String INVALID_A_FILENAME = "NotTestCase";
    private static final String INVALID_A =
        "package com.android.test;" +
        "public class NotTestCase {" +
        "  public void testA() throws Exception {" +
        "    helper();" +
        "  }" +
        "  public void helper() {" +
        "    fail();" +
        "  }" +
        "}";

    // TestCases cant be abstract classes
    private static final String INVALID_B_FILENAME = "AbstractClass";
    private static final String INVALID_B =
        "package com.android.test;" +
        "import junit.framework.TestCase;" +
        "public abstract class AbstractClass extends TestCase {" +
        "  public void testA() throws Exception {" +
        "    helper();" +
        "  }" +
        "  public void helper() {" +
        "    fail();" +
        "  }" +
        "}";

    public void testValidFile() throws Exception {
        String result = runScanner(VALID_FILENAME, VALID);
        assertEquals(VALID_RESULT, result);
    }

    public void testInvalidFileA() throws Exception {
        assertEquals("", runScanner(INVALID_A_FILENAME, INVALID_A));
    }

    public void testInvalidFileB() throws Exception {
        assertEquals("", runScanner(INVALID_B_FILENAME, INVALID_B));
    }

    private static String runScanner(String filename, String content) throws Exception {
        final File parent0 = new File(System.getProperty("java.io.tmpdir"));
        final File parent1 = new File(parent0, "tmp" + System.currentTimeMillis());
        final File parent2 = new File(parent1, "com");
        final File parent3 = new File(parent2, "android");
        final File parent4 = new File(parent3, "test");
        File f = null;
        try {
            parent4.mkdirs();
            f = new File(parent4, filename + ".java");
            final PrintWriter out = new PrintWriter(f);
            out.print(content);
            out.flush();
            out.close();
            ArrayList<String> args = new ArrayList<String>();
            args.add("java");
            args.add("-jar");
            args.add(JAR);
            args.add("-s");
            args.add(parent1.toString());
            args.add("-d");
            args.add(JAR);

            final Process p = new ProcessBuilder(args).start();
            final StringBuilder output = new StringBuilder();
            final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                output.append(line);
            }
            int ret = p.waitFor();
            if (ret == 0) {
                return output.toString();
            }
        } finally {
            if (f != null) {
                f.delete();
            }
            parent4.delete();
            parent3.delete();
            parent2.delete();
            parent1.delete();
        }
        return null;
    }
}
