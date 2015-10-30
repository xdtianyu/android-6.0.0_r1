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

package com.android.tv.settings.device.apps;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Handles uninstalling of an application.
 */
class UninstallManager {

    private final Activity mActivity;
    private final AppInfo mAppInfo;
    private static Signature[] sSystemSignature;

    UninstallManager(Activity activity, AppInfo appInfo) {
        mActivity = activity;
        mAppInfo = appInfo;
    }

    boolean canUninstall() {
        return !mAppInfo.isUpdatedSystemApp() && !mAppInfo.isSystemApp();
    }

    boolean isEnabled() {
        return mAppInfo.isEnabled();
    }

    private static boolean signaturesMatch(PackageManager pm, String pkg1, String pkg2) {
        if (pkg1 != null && pkg2 != null) {
            try {
                final int match = pm.checkSignatures(pkg1, pkg2);
                if (match >= PackageManager.SIGNATURE_MATCH) {
                    return true;
                }
            } catch (Exception e) {
                // e.g. named alternate package not found during lookup;
                // this is an expected case sometimes
            }
        }
        return false;
    }

    private HashSet<String> getHomePackages(PackageManager pm) {
        HashSet<String> homePackages = new HashSet<String>();
        // Get list of "home" apps and trace through any meta-data references
        List<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        pm.getHomeActivities(homeActivities);
        for (int i = 0; i < homeActivities.size(); i++) {
            ResolveInfo ri = homeActivities.get(i);
            final String activityPkg = ri.activityInfo.packageName;
            homePackages.add(activityPkg);
            // Also make sure to include anything proxying for the home app
            final Bundle metadata = ri.activityInfo.metaData;
            if (metadata != null) {
                final String metaPkg = metadata.getString(ActivityManager.META_HOME_ALTERNATE);
                if (signaturesMatch(pm, metaPkg, activityPkg)) {
                    homePackages.add(metaPkg);
                }
            }
        }
        return homePackages;
    }

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg != null && pkg.signatures != null && pkg.signatures.length > 0) {
            return pkg.signatures[0];
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            final PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return getFirstSignature(sys);
        } catch (NameNotFoundException e) {
        }
        return null;
    }

    /**
     * Determine whether a package is a "system package", in which case certain things (like
     * disabling notifications or disabling the package altogether) should be disallowed.
     */
    public static boolean isSystemPackage(PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{ getSystemSignature(pm) };
        }
        return sSystemSignature[0] != null && sSystemSignature[0].equals(getFirstSignature(pkg));
    }

    boolean canDisable() {
        final PackageManager pm = mActivity.getPackageManager();
        final HashSet<String> homePackages = getHomePackages(pm);
        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(mAppInfo.getPackageName(),
                    PackageManager.GET_DISABLED_COMPONENTS |
                    PackageManager.GET_UNINSTALLED_PACKAGES |
                    PackageManager.GET_SIGNATURES);
        } catch (NameNotFoundException e) {
            return false;
        }
        return ! (homePackages.contains(mAppInfo.getPackageName()) ||
                isSystemPackage(pm, packageInfo));
    }

    boolean canUninstallUpdates() {
        return mAppInfo.isUpdatedSystemApp();
    }

    void uninstallUpdates(int requestId) {
        if (canUninstallUpdates()) {
            uninstallPackage(true, requestId);
        }
    }

    void uninstall(int requestId) {
        if (canUninstall()) {
            uninstallPackage(!mAppInfo.isInstalled(), requestId);
        }
    }

    private void uninstallPackage(boolean allUsers, int requestId) {
        Uri packageURI = Uri.parse("package:" + mAppInfo.getPackageName());
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, allUsers);
        uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        uninstallIntent.putExtra(Intent.EXTRA_KEY_CONFIRM, true);
        mActivity.startActivityForResult(uninstallIntent, requestId);
    }
}
