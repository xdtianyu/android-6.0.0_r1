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

package com.android.tv.settings.device.display.daydream;

// This setting controls when we will start dreaming

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;

import java.util.ArrayList;

import static android.provider.Settings.Secure.SLEEP_TIMEOUT;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

// This setting controls when we'll turn the output off and go to sleep

/**
 * Activity that allows the setting of daydreams.
 */
public class DaydreamActivity extends DialogActivity implements ActionAdapter.Listener {

    enum ActionType {
        SELECT,
        LIST_DREAM_TIMEOUT,
        LIST_SYSTEM_SLEEP_TIMEOUT,
        SET_DREAM_TIMEOUT,
        SET_SYSTEM_SLEEP_TIMEOUT,
        TEST
    }

    /** If there is no setting in the provider, use this. */
    private static final int DREAM_SETTINGS_REQUEST = 1;
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 1800000;
    private static final String EXTRA_LIST_VALUE = "list_value";
    private static final int CHECK_SET_ID = 1;

    private static final int DEFAULT_SLEEP_TIMEOUT_MS = 3 * 60 * 60 * 1000;

    private DreamBackend mDreamBackend;
    private ContentFragment mContentFragment;
    private ActionFragment mActionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDreamBackend = new DreamBackend(this);
        mDreamBackend.initDreamInfoActions();
        mContentFragment = createMainMenuContentFragment();
        mActionFragment = ActionFragment.newInstance(getMainActions());
        setContentAndActionFragments(mContentFragment, mActionFragment);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        if (requestCode == DREAM_SETTINGS_REQUEST) {
            goToMainScreen();
        }
    }

    @Override
    public void onActionClicked(Action action) {
        if (action instanceof DreamInfoAction) {
            DreamInfoAction dreamInfoAction = (DreamInfoAction) action;
            dreamInfoAction.setDream(mDreamBackend);

            /**
             * If this day dream has settings, launch the activity to set its
             * settings before returning to main menu. Otherwise just return to
             * main menu.
             */
            Intent settingsIntent = dreamInfoAction.getSettingsIntent();
            if (settingsIntent != null) {
                startActivityForResult(settingsIntent, DREAM_SETTINGS_REQUEST);
            } else {
                goToMainScreen();
            }
        } else {
            ActionType type = ActionType.valueOf(action.getKey());
            switch (type) {
                case SELECT:
                    onSelect();
                    break;
                case LIST_DREAM_TIMEOUT:
                    onListDreamTimeouts();
                    break;
                case LIST_SYSTEM_SLEEP_TIMEOUT:
                    onListSystemSleepTimeouts();
                    break;
                case SET_DREAM_TIMEOUT:
                    onSetDreamTimeout(action);
                    break;
                case SET_SYSTEM_SLEEP_TIMEOUT:
                    onSetSystemSleepTimeout(action);
                    break;
                case TEST:
                    onTest();
                    break;
                default:
                    break;
            }
        }
    }

    private void onSelect() {
        setContentAndActionFragments(
                createSubMenuContentFragment(getString(R.string.device_daydreams_select), null),
                ActionFragment.newInstance(mDreamBackend.getDreamInfoActions()));
    }

    private void onListDreamTimeouts() {
        setContentAndActionFragments(createSubMenuContentFragment(
                getString(R.string.device_daydreams_sleep),
                getString(R.string.device_daydreams_sleep_description)),
                ActionFragment.newInstance(getListActions(ActionType.SET_DREAM_TIMEOUT.name(),
                        R.array.sleep_timeout_values, R.array.sleep_timeout_entries,
                        getDreamTimeoutValue())));
    }

    private void onListSystemSleepTimeouts() {
        setContentAndActionFragments(createSubMenuContentFragment(
                getString(R.string.device_daydreams_screen_off),
                getString(R.string.device_daydreams_screen_off_description)),
                ActionFragment.newInstance(getListActions(ActionType.SET_SYSTEM_SLEEP_TIMEOUT.name(),
                        R.array.screen_off_timeout_values, R.array.screen_off_timeout_entries,
                        getSystemSleepTimeout())));
    }

    private void onSetDreamTimeout(Action action) {
        long sleepValue = action.getIntent().getLongExtra(EXTRA_LIST_VALUE, 0);
        Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, (int) sleepValue);
        goToMainScreen();
    }

    private void onSetSystemSleepTimeout(Action action) {
        int screenOffValue = (int) action.getIntent().getLongExtra(EXTRA_LIST_VALUE, 0);
        setSystemSleepTimeout(screenOffValue);
        goToMainScreen();
    }

    private void onTest() {
        mDreamBackend.startDreaming();
    }

    private void goToMainScreen() {
        updateMainScreen();
        getFragmentManager().popBackStack(null, 0);
    }

    private ArrayList<Action> getMainActions() {
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new Action.Builder().key(ActionType.SELECT.name())
                .title(getString(R.string.device_daydreams_select))
                .description(mDreamBackend.getActiveDreamTitle()).build());
        actions.add(new Action.Builder()
                .key(ActionType.LIST_DREAM_TIMEOUT.name())
                .title(getString(R.string.device_daydreams_sleep))
                .description((getString(R.string.device_daydreams_sleep_summary,
                        getEntry(R.array.sleep_timeout_values, R.array.sleep_timeout_entries,
                                getDreamTimeoutValue()))))
                .build());

        String[] screenOffEntries = getResources().getStringArray(
                R.array.screen_off_timeout_entries);
        int systemSleepTimeout = getSystemSleepTimeout();
        // Only add the summary text if the value is not "Never"
        String screenOffDescription = systemSleepTimeout > 0 ? getString(
                R.string.device_daydreams_sleep_summary,
                getEntry(R.array.screen_off_timeout_values, R.array.screen_off_timeout_entries,
                        systemSleepTimeout)) : screenOffEntries[screenOffEntries.length - 1];
        actions.add(new Action.Builder()
                .key(ActionType.LIST_SYSTEM_SLEEP_TIMEOUT.name())
                .title(getString(R.string.device_daydreams_screen_off))
                .description(screenOffDescription)
                .build());
        actions.add(new Action.Builder().key(ActionType.TEST.name())
                .title(getString(R.string.device_daydreams_test)).enabled(mDreamBackend.isEnabled())
                .build());
        return actions;
    }

    private ContentFragment createSubMenuContentFragment(String title, String description) {
        return ContentFragment.newInstance(title, getString(R.string.device_daydream), description,
                R.drawable.ic_settings_daydream, getResources().getColor(R.color.icon_background));
    }

    private ContentFragment createMainMenuContentFragment() {
        return ContentFragment.newInstance(
                getString(R.string.device_daydream), getString(R.string.device_display),
                null, R.drawable.ic_settings_daydream,
                getResources().getColor(R.color.icon_background));
    }

    private void updateMainScreen() {
        ((ActionAdapter) mActionFragment.getAdapter()).setActions(getMainActions());
    }

    private ArrayList<Action> getListActions(String key, int valuesResId, int entriesResId,
            long value) {
        String[] sleepOptionValues = getResources().getStringArray(valuesResId);
        String[] sleepOptionEntries = getResources().getStringArray(entriesResId);

        ArrayList<Action> actions = new ArrayList<>();
        for (int index = 0; index < sleepOptionValues.length; ++index) {
            long loopValue = Long.parseLong(sleepOptionValues[index]);
            Action sleepAction = new Action.Builder().key(key)
                    .title(sleepOptionEntries[index])
                    .checked(loopValue == value)
                    .intent(new Intent().putExtra(EXTRA_LIST_VALUE, loopValue))
                    .checkSetId(CHECK_SET_ID).build();
            actions.add(sleepAction);
        }
        return actions;
    }

    private int getDreamTimeoutValue() {
        return Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
    }

    private String getEntry(int valuesResId, int entriesResId, long value) {
        String[] sleepOptionValues = getResources().getStringArray(valuesResId);
        String[] sleepOptionEntries = getResources().getStringArray(entriesResId);
        for (int index = 0; index < sleepOptionValues.length; ++index) {
            long loopValue = Long.parseLong(sleepOptionValues[index]);
            if (loopValue == value) {
                return sleepOptionEntries[index];
            }
        }
        return null;
    }

    private int getSystemSleepTimeout() {
        return Settings.Secure.getInt(getContentResolver(), SLEEP_TIMEOUT,
                DEFAULT_SLEEP_TIMEOUT_MS);
    }

    private void setSystemSleepTimeout(int valueMs) {
        Settings.Secure.putInt(getContentResolver(), SLEEP_TIMEOUT, valueMs);
    }
}
