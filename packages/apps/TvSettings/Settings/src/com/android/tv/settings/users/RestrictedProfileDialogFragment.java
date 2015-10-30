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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.util.Log;

import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.DialogFragment;
import com.android.tv.settings.dialog.DialogFragment.Action;
import com.android.tv.settings.dialog.PinDialogFragment;

import java.util.ArrayList;

/**
 * Activity that allows the configuration of a user's restricted profile.
 */
public class RestrictedProfileDialogFragment extends Fragment implements Action.Listener,
        AppLoadingTask.Listener, RestrictedProfilePinDialogFragment.Callback {

    private static final String TAG = "RestrictedProfile";

    private static final String ACTION_RESTRICTED_PROFILE_SETUP_LOCKSCREEN =
            "restricted_setup_locakscreen";
    private static final String ACTION_RESTRICTED_PROFILE_CREATE = "restricted_profile_create";
    private static final String ACTION_RESTRICTED_PROFILE_SWITCH_TO =
            "restricted_profile_switch_to";
    private static final String  ACTION_RESTRICTED_PROFILE_SWITCH_OUT =
            "restricted_profile_switch_out";
    private static final String ACTION_RESTRICTED_PROFILE_CONFIG = "restricted_profile_config";
    private static final String ACTION_RESTRICTED_PROFILE_CONFIG_APPS =
            "restricted_profile_config_apps";
    private static final String ACTION_RESTRICTED_PROFILE_CHANGE_PASSWORD =
            "restricted_profile_change_password";
    private static final String ACTION_RESTRICTED_PROFILE_DELETE = "restricted_profile_delete";
    private static final String
            ACTION_RESTRICTED_PROFILE_DELETE_CONFIRM = "restricted_profile_delete_confirm";
    private static final String
            ACTION_RESTRICTED_PROFILE_DELETE_CANCEL = "restricted_profile_delete_cancel";

    private static final String STATE_PIN_MODE = "RestrictedProfileActivity.pinMode";

    private static final int PIN_MODE_NONE = 0;
    private static final int PIN_MODE_CHOOSE_LOCKSCREEN = 1;
    private static final int PIN_MODE_RESTRICTED_PROFILE_SWITCH_OUT = 2;
    private static final int PIN_MODE_RESTRICTED_PROFILE_CHANGE_PASSWORD = 3;
    private static final int PIN_MODE_RESTRICTED_PROFILE_DELETE = 4;

    private UserManager mUserManager;
    private UserInfo mRestrictedUserInfo;
    private DialogFragment mMainMenuDialogFragment;
    private ILockSettings mLockSettingsService;
    private Handler mHandler;
    private IPackageManager mIPm;
    private AppLoadingTask mAppLoadingTask;
    private Action mConfigAppsAction;
    private DialogFragment mConfigDialogFragment;

    private int mPinMode;

    private final boolean mIsOwner = UserHandle.myUserId() == UserHandle.USER_OWNER;
    private final AsyncTask<Void, Void, UserInfo> mAddUserAsyncTask =
            new AsyncTask<Void, Void, UserInfo>() {
        @Override
        protected UserInfo doInBackground(Void... params) {
            UserInfo restrictedUserInfo = mUserManager.createUser(
                    RestrictedProfileDialogFragment.this.getString(R.string.user_new_profile_name),
                    UserInfo.FLAG_RESTRICTED);
            if (restrictedUserInfo == null) {
                Log.wtf(TAG, "Got back a null user handle!");
                return null;
            }
            int userId = restrictedUserInfo.id;
            UserHandle user = new UserHandle(userId);
            mUserManager.setUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS, true, user);
            Bitmap bitmap = createBitmapFromDrawable(R.drawable.ic_avatar_default);
            mUserManager.setUserIcon(userId, bitmap);
            // Add shared accounts
            AccountManager am = AccountManager.get(getActivity());
            Account[] accounts = am.getAccounts();
            if (accounts != null) {
                for (Account account : accounts) {
                    am.addSharedAccount(account, user);
                }
            }
            return restrictedUserInfo;
        }

        @Override
        protected void onPostExecute(UserInfo result) {
            if (result == null) {
                return;
            }
            mRestrictedUserInfo = result;
            UserSwitchListenerService.updateLaunchPoint(getActivity(), true);
            int userId = result.id;
            if (result.isRestricted() && mIsOwner) {
                DialogFragment dialogFragment = UserAppRestrictionsDialogFragment.newInstance(
                        getActivity(), userId, true);
                DialogFragment.add(getFragmentManager(), dialogFragment);
                mMainMenuDialogFragment.setActions(getMainMenuActions());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        mRestrictedUserInfo = findRestrictedUser(mUserManager);
        mConfigAppsAction = createConfigAppsAction(-1);
        mMainMenuDialogFragment = new DialogFragment.Builder()
                .title(getString(R.string.launcher_restricted_profile_app_name))
                .description(getString(R.string.user_add_profile_item_summary))
                .iconResourceId(getIconResource())
                .iconBackgroundColor(getResources().getColor(R.color.icon_background))
                .actions(getMainMenuActions()).build();
        mMainMenuDialogFragment.setListener(this);
        DialogFragment.add(getFragmentManager(), mMainMenuDialogFragment);
        if (savedInstanceState != null) {
            mPinMode = savedInstanceState.getInt(STATE_PIN_MODE, PIN_MODE_NONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRestrictedUserInfo != null && (mAppLoadingTask == null
                || mAppLoadingTask.getStatus() == AsyncTask.Status.FINISHED)) {
            mAppLoadingTask = new AppLoadingTask(getActivity(), mRestrictedUserInfo.id, false, mIPm,
                    this);
            mAppLoadingTask.execute((Void[]) null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PIN_MODE, mPinMode);
    }

    @Override
    public void onPackageEnableChanged(String packageName, boolean enabled) {
    }

    @Override
    public void onActionsLoaded(ArrayList<Action> actions) {
        int allowedApps = 0;
        for(Action action : actions) {
            if(action.isChecked()) {
                allowedApps++;
            }
        }
        mConfigAppsAction = createConfigAppsAction(allowedApps);
        if (mConfigDialogFragment != null) {
            mConfigDialogFragment.setActions(getConfigActions());
        }
    }

    @Override
    public void onActionClicked(Action action) {
        if (ACTION_RESTRICTED_PROFILE_SWITCH_TO.equals(action.getKey())) {
            switchUserNow(mRestrictedUserInfo.id);
            getActivity().finish();
        } else if (ACTION_RESTRICTED_PROFILE_SWITCH_OUT.equals(action.getKey())) {
            if (getFragmentManager().findFragmentByTag(PinDialogFragment.DIALOG_TAG) != null) {
                return;
            }
            mPinMode = PIN_MODE_RESTRICTED_PROFILE_SWITCH_OUT;
            RestrictedProfilePinDialogFragment restrictedProfilePinDialogFragment =
                    RestrictedProfilePinDialogFragment.newInstance(
                            PinDialogFragment.PIN_DIALOG_TYPE_ENTER_PIN);
            restrictedProfilePinDialogFragment.setTargetFragment(this, 0);
            restrictedProfilePinDialogFragment.show(getFragmentManager(),
                    PinDialogFragment.DIALOG_TAG);
        } else if (ACTION_RESTRICTED_PROFILE_CHANGE_PASSWORD.equals(action.getKey())) {
            if (getFragmentManager().findFragmentByTag(PinDialogFragment.DIALOG_TAG) != null) {
                return;
            }
            mPinMode = PIN_MODE_RESTRICTED_PROFILE_CHANGE_PASSWORD;
            RestrictedProfilePinDialogFragment restrictedProfilePinDialogFragment =
                    RestrictedProfilePinDialogFragment.newInstance(
                            PinDialogFragment.PIN_DIALOG_TYPE_NEW_PIN);
            restrictedProfilePinDialogFragment.setTargetFragment(this, 0);
            restrictedProfilePinDialogFragment.show(getFragmentManager(),
                    PinDialogFragment.DIALOG_TAG);
        } else if (ACTION_RESTRICTED_PROFILE_CONFIG.equals(action.getKey())) {
            mConfigDialogFragment = new DialogFragment.Builder()
                    .title(getString(R.string.restricted_profile_configure_title))
                    .iconResourceId(getIconResource())
                    .iconBackgroundColor(getResources().getColor(R.color.icon_background))
                    .actions(getConfigActions()).build();
            mConfigDialogFragment.setListener(this);
            DialogFragment.add(getFragmentManager(), mConfigDialogFragment);
        } else if (ACTION_RESTRICTED_PROFILE_CONFIG_APPS.equals(action.getKey())) {
            DialogFragment dialogFragment = UserAppRestrictionsDialogFragment.newInstance(
                    getActivity(), mRestrictedUserInfo.id, false);
            DialogFragment.add(getFragmentManager(), dialogFragment);
        } else if (ACTION_RESTRICTED_PROFILE_DELETE.equals(action.getKey())) {
            if (getFragmentManager().findFragmentByTag(PinDialogFragment.DIALOG_TAG) != null) {
                return;
            }
            mPinMode = PIN_MODE_RESTRICTED_PROFILE_DELETE;
            RestrictedProfilePinDialogFragment restrictedProfilePinDialogFragment =
                    RestrictedProfilePinDialogFragment.newInstance(
                            PinDialogFragment.PIN_DIALOG_TYPE_ENTER_PIN);
            restrictedProfilePinDialogFragment.setTargetFragment(this, 0);
            restrictedProfilePinDialogFragment.show(getFragmentManager(),
                    PinDialogFragment.DIALOG_TAG);
        } else if (ACTION_RESTRICTED_PROFILE_DELETE_CONFIRM.equals(action.getKey())) {
            // TODO remove once we confirm it's not needed
            removeRestrictedUser();
            LockPatternUtils lpu = new LockPatternUtils(getActivity());
            lpu.clearLock(UserHandle.myUserId());
        } else if (ACTION_RESTRICTED_PROFILE_DELETE_CANCEL.equals(action.getKey())) {
            // TODO remove once we confirm it's not needed
            getActivity().onBackPressed();
        } else if (ACTION_RESTRICTED_PROFILE_CREATE.equals(action.getKey())) {
            if (hasLockscreenSecurity(new LockPatternUtils(getActivity()))) {
                addRestrictedUser();
            } else {
                launchChooseLockscreen();
            }
        }
    }

    /**
     * The description string that should be used for an action that launches the restricted profile
     * activity.
     *
     * @param context used to get the appropriate string.
     * @return the description string that should be used for an action that launches the restricted
     *         profile activity.
     */
    public static String getActionDescription(Context context) {
        return context.getString(isRestrictedProfileInEffect(context) ? R.string.on : R.string.off);
    }

    public static boolean isRestrictedProfileInEffect(Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        UserInfo userInfo = userManager.getUserInfo(UserHandle.myUserId());
        return userInfo.isRestricted();
    }

    /* package */ static void switchUserNow(int userId) {
        try {
            ActivityManagerNative.getDefault().switchUser(userId);
        } catch (RemoteException re) {
            Log.e(TAG, "Caught exception while switching user! ", re);
        }
    }

    /* package */ static int getIconResource() {
        return R.drawable.ic_settings_restricted_profile;
    }

    /* package */ static UserInfo findRestrictedUser(UserManager userManager) {
        for (UserInfo userInfo : userManager.getUsers()) {
            if (userInfo.isRestricted()) {
                return userInfo;
            }
        }
        return null;
    }

    // RestrictedProfilePinDialogFragment.Callback methods
    @Override
    public void saveLockPassword(String pin, int quality) {
        new LockPatternUtils(getActivity()).saveLockPassword(pin, null, quality,
                UserHandle.myUserId());
    }

    @Override
    public boolean checkPassword(String password, int userId) {
        try {
            return getLockSettings().checkPassword(password, userId).getResponseCode()
                == VerifyCredentialResponse.RESPONSE_OK;
        } catch (final RemoteException e) {
            // ignore
        }
        return false;
    }

    @Override
    public boolean hasLockscreenSecurity() {
        return RestrictedProfileDialogFragment.hasLockscreenSecurity(
                new LockPatternUtils(getActivity()));
    }

    @Override
    public void pinFragmentDone(boolean success) {
        switch (mPinMode) {
            case PIN_MODE_CHOOSE_LOCKSCREEN:
                if (success) {
                    addRestrictedUser();
                }
                break;
            case PIN_MODE_RESTRICTED_PROFILE_SWITCH_OUT:
                if (success) {
                    switchUserNow(UserHandle.USER_OWNER);
                    getActivity().finish();
                }
                break;
            case PIN_MODE_RESTRICTED_PROFILE_CHANGE_PASSWORD:
                // do nothing
                break;
            case PIN_MODE_RESTRICTED_PROFILE_DELETE:
                if (success) {
                    removeRestrictedUser();
                    new LockPatternUtils(getActivity()).clearLock(UserHandle.myUserId());
                }
                break;
        }
    }

    private ILockSettings getLockSettings() {
        if (mLockSettingsService == null) {
            mLockSettingsService = ILockSettings.Stub.asInterface(
                    ServiceManager.getService("lock_settings"));
        }
        return mLockSettingsService;
    }

    private ArrayList<Action> getMainMenuActions() {
        ArrayList<Action> actions = new ArrayList<>();
        if (mRestrictedUserInfo != null) {
            if (mIsOwner) {
                actions.add(new Action.Builder()
                        .key(ACTION_RESTRICTED_PROFILE_SWITCH_TO)
                        .title(getString(R.string.restricted_profile_switch_to))
                        .build());
                actions.add(new Action.Builder()
                        .key(ACTION_RESTRICTED_PROFILE_CONFIG)
                        .title(getString(R.string.restricted_profile_configure_title))
                        .build());
                actions.add(new Action.Builder()
                        .key(ACTION_RESTRICTED_PROFILE_DELETE)
                        .title(getString(R.string.restricted_profile_delete_title))
                        .build());
            } else {
                actions.add(new Action.Builder()
                        .key(ACTION_RESTRICTED_PROFILE_SWITCH_OUT)
                        .title(getString(R.string.restricted_profile_switch_out))
                        .build());
            }
        } else {
            actions.add(new Action.Builder()
                    .key(ACTION_RESTRICTED_PROFILE_CREATE)
                        .title(getString(R.string.restricted_profile_configure_title))
                    .build());
        }
        return actions;
    }

    private ArrayList<Action> getConfigActions() {
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new Action.Builder()
                .key(ACTION_RESTRICTED_PROFILE_CHANGE_PASSWORD)
                .title(getString(R.string.restricted_profile_change_password_title))
                .build());
        actions.add(mConfigAppsAction);
        return actions;
    }

    private Action createConfigAppsAction(int allowedApps) {
        String description = allowedApps >= 0 ? getResources().getQuantityString(
                R.plurals.restricted_profile_configure_apps_description, allowedApps, allowedApps)
                : getString(R.string.restricted_profile_configure_apps_description_loading);
        return new Action.Builder()
                .key(ACTION_RESTRICTED_PROFILE_CONFIG_APPS)
                .title(getString(R.string.restricted_profile_configure_apps_title))
                .description(description)
                .build();
    }

    private static boolean hasLockscreenSecurity(LockPatternUtils lpu) {
        return lpu.isLockPasswordEnabled(UserHandle.myUserId())
                || lpu.isLockPatternEnabled(UserHandle.myUserId());
    }

    private void launchChooseLockscreen() {
        if (getFragmentManager().findFragmentByTag(PinDialogFragment.DIALOG_TAG) != null) {
            return;
        }
        mPinMode = PIN_MODE_CHOOSE_LOCKSCREEN;
        RestrictedProfilePinDialogFragment restrictedProfilePinDialogFragment =
                RestrictedProfilePinDialogFragment.newInstance(
                        PinDialogFragment.PIN_DIALOG_TYPE_NEW_PIN);
        restrictedProfilePinDialogFragment.setTargetFragment(this, 0);
        restrictedProfilePinDialogFragment.show(getFragmentManager(), PinDialogFragment.DIALOG_TAG);
    }

    private void removeRestrictedUser() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mUserManager.removeUser(mRestrictedUserInfo.id);
                // pop confirm dialog
                mRestrictedUserInfo = null;
                UserSwitchListenerService.updateLaunchPoint(getActivity(), false);
                mMainMenuDialogFragment.setActions(getMainMenuActions());
                getFragmentManager().popBackStack();
            }
        });
    }

    private Bitmap createBitmapFromDrawable(int resId) {
        Drawable icon = getActivity().getDrawable(resId);
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        icon.draw(new Canvas(bitmap));
        return bitmap;
    }

    private void addRestrictedUser() {
        if (AsyncTask.Status.PENDING == mAddUserAsyncTask.getStatus()) {
            mAddUserAsyncTask.execute((Void[]) null);
        }
    }
}
