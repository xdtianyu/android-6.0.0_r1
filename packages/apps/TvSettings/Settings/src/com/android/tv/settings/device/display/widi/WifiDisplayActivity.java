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

package com.android.tv.settings.device.display.widi;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.os.Bundle;
import android.provider.Settings;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;

import java.util.ArrayList;

/**
 * Activity allowing the selection of wifi display settings.
 */
public class WifiDisplayActivity extends DialogActivity implements ActionAdapter.Listener {

    enum Tag {
        RENAME, WIFI_DISPLAY, PIN_REQUIRED, WIFI_DISPLAY_ON, WIFI_DISPLAY_OFF, PIN_REQUIRED_ON,
        PIN_REQUIRED_OFF
    }

    private DisplayManager mDisplayManager;
    private WifiDisplayStatus mWifiDisplayStatus;
    private ActionFragment mActionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();

        mActionFragment = ActionFragment.newInstance(getMainActions());
        setContentAndActionFragments(ContentFragment.newInstance(
                getString(R.string.accessories_wifi_display), null, null,
                R.drawable.ic_settings_widi, getResources().getColor(R.color.icon_background)),
                mActionFragment);
    }

    @Override
    public void onActionClicked(Action action) {
        Tag tag = Tag.valueOf(action.getKey());
        switch (tag) {
            case PIN_REQUIRED:
                showPinRequired();
                break;
            case PIN_REQUIRED_OFF:
                // TODO: disable pin requirement.
                showMainScreen();
                break;
            case PIN_REQUIRED_ON:
                // TODO: enable pin requirement.
                showMainScreen();
                break;
            case RENAME:
                showRename();
                break;
            case WIFI_DISPLAY:
                showWifiDisplay();
                break;
            case WIFI_DISPLAY_OFF:
                setWifiDisplayEnabled(false);
                showMainScreen();
                break;
            case WIFI_DISPLAY_ON:
                setWifiDisplayEnabled(true);
                showMainScreen();
                break;
            default:
                break;
        }
    }

    private void showWifiDisplay() {
        boolean isOn = getWifiDisplayEnabled();
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action.Builder().key(Tag.WIFI_DISPLAY_ON.name())
                .title(getString(R.string.action_on_title)).checked(isOn).build());
        actions.add(new Action.Builder().key(Tag.WIFI_DISPLAY_OFF.name())
                .title(getString(R.string.action_off_title)).checked(!isOn).build());
        setContentAndActionFragments(ContentFragment.newInstance(
                getString(R.string.accessories_wifi_display_enable),
                getString(R.string.accessories_wifi_display), null, R.drawable.ic_settings_widi,
                getResources().getColor(R.color.icon_background)),
                ActionFragment.newInstance(actions));
    }

    private void showRename() {
        // TODO: launch multi-page form with current name pre-filled out in form.
    }

    private void showPinRequired() {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action.Builder().key(Tag.PIN_REQUIRED_ON.name())
                .title(getString(R.string.action_on_title)).checked(false).build());
        actions.add(new Action.Builder().key(Tag.PIN_REQUIRED_OFF.name())
                .title(getString(R.string.action_off_title)).checked(true).build());
        setContentAndActionFragments(ContentFragment.newInstance(
                getString(R.string.accessories_wifi_display_pin_required),
                getString(R.string.accessories_wifi_display), null, R.drawable.ic_settings_widi,
                getResources().getColor(R.color.icon_background)),
                ActionFragment.newInstance(actions));
    }

    private void showMainScreen() {
        ((ActionAdapter) mActionFragment.getAdapter()).setActions(getMainActions());
        getFragmentManager().popBackStack(null, 0);
    }

    private ArrayList<Action> getMainActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        if (mWifiDisplayStatus.getFeatureState() != WifiDisplayStatus.FEATURE_STATE_DISABLED) {
            actions.add(new Action.Builder().key(Tag.RENAME.name())
                    .title(getString(R.string.accessories_wifi_display_rename_device)).build());
            actions.add(new Action.Builder().key(Tag.WIFI_DISPLAY.name())
                    .title(getString(R.string.accessories_wifi_display_enable))
                    .description(getWifiDisplayEnabled() ? getString(R.string.action_on_title)
                            : getString(R.string.action_off_title)).build());
            actions.add(new Action.Builder().key(Tag.PIN_REQUIRED.name())
                    .title(getString(R.string.accessories_wifi_display_pin_required))
                    .description(getString(R.string.action_off_title)).build());
        }
        return actions;
    }

    private boolean getWifiDisplayEnabled() {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.WIFI_DISPLAY_ON, 0)
                != 0;
    }

    private void setWifiDisplayEnabled(boolean enabled) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.WIFI_DISPLAY_ON,
                enabled ? 1 : 0);
    }
}
