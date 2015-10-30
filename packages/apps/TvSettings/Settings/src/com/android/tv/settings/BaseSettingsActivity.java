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

package com.android.tv.settings;

import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;

import android.app.Fragment;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Stack;

public abstract class BaseSettingsActivity extends DialogActivity {

    protected Object mState;
    protected final Stack<Object> mStateStack = new Stack<>();
    protected Resources mResources;
    protected Fragment mContentFragment;
    protected Fragment mActionFragment;
    protected ArrayList<Action> mActions;

    /**
     * This method initializes the parameter and sets the initial state. <br/>
     * Activities extending {@link BaseSettingsActivity} should initialize their
     * own local variables, if any, before calling {@link #onCreate}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mResources = getResources();
        mActions = new ArrayList<Action>();
        super.onCreate(savedInstanceState);

        setState(getInitialState(), true);
    }

    protected abstract Object getInitialState();

    protected void setState(Object state, boolean updateStateStack) {
        if (updateStateStack && mState != null) {
            mStateStack.push(mState);
        }
        mState = state;

        updateView();
    }

    protected void setView(int titleResId, int breadcrumbResId, int descResId, int iconResId) {
        String title = titleResId != 0 ? mResources.getString(titleResId) : null;
        String breadcrumb = breadcrumbResId != 0 ? mResources.getString(breadcrumbResId) : null;
        String description = descResId != 0 ? mResources.getString(descResId) : null;
        setView(title, breadcrumb, description, iconResId);
    }

    protected void setView(String title, String breadcrumb, String description, int iconResId) {
        mContentFragment = ContentFragment.newInstance(title, breadcrumb, description, iconResId,
                getResources().getColor(R.color.icon_background));
        mActionFragment = ActionFragment.newInstance(mActions);
        setContentAndActionFragments(mContentFragment, mActionFragment);
    }

    /**
     * Set the view.
     *
     * @param uri Uri of icon resource.
     */
    protected void setView(String title, String breadcrumb, String description, Uri uri) {
        mContentFragment = ContentFragment.newInstance(title, breadcrumb, null, uri,
                getResources().getColor(R.color.icon_background));
        mActionFragment = ActionFragment.newInstance(mActions);
        setContentAndActionFragments(mContentFragment, mActionFragment);
    }

    protected void setView(int titleResId, String breadcrumb, int descResId, Uri uri) {
        String title = titleResId != 0 ? mResources.getString(titleResId) : null;
        String description = descResId != 0 ? mResources.getString(descResId) : null;
        setView(title, breadcrumb, description, uri);
    }

    /**
     * This method is called by {@link #setState}, and updates the layout based
     * on the current state
     */
    protected abstract void updateView();

    /**
     * This method is called to update the contents of mActions to reflect the
     * list of actions for the current state.
     */
    protected abstract void refreshActionList();

    /**
     * This method is used to set boolean properties
     *
     * @param enable whether or not to enable the property
     */
    protected abstract void setProperty(boolean enable);

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void goBack(){
        if (mState.equals(getInitialState())) {
            finish();
        } else if (getPrevState() != null) {
            mState = mStateStack.pop();
            // Using the synchronous version of popBackStack so that we can get
            // the updated
            // instance of the action Fragment on the following line.
            getFragmentManager().popBackStackImmediate();
            mActionFragment = getActionFragment();
            // Update Current State Actions
            if ((mActionFragment != null) && (mActionFragment instanceof ActionFragment)) {
                ActionFragment actFrag = (ActionFragment) mActionFragment;
                refreshActionList();
                ((ActionAdapter) actFrag.getAdapter()).setActions(mActions);
            }
        }
    }

    protected Object getPrevState() {
        return mStateStack.isEmpty() ? null : mStateStack.peek();
    }
}
