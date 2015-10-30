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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.setup.TextInputWizardFragment;
import com.android.tv.settings.util.ThemeHelper;

/**
 * Activity that changes TV input label
 */
public class InputsCustomLabelActivity extends Activity
        implements TextInputWizardFragment.Listener {

    private static final String TAG = "InputsCustomLabelActivity";
    private static final boolean DEBUG = false;

    public static final String KEY_ID = "id";
    public static final String KEY_LABEL = "label";

    private String mId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHelper.getThemeResource(getIntent()));
        setContentView(R.layout.setup_auth_activity);

        Intent intent = getIntent();
        mId = intent.getStringExtra(KEY_ID);
        displayTextInputFragment(
                intent.getStringExtra(KEY_LABEL));
    }

    @Override
    public boolean onTextInputComplete(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }

        Intent intent = new Intent();
        intent.putExtra(KEY_ID, mId);
        intent.putExtra(KEY_LABEL, name);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    /**
     * Show the fragment that allows the user to give a custom name to their device
     */
    private void displayTextInputFragment(String customLabel) {
        // Magically TextInputWizardFragment hopes its enclosing activity is an instance of
        // its listener type, and we are so, onTextInputComplete(String) is automatically
        // called here
        TextInputWizardFragment fragment = TextInputWizardFragment.newInstance(
                getString(R.string.inputs_custom_title),
                null,
                TextInputWizardFragment.INPUT_TYPE_NORMAL,
                customLabel);
        getFragmentManager().beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
    }
}
