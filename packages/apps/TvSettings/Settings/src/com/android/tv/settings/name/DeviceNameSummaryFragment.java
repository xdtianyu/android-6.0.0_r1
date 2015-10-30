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

package com.android.tv.settings.name;

import android.os.Build;
import com.android.tv.settings.R;
import com.android.tv.settings.widget.ScrollAdapterView;
import com.android.tv.settings.widget.ScrollArrayAdapter;

import android.app.Fragment;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Loads the device's current name and displays it. Gives the user the action
 * options that correspond to the ArrayList<String> passed in the
 * {@link #ARG_OPTION_LIST} extra.
 */
public class DeviceNameSummaryFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String TAG = "DeviceNameSummaryFragment";
    private static final String ARG_DESCRIPTION = "text_desc";
    private static final String ARG_TITLE = "text_title";
    private static final String ARG_OPTION_LIST = "options_list";

    /**
     * Creates a DeviceNameSummaryFragment with the correct arguments.
     * @param title The title to be displayed to the user.
     * @param description The description to be displayed to the user.
     * @param options The options to present to the user.
     */
    public static DeviceNameSummaryFragment createInstance(String title, String description,
                                                           ArrayList<String> options) {
        DeviceNameSummaryFragment fragment = new DeviceNameSummaryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putStringArrayList(ARG_OPTION_LIST, options);
        fragment.setArguments(args);
        return fragment;
    }

    private ArrayList<String> mOptions;
    private AdapterView.OnItemClickListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOptions = getArguments().getStringArrayList(ARG_OPTION_LIST);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View content = inflater.inflate(R.layout.setup_content_area, container, false);
        final FrameLayout actionArea = (FrameLayout) content.findViewById(R.id.action);
        final FrameLayout descArea = (FrameLayout) content.findViewById(R.id.description);

        final View body = inflater.inflate(R.layout.setup_text_and_description, descArea, false);

        // Title
        String title = getArguments().getString(ARG_TITLE);
        ((TextView) body.findViewById(R.id.title_text))
                .setText(TextUtils.expandTemplate(title, Build.MODEL));

        // Description
        String descriptionText = getArguments().getString(ARG_DESCRIPTION);
        String deviceName = DeviceManager.getDeviceName(getActivity());
        ((TextView) body.findViewById(R.id.description_text))
                .setText(TextUtils.expandTemplate(descriptionText, deviceName, Build.MODEL));

        descArea.addView(body);

        // Options
        final ScrollAdapterView actionList = (ScrollAdapterView) inflater.inflate(
                R.layout.setup_scroll_adapter_view, actionArea, false);
        ScrollArrayAdapter<String> adapter = new ScrollArrayAdapter<String>(getActivity(),
                R.layout.setup_list_item_text_only, R.id.list_item_text, mOptions);
        actionList.setAdapter(adapter);
        actionList.setOnItemClickListener(this);
        actionArea.addView(actionList);

        return content;
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        // forward the result on
        if (mListener != null) {
            mListener.onItemClick(adapter, view, position, id);
        } else {
            Log.w(TAG, "Action selected, but no listener available.");
        }
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        mListener = listener;
    }
}
