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

package com.android.tv.settings.device.apps;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsConstant;
import com.android.tv.settings.device.storage.MoveAppProgressFragment;
import com.android.tv.settings.device.storage.MoveAppStepFragment;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Activity that manages an app.
 */
public class AppManagementActivity extends SettingsLayoutActivity implements
        ApplicationsState.Callbacks, DataClearer.Listener, CacheClearer.Listener,
        DefaultClearer.Listener, MoveAppStepFragment.Callback {

    private static final String TAG = "AppManagementActivity";

    private static final String DIALOG_BACKSTACK_TAG = "storageUsed";

    private static final String SAVE_STATE_MOVE_ID = "AppManagementActivity.moveId";

    // Action IDs
    private static final int ACTION_FORCE_STOP = 1;
    private static final int ACTION_STORAGE_USED = 2;
    private static final int ACTION_CLEAR_DATA = 3;
    private static final int ACTION_CLEAR_CACHE = 4;
    private static final int ACTION_CLEAR_DEFAULTS = 5;

    private static final int ACTION_NOTIFICATIONS_ON = 6;
    private static final int ACTION_NOTIFICATIONS_OFF = 7;

    private static final int ACTION_PERMISSIONS = 8;

    private static final int ACTION_UNINSTALL = 9;
    private static final int ACTION_DISABLE = 10;
    private static final int ACTION_ENABLE = 11;
    private static final int ACTION_UNINSTALL_UPDATES = 12;

    // Result code identifiers
    private static final int REQUEST_UNINSTALL = 1;
    private static final int REQUEST_MANAGE_SPACE = 2;
    private static final int REQUEST_UNINSTALL_UPDATES = 3;

    private PackageManager mPackageManager;
    private StorageManager mStorageManager;
    private String mPackageName;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private AppInfo mAppInfo;
    private OpenManager mOpenManager;
    private ForceStopManager mForceStopManager;
    private UninstallManager mUninstallManager;
    private NotificationSetter mNotificationSetter;
    private DataClearer mDataClearer;
    private DefaultClearer mDefaultClearer;
    private CacheClearer mCacheClearer;

    private int mAppMoveId;
    private final PackageManager.MoveCallback mMoveCallback = new PackageManager.MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (moveId != mAppMoveId || !PackageManager.isMoveStatusFinished(status)) {
                return;
            }

            getFragmentManager().popBackStack(DIALOG_BACKSTACK_TAG,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            final int userId = UserHandle.getUserId(mAppInfo.getUid());
            mApplicationsState.invalidatePackage(mPackageName, userId);

            if (status != PackageManager.MOVE_SUCCEEDED) {
                Log.d(TAG, "Move failure status: " + status);
                Toast.makeText(AppManagementActivity.this,
                        MoveAppProgressFragment.moveStatusToMessage(AppManagementActivity.this,
                                status),
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPackageManager = getPackageManager();
        mStorageManager = getSystemService(StorageManager.class);
        final Uri uri = getIntent().getData();
        if (uri == null) {
            Log.e(TAG, "No app to inspect (missing data uri in intent)");
            finish();
            return;
        }
        mPackageName = uri.getSchemeSpecificPart();
        mApplicationsState = ApplicationsState.getInstance(getApplication());
        mSession = mApplicationsState.newSession(this);
        final int userId = UserHandle.myUserId();
        final ApplicationsState.AppEntry entry = mApplicationsState.getEntry(mPackageName, userId);
        if (entry == null) {
            Log.e(TAG, "Failed to load entry for package " + mPackageName);
            finish();
            return;
        }
        mAppInfo = new AppInfo(this, entry);
        mOpenManager = new OpenManager(this, mAppInfo);
        mForceStopManager = new ForceStopManager(this, mAppInfo);
        mUninstallManager = new UninstallManager(this, mAppInfo);
        mNotificationSetter = new NotificationSetter(mAppInfo);
        mDataClearer = new DataClearer(this, mAppInfo);
        mDefaultClearer = new DefaultClearer(this, mAppInfo);
        mCacheClearer = new CacheClearer(this, mAppInfo);

        mAppMoveId = savedInstanceState != null ?
                savedInstanceState.getInt(SAVE_STATE_MOVE_ID) : -1;

        mPackageManager.registerMoveCallback(mMoveCallback, new Handler());

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSession.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSession.pause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(SAVE_STATE_MOVE_ID, mAppMoveId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPackageManager.unregisterMoveCallback(mMoveCallback);
    }

    public static Intent getLaunchIntent(String packageName) {
        Intent i = new Intent();
        i.setComponent(new ComponentName(SettingsConstant.PACKAGE,
                SettingsConstant.PACKAGE + ".device.apps.AppManagementActivity"));
        i.setData(Uri.parse("package:" + packageName));
        return i;
    }

    static class DisableChanger extends AsyncTask<Void, Void, Void> {
        final PackageManager mPm;
        final WeakReference<AppManagementActivity> mActivity;
        final ApplicationInfo mInfo;
        final int mState;

        DisableChanger(AppManagementActivity activity, ApplicationInfo info, int state) {
            mPm = activity.getPackageManager();
            mActivity = new WeakReference<>(activity);
            mInfo = info;
            mState = state;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mPm.setApplicationEnabledSetting(mInfo.packageName, mState, 0);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            AppManagementActivity activity = mActivity.get();
            if (activity != null) {
                activity.mUninstallLayoutGetter.refreshView();
            }
        }
    }

    @Override
    public void onActionClicked(Layout.Action action) {
        switch (action.getId()) {
            case ACTION_FORCE_STOP:
                onForceStopOk();
                break;
            case ACTION_STORAGE_USED:
                startDialogFragment(MoveAppStepFragment.newInstance(mPackageName,
                        mAppInfo.getName()));
                break;
            case ACTION_CLEAR_DATA:
                onClearDataOk();
                break;
            case ACTION_CLEAR_CACHE:
                onClearCacheOk();
                break;
            case ACTION_CLEAR_DEFAULTS:
                onClearDefaultOk();
                break;
            case ACTION_NOTIFICATIONS_ON:
                onNotificationsOn();
                break;
            case ACTION_NOTIFICATIONS_OFF:
                onNotificationsOff();
                break;
            case ACTION_PERMISSIONS:
                startManagePermissionsActivity();
                break;
            case ACTION_UNINSTALL:
                onUninstallOk();
                break;
            case ACTION_DISABLE:
                onDisableOk();
                break;
            case ACTION_ENABLE:
                onEnableOk();
                break;
            case ACTION_UNINSTALL_UPDATES:
                onUninstallUpdatesOk();

            case Layout.Action.ACTION_INTENT:
                final Intent intent = action.getIntent();
                if (intent != null) {
                    try {
                        startActivity(intent);
                    } catch (final ActivityNotFoundException e) {
                        Log.d(TAG, "Activity not found", e);
                    }
                }
                break;

            default:
                Log.wtf(TAG, "Unknown action: " + action);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_UNINSTALL:
                if (resultCode == RESULT_OK) {
                    final int userId =  UserHandle.getUserId(mAppInfo.getUid());
                    mApplicationsState.removePackage(mPackageName, userId);
                    finish();
                }
                break;
            case REQUEST_MANAGE_SPACE:
                mDataClearer.onActivityResult(resultCode);
                break;
            case REQUEST_UNINSTALL_UPDATES:
                mUninstallLayoutGetter.refreshView();
                break;
        }
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onLoadEntriesCompleted() {
        mStorageDescriptionGetter.refreshView();
        mDataDescriptionGetter.refreshView();
        mCacheDescriptionGetter.refreshView();
    }

    @Override
    public void onPackageListChanged() {
        final int userId = UserHandle.getUserId(mAppInfo.getUid());
        mAppInfo.setEntry(mApplicationsState.getEntry(mPackageName, userId));
        mStorageDescriptionGetter.refreshView();
        mDataDescriptionGetter.refreshView();
        mCacheDescriptionGetter.refreshView();
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        mStorageDescriptionGetter.refreshView();
        mDataDescriptionGetter.refreshView();
        mCacheDescriptionGetter.refreshView();
    }

    @Override
    public void onAllSizesComputed() {
        mStorageDescriptionGetter.refreshView();
        mDataDescriptionGetter.refreshView();
        mCacheDescriptionGetter.refreshView();
    }

    @Override
    public void dataCleared(boolean succeeded) {
        if (succeeded) {
            final int userId =  UserHandle.getUserId(mAppInfo.getUid());
            mApplicationsState.requestSize(mPackageName, userId);
        } else {
            Log.w(TAG, "Failed to clear data!");
            mDataDescriptionGetter.refreshView();
        }
    }

    @Override
    public void defaultCleared() {
        mDefaultsDescriptionGetter.refreshView();
    }

    @Override
    public void cacheCleared(boolean succeeded) {
        if (succeeded) {
            final int userId =  UserHandle.getUserId(mAppInfo.getUid());
            mApplicationsState.requestSize(mPackageName, userId);
        } else {
            Log.w(TAG, "Failed to clear cache!");
            mCacheDescriptionGetter.refreshView();
        }
    }

    private void startManagePermissionsActivity() {
        // start new activity to manage app permissions
        Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mPackageName);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }

    private void onUninstallOk() {
        mUninstallManager.uninstall(REQUEST_UNINSTALL);
        mUninstallLayoutGetter.refreshView();
    }

    private void onDisableOk() {
        new DisableChanger(this, mAppInfo.getApplicationInfo(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER).execute();
        mUninstallLayoutGetter.refreshView();
        onBackPressed();
    }

    private void onEnableOk() {
        new DisableChanger(this, mAppInfo.getApplicationInfo(),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT).execute();
        mUninstallLayoutGetter.refreshView();
        onBackPressed();
    }

    private void onUninstallUpdatesOk() {
        mUninstallManager.uninstallUpdates(REQUEST_UNINSTALL_UPDATES);
        onBackPressed();
        mUninstallLayoutGetter.refreshView();
    }

    private void onNotificationsOn() {
        if (!mNotificationSetter.enableNotifications()) {
            Log.w(TAG, "Failed to enable notifications!");
        }
        onBackPressed();
    }

    private void onNotificationsOff() {
        if (!mNotificationSetter.disableNotifications()) {
            Log.w(TAG, "Failed to disable notifications!");
        }
        onBackPressed();
    }

    private void onForceStopOk() {
        mForceStopManager.forceStop(mApplicationsState);
        onBackPressed();
    }

    private void onClearDataOk() {
        mDataClearer.clearData(this, REQUEST_MANAGE_SPACE);
        mDataDescriptionGetter.refreshView();
        onBackPressed();
    }

    private void onClearDefaultOk() {
        mDefaultClearer.clearDefault(this);
        mDefaultsDescriptionGetter.refreshView();
        onBackPressed();
    }

    private void onClearCacheOk() {
        mCacheClearer.clearCache(this);
        mCacheDescriptionGetter.refreshView();
        onBackPressed();
    }

    private class AppIconDrawableGetter extends Layout.DrawableGetter {
        private Drawable mDrawable;

        public void refreshFromAppInfo(AppInfo info) {
            int iconRes = info.getIconResource();
            Drawable iconDrawable = null;
            if (iconRes != 0) {
                try {
                    Resources targetRes = getPackageManager()
                            .getResourcesForApplication(info.getApplicationInfo());
                    // noinspection deprecation
                    iconDrawable = targetRes.getDrawable(iconRes);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "Exception while loading app icon", e);
                }
            } else {
                // noinspection deprecation
                iconDrawable = Resources.getSystem().getDrawable(
                        com.android.internal.R.drawable.sym_def_app_icon);
            }

            if (iconDrawable == null) {
                // noinspection deprecation
                Resources.getSystem().getDrawable(
                        com.android.internal.R.drawable.sym_app_on_sd_unavailable_icon);
            }

            if (iconDrawable != null) {
                final Resources resources = getResources();
                // We need to inset the drawable by 16dp, but the inset gets applied around the
                // inner drawable's natural bounds, before the drawable is scaled by the ImageView.
                // Therefore we need to pre-scale the inset so that it appears at the correct size
                // regardless of the inner drawable's natural bounds.
                final int longestDimension = Math.max(iconDrawable.getIntrinsicHeight(),
                        iconDrawable.getIntrinsicWidth());
                final float rawInset = resources.getDimension(R.dimen.content_icon_inset);
                final float scale = (float) longestDimension /
                        (resources.getDimension(R.dimen.lb_content_fragment_icon_width)
                                - 2 * rawInset);
                final int inset = Math.round(rawInset * scale);
                mDrawable = new InsetDrawable(iconDrawable, inset);
            } else {
                mDrawable = null;
            }
        }

        @Override
        public Drawable get() {
            return mDrawable;
        }
    }

    public final AppIconDrawableGetter mAppIconDrawableGetter = new AppIconDrawableGetter();

    @Override
    public Layout createLayout() {
        final Resources res = getResources();
        mAppIconDrawableGetter.refreshFromAppInfo(mAppInfo);
        final Layout.Header header = new Layout.Header.Builder(res)
                .title(mAppInfo.getName())
                .detailedDescription(getString(R.string.device_apps_app_management_version,
                        mAppInfo.getVersion()))
                .icon(mAppIconDrawableGetter)
                .build();

        // Open
        if (mOpenManager.canOpen()) {
            header.add(new Layout.Action.Builder(res, mOpenManager.getLaunchIntent())
                    .title(R.string.device_apps_app_management_open)
                    .build());
        }
        // Force Stop
        if (mForceStopManager.canForceStop()) {
            header.add(new Layout.Header.Builder(res)
                    .title(R.string.device_apps_app_management_force_stop)
                    .detailedDescription(R.string.device_apps_app_management_force_stop_desc)
                    .build()
                    .add(new Layout.Action.Builder(res, ACTION_FORCE_STOP)
                            .title(android.R.string.ok)
                            .build())
                    .add(new Layout.Action.Builder(res, Layout.Action.ACTION_BACK)
                            .title(android.R.string.cancel)
                            .defaultSelection()
                            .build()));
        }
        // Uninstall/Disable/Enable
        header.add(mUninstallLayoutGetter);
        // Storage used
        header.add(new Layout.Action.Builder(res, ACTION_STORAGE_USED)
                .title(R.string.device_apps_app_management_storage_used)
                .description(mStorageDescriptionGetter)
                .build());
        // Clear data
        header.add(new Layout.Header.Builder(res)
                .title(R.string.device_apps_app_management_clear_data)
                .detailedDescription(R.string.device_apps_app_management_clear_data_desc)
                .description(mDataDescriptionGetter)
                .build()
                .add(new Layout.Action.Builder(res, ACTION_CLEAR_DATA)
                        .title(android.R.string.ok)
                        .build())
                .add(new Layout.Action.Builder(res, Layout.Action.ACTION_BACK)
                        .title(android.R.string.cancel)
                        .defaultSelection()
                        .build()));
        // Clear cache
        header.add(new Layout.Header.Builder(res)
                .title(R.string.device_apps_app_management_clear_cache)
                .description(mCacheDescriptionGetter)
                .build()
                .add(new Layout.Action.Builder(res, ACTION_CLEAR_CACHE)
                        .title(android.R.string.ok)
                        .build())
                .add(new Layout.Action.Builder(res, Layout.Action.ACTION_BACK)
                        .title(android.R.string.cancel)
                        .defaultSelection()
                        .build()));
        // Clear defaults
        header.add(new Layout.Header.Builder(res)
                .title(R.string.device_apps_app_management_clear_default)
                .description(mDefaultsDescriptionGetter)
                .build()
                .add(new Layout.Action.Builder(res, ACTION_CLEAR_DEFAULTS)
                        .title(android.R.string.ok)
                        .build())
                .add(new Layout.Action.Builder(res, Layout.Action.ACTION_BACK)
                        .title(android.R.string.cancel)
                        .defaultSelection()
                        .build()));
        // Notifications
        header.add(new Layout.Header.Builder(res)
                .title(R.string.device_apps_app_management_notifications)
                .build()
                .setSelectionGroup(
                        new Layout.SelectionGroup.Builder(2)
                                .add(getString(R.string.settings_on), null,
                                        ACTION_NOTIFICATIONS_ON)
                                .add(getString(R.string.settings_off), null,
                                        ACTION_NOTIFICATIONS_OFF)
                                .select((mNotificationSetter.areNotificationsOn())
                                        ? ACTION_NOTIFICATIONS_ON : ACTION_NOTIFICATIONS_OFF)
                                .build()));
        // Permissions
        header.add(new Layout.Action.Builder(res, ACTION_PERMISSIONS)
                .title(R.string.device_apps_app_management_permissions)
                .build());

        return new Layout().breadcrumb(getString(R.string.device_apps)).add(header);
    }

    private final Layout.LayoutGetter mUninstallLayoutGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            final Layout layout = new Layout();
            final Resources res = getResources();
            if (mUninstallManager.canUninstall()) {
                layout.add(new Layout.Header.Builder(res)
                        .title(R.string.device_apps_app_management_uninstall)
                        .detailedDescription(R.string.device_apps_app_management_uninstall_desc)
                        .build()
                        .add(new Layout.Action.Builder(res, ACTION_UNINSTALL)
                                .title(android.R.string.ok)
                                .build())
                        .add(new Layout.Action.Builder(res, Layout.Action.ACTION_BACK)
                                .title(android.R.string.cancel)
                                .defaultSelection()
                                .build()));
            } else {
                if (mUninstallManager.canUninstallUpdates()) {
                    layout.add(new Layout.Header.Builder(res)
                            .title(R.string.device_apps_app_management_uninstall_updates)
                            .detailedDescription(
                                    R.string.device_apps_app_management_uninstall_updates_desc)
                            .build()
                            .add(new Layout.Action.Builder(res, ACTION_UNINSTALL_UPDATES)
                                    .title(android.R.string.ok)
                                    .build())
                            .add(new Layout.Action.Builder(res, Layout.Action.ACTION_BACK)
                                    .title(android.R.string.cancel)
                                    .defaultSelection()
                                    .build()));
                }
                if (mUninstallManager.canDisable()) {
                    if (mUninstallManager.isEnabled()) {
                        layout.add(new Layout.Header.Builder(res)
                                .title(R.string.device_apps_app_management_disable)
                                .detailedDescription(
                                        R.string.device_apps_app_management_disable_desc)
                                .build()
                                .add(new Layout.Action.Builder(res, ACTION_DISABLE)
                                        .title(android.R.string.ok)
                                        .build())
                                .add(new Layout.Action.Builder(res, Layout.Action.ACTION_BACK)
                                        .title(android.R.string.cancel)
                                        .defaultSelection()
                                        .build()));
                    } else {
                        layout.add(new Layout.Header.Builder(res)
                                .title(R.string.device_apps_app_management_enable)
                                .detailedDescription(
                                        R.string.device_apps_app_management_disable_desc)
                                .build()
                                .add(new Layout.Action.Builder(res, ACTION_ENABLE)
                                        .title(android.R.string.ok)
                                        .build())
                                .add(new Layout.Action.Builder(res, Layout.Action.ACTION_BACK)
                                        .title(android.R.string.cancel)
                                        .defaultSelection()
                                        .build()));
                    }
                }
            }
            return layout;
        }
    };

    private final Layout.StringGetter mStorageDescriptionGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            final ApplicationInfo applicationInfo = mAppInfo.getApplicationInfo();
            final VolumeInfo volumeInfo = mPackageManager.getPackageCurrentVolume(applicationInfo);
            final String volumeDesc = mStorageManager.getBestVolumeDescription(volumeInfo);
            final String size = mAppInfo.getSize();
            if (TextUtils.isEmpty(size)) {
                return getString(R.string.storage_calculating_size);
            } else {
                return getString(R.string.device_apps_app_management_storage_used_desc,
                        mAppInfo.getSize(), volumeDesc);
            }
        }
    };

    private final Layout.StringGetter mDataDescriptionGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            return mDataClearer.getDataSize(AppManagementActivity.this);
        }
    };

    private final Layout.StringGetter mCacheDescriptionGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            return mCacheClearer.getCacheSize(AppManagementActivity.this);
        }
    };

    private final Layout.StringGetter mDefaultsDescriptionGetter = new Layout.StringGetter() {
        @Override
        public String get() {
            return mDefaultClearer.getDescription(AppManagementActivity.this);
        }
    };

    @Override
    public void onRequestMovePackageToVolume(String packageName, VolumeInfo destination) {
        // Kick off the move
        mAppMoveId = mPackageManager.movePackage(packageName, destination);
        // Show the progress dialog
        startDialogFragment(MoveAppProgressFragment.newInstance(mAppInfo.getName()));
    }

    private void startDialogFragment(Fragment fragment) {
        // Get rid of any previous wizard screen(s)
        getFragmentManager().popBackStack(DIALOG_BACKSTACK_TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // Replace it with the progress screen
        getFragmentManager().beginTransaction()
                .addToBackStack(DIALOG_BACKSTACK_TAG)
                .replace(android.R.id.content, fragment)
                .commit();
    }
}
