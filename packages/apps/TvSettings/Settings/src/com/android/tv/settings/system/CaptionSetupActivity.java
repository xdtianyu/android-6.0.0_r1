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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.CaptioningManager;

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.BaseSettingsActivity;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class CaptionSetupActivity extends BaseSettingsActivity implements ActionAdapter.Listener,
                                  ActionAdapter.OnFocusListener {

    private static final String TAG = "CaptionSetupActivity";
    private static final boolean DEBUG = false;

    private Resources mResources;
    private boolean mDisplayEnabled;
    private String mNone;

    private String mLanguage;
    private String mLanguageName;
    private String[] mLanguageLocales;
    private String[] mLanguageNames;

    private String mTextSize;
    private String mTextSizeName;
    private String[] mTextSizes;
    private String[] mTextSizeNames;

    private String mStyle;
    private String mStyleName;
    private String[] mStyles;
    private String[] mStyleNames;

    private String mFontFamily;
    private String mFontFamilyName;
    private String[] mFontFamilies;
    private String[] mFontFamilyNames;

    private int[] mColorResIds;
    private int[] mColorRGBs;
    private String[] mColorNames;

    private int mTextColor;
    private String mTextColorName;

    private String mEdgeType;
    private String mEdgeTypeName;
    private String[] mEdgeTypes;
    private String[] mEdgeTypeNames;

    private int mEdgeColor;
    private String mEdgeColorName;

    private int mBackgroundColor;
    private String mBackgroundColorName;

    private String mBackgroundOpacity;
    private String mBackgroundOpacityName;
    private String[] mOpacities;
    private String[] mOpacityNames;

    private String mTextOpacity;
    private String mTextOpacityName;

    private boolean mWindowEnabled;
    private int mWindowColor;
    private String mWindowColorName;

    private String mWindowOpacity;
    private String mWindowOpacityName;

    private CaptioningManager mCaptioningManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mResources = getResources();
        mActions = new ArrayList<>();
        mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);

        mStyles = getIntArrayAsStringArray(R.array.captioning_preset_selector_values);

        mStyleNames = mResources.getStringArray(R.array.captioning_preset_selector_titles);

        mTextSizes = mResources.getStringArray(R.array.captioning_font_size_selector_values);
        mTextSizeNames = mResources.getStringArray(R.array.captioning_font_size_selector_titles);

        getLanguages();

        mFontFamilies = mResources.getStringArray(R.array.captioning_typeface_selector_values);
        mFontFamilyNames = mResources.getStringArray(R.array.captioning_typeface_selector_titles);

        getColorSelection();

        mEdgeTypes = getIntArrayAsStringArray (R.array.captioning_edge_type_selector_values);
        mEdgeTypeNames = mResources.getStringArray(R.array.captioning_edge_type_selector_titles);

        mOpacities = getIntArrayAsStringArray(R.array.captioning_opacity_selector_values);
        mOpacityNames = mResources.getStringArray(R.array.captioning_opacity_selector_titles);

        mNone = mResources.getString(R.string.accessibility_none);

        getCaptionSettings ();

        super.onCreate(savedInstanceState);

        mContentFragment = CaptionPreviewFragment.newInstance();
        setContentFragment (mContentFragment);
    }

    private String[] getIntArrayAsStringArray(int resourceId) {
        int[] a = mResources.getIntArray(resourceId);
        String[] s = new String[a.length];
        for (int i = 0; i < a.length; ++i)
            s[i] = String.valueOf(a[i]);
        return s;
    }

    private void getColorSelection() {
        // Load the resource ids of the selectable colors.
        TypedArray ar =
            getApplicationContext().getResources().obtainTypedArray
                (R.array.captioning_color_selector_ids);
        int len = ar.length();
        mColorResIds = new int [len];
        for (int i = 0; i < len; ++i)
            mColorResIds [i] = ar.getResourceId(i, 0);
        ar.recycle();
        // Load the RGB values of the colors.
        mColorRGBs = new int[len];
        for (int i = 0; i < len; ++i)
            mColorRGBs[i] = mResources.getColor(mColorResIds[i]) & 0xffffff;
        // Initialize the color names that will be displayed to the user.
        String[] colorNames = mResources.getStringArray(R.array.captioning_color_selector_titles);
        mColorNames = new String[len];
        for (int i = 0; i < colorNames.length; ++i) {
            mColorNames[i] = colorNames[i];
        }
        for (int i = colorNames.length; i < len; ++i) {
            mColorNames[i] = String.format("#%06X", mColorRGBs[i]);
        }
    }

    private String getColorName(int color) {
        for (int x = 0; x < mColorRGBs.length; ++x)
            if (mColorRGBs[x] == color)
                return mColorNames[x];
        return "";
    }

    private void getCaptionSettings() {

        mDisplayEnabled = mCaptioningManager.isEnabled();

        mLanguage = mCaptioningManager.getRawLocale();
        if (mLanguage != null)
            mLanguageName = getLanguageName(mLanguage);

        mTextSize = getClosestValue(mCaptioningManager.getFontScale(), mTextSizes);
        mTextSizeName = getTextSizeName(mTextSize);

        int style = mCaptioningManager.getRawUserStyle();
        mStyle = String.valueOf (style);
        mStyleName = getStyleName(mStyle);

        if (style == CaptioningManager.CaptionStyle.PRESET_CUSTOM) {
            getCustomCaptionStyle();
        }
    }

    private String getClosestValue(float value, String[] values) {
        int ndx = -1;
        float delta = 0;
        for (int i = 0; i < values.length; ++i) {
            float d = Math.abs(value - Float.parseFloat(values [i]));
            if (ndx == -1 || d < delta) {
                ndx = i;
                delta = d;
            }
        }
        if (ndx == -1) {
            return "";
        } else {
            return values [ndx];
        }
    }

    private void getCustomCaptionStyle() {
        CaptioningManager.CaptionStyle cs = mCaptioningManager.getUserStyle();

        mFontFamily = cs.mRawTypeface;
        mFontFamilyName = getFontFamilyName(mFontFamily);

        mTextColor = cs.foregroundColor & 0xffffff;
        mTextColorName = getColorName(mTextColor);

        mTextOpacity = getClosestValue(cs.foregroundColor | 0xffffff, mOpacities);
        mTextOpacityName = getOpacityName(mTextOpacity);

        mEdgeType = Integer.toString (cs.edgeType);
        mEdgeTypeName = getEdgeTypeName(mEdgeType);

        mEdgeColor = cs.edgeColor & 0xffffff;
        mEdgeColorName = getColorName(mEdgeColor);

        mBackgroundColor = cs.backgroundColor & 0xffffff;
        mBackgroundColorName = getColorName(mBackgroundColor);

        mBackgroundOpacity = getClosestValue(cs.backgroundColor | 0xffffff, mOpacities);
        mBackgroundOpacityName = getOpacityName(mBackgroundOpacity);

        mWindowEnabled = cs.hasWindowColor() && (cs.windowColor & 0xff000000) != 0;
        mWindowColor = cs.windowColor & 0xffffff;
        if (mWindowEnabled) {
            mWindowColorName = getColorName(mWindowColor);
        } else {
            mWindowColorName = mNone;
        }
        mWindowOpacity = getClosestValue(cs.windowColor | 0xffffff, mOpacities);
        if (mWindowEnabled) {
            mWindowOpacityName = getOpacityName(mWindowOpacity);
        } else {
            mWindowOpacityName = mNone;
        }
    }

    private void colorsToActions(int selectedColor) {
        mActions.clear();
        for (int i = 0; i < mColorResIds.length; ++i) {
            mActions.add(
                new Action.Builder().key(Integer.toString(mColorRGBs[i]))
                                    .drawableResource(mColorResIds[i])
                                    .title(mColorNames[i])
                                    .checked(selectedColor == mColorRGBs[i]).build());
        }
    }

    @Override
    protected void refreshActionList() {
        if(mContentFragment instanceof CaptionPreviewFragment) {
            ((CaptionPreviewFragment)mContentFragment).resetLivePreview();
        }
        mActions.clear();
        switch ((ActionType) mState) {
            case CAPTIONS_OVERVIEW:
                int statusResource = mDisplayEnabled ?
                        R.string.captions_display_on : R.string.captions_display_off;
                mActions.add(ActionType.CAPTIONS_DISPLAY.toAction(
                             mResources, mResources.getString(statusResource)));
                mActions.add(ActionType.CAPTIONS_CONFIGURE.toAction(mResources,
                             mResources.getString(R.string.display_options)));
                break;
            case CAPTIONS_DISPLAY:
                mActions.add(ActionBehavior.ON.toAction(ActionBehavior.getOnKey(
                             ActionType.CAPTIONS_DISPLAY.name()), mResources, mDisplayEnabled));
                mActions.add(ActionBehavior.OFF.toAction(ActionBehavior.getOffKey(
                             ActionType.CAPTIONS_DISPLAY.name()), mResources, ! mDisplayEnabled));
                break;
            case CAPTIONS_CONFIGURE:
                mActions.add(ActionType.CAPTIONS_LANGUAGE.toAction(mResources, mLanguageName));
                mActions.add(ActionType.CAPTIONS_TEXTSIZE.toAction(mResources, mTextSizeName));
                mActions.add(ActionType.CAPTIONS_CAPTIONSTYLE.toAction(mResources, mStyleName));
                break;
            case CAPTIONS_TEXTSIZE:
                mActions = Action.createActionsFromArrays(mTextSizes, mTextSizeNames);
                for (Action action : mActions) {
                    action.setChecked(action.getKey().equals(mTextSize));
                }
                break;
            case CAPTIONS_CAPTIONSTYLE:
                mActions = Action.createActionsFromArrays(mStyles, mStyleNames);
                for (Action action : mActions) {
                    action.setChecked(action.getKey().equals(mStyle));
                }
                break;
            case CAPTIONS_CUSTOMOPTIONS:
                mActions.add(ActionType.CAPTIONS_FONTFAMILY.toAction(mResources, mFontFamilyName));
                mActions.add(ActionType.CAPTIONS_TEXTCOLOR.toAction(mResources, mTextColorName));
                mActions.add(ActionType.CAPTIONS_TEXTOPACITY.toAction(mResources,
                        mTextOpacityName));
                mActions.add(ActionType.CAPTIONS_EDGETYPE.toAction(mResources, mEdgeTypeName));
                mActions.add(ActionType.CAPTIONS_EDGECOLOR.toAction(mResources, mEdgeColorName));
                mActions.add(ActionType.CAPTIONS_BACKGROUNDCOLOR.toAction(mResources,
                               mBackgroundColorName));
                mActions.add(ActionType.CAPTIONS_BACKGROUNDOPACITY.toAction(mResources,
                               mBackgroundOpacityName));
                mActions.add(ActionType.CAPTIONS_WINDOWCOLOR.toAction(mResources,
                        mWindowColorName));
                mActions.add(ActionType.CAPTIONS_WINDOWOPACITY.toAction(mResources,
                        mWindowOpacityName, mWindowEnabled));
                break;
            case CAPTIONS_LANGUAGE:
                mActions = Action.createActionsFromArrays(mLanguageLocales, mLanguageNames);
                for (Action action : mActions) {
                    action.setChecked(action.getKey().equals(mLanguage));
                }
                break;
            case CAPTIONS_FONTFAMILY:
                mActions = Action.createActionsFromArrays(mFontFamilies, mFontFamilyNames);
                for (Action action : mActions) {
                    action.setChecked(action.getKey().equals(mFontFamily));
                }
                break;
            case CAPTIONS_TEXTCOLOR:
                colorsToActions(mTextColor);
                break;
            case CAPTIONS_TEXTOPACITY:
                mActions = Action.createActionsFromArrays(mOpacities, mOpacityNames);
                for (Action action : mActions) {
                    action.setChecked(action.getKey().equals(mTextOpacity));
                }
                break;
            case CAPTIONS_EDGETYPE:
                mActions = Action.createActionsFromArrays(mEdgeTypes, mEdgeTypeNames);
               for (Action action : mActions) {
                    action.setChecked(action.getKey().equals(mEdgeType));
                }
                break;
            case CAPTIONS_EDGECOLOR:
                colorsToActions(mEdgeColor);
                break;
            case CAPTIONS_BACKGROUNDCOLOR:
                colorsToActions(mBackgroundColor);
                break;
            case CAPTIONS_BACKGROUNDOPACITY:
                mActions = Action.createActionsFromArrays(mOpacities, mOpacityNames);
                for (Action action : mActions) {
                    action.setChecked(action.getKey().equals(mBackgroundOpacity));
                }
                break;
            case CAPTIONS_WINDOWCOLOR:
                mActions.clear();
                mActions.add(
                    new Action.Builder().key(mNone)
                                        .drawableResource(mColorResIds[1])
                                        .title(mNone)
                                        .checked(!mWindowEnabled).build());
                for (int i = 0; i < mColorResIds.length; ++i) {
                    mActions.add(
                        new Action.Builder().key(Integer.toString(mColorRGBs[i]))
                                            .drawableResource(mColorResIds[i])
                                            .title(mColorNames[i])
                                            .checked(mWindowEnabled && mWindowColor ==
                                                    mColorRGBs[i]).build());
                }
                break;
            case CAPTIONS_WINDOWOPACITY:
                mActions = Action.createActionsFromArrays(mOpacities, mOpacityNames);
                for (Action action : mActions) {
                    action.setChecked(action.getKey().equals(mWindowOpacity));
                }
                break;
        }
    }

    @Override
    protected void updateView() {
        refreshActionList();
        mActionFragment = ActionFragment.newInstance(mActions);
        setActionFragment (mActionFragment);
    }

    @Override
    public void onActionFocused(Action action) {
        final String key = action.getKey();
        switch ((ActionType) mState) {
            case CAPTIONS_LANGUAGE:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewLanguage(key);
                }
                break;
            case CAPTIONS_TEXTSIZE:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewFontScale(Float.parseFloat(key));
                }
                break;
            case CAPTIONS_CAPTIONSTYLE:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewCaptionStyle(Integer.parseInt(key));
                }
                break;
            case CAPTIONS_FONTFAMILY:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewFontFamily(key);
                }
                break;
            case CAPTIONS_TEXTCOLOR:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewTextColor(Integer.parseInt(key));
                }
                break;
            case CAPTIONS_TEXTOPACITY:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewTextOpacity(key);
                }
                break;
            case CAPTIONS_EDGETYPE:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewEdgeType(Integer.parseInt(key));
                }
                break;
            case CAPTIONS_EDGECOLOR:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewEdgeColor(Integer.parseInt(key));
                }
                break;
            case CAPTIONS_BACKGROUNDCOLOR:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewBackgroundColor(Integer.parseInt(key));
                }
                break;
            case CAPTIONS_BACKGROUNDOPACITY:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewBackgroundOpacity(key);
                }
                break;
            case CAPTIONS_WINDOWCOLOR:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    if (TextUtils.equals(key, mNone)) {
                        ((CaptionPreviewFragment)mContentFragment).
                            livePreviewWindowColorNone();
                    } else {
                        ((CaptionPreviewFragment)mContentFragment).
                            livePreviewWindowColor(Integer.parseInt(key));
                    }
                }
                break;
            case CAPTIONS_WINDOWOPACITY:
                if(mContentFragment instanceof CaptionPreviewFragment) {
                    ((CaptionPreviewFragment)mContentFragment).
                        livePreviewWindowOpacity(key);
                }
                break;
        }
    }

    @Override
    public void onActionClicked(Action action) {
        final String key = action.getKey();
        switch ((ActionType) mState) {
            case CAPTIONS_LANGUAGE:
                setLanguage(key);
                goBack();
                break;
            case CAPTIONS_TEXTSIZE:
                setTextSize(key);
                goBack();
                break;
            case CAPTIONS_CAPTIONSTYLE:
                setStyle(key);
                if (Integer.parseInt(key) == CaptioningManager.CaptionStyle.PRESET_CUSTOM) {
                    setState(ActionType.CAPTIONS_CUSTOMOPTIONS, true);
                } else {
                    goBack();
                }
                break;
            case CAPTIONS_FONTFAMILY:
                setFontFamily(key);
                goBack();
                break;
            case CAPTIONS_TEXTCOLOR:
                setTextColor(key);
                goBack();
                break;
            case CAPTIONS_TEXTOPACITY:
                setTextOpacity(key);
                goBack();
                break;
            case CAPTIONS_EDGETYPE:
                setEdgeType(key);
                goBack();
                break;
            case CAPTIONS_EDGECOLOR:
                setEdgeColor(key);
                goBack();
                break;
            case CAPTIONS_BACKGROUNDCOLOR:
                setBackgroundColor(key);
                goBack();
                break;
            case CAPTIONS_BACKGROUNDOPACITY:
                setBackgroundOpacity(key);
                goBack();
                break;
            case CAPTIONS_WINDOWCOLOR:
                setWindowColor(key);
                goBack();
                break;
            case CAPTIONS_WINDOWOPACITY:
                setWindowOpacity(key);
                goBack();
                break;
            default:
                ActionKey<ActionType, ActionBehavior> actionKey =
                    new ActionKey<>(
                        ActionType.class, ActionBehavior.class, action.getKey());
                final ActionType type = actionKey.getType();
                final ActionBehavior behavior = actionKey.getBehavior();
                if (behavior == null) {
                    return;
                }
                switch (behavior) {
                    case ON:
                        setProperty(true);
                        break;
                    case OFF:
                        setProperty(false);
                        break;
                    case INIT:
                        setState(type, true);
                        break;
                }
                break;
        }
    }

    @Override
    protected Object getInitialState() {
        return ActionType.CAPTIONS_OVERVIEW;
    }

    @Override
    protected void setProperty(boolean enable) {
        switch ((ActionType) mState) {
            case CAPTIONS_DISPLAY:
                setEnabled(enable);
                break;
        }
        goBack();
    }

    private String getTextSizeName(String textSize) {
        for (int i = 0; i < mTextSizes.length; ++i) {
            if (mTextSizes [i] == textSize) {
                return mTextSizeNames [i];
            }
        }
        return "";
    }

    private String getStyleName(String style) {
        for (int i = 0; i < mStyles.length; ++i) {
            if (mStyles [i] == style) {
                return mStyleNames [i];
            }
        }
        return "";
    }

    private void setEnabled(boolean enabled) {
        mDisplayEnabled = enabled;
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED,
                               enabled ? 1 : 0);
    }

    private void setStyle(String style) {
        mStyle = style;
        mStyleName = getStyleName(style);
        final ContentResolver cr = getContentResolver();
        int s = Integer.parseInt(style);
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, s);
        if (s == CaptioningManager.CaptionStyle.PRESET_CUSTOM) {
            getCustomCaptionStyle();
        }
        if(mContentFragment instanceof CaptionPreviewFragment) {
            ((CaptionPreviewFragment)mContentFragment).refreshPreviewText();
        }
    }

    private void setTextSize(String textSize) {
        mTextSize = textSize;
        mTextSizeName = getTextSizeName(textSize);
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putFloat(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE,
                                 Float.parseFloat(textSize));
    }

    private void setLanguage(String language) {
        mLanguage = language;
        mLanguageName = getLanguageName(language);
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putString(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE, language);
    }

    private void setFontFamily(String fontFamily) {
        mFontFamily = fontFamily;
        mFontFamilyName = getFontFamilyName(fontFamily);
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putString(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_TYPEFACE,
                                  fontFamily);
    }

    private void setTextColor(String textColor) {
        mTextColor = Integer.parseInt(textColor);
        mTextColorName = getColorName(mTextColor);
        updateCaptioningTextColor();
    }

    private void setTextOpacity(String textOpacity) {
        mTextOpacity = textOpacity;
        mTextOpacityName = getOpacityName(textOpacity);
        updateCaptioningTextColor();
    }

    private void updateCaptioningTextColor() {
        int opacity = Integer.parseInt(mTextOpacity) & 0xff000000;
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
                                opacity | mTextColor);
    }

    private void setWindowColor(String windowColor) {
        if (TextUtils.equals(windowColor, mNone)) {
            mWindowEnabled = false;
            mWindowColorName = mNone;
            mWindowOpacityName = mNone;
        } else {
            mWindowEnabled = true;
            mWindowColor = Integer.parseInt(windowColor);
            mWindowColorName = getColorName(mWindowColor);
            mWindowOpacityName = getOpacityName(mWindowOpacity);
        }
        updateCaptioningWindowColor();
    }

    private void setWindowOpacity(String windowOpacity) {
        mWindowOpacity = windowOpacity;
        mWindowOpacityName = getOpacityName(windowOpacity);
        updateCaptioningWindowColor();
    }

    private void updateCaptioningWindowColor() {
        int opacity = mWindowEnabled ? Integer.parseInt(mWindowOpacity) & 0xff000000 : 0;
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR,
                                opacity | mWindowColor);
    }

    private void setEdgeType(String edgeType) {
        mEdgeType = edgeType;
        mEdgeTypeName = getEdgeTypeName(edgeType);
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE,
                                Integer.parseInt(edgeType));
    }

    private void setEdgeColor(String edgeColor) {
        mEdgeColor = Integer.parseInt(edgeColor);
        mEdgeColorName = getColorName(mEdgeColor);
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR,
                                0xff000000 | mEdgeColor);
    }

    private void updateCaptioningBackgroundColor() {
        int opacity = Integer.parseInt(mBackgroundOpacity) & 0xff000000;
        final ContentResolver cr = getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
                                opacity | mBackgroundColor);
    }

    private void setBackgroundColor(String backgroundColor) {
        mBackgroundColor = Integer.parseInt(backgroundColor);
        mBackgroundColorName = getColorName(mBackgroundColor);
        updateCaptioningBackgroundColor();
    }

    private void setBackgroundOpacity(String backgroundOpacity) {
        mBackgroundOpacity = backgroundOpacity;
        mBackgroundOpacityName = getOpacityName(backgroundOpacity);
        updateCaptioningBackgroundColor();
    }

    private String getLanguageName(String language) {
        for (int i = 0; i < mLanguageLocales.length; ++i) {
            if (language.equals(mLanguageLocales [i])) {
                return mLanguageNames [i];
            }
        }
        return "";
    }

    private static String getDisplayName(
            Locale l, String[] specialLocaleCodes, String[] specialLocaleNames) {
        String code = l.toString();
        for (int i = 0; i < specialLocaleCodes.length; i++) {
            if (specialLocaleCodes[i].equals(code)) {
                return specialLocaleNames[i];
            }
        }
        return l.getDisplayName(l);
    }

    private String getFontFamilyName(String fontFamily) {
        int x = indexOf(mFontFamilies, fontFamily);
        if (x == -1) {
            return "";
        } else {
            return mFontFamilyNames [x];
        }
    }

    private String getOpacityName(String opacity) {
        int x = indexOf(mOpacities, opacity);
        if (x == -1) {
            return "";
        } else {
            return mOpacityNames [x];
        }
    }

    private int indexOf(String[] a, String s) {
        if (s != null) {
            for (int i = 0; i < a.length; ++i) {
                if (s.equals(a [i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getEdgeTypeName(String edgeType) {
        int x = indexOf(mEdgeTypes, edgeType);
        if (x == -1) {
            return "";
        } else {
            return mEdgeTypeNames [x];
        }
    }

    private static class LocaleInfo implements Comparable<LocaleInfo> {
        private static final Collator sCollator = Collator.getInstance();

        public String label;
        public final Locale locale;

        public LocaleInfo(String label, Locale locale) {
            this.label = label;
            this.locale = locale;
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public int compareTo(LocaleInfo another) {
            return sCollator.compare(this.label, another.label);
        }
    }

    private void getLanguages() {
        final String[] systemLocales = Resources.getSystem().getAssets().getLocales();
        Arrays.sort(systemLocales);

        final Context context = getApplicationContext();
        final Resources resources = context.getResources();
        final String[] specialLocaleCodes = resources.getStringArray(
                com.android.internal.R.array.special_locale_codes);
        final String[] specialLocaleNames = resources.getStringArray(
                com.android.internal.R.array.special_locale_names);

        int finalSize = 0;

        final int origSize = systemLocales.length;
        final LocaleInfo[] localeInfos = new LocaleInfo[origSize];
        for (int i = 0; i < origSize; i++) {
            final String localeStr = systemLocales[i];
            final Locale locale = Locale.forLanguageTag(localeStr.replace('_', '-'));
            // "und" means undefined.
            if ("und".equals(locale.getLanguage()) || locale.getLanguage().isEmpty() ||
                    locale.getCountry().isEmpty()) {
                continue;
            }

            if (finalSize == 0) {
                localeInfos[finalSize++] =
                       new LocaleInfo(locale.getDisplayLanguage(locale), locale);
            } else {
                // check previous entry:
                // same lang and a country -> upgrade to full name and
                // insert ours with full name
                // diff lang -> insert ours with lang-only name
                final LocaleInfo previous = localeInfos[finalSize - 1];
                if (previous.locale.getLanguage().equals(locale.getLanguage())
                        && !previous.locale.getLanguage().equals("zz")) {
                    previous.label = getDisplayName(
                            localeInfos[finalSize - 1].locale, specialLocaleCodes,
                            specialLocaleNames);
                    localeInfos[finalSize++] = new LocaleInfo(getDisplayName(locale,
                            specialLocaleCodes, specialLocaleNames), locale);
                } else {
                    final String displayName;
                    if (localeStr.equals("zz_ZZ")) {
                        displayName = "[Developer] Accented English";
                    } else if (localeStr.equals("zz_ZY")) {
                        displayName = "[Developer] Fake Bi-Directional";
                    } else {
                        displayName = locale.getDisplayLanguage(locale);
                    }
                    localeInfos[finalSize++] = new LocaleInfo(displayName, locale);
                }
            }
        }

        mLanguageLocales = new String [finalSize];
        mLanguageNames = new String [finalSize];

        Arrays.sort(localeInfos, 0, finalSize);
        for (int i = 0; i < finalSize; i++) {
            final LocaleInfo info = localeInfos[i];
            mLanguageLocales[i] = info.locale.toString();
            mLanguageNames[i] = info.toString();
        }
    }
}

