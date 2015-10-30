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

package com.android.tv.settings.users;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.service.dreams.DreamService;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.tv.settings.dialog.DialogFragment.Action;
import com.android.tv.settings.util.UriUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AppLoadingTask extends AsyncTask<Void, Void, List<AppLoadingTask.SelectableAppInfo>> {

    interface Listener {
        void onPackageEnableChanged(String packageName, boolean enabled);

        void onActionsLoaded(ArrayList<Action> actions);
    }

    private static final boolean DEBUG = false;
    private static final String TAG = "RestrictedProfile";

    private final Context mContext;
    private final int mUserId;
    private final boolean mNewUser;
    private final PackageManager mPackageManager;
    private final IPackageManager mIPackageManager;
    private final Listener mListener;
    private final PackageInfo mSysPackageInfo;
    private final HashMap<String, Boolean> mSelectedPackages = new HashMap<String, Boolean>();
    private boolean mFirstTime = true;

    /**
     * Loads the list of activities that the user can enable or disable in a restricted profile.
     *
     * @param context context for querying the list of activities.
     * @param userId the user ID of the user whose apps should be listed.
     * @param newUser true if this is a newly create user.
     * @param iPackageManager used to get application info.
     * @param listener listener for package enable state changes.
     */
    AppLoadingTask(Context context, int userId, boolean newUser, IPackageManager iPackageManager,
            Listener listener) {
        mContext = context;
        mUserId = userId;
        mNewUser = newUser;
        mPackageManager = context.getPackageManager();
        mIPackageManager = iPackageManager;
        mListener = listener;
        PackageInfo sysPackageInfo = null;
        try {
            sysPackageInfo = mPackageManager.getPackageInfo("android",
                    PackageManager.GET_SIGNATURES);
        } catch (NameNotFoundException nnfe) {
            Log.wtf(TAG, "Failed to get package signatures!");
        }
        mSysPackageInfo = sysPackageInfo;
    }

    @Override
    protected List<SelectableAppInfo> doInBackground(Void... params) {
        return fetchAndMergeApps();
    }

    @Override
    protected void onPostExecute(List<SelectableAppInfo> visibleApps) {
        populateApps(visibleApps);
    }

    private void populateApps(List<SelectableAppInfo> visibleApps) {
        ArrayList<Action> actions = new ArrayList<Action>();
        Intent restrictionsIntent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
        List<ResolveInfo> receivers = mPackageManager.queryBroadcastReceivers(restrictionsIntent,
                0);
        for (SelectableAppInfo app : visibleApps) {
            String packageName = app.packageName;
            if (packageName == null) {
                if (DEBUG) {
                    Log.d(TAG, "App has no package name: " + app.appName);
                }
                continue;
            }
            final boolean isSettingsApp = packageName.equals(mContext.getPackageName());
            final boolean hasSettings = resolveInfoListHasPackage(receivers, packageName);
            boolean isAllowed = false;
            String controllingActivity = null;
            if (app.masterEntry != null) {
                controllingActivity = app.masterEntry.activityName.toString();
            }
            boolean hasCustomizableRestrictions = ((hasSettings || isSettingsApp)
                    && app.masterEntry == null);
            PackageInfo pi = null;
            try {
                pi = mIPackageManager.getPackageInfo(packageName,
                        PackageManager.GET_UNINSTALLED_PACKAGES
                        | PackageManager.GET_SIGNATURES, mUserId);
            } catch (RemoteException e) {
            }
            boolean canBeEnabledDisabled = true;
            if (pi != null && (pi.requiredForAllUsers || isPlatformSigned(pi))) {
                isAllowed = true;
                canBeEnabledDisabled = false;
                // If the app is required and has no restrictions, skip showing it
                if (!hasSettings && !isSettingsApp) {
                    if (DEBUG) {
                        Log.d(TAG, "App is required and has no settings: " + app.appName);
                    }
                    continue;
                }
                // Get and populate the defaults, since the user is not going to be
                // able to toggle this app ON (it's ON by default and immutable).
                // Only do this for restricted profiles, not single-user restrictions
                // Also don't do this for slave icons
            } else if (!mNewUser && isAppEnabledForUser(pi)) {
                isAllowed = true;
            }
            boolean availableForRestrictedProfile = true;
            if (pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                availableForRestrictedProfile = false;
                isAllowed = false;
                canBeEnabledDisabled = false;
            }
            boolean canSeeRestrictedAccounts = pi.restrictedAccountType != null;
            if (app.masterEntry != null) {
                canBeEnabledDisabled = false;
                isAllowed = mSelectedPackages.get(packageName);
            }
            onPackageEnableChanged(packageName, isAllowed);
            if (DEBUG) {
                Log.d(TAG, "Adding action for: " + app.appName + " has restrictions: "
                        + hasCustomizableRestrictions);
            }
            actions.add(UserAppRestrictionsDialogFragment.createAction(mContext, packageName,
                    app.activityName.toString(), getAppIconUri(mContext, app.info, app.iconRes),
                    canBeEnabledDisabled, isAllowed, hasCustomizableRestrictions,
                    canSeeRestrictedAccounts, availableForRestrictedProfile, controllingActivity));
        }
        mListener.onActionsLoaded(actions);
        // If this is the first time for a new profile, install/uninstall default apps for
        // profile
        // to avoid taking the hit in onPause(), which can cause race conditions on user switch.
        if (mNewUser && mFirstTime) {
            mFirstTime = false;
            UserAppRestrictionsDialogFragment.applyUserAppsStates(mSelectedPackages, actions,
                    mIPackageManager, mUserId);
        }
    }

    private void onPackageEnableChanged(String packageName, boolean enabled) {
        mListener.onPackageEnableChanged(packageName, enabled);
        mSelectedPackages.put(packageName, enabled);
    }

    private boolean resolveInfoListHasPackage(List<ResolveInfo> receivers, String packageName) {
        for (ResolveInfo info : receivers) {
            if (info.activityInfo.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private List<SelectableAppInfo> fetchAndMergeApps() {
        List<SelectableAppInfo> visibleApps = new ArrayList<SelectableAppInfo>();

        // Find all pre-installed input methods that are marked as default and add them to an
        // exclusion list so that they aren't presented to the user for toggling. Don't add
        // non-default ones, as they may include other stuff that we don't need to auto-include.
        final HashSet<String> defaultSystemImes = getDefaultSystemImes();

        // Add Settings
        try {
            visibleApps.add(new SelectableAppInfo(mPackageManager,
                    mPackageManager.getApplicationInfo(mContext.getPackageName(), 0)));
        } catch (NameNotFoundException nnfe) {
            Log.e(TAG, "Couldn't add settings item to list!", nnfe);
        }

        // Add leanback launchers
        Intent leanbackLauncherIntent = new Intent(Intent.ACTION_MAIN);
        leanbackLauncherIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        addSystemApps(visibleApps, leanbackLauncherIntent, defaultSystemImes, mUserId);

        // Add widgets
        Intent widgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        addSystemApps(visibleApps, widgetIntent, defaultSystemImes, mUserId);

        // Add daydreams
        Intent daydreamIntent = new Intent(DreamService.SERVICE_INTERFACE);
        addSystemApps(visibleApps, daydreamIntent, defaultSystemImes, mUserId);

        List<ApplicationInfo> installedApps = mPackageManager.getInstalledApplications(
                PackageManager.GET_UNINSTALLED_PACKAGES);
        addNonSystemApps(installedApps, true, visibleApps);

        // Get the list of apps already installed for the user
        try {
            List<ApplicationInfo> userApps = mIPackageManager.getInstalledApplications(
                    PackageManager.GET_UNINSTALLED_PACKAGES, mUserId).getList();
            addNonSystemApps(userApps, false, visibleApps);
        } catch (RemoteException re) {
        }

        // Sort the list of visible apps
        Collections.sort(visibleApps, new AppLabelComparator());

        // Remove dupes
        Set<String> dedupPackageSet = new HashSet<String>();
        for (int i = visibleApps.size() - 1; i >= 0; i--) {
            SelectableAppInfo info = visibleApps.get(i);
            if (DEBUG) {
                Log.i(TAG, info.toString());
            }
            String both = info.packageName + "+" + info.activityName;
            if (!TextUtils.isEmpty(info.packageName)
                    && !TextUtils.isEmpty(info.activityName)
                    && dedupPackageSet.contains(both)) {
                if (DEBUG) {
                    Log.d(TAG, "Removing app: " + info.appName);
                }
                visibleApps.remove(i);
            } else {
                dedupPackageSet.add(both);
            }
        }

        // Establish master/slave relationship for entries that share a package name
        HashMap<String, SelectableAppInfo> packageMap = new HashMap<String,
                SelectableAppInfo>();
        for (SelectableAppInfo info : visibleApps) {
            if (packageMap.containsKey(info.packageName)) {
                info.masterEntry = packageMap.get(info.packageName);
            } else {
                packageMap.put(info.packageName, info);
            }
        }
        return visibleApps;
    }

    private void addNonSystemApps(List<ApplicationInfo> apps, boolean disableSystemApps,
            List<SelectableAppInfo> visibleApps) {
        if (apps == null) {
            return;
        }

        for (ApplicationInfo app : apps) {
            // If it's not installed, skip
            if ((app.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
                continue;
            }

            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    && (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                // Downloaded app
                visibleApps.add(new SelectableAppInfo(mPackageManager, app));
            } else if (disableSystemApps) {
                try {
                    PackageInfo pi = mPackageManager.getPackageInfo(app.packageName, 0);
                    // If it's a system app that requires an account and doesn't see restricted
                    // accounts, mark for removal. It might get shown in the UI if it has an
                    // icon but will still be marked as false and immutable.
                    if (pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                        onPackageEnableChanged(app.packageName, false);
                    }
                } catch (NameNotFoundException re) {
                }
            }
        }
    }

    static class SelectableAppInfo {
        private final String packageName;
        private final CharSequence appName;
        private final CharSequence activityName;
        private final ApplicationInfo info;
        private final int iconRes;
        private SelectableAppInfo masterEntry;

        SelectableAppInfo(PackageManager packageManager, ResolveInfo resolveInfo) {
            packageName = resolveInfo.activityInfo.packageName;
            appName = resolveInfo.activityInfo.applicationInfo.loadLabel(packageManager);
            CharSequence label = resolveInfo.activityInfo.loadLabel(packageManager);
            activityName = (label != null) ? label : appName;
            int activityIconRes = getIconResource(resolveInfo.activityInfo);
            info = resolveInfo.activityInfo.applicationInfo;
            iconRes = activityIconRes != 0 ? activityIconRes
                    : getIconResource(resolveInfo.activityInfo.applicationInfo);
        }

        SelectableAppInfo(PackageManager packageManager, ApplicationInfo applicationInfo) {
            packageName = applicationInfo.packageName;
            appName = applicationInfo.loadLabel(packageManager);
            activityName = appName;
            info = applicationInfo;
            iconRes = getIconResource(applicationInfo);
        }

        @Override
        public String toString() {
            return packageName + ": appName=" + appName + "; activityName=" + activityName
                    + "; masterEntry=" + masterEntry;
        }

        private int getIconResource(PackageItemInfo packageItemInfo) {
            if (packageItemInfo.banner != 0) {
                return packageItemInfo.banner;
            }
            if (packageItemInfo.logo != 0) {
                return packageItemInfo.logo;
            }
            return packageItemInfo.icon;
        }
    }

    private static class AppLabelComparator implements Comparator<SelectableAppInfo> {

        @Override
        public int compare(SelectableAppInfo lhs, SelectableAppInfo rhs) {
            String lhsLabel = lhs.activityName.toString();
            String rhsLabel = rhs.activityName.toString();
            return lhsLabel.toLowerCase().compareTo(rhsLabel.toLowerCase());
        }
    }

    /**
     * Find all pre-installed input methods that are marked as default and add them to an exclusion
     * list so that they aren't presented to the user for toggling. Don't add non-default ones, as
     * they may include other stuff that we don't need to auto-include.
     *
     * @return the set of default system imes
     */
    private HashSet<String> getDefaultSystemImes() {
        HashSet<String> defaultSystemImes = new HashSet<String>();
        InputMethodManager imm = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> imis = imm.getInputMethodList();
        for (InputMethodInfo imi : imis) {
            try {
                if (imi.isDefault(mContext) && isSystemPackage(imi.getPackageName())) {
                    defaultSystemImes.add(imi.getPackageName());
                }
            } catch (Resources.NotFoundException rnfe) {
                // Not default
            }
        }
        return defaultSystemImes;
    }

    private boolean isSystemPackage(String packageName) {
        try {
            final PackageInfo pi = mPackageManager.getPackageInfo(packageName, 0);
            if (pi.applicationInfo == null)
                return false;
            final int flags = pi.applicationInfo.flags;
            if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                return true;
            }
        } catch (NameNotFoundException nnfe) {
            // Missing package?
        }
        return false;
    }

    /**
     * Add system apps that match an intent to the list, excluding any packages in the exclude list.
     *
     * @param visibleApps list of apps to append the new list to
     * @param intent the intent to match
     * @param excludePackages the set of package names to be excluded, since they're required
     */
    private void addSystemApps(List<SelectableAppInfo> visibleApps, Intent intent,
            Set<String> excludePackages, int userId) {
        final PackageManager pm = mPackageManager;
        List<ResolveInfo> launchableApps = pm.queryIntentActivities(intent,
                PackageManager.GET_DISABLED_COMPONENTS
                | PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ResolveInfo app : launchableApps) {
            if (app.activityInfo != null && app.activityInfo.applicationInfo != null) {
                final String packageName = app.activityInfo.packageName;
                int flags = app.activityInfo.applicationInfo.flags;
                if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    if (DEBUG) {
                        Log.d(TAG, "Found system app: "
                                + app.activityInfo.applicationInfo.loadLabel(pm));
                    }
                    // System app
                    // Skip excluded packages
                    if (excludePackages.contains(packageName)) {
                        if (DEBUG) {
                            Log.d(TAG, "App is an excluded ime, not adding: "
                                    + app.activityInfo.applicationInfo.loadLabel(pm));
                        }
                        continue;
                    }
                    int enabled = pm.getApplicationEnabledSetting(packageName);
                    if (enabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
                            || enabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        // Check if the app is already enabled for the target user
                        ApplicationInfo targetUserAppInfo = getAppInfoForUser(packageName,
                                0, userId);
                        if (targetUserAppInfo == null
                                || (targetUserAppInfo.flags & ApplicationInfo.FLAG_INSTALLED)
                                        == 0) {
                            if (DEBUG) {
                                Log.d(TAG, "App is already something, not adding: "
                                        + app.activityInfo.applicationInfo.loadLabel(pm));
                            }
                            continue;
                        }
                    }

                    if (DEBUG) {
                        Log.d(TAG, "Adding system app: "
                                + app.activityInfo.applicationInfo.loadLabel(pm));
                    }
                    visibleApps.add(new SelectableAppInfo(pm, app));
                }
            }
        }
    }

    private ApplicationInfo getAppInfoForUser(String packageName, int flags, int userId) {
        try {
            ApplicationInfo targetUserAppInfo = mIPackageManager.getApplicationInfo(packageName,
                    flags,
                    userId);
            return targetUserAppInfo;
        } catch (RemoteException re) {
            return null;
        }
    }

    private boolean isPlatformSigned(PackageInfo pi) {
        return (pi != null && pi.signatures != null &&
                mSysPackageInfo.signatures[0].equals(pi.signatures[0]));
    }

    private boolean isAppEnabledForUser(PackageInfo pi) {
        if (pi == null)
            return false;
        final int flags = pi.applicationInfo.flags;
        final int privateFlags = pi.applicationInfo.privateFlags;
        // Return true if it is installed and not hidden
        return ((flags & ApplicationInfo.FLAG_INSTALLED) != 0
                && (privateFlags & ApplicationInfo.PRIVATE_FLAG_HIDDEN) == 0);
    }

    private static Uri getAppIconUri(Context context, ApplicationInfo info, int iconRes) {
        String iconUri = null;
        if (iconRes != 0) {
            try {
                Resources resources = context.getPackageManager()
                        .getResourcesForApplication(info);
                ShortcutIconResource iconResource = new ShortcutIconResource();
                iconResource.packageName = info.packageName;
                iconResource.resourceName = resources.getResourceName(iconRes);
                iconUri = UriUtils.getShortcutIconResourceUri(iconResource).toString();
            } catch (Exception e1) {
                Log.w("AppsBrowseInfo", e1.toString());
            }
        } else {
            iconUri = UriUtils.getAndroidResourceUri(Resources.getSystem(),
                    com.android.internal.R.drawable.sym_def_app_icon);
        }

        if (iconUri == null) {
            iconUri = UriUtils.getAndroidResourceUri(context.getResources(),
                    com.android.internal.R.drawable.sym_app_on_sd_unavailable_icon);
        }
        return Uri.parse(iconUri);
    }
}
