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
package android.graphics.cts;

import android.cts.util.FileUtils;

import junit.framework.TestCase;

import java.io.File;

public class VulkanReservedTest extends TestCase {

    /**
     * Assert that file with given path does not exist.
     */
    private static void assertNoFile(String filename) {
        assertFalse(filename + " must not exist", new File(filename).exists());
    }

    /**
     * Test that no vendor ships libvulkan.so before ratification and
     * appropriate CTS coverage.
     */
    public void testNoVulkan() {
        assertNoFile("/system/lib/libvulkan.so");
        assertNoFile("/system/lib64/libvulkan.so");
        assertNoFile("/vendor/lib/libvulkan.so");
        assertNoFile("/vendor/lib64/libvulkan.so");
    }
}
