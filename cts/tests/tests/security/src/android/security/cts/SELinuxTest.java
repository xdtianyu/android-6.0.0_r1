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

import android.content.Context;
import android.content.res.AssetManager;
import android.test.AndroidTestCase;

import junit.framework.TestCase;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

/**
 * Verify that the SELinux configuration is sane.
 */
public class SELinuxTest extends AndroidTestCase {

    static {
        System.loadLibrary("ctssecurity_jni");
    }

    public void testMyJni() {
        try {
            checkSELinuxAccess(null, null, null, null, null);
            fail("checkSELinuxAccess should have thrown");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            checkSELinuxContext(null);
            fail("checkSELinuxContext should have thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testCheckAccessSane() {
        assertFalse(checkSELinuxAccess("a", "b", "c", "d", "e"));
    }

    public void testCheckContextSane() {
        assertFalse(checkSELinuxContext("a"));
    }

    public void testZygoteContext() {
        assertTrue(checkSELinuxContext("u:r:zygote:s0"));
    }

    public void testZygote() {
        assertFalse(checkSELinuxAccess("u:r:zygote:s0", "u:object_r:runas_exec:s0", "file", "getattr", "/system/bin/run-as"));
    }

    public void testNoBooleans() throws Exception {
        // Intentionally not using JNI bindings to keep things simple
        File[] files = new File("/sys/fs/selinux/booleans/").listFiles();
        assertEquals(0, files.length);
    }

    public void testCTSIsUntrustedApp() throws IOException {
        String found = KernelSettingsTest.getFile("/proc/self/attr/current");
        String expected = "u:r:untrusted_app:s0";
        String msg = "Expected prefix context: \"" + expected + "\"" +
                        ", Found: \"" + found + "\"";
        assertTrue(msg, found.startsWith(expected));
    }

    public void testCTSAppDataContext() throws Exception {
        File appDataDir = getContext().getFilesDir();
        String found = getFileContext(appDataDir.getAbsolutePath());
        String expected = "u:object_r:app_data_file:s0";
        String msg = "Expected prefix context: \"" + expected + "\"" +
                        ", Found: \"" + found + "\"";
        assertTrue(msg, found.startsWith(expected));
    }

    public void testFileContexts() throws Exception {
        assertEquals(getFileContext("/"), "u:object_r:rootfs:s0");
        assertEquals(getFileContext("/dev"), "u:object_r:device:s0");
        assertEquals(getFileContext("/dev/socket"), "u:object_r:socket_device:s0");
        assertEquals(getFileContext("/dev/binder"), "u:object_r:binder_device:s0");
        assertEquals(getFileContext("/system"), "u:object_r:system_file:s0");
        assertEquals(getFileContext("/system/bin/app_process"), "u:object_r:zygote_exec:s0");
        assertEquals(getFileContext("/data"), "u:object_r:system_data_file:s0");
        assertEquals(getFileContext("/data/app"), "u:object_r:apk_data_file:s0");
        assertEquals(getFileContext("/data/local/tmp"), "u:object_r:shell_data_file:s0");
        assertEquals(getFileContext("/cache"), "u:object_r:cache_file:s0");
        assertEquals(getFileContext("/sys"), "u:object_r:sysfs:s0");
    }

    private static native boolean checkSELinuxAccess(String scon, String tcon, String tclass, String perm, String extra);

    private static native boolean checkSELinuxContext(String con);

    private static final native String getFileContext(String path);
}
