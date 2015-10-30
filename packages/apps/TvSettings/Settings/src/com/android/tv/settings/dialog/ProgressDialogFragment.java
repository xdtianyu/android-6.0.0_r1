/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tv.settings.dialog;

import android.annotation.DrawableRes;
import android.annotation.Nullable;
import android.annotation.StringRes;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.tv.settings.R;

public class ProgressDialogFragment extends Fragment {

    private ImageView mIconView;
    private TextView mTitleView;
    private TextView mTitleEndView;
    private TextView mSummaryView;
    private ProgressBar mProgressBar;

    @Override
    public @Nullable View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        final ViewGroup view =
                (ViewGroup) inflater.inflate(R.layout.progress_fragment, container, false);

        mIconView = (ImageView) view.findViewById(android.R.id.icon);
        mTitleView = (TextView) view.findViewById(android.R.id.title);
        mTitleEndView = (TextView) view.findViewById(R.id.title_end);
        mSummaryView = (TextView) view.findViewById(android.R.id.summary);
        mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);

        return view;
    }

    public void setIcon(@DrawableRes int resId) {
        mIconView.setImageResource(resId);
        mIconView.setVisibility(View.VISIBLE);
    }

    public void setIcon(@Nullable Drawable icon) {
        mIconView.setImageDrawable(icon);
        mIconView.setVisibility(icon == null ? View.GONE : View.VISIBLE);
    }

    public void setTitle(@StringRes int resId) {
        mTitleView.setText(resId);
    }

    public void setTitle(CharSequence title) {
        mTitleView.setText(title);
    }

    public void setTitleEnd(@StringRes int resId) {
        mTitleEndView.setText(resId);
    }

    public void setTitleEnd(CharSequence title) {
        mTitleEndView.setText(title);
        mTitleEndView.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);
    }

    public void setSummary(@StringRes int resId) {
        mSummaryView.setText(resId);
    }

    public void setSummary(CharSequence title) {
        mSummaryView.setText(title);
    }

    public void setIndeterminte(boolean indeterminte) {
        mProgressBar.setIndeterminate(indeterminte);
    }

    public void setProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    public void setProgressMax(int max) {
        mProgressBar.setMax(max);
    }
}
