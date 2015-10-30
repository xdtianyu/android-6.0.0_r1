/*
 * Copyright (C) 2010 The Android Open Source Project
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

package vogar.target;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import vogar.util.Threads;

/**
 * Runs a user-supplied {@code main(String[] args)} method in the context of an
 * Android activity. The result of the method (success or exception) is reported
 * to a file where vogar can pick it up.
 */
public class TestActivity extends Activity {

    private final static String TAG = "TestActivity";

    private TextView view;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.view = new TextView(this);
        log("TestActivity starting...");
        setContentView(view);

        AndroidLog log = new AndroidLog(TAG);
        ExecutorService executor = Threads.fixedThreadsExecutor(log, "testactivity", 1);
        executor.execute(new Runnable() {
            public void run() {
                try {
                    TestRunner testRunner = new TestRunner(Collections.<String>emptyList());
                    testRunner.useSocketMonitor();
                    testRunner.run();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        executor.shutdown();
    }

    private void log(String message) {
        Log.i(TAG, message);
        view.append(message + "\n");
    }
}
