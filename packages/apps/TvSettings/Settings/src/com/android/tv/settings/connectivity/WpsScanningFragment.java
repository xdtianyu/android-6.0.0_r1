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

package com.android.tv.settings.connectivity;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.tv.settings.R;

/**
 * Displays a UI for showing that WPS is active
 */
public class WpsScanningFragment extends Fragment {

    public static WpsScanningFragment newInstance() {
        return new WpsScanningFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        final View view = inflater.inflate(R.layout.wps_fragment, container, false);
        final TextView titleView = (TextView) view.findViewById(R.id.title);
        titleView.setText(getActivity().getString(R.string.wifi_wps_title));
        final TextView descriptionView = (TextView) view.findViewById(R.id.description);
        descriptionView.setText(getActivity().getString(R.string.wifi_wps_instructions));
        return view;
    }
}
