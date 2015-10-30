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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputInfo.TvInputSettings;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.Layout.Action;
import com.android.tv.settings.dialog.Layout.Header;
import com.android.tv.settings.dialog.Layout.LayoutGetter;
import com.android.tv.settings.dialog.Layout.Static;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Activity to control TV input settings.
 */
public class InputsActivity extends SettingsLayoutActivity {
    private static final String TAG = "InputsActivity";
    private static final boolean DEBUG = false;

    private static final int ACTION_EDIT_LABEL = 0;
    private static final int ACTION_CUSTOM_LABEL = 1;
    private static final int ACTION_HIDE = 2;
    private static final int ACTION_HDMI_CONTROL = 3;
    private static final int ACTION_DEVICE_AUTO_OFF = 4;
    private static final int ACTION_TV_AUTO_ON = 5;

    private static final String KEY_ID = "id";
    private static final String KEY_LABEL = "label";
    private static final String KEY_ON = "on";

    private static final int REQUEST_CODE_CUSTOM_LABEL = 0;

    private static final int DISABLED = 0;
    private static final int ENABLED = 1;

    private static final int PREDEFINED_LABEL_RES_IDS[] = {
        R.string.inputs_blu_ray,
        R.string.inputs_cable,
        R.string.inputs_dvd,
        R.string.inputs_game,
        R.string.inputs_aux
    };

    private static final Map<Integer, Integer> STATE_STRING_ID_MAP =
            new LinkedHashMap<Integer, Integer>() {{
                put(TvInputManager.INPUT_STATE_CONNECTED,
                        R.plurals.inputs_header_connected_input);
                put(TvInputManager.INPUT_STATE_CONNECTED_STANDBY,
                        R.plurals.inputs_header_standby_input);
                put(TvInputManager.INPUT_STATE_DISCONNECTED,
                        R.plurals.inputs_header_disconnected_input);
            }};

    private TvInputManager mTvInputManager;
    private Resources mRes;
    private Map<String, String> mCustomLabels;
    private Set<String> mHiddenIds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mTvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        mRes = getResources();
        mCustomLabels = TvInputSettings.getCustomLabels(this, UserHandle.USER_OWNER);
        mHiddenIds = TvInputSettings.getHiddenTvInputIds(this, UserHandle.USER_OWNER);
        super.onCreate(savedInstanceState);
    }

    final LayoutGetter mInputListLayout = new LayoutGetter() {
        @Override
        public Layout get() {
            return getExternalTvInputListLayout();
        }
    };

    final LayoutGetter mCecSettingsLayout = new LayoutGetter() {
        @Override
        public Layout get() {
            boolean hdmiControl = readCecOption(getCecOptionKey(ACTION_HDMI_CONTROL));
            boolean deviceAutoOff = readCecOption(getCecOptionKey(ACTION_DEVICE_AUTO_OFF));
            boolean tvAutoOn = readCecOption(getCecOptionKey(ACTION_TV_AUTO_ON));

            return
                new Layout()
                    .add(new Header.Builder(mRes)
                            .title(R.string.inputs_hdmi_control)
                            .description(getOnOffResId(hdmiControl))
                            .detailedDescription(R.string.inputs_hdmi_control_desc)
                            .build()
                        .add(getOnOffLayout(
                                R.string.inputs_hdmi_control,
                                R.string.inputs_hdmi_control_desc,
                                ACTION_HDMI_CONTROL,
                                null,
                                hdmiControl)))
                    .add(new Header.Builder(mRes)
                            .title(R.string.inputs_device_auto_off)
                            .description(getOnOffResId(deviceAutoOff))
                            .detailedDescription(R.string.inputs_device_auto_off_desc)
                            .build()
                        .add(getOnOffLayout(
                                R.string.inputs_device_auto_off,
                                R.string.inputs_device_auto_off_desc,
                                ACTION_DEVICE_AUTO_OFF,
                                null,
                                deviceAutoOff)))
                    .add(new Header.Builder(mRes)
                            .title(R.string.inputs_tv_auto_on)
                            .description(getOnOffResId(tvAutoOn))
                            .detailedDescription(R.string.inputs_tv_auto_on_desc)
                            .build()
                        .add(getOnOffLayout(
                                R.string.inputs_tv_auto_on,
                                R.string.inputs_tv_auto_on_desc,
                                ACTION_TV_AUTO_ON,
                                null,
                                tvAutoOn)));
        }
    };

    @Override
    public Layout createLayout() {
        return
            new Layout()
                .breadcrumb(getString(R.string.header_category_preferences))
                .add(new Header.Builder(mRes)
                        .icon(R.drawable.ic_settings_inputs)
                        .title(R.string.inputs_inputs)
                        .build()
                    .add(mInputListLayout)
                    .add(new Static.Builder(mRes)
                            .title(R.string.inputs_header_cec)
                            .build())
                    .add(new Header.Builder(mRes)
                             .title(R.string.inputs_cec_settings)
                             .build()
                        .add(mCecSettingsLayout))
                    );
    }

    private static Bundle createData(TvInputInfo info) {
        Bundle data = new Bundle();
        data.putString(KEY_ID, info.getId());
        return data;
    }

    private static Bundle createData(TvInputInfo info, String label) {
        Bundle data = createData(info);
        data.putString(KEY_LABEL, label);
        return data;
    }

    private static int getOnOffResId(boolean enabled) {
        return enabled ? R.string.on : R.string.off;
    }

    private LayoutGetter getOnOffLayout(final int titleResId, final int descResId,
            final int action, final Bundle data, final boolean checked) {
        return new LayoutGetter() {
            @Override
            public Layout get() {
                Bundle on = (data == null) ? new Bundle() : new Bundle(data);
                on.putBoolean(KEY_ON, true);
                Bundle off = (data == null) ? new Bundle() : new Bundle(data);
                off.putBoolean(KEY_ON, false);

                Layout layout = new Layout()
                            .add(new Action.Builder(mRes, action)
                                    .title(R.string.on)
                                    .data(on)
                                    .checked(checked)
                                    .build())
                            .add(new Action.Builder(mRes, action)
                                    .title(R.string.off)
                                    .data(off)
                                    .checked(!checked)
                                    .build());

                return layout;
            }
        };
    }

    private LayoutGetter getEditLabelLayout(final TvInputInfo info) {
        return new LayoutGetter() {
            @Override
            public Layout get() {
                String defaultLabel = info.loadLabel(InputsActivity.this).toString();
                String customLabel = mCustomLabels.get(info.getId());
                boolean isHidden = mHiddenIds.contains(info.getId());
                boolean isChecked = false;

                // Add default.
                boolean isDefault = !isHidden && TextUtils.isEmpty(customLabel);
                Layout layout = new Layout()
                    .add(new Action.Builder(mRes, ACTION_EDIT_LABEL)
                        .title(defaultLabel)
                        .data(createData(info, null))
                        .checked(isDefault)
                        .build());
                isChecked |= isDefault;

                // Add pre-defined labels.
                for (int i = 0; i < PREDEFINED_LABEL_RES_IDS.length; i++) {
                    String name = getString(PREDEFINED_LABEL_RES_IDS[i]);
                    boolean checked = !isHidden && name.equals(customLabel);
                    layout.add(new Action.Builder(mRes, ACTION_EDIT_LABEL)
                              .title(name)
                              .data(createData(info, name))
                              .checked(checked)
                              .build());
                    isChecked |= checked;
                }

                // Add hidden.
                layout.add(new Action.Builder(mRes, ACTION_HIDE)
                          .title(R.string.inputs_hide)
                          .description(R.string.inputs_hide_desc)
                          .data(createData(info))
                          .checked(isHidden)
                          .build());
                isChecked |= isHidden;

                // Add custom Label.
                String label =  (isChecked) ? defaultLabel : customLabel;
                layout.add(new Action.Builder(mRes, ACTION_CUSTOM_LABEL)
                          .title(R.string.inputs_custom_name)
                          .data(createData(info, label))
                          .description(label)
                          .checked(!isChecked)
                          .build());

                return layout;
            }
        };
    }

    private Layout getExternalTvInputListLayout() {
        HashMap<Integer, ArrayList<Pair<String, TvInputInfo>>> externalInputs =
                new HashMap<>();
        for (TvInputInfo info : mTvInputManager.getTvInputList()) {
            if (info.getType() != TvInputInfo.TYPE_TUNER &&
                    TextUtils.isEmpty(info.getParentId())) {
                int state;
                try {
                    state = mTvInputManager.getInputState(info.getId());
                } catch (IllegalArgumentException e) {
                    // Input is gone while iterating. Ignore.
                    continue;
                }

                ArrayList<Pair<String, TvInputInfo>> list = externalInputs.get(state);
                if (list == null) {
                    list = new ArrayList<>();
                    externalInputs.put(state, list);
                }
                // Cache label because loadLabel does binder call internally
                // and it would be the sort key.
                list.add(Pair.create(info.loadLabel(this).toString(), info));
            }
        }

        for (ArrayList<Pair<String, TvInputInfo>> list : externalInputs.values()) {
            Collections.sort(list, new Comparator<Pair<String, TvInputInfo>>() {
                @Override
                public int compare(Pair<String, TvInputInfo> a, Pair<String, TvInputInfo> b) {
                    return a.first.compareTo(b.first);
                }
            });
        }

        Layout layout = new Layout();
        for (Map.Entry<Integer, Integer> state : STATE_STRING_ID_MAP.entrySet()) {
            ArrayList<Pair<String, TvInputInfo>> list = externalInputs.get(state.getKey());
            if (list != null && list.size() > 0) {
                String header = mRes.getQuantityString(state.getValue(), list.size());
                layout.add(new Static.Builder(mRes)
                          .title(header)
                          .build());
                for (Pair<String, TvInputInfo> input : list) {
                    String customLabel;
                    if (mHiddenIds.contains(input.second.getId())) {
                        customLabel = getString(R.string.inputs_hide);
                    } else {
                        customLabel = mCustomLabels.get(input.second.getId());
                        if (TextUtils.isEmpty(customLabel)) {
                            customLabel = input.second.loadLabel(this).toString();
                        }
                    }
                    layout.add(new Header.Builder(mRes)
                                  .title(input.first)
                                  .description(customLabel)
                                  .build()
                              .add(getEditLabelLayout(input.second)));
                }
            }
        }

        return layout;
    }

    @Override
    public void onActionClicked(Action action) {
        switch (action.getId()) {
            case ACTION_EDIT_LABEL:
                handleEditLabel(action);
                goBackToTitle(getString(R.string.inputs_inputs));
                break;
            case ACTION_CUSTOM_LABEL:
                displayCustomLabelActivity(action.getData());
                goBackToTitle(getString(R.string.inputs_inputs));
                break;
            case ACTION_HIDE:
                handleHide(action);
                goBackToTitle(getString(R.string.inputs_inputs));
                break;
            case ACTION_HDMI_CONTROL:
            case ACTION_DEVICE_AUTO_OFF:
            case ACTION_TV_AUTO_ON:
                handleCecOption(action);
                goBackToTitle(getString(R.string.inputs_cec_settings));
                break;
        }
    }

    private void handleEditLabel(Action action) {
        String id = action.getData().getString(KEY_ID);
        String name = action.getData().getString(KEY_LABEL);
        saveCustomLabel(id, name);
    }

    private void handleHide(Action action) {
        handleHide(action.getData().getString(KEY_ID), true);
    }

    private void handleHide(String inputId, boolean hide) {
        if (DEBUG) Log.d(TAG, "Hide " + inputId + ": " + hide);

        boolean changed = false;
        if (hide) {
            if (!mHiddenIds.contains(inputId)) {
                mHiddenIds.add(inputId);
                changed = true;
            }
        } else {
            if (mHiddenIds.contains(inputId)) {
                mHiddenIds.remove(inputId);
                changed = true;
            }
        }
        if (changed) {
            TvInputSettings.putHiddenTvInputs(this, mHiddenIds, UserHandle.USER_OWNER);
        }
    }

    private void handleCecOption(Action action) {
        String key = getCecOptionKey(action.getId());
        boolean enabled = action.getData().getBoolean(KEY_ON);
        writeCecOption(key, enabled);
    }

    private void saveCustomLabel(String inputId, String label) {
        if (DEBUG) Log.d(TAG, "Setting " + inputId + " => " + label);

        if (!TextUtils.isEmpty(label)) {
            mCustomLabels.put(inputId, label);
        } else {
            mCustomLabels.remove(inputId);
        }

        TvInputSettings.putCustomLabels(this, mCustomLabels, UserHandle.USER_OWNER);
        handleHide(inputId, false);
    }

    private void displayCustomLabelActivity(Bundle data) {
        Intent intent = new Intent(this, InputsCustomLabelActivity.class);
        intent.putExtra(InputsCustomLabelActivity.KEY_ID, data.getString(KEY_ID));
        intent.putExtra(InputsCustomLabelActivity.KEY_LABEL, data.getString(KEY_LABEL));
        startActivityForResult(intent, REQUEST_CODE_CUSTOM_LABEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CUSTOM_LABEL) {
            if (resultCode == InputsCustomLabelActivity.RESULT_OK) {
                String inputId = data.getStringExtra(InputsCustomLabelActivity.KEY_ID);
                String label = data.getStringExtra(InputsCustomLabelActivity.KEY_LABEL);
                saveCustomLabel(inputId, label);
                goBackToTitle(getString(R.string.inputs_inputs));
            }
        }
    }

    private String getCecOptionKey(int action) {
        switch (action) {
            case ACTION_HDMI_CONTROL:
                return Global.HDMI_CONTROL_ENABLED;
            case ACTION_DEVICE_AUTO_OFF:
                return Global.HDMI_CONTROL_AUTO_DEVICE_OFF_ENABLED;
            case ACTION_TV_AUTO_ON:
                return Global.HDMI_CONTROL_AUTO_WAKEUP_ENABLED;
        }
        return "";
    }

    private static int toInt(boolean enabled) {
        return enabled ? ENABLED : DISABLED;
    }

    private boolean readCecOption(String key) {
        return Global.getInt(getContentResolver(), key, toInt(true)) == ENABLED;
    }

    private void writeCecOption(String key, boolean value) {
        if (DEBUG) {
            Log.d(TAG, "Writing CEC option " + key + " to " + value);
        }
        Global.putInt(getContentResolver(), key, toInt(value));
    }
}
