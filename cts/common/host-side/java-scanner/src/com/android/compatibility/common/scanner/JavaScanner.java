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
import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Scans a source directory for java tests and outputs a list of test classes and methods.
 */
public class JavaScanner {

    static final String[] SOURCE_PATHS = {
        "./frameworks/base/core/java",
        "./frameworks/base/test-runner/src",
        "./external/junit/src",
        "./development/tools/hosttestlib/src",
        "./libcore/dalvik/src/main/java",
        "./common/device-side/util/src",
        "./common/host-side/tradefed/src",
        "./common/util/src"
    };
    static final String[] CLASS_PATHS = {
        "./prebuilts/misc/common/tradefed/tradefed-prebuilt.java",
        "./prebuilts/misc/common/ub-uiautomator/ub-uiautomator.java"
    };
    private final File mSourceDir;
    private final File mDocletDir;

    /**
     * @param sourceDir The directory holding the source to scan.
     * @param docletDir The directory holding the doclet (or its jar).
     */
    JavaScanner(File sourceDir, File docletDir) {
        this.mSourceDir = sourceDir;
        this.mDocletDir = docletDir;
    }

    int scan() throws Exception {
        final ArrayList<String> args = new ArrayList<String>();
        args.add("javadoc");
        args.add("-doclet");
        args.add("com.android.compatibility.common.scanner.JavaScannerDoclet");
        args.add("-sourcepath");
        args.add(getSourcePath(mSourceDir));
        args.add("-classpath");
        args.add(getClassPath());
        args.add("-docletpath");
        args.add(mDocletDir.toString());
        args.addAll(getSourceFiles(mSourceDir));

        // Dont want p to get blocked due to a full pipe.
        final Process p = new ProcessBuilder(args).redirectErrorStream(true).start();
        final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("suite:") ||
                    line.startsWith("case:") ||
                    line.startsWith("test:")) {
                    System.out.println(line);
                }
            }
        } finally {
          if (in != null) {
              in.close();
          }
        }

        return p.waitFor();
    }

    private static String getSourcePath(File sourceDir) {
        final ArrayList<String> sourcePath = new ArrayList<String>(Arrays.asList(SOURCE_PATHS));
        sourcePath.add(sourceDir.toString());
        return join(sourcePath, ":");
    }

    private static String getClassPath() {
        return join(Arrays.asList(CLASS_PATHS), ":");
    }

    private static ArrayList<String> getSourceFiles(File sourceDir) {
        final ArrayList<String> sourceFiles = new ArrayList<String>();
        final File[] files = sourceDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory() || pathname.toString().endsWith(".java");
            }
        });
        for (File f : files) {
            if (f.isDirectory()) {
                sourceFiles.addAll(getSourceFiles(f));
            } else {
                sourceFiles.add(f.toString());
            }
        }
        return sourceFiles;
    }

    private static String join(List<String> list, String delimiter) {
        final StringBuilder builder = new StringBuilder();
        for (String s : list) {
            builder.append(s);
            builder.append(delimiter);
        }
        // Adding the delimiter each time and then removing the last one at the end is more
        // efficient than doing a check in each iteration of the loop.
        return builder.substring(0, builder.length() - delimiter.length());
    }

    public static void main(String[] args) throws Exception {
        final HashMap<String, String> argsMap = KeyValueArgsParser.parse(args);
        final String sourcePath = argsMap.get("-s");
        final String docletPath = argsMap.get("-d");
        if (sourcePath == null || docletPath == null) {
            usage(args);
        }
        System.exit(new JavaScanner(new File(sourcePath), new File(docletPath)).scan());
    }

    private static void usage(String[] args) {
        System.err.println("Arguments: " + Arrays.toString(args));
        System.err.println("Usage: javascanner -s SOURCE_DIR -d DOCLET_PATH");
        System.exit(1);
    }
}
