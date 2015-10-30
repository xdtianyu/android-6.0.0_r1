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

package com.android.tv.settings.about;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.tv.settings.PreferenceUtils;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.SettingsLayoutActivity;
import com.android.tv.settings.name.DeviceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity which shows the build / model / legal info / etc.
 */
public class AboutActivity extends SettingsLayoutActivity {

    private static final String TAG = "AboutActivity";

    /**
     * Action keys for switching over in onActionClicked.
     */
    private static final int KEY_BUILD = 0;
    private static final int KEY_VERSION = 1;
    private static final int KEY_REBOOT = 2;

    /**
     * Intent action of SettingsLicenseActivity (for displaying open source licenses.)
     */
    private static final String SETTINGS_LEGAL_LICENSE_INTENT_ACTION = "android.settings.LICENSE";

    /**
     * Intent action of SettingsTosActivity (for displaying terms of service.)
     */
    private static final String SETTINGS_LEGAL_TERMS_OF_SERVICE = "android.settings.TERMS";

    /**
     * Intent action of device name activity.
     */
    private static final String SETTINGS_DEVICE_NAME_INTENT_ACTION = "android.settings.DEVICE_NAME";

    /**
     * Intent action of system update activity.
     */
    private static final String SETTINGS_UPDATE_SYSTEM = "android.settings.SYSTEM_UPDATE_SETTINGS";

    /**
     * Intent to launch ads activity.
     */
    private static final String SETTINGS_ADS_ACTIVITY_PACKAGE = "com.google.android.gms";
    private static final String SETTINGS_ADS_ACTIVITY_ACTION =
            "com.google.android.gms.settings.ADS_PRIVACY";

    /**
     * Intent component to launch PlatLogo Easter egg.
     */
    private static final ComponentName mPlatLogoActivity = new ComponentName("android",
            "com.android.internal.app.PlatLogoActivity");

    /**
     * Number of clicks it takes to be a developer.
     */
    private static final int NUM_DEVELOPER_CLICKS = 7;

    private int mDeveloperClickCount;
    private PreferenceUtils mPreferenceUtils;
    private Toast mToast;
    private final long[] mHits = new long[3];
    private int mHitsIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferenceUtils = new PreferenceUtils(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDeveloperClickCount = 0;
        mDeviceNameLayoutGetter.refreshView();
    }

    @Override
    public void onActionClicked(Layout.Action action) {
        final int key = action.getId();
        if (key == KEY_BUILD) {
            mDeveloperClickCount++;
            if (!mPreferenceUtils.isDeveloperEnabled()) {
                int numLeft = NUM_DEVELOPER_CLICKS - mDeveloperClickCount;
                if (numLeft < 3 && numLeft > 0) {
                    showToast(getResources().getQuantityString(
                            R.plurals.show_dev_countdown, numLeft, numLeft));
                }
                if (numLeft == 0) {
                    mPreferenceUtils.setDeveloperEnabled(true);
                    showToast(getString(R.string.show_dev_on));
                    mDeveloperClickCount = 0;
                }
            } else {
                if (mDeveloperClickCount > 3) {
                    showToast(getString(R.string.show_dev_already));
                }
            }
        } else if (key == KEY_VERSION) {
            mHits[mHitsIndex] = SystemClock.uptimeMillis();
            mHitsIndex = (mHitsIndex + 1) % mHits.length;
            if (mHits[mHitsIndex] >= SystemClock.uptimeMillis() - 500) {
                Intent intent = new Intent();
                intent.setComponent(mPlatLogoActivity);
                startActivity(intent);
            }
        } else if (key == KEY_REBOOT) {
            final Fragment f = new RebootConfirmFragment();
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, f)
                    .addToBackStack(null)
                    .commit();
        } else {
            Intent intent = action.getIntent();
            if (intent != null) {
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "intent for (" + action.getTitle() + ") not found:", e);
                }
            } else {
                Log.e(TAG, "null intent for: " + action.getTitle());
            }
        }
    }

    private final Layout.LayoutGetter mDeviceNameLayoutGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            return new Layout().add(new Layout.Action.Builder(getResources(),
                    new Intent(SETTINGS_DEVICE_NAME_INTENT_ACTION))
                    .title(R.string.device_name)
                    .description(DeviceManager.getDeviceName(AboutActivity.this))
                    .build());
        }
    };

    @Override
    public Layout createLayout() {
        final Resources res = getResources();

        final Layout.Header header = new Layout.Header.Builder(res)
                .icon(R.drawable.ic_settings_about)
                .title(R.string.about_preference)
                .build();

        header.add(new Layout.Action.Builder(res, systemIntent(SETTINGS_UPDATE_SYSTEM))
                .title(R.string.about_system_update)
                .build());
        header.add(mDeviceNameLayoutGetter);
        header.add(new Layout.Action.Builder(res, KEY_REBOOT)
                .title(R.string.restart_button_label)
                .build());
        header.add(new Layout.Header.Builder(res)
                .title(R.string.about_legal_info)
                .build()
                .add(new Layout.Action.Builder(res,
                        systemIntent(SETTINGS_LEGAL_LICENSE_INTENT_ACTION))
                        .title(R.string.about_legal_license)
                        .build())
                .add(new Layout.Action.Builder(res, systemIntent(SETTINGS_LEGAL_TERMS_OF_SERVICE))
                        .title(R.string.about_terms_of_service)
                        .build()));

        final Intent adsIntent = new Intent();
        adsIntent.setPackage(SETTINGS_ADS_ACTIVITY_PACKAGE);
        adsIntent.setAction(SETTINGS_ADS_ACTIVITY_ACTION);
        adsIntent.addCategory(Intent.CATEGORY_DEFAULT);
        final List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(adsIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (!resolveInfos.isEmpty()) {
            header.add(new Layout.Action.Builder(res, adsIntent)
                    .title(R.string.about_ads)
                    .build());
        }

        header.add(new Layout.Status.Builder(res)
                .title(R.string.about_model)
                .description(Build.MODEL)
                .build());

        String patch = Build.VERSION.SECURITY_PATCH;
        if (!TextUtils.isEmpty(patch)) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(patch);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                patch = DateFormat.format(format, patchDate).toString();
            } catch (ParseException e) {
                // broken parse; fall through and use the raw string
            }
            header.add(new Layout.Status.Builder(res)
                    .title(R.string.security_patch)
                    .description(patch)
                    .build());
        }

        header.add(new Layout.Action.Builder(res, KEY_VERSION)
                .title(R.string.about_version)
                .description(Build.VERSION.RELEASE)
                .build());
        header.add(new Layout.Status.Builder(res)
                .title(R.string.about_serial)
                .description(Build.SERIAL)
                .build());
        header.add(new Layout.Action.Builder(res, KEY_BUILD)
                .title(R.string.about_build)
                .description(Build.DISPLAY)
                .build());
        return new Layout().breadcrumb(getString(R.string.header_category_device)).add(header);
    }

    private void showToast(String toastString) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, toastString, Toast.LENGTH_SHORT);
        mToast.show();
    }

    // Returns an Intent for the given action if a system app can handle it, otherwise null.
    private Intent systemIntent(String action) {
        final Intent intent = new Intent(action);

        // Limit the intent to an activity that is in the system image.
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : activities) {
            if ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                if (info.activityInfo.isEnabled()) {
                    intent.setPackage(info.activityInfo.packageName);
                    return intent;
                }
            }
        }
        return null;  // No system image package found.
    }

    public static class RebootConfirmFragment extends GuidedStepFragment {

        private static final int ACTION_RESTART = 1;

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setSelectedActionPosition(1);
        }

        @Override
        public @NonNull GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            return new GuidanceStylist.Guidance(
                    getString(R.string.system_reboot_confirm),
                    "",
                    getString(R.string.about_preference),
                    getActivity().getDrawable(R.drawable.ic_settings_warning)
                    );
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions,
                Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder()
                    .title(getString(R.string.restart_button_label))
                    .id(ACTION_RESTART)
                    .build());
            actions.add(new GuidedAction.Builder()
                    .title(getString(android.R.string.cancel))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == ACTION_RESTART) {
                PowerManager pm =
                        (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
                pm.reboot(null);
            } else {
                getFragmentManager().popBackStack();
            }
        }
    }
}
