/*
 * Copyright 2015, The Android Open Source Project
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

package com.android.managedprovisioning;

import android.app.Activity;
import android.widget.Button;

import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

/**
 * Base class for setting up the layout.
 */
public abstract class SetupLayoutActivity extends Activity implements NavigationBarListener {

    protected Button mNextButton;
    protected Button mBackButton;

    public static final int NEXT_BUTTON_EMPTY_LABEL = 0;

    public void initializeLayoutParams(int layoutResourceId, int headerResourceId,
            boolean showProgressBar) {
        setContentView(layoutResourceId);
        SetupWizardLayout layout = (SetupWizardLayout) findViewById(R.id.setup_wizard_layout);
        layout.setHeaderText(headerResourceId);
        if (showProgressBar) {
            layout.showProgressBar();
        }
        setupNavigationBar(layout.getNavigationBar());
    }

    private void setupNavigationBar(NavigationBar bar) {
        bar.setNavigationBarListener(this);
        mNextButton = bar.getNextButton();
        mBackButton = bar.getBackButton();
    }

    public void configureNavigationButtons(int nextButtonResourceId, int nextButtonVisibility,
            int backButtonVisibility) {
        if (nextButtonResourceId != NEXT_BUTTON_EMPTY_LABEL) {
            mNextButton.setText(nextButtonResourceId);
        }
        mNextButton.setVisibility(nextButtonVisibility);
        mBackButton.setVisibility(backButtonVisibility);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
    }
}