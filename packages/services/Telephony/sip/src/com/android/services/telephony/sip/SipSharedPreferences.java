/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.services.telephony.sip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.sip.SipManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

/**
 * Wrapper for SIP's shared preferences.
 */
public class SipSharedPreferences {
    private static final String PREFIX = "[SipSharedPreferences] ";
    private static final boolean VERBOSE = false; /* STOP SHIP if true */

    private static final String SIP_SHARED_PREFERENCES = "SIP_PREFERENCES";

    /**
     * @deprecated Primary account selection for SIP accounts is no longer relevant.
     */
    @Deprecated
    private static final String KEY_PRIMARY_ACCOUNT = "primary";

    private static final String KEY_NUMBER_OF_PROFILES = "profiles";

    private SharedPreferences mPreferences;
    private Context mContext;

    public SipSharedPreferences(Context context) {
        mPreferences = context.getSharedPreferences(
                SIP_SHARED_PREFERENCES, Context.MODE_WORLD_READABLE);
        mContext = context;
    }

    /**
     * Returns the primary account URI or null if it does not exist.
     * @deprecated The primary account setting is no longer used.
     */
    @Deprecated
    public String getPrimaryAccount() {
        return mPreferences.getString(KEY_PRIMARY_ACCOUNT, null);
    }

    public void setProfilesCount(int number) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(KEY_NUMBER_OF_PROFILES, number);
        editor.apply();
    }

    public int getProfilesCount() {
        return mPreferences.getInt(KEY_NUMBER_OF_PROFILES, 0);
    }

    public void setSipCallOption(String option) {
        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.SIP_CALL_OPTIONS, option);

        // Notify SipAccountRegistry in the telephony layer that the configuration has changed.
        // This causes the SIP PhoneAccounts to be re-registered.  This ensures the supported URI
        // schemes for the SIP PhoneAccounts matches the new SIP_CALL_OPTIONS setting.
        Intent intent = new Intent(SipManager.ACTION_SIP_CALL_OPTION_CHANGED);
        mContext.sendBroadcast(intent);
    }

    public String getSipCallOption() {
        String option = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.SIP_CALL_OPTIONS);
        return (option != null) ? option
                                : mContext.getString(R.string.sip_address_only);
    }

    public void setReceivingCallsEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SIP_RECEIVE_CALLS, (enabled ? 1 : 0));
    }

    public boolean isReceivingCallsEnabled() {
        try {
            return (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SIP_RECEIVE_CALLS) != 0);
        } catch (SettingNotFoundException e) {
            log("isReceivingCallsEnabled, option not set; use default value, exception: " + e);
            return false;
        }
    }

    /**
     * Performs cleanup of the shared preferences, removing the deprecated primary account key if
     * it exists.
     */
    public void cleanupPrimaryAccountSetting() {
        if (mPreferences.contains(KEY_PRIMARY_ACCOUNT)) {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.remove(KEY_PRIMARY_ACCOUNT);
            editor.apply();
        }
    }

    // TODO: back up to Android Backup

    private static void log(String msg) {
        Log.d(SipUtil.LOG_TAG, PREFIX + msg);
    }
}
