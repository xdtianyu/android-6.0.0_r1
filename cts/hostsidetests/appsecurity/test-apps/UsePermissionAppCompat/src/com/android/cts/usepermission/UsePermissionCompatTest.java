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

package com.android.cts.usepermission;

import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirNoAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertDirReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertMediaNoAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.assertMediaReadWriteAccess;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.getAllPackageSpecificPaths;
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.logCommand;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Process;
import android.test.InstrumentationTestCase;

import java.io.File;

public class UsePermissionCompatTest extends InstrumentationTestCase {
    private static final String TAG = "UsePermissionTest";

    public void testCompatDefault() throws Exception {
        final Context context = getInstrumentation().getContext();
        logCommand("/system/bin/cat", "/proc/self/mountinfo");

        // Legacy permission model is granted by default
        assertEquals(PackageManager.PERMISSION_GRANTED,
                context.checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        Process.myPid(), Process.myUid()));
        assertEquals(PackageManager.PERMISSION_GRANTED,
                context.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Process.myPid(), Process.myUid()));
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
        assertDirReadWriteAccess(Environment.getExternalStorageDirectory());
        for (File path : getAllPackageSpecificPaths(context)) {
            if (path != null) {
                assertDirReadWriteAccess(path);
            }
        }
        assertMediaReadWriteAccess(getInstrumentation().getContext().getContentResolver());
    }

    public void testCompatRevoked() throws Exception {
        final Context context = getInstrumentation().getContext();
        logCommand("/system/bin/cat", "/proc/self/mountinfo");

        // Legacy permission model appears granted, but storage looks and
        // behaves like it's ejected
        assertEquals(PackageManager.PERMISSION_GRANTED,
                context.checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        Process.myPid(), Process.myUid()));
        assertEquals(PackageManager.PERMISSION_GRANTED,
                context.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Process.myPid(), Process.myUid()));
        assertEquals(Environment.MEDIA_UNMOUNTED, Environment.getExternalStorageState());
        assertDirNoAccess(Environment.getExternalStorageDirectory());
        for (File dir : getAllPackageSpecificPaths(context)) {
            if (dir != null) {
                assertDirNoAccess(dir);
            }
        }
        assertMediaNoAccess(getInstrumentation().getContext().getContentResolver());

        // Just to be sure, poke explicit path
        assertDirNoAccess(new File(Environment.getExternalStorageDirectory(),
                "/Android/data/" + getInstrumentation().getContext().getPackageName()));
    }
}
