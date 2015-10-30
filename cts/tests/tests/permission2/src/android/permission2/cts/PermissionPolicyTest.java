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

package android.permission2.cts;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.test.AndroidTestCase;
import android.text.TextUtils;
import android.util.ArraySet;

import java.util.Set;

/**
 * Tests for permission policy on the platform.
 */
public class PermissionPolicyTest extends AndroidTestCase {
    private static final String PLATFORM_PACKAGE_NAME = "android";

    private static final Set<String> PERMISSION_GROUPS = new ArraySet<>();
    static {
        PERMISSION_GROUPS.add(Manifest.permission_group.CALENDAR);
        PERMISSION_GROUPS.add(Manifest.permission_group.CAMERA);
        PERMISSION_GROUPS.add(Manifest.permission_group.CONTACTS);
        PERMISSION_GROUPS.add(Manifest.permission_group.LOCATION);
        PERMISSION_GROUPS.add(Manifest.permission_group.MICROPHONE);
        PERMISSION_GROUPS.add(Manifest.permission_group.PHONE);
        PERMISSION_GROUPS.add(Manifest.permission_group.SENSORS);
        PERMISSION_GROUPS.add(Manifest.permission_group.SMS);
        PERMISSION_GROUPS.add(Manifest.permission_group.STORAGE);
    }

    public void testPlatformDefinedRuntimePermissionValid() throws Exception {
        PackageManager packageManager = getContext().getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageInfo(PLATFORM_PACKAGE_NAME,
                PackageManager.GET_PERMISSIONS);
        for (PermissionInfo permission : packageInfo.permissions) {
            if ((permission.protectionLevel & PermissionInfo.PROTECTION_DANGEROUS) == 0) {
                continue;
            }
            assertTrue(permission.name + " must be in one of these groups: " + PERMISSION_GROUPS,
                    PERMISSION_GROUPS.contains(permission.group));
            assertFalse(permission.name + " must have non-empty label",
                    TextUtils.isEmpty(permission.loadLabel(packageManager)));
            assertFalse(permission.name + " must have non-empty description",
                    TextUtils.isEmpty(permission.loadDescription(packageManager)));
        }
    }
}
