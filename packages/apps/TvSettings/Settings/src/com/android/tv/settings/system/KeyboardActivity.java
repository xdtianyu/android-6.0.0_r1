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

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.BaseSettingsActivity;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;

import android.app.ActivityManagerNative;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KeyboardActivity extends BaseSettingsActivity implements ActionAdapter.Listener {

    private static final String TAG = "KeyboardActivity";
    private static final boolean DEBUG = false;

    private static final String INPUT_METHOD_SEPARATOR = ":";
    private InputMethodManager mInputMan;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mInputMan = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        // enable all available input methods
        enableAllInputMethods();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActionClicked(Action action) {
        /*
         * For list preferences
         */
        String key = action.getKey();
        switch ((ActionType) mState) {
            case KEYBOARD_OVERVIEW_CURRENT_KEYBOARD:
                setInputMethod(key);
                goBack();
                return;
            default:
                break;
        }

        /*
         * For regular states
         */
        ActionKey<ActionType, ActionBehavior> actionKey = new ActionKey<ActionType, ActionBehavior>(
                ActionType.class, ActionBehavior.class, action.getKey());
        final ActionType type = actionKey.getType();
        switch (type) {
            case KEYBOARD_OVERVIEW_CONFIGURE:
                InputMethodInfo currentInputMethodInfo = getCurrentInputMethodInfo();
                if (currentInputMethodInfo != null &&
                        currentInputMethodInfo.getSettingsActivity() != null) {
                    startActivity(getInputMethodSettingsIntent(currentInputMethodInfo));
                }
                return;
            default:
        }

        final ActionBehavior behavior = actionKey.getBehavior();
        if (behavior == null) {
            return;
        }
        switch (behavior) {
            case INIT:
                setState(type, true);
                break;
            case ON:
                setProperty(true);
                break;
            case OFF:
                setProperty(false);
                break;
            default:
        }
    }

    @Override
    protected Object getInitialState() {
        return ActionType.KEYBOARD_OVERVIEW;
    }

    @Override
    protected void refreshActionList() {
        mActions.clear();
        InputMethodInfo currentInputMethodInfo = getCurrentInputMethodInfo();
        switch ((ActionType) mState) {
            case KEYBOARD_OVERVIEW:
                String name = getDefaultInputMethodName();
                mActions.add(ActionType.KEYBOARD_OVERVIEW_CURRENT_KEYBOARD
                        .toAction(mResources, TextUtils.isEmpty(name) ? "" : name));

                if (currentInputMethodInfo != null
                        && currentInputMethodInfo.getSettingsActivity() != null) {
                    mActions.add(ActionType.KEYBOARD_OVERVIEW_CONFIGURE.toAction(mResources));
                }
                break;
            case KEYBOARD_OVERVIEW_CURRENT_KEYBOARD:
                mActions = Action.createActionsFromArrays(getInputMethodIds(),
                        getInputMethodNames());
                for (Action action : mActions) {
                    action.setChecked(currentInputMethodInfo != null &&
                            action.getKey().equals(currentInputMethodInfo.getId()));
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void updateView() {
        refreshActionList();
        switch ((ActionType) mState) {
            case KEYBOARD_OVERVIEW:
                setView(R.string.system_keyboard, R.string.settings_app_name, 0,
                        R.drawable.ic_settings_keyboard);
                break;
            case KEYBOARD_OVERVIEW_CURRENT_KEYBOARD:
                setView(R.string.title_current_keyboard, R.string.system_keyboard, 0,
                        R.drawable.ic_settings_keyboard);
                break;
            default:
                break;
        }
    }

    @Override
    protected void setProperty(boolean enable) {
    }

    private String getDefaultInputMethodId() {
        return Settings.Secure.getString(getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
    }

    private String getDefaultInputMethodName() {
        String defaultInputMethodInfo = getDefaultInputMethodId();

        List<InputMethodInfo> enabledInputMethodInfos = getEnabledSystemInputMethodList();
        for (InputMethodInfo info : enabledInputMethodInfos) {
            if (defaultInputMethodInfo.equals(info.getId())) {
                return info.loadLabel(getPackageManager()).toString();
            }
        }
        return null;
    }

    private InputMethodInfo getCurrentInputMethodInfo() {
        String defaultInputMethodInfo = getDefaultInputMethodId();

        List<InputMethodInfo> enabledInputMethodInfos = getEnabledSystemInputMethodList();
        for (InputMethodInfo info : enabledInputMethodInfos) {
            if (defaultInputMethodInfo.equals(info.getId())) {
                return info;
            }
        }
        return null;
    }

    private String[] getInputMethodNames() {
        List<InputMethodInfo> enabledInputMethodInfos = getEnabledSystemInputMethodList();
        int totalInputMethods = enabledInputMethodInfos.size();
        String[] inputMethodNames = new String[totalInputMethods];
        for (int i = 0; i < totalInputMethods; i++) {
            inputMethodNames[i] = enabledInputMethodInfos.get(i)
                    .loadLabel(getPackageManager()).toString();
        }
        return inputMethodNames;
    }

    private String[] getInputMethodIds() {
        List<InputMethodInfo> enabledInputMethodInfos = getEnabledSystemInputMethodList();
        int totalInputMethods = enabledInputMethodInfos.size();
        String[] inputMethodIds = new String[totalInputMethods];
        for (int i = 0; i < totalInputMethods; i++) {
            inputMethodIds[i] = enabledInputMethodInfos.get(i).getId();
        }
        return inputMethodIds;
    }

    private List<InputMethodInfo> getEnabledSystemInputMethodList() {
        List<InputMethodInfo> enabledInputMethodInfos =
                new ArrayList<>(mInputMan.getEnabledInputMethodList());
        // Filter auxiliary keyboards out
        for (Iterator<InputMethodInfo> it = enabledInputMethodInfos.iterator(); it.hasNext();) {
            if (it.next().isAuxiliaryIme()) {
                it.remove();
            }
        }
        return enabledInputMethodInfos;
    }

    private void setInputMethod(String imid) {
        if (imid == null) {
            throw new IllegalArgumentException("Unknown id: " + imid);
        }

        int userId;
        try {
            userId = ActivityManagerNative.getDefault().getCurrentUser().id;
            Settings.Secure.putStringForUser(getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD, imid, userId);

            if (ActivityManagerNative.isSystemReady()) {
                Intent intent = new Intent(Intent.ACTION_INPUT_METHOD_CHANGED);
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                intent.putExtra("input_method_id", imid);
                sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "set default input method remote exception");
        }
    }

    private void enableAllInputMethods() {
        List<InputMethodInfo> allInputMethodInfos =
                new ArrayList<InputMethodInfo>(mInputMan.getInputMethodList());
        boolean needAppendSeparator = false;
        StringBuilder builder = new StringBuilder();
        for (InputMethodInfo imi : allInputMethodInfos) {
            if (needAppendSeparator) {
                builder.append(INPUT_METHOD_SEPARATOR);
            } else {
                needAppendSeparator = true;
            }
            builder.append(imi.getId());
        }
        Settings.Secure.putString(getContentResolver(), Settings.Secure.ENABLED_INPUT_METHODS,
                builder.toString());
    }

    private Intent getInputMethodSettingsIntent(InputMethodInfo imi) {
        final PackageManager pm = getPackageManager();
        final CharSequence label = imi.loadLabel(pm);// IME settings
        final Intent intent;
        final String settingsActivity = imi.getSettingsActivity();
        if (!TextUtils.isEmpty(settingsActivity)) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(imi.getPackageName(), settingsActivity);
        } else {
            intent = null;
        }
        return intent;
    }
}
