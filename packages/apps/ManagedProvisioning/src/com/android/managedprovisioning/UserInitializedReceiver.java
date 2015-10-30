/*
 * Copyright 2015, The Android Open Source Project
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

package com.android.managedprovisioning;

import static android.app.admin.DeviceAdminReceiver.ACTION_READY_FOR_USER_INITIALIZATION;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_INITIALIZER_COMPONENT_NAME;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION;
import static android.Manifest.permission.BIND_DEVICE_ADMIN;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;

import java.util.List;

/**
 * On secondary user initialization, send a broadcast to the primary user to request CA certs.
 * Also, if this device has a Device Owner, send an intent to start managed provisioning.
  */
public class UserInitializedReceiver extends BroadcastReceiver {

    private static final String MP_PACKAGE_NAME = "com.android.managedprovisioning";
    private static final String MP_ACTIVITY_NAME =
            "com.android.managedprovisioning.DeviceOwnerPreProvisioningActivity";

    @Override
    public void onReceive(Context context, Intent receivedIntent) {
        ProvisionLogger.logi("User is initialized");
        DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!Utils.isCurrentUserOwner() && !Utils.isManagedProfile(context) &&
                Utils.hasDeviceInitializer(context)) {
            ProvisionLogger.logi("Initializing secondary user with a device initializer. " +
                    "Starting managed provisioning.");
            requestCACerts(context);
            launchManagedProvisioning(context);
        }
    }

    private void requestCACerts(Context context) {
        Intent intent = new Intent(InstallCertRequestReceiver.REQUEST_CERT_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(CertService.EXTRA_REQUESTING_USER, Process.myUserHandle());
        context.sendBroadcastAsUser(intent, UserHandle.OWNER);
    }

    /**
     * Construct an appropriate intent and launch managed provisioning for a secondary user.
     */
    private void launchManagedProvisioning(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        Intent startMpIntent = new Intent(context, DeviceOwnerPreProvisioningActivity.class);
        startMpIntent.setAction(DeviceOwnerPreProvisioningActivity.ACTION_PROVISION_SECONDARY_USER);
        startMpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startMpIntent.putExtra(
                EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME, dpm.getDeviceOwner());

        ComponentName diComponentName = getDeviceInitializerComponentName(
                dpm.getDeviceInitializerApp(), context);
        if (diComponentName != null) {
            startMpIntent.putExtra(
                    EXTRA_PROVISIONING_DEVICE_INITIALIZER_COMPONENT_NAME,
                    diComponentName);
        }

        // Rely on DPC to disable any system apps that need to be turned off
        startMpIntent.putExtra(EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true);

        // For secondary users, if the device needs to be encrypted, it has already happened
        startMpIntent.putExtra(EXTRA_PROVISIONING_SKIP_ENCRYPTION, true);
        ProvisionLogger.logd("Sending intent to start managed provisioning");
        context.startActivity(startMpIntent);
    }

    /**
     * Find the name of the device initializer component within the given package. It must be a
     * broadcast receiver with ACTION_READY_FOR_USER_INITIALIZATION and the BIND_DEVICE_OWNER
     * permission.
     * @param deviceInitializerPackageName The package to check
     * @return The ComponentName for the DI, or null if an appropriate component couldn't be found
     */
    private ComponentName getDeviceInitializerComponentName(String deviceInitializerPackageName,
            Context context) {

        if (!TextUtils.isEmpty(deviceInitializerPackageName)) {
            Intent findDeviceInitIntent = new Intent(ACTION_READY_FOR_USER_INITIALIZATION);
            findDeviceInitIntent.setPackage(deviceInitializerPackageName);

            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> results;
            results = pm.queryBroadcastReceivers(findDeviceInitIntent,
                    PackageManager.GET_DISABLED_COMPONENTS, UserHandle.USER_OWNER);

            for (ResolveInfo result : results) {
                if (result.activityInfo.permission != null &&
                        result.activityInfo.permission.equals(BIND_DEVICE_ADMIN)) {
                    return new ComponentName(
                            result.activityInfo.packageName, result.activityInfo.name);
                }
            }
        }
        return null;
    }
}
