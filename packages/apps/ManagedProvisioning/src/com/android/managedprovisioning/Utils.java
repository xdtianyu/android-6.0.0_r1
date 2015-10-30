/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.managedprovisioning;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Base64;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;

/**
 * Class containing various auxiliary methods.
 */
public class Utils {
    private Utils() {}

    public static Set<String> getCurrentSystemApps(int userId) {
        IPackageManager ipm = IPackageManager.Stub.asInterface(ServiceManager
                .getService("package"));
        Set<String> apps = new HashSet<String>();
        List<ApplicationInfo> aInfos = null;
        try {
            aInfos = ipm.getInstalledApplications(
                    PackageManager.GET_UNINSTALLED_PACKAGES, userId).getList();
        } catch (RemoteException neverThrown) {
            ProvisionLogger.loge("This should not happen.", neverThrown);
        }
        for (ApplicationInfo aInfo : aInfos) {
            if ((aInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                apps.add(aInfo.packageName);
            }
        }
        return apps;
    }

    public static void disableComponent(ComponentName toDisable, int userId) {
        try {
            IPackageManager ipm = IPackageManager.Stub.asInterface(ServiceManager
                .getService("package"));

            ipm.setComponentEnabledSetting(toDisable,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP,
                    userId);
        } catch (RemoteException neverThrown) {
            ProvisionLogger.loge("This should not happen.", neverThrown);
        } catch (Exception e) {
            ProvisionLogger.logw("Component not found, not disabling it: "
                + toDisable.toShortString());
        }
    }

    /**
     * Exception thrown when the provisioning has failed completely.
     *
     * We're using a custom exception to avoid catching subsequent exceptions that might be
     * significant.
     */
    public static class IllegalProvisioningArgumentException extends Exception {
        public IllegalProvisioningArgumentException(String message) {
            super(message);
        }

        public IllegalProvisioningArgumentException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * Check the validity of the admin component name supplied, or try to infer this componentName
     * from the package.
     *
     * We are supporting lookup by package name for legacy reasons.
     *
     * If mdmComponentName is supplied (not null):
     * mdmPackageName is ignored.
     * Check that the package of mdmComponentName is installed, that mdmComponentName is a
     * receiver in this package, and return it.
     *
     * Otherwise:
     * mdmPackageName must be supplied (not null).
     * Check that this package is installed, try to infer a potential device admin in this package,
     * and return it.
     */
    public static ComponentName findDeviceAdmin(String mdmPackageName,
            ComponentName mdmComponentName, Context c) throws IllegalProvisioningArgumentException {
        if (mdmComponentName != null) {
            mdmPackageName = mdmComponentName.getPackageName();
        }
        if (mdmPackageName == null) {
            throw new IllegalProvisioningArgumentException("Neither the package name nor the"
                    + " component name of the admin are supplied");
        }
        PackageInfo pi;
        try {
            pi = c.getPackageManager().getPackageInfo(mdmPackageName,
                    PackageManager.GET_RECEIVERS);
        } catch (NameNotFoundException e) {
            throw new IllegalProvisioningArgumentException("Mdm "+ mdmPackageName
                    + " is not installed. ", e);
        }
        if (mdmComponentName != null) {
            // If the component was specified in the intent: check that it is in the manifest.
            checkAdminComponent(mdmComponentName, pi);
            return mdmComponentName;
        } else {
            // Otherwise: try to find a potential device admin in the manifest.
            return findDeviceAdminInPackage(mdmPackageName, pi);
        }
    }

    private static void checkAdminComponent(ComponentName mdmComponentName, PackageInfo pi)
            throws IllegalProvisioningArgumentException{
        for (ActivityInfo ai : pi.receivers) {
            if (mdmComponentName.getClassName().equals(ai.name)) {
                return;
            }
        }
        throw new IllegalProvisioningArgumentException("The component " + mdmComponentName
                + " cannot be found");
    }

    private static ComponentName findDeviceAdminInPackage(String mdmPackageName, PackageInfo pi)
            throws IllegalProvisioningArgumentException {
        ComponentName mdmComponentName = null;
        for (ActivityInfo ai : pi.receivers) {
            if (!TextUtils.isEmpty(ai.permission) &&
                    ai.permission.equals(android.Manifest.permission.BIND_DEVICE_ADMIN)) {
                if (mdmComponentName != null) {
                    throw new IllegalProvisioningArgumentException("There are several "
                            + "device admins in " + mdmPackageName + " but no one in specified");
                } else {
                    mdmComponentName = new ComponentName(mdmPackageName, ai.name);
                }
            }
        }
        if (mdmComponentName == null) {
            throw new IllegalProvisioningArgumentException("There are no device admins in"
                    + mdmPackageName);
        }
        return mdmComponentName;
    }

    public static MdmPackageInfo getMdmPackageInfo(PackageManager pm, String packageName) {
        if (packageName != null) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, /* default flags */ 0);
                if (ai != null) {
                    return new MdmPackageInfo(pm.getApplicationIcon(packageName),
                            pm.getApplicationLabel(ai).toString());
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Package does not exist, ignore. Should never happen.
                ProvisionLogger.loge("Package does not exist. Should never happen.");
            }
        }

        return null;
    }

    /**
     * Information relating to the currently installed MDM package manager.
     */
    public static final class MdmPackageInfo {
        private final Drawable packageIcon;
        private final String appLabel;

        private MdmPackageInfo(Drawable packageIcon, String appLabel) {
            this.packageIcon = packageIcon;
            this.appLabel = appLabel;
        }

        public String getAppLabel() {
            return appLabel;
        }

        public Drawable getPackageIcon() {
            return packageIcon;
        }
    }

    public static boolean isCurrentUserOwner() {
        return UserHandle.myUserId() == UserHandle.USER_OWNER;
    }

    public static boolean hasDeviceOwner(Context context) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return !TextUtils.isEmpty(dpm.getDeviceOwner());
    }

    public static boolean hasDeviceInitializer(Context context) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.getDeviceInitializerApp() != null;
    }

    public static boolean isManagedProfile(Context context) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        UserInfo user = um.getUserInfo(UserHandle.myUserId());
        return user != null ? user.isManagedProfile() : false;
    }

    /**
     * Returns true if the given package does not exist on the device or if its version code is less
     * than the given version, and false otherwise.
     */
    public static boolean packageRequiresUpdate(String packageName, int minSupportedVersion,
            Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo.versionCode >= minSupportedVersion) {
                return false;
            }
        } catch (NameNotFoundException e) {
            // Package not on device.
        }

        return true;
    }

    public static byte[] stringToByteArray(String s)
        throws NumberFormatException {
        try {
            return Base64.decode(s, Base64.URL_SAFE);
        } catch (IllegalArgumentException e) {
            throw new NumberFormatException("Incorrect format. Should be Url-safe Base64 encoded.");
        }
    }

    public static String byteArrayToString(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    public static void markDeviceProvisioned(Context context) {
        if (isCurrentUserOwner()) {
            // This only needs to be set once per device
            Global.putInt(context.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        }

        // Setting this flag will either cause Setup Wizard to finish immediately when it starts (if
        // it is not already running), or when its next activity starts (if it is already running,
        // e.g. the non-NFC flow).
        // When either of these things happen, a home intent is fired. We catch that in
        // HomeReceiverActivity before sending the intent to notify the mdm that provisioning is
        // complete.
        // Note that, in the NFC flow or for secondary users, setting this flag will prevent the
        // user from seeing SUW, even if no other device initialization app was specified.
        Secure.putInt(context.getContentResolver(), Secure.USER_SETUP_COMPLETE, 1);
    }

    public static boolean isUserSetupCompleted(Context context) {
        return Secure.getInt(context.getContentResolver(), Secure.USER_SETUP_COMPLETE, 0) != 0;
    }

    public static UserHandle getManagedProfile(Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        int currentUserId = userManager.getUserHandle();
        List<UserInfo> userProfiles = userManager.getProfiles(currentUserId);
        for (UserInfo profile : userProfiles) {
            if (profile.isManagedProfile()) {
                return new UserHandle(profile.id);
            }
        }
        return null;
    }

    /**
     * @return The User id of an already existing managed profile or -1 if none
     * exists
     */
    public static int alreadyHasManagedProfile(Context context) {
        UserHandle managedUser = getManagedProfile(context);
        if (managedUser != null) {
            return managedUser.getIdentifier();
        } else {
            return -1;
        }
    }

    public static void removeAccount(Context context, Account account) {
        try {
            AccountManager accountManager =
                    (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
            AccountManagerFuture<Bundle> bundle = accountManager.removeAccount(account,
                    null, null /* callback */, null /* handler */);
            // Block to get the result of the removeAccount operation
            if (bundle.getResult().getBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)) {
                ProvisionLogger.logw("Account removed from the primary user.");
            } else {
                Intent removeIntent = (Intent) bundle.getResult().getParcelable(
                        AccountManager.KEY_INTENT);
                if (removeIntent != null) {
                    ProvisionLogger.logi("Starting activity to remove account");
                    TrampolineActivity.startActivity(context, removeIntent);
                } else {
                    ProvisionLogger.logw("Could not remove account from the primary user.");
                }
            }
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
            ProvisionLogger.logw("Exception removing account from the primary user.", e);
        }
    }

    public static boolean isFrpSupported(Context context) {
        Object pdbManager = context.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
        return pdbManager != null;
    }

}
