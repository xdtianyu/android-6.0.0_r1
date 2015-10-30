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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.DialogFragment;
import com.android.tv.settings.dialog.DialogFragment.Action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Handles all aspects of DialogFragment Actions for setting individual app restrictions.
 */
class AppRestrictionsManager implements Action.Listener {

    interface Listener {
        void onRestrictionActionsLoaded(String packageName, ArrayList<Action> actions);
    }

    private static final boolean DEBUG = false;
    private static final String TAG = "RestrictedProfile";

    private static final String EXTRA_PACKAGE_NAME = "AppRestrictionsManager.package_name";
    private static final String EXTRA_RESTRICTIONS = "AppRestrictionsManager.restrictions";
    private static final String EXTRA_USER_HANDLE = "user_handle";
    private static final String EXTRA_ENABLED = "enabled";
    private static final String EXTRA_RESTRICTION_KEY = "restriction_key";
    private static final String EXTRA_CHOICE_VALUE = "choice_value";
    private static final String ACTION_CUSTOM_CONFIGURATION = "action_custom_configuration";
    private static final String ACTION_TRUE = "action_true";
    private static final String ACTION_FALSE = "action_false";
    private static final String ACTION_CHANGE_RESTRICTION = "action_change_restriction";
    private static final String ACTION_CHOICE = "action_choice";
    private static final String ACTION_MULTI_CHOICE = "action_multi_choice";
    private static final int MUTUALLY_EXCLUSIVE_CHOICE_CHECK_SET_ID = 1;

    private final Fragment mFragment;
    private final UserManager mUserManager;
    private final UserHandle mUserHandle;
    private final String mPackageName;
    private final Listener mListener;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        private static final String CUSTOM_RESTRICTIONS_INTENT = Intent.EXTRA_RESTRICTIONS_INTENT;

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle results = getResultExtras(true);
            mRestrictions = results.getParcelableArrayList(Intent.EXTRA_RESTRICTIONS_LIST);
            mRestrictionsIntent = results.getParcelable(CUSTOM_RESTRICTIONS_INTENT);

            if (mRestrictions != null && mRestrictionsIntent == null) {
                saveRestrictions();
            } else if (mRestrictionsIntent != null) {
                getRestrictionActions();
            }
        }
    };

    private ArrayList<RestrictionEntry> mRestrictions;
    private Intent mRestrictionsIntent;
    private DialogFragment mCurrentDialogFragment;

    /**
     * @param fragment THe fragment for which DialogFragment actions are being managed.
     * @param userHandle the user whose app restrictions are being set.
     * @param userManager the user manager for setting app restrictions.
     * @param packageName settings' package name (for special case restrictions).
     * @param listener a listener for when restriction actions are ready to be displayed.
     */
    AppRestrictionsManager(Fragment fragment, UserHandle userHandle, UserManager userManager,
            String packageName, Listener listener) {
        mFragment = fragment;
        mUserHandle = userHandle;
        mUserManager = userManager;
        mPackageName = packageName;
        mListener = listener;
    }

    void loadRestrictionActions() {
        if (mPackageName.equals(mFragment.getActivity().getPackageName())) {
            // Settings, fake it by using user restrictions
            mRestrictions = RestrictionUtils.getRestrictions(mFragment.getActivity(), mUserHandle);
            mRestrictionsIntent = null;
            getRestrictionActions();
        } else {
            requestRestrictionsForApp();
        }
    }

    @Override
    public void onActionClicked(Action action) {
        String restrictionEntryKey = action.getIntent().getStringExtra(EXTRA_RESTRICTION_KEY);
        RestrictionEntry entry = restrictionEntryKey != null ? findRestriction(restrictionEntryKey)
                : null;

        if (ACTION_CUSTOM_CONFIGURATION.equals(action.getKey())) {
            mFragment.startActivityForResult(action.getIntent(), 1);
        } else if (ACTION_CHANGE_RESTRICTION.equals(action.getKey())) {
            ArrayList<Action> actions = new ArrayList<>();
            switch (entry.getType()) {
                case RestrictionEntry.TYPE_BOOLEAN:
                    actions.add(new Action.Builder()
                            .key(ACTION_TRUE)
                            .title(Boolean.toString(true))
                            .checked(entry.getSelectedState())
                            .checkSetId(MUTUALLY_EXCLUSIVE_CHOICE_CHECK_SET_ID)
                            .intent(action.getIntent())
                            .build());
                    actions.add(new Action.Builder()
                            .key(ACTION_FALSE)
                            .title(Boolean.toString(false))
                            .checked(!entry.getSelectedState())
                            .checkSetId(MUTUALLY_EXCLUSIVE_CHOICE_CHECK_SET_ID)
                            .intent(action.getIntent())
                            .build());
                    break;
                case RestrictionEntry.TYPE_CHOICE:
                case RestrictionEntry.TYPE_CHOICE_LEVEL:
                {
                    String value = entry.getSelectedString();
                    if (value == null) {
                        value = entry.getDescription();
                    }
                    String[] choiceEntries = entry.getChoiceEntries();
                    String[] choiceValues = entry.getChoiceValues();
                    boolean useValue = (choiceEntries == null
                            || choiceEntries.length != choiceValues.length);
                    for (int i = 0; i < choiceValues.length; i++) {
                        String choiceValue = choiceValues[i];
                        String title = useValue ? choiceValue : choiceEntries[i];
                        Intent intent = new Intent(action.getIntent());
                        intent.putExtra(EXTRA_CHOICE_VALUE, choiceValue);
                        actions.add(new Action.Builder()
                                .key(ACTION_CHOICE)
                                .title(title)
                                .checked(choiceValue.equals(value))
                                .checkSetId(MUTUALLY_EXCLUSIVE_CHOICE_CHECK_SET_ID)
                                .intent(intent)
                                .build());
                    }
                }
                    break;
                case RestrictionEntry.TYPE_MULTI_SELECT:
                {
                    List<String> selectedChoiceValues = Arrays.asList(
                            entry.getAllSelectedStrings());
                    String[] choiceEntries = entry.getChoiceEntries();
                    String[] choiceValues = entry.getChoiceValues();
                    boolean useValues = (choiceEntries == null
                            || choiceEntries.length != choiceValues.length);
                    for (int i = 0; i < choiceValues.length; i++) {
                        String choiceValue = choiceValues[i];
                        String title = useValues ? choiceValue : choiceEntries[i];
                        Intent intent = new Intent(action.getIntent());
                        intent.putExtra(EXTRA_CHOICE_VALUE, choiceValue);
                        actions.add(new Action.Builder()
                                .key(ACTION_MULTI_CHOICE)
                                .title(title)
                                .checked(selectedChoiceValues.contains(choiceValue))
                                .intent(intent)
                                .build());
                    }
                }
                    break;
                case RestrictionEntry.TYPE_NULL:
                default:
            }

            if (!actions.isEmpty()) {
                mCurrentDialogFragment = new DialogFragment.Builder()
                        .title(entry.getTitle())
                        .description(entry.getDescription())
                        .iconResourceId(RestrictedProfileDialogFragment.getIconResource())
                        .iconBackgroundColor(
                                mFragment.getActivity().getResources()
                                        .getColor(R.color.icon_background))
                        .actions(actions).build();
                mCurrentDialogFragment.setListener(this);
                DialogFragment.add(mFragment.getFragmentManager(), mCurrentDialogFragment);

            }
        } else if (ACTION_TRUE.equals(action.getKey())) {
            entry.setSelectedState(true);
            saveRestrictions();
        } else if (ACTION_FALSE.equals(action.getKey())) {
            entry.setSelectedState(false);
            saveRestrictions();
        } else if (ACTION_CHOICE.equals(action.getKey())) {
            entry.setSelectedString(getChoiceValue(action));
            saveRestrictions();
        } else if (ACTION_MULTI_CHOICE.equals(action.getKey())) {

            action.setChecked(!action.isChecked());
            if (mCurrentDialogFragment != null) {
                mCurrentDialogFragment.setActions(mCurrentDialogFragment.getActions());
            }
            HashSet<String> selectedChoiceValues = new HashSet<>();
            selectedChoiceValues.addAll(Arrays.asList(entry.getAllSelectedStrings()));

            if (action.isChecked()) {
                selectedChoiceValues.add(getChoiceValue(action));
            } else {
                selectedChoiceValues.remove(getChoiceValue(action));
            }
            entry.setAllSelectedStrings(
                    selectedChoiceValues.toArray(new String[selectedChoiceValues.size()]));
            saveRestrictions();
        }
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            ArrayList<RestrictionEntry> list =
                    data.getParcelableArrayListExtra(Intent.EXTRA_RESTRICTIONS_LIST);
            Bundle bundle = data.getBundleExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE);
            if (list != null) {
                // If there's a valid result, persist it to the user manager.
                mUserManager.setApplicationRestrictions(mPackageName,
                        RestrictionUtils.restrictionsToBundle(list), mUserHandle);
            } else if (bundle != null) {
                // If there's a valid result, persist it to the user manager.
                mUserManager.setApplicationRestrictions(mPackageName, bundle, mUserHandle);
            }
            loadRestrictionActions();
        }
    }

    private RestrictionEntry findRestriction(String key) {
        for (RestrictionEntry entry : mRestrictions) {
            if (entry.getKey().equals(key)) {
                return entry;
            }
        }
        Log.wtf(TAG, "Couldn't find the restriction to set!");
        return null;
    }

    private String getChoiceValue(Action action) {
        return action.getIntent().getStringExtra(EXTRA_CHOICE_VALUE);
    }

    /**
     * Send a broadcast to the app to query its restrictions
     */
    private void requestRestrictionsForApp() {
        Bundle oldEntries = mUserManager.getApplicationRestrictions(mPackageName, mUserHandle);
        Intent intent = new Intent(Intent.ACTION_GET_RESTRICTION_ENTRIES);
        intent.setPackage(mPackageName);
        intent.putExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE, oldEntries);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        mFragment.getActivity().sendOrderedBroadcast(intent, null, mBroadcastReceiver, null,
                Activity.RESULT_OK, null, null);
    }

    private void getRestrictionActions() {
        ArrayList<Action> actions = new ArrayList<>();

        if (mRestrictionsIntent != null) {
            actions.add(new Action.Builder()
                    .key(ACTION_CUSTOM_CONFIGURATION)
                    .title(mFragment.getActivity().getString(
                            R.string.restricted_profile_customize_restrictions))
                    .intent(mRestrictionsIntent)
                    .hasNext(true)
                    .build());
        }

        if (mRestrictions != null) {
            for (RestrictionEntry entry : mRestrictions) {
                Intent data = new Intent().putExtra(EXTRA_RESTRICTION_KEY, entry.getKey());
                switch (entry.getType()) {
                    case RestrictionEntry.TYPE_BOOLEAN:
                        actions.add(new Action.Builder()
                                .key(ACTION_CHANGE_RESTRICTION)
                                .title(entry.getTitle())
                                .description(mFragment.getActivity().getString(
                                        R.string.restriction_description, entry.getDescription(),
                                        Boolean.toString(entry.getSelectedState())))
                                .hasNext(mRestrictionsIntent == null)
                                .infoOnly(mRestrictionsIntent != null)
                                .intent(data)
                                .multilineDescription(true)
                                .build());
                        break;
                    case RestrictionEntry.TYPE_CHOICE:
                    case RestrictionEntry.TYPE_CHOICE_LEVEL:
                        String value = entry.getSelectedString();
                        if (value == null) {
                            value = entry.getDescription();
                        }
                        actions.add(new Action.Builder()
                                .key(ACTION_CHANGE_RESTRICTION)
                                .title(entry.getTitle())
                                .description(mFragment.getActivity().getString(
                                        R.string.restriction_description,
                                        entry.getDescription(), findInArray(
                                                entry.getChoiceEntries(), entry.getChoiceValues(),
                                                value)))
                                .hasNext(mRestrictionsIntent == null)
                                .infoOnly(mRestrictionsIntent != null)
                                .intent(data)
                                .multilineDescription(true)
                                .build());
                        break;
                    case RestrictionEntry.TYPE_MULTI_SELECT:
                        actions.add(new Action.Builder()
                                .key(ACTION_CHANGE_RESTRICTION)
                                .title(entry.getTitle())
                                .description(mFragment.getActivity().getString(
                                        R.string.restriction_description,
                                        entry.getDescription(), TextUtils.join(
                                                ", ", findSelectedStrings(entry.getChoiceEntries(),
                                                        entry.getChoiceValues(),
                                                        entry.getAllSelectedStrings()))))
                                .hasNext(mRestrictionsIntent == null)
                                .infoOnly(mRestrictionsIntent != null)
                                .intent(data)
                                .multilineDescription(true)
                                .build());
                        break;
                    case RestrictionEntry.TYPE_NULL:
                    default:
                }
            }
        }

        mListener.onRestrictionActionsLoaded(mPackageName, actions);
    }

    private String findInArray(String[] choiceEntries, String[] choiceValues,
            String selectedString) {
        if (choiceEntries == null || choiceValues.length != choiceEntries.length) {
            return selectedString;
        }
        for (int i = 0; i < choiceValues.length; i++) {
            if (choiceValues[i].equals(selectedString)) {
                return choiceEntries[i];
            }
        }
        return selectedString;
    }

    private String[] findSelectedStrings(String[] choiceEntries, String[] choiceValues,
            String[] selectedStrings) {
        if (choiceEntries == null || choiceValues.length != choiceEntries.length) {
            return selectedStrings;
        }
        String[] selectedStringsMapped = new String[selectedStrings.length];
        for (int i = 0; i < selectedStrings.length; i++) {
            selectedStringsMapped[i] = selectedStrings[i];
            for (int j = 0; j < choiceValues.length; j++) {
                if (choiceValues[j].equals(selectedStrings[i])) {
                    selectedStringsMapped[i] = choiceEntries[j];
                }
            }
        }
        return selectedStringsMapped;
    }

    private void saveRestrictions() {
        getRestrictionActions();
        if (mPackageName.equals(mFragment.getActivity().getPackageName())) {
            RestrictionUtils.setRestrictions(mFragment.getActivity(), mRestrictions, mUserHandle);
        } else {
            Bundle bundle = RestrictionUtils.restrictionsToBundle(mRestrictions);
            mUserManager.setApplicationRestrictions(mPackageName, bundle, mUserHandle);
        }
    }
}
