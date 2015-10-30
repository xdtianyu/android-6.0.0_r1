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

package com.android.tv.settings.system;

import com.android.tv.settings.R;
import com.android.tv.settings.users.RestrictedProfileDialogFragment;
import com.android.tv.settings.util.SettingsHelper;
import com.android.tv.settings.dialog.DialogFragment;
import com.android.tv.settings.dialog.DialogFragment.Action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages app security preferences. TODO: get a better icon from UX TODO: implement Notification
 * listener settings
 */
public class SecurityActivity extends Activity implements Action.Listener {

    private static final int CHECK_SET_ID = 1;
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String ACTION_RESTRICTED_PROFILE = "action_restricted_profile";
    private static final String ACTION_SECURITY_UNKNOWN_SOURCES = "action_security_unknown_sources";
    private static final String ACTION_SECURITY_UNKNOWN_SOURCES_OFF =
            "action_security_unknown_sources_off";
    private static final String ACTION_SECURITY_VERIFY_APPS = "action_security_verify_apps";
    private static final String ACTION_SECURITY_VERIFY_APPS_ON = "action_security_verify_apps_on";
    private static final String ACTION_SECURITY_VERIFY_APPS_OFF = "action_security_verify_apps_off";
    private static final String ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM =
            "action_security_verify_apps_confirm";
    private static final String ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM_OK =
            "action_security_verify_apps_confirm_ok";
    private static final String ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM_CANCEL =
            "action_security_verify_apps_confirm_cancel";

    private static final String TAG_RESTRICTED_PROFILE_SIDECAR_FRAGMENT =
            "restricted_profile_sidecar";

    private SettingsHelper mHelper;
    private boolean mVerifierInstalled;
    private DialogFragment mMainMenuDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mHelper = new SettingsHelper(getApplicationContext());
        mVerifierInstalled = isVerifierInstalled();
        // Do this after setting up what's needed.
        super.onCreate(savedInstanceState);
        mMainMenuDialogFragment = new DialogFragment.Builder()
                .breadcrumb(getString(R.string.header_category_personal))
                .title(getString(R.string.system_security))
                .iconResourceId(R.drawable.ic_settings_security)
                .iconBackgroundColor(getResources().getColor(R.color.icon_background))
                .actions(getMainMenuActions()).build();
        DialogFragment.add(getFragmentManager(), mMainMenuDialogFragment);
    }

    @Override
    public void onActionClicked(Action action) {
        if (ACTION_RESTRICTED_PROFILE.equals(action.getKey())) {
            getFragmentManager().beginTransaction().add(new RestrictedProfileDialogFragment(),
                    TAG_RESTRICTED_PROFILE_SIDECAR_FRAGMENT).commit();
        } else if (ACTION_SECURITY_UNKNOWN_SOURCES.equals(action.getKey())) {
            boolean isNonMarketAppsAllowed = isNonMarketAppsAllowed();
            ArrayList<Action> actions = new ArrayList<>();
            // Note we only use the check set id if the "on" is checked so if "off" is selected
            // there is an animation of the check for "off".  We don't want the same behavior for
            // "on" because it has to go through a confirmation sub-dialog.
            actions.add(new Action.Builder()
                    .key(ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM)
                    .title(getString(R.string.settings_on))
                    .checked(isNonMarketAppsAllowed)
                    .checkSetId(isNonMarketAppsAllowed ? CHECK_SET_ID : 0)
                    .build());
            actions.add(new Action.Builder()
                    .key(ACTION_SECURITY_UNKNOWN_SOURCES_OFF)
                    .title(getString(R.string.settings_off))
                    .checked(!isNonMarketAppsAllowed)
                    .checkSetId(CHECK_SET_ID)
                    .build());

            DialogFragment dialogFragment = new DialogFragment.Builder()
                    .breadcrumb(getString(R.string.system_security))
                    .title(getString(R.string.security_unknown_sources_title))
                    .description(getString(R.string.security_unknown_sources_desc))
                    .actions(actions)
                    .build();
            DialogFragment.add(getFragmentManager(), dialogFragment);
        } else if (ACTION_SECURITY_UNKNOWN_SOURCES_OFF.equals(action.getKey())) {
            setNonMarketAppsAllowed(false);
            mMainMenuDialogFragment.setActions(getMainMenuActions());
            getFragmentManager().popBackStack();
        } else if (ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM.equals(action.getKey())) {
            // No point in issuing the confirmation sub-dialog if we're already "on".
            if (isNonMarketAppsAllowed()) {
                getFragmentManager().popBackStack();
            } else {
                ArrayList<Action> actions = new ArrayList<>();
                actions.add(new Action.Builder()
                        .key(ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM_OK)
                        .title(getString(R.string.settings_ok))
                        .build());
                actions.add(new Action.Builder()
                        .key(ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM_CANCEL)
                        .title(getString(R.string.settings_cancel))
                        .build());

                DialogFragment dialogFragment = new DialogFragment.Builder()
                        .breadcrumb(getString(R.string.system_security))
                        .title(getString(R.string.security_unknown_sources_title))
                        .description(getString(R.string.security_unknown_sources_confirm_desc))
                        .actions(actions)
                        .build();
                DialogFragment.add(getFragmentManager(), dialogFragment);
            }
        } else if (ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM_OK.equals(action.getKey())) {
            setNonMarketAppsAllowed(true);
            mMainMenuDialogFragment.setActions(getMainMenuActions());
            getFragmentManager().popBackStack();
            getFragmentManager().popBackStack();
        } else if (ACTION_SECURITY_UNKNOWN_SOURCES_CONFIRM_CANCEL.equals(action.getKey())) {
            getFragmentManager().popBackStack();
            getFragmentManager().popBackStack();
        } else if (ACTION_SECURITY_VERIFY_APPS.equals(action.getKey())) {
            boolean isVerifyAppsEnabled = mVerifierInstalled && isVerifyAppsEnabled();

            ArrayList<Action> actions = new ArrayList<>();
            actions.add(new Action.Builder()
                    .key(ACTION_SECURITY_VERIFY_APPS_ON)
                    .title(getString(R.string.settings_on))
                    .checked(isVerifyAppsEnabled)
                    .checkSetId(CHECK_SET_ID)
                    .build());
            actions.add(new Action.Builder()
                    .key(ACTION_SECURITY_VERIFY_APPS_OFF)
                    .title(getString(R.string.settings_off))
                    .checked(!isVerifyAppsEnabled)
                    .checkSetId(CHECK_SET_ID)
                    .build());

            DialogFragment dialogFragment = new DialogFragment.Builder()
                    .breadcrumb(getString(R.string.system_security))
                    .title(getString(R.string.security_verify_apps_title))
                    .description(getString(R.string.security_verify_apps_desc))
                    .actions(actions)
                    .build();
            DialogFragment.add(getFragmentManager(), dialogFragment);
        } else if (ACTION_SECURITY_VERIFY_APPS_ON.equals(action.getKey())) {
            setVerifyAppsEnabled(true);
            mMainMenuDialogFragment.setActions(getMainMenuActions());
            getFragmentManager().popBackStack();
        } else if (ACTION_SECURITY_VERIFY_APPS_OFF.equals(action.getKey())) {
            setVerifyAppsEnabled(false);
            mMainMenuDialogFragment.setActions(getMainMenuActions());
            getFragmentManager().popBackStack();
        }
    }

    private ArrayList<Action> getMainMenuActions() {
        boolean isNonMarketAppsAllowed = isNonMarketAppsAllowed();
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new Action.Builder()
                .key(ACTION_SECURITY_UNKNOWN_SOURCES)
                .title(getString(R.string.security_unknown_sources_title))
                .description(mHelper.getStatusStringFromBoolean(isNonMarketAppsAllowed()))
                .build());
        if (showVerifierSetting()) {
            actions.add(new Action.Builder()
                    .key(ACTION_SECURITY_VERIFY_APPS)
                    .title(getString(R.string.security_verify_apps_title))
                    .description(mHelper.getStatusStringFromBoolean(isVerifyAppsEnabled()
                            && mVerifierInstalled))
                    .enabled(mVerifierInstalled)
                    .build());
        }
        actions.add(new Action.Builder().key(ACTION_RESTRICTED_PROFILE)
                .title(getString(R.string.launcher_restricted_profile_app_name))
                .description(RestrictedProfileDialogFragment.getActionDescription(this))
                .build());
        return actions;
    }

    private boolean isNonMarketAppsAllowed() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.INSTALL_NON_MARKET_APPS, 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            return;
        }
        // Change the system setting
        Settings.Global.putInt(getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS,
                enabled ? 1 : 0);
    }

    private boolean isVerifyAppsEnabled() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_ENABLE, 1) > 0;
    }

    private void setVerifyAppsEnabled(boolean enable) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.PACKAGE_VERIFIER_ENABLE,
                enable ? 1 : 0);
    }

    private boolean isVerifierInstalled() {
        final PackageManager pm = getPackageManager();
        final Intent verification = new Intent(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
        verification.setType(PACKAGE_MIME_TYPE);
        verification.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(verification, 0);
        return (receivers.size() > 0) ? true : false;
    }

    private boolean showVerifierSetting() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 1) > 0;
    }

}
