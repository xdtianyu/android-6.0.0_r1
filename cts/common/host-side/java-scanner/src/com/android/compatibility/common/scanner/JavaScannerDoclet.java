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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

import java.io.PrintWriter;

/**
 * Doclet that scans java files looking for tests.
 *
 * Sample Ouput;
 * suite:com.android.sample.cts
 * case:SampleDeviceTest
 * test:testSharedPreferences
 */
public class JavaScannerDoclet extends Doclet {

    private static final String JUNIT_TEST_CASE_CLASS_NAME = "junit.framework.testcase";

    public static boolean start(RootDoc root) {
        ClassDoc[] classes = root.classes();
        if (classes == null) {
            return false;
        }

        PrintWriter writer = new PrintWriter(System.out);

        for (ClassDoc clazz : classes) {
            if (clazz.isAbstract() || !isValidJUnitTestCase(clazz)) {
                continue;
            }
            writer.append("suite:").println(clazz.containingPackage().name());
            writer.append("case:").println(clazz.name());
            for (; clazz != null; clazz = clazz.superclass()) {
                for (MethodDoc method : clazz.methods()) {
                    if (method.name().startsWith("test")) {
                        writer.append("test:").println(method.name());
                    }
                }
            }
        }

        writer.close();
        return true;
    }

    private static boolean isValidJUnitTestCase(ClassDoc clazz) {
        while ((clazz = clazz.superclass()) != null) {
            if (JUNIT_TEST_CASE_CLASS_NAME.equals(clazz.qualifiedName().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
