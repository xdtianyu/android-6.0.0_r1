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

package com.android.cts.verifier.managedprovisioning;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.IntentDrivenTestActivity;
import com.android.cts.verifier.IntentDrivenTestActivity.ButtonInfo;
import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter.TestListItem;
import com.android.cts.verifier.TestResult;

/**
 * Activity that lists all positive device owner tests. Requires the following adb command be issued
 * by the user prior to starting the tests:
 *
 * adb shell dpm set-device-owner
 *  'com.android.cts.verifier/com.android.cts.verifier.managedprovisioning.DeviceAdminTestReceiver'
 */
public class DeviceOwnerPositiveTestActivity extends PassFailButtons.TestListActivity {
    private static final String TAG = "DeviceOwnerPositiveTestActivity";

    static final String EXTRA_COMMAND = "extra-command";
    static final String EXTRA_TEST_ID = "extra-test-id";
    static final String COMMAND_SET_POLICY = "set-policy";
    static final String EXTRA_POLICY = "extra-policy";
    static final String EXTRA_PARAMETER_1 = "extra_parameter_1";
    static final String EXTRA_PARAMETER_2 = "extra_parameter_2";
    static final String COMMAND_ADD_USER_RESTRICTION = "add-user-restriction";
    static final String COMMAND_CLEAR_USER_RESTRICTION = "clear-user-restriction";
    static final String EXTRA_RESTRICTION = "extra-restriction";
    static final String COMMAND_TEAR_DOWN = "tear-down";
    static final String COMMAND_CHECK_DEVICE_OWNER = "check-device-owner";
    static final String COMMAND_SET_GLOBAL_SETTING = "set-global-setting";
    static final String COMMAND_SET_STATUSBAR_DISABLED = "set-statusbar-disabled";
    static final String COMMAND_SET_KEYGUARD_DISABLED = "set-keyguard-disabled";
    static final String COMMAND_CHECK_PERMISSION_LOCKDOWN = "check-permission-lockdown";
    static final String EXTRA_SETTING = "extra-setting";

    private static final String CHECK_DEVICE_OWNER_TEST_ID = "CHECK_DEVICE_OWNER";
    private static final String DEVICE_ADMIN_SETTINGS_ID = "DEVICE_ADMIN_SETTINGS";
    private static final String WIFI_LOCKDOWN_TEST_ID = WifiLockdownTestActivity.class.getName();
    private static final String DISABLE_STATUS_BAR_TEST_ID = "DISABLE_STATUS_BAR";
    private static final String DISABLE_KEYGUARD_TEST_ID = "DISABLE_KEYGUARD";
    private static final String CHECK_PERMISSION_LOCKDOWN_TEST_ID =
            PermissionLockdownTestActivity.class.getName();
    private static final String DISALLOW_CONFIG_BT_ID = "DISALLOW_CONFIG_BT";
    private static final String DISALLOW_CONFIG_WIFI_ID = "DISALLOW_CONFIG_WIFI";
    private static final String REMOVE_DEVICE_OWNER_TEST_ID = "REMOVE_DEVICE_OWNER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.positive_device_owner);
        setInfoResources(R.string.device_owner_positive_tests,
                R.string.device_owner_positive_tests_info, 0);
        setPassFailButtonClickListeners();

        final ArrayTestListAdapter adapter = new ArrayTestListAdapter(this);
        adapter.add(TestListItem.newCategory(this, R.string.device_owner_positive_category));

        addTestsToAdapter(adapter);

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updatePassButton();
            }
        });

        setTestListAdapter(adapter);

        View setDeviceOwnerButton = findViewById(R.id.set_device_owner_button);
        setDeviceOwnerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(
                        DeviceOwnerPositiveTestActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(R.string.set_device_owner_dialog_title)
                        .setMessage(R.string.set_device_owner_dialog_text)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

    }

    @Override
    public void finish() {
        // Pass and fail buttons are known to call finish() when clicked, and this is when we want
        // to remove the device owner.
        startActivity(createTearDownIntent());
        super.finish();
    }

    /**
     * Enable Pass Button when all tests passed.
     */
    private void updatePassButton() {
        getPassButton().setEnabled(mAdapter.allTestsPassed());
    }

    private void addTestsToAdapter(final ArrayTestListAdapter adapter) {
        adapter.add(createTestItem(this, CHECK_DEVICE_OWNER_TEST_ID,
                R.string.device_owner_check_device_owner_test,
                new Intent(this, CommandReceiver.class)
                        .putExtra(EXTRA_COMMAND, COMMAND_CHECK_DEVICE_OWNER)
                        ));

        // device admin settings
        adapter.add(createInteractiveTestItem(this, DEVICE_ADMIN_SETTINGS_ID,
                R.string.device_owner_device_admin_visible,
                R.string.device_owner_device_admin_visible_info,
                new ButtonInfo(
                        R.string.device_owner_settings_go,
                        new Intent(Settings.ACTION_SECURITY_SETTINGS))));

        PackageManager packageManager = getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            // WiFi Lock down tests
            adapter.add(createTestItem(this, WIFI_LOCKDOWN_TEST_ID,
                    R.string.device_owner_wifi_lockdown_test,
                    new Intent(this, WifiLockdownTestActivity.class)));

            // DISALLOW_CONFIG_WIFI
            adapter.add(createInteractiveTestItem(this, DISALLOW_CONFIG_WIFI_ID,
                    R.string.device_owner_disallow_config_wifi,
                    R.string.device_owner_disallow_config_wifi_info,
                    new ButtonInfo[] {
                            new ButtonInfo(
                                    R.string.device_owner_user_restriction_set,
                                    createSetUserRestrictionIntent(
                                            UserManager.DISALLOW_CONFIG_WIFI)),
                            new ButtonInfo(
                                    R.string.device_owner_settings_go,
                                    new Intent(Settings.ACTION_WIFI_SETTINGS))}));
        }

        // DISALLOW_CONFIG_BLUETOOTH
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            adapter.add(createInteractiveTestItem(this, DISALLOW_CONFIG_BT_ID,
                    R.string.device_owner_disallow_config_bt,
                    R.string.device_owner_disallow_config_bt_info,
                    new ButtonInfo[] {
                            new ButtonInfo(
                                    R.string.device_owner_user_restriction_set,
                                    createSetUserRestrictionIntent(
                                            UserManager.DISALLOW_CONFIG_BLUETOOTH)),
                            new ButtonInfo(
                                    R.string.device_owner_settings_go,
                                    new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))}));
        }

        // setStatusBarDisabled
        adapter.add(createInteractiveTestItem(this, DISABLE_STATUS_BAR_TEST_ID,
                R.string.device_owner_disable_statusbar_test,
                R.string.device_owner_disable_statusbar_test_info,
                new ButtonInfo[] {
                        new ButtonInfo(
                                R.string.device_owner_disable_statusbar_button,
                                createDeviceOwnerIntentWithBooleanParameter(
                                        COMMAND_SET_STATUSBAR_DISABLED, true)),
                        new ButtonInfo(
                                R.string.device_owner_reenable_statusbar_button,
                                createDeviceOwnerIntentWithBooleanParameter(
                                        COMMAND_SET_STATUSBAR_DISABLED, false))}));

        // setKeyguardDisabled
        adapter.add(createInteractiveTestItem(this, DISABLE_KEYGUARD_TEST_ID,
                R.string.device_owner_disable_keyguard_test,
                R.string.device_owner_disable_keyguard_test_info,
                new ButtonInfo[] {
                        new ButtonInfo(
                                R.string.device_owner_disable_keyguard_button,
                                createDeviceOwnerIntentWithBooleanParameter(
                                        COMMAND_SET_KEYGUARD_DISABLED, true)),
                        new ButtonInfo(
                                R.string.device_owner_reenable_keyguard_button,
                                createDeviceOwnerIntentWithBooleanParameter(
                                        COMMAND_SET_KEYGUARD_DISABLED, false))}));

        // setPermissionGrantState
        adapter.add(createTestItem(this, CHECK_PERMISSION_LOCKDOWN_TEST_ID,
                R.string.device_profile_owner_permission_lockdown_test,
                new Intent(PermissionLockdownTestActivity.ACTION_CHECK_PERMISSION_LOCKDOWN)));

        // removeDeviceOwner
        adapter.add(createInteractiveTestItem(this, REMOVE_DEVICE_OWNER_TEST_ID,
                R.string.device_owner_remove_device_owner_test,
                R.string.device_owner_remove_device_owner_test_info,
                new ButtonInfo(
                        R.string.remove_device_owner_button,
                        createTearDownIntent())));
    }

    static TestListItem createInteractiveTestItem(Activity activity, String id, int titleRes,
            int infoRes, ButtonInfo buttonInfo) {
        return createInteractiveTestItem(activity, id, titleRes, infoRes,
                new ButtonInfo[] { buttonInfo });
    }

    static TestListItem createInteractiveTestItem(Activity activity, String id, int titleRes,
            int infoRes, ButtonInfo[] buttonInfos) {
        return TestListItem.newTest(activity, titleRes,
                id, new Intent(activity, IntentDrivenTestActivity.class)
                .putExtra(IntentDrivenTestActivity.EXTRA_ID, id)
                .putExtra(IntentDrivenTestActivity.EXTRA_TITLE, titleRes)
                .putExtra(IntentDrivenTestActivity.EXTRA_INFO, infoRes)
                .putExtra(IntentDrivenTestActivity.EXTRA_BUTTONS, buttonInfos),
                null);
    }

    static TestListItem createTestItem(Activity activity, String id, int titleRes,
            Intent intent) {
        return TestListItem.newTest(activity, titleRes, id, intent.putExtra(EXTRA_TEST_ID, id),
                null);
    }

    private Intent createTearDownIntent() {
        return new Intent(this, CommandReceiver.class)
                .putExtra(EXTRA_COMMAND, COMMAND_TEAR_DOWN);
    }

    private Intent createDeviceOwnerIntentWithBooleanParameter(String command, boolean value) {
        return new Intent(this, CommandReceiver.class)
                .putExtra(EXTRA_COMMAND, command)
                .putExtra(EXTRA_PARAMETER_1, value);
    }

    private Intent createSetUserRestrictionIntent(String restriction) {
        return new Intent(this, CommandReceiver.class)
                .putExtra(EXTRA_COMMAND, COMMAND_ADD_USER_RESTRICTION)
                .putExtra(EXTRA_RESTRICTION, restriction);
    }

    public static class CommandReceiver extends Activity {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Intent intent = getIntent();
            String command = intent.getStringExtra(EXTRA_COMMAND);
            try {
                DevicePolicyManager dpm = (DevicePolicyManager)
                        getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName admin = DeviceAdminTestReceiver.getReceiverComponentName();
                Log.i(TAG, "Command: " + command);

                if (COMMAND_ADD_USER_RESTRICTION.equals(command)) {
                    String restrictionKey = intent.getStringExtra(EXTRA_RESTRICTION);
                    dpm.addUserRestriction(admin, restrictionKey);
                    Log.i(TAG, "Added user restriction " + restrictionKey);
                } else if (COMMAND_CLEAR_USER_RESTRICTION.equals(command)) {
                    String restrictionKey = intent.getStringExtra(EXTRA_RESTRICTION);
                    dpm.clearUserRestriction(admin, restrictionKey);
                    Log.i(TAG, "Cleared user restriction " + restrictionKey);
                } else if (COMMAND_TEAR_DOWN.equals(command)) {
                    tearDown(dpm, admin);
                } else if (COMMAND_SET_GLOBAL_SETTING.equals(command)) {
                    final String setting = intent.getStringExtra(EXTRA_SETTING);
                    final String value = intent.getStringExtra(EXTRA_PARAMETER_1);
                    dpm.setGlobalSetting(admin, setting, value);
                } else if (COMMAND_SET_STATUSBAR_DISABLED.equals(command)) {
                    final boolean value = intent.getBooleanExtra(EXTRA_PARAMETER_1, false);
                    dpm.setStatusBarDisabled(admin, value);
                } else if (COMMAND_SET_KEYGUARD_DISABLED.equals(command)) {
                    final boolean value = intent.getBooleanExtra(EXTRA_PARAMETER_1, false);
                    if (value) {
                        dpm.resetPassword(null, 0);
                    }
                    dpm.setKeyguardDisabled(admin, value);
                } else if (COMMAND_CHECK_DEVICE_OWNER.equals(command)) {
                    if (dpm.isDeviceOwnerApp(getPackageName())) {
                        TestResult.setPassedResult(this, intent.getStringExtra(EXTRA_TEST_ID),
                                null, null);
                    } else {
                        TestResult.setFailedResult(this, intent.getStringExtra(EXTRA_TEST_ID),
                                getString(R.string.device_owner_incorrect_device_owner), null);
                    }
                } else {
                    Log.e(TAG, "Invalid command: " + command);
                }
            } catch (Exception e) {
                Log.e(TAG, "Command " + command + " failed with exception " + e);
            } finally {
                // No matter what happened, don't let the activity run
                finish();
            }
        }

        private void tearDown(DevicePolicyManager dpm, ComponentName admin) {
            if (dpm == null || !dpm.isDeviceOwnerApp(getPackageName())) {
                return;
            }

            dpm.setStatusBarDisabled(admin, false);
            dpm.setKeyguardDisabled(admin, false);
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_BLUETOOTH);
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_WIFI);
            dpm.clearDeviceOwnerApp(getPackageName());
        }
    }
}

