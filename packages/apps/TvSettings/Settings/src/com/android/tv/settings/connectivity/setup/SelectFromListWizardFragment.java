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

package com.android.tv.settings.connectivity.setup;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tv.settings.R;
import com.android.tv.settings.connectivity.WifiSecurity;
import com.android.tv.settings.util.AccessibilityHelper;
import com.android.tv.settings.widget.ScrollAdapterView;
import com.android.tv.settings.widget.ScrollArrayAdapter;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Displays a UI for selecting a wifi network from a list in the "wizard" style.
 */
public class SelectFromListWizardFragment extends Fragment {

    private static final String TAG = "SelectFromListWizardFragment";
    private static final boolean DEBUG = false;

    public static class ListItemComparator implements Comparator<ListItem> {
        @Override
        public int compare(ListItem o1, ListItem o2) {
            ScanResult o1ScanResult = o1.getScanResult();
            ScanResult o2ScanResult = o2.getScanResult();
            if (o1ScanResult == null) {
                if (o2ScanResult == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (o2ScanResult == null) {
                    return -1;
                } else {
                    int levelDiff = o2ScanResult.level - o1ScanResult.level;
                    if (levelDiff != 0) {
                        return levelDiff;
                    }
                    return o1ScanResult.SSID.compareTo(o2ScanResult.SSID);
                }
            }
        }
    }

    public static class ListItem implements Parcelable {

        private final String mName;
        private final int mIconResource;
        private final int mIconLevel;
        private final boolean mHasIconLevel;
        private final ScanResult mScanResult;

        public ListItem(String name, int iconResource) {
            mName = name;
            mIconResource = iconResource;
            mIconLevel = 0;
            mHasIconLevel = false;
            mScanResult = null;
        }

        public ListItem(ScanResult scanResult) {
            mName = scanResult.SSID;
            mIconResource = WifiSecurity.isOpen(scanResult) ? R.drawable.setup_wifi_signal_open
                    : R.drawable.setup_wifi_signal_lock;
            mIconLevel = WifiManager.calculateSignalLevel(scanResult.level, 4);
            mHasIconLevel = true;
            mScanResult = scanResult;
        }

        public String getName() {
            return mName;
        }

        int getIconResource() {
            return mIconResource;
        }

        int getIconLevel() {
            return mIconLevel;
        }

        boolean hasIconLevel() {
            return mHasIconLevel;
        }

        ScanResult getScanResult() {
            return mScanResult;
        }

        @Override
        public String toString() {
            return mName;
        }

        public static Parcelable.Creator<ListItem> CREATOR = new Parcelable.Creator<ListItem>() {

            @Override
            public ListItem createFromParcel(Parcel source) {
                ScanResult scanResult = (ScanResult) source.readParcelable(
                        ScanResult.class.getClassLoader());
                if (scanResult == null) {
                    return new ListItem(source.readString(), source.readInt());
                } else {
                    return new ListItem(scanResult);
                }
            }

            @Override
            public ListItem[] newArray(int size) {
                return new ListItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(mScanResult, flags);
            if (mScanResult == null) {
                dest.writeString(mName);
                dest.writeInt(mIconResource);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ListItem) {
                ListItem li = (ListItem) o;
                if (mScanResult == null && li.mScanResult == null) {
                    return mName.equals(li.mName);
                }
                return (mScanResult != null && li.mScanResult != null && mName.equals(li.mName) &&
                        WifiSecurity.getSecurity(mScanResult)
                        == WifiSecurity.getSecurity(li.mScanResult));
            }
            return false;
        }
    }

    public interface Listener {
        void onListSelectionComplete(ListItem listItem);
        void onListFocusChanged(ListItem listItem);
    }

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_DESCRIPTION = "description";
    private static final String EXTRA_LIST_ELEMENTS = "list_elements";
    private static final String EXTRA_LAST_SELECTION = "last_selection";

    public static SelectFromListWizardFragment newInstance(String title, String description,
            ArrayList<ListItem> listElements, ListItem lastSelection) {
        SelectFromListWizardFragment fragment = new SelectFromListWizardFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_DESCRIPTION, description);
        args.putParcelableArrayList(EXTRA_LIST_ELEMENTS, listElements);
        args.putParcelable(EXTRA_LAST_SELECTION, lastSelection);
        fragment.setArguments(args);
        return fragment;
    }

    private Handler mHandler;
    private View mMainView;
    private ScrollArrayAdapter<ListItem> mAdapter;
    private ScrollAdapterView mScrollAdapterView;
    private ArrayList<ListItem> mListItems;
    private ListItem mLastSelected;

    public void update(ArrayList<ListItem> listElements) {
        ListItem lastSelection = mLastSelected;
        mListItems = listElements;
        mAdapter.clear();
        mAdapter.addAll(listElements);
        if (lastSelection != null) {
            for (int i = 0, size = listElements.size(); i < size; i++) {
                if (lastSelection.equals(listElements.get(i))) {
                    if (DEBUG) {
                        Log.d(TAG, "Found " + lastSelection.getName() + " with ssid "
                                + (mLastSelected.getScanResult() == null ? null
                                        : mLastSelected.getScanResult().SSID)
                                + " and bssid " + (mLastSelected.getScanResult() == null ? null
                                        : mLastSelected.getScanResult().BSSID));
                    }
                    mScrollAdapterView.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        mHandler = new Handler();
        mMainView = inflater.inflate(R.layout.account_content_area, container, false);

        final ViewGroup descriptionArea = (ViewGroup) mMainView.findViewById(R.id.description);
        final View content = inflater.inflate(R.layout.wifi_content, descriptionArea, false);
        descriptionArea.addView(content);

        final ViewGroup actionArea = (ViewGroup) mMainView.findViewById(R.id.action);
        mScrollAdapterView = (ScrollAdapterView) inflater.inflate(
                R.layout.setup_scroll_adapter_view, actionArea, false);
        actionArea.addView(mScrollAdapterView);

        TextView titleText = (TextView) content.findViewById(R.id.title_text);
        TextView descriptionText = (TextView) content.findViewById(R.id.description_text);

        Bundle args = getArguments();
        String title = args.getString(EXTRA_TITLE);
        String description = args.getString(EXTRA_DESCRIPTION);
        mListItems = args.getParcelableArrayList(EXTRA_LIST_ELEMENTS);
        ListItem lastSelection = args.getParcelable(EXTRA_LAST_SELECTION);

        mAdapter = new ScrollArrayAdapter<ListItem>(getActivity(), R.layout.setup_list_item,
                R.id.list_item_text, mListItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ListItem item = getItem(position);
                FrameLayout iconHolder = (FrameLayout) v.findViewById(R.id.list_item_icon);
                iconHolder.removeAllViews();
                Activity a = getActivity();
                if (a != null) {
                    ImageView icon = new ImageView(a);
                    icon.setImageResource(item.getIconResource());
                    if (item.hasIconLevel()) {
                        icon.setImageLevel(item.getIconLevel());
                    }
                    iconHolder.addView(icon);
                }
                return v;
            }
        };
        mScrollAdapterView.setAdapter(mAdapter);

        boolean forceFocusable = AccessibilityHelper.forceFocusableViews(getActivity());
        if (title != null) {
            titleText.setText(title);
            titleText.setVisibility(View.VISIBLE);
            if (forceFocusable) {
                titleText.setFocusable(true);
                titleText.setFocusableInTouchMode(true);
            }
        } else {
            titleText.setVisibility(View.GONE);
        }

        if (description != null) {
            descriptionText.setText(description);
            descriptionText.setVisibility(View.VISIBLE);
            if (forceFocusable) {
                descriptionText.setFocusable(true);
                descriptionText.setFocusableInTouchMode(true);
            }
        } else {
            descriptionText.setVisibility(View.GONE);
        }

        if (lastSelection != null) {
            for (int i = 0, size = mListItems.size(); i < size; i++) {
                if (lastSelection.equals(mListItems.get(i))) {
                    mScrollAdapterView.setSelection(i);
                    break;
                }
            }
        }

        mScrollAdapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> pare, View view, int position, long id) {
                Activity a = getActivity();
                if (a instanceof Listener) {
                    ((Listener) a).onListSelectionComplete(mListItems.get(position));
                }
            }
        });

        mScrollAdapterView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mLastSelected = mListItems.get(position);
                if (DEBUG) {
                    Log.d(TAG, "Scrolled to " + mLastSelected.getName() + " with ssid "
                            + (mLastSelected.getScanResult() == null ? null
                                    : mLastSelected.getScanResult().SSID) + " and bssid "
                            + (mLastSelected.getScanResult() == null ? null
                                    : mLastSelected.getScanResult().BSSID));
                }
                Activity a = getActivity();
                if (a instanceof Listener) {
                    ((Listener) a).onListFocusChanged(mLastSelected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }

        });

        return mMainView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(
                        mMainView.getApplicationWindowToken(), 0);
            }
        });
    }
}
