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

package android.media.cts;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

public class Utils {
    public static void enableAppOps(String packageName, String operation,
            Instrumentation instrumentation) {
        setAppOps(packageName, operation, instrumentation, true);
    }

    public static void disableAppOps(String packageName, String operation,
            Instrumentation instrumentation) {
        setAppOps(packageName, operation, instrumentation, false);
    }

    public static String convertStreamToString(InputStream is) {
        try (Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static void setAppOps(String packageName, String operation,
            Instrumentation instrumentation, boolean enable) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("appops set ");
        cmd.append(packageName);
        cmd.append(" ");
        cmd.append(operation);
        cmd.append(enable ? " allow" : " deny");
        instrumentation.getUiAutomation().executeShellCommand(cmd.toString());

        StringBuilder query = new StringBuilder();
        query.append("appops get ");
        query.append(packageName);
        query.append(" ");
        query.append(operation);
        String queryStr = query.toString();

        String expectedResult = enable ? "allow" : "deny";
        String result = "";
        while(!result.contains(expectedResult)) {
            ParcelFileDescriptor pfd = instrumentation.getUiAutomation().executeShellCommand(
                                                            queryStr);
            InputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
            result = convertStreamToString(inputStream);
        }
    }
}
