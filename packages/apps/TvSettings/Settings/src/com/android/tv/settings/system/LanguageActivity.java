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

import android.app.ActivityManagerNative;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.android.internal.app.LocalePicker;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;
import com.android.tv.settings.widget.BitmapDownloader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.MissingResourceException;

public class LanguageActivity extends DialogActivity implements ActionAdapter.Listener {

    private static final String TAG = "LanguageActivity";
    private static final boolean DEBUG = false;

    private static final String KEY_LOCALE = "locale";
    /**
     * Comparator of LocaleInfo objects that prioritizes US locales above all
     * others. Between US locales it prioritizes English. All other locales are
     * sorted alphabetically first by language and then by region. So, Canadian
     * French is presented before French French.
     */
    private static class LocaleComparator implements Comparator<LocalePicker.LocaleInfo> {
        @Override
        public int compare(LocalePicker.LocaleInfo l, LocalePicker.LocaleInfo r) {
            Locale lhs = l.getLocale();
            Locale rhs = r.getLocale();

            String lhsCountry = "";
            String rhsCountry = "";

            try {
                lhsCountry = lhs.getISO3Country();
            } catch (MissingResourceException e) {
                Log.e(TAG, "LocaleComparator cuaught exception, country set to empty.");
            }

            try {
                rhsCountry = rhs.getISO3Country();
            } catch (MissingResourceException e) {
                Log.e(TAG, "LocaleComparator cuaught exception, country set to empty.");
            }

            String lhsLang = "";
            String rhsLang = "";

            try {
                lhsLang = lhs.getISO3Language();
            } catch (MissingResourceException e) {
                Log.e(TAG, "LocaleComparator cuaught exception, language set to empty.");
            }

            try {
                rhsLang = rhs.getISO3Language();
            } catch (MissingResourceException e) {
                Log.e(TAG, "LocaleComparator cuaught exception, language set to empty.");
            }

            // if they're the same locale, return 0
            if (lhsCountry.equals(rhsCountry) && lhsLang.equals(rhsLang)) {
                return 0;
            }

            // prioritize US over other countries
            if ("USA".equals(lhsCountry)) {
                // if right hand side is not also USA, left hand side is first
                if (!"USA".equals(rhsCountry)) {
                    return -1;
                } else {
                    // if one of the languages is english we want to prioritize
                    // it, otherwise we don't care, just alphabetize
                    if ("ENG".equals(lhsLang) && "ENG".equals(rhsLang)) {
                        return 0;
                    } else {
                        return "ENG".equals(lhsLang) ? -1 : 1;
                    }
                }
            } else if ("USA".equals(rhsCountry)) {
                // right-hand side is the US and the other isn't, return greater than 1
                return 1;
            } else {
                // neither side is the US, sort based on display language name
                // then country name
                int langEquiv = lhs.getDisplayLanguage(lhs)
                        .compareToIgnoreCase(rhs.getDisplayLanguage(rhs));
                if (langEquiv == 0) {
                    return lhs.getDisplayCountry(lhs)
                            .compareToIgnoreCase(rhs.getDisplayCountry(rhs));
                } else {
                    return langEquiv;
                }
            }
        }
    }

    private Fragment mContentFragment;
    private ActionFragment mActionFragment;
    private Resources mResources;
    private ArrayList<Action> mActions;
    private ArrayAdapter<LocalePicker.LocaleInfo> mLocales;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResources = getResources();
        init();
        setContentAndActionFragments(mContentFragment, mActionFragment);

    }

    @Override
    public void onActionClicked(Action action) {
        String key = action.getKey();
        if (key.contains(KEY_LOCALE)) {
            String s = key.substring(KEY_LOCALE.length());
            int i = Integer.parseInt(s);
            setLanguagePreference(i);
            setWifiCountryCode();
            // Locale change can mean new icons for RTL languages, so invalidate
            // any cached images from resources.
            BitmapDownloader.getInstance(this).invalidateCachedResources();
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mResources = getResources();
        makeContentFragment();
        setContentFragment(mContentFragment);
    }

    private void makeContentFragment() {
        mContentFragment = ContentFragment.newInstance(
                mResources.getString(R.string.system_language), null, null,
                R.drawable.ic_settings_language, getResources().getColor(R.color.icon_background));
    }

    private void init() {
        mActions = new ArrayList<Action>();
        makeContentFragment();

        final String[] specialLocaleCodes = getResources().getStringArray(
                com.android.internal.R.array.special_locale_codes);
        final String[] specialLocaleNames = getResources().getStringArray(
                com.android.internal.R.array.special_locale_names);
        mLocales = LocalePicker.constructAdapter(this);
        mLocales.sort(new LocaleComparator());
        final String[] localeNames = new String[mLocales.getCount()];
        Locale currentLocale;
        try {
            currentLocale = ActivityManagerNative.getDefault().getConfiguration().locale;
        } catch (RemoteException e) {
            currentLocale = null;
        }
        for (int i = 0; i < localeNames.length; i++) {
            Locale target = mLocales.getItem(i).getLocale();
            localeNames[i] = getDisplayName(target, specialLocaleCodes, specialLocaleNames);

            // if this locale's label has a country, use the shortened version
            // instead
            if (mLocales.getItem(i).getLabel().contains("(")) {
                String country = target.getCountry();
                if (!TextUtils.isEmpty(country)) {
                    localeNames[i] = localeNames[i] + " (" + target.getCountry() + ")";
                }
            }

            // For some reason locales are not always first letter cased, for example for
            // in the Spanish locale.
            if (localeNames[i].length() > 0) {
                localeNames[i] = localeNames[i].substring(0, 1).toUpperCase(currentLocale) +
                        localeNames[i].substring(1);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(KEY_LOCALE).append(i);
            mActions.add(new Action.Builder()
                    .key(sb.toString())
                    .title(localeNames[i])
                    .checked(target.equals(currentLocale))
                    .build());
        }

        mActionFragment = ActionFragment.newInstance(mActions);
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

    private void setLanguagePreference(int offset) {
        LocalePicker.updateLocale(mLocales.getItem(offset).getLocale());
    }

    private void setWifiCountryCode() {
        String countryCode = Locale.getDefault().getCountry();
        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr != null && !TextUtils.isEmpty(countryCode)) {
            wifiMgr.setCountryCode(countryCode, true);
        }
    }
}
