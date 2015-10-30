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

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tv.settings.R;
import com.android.tv.settings.widget.ScrollAdapterView;
import com.android.tv.settings.widget.ScrollArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class SetDeviceNameFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String ARG_NAME_LIST = "suggested_names";
    private static final String ARG_SHOW_CUSTOM_OPTION = "allow_custom";
    private static final int LAYOUT_MAIN = R.layout.setup_content_area;
    private static final int LAYOUT_DESCRIPTION = R.layout.setup_text_and_description;
    private static final int LAYOUT_ACTION = R.layout.setup_scroll_adapter_view;
    private static final int LAYOUT_LIST_ITEM = R.layout.setup_list_item_text_only;
    private static final int LAYOUT_ITEM_TEXT = R.id.list_item_text;

    public interface SetDeviceNameListener {
        void onDeviceNameSelected(String name);
        void onCustomNameRequested();
    }

    public static SetDeviceNameFragment createInstance(ArrayList<String> names,
                                                    boolean allowCustom) {
        SetDeviceNameFragment frag = new SetDeviceNameFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(SetDeviceNameFragment.ARG_NAME_LIST, names);
        args.putBoolean(SetDeviceNameFragment.ARG_SHOW_CUSTOM_OPTION, allowCustom);
        frag.setArguments(args);
        return frag;
    }

    private ArrayList<String> mOptions;
    private String mCustomRoomString = "";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof SetDeviceNameListener)) {
            throw new IllegalStateException("Activity must be instance of SetDeviceNameLisener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args.containsKey(ARG_NAME_LIST)) {
            mOptions = new ArrayList<>(args.getStringArrayList(ARG_NAME_LIST));
        } else {
            mOptions = new ArrayList<>();
        }

        if (args.getBoolean(ARG_SHOW_CUSTOM_OPTION, false)) {
            mCustomRoomString = getResources().getString(R.string.custom_room);
            mOptions.add(mCustomRoomString);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(LAYOUT_MAIN, null);
        final ViewGroup actionArea = (ViewGroup) view.findViewById(R.id.action);
        final ViewGroup descriptionArea = (ViewGroup) view.findViewById(R.id.description);
        descriptionArea.addView(inflater.inflate(LAYOUT_DESCRIPTION, null));
        final String title = getResources().getString(R.string.select_title);
        ((TextView) descriptionArea.findViewById(R.id.title_text))
                .setText(TextUtils.expandTemplate(title, Build.MODEL));
        final String description = getResources().getString(R.string.select_description);
        ((TextView) descriptionArea.findViewById(R.id.description_text))
                .setText(TextUtils.expandTemplate(description, Build.MODEL));

        final ScrollAdapterView list = (ScrollAdapterView) inflater.inflate(LAYOUT_ACTION, null);
        actionArea.addView(list);
        final Adapter listAdapter =
                new Adapter(getActivity(), LAYOUT_LIST_ITEM, LAYOUT_ITEM_TEXT, mOptions);
        list.setAdapter(listAdapter);
        list.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final SetDeviceNameListener listener = (SetDeviceNameListener) getActivity();
        if (listener == null) {
            return;
        }

        if (isCustomListItem(position)) {
            listener.onCustomNameRequested();
        } else {
            listener.onDeviceNameSelected(mOptions.get(position));
        }
    }

    private class Adapter extends ScrollArrayAdapter<String> {
        private static final int VIEW_TYPE_TEXT = 0;
        private static final int VIEW_TYPE_TEXT_AND_ICON = 1;

        public Adapter(Context context, int resource, int textViewResourceId,
                List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // for a "standard" item, return standard view
            if (!isCustomListItem(position)) {
                return super.getView(position, convertView, parent);
            }

            // for the "other option" draw a custom view that includes an icon
            if (convertView == null) {
                LayoutInflater helium = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = helium.inflate(R.layout.setup_list_item, parent, false);

                // our image view is always going to be the same, so set that here
                ImageView plusIcon = new ImageView(getActivity());
                plusIcon.setImageResource(R.drawable.ic_menu_add);

                ((ViewGroup) convertView.findViewById(R.id.list_item_icon)).addView(plusIcon);
            }

            TextView itemLabel = (TextView) convertView.findViewById(R.id.list_item_text);
            itemLabel.setText(getItem(position));
            return convertView;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).equals(mCustomRoomString) ?
                    VIEW_TYPE_TEXT_AND_ICON : VIEW_TYPE_TEXT;
        }
    }

    private boolean isCustomListItem(int position) {
        return mOptions.get(position).equals(mCustomRoomString);
    }
}
