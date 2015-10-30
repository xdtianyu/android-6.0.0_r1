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

import com.android.compatibility.common.util.KeyValueArgsParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import vogar.ExpectationStore;
import vogar.ModeId;
import vogar.Result;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Passes the scanner output and outputs an xml description of the tests.
 */
public class XmlPlanGenerator {

    private final ExpectationStore mExpectations;
    private final String mAppNameSpace;
    private final String mAppPackageName;
    private final String mName;
    private final String mRunner;
    private final String mTargetBinaryName;
    private final String mTargetNameSpace;
    private final String mJarPath;
    private final String mTestType;
    private final String mOutput;

    private XmlPlanGenerator(ExpectationStore expectations, String appNameSpace,
            String appPackageName, String name, String runner, String targetBinaryName,
            String targetNameSpace, String jarPath, String testType, String output) {
        mExpectations = expectations;
        mAppNameSpace = appNameSpace;
        mAppPackageName = appPackageName;
        mName = name;
        mRunner = runner;
        mTargetBinaryName = targetBinaryName;
        mTargetNameSpace = targetNameSpace;
        mJarPath = jarPath;
        mTestType = testType;
        mOutput = output;
    }

    private void writePackageXml() throws IOException {
        OutputStream out = System.out;
        if (mOutput != null) {
            out = new FileOutputStream(mOutput);
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(out);
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writeTestPackage(writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void writeTestPackage(PrintWriter writer) {
        writer.append("<TestPackage");
        if (mAppNameSpace != null) {
            writer.append(" appNameSpace=\"").append(mAppNameSpace).append("\"");
        }

        writer.append(" appPackageName=\"").append(mAppPackageName).append("\"");
        writer.append(" name=\"").append(mName).append("\"");

        if (mRunner != null) {
            writer.append(" runner=\"").append(mRunner).append("\"");
        }

        if (mAppNameSpace != null && mTargetNameSpace != null
                && !mAppNameSpace.equals(mTargetNameSpace)) {
            writer.append(" targetBinaryName=\"").append(mTargetBinaryName).append("\"");
            writer.append(" targetNameSpace=\"").append(mTargetNameSpace).append("\"");
        }

        if (mTestType != null && !mTestType.isEmpty()) {
            writer.append(" testType=\"").append(mTestType).append("\"");
        }

        if (mJarPath != null) {
            writer.append(" jarPath=\"").append(mJarPath).append("\"");
        }

        writer.println(" version=\"1.0\">");

        final HashMap<String, TestSuite> suites = TestListParser.parse(System.in);
        if (suites.isEmpty()) {
            throw new RuntimeException("No TestSuites Found");
        }
        writeTestSuites(writer, suites, "");
        writer.println("</TestPackage>");
    }

    private void writeTestSuites(PrintWriter writer, HashMap<String, TestSuite> suites, String name) {
        for (String suiteName : suites.keySet()) {
            final TestSuite suite = suites.get(suiteName);
            writer.append("<TestSuite name=\"").append(suiteName).println("\">");
            final String fullname = name + suiteName + ".";
            writeTestSuites(writer, suite.getTestSuites(), fullname);
            writeTestCases(writer, suite.getTestCases(), fullname);
            writer.println("</TestSuite>");
        }
    }

    private void writeTestCases(PrintWriter writer, ArrayList<TestCase> cases, String name) {
        for (TestCase testCase : cases) {
            final String caseName = testCase.getName();
            writer.append("<TestCase name=\"").append(caseName).println("\">");
            final String fullname = name + caseName;
            writeTests(writer, testCase.getTests(), fullname);
            writer.println("</TestCase>");
        }
    }

    private void writeTests(PrintWriter writer, ArrayList<Test> tests, String name) {
        if (tests.isEmpty()) {
            throw new RuntimeException("No Tests Found");
        }
        for (Test test : tests) {
            final String testName = test.getName();
            writer.append("<Test name=\"").append(testName).append("\"");
            final String fullname = name + "#" + testName;
            if (isKnownFailure(mExpectations, fullname)) {
                writer.append(" expectation=\"failure\"");
            }
            writer.println(" />");
        }
    }

    public static boolean isKnownFailure(ExpectationStore store, String fullname) {
        return store != null && store.get(fullname).getResult() != Result.SUCCESS;
    }

    public static void main(String[] args) throws Exception {
        final HashMap<String, String> argsMap = KeyValueArgsParser.parse(args);
        final String packageName = argsMap.get("-p");
        final String name = argsMap.get("-n");
        final String testType = argsMap.get("-t");
        final String jarPath = argsMap.get("-j");
        final String instrumentation = argsMap.get("-i");
        final String manifest = argsMap.get("-m");
        final String expectations = argsMap.get("-e");
        final String output = argsMap.get("-o");
        String appNameSpace = argsMap.get("-a");
        String targetNameSpace = argsMap.get("-r");
        if (packageName == null || name == null) {
            usage(args);
        }
        String runner = null;
        if (manifest != null) {
            Document m = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest);
            Element elem = m.getDocumentElement();
            appNameSpace = elem.getAttribute("package");
            runner = getElementAttribute(elem, "instrumentation", "android:name");
            targetNameSpace = getElementAttribute(elem, "instrumentation", "android:targetPackage");
        }

        final HashSet<File> expectationFiles = new HashSet<File>();
        if (expectations != null) {
            expectationFiles.add(new File(expectations));
        }
        final ExpectationStore store = ExpectationStore.parse(expectationFiles, ModeId.DEVICE);
        XmlPlanGenerator generator = new XmlPlanGenerator(store, appNameSpace, packageName, name,
            runner, instrumentation, targetNameSpace, jarPath, testType, output);
        generator.writePackageXml();
    }

    private static String getElementAttribute(Element parent, String elem, String name) {
        NodeList nodeList = parent.getElementsByTagName(elem);
        if (nodeList.getLength() > 0) {
             Element element = (Element) nodeList.item(0);
             return element.getAttribute(name);
        }
        return null;
    }

    private static void usage(String[] args) {
        System.err.println("Arguments: " + Arrays.toString(args));
        System.err.println("Usage: compatibility-xml-plan-generator -p PACKAGE_NAME -n NAME" +
            "[-t TEST_TYPE] [-j JAR_PATH] [-i INSTRUMENTATION] [-m MANIFEST] [-e EXPECTATIONS]" +
            "[-o OUTPUT]");
        System.exit(1);
    }
}
