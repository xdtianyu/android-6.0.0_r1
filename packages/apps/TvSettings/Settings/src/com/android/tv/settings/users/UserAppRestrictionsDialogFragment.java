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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.DialogFragment;
import com.android.tv.settings.dialog.DialogFragment.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * DialogFragment that configures the app restrictions for a given user.
 */
public class UserAppRestrictionsDialogFragment extends DialogFragment implements Action.Listener,
        AppLoadingTask.Listener {

    private static final boolean DEBUG = false;
    private static final String TAG = "RestrictedProfile";

    /** Key for extra passed in from calling fragment for the userId of the user being edited */
    public static final String EXTRA_USER_ID = "user_id";

    /** Key for extra passed in from calling fragment to indicate if this is a newly created user */
    public static final String EXTRA_NEW_USER = "new_user";

    private static final String EXTRA_CONTENT_TITLE = "title";
    private static final String EXTRA_CONTENT_BREADCRUMB = "breadcrumb";
    private static final String EXTRA_CONTENT_DESCRIPTION = "description";
    private static final String EXTRA_CONTENT_ICON_RESOURCE_ID = "iconResourceId";
    private static final String EXTRA_CONTENT_ICON_URI = "iconUri";
    private static final String EXTRA_CONTENT_ICON_BITMAP = "iconBitmap";
    private static final String EXTRA_CONTENT_ICON_BACKGROUND = "iconBackground";

    private static final String EXTRA_PACKAGE_NAME = "packageName";
    private static final String EXTRA_CAN_CONFIGURE_RESTRICTIONS = "canConfigureRestrictions";
    private static final String EXTRA_CAN_SEE_RESTRICTED_ACCOUNTS = "canSeeRestrictedAccounts";
    private static final String EXTRA_IS_ALLOWED = "isAllowed";
    private static final String EXTRA_CAN_BE_ENABLED_DISABLED = "canBeEnabledDisabled";
    private static final String EXTRA_CONTROLLING_APP = "controllingApp";

    private static final String ACTION_ALLOW = "allow";
    private static final String ACTION_DISALLOW = "disallow";
    private static final String ACTION_CONFIGURE = "configure";
    private static final String ACTION_CUSTOMIZE_RESTRICTIONS = "customizeRestriction";

    private static final int CHECK_SET_ID = 1;

    public static UserAppRestrictionsDialogFragment newInstance(Context context, int userId,
            boolean newUser) {
        UserAppRestrictionsDialogFragment fragment = new UserAppRestrictionsDialogFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_CONTENT_TITLE,
                context.getString(R.string.restricted_profile_configure_apps_title));
        args.putInt(EXTRA_CONTENT_ICON_RESOURCE_ID, R.drawable.ic_settings_launcher_icon);
        args.putInt(EXTRA_CONTENT_ICON_BACKGROUND,
                context.getResources().getColor(R.color.icon_background));
        args.putInt(EXTRA_USER_ID, userId);
        args.putBoolean(EXTRA_NEW_USER, newUser);
        fragment.setArguments(args);
        return fragment;
    }

    private UserManager mUserManager;
    private IPackageManager mIPm;
    private AppLoadingTask mAppLoadingTask;
    private final HashMap<String, Boolean> mSelectedPackages = new HashMap<>();
    private UserHandle mUser;
    private boolean mNewUser;
    private boolean mAppListChanged;
    private AppRestrictionsManager mAppRestrictionsManager;

    private final BroadcastReceiver mUserBackgrounding = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update the user's app selection right away without waiting for a pause
            // onPause() might come in too late, causing apps to disappear after broadcasts
            // have been scheduled during user startup.
            if (mAppListChanged) {
                if (DEBUG) {
                    Log.d(TAG, "User backgrounding, update app list");
                }
                applyUserAppsStates(mSelectedPackages, getActions(), mIPm, mUser.getIdentifier());
                if (DEBUG) {
                    Log.d(TAG, "User backgrounding, done updating app list");
                }
            }
        }
    };
    private final BroadcastReceiver mPackageObserver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onPackageChanged(intent);
        }

        private void onPackageChanged(Intent intent) {
            String action = intent.getAction();
            String packageName = intent.getData().getSchemeSpecificPart();
            // Package added, check if the preference needs to be enabled
            ArrayList<Action> matchingActions = findActionsWithPackageName(getActions(),
                    packageName);
            for (Action matchingAction : matchingActions) {
                if ((Intent.ACTION_PACKAGE_ADDED.equals(action) && matchingAction.isChecked()) || (
                        Intent.ACTION_PACKAGE_REMOVED.equals(action)
                        && !matchingAction.isChecked())) {
                    matchingAction.setEnabled(true);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        setActions(new ArrayList<Action>());
        setListener(this);
        if (icicle != null) {
            mUser = new UserHandle(icicle.getInt(EXTRA_USER_ID));
        } else {
            Bundle args = getArguments();
            if (args != null) {
                if (args.containsKey(EXTRA_USER_ID)) {
                    mUser = new UserHandle(args.getInt(EXTRA_USER_ID));
                }
                mNewUser = args.getBoolean(EXTRA_NEW_USER, false);
            }
        }

        if (mUser == null) {
            mUser = android.os.Process.myUserHandle();
        }

        mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mUserBackgrounding,
                new IntentFilter(Intent.ACTION_USER_BACKGROUND));
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");
        getActivity().registerReceiver(mPackageObserver, packageFilter);

        if (mAppLoadingTask == null) {
            mAppListChanged = false;
            mAppLoadingTask = new AppLoadingTask(getActivity(), mUser.getIdentifier(), mNewUser,
                    mIPm, this);
            mAppLoadingTask.execute((Void[]) null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mNewUser = false;
        getActivity().unregisterReceiver(mUserBackgrounding);
        getActivity().unregisterReceiver(mPackageObserver);
        if (mAppListChanged) {
            new Thread() {
                public void run() {
                    applyUserAppsStates(mSelectedPackages, getActions(), mIPm,
                            mUser.getIdentifier());
                }
            }.start();
        }
    }

    @Override
    public void onActionClicked(Action action) {
        String packageName = action.getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        if (ACTION_CONFIGURE.equals(action.getKey())) {
            boolean isAllowed = action.getIntent().getBooleanExtra(EXTRA_IS_ALLOWED, false);
            boolean canBeEnabledDisabled = action.getIntent().getBooleanExtra(
                    EXTRA_CAN_BE_ENABLED_DISABLED, false);
            String controllingActivity = action.getIntent().getStringExtra(EXTRA_CONTROLLING_APP);
            final ArrayList<Action> initialAllowDisallowActions = new ArrayList<>();
            if (controllingActivity != null) {
                initialAllowDisallowActions.add(new Action.Builder()
                        .title(getString(isAllowed ? R.string.restricted_profile_allowed
                                : R.string.restricted_profile_not_allowed))
                        .description(getString(R.string.user_restrictions_controlled_by,
                                controllingActivity))
                        .checked(isAllowed)
                        .infoOnly(true)
                        .build());
            } else if (!canBeEnabledDisabled) {
                initialAllowDisallowActions.add(new Action.Builder()
                        .title(getString(isAllowed ? R.string.restricted_profile_allowed
                                : R.string.restricted_profile_not_allowed))
                        .checked(isAllowed)
                        .infoOnly(true)
                        .build());
            } else {
                boolean canSeeRestrictedAccounts = action.getIntent().getBooleanExtra(
                        EXTRA_CAN_SEE_RESTRICTED_ACCOUNTS, true);
                initialAllowDisallowActions.add(new Action.Builder().key(ACTION_DISALLOW)
                        .title(getString(R.string.restricted_profile_not_allowed))
                        .intent(action.getIntent())
                        .checked(!isAllowed)
                        .checkSetId(CHECK_SET_ID).build());
                initialAllowDisallowActions.add(new Action.Builder().key(ACTION_ALLOW)
                        .title(getString(R.string.restricted_profile_allowed))
                        .description(canSeeRestrictedAccounts ? getString(
                                R.string.app_sees_restricted_accounts)
                                : null)
                        .intent(action.getIntent())
                        .checkSetId(CHECK_SET_ID)
                        .checked(isAllowed)
                        .build());
            }

            final DialogFragment dialogFragment = new DialogFragment.Builder()
                    .title(action.getTitle()).iconUri(action.getIconUri())
                    .actions(initialAllowDisallowActions).build();

            boolean canConfigureRestrictions = action.getIntent().getBooleanExtra(
                    EXTRA_CAN_CONFIGURE_RESTRICTIONS, false);
            if (canConfigureRestrictions) {
                mAppRestrictionsManager = new AppRestrictionsManager(this, mUser,
                        mUserManager, packageName, new AppRestrictionsManager.Listener() {
                            @Override
                            public void onRestrictionActionsLoaded(String packageName,
                                    ArrayList<Action> restrictionActions) {
                                ArrayList<Action> oldActions = dialogFragment.getActions();
                                ArrayList<Action> newActions = new ArrayList<>();
                                if (oldActions != null && oldActions.size()
                                        >= initialAllowDisallowActions.size()) {
                                    for (int i = 0, size = initialAllowDisallowActions.size();
                                            i < size; i++) {
                                        newActions.add(oldActions.get(i));
                                    }
                                } else {
                                    newActions.addAll(initialAllowDisallowActions);
                                }
                                newActions.addAll(restrictionActions);
                                dialogFragment.setActions(newActions);
                            }
                        });
                mAppRestrictionsManager.loadRestrictionActions();
            }

            dialogFragment.setListener(this);
            DialogFragment.add(getFragmentManager(), dialogFragment);
        } else if (ACTION_ALLOW.equals(action.getKey())) {
            setEnabled(packageName, true);
            getFragmentManager().popBackStack();
        } else if (ACTION_DISALLOW.equals(action.getKey())) {
            setEnabled(packageName, false);
            getFragmentManager().popBackStack();
        } else if (mAppRestrictionsManager != null) {
            mAppRestrictionsManager.onActionClicked(action);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mAppRestrictionsManager != null) {
            mAppRestrictionsManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setEnabled(String packageName, boolean enabled) {
        onPackageEnableChanged(packageName, enabled);
        for (Action action : getActions()) {
            String actionPackageName = action.getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
            if (actionPackageName.equals(packageName)) {
                action.setChecked(enabled);
                action.setDescription(getString(enabled ? R.string.restricted_profile_allowed
                        : R.string.restricted_profile_not_allowed));
                action.getIntent().putExtra(EXTRA_IS_ALLOWED, enabled);
            }
        }
        onActionsLoaded(getActions());
    }

    @Override
    public void onPackageEnableChanged(String packageName, boolean enabled) {
        mSelectedPackages.put(packageName, enabled);
        mAppListChanged = true;
        if (getActivity() instanceof AppLoadingTask.Listener) {
            ((AppLoadingTask.Listener) getActivity()).onPackageEnableChanged(packageName, enabled);
        }
    }

    @Override
    public void onActionsLoaded(ArrayList<Action> actions) {
        setActions(actions);
        if (getActivity() instanceof AppLoadingTask.Listener) {
            ((AppLoadingTask.Listener) getActivity()).onActionsLoaded(actions);
        }
    }

    static Action createAction(Context context, String packageName, String title, Uri iconUri,
            boolean canBeEnabledDisabled, boolean isAllowed, boolean hasCustomizableRestrictions,
            boolean canSeeRestrictedAccounts, boolean availableForRestrictedProfile,
            String controllingActivity) {
        String description = context.getString(
                availableForRestrictedProfile ? isAllowed ? R.string.restricted_profile_allowed
                        : R.string.restricted_profile_not_allowed
                        : R.string.app_not_supported_in_limited);

        Intent intent = new Intent().putExtra(EXTRA_IS_ALLOWED, isAllowed)
                .putExtra(EXTRA_CAN_CONFIGURE_RESTRICTIONS,
                        (hasCustomizableRestrictions && (controllingActivity == null)))
                .putExtra(EXTRA_CAN_BE_ENABLED_DISABLED, canBeEnabledDisabled)
                .putExtra(EXTRA_PACKAGE_NAME, packageName)
                .putExtra(EXTRA_CONTROLLING_APP, controllingActivity)
                .putExtra(EXTRA_CAN_SEE_RESTRICTED_ACCOUNTS, canSeeRestrictedAccounts);

        if (DEBUG) {
            Log.d(TAG, "Icon uri: " + (iconUri != null ? iconUri.toString() : "null"));
        }
        return new Action.Builder()
                .key(ACTION_CONFIGURE)
                .title(title)
                .description(description)
                .enabled(availableForRestrictedProfile)
                .iconUri(iconUri)
                .checked(isAllowed)
                .intent(intent)
                .build();
    }

    static void applyUserAppsStates(HashMap<String, Boolean> selectedPackages,
            ArrayList<Action> actions, IPackageManager ipm, int userId) {
        for (Map.Entry<String, Boolean> entry : selectedPackages.entrySet()) {
            String packageName = entry.getKey();
            boolean enabled = entry.getValue();
            if (applyUserAppState(ipm, userId, packageName, enabled)) {
                disableActionForPackage(actions, packageName);
            }
        }
    }

    static boolean applyUserAppState(IPackageManager ipm, int userId, String packageName,
            boolean enabled) {
        boolean disableActionForPackage = false;
        if (enabled) {
            // Enable selected apps
            try {
                ApplicationInfo info = ipm.getApplicationInfo(packageName,
                        PackageManager.GET_UNINSTALLED_PACKAGES, userId);
                if (info == null || !info.enabled
                        || (info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
                    ipm.installExistingPackageAsUser(packageName, userId);
                    if (DEBUG) {
                        Log.d(TAG, "Installing " + packageName);
                    }
                }
                if (info != null && (info.privateFlags & ApplicationInfo.PRIVATE_FLAG_HIDDEN) != 0
                        && (info.flags & ApplicationInfo.FLAG_INSTALLED) != 0) {
                    disableActionForPackage = true;
                    ipm.setApplicationHiddenSettingAsUser(packageName, false, userId);
                    if (DEBUG) {
                        Log.d(TAG, "Unhiding " + packageName);
                    }
                }
            } catch (RemoteException re) {
                Log.e(TAG, "Caught exception while installing " + packageName + "!", re);
            }
        } else {
            // Blacklist all other apps, system or downloaded
            try {
                ApplicationInfo info = ipm.getApplicationInfo(packageName, 0, userId);
                if (info != null) {
                    ipm.deletePackageAsUser(packageName, null, userId,
                            PackageManager.DELETE_SYSTEM_APP);
                    if (DEBUG) {
                        Log.d(TAG, "Uninstalling " + packageName);
                    }
                }
            } catch (RemoteException re) {
                Log.e(TAG, "Caught exception while uninstalling " + packageName + "!", re);
            }
        }
        return disableActionForPackage;
    }

    private static void disableActionForPackage(ArrayList<Action> actions, String packageName) {
        ArrayList<Action> matchingActions = findActionsWithPackageName(actions, packageName);
        for(Action matchingAction : matchingActions) {
            matchingAction.setEnabled(false);
        }
    }

    private static ArrayList<Action> findActionsWithPackageName(ArrayList<Action> actions,
            String packageName) {
        ArrayList<Action> matchingActions = new ArrayList<>();
        if (packageName != null) {
            for (Action action : actions) {
                if (packageName.equals(action.getIntent().getStringExtra(EXTRA_PACKAGE_NAME))) {
                    matchingActions.add(action);
                }
            }
        }
        return matchingActions;
    }
}
