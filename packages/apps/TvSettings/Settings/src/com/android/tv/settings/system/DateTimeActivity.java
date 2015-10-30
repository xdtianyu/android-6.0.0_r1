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

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;

import com.android.settingslib.datetime.ZoneGetter;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.SettingsLayoutActivity;
import com.android.tv.settings.util.SettingsHelper;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class DateTimeActivity extends SettingsLayoutActivity {

    private static final String TAG = "DateTimeActivity";

    private static final String HOURS_12 = "12";
    private static final String HOURS_24 = "24";

    private static final int ACTION_AUTO_TIME_ON = 0;
    private static final int ACTION_AUTO_TIME_OFF = 1;
    private static final int ACTION_24HOUR_FORMAT_ON = 3;
    private static final int ACTION_24HOUR_FORMAT_OFF = 4;
    private static final int ACTION_SET_TIMEZONE_BASE = 1<<10;


    private Calendar mDummyDate;
    private boolean mIsResumed;

    private String mNowDate;
    private String mNowTime;

    private SettingsHelper mHelper;

    private final BroadcastReceiver mTimeUpdateIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIsResumed) {
                updateTimeAndDateStrings();
            }
        }
    };

    private final Layout.StringGetter mTimeStringGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            return mNowTime;
        }
    };

    private final Layout.StringGetter mDateStringGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            return mNowDate;
        }
    };

    private final Layout.StringGetter mTimezoneStringGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            final Calendar now = Calendar.getInstance();
            TimeZone tz = now.getTimeZone();

            Date date = new Date();
            return getString(R.string.desc_set_time_zone,
                    formatOffset(tz.getOffset(date.getTime())),
                    tz.getDisplayName(tz.inDaylightTime(date), TimeZone.LONG));
        }
    };

    private final Layout.StringGetter mTimeFormatStringGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            String status = mHelper.getStatusStringFromBoolean(isTimeFormat24h());
            return getString(R.string.desc_set_time_format, status,
                    DateFormat.getTimeFormat(DateTimeActivity.this).format(mDummyDate.getTime()));
        }
    };

    private final Layout.LayoutGetter mSetDateLayoutGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            final Resources res = getResources();
            return new Layout()
                    .add(new Layout.Header.Builder(res)
                            .title(R.string.system_date)
                            .description(mDateStringGetter)
                            .enabled(!getAutoState(Settings.Global.AUTO_TIME))
                            .build()
                            .add(new Layout.Action.Builder(res,
                                    SetDateTimeActivity.getSetDateIntent(DateTimeActivity.this))
                                    .title(R.string.system_set_date)
                                    .description(mDateStringGetter)
                                    .build()));
        }
    };

    private final Layout.LayoutGetter mSetTimeLayoutGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            final Resources res = getResources();
            return new Layout()
                    .add(new Layout.Action.Builder(res,
                            SetDateTimeActivity.getSetTimeIntent(DateTimeActivity.this))
                            .title(R.string.system_set_time)
                            .description(mTimeStringGetter)
                            .enabled(!getAutoState(Settings.Global.AUTO_TIME))
                            .build());

        }
    };

    private Layout.SelectionGroup mAutoDateTimeSelector;
    private Layout.SelectionGroup mTimezoneSelector;
    private Layout.SelectionGroup mTimeFormatSelector;

    private List<Map<String, Object>> mTimeZones;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDummyDate = Calendar.getInstance();

        mHelper = new SettingsHelper(getApplicationContext());

        // Auto date time
        mAutoDateTimeSelector = new Layout.SelectionGroup.Builder(2)
                .add(getString(R.string.action_on_description), null, ACTION_AUTO_TIME_ON)
                .add(getString(R.string.action_off_description), null, ACTION_AUTO_TIME_OFF)
                .build();
        mAutoDateTimeSelector.setSelected(getAutoState(Settings.Global.AUTO_TIME) ?
                ACTION_AUTO_TIME_ON : ACTION_AUTO_TIME_OFF);

        // Time zone
        mTimeZones = ZoneGetter.getZonesList(this);
        // Sort the Time Zones list in ascending offset order
        Collections.sort(mTimeZones, new MyComparator(ZoneGetter.KEY_OFFSET));
        final TimeZone currentTz = TimeZone.getDefault();

        final Layout.SelectionGroup.Builder tzBuilder =
                new Layout.SelectionGroup.Builder(mTimeZones.size());

        int i = ACTION_SET_TIMEZONE_BASE;
        int currentTzActionId = -1;
        for (final Map<String, Object> tz : mTimeZones) {
            if (currentTz.getID().equals(tz.get(ZoneGetter.KEY_ID))) {
                currentTzActionId = i;
            }
            tzBuilder.add((String) tz.get(ZoneGetter.KEY_DISPLAYNAME),
                    formatOffset((Integer) tz.get(ZoneGetter.KEY_OFFSET)), i);
            i++;
        }

        mTimezoneSelector = tzBuilder.build();
        mTimezoneSelector.setSelected(currentTzActionId);

        // Time Format
        mTimeFormatSelector = new Layout.SelectionGroup.Builder(2)
                .add(getString(R.string.settings_on), null, ACTION_24HOUR_FORMAT_ON)
                .add(getString(R.string.settings_off), null, ACTION_24HOUR_FORMAT_OFF)
                .build();

        setSampleDate();

        updateTimeAndDateStrings();

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsResumed = true;

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);

        registerReceiver(mTimeUpdateIntentReceiver, filter);

        updateTimeAndDateStrings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsResumed = false;
        unregisterReceiver(mTimeUpdateIntentReceiver);
    }

    @Override
    public Layout createLayout() {
        final Resources res = getResources();
        return new Layout().breadcrumb(getString(R.string.header_category_preferences))
                .add(new Layout.Header.Builder(res)
                        .icon(R.drawable.ic_settings_datetime)
                        .title(R.string.system_date_time)
                        .build()
                        .add(new Layout.Header.Builder(res)
                                .title(R.string.system_auto_date_time)
                                .description(mAutoDateTimeSelector)
                                .build()
                                .add(mAutoDateTimeSelector))
                        .add(mSetDateLayoutGetter)
                        .add(new Layout.Header.Builder(res)
                                .title(R.string.system_time)
                                .description(mTimeStringGetter)
                                .build()
                                .add(mSetTimeLayoutGetter)
                                .add(new Layout.Header.Builder(res)
                                        .title(R.string.system_set_time_zone)
                                        .description(mTimezoneStringGetter)
                                        .build()
                                        .add(mTimezoneSelector))
                                .add(new Layout.Header.Builder(res)
                                        .title(R.string.system_set_time_format)
                                        .description(mTimeFormatStringGetter)
                                        .build()
                                        .add(mTimeFormatSelector))));
    }

    private void setSampleDate() {
        Calendar now = Calendar.getInstance();
        mDummyDate.setTimeZone(now.getTimeZone());
        // We use December 31st because it's unambiguous when demonstrating the date format.
        // We use 15:14 so we can demonstrate the 12/24 hour options.
        mDummyDate.set(now.get(Calendar.YEAR), 11, 31, 15, 14, 0);
    }

    private boolean getAutoState(String name) {
        try {
            return Settings.Global.getInt(getContentResolver(), name) > 0;
        } catch (SettingNotFoundException snfe) {
            return false;
        }
    }


    private boolean isTimeFormat24h() {
        return DateFormat.is24HourFormat(this);
    }

    private void setTime24Hour(boolean is24Hour) {
        Settings.System.putString(getContentResolver(),
                Settings.System.TIME_12_24,
                is24Hour ? HOURS_24 : HOURS_12);
        updateTimeAndDateStrings();
    }

    private void setAutoDateTime(boolean on) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.AUTO_TIME, on ? 1 : 0);
    }

    // Updates the member strings to reflect the current date and time.
    private void updateTimeAndDateStrings() {
        final Calendar now = Calendar.getInstance();
        java.text.DateFormat dateFormat = DateFormat.getDateFormat(this);
        mNowDate = dateFormat.format(now.getTime());
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(this);
        mNowTime = timeFormat.format(now.getTime());

        mDateStringGetter.refreshView();
        mTimeStringGetter.refreshView();
    }

    @Override
    public void onActionClicked(Layout.Action action) {
        final int actionId = action.getId();
        switch (actionId) {
            case Layout.Action.ACTION_INTENT:
                startActivity(action.getIntent());
                break;
            case ACTION_AUTO_TIME_ON:
                setAutoDateTime(true);
                mSetDateLayoutGetter.refreshView();
                mSetTimeLayoutGetter.refreshView();
                break;
            case ACTION_AUTO_TIME_OFF:
                setAutoDateTime(false);
                mSetDateLayoutGetter.refreshView();
                mSetTimeLayoutGetter.refreshView();
                break;
            case ACTION_24HOUR_FORMAT_ON:
                setTime24Hour(true);
                break;
            case ACTION_24HOUR_FORMAT_OFF:
                setTime24Hour(false);
                break;
            default:
                if ((actionId & ACTION_SET_TIMEZONE_BASE) != 0) {
                    setTimeZone((String) mTimeZones.get(actionId - ACTION_SET_TIMEZONE_BASE)
                            .get(ZoneGetter.KEY_ID));
                }
                break;
        }
    }

    /**
     * Formats the provided timezone offset into a string of the form GMT+XX:XX
     */
    private static String formatOffset(long offset) {
        long off = offset / 1000 / 60;
        final StringBuilder sb = new StringBuilder();

        sb.append("GMT");
        if (off < 0) {
            sb.append('-');
            off = -off;
        } else {
            sb.append('+');
        }

        int hours = (int) (off / 60);
        int minutes = (int) (off % 60);

        sb.append((char) ('0' + hours / 10));
        sb.append((char) ('0' + hours % 10));

        sb.append(':');

        sb.append((char) ('0' + minutes / 10));
        sb.append((char) ('0' + minutes % 10));

        return sb.toString();
    }

    private void setTimeZone(String tzId) {
        // Update the system timezone value
        final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(tzId);

        setSampleDate();
    }

    private static class MyComparator implements Comparator<Map<?, ?>> {
        private String mSortingKey;

        public MyComparator(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public void setSortingKey(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public int compare(Map<?, ?> map1, Map<?, ?> map2) {
            Object value1 = map1.get(mSortingKey);
            Object value2 = map2.get(mSortingKey);

            /*
             * This should never happen, but just in-case, put non-comparable
             * items at the end.
             */
            if (!isComparable(value1)) {
                return isComparable(value2) ? 1 : 0;
            } else if (!isComparable(value2)) {
                return -1;
            }

            return ((Comparable) value1).compareTo(value2);
        }

        private boolean isComparable(Object value) {
            return (value != null) && (value instanceof Comparable);
        }
    }

}
