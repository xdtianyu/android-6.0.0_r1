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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.android.tv.settings.R;
import com.android.tv.settings.util.SettingsAsyncTaskLoader;

import java.util.List;
import java.util.Map;

public class FormatActivity extends Activity
        implements MoveAppStepFragment.Callback, FormatAsPrivateStepFragment.Callback,
        FormatAsPublicStepFragment.Callback, SlowDriveStepFragment.Callback {

    private static final String TAG = "FormatActivity";

    public static final String INTENT_ACTION_FORMAT_AS_PRIVATE =
            "com.android.tv.settings.device.storage.FormatActivity.formatAsPrivate";
    public static final String INTENT_ACTION_FORMAT_AS_PUBLIC =
            "com.android.tv.settings.device.storage.FormatActivity.formatAsPublic";

    private static final String MOVE_PROGRESS_DIALOG_BACKSTACK_TAG = "moveProgressDialog";

    private static final String SAVE_STATE_MOVE_ID = "StorageResetActivity.moveId";
    private static final String SAVE_STATE_FORMAT_PRIVATE_DISK_ID =
            "StorageResetActivity.formatPrivateDiskId";
    private static final String SAVE_STATE_FORMAT_DISK_DESC =
            "StorageResetActivity.formatDiskDesc";
    private static final String SAVE_STATE_FORMAT_PUBLIC_DISK_ID =
            "StorageResetActivity.formatPrivateDiskId";

    // Non-null means we're in the process of formatting this volume as private
    private String mFormatAsPrivateDiskId;
    // Non-null means we're in the process of formatting this volume as public
    private String mFormatAsPublicDiskId;

    private String mFormatDiskDesc;

    private static final int LOADER_FORMAT_AS_PRIVATE = 0;
    private static final int LOADER_FORMAT_AS_PUBLIC = 1;

    private final Handler mHandler = new Handler();
    private PackageManager mPackageManager;
    private StorageManager mStorageManager;

    private int mAppMoveId = -1;
    private final PackageManager.MoveCallback mMoveCallback = new PackageManager.MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (moveId != mAppMoveId || !PackageManager.isMoveStatusFinished(status)) {
                return;
            }

            getFragmentManager().popBackStack(MOVE_PROGRESS_DIALOG_BACKSTACK_TAG,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // TODO: refresh ui

            if (status != PackageManager.MOVE_SUCCEEDED) {
                Log.d(TAG, "Move failure status: " + status);
                Toast.makeText(FormatActivity.this,
                        MoveAppProgressFragment.moveStatusToMessage(FormatActivity.this, status),
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    public static Intent getFormatAsPublicIntent(Context context, String diskId) {
        final Intent i = new Intent(context, FormatActivity.class);
        i.setAction(INTENT_ACTION_FORMAT_AS_PUBLIC);
        i.putExtra(DiskInfo.EXTRA_DISK_ID, diskId);
        return i;
    }

    public static Intent getFormatAsPrivateIntent(Context context, String diskId) {
        final Intent i = new Intent(context, FormatActivity.class);
        i.setAction(INTENT_ACTION_FORMAT_AS_PRIVATE);
        i.putExtra(DiskInfo.EXTRA_DISK_ID, diskId);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPackageManager = getPackageManager();
        mPackageManager.registerMoveCallback(mMoveCallback, new Handler());

        mStorageManager = getSystemService(StorageManager.class);

        if (savedInstanceState != null) {
            mAppMoveId = savedInstanceState.getInt(SAVE_STATE_MOVE_ID);
            mFormatAsPrivateDiskId =
                    savedInstanceState.getString(SAVE_STATE_FORMAT_PRIVATE_DISK_ID);
            mFormatAsPublicDiskId = savedInstanceState.getString(SAVE_STATE_FORMAT_PUBLIC_DISK_ID);
            mFormatDiskDesc = savedInstanceState.getString(SAVE_STATE_FORMAT_DISK_DESC);
        } else {
            final String diskId = getIntent().getStringExtra(DiskInfo.EXTRA_DISK_ID);
            final String action = getIntent().getAction();
            final Fragment f;
            if (TextUtils.equals(action, INTENT_ACTION_FORMAT_AS_PRIVATE)) {
                f = FormatAsPrivateStepFragment.newInstance(diskId);
            } else if (TextUtils.equals(action, INTENT_ACTION_FORMAT_AS_PUBLIC)) {
                f = FormatAsPublicStepFragment.newInstance(diskId);
            } else {
                throw new IllegalStateException("No known action specified");
            }
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, f)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        kickFormatAsPrivateLoader();
        kickFormatAsPublicLoader();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_MOVE_ID, mAppMoveId);
        outState.putString(SAVE_STATE_FORMAT_PRIVATE_DISK_ID, mFormatAsPrivateDiskId);
        outState.putString(SAVE_STATE_FORMAT_PUBLIC_DISK_ID, mFormatAsPublicDiskId);
        outState.putString(SAVE_STATE_FORMAT_DISK_DESC, mFormatDiskDesc);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPackageManager.unregisterMoveCallback(mMoveCallback);
    }


    private static class FormatAsPrivateTaskLoader
            extends SettingsAsyncTaskLoader<Map<String, Object>> {

        public static final String RESULT_EXCEPTION = "exception";
        public static final String RESULT_INTERNAL_BENCH = "internalBench";
        public static final String RESULT_PRIVATE_BENCH = "privateBench";

        private final StorageManager mStorageManager;
        private final String mDiskId;

        public FormatAsPrivateTaskLoader(Context context, String diskId) {
            super(context);
            mStorageManager = getContext().getSystemService(StorageManager.class);
            mDiskId = diskId;
        }

        @Override
        protected void onDiscardResult(Map<String, Object> result) {}

        @Override
        public Map<String, Object> loadInBackground() {
            final Map<String, Object> result = new ArrayMap<>(3);
            try {
                mStorageManager.partitionPrivate(mDiskId);
                final Long internalBench = mStorageManager.benchmark(null);
                result.put(RESULT_INTERNAL_BENCH, internalBench);

                final VolumeInfo privateVol = findVolume();
                if (privateVol != null) {
                    final Long externalBench = mStorageManager.benchmark(privateVol.getId());
                    result.put(RESULT_PRIVATE_BENCH, externalBench);
                }
            } catch (Exception e) {
                result.put(RESULT_EXCEPTION, e);
            }
            return result;
        }

        private VolumeInfo findVolume() {
            final List<VolumeInfo> vols = mStorageManager.getVolumes();
            for (final VolumeInfo vol : vols) {
                if (TextUtils.equals(mDiskId, vol.getDiskId())
                        && (vol.getType() == VolumeInfo.TYPE_PRIVATE)) {
                    return vol;
                }
            }
            return null;
        }
    }

    private class FormatAsPrivateLoaderCallback
            implements LoaderManager.LoaderCallbacks<Map<String, Object>> {

        private final String mDiskId;
        private final String mDescription;

        public FormatAsPrivateLoaderCallback(String diskId, String description) {
            mDiskId = diskId;
            mDescription = description;
        }

        @Override
        public Loader<Map<String, Object>> onCreateLoader(int id, Bundle args) {
            return new FormatAsPrivateTaskLoader(FormatActivity.this, mDiskId);
        }

        @Override
        public void onLoadFinished(Loader<Map<String, Object>> loader, Map<String, Object> data) {
            if (data == null) {
                // No results yet, wait for something interesting to come in.
                return;
            }

            final Exception e = (Exception) data.get(FormatAsPrivateTaskLoader.RESULT_EXCEPTION);
            if (e == null) {
                if (isResumed()) {
                    Toast.makeText(FormatActivity.this, getString(R.string.storage_format_success,
                            mDescription), Toast.LENGTH_SHORT).show();
                }

                final Long internalBench =
                        (Long) data.get(FormatAsPrivateTaskLoader.RESULT_INTERNAL_BENCH);
                final Long privateBench =
                        (Long) data.get(FormatAsPrivateTaskLoader.RESULT_PRIVATE_BENCH);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isResumed()) {
                            if (internalBench != null && privateBench != null) {
                                final float frac = (float) privateBench / (float) internalBench;
                                Log.d(TAG, "New volume is " + frac + "x the speed of internal");

                                // TODO: better threshold
                                if (privateBench > 2000000000) {
                                    getFragmentManager().beginTransaction()
                                            .replace(android.R.id.content,
                                                    SlowDriveStepFragment.newInstance())
                                            .commit();
                                    return;
                                }
                            }
                            launchMigrateStorageAndFinish(mDiskId);
                        }
                    }
                });

            } else {
                Log.e(TAG, "Failed to format " + mDiskId, e);
                Toast.makeText(FormatActivity.this, getString(R.string.storage_format_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        @Override
        public void onLoaderReset(Loader<Map<String, Object>> loader) {}
    }

    private static class FormatAsPublicTaskLoader
            extends SettingsAsyncTaskLoader<Map<String, Object>> {

        public static final String RESULT_EXCEPTION = "exception";

        private final StorageManager mStorageManager;
        private final String mDiskId;

        public FormatAsPublicTaskLoader(Context context, String diskId) {
            super(context);
            mStorageManager = getContext().getSystemService(StorageManager.class);
            mDiskId = diskId;
        }

        @Override
        protected void onDiscardResult(Map<String, Object> result) {}

        @Override
        public Map<String, Object> loadInBackground() {
            final Map<String, Object> result = new ArrayMap<>(3);
            try {
                final List<VolumeInfo> volumes = mStorageManager.getVolumes();
                for (final VolumeInfo volume : volumes) {
                    if (TextUtils.equals(mDiskId, volume.getDiskId()) &&
                            volume.getType() == VolumeInfo.TYPE_PRIVATE) {
                        mStorageManager.forgetVolume(volume.getFsUuid());
                    }
                }

                mStorageManager.partitionPublic(mDiskId);
            } catch (Exception e) {
                result.put(RESULT_EXCEPTION, e);
            }
            return result;
        }
    }

    private class FormatAsPublicLoaderCallback
            implements LoaderManager.LoaderCallbacks<Map<String, Object>> {

        private final String mDiskId;
        private final String mDescription;

        public FormatAsPublicLoaderCallback(String diskId, String description) {
            mDiskId = diskId;
            mDescription = description;
        }

        @Override
        public Loader<Map<String, Object>> onCreateLoader(int id, Bundle args) {
            return new FormatAsPublicTaskLoader(FormatActivity.this, mDiskId);
        }

        @Override
        public void onLoadFinished(Loader<Map<String, Object>> loader, Map<String, Object> data) {
            if (data == null) {
                // No results yet, wait for something interesting to come in.
                return;
            }

            final Exception e = (Exception) data.get(FormatAsPublicTaskLoader.RESULT_EXCEPTION);
            if (e == null) {
                Toast.makeText(FormatActivity.this, getString(R.string.storage_format_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to format " + mDiskId, e);
                Toast.makeText(FormatActivity.this, getString(R.string.storage_format_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
            finish();
        }

        @Override
        public void onLoaderReset(Loader<Map<String, Object>> loader) {}
    }

    @Override
    public void onRequestMovePackageToVolume(String packageName, VolumeInfo destination) {
        mAppMoveId = mPackageManager.movePackage(packageName, destination);
        final ApplicationInfo applicationInfo;
        try {
            applicationInfo = mPackageManager
                    .getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }

        final MoveAppProgressFragment fragment = MoveAppProgressFragment
                .newInstance(mPackageManager.getApplicationLabel(applicationInfo));

        getFragmentManager().beginTransaction()
                .addToBackStack(MOVE_PROGRESS_DIALOG_BACKSTACK_TAG)
                .replace(android.R.id.content, fragment)
                .commit();

    }

    @Override
    public void onRequestFormatAsPrivate(String diskId) {
        final FormattingProgressFragment fragment = FormattingProgressFragment.newInstance();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();

        mFormatAsPrivateDiskId = diskId;
        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        for (final VolumeInfo volume : volumes) {
            if ((volume.getType() == VolumeInfo.TYPE_PRIVATE ||
                    volume.getType() == VolumeInfo.TYPE_PUBLIC) &&
                    TextUtils.equals(volume.getDiskId(), diskId)) {
                mFormatDiskDesc = mStorageManager.getBestVolumeDescription(volume);
            }
        }
        if (TextUtils.isEmpty(mFormatDiskDesc)) {
            final DiskInfo info = mStorageManager.findDiskById(diskId);
            if (info != null) {
                mFormatDiskDesc = info.getDescription();
            }
        }
        kickFormatAsPrivateLoader();
    }

    private void kickFormatAsPrivateLoader() {
        if (!TextUtils.isEmpty(mFormatAsPrivateDiskId)) {
            getLoaderManager().initLoader(LOADER_FORMAT_AS_PRIVATE, null,
                    new FormatAsPrivateLoaderCallback(mFormatAsPrivateDiskId, mFormatDiskDesc));
        }
    }

    private void launchMigrateStorageAndFinish(String diskId) {
        final List<VolumeInfo> candidates =
                mPackageManager.getPrimaryStorageCandidateVolumes();
        VolumeInfo moveTarget = null;
        for (final VolumeInfo candidate : candidates) {
            if (TextUtils.equals(candidate.getDiskId(), diskId)) {
                moveTarget = candidate;
                break;
            }
        }

        if (moveTarget != null) {
            startActivity(MigrateStorageActivity.getLaunchIntent(this, moveTarget.getId(), true));
        }

        finish();
    }

    @Override
    public void onRequestFormatAsPublic(String diskId, String volumeId) {
        final FormattingProgressFragment fragment = FormattingProgressFragment.newInstance();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();

        mFormatAsPublicDiskId = diskId;
        if (!TextUtils.isEmpty(volumeId)) {
            final VolumeInfo info = mStorageManager.findVolumeById(volumeId);
            if (info != null) {
                mFormatDiskDesc = mStorageManager.getBestVolumeDescription(info);
            }
        }
        if (TextUtils.isEmpty(mFormatDiskDesc)) {
            final DiskInfo info = mStorageManager.findDiskById(diskId);
            if (info != null) {
                mFormatDiskDesc = info.getDescription();
            }
        }
        kickFormatAsPublicLoader();
    }

    private void kickFormatAsPublicLoader() {
        if (!TextUtils.isEmpty(mFormatAsPublicDiskId)) {
            getLoaderManager().initLoader(LOADER_FORMAT_AS_PUBLIC, null,
                    new FormatAsPublicLoaderCallback(mFormatAsPublicDiskId, mFormatDiskDesc));
        }
    }

    @Override
    public void onCancelFormatDialog() {
        finish();
    }

    @Override
    public void onSlowDriveWarningComplete() {
        launchMigrateStorageAndFinish(mFormatAsPrivateDiskId);
    }
}
