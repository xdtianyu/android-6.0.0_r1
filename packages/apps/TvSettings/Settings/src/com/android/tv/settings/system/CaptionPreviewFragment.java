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

import com.android.tv.settings.R;

import android.app.Fragment;
import android.app.Activity;
import android.os.Bundle;

import com.android.internal.widget.SubtitleView;

import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.Log;

import java.util.Locale;
import android.text.TextUtils;


/**
 * Fragment that shows caption preview image with text overlay.
 */
public class CaptionPreviewFragment extends Fragment {

    private static final String TAG = "CaptionPreviewFragment";
    private static final boolean DEBUG = false;

    private int mDefaultFontSize;

    private SubtitleView mPreviewText;
    private View mPreviewWindow;
    private CaptioningManager mCaptioningManager;
    private CaptioningManager.CaptioningChangeListener mCaptionChangeListener;

    private float mFontScale;
    private int mStyleId;
    private int mTextColor;
    private int mBackgroundColor;
    private int mWindowColor;
    private Locale mLocale;
    private boolean mShowingLivePreview;

    public static CaptionPreviewFragment newInstance() {
        CaptionPreviewFragment fragment = new CaptionPreviewFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mCaptioningManager = (CaptioningManager) getActivity()
                .getSystemService(Context.CAPTIONING_SERVICE);

        mCaptionChangeListener =
            new CaptioningManager.CaptioningChangeListener() {

                @Override
                public void onEnabledChanged(boolean enabled) {
                    if(DEBUG) Log.d(TAG, "onEnableChanged");
                    refreshPreviewText();
                }

                @Override
                public void onUserStyleChanged(CaptionStyle userStyle) {
                    if(DEBUG) Log.d(TAG, "onUserStyleChanged");
                    loadCaptionSettings();
                    refreshPreviewText();
                }

                @Override
                public void onLocaleChanged(Locale locale) {
                    if(DEBUG) Log.d(TAG, "onLocaleChanged");
                    loadCaptionSettings();
                    refreshPreviewText();
                }

                @Override
                public void onFontScaleChanged(float fontScale) {
                    if(DEBUG) Log.d(TAG, "onFontScaleChanged " + fontScale);
                    loadCaptionSettings();
                    refreshPreviewText();
                }
            };

        mDefaultFontSize =
            getResources().getInteger(R.integer.captioning_preview_default_font_size);
        loadCaptionSettings();
    }

    @Override
    public void onStart() {
        super.onStart();
        mCaptioningManager.addCaptioningChangeListener (mCaptionChangeListener);
    }

    @Override
    public void onStop() {
        mCaptioningManager.removeCaptioningChangeListener (mCaptionChangeListener);
        super.onStop();
    }

    private void loadCaptionSettings() {
        mFontScale = mCaptioningManager.getFontScale();
        mStyleId = mCaptioningManager.getRawUserStyle();
        mLocale = mCaptioningManager.getLocale();

        CaptioningManager.CaptionStyle cs = mCaptioningManager.getUserStyle();
        mTextColor = cs.foregroundColor;
        mBackgroundColor = cs.backgroundColor;
        mWindowColor = cs.windowColor;

        mShowingLivePreview = false;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "onCreateView");
        final View rootView = inflater.inflate(R.layout.captioning_preview, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        mPreviewText = (SubtitleView) view.findViewById(R.id.preview_text);
        mPreviewWindow = view.findViewById(R.id.preview_window);
        refreshPreviewText();
    }

    static CharSequence getTextForLocale(Context context, Locale locale, int resId) {
        final Resources res = context.getResources();
        final Configuration config = res.getConfiguration();
        final Locale prevLocale = config.locale;
        try {
            config.locale = locale;
            res.updateConfiguration(config, null);
            return res.getText(resId);
        } finally {
            config.locale = prevLocale;
            res.updateConfiguration(config, null);
        }
    }

    public void livePreviewLanguage(String language) {
        mLocale = null;
        if (!TextUtils.isEmpty(language)) {
            final String[] splitLocale = language.split("_");
            switch (splitLocale.length) {
                case 3:
                    mLocale = new Locale(splitLocale[0], splitLocale[1], splitLocale[2]);
                    break;
                case 2:
                    mLocale = new Locale(splitLocale[0], splitLocale[1]);
                    break;
                case 1:
                    mLocale = new Locale(splitLocale[0]);
                    break;
            }
        }
        mShowingLivePreview = true;
        refreshPreviewText();
    }

    public void livePreviewFontScale(float fontScale) {
        mFontScale = fontScale;
        mShowingLivePreview = true;
        refreshPreviewText();
    }

    public void livePreviewCaptionStyle(int styleId) {
        mStyleId = styleId;
        mShowingLivePreview = true;
        refreshPreviewText();
    }

    public void livePreviewFontFamily(String fontFamily) {
        Typeface typeface = Typeface.create (fontFamily, Typeface.NORMAL);
        mPreviewText.setTypeface(typeface);
        mShowingLivePreview = true;
    }

    public void livePreviewTextColor(int textColor) {
        int color = mTextColor & 0xff000000 | textColor & 0xffffff;
        mPreviewText.setForegroundColor(color);
        mShowingLivePreview = true;
    }

    public void livePreviewTextOpacity(String textOpacity) {
        int opacity = Integer.parseInt(textOpacity);
        int color = mTextColor & 0xffffff | opacity & 0xff000000;
        mPreviewText.setForegroundColor(color);
        mShowingLivePreview = true;
    }

    public void livePreviewBackgroundColor(int bgColor) {
        int color = mBackgroundColor & 0xff000000 | bgColor & 0xffffff;
        mPreviewText.setBackgroundColor(color);
        mShowingLivePreview = true;
    }

    public void livePreviewBackgroundOpacity(String bgOpacity) {
        int opacity = Integer.parseInt(bgOpacity);
        int color = mBackgroundColor & 0xffffff | opacity & 0xff000000;
        mPreviewText.setBackgroundColor(color);
        mShowingLivePreview = true;
    }

    public void livePreviewEdgeColor(int edgeColor) {
        edgeColor |= 0xff000000;
        mPreviewText.setEdgeColor(edgeColor);
        mShowingLivePreview = true;
    }

    public void livePreviewEdgeType(int edgeType) {
        mPreviewText.setEdgeType(edgeType);
        mShowingLivePreview = true;
    }

    public void livePreviewWindowColorNone() {
        final CaptionStyle defStyle = CaptionStyle.DEFAULT;
        mPreviewWindow.setBackgroundColor(defStyle.windowColor);
    }

    public void livePreviewWindowColor(int windowColor) {
        int opacity = mWindowColor & 0xff000000;
        if (opacity == 0)
            opacity = 0xff000000;
        int color = opacity | windowColor & 0xffffff;
        mPreviewWindow.setBackgroundColor(color);
        mShowingLivePreview = true;
    }

    public void livePreviewWindowOpacity(String windowOpacity) {
        int opacity = Integer.parseInt(windowOpacity);
        int color = mWindowColor & 0xffffff | opacity & 0xff000000;
        mPreviewWindow.setBackgroundColor(color);
        mShowingLivePreview = true;
    }

    public void resetLivePreview() {
        if (mShowingLivePreview) {
            loadCaptionSettings();
            refreshPreviewText();
        }
    }

    public void refreshPreviewText() {
        if(DEBUG) Log.d(TAG, "refreshPreviewText");
        if (mPreviewText != null) {
            boolean enabled = mCaptioningManager.isEnabled();
            if (enabled) {
                mPreviewText.setVisibility(View.VISIBLE);
                Activity activity = getActivity();
                mPreviewText.setStyle(mStyleId);
                mPreviewText.setTextSize(mFontScale * mDefaultFontSize);
                if (mLocale != null) {
                    CharSequence localizedText = getTextForLocale(
                            activity, mLocale, R.string.captioning_preview_text);
                    mPreviewText.setText(localizedText);
                } else {
                    mPreviewText.setText(getResources()
                        .getString(R.string.captioning_preview_text));
                }

                final CaptionStyle style = mCaptioningManager.getUserStyle();
                if (style.hasWindowColor()) {
                    mPreviewWindow.setBackgroundColor(mWindowColor);
                } else {
                    final CaptionStyle defStyle = CaptionStyle.DEFAULT;
                    mPreviewWindow.setBackgroundColor(defStyle.windowColor);
                }

                mPreviewText.invalidate();
            } else {
                mPreviewText.setVisibility(View.INVISIBLE);
            }
        }
    }
}
