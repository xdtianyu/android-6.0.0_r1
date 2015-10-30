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

package android.security.cts;

import junit.framework.TestCase;

public class AudioPolicyBinderTest extends TestCase {

    static {
        System.loadLibrary("ctssecurity_jni");
    }

    /**
     * Checks that IAudioPolicyService::startOutput() cannot be called with an
     * invalid stream type.
     */
    public void test_startOutput() throws Exception {
        assertTrue(native_test_startOutput());
    }

    /**
     * Checks that IAudioPolicyService::stopOutput() cannot be called with an
     * invalid stream type.
     */
    public void test_stopOutput() throws Exception {
        assertTrue(native_test_stopOutput());
    }

    /**
     * Checks that IAudioPolicyService::isStreamActive() cannot be called with an
     * invalid stream type.
     */
    public void test_isStreamActive() throws Exception {
        assertTrue(native_test_isStreamActive());
    }

    /**
     * Checks that IAudioPolicyService::isStreamActiveRemotely() cannot be called with an
     * invalid stream type.
     */
    public void test_isStreamActiveRemotely() throws Exception {
        assertTrue(native_test_isStreamActiveRemotely());
    }

    private static native boolean native_test_startOutput();
    private static native boolean native_test_stopOutput();
    private static native boolean native_test_isStreamActive();
    private static native boolean native_test_isStreamActiveRemotely();
}
