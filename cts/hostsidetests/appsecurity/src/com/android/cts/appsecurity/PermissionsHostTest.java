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

package com.android.cts.appsecurity;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IAbi;
import com.android.tradefed.testtype.IAbiReceiver;
import com.android.tradefed.testtype.IBuildReceiver;

/**
 * Set of tests that verify behavior of runtime permissions, including both
 * dynamic granting and behavior of legacy apps.
 */
public class PermissionsHostTest extends DeviceTestCase implements IAbiReceiver, IBuildReceiver {
    private static final String PKG = "com.android.cts.usepermission";

    private static final String APK = "CtsUsePermissionApp.apk";
    private static final String APK_COMPAT = "CtsUsePermissionAppCompat.apk";

    private IAbi mAbi;
    private CtsBuildHelper mCtsBuild;

    @Override
    public void setAbi(IAbi abi) {
        mAbi = abi;
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        assertNotNull(mAbi);
        assertNotNull(mCtsBuild);

        getDevice().uninstallPackage(PKG);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        getDevice().uninstallPackage(PKG);
    }

    public void testFail() throws Exception {
        // Sanity check that remote failure is host failure
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK), false, false));
        try {
            runDeviceTests(PKG, ".UsePermissionTest", "testFail");
            fail("Expected remote failure");
        } catch (AssertionError expected) {
        }
    }

    public void testKill() throws Exception {
        // Sanity check that remote kill is host failure
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK), false, false));
        try {
            runDeviceTests(PKG, ".UsePermissionTest", "testKill");
            fail("Expected remote failure");
        } catch (AssertionError expected) {
        }
    }

    public void testDefault() throws Exception {
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK), false, false));
        runDeviceTests(PKG, ".UsePermissionTest", "testDefault");
    }

    public void testGranted() throws Exception {
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK), false, false));
        grantPermission(PKG, "android.permission.READ_EXTERNAL_STORAGE");
        grantPermission(PKG, "android.permission.WRITE_EXTERNAL_STORAGE");
        runDeviceTests(PKG, ".UsePermissionTest", "testGranted");
    }

    public void testInteractiveGrant() throws Exception {
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK), false, false));
        runDeviceTests(PKG, ".UsePermissionTest", "testInteractiveGrant");
    }

    public void testRuntimeGroupGrantSpecificity() throws Exception {
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK), false, false));
        runDeviceTests(PKG, ".UsePermissionTest", "testRuntimeGroupGrantSpecificity");
    }

    public void testRuntimeGroupGrantExpansion() throws Exception {
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK), false, false));
        runDeviceTests(PKG, ".UsePermissionTest", "testRuntimeGroupGrantExpansion");
    }

    public void testCompatDefault() throws Exception {
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK_COMPAT), false, false));
        runDeviceTests(PKG, ".UsePermissionCompatTest", "testCompatDefault");
    }

    public void testCompatRevoked() throws Exception {
        assertNull(getDevice().installPackage(mCtsBuild.getTestApp(APK_COMPAT), false, false));
        setAppOps(PKG, "android:read_external_storage", "deny");
        setAppOps(PKG, "android:write_external_storage", "deny");
        runDeviceTests(PKG, ".UsePermissionCompatTest", "testCompatRevoked");
    }

    private void runDeviceTests(String packageName, String testClassName, String testMethodName)
            throws DeviceNotAvailableException {
        Utils.runDeviceTests(getDevice(), packageName, testClassName, testMethodName);
    }

    private void grantPermission(String pkg, String permission) throws Exception {
        assertEmpty(getDevice().executeShellCommand("pm grant " + pkg + " " + permission));
    }

    private void revokePermission(String pkg, String permission) throws Exception {
        assertEmpty(getDevice().executeShellCommand("pm revoke " + pkg + " " + permission));
    }

    private void setAppOps(String pkg, String op, String mode) throws Exception {
        assertEmpty(getDevice().executeShellCommand("appops set " + pkg + " " + op + " " + mode));
    }

    private static void assertEmpty(String str) {
        if (str == null || str.length() == 0) {
            return;
        } else {
            fail("Expected empty string but found " + str);
        }
    }
}
