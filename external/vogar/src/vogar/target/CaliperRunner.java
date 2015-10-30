/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.common.collect.ObjectArrays;
import java.util.concurrent.atomic.AtomicReference;
import vogar.Result;
import vogar.monitor.TargetMonitor;

/**
 * Runs a <a href="http://code.google.com/p/caliper/">Caliper</a> benchmark.
 */
public final class CaliperRunner implements vogar.target.Runner {

    private TargetMonitor monitor;
    private boolean profile;
    private Class<?> testClass;

    public void init(TargetMonitor monitor, String actionName, String qualification,
            Class<?> testClass, AtomicReference<String> skipPastReference,
            TestEnvironment testEnvironment, int timeoutSeconds, boolean profile) {
        this.monitor = monitor;
        this.profile = profile;
        this.testClass = testClass;
    }

    public boolean run(String actionName, Profiler profiler,
            String[] args) {
        monitor.outcomeStarted(this, testClass.getName(), actionName);
        String[] arguments = ObjectArrays.concat(testClass.getName(), args);
        if (profile) {
            arguments = ObjectArrays.concat("--debug", arguments);
        }
        try {
            if (profiler != null) {
                profiler.start();
            }
            new Runner().run(arguments);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (profiler != null) {
                profiler.stop();
            }
        }
        monitor.outcomeFinished(Result.SUCCESS);
        return true;
    }

    public boolean supports(Class<?> klass) {
        return SimpleBenchmark.class.isAssignableFrom(klass);
    }
}
