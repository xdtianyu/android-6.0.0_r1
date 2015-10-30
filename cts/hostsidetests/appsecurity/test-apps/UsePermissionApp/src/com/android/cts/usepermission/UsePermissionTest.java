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
import static com.android.cts.externalstorageapp.CommonExternalStorageTest.logCommand;

import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.test.InstrumentationTestCase;

public class UsePermissionTest extends InstrumentationTestCase {
    private static final String TAG = "UsePermissionTest";

    private UiDevice mDevice;
    private MyActivity mActivity;

    public void testFail() throws Exception {
        fail("Expected");
    }

    public void testKill() throws Exception {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void testDefault() throws Exception {
        logCommand("/system/bin/cat", "/proc/self/mountinfo");

        // New permission model is denied by default
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE));
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
        assertDirNoAccess(Environment.getExternalStorageDirectory());
        assertDirReadWriteAccess(getInstrumentation().getContext().getExternalCacheDir());
        assertMediaNoAccess(getInstrumentation().getContext().getContentResolver());
    }

    public void testGranted() throws Exception {
        logCommand("/system/bin/cat", "/proc/self/mountinfo");

        assertEquals(PackageManager.PERMISSION_GRANTED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE));
        assertEquals(PackageManager.PERMISSION_GRANTED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
        assertDirReadWriteAccess(Environment.getExternalStorageDirectory());
        assertDirReadWriteAccess(getInstrumentation().getContext().getExternalCacheDir());
        assertMediaReadWriteAccess(getInstrumentation().getContext().getContentResolver());
    }

    public void testInteractiveGrant() throws Exception {
        logCommand("/system/bin/cat", "/proc/self/mountinfo");

        // Start out without permission
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE));
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
        assertDirNoAccess(Environment.getExternalStorageDirectory());
        assertDirReadWriteAccess(getInstrumentation().getContext().getExternalCacheDir());
        assertMediaNoAccess(getInstrumentation().getContext().getContentResolver());

        // Go through normal grant flow
        mDevice = UiDevice.getInstance(getInstrumentation());
        mActivity = launchActivity(getInstrumentation().getTargetContext().getPackageName(),
                MyActivity.class, null);
        mDevice.waitForIdle();

        mActivity.requestPermissions(new String[] {
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE }, 42);
        mDevice.waitForIdle();

        new UiObject(new UiSelector()
                .resourceId("com.android.packageinstaller:id/permission_allow_button")).click();
        mDevice.waitForIdle();

        MyActivity.Result result = mActivity.getResult();
        assertEquals(42, result.requestCode);
        assertEquals(android.Manifest.permission.READ_EXTERNAL_STORAGE, result.permissions[0]);
        assertEquals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, result.permissions[1]);
        assertEquals(PackageManager.PERMISSION_GRANTED, result.grantResults[0]);
        assertEquals(PackageManager.PERMISSION_GRANTED, result.grantResults[1]);

        logCommand("/system/bin/cat", "/proc/self/mountinfo");

        // We should have permission now!
        assertEquals(PackageManager.PERMISSION_GRANTED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE));
        assertEquals(PackageManager.PERMISSION_GRANTED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
        assertEquals(Environment.MEDIA_MOUNTED, Environment.getExternalStorageState());
        assertDirReadWriteAccess(Environment.getExternalStorageDirectory());
        assertDirReadWriteAccess(getInstrumentation().getContext().getExternalCacheDir());
        assertMediaReadWriteAccess(getInstrumentation().getContext().getContentResolver());

        mActivity.finish();
    }

    public void testRuntimeGroupGrantSpecificity() throws Exception {
        // Start out without permission
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS));
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.READ_CONTACTS));

        // Go through normal grant flow
        mDevice = UiDevice.getInstance(getInstrumentation());
        mActivity = launchActivity(getInstrumentation().getTargetContext().getPackageName(),
                MyActivity.class, null);
        mDevice.waitForIdle();

        // request only one permission from the 'contacts' permission group
        mActivity.requestPermissions(new String[] {
                android.Manifest.permission.WRITE_CONTACTS }, 43);
        mDevice.waitForIdle();

        new UiObject(new UiSelector()
                .resourceId("com.android.packageinstaller:id/permission_allow_button")).click();
        mDevice.waitForIdle();

        MyActivity.Result result = mActivity.getResult();
        assertEquals(43, result.requestCode);
        assertEquals(android.Manifest.permission.WRITE_CONTACTS, result.permissions[0]);
        assertEquals(PackageManager.PERMISSION_GRANTED, result.grantResults[0]);

        // We should have only the explicitly requested permission from this group
        assertEquals(PackageManager.PERMISSION_GRANTED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS));
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.READ_CONTACTS));

        mActivity.finish();
    }

    public void testRuntimeGroupGrantExpansion() throws Exception {
        // Start out without permission
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.RECEIVE_SMS));
        assertEquals(PackageManager.PERMISSION_DENIED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.SEND_SMS));

        // Go through normal grant flow
        mDevice = UiDevice.getInstance(getInstrumentation());
        mActivity = launchActivity(getInstrumentation().getTargetContext().getPackageName(),
                MyActivity.class, null);
        mDevice.waitForIdle();

        // request only one permission from the 'SMS' permission group at runtime,
        // but two from this group are <uses-permission> in the manifest
        mActivity.requestPermissions(new String[] {
                android.Manifest.permission.RECEIVE_SMS }, 44);
        mDevice.waitForIdle();

        new UiObject(new UiSelector()
                .resourceId("com.android.packageinstaller:id/permission_allow_button")).click();
        mDevice.waitForIdle();

        MyActivity.Result result = mActivity.getResult();
        assertEquals(44, result.requestCode);
        assertEquals(android.Manifest.permission.RECEIVE_SMS, result.permissions[0]);
        assertEquals(PackageManager.PERMISSION_GRANTED, result.grantResults[0]);

        // We should now have been granted both of the permissions from this group
        // that are mentioned in our manifest
        assertEquals(PackageManager.PERMISSION_GRANTED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.RECEIVE_SMS));
        assertEquals(PackageManager.PERMISSION_GRANTED, getInstrumentation().getContext()
                .checkSelfPermission(android.Manifest.permission.SEND_SMS));

        mActivity.finish();
    }
}
