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

package com.android.tv.settings.device.storage;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.settingslib.applications.ApplicationsState;
import com.android.tv.settings.R;
import com.android.tv.settings.device.apps.AppInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BackupAppsStepFragment extends StorageGuidedStepFragment implements
        ApplicationsState.Callbacks {

    private static final String TAG = "BackupAppsStepFragment";

    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;

    private PackageManager mPackageManager;
    private StorageManager mStorageManager;

    private String mVolumeId;
    private ApplicationsState.AppFilter mAppFilter;

    private IconLoaderTask mIconLoaderTask;
    private final Map<String, Drawable> mIconMap = new ArrayMap<>();

    private final List<ApplicationsState.AppEntry> mEntries = new ArrayList<>();

    public static BackupAppsStepFragment newInstance(String volumeId) {
        final BackupAppsStepFragment fragment = new BackupAppsStepFragment();
        final Bundle b = new Bundle(1);
        b.putString(VolumeInfo.EXTRA_VOLUME_ID, volumeId);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Need mPackageManager before onCreateActions, which is called from super.onCreate
        mPackageManager = getActivity().getPackageManager();
        mStorageManager = getActivity().getSystemService(StorageManager.class);

        mVolumeId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
        final VolumeInfo info = mStorageManager.findVolumeById(mVolumeId);
        if (info != null) {
            mAppFilter = new ApplicationsState.VolumeFilter(info.getFsUuid());
        } else {
            // TODO: bail out somehow
            mAppFilter = new ApplicationsState.AppFilter() {
                @Override
                public void init() {}

                @Override
                public boolean filterApp(ApplicationsState.AppEntry info) {
                    return false;
                }
            };
        }

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.resume();
        updateActions();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSession.pause();
    }

    @Override
    public @NonNull GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        final String title;
        final VolumeInfo volumeInfo = mStorageManager.findVolumeById(mVolumeId);
        final String volumeDesc = mStorageManager.getBestVolumeDescription(volumeInfo);
        final String primaryStorageVolumeId =
                mPackageManager.getPrimaryStorageCurrentVolume().getId();
        if (TextUtils.equals(primaryStorageVolumeId, volumeInfo.getId())) {
            title = getString(R.string.storage_wizard_back_up_apps_and_data_title, volumeDesc);
        } else {
            title = getString(R.string.storage_wizard_back_up_apps_title, volumeDesc);
        }
        return new GuidanceStylist.Guidance(
                title,
                "",
                "",
                getActivity().getDrawable(R.drawable.ic_settings_storage));
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        final List<ApplicationsState.AppEntry> entries = mSession.rebuild(mAppFilter,
                ApplicationsState.ALPHA_COMPARATOR);
        if (entries != null) {
            actions.addAll(getAppActions(true, entries));
        }
    }

    private List<GuidedAction> getAppActions(boolean refreshIcons,
            List<ApplicationsState.AppEntry> entries) {

        final List<GuidedAction> actions = new ArrayList<>(entries.size());
        for (final ApplicationsState.AppEntry entry : entries) {
            final int index = actions.size();
            final ApplicationInfo info = entry.info;
            final AppInfo appInfo = new AppInfo(getActivity(), entry);
            actions.add(new GuidedAction.Builder()
                    .title(appInfo.getName())
                    .description(appInfo.getSize())
                    .icon(mIconMap.get(info.packageName))
                    .id(index)
                    .build());
        }
        mEntries.clear();
        mEntries.addAll(entries);

        if (refreshIcons) {
            if (mIconLoaderTask != null) {
                mIconLoaderTask.cancel(true);
            }
            mIconLoaderTask = new IconLoaderTask(entries);
            mIconLoaderTask.execute();
        }
        return actions;
    }

    private void updateActions() {
        final List<ApplicationsState.AppEntry> entries = mSession.rebuild(mAppFilter,
                ApplicationsState.ALPHA_COMPARATOR);
        if (entries != null) {
            setActions(getAppActions(true, entries));
        } else {
            setActions(getAppActions(true, mEntries));
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        final int actionId = (int) action.getId();
        final ApplicationsState.AppEntry entry = mEntries.get(actionId);
        final AppInfo appInfo = new AppInfo(getActivity(), entry);

        final MoveAppStepFragment fragment = MoveAppStepFragment.newInstance(entry.info.packageName,
                appInfo.getName());
        getFragmentManager().beginTransaction()
                .addToBackStack(null)
                .replace(android.R.id.content, fragment)
                .commit();
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        updateActions();
    }

    @Override
    public void onPackageListChanged() {
        updateActions();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
        setActions(getAppActions(true, apps));
    }

    @Override
    public void onLauncherInfoChanged() {
        updateActions();
    }

    @Override
    public void onLoadEntriesCompleted() {
        updateActions();
    }

    @Override
    public void onPackageIconChanged() {
        updateActions();
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        updateActions();
    }

    @Override
    public void onAllSizesComputed() {
        updateActions();
    }

    private class IconLoaderTask extends AsyncTask<Void, Void, Map<String, Drawable>> {
        private final List<ApplicationsState.AppEntry> mEntries;

        public IconLoaderTask(List<ApplicationsState.AppEntry> entries) {
            mEntries = entries;
        }

        @Override
        protected Map<String, Drawable> doInBackground(Void... params) {
            // NB: Java doesn't like parameterized generics in varargs
            final Map<String, Drawable> result = new ArrayMap<>(mEntries.size());
            for (final ApplicationsState.AppEntry entry : mEntries) {
                result.put(entry.info.packageName, mPackageManager.getApplicationIcon(entry.info));
            }
            return result;
        }

        @Override
        protected void onPostExecute(Map<String, Drawable> stringDrawableMap) {
            mIconMap.putAll(stringDrawableMap);
            setActions(getAppActions(false, mEntries));
            mIconLoaderTask = null;
        }
    }

}
