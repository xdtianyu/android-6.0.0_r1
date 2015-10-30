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

package com.android.tv.settings.device;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementDetails;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementReceiver;
import com.android.tv.settings.R;
import com.android.tv.settings.device.apps.AppsActivity;
import com.android.tv.settings.device.storage.ForgetPrivateStepFragment;
import com.android.tv.settings.device.storage.FormatActivity;
import com.android.tv.settings.device.storage.MigrateStorageActivity;
import com.android.tv.settings.device.storage.NewStorageActivity;
import com.android.tv.settings.device.storage.UnmountActivity;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.Layout.Action;
import com.android.tv.settings.dialog.Layout.Header;
import com.android.tv.settings.dialog.Layout.Static;
import com.android.tv.settings.dialog.Layout.Status;
import com.android.tv.settings.dialog.Layout.StringGetter;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity to view storage consumption and factory reset device.
 */
public class StorageResetActivity extends SettingsLayoutActivity
        implements  ForgetPrivateStepFragment.Callback {

    private static final String TAG = "StorageResetActivity";
    private static final long SIZE_CALCULATING = -1;
    private static final int ACTION_RESET_DEVICE = 1;
    private static final int ACTION_CANCEL = 2;
    private static final int ACTION_CLEAR_CACHE = 3;
    private static final int ACTION_EJECT_PRIVATE = 4;
    private static final int ACTION_EJECT_PUBLIC = 5;
    private static final int ACTION_ERASE_PRIVATE = 6;
    private static final int ACTION_ERASE_PUBLIC = 7;
    private static final int ACTION_FORGET = 8;

    /**
     * Support for shutdown-after-reset. If our launch intent has a true value for
     * the boolean extra under the following key, then include it in the intent we
     * use to trigger a factory reset. This will cause us to shut down instead of
     * restart after the reset.
     */
    private static final String SHUTDOWN_INTENT_EXTRA = "shutdown";

    private static final String FORGET_DIALOG_BACKSTACK_TAG = "forgetDialog";

    private class SizeStringGetter extends StringGetter {
        private long mSize = SIZE_CALCULATING;

        @Override
        public String get() {
            return String.format(getString(R.string.storage_size), formatSize(mSize));
        }

        public void setSize(long size) {
            mSize = size;
            refreshView();
        }
    }

    private StorageManager mStorageManager;

    private final Map<String, StorageLayoutGetter> mStorageLayoutGetters = new ArrayMap<>();
    private final Map<String, StorageLayoutGetter> mMissingStorageLayoutGetters = new ArrayMap<>();

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            switch(vol.getType()) {
                case VolumeInfo.TYPE_PRIVATE:
                case VolumeInfo.TYPE_PUBLIC:
                    mStorageHeadersGetter.refreshView();
                    StorageLayoutGetter getter = mStorageLayoutGetters.get(vol.getDiskId());
                    if (getter != null) {
                        getter.onVolumeUpdated();
                    }
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorageManager = getSystemService(StorageManager.class);
        mStorageHeadersGetter.refreshView();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
        for (StorageLayoutGetter getter : mStorageLayoutGetters.values()) {
            getter.startListening();
        }
    }

    @Override
    protected void onPause() {
        mStorageManager.unregisterListener(mStorageListener);
        for (StorageLayoutGetter getter : mStorageLayoutGetters.values()) {
            getter.stopListening();
        }
        super.onPause();
    }

    @Override
    public Layout createLayout() {
        return new Layout().breadcrumb(getString(R.string.header_category_device))
                .add(new Header.Builder(getResources())
                        .icon(R.drawable.ic_settings_storage)
                        .title(R.string.device_storage_reset)
                        .build()
                        .add(mStorageHeadersGetter)
                        .add(new Static.Builder(getResources())
                                .title(R.string.storage_reset_section)
                                .build())
                        .add(createResetHeaders())
                );
    }

    private final Layout.LayoutGetter mStorageHeadersGetter = new Layout.LayoutGetter() {
        @Override
        public Layout get() {
            final Resources res = getResources();
            final Layout layout = new Layout();
            if (mStorageManager == null) {
                return layout;
            }
            final List<VolumeInfo> volumes = mStorageManager.getVolumes();
            Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

            final List<VolumeInfo> privateVolumes = new ArrayList<>(volumes.size());
            final List<VolumeInfo> publicVolumes = new ArrayList<>(volumes.size());

            // Find mounted volumes
            for (final VolumeInfo vol : volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                    privateVolumes.add(vol);
                } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                    publicVolumes.add(vol);
                } else {
                    Log.d(TAG, "Skipping volume " + vol.toString());
                }
            }

            // Find missing private filesystems
            final List<VolumeRecord> volumeRecords = mStorageManager.getVolumeRecords();
            final List<String> privateMissingFsUuids = new ArrayList<>(volumeRecords.size());

            for (final VolumeRecord record : volumeRecords) {
                if (record.getType() == VolumeInfo.TYPE_PRIVATE
                        && mStorageManager.findVolumeByUuid(record.getFsUuid()) == null) {
                    privateMissingFsUuids.add(record.getFsUuid());
                }
            }

            // Find unreadable disks
            final List<DiskInfo> disks = mStorageManager.getDisks();
            final List<String> unsupportedDiskIds = new ArrayList<>(disks.size());
            for (final DiskInfo disk : disks) {
                if (disk.volumeCount == 0 && disk.size > 0) {
                    unsupportedDiskIds.add(disk.getId());
                }
            }

            // Add device section if needed
            if (privateVolumes.size() + privateMissingFsUuids.size() > 0) {
                layout.add(new Static.Builder(res)
                        .title(R.string.storage_device_storage_section)
                        .build());
            }

            // Add private headers
            for (final VolumeInfo vol : privateVolumes) {
                layout.add(getVolumeHeader(res, vol));
            }
            for (final String fsUuid : privateMissingFsUuids) {
                layout.add(getMissingVolumeHeader(res, fsUuid));
            }

            // Add removable section if needed
            if (publicVolumes.size() + unsupportedDiskIds.size() > 0) {
                layout.add(new Static.Builder(res)
                        .title(R.string.storage_removable_storage_section)
                        .build());
            }

            // Add public headers
            for (final VolumeInfo vol : publicVolumes) {
                layout.add(getVolumeHeader(res, vol));
            }
            for (final String diskId : unsupportedDiskIds) {
                layout.add(getUnsupportedDiskAction(res, diskId));
            }
            return layout;
        }

        private Header getVolumeHeader(Resources res, VolumeInfo vol) {
            final String diskId = vol.getDiskId();
            final String fsUuid = vol.getFsUuid();
            StorageLayoutGetter storageGetter = mStorageLayoutGetters.get(diskId);
            if (storageGetter == null) {
                storageGetter = new StorageLayoutGetter(diskId, fsUuid);
                mStorageLayoutGetters.put(diskId, storageGetter);
                if (isResumed()) {
                    storageGetter.startListening();
                }
            }
            return new Header.Builder(res)
                    .title(mStorageManager.getBestVolumeDescription(vol))
                    .description(getSize(vol))
                    .build().add(storageGetter);
        }

        private Header getMissingVolumeHeader(Resources res, String fsUuid) {
            StorageLayoutGetter storageGetter = mMissingStorageLayoutGetters.get(fsUuid);
            if (storageGetter == null) {
                storageGetter = new StorageLayoutGetter(null, fsUuid);
                mMissingStorageLayoutGetters.put(fsUuid, storageGetter);
            }
            final VolumeRecord volumeRecord = mStorageManager.findRecordByUuid(fsUuid);
            return new Header.Builder(res)
                    .title(volumeRecord.getNickname())
                    .description(R.string.storage_not_connected)
                    .build().add(storageGetter);
        }

        private Action getUnsupportedDiskAction(Resources res, String diskId) {
            final DiskInfo info = mStorageManager.findDiskById(diskId);
            return new Action.Builder(res,
                    NewStorageActivity.getNewStorageLaunchIntent(StorageResetActivity.this, null,
                            diskId))
                    .title(info.getDescription())
                    .build();
        }

        private String getSize(VolumeInfo vol) {
            final File path = vol.getPath();
            if (vol.isMountedReadable() && path != null) {
                return String.format(getString(R.string.storage_size),
                        formatSize(path.getTotalSpace()));
            } else {
                return null;
            }
        }
    };

    private class StorageLayoutGetter extends Layout.LayoutGetter {

        private final String mDiskId;
        private final String mFsUuid;

        private StorageMeasurement mMeasure;
        private final SizeStringGetter mAppsSize = new SizeStringGetter();
        private final SizeStringGetter mDcimSize = new SizeStringGetter();
        private final SizeStringGetter mMusicSize = new SizeStringGetter();
        private final SizeStringGetter mDownloadsSize = new SizeStringGetter();
        private final SizeStringGetter mCacheSize = new SizeStringGetter();
        private final SizeStringGetter mMiscSize = new SizeStringGetter();
        private final SizeStringGetter mAvailSize = new SizeStringGetter();

        private final MeasurementReceiver mReceiver = new MeasurementReceiver() {

            private MeasurementDetails mLastMeasurementDetails = null;

            @Override
            public void onDetailsChanged(MeasurementDetails details) {
                mLastMeasurementDetails = details;
                updateDetails(mLastMeasurementDetails);
            }
        };

        public StorageLayoutGetter(String diskId, String fsUuid) {
            mDiskId = diskId;
            mFsUuid = fsUuid;
        }

        @Override
        public Layout get() {
            final Resources res = getResources();
            final Layout layout = new Layout();

            VolumeInfo volume = getVolumeInfo();

            if (volume == null) {
                final VolumeRecord volumeRecord = !TextUtils.isEmpty(mFsUuid) ?
                        mStorageManager.findRecordByUuid(mFsUuid) : null;
                if (volumeRecord == null || volumeRecord.getType() == VolumeInfo.TYPE_PUBLIC) {
                    layout
                            .add(new Status.Builder(res)
                                    .title(R.string.storage_not_connected)
                                    .build());
                } else {
                    addPrivateMissingHeaders(layout, mFsUuid);
                }
            } else if (volume.getType() == VolumeInfo.TYPE_PRIVATE) {
                if (volume.getState() == VolumeInfo.STATE_UNMOUNTED) {
                    addPrivateMissingHeaders(layout, mFsUuid);
                } else {
                    addPrivateStorageHeaders(layout, volume);
                }
            } else {
                if (volume.getState() == VolumeInfo.STATE_UNMOUNTED) {
                    addPublicUnmountedHeaders(layout, volume);
                } else {
                    final Bundle data = new Bundle(2);
                    data.putString(VolumeInfo.EXTRA_VOLUME_ID, volume.getId());
                    data.putString(DiskInfo.EXTRA_DISK_ID, mDiskId);
                    layout
                            .add(new Action.Builder(res, ACTION_EJECT_PUBLIC)
                                    .title(R.string.storage_eject)
                                    .data(data)
                                    .build())
                            .add(new Action.Builder(res, ACTION_ERASE_PUBLIC)
                                    .title(R.string.storage_format_for_private)
                                    .data(data)
                                    .build())
                            .add(new Status.Builder(res)
                                    .title(R.string.storage_media_misc_usage)
                                    .icon(R.drawable.storage_indicator_misc)
                                    .description(mMiscSize)
                                    .build())
                            .add(new Status.Builder(res)
                                    .title(R.string.storage_available)
                                    .icon(R.drawable.storage_indicator_available)
                                    .description(mAvailSize)
                                    .build());
                }
            }
            return layout;
        }

        private @Nullable VolumeInfo getVolumeInfo() {
            if (TextUtils.isEmpty(mFsUuid) && TextUtils.isEmpty(mDiskId)) {
                // Means private internal
                return mStorageManager.findVolumeById(VolumeInfo.ID_PRIVATE_INTERNAL);
            }

            if (!TextUtils.isEmpty(mFsUuid)) {
                final VolumeInfo volume = mStorageManager.findVolumeByUuid(mFsUuid);
                if (volume != null) {
                    return volume;
                }
            }

            if (!TextUtils.isEmpty(mDiskId)) {
                final List<VolumeInfo> volumes = mStorageManager.getVolumes();
                for (final VolumeInfo v : volumes) {
                    if (TextUtils.equals(v.getDiskId(), mDiskId)) {
                        return v;
                    }
                }
            }

            return null;
        }

        private void addPublicUnmountedHeaders(Layout layout, VolumeInfo volume) {
            final Resources res = getResources();
            final String volumeDescription = mStorageManager.getBestVolumeDescription(volume);

            layout
                    .add(new Status.Builder(res)
                            .title(getString(R.string.storage_unmount_success,
                                    volumeDescription))
                            .enabled(false)
                            .build());
        }

        private void addPrivateMissingHeaders(Layout layout, String fsUuid) {
            final Resources res = getResources();
            final Bundle data = new Bundle(1);
            data.putString(VolumeRecord.EXTRA_FS_UUID, fsUuid);
            final VolumeRecord volumeRecord = mStorageManager.findRecordByUuid(fsUuid);

            layout
                    .add(new Layout.WallOfText.Builder(res)
                            .title(getString(R.string.storage_forget_wall_of_text,
                                    volumeRecord.getNickname()))
                            .build())
                    .add(new Action.Builder(res, ACTION_FORGET)
                            .title(getString(R.string.storage_forget))
                            .data(data)
                            .build());
        }

        private void addPrivateStorageHeaders(Layout layout, VolumeInfo volume) {
            final Resources res = getResources();
            final Bundle data = new Bundle(2);
            final String volumeId = volume.getId();
            data.putString(VolumeInfo.EXTRA_VOLUME_ID, volumeId);
            data.putString(DiskInfo.EXTRA_DISK_ID, mDiskId);

            final String volumeUuid = volume.getFsUuid();
            final String volumeDescription = mStorageManager.getBestVolumeDescription(volume);

            boolean showMigrate = false;
            final VolumeInfo currentExternal = getPackageManager().getPrimaryStorageCurrentVolume();
            if (!TextUtils.equals(currentExternal.getId(), volumeId)) {
                final List<VolumeInfo> candidates =
                        getPackageManager().getPrimaryStorageCandidateVolumes();
                for (final VolumeInfo candidate : candidates) {
                    if (TextUtils.equals(candidate.getId(), volumeId)) {
                        showMigrate = true;
                        break;
                    }
                }
            }

            if (showMigrate) {
                layout
                        .add(new Action.Builder(res, MigrateStorageActivity.getLaunchIntent(
                                        StorageResetActivity.this, volumeId, true))
                                .title(R.string.storage_migrate)
                                .data(data)
                                .build());
            }

            if (!VolumeInfo.ID_PRIVATE_INTERNAL.equals(volumeId)) {
                layout
                        .add(new Action.Builder(res, ACTION_EJECT_PRIVATE)
                                .title(R.string.storage_eject)
                                .data(data)
                                .build())
                        .add(new Action.Builder(res, ACTION_ERASE_PRIVATE)
                                .title(R.string.storage_format)
                                .data(data)
                                .build());
            }
            layout
                    .add(new Action.Builder(res,
                            new Intent(StorageResetActivity.this, AppsActivity.class)
                                    .putExtra(AppsActivity.EXTRA_VOLUME_UUID, volumeUuid)
                                    .putExtra(AppsActivity.EXTRA_VOLUME_NAME,
                                            volumeDescription))
                            .title(R.string.storage_apps_usage)
                            .icon(R.drawable.storage_indicator_apps)
                            .description(mAppsSize)
                            .build())
                    .add(new Status.Builder(res)
                            .title(R.string.storage_dcim_usage)
                            .icon(R.drawable.storage_indicator_dcim)
                            .description(mDcimSize)
                            .build())
                    .add(new Status.Builder(res)
                            .title(R.string.storage_music_usage)
                            .icon(R.drawable.storage_indicator_music)
                            .description(mMusicSize)
                            .build())
                    .add(new Status.Builder(res)
                            .title(R.string.storage_downloads_usage)
                            .icon(R.drawable.storage_indicator_downloads)
                            .description(mDownloadsSize)
                            .build())
                    .add(new Action.Builder(res, ACTION_CLEAR_CACHE)
                            .title(R.string.storage_media_cache_usage)
                            .icon(R.drawable.storage_indicator_cache)
                            .description(mCacheSize)
                            .build())
                    .add(new Status.Builder(res)
                            .title(R.string.storage_media_misc_usage)
                            .icon(R.drawable.storage_indicator_misc)
                            .description(mMiscSize)
                            .build())
                    .add(new Status.Builder(res)
                            .title(R.string.storage_available)
                            .icon(R.drawable.storage_indicator_available)
                            .description(mAvailSize)
                            .build());
        }

        public void onVolumeUpdated() {
            stopListening();
            startListening();
            refreshView();
        }

        public void startListening() {
            VolumeInfo volume = getVolumeInfo();

            if (volume != null && volume.isMountedReadable()) {
                final VolumeInfo sharedVolume = mStorageManager.findEmulatedForPrivate(volume);
                mMeasure = new StorageMeasurement(StorageResetActivity.this, volume,
                        sharedVolume);
                mMeasure.setReceiver(mReceiver);
                mMeasure.forceMeasure();
            }
        }

        public void stopListening() {
            if (mMeasure != null) {
                mMeasure.onDestroy();
            }
        }

        private void updateDetails(MeasurementDetails details) {
            final int currentUser = ActivityManager.getCurrentUser();
            final long dcimSize = totalValues(details.mediaSize.get(currentUser),
                    Environment.DIRECTORY_DCIM,
                    Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES);

            final long musicSize = totalValues(details.mediaSize.get(currentUser),
                    Environment.DIRECTORY_MUSIC,
                    Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS,
                    Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);

            final long downloadsSize = totalValues(details.mediaSize.get(currentUser),
                    Environment.DIRECTORY_DOWNLOADS);

            mAvailSize.setSize(details.availSize);
            mAppsSize.setSize(details.appsSize.get(currentUser));
            mDcimSize.setSize(dcimSize);
            mMusicSize.setSize(musicSize);
            mDownloadsSize.setSize(downloadsSize);
            mCacheSize.setSize(details.cacheSize);
            mMiscSize.setSize(details.miscSize.get(currentUser));
        }
    }

    private Header createResetHeaders() {
        final Resources res = getResources();
        return new Header.Builder(res)
                .title(R.string.device_reset)
                .build()
                .add(new Header.Builder(res)
                        .title(R.string.device_reset)
                        .build()
                        .add(new Action.Builder(res, ACTION_RESET_DEVICE)
                                .title(R.string.confirm_factory_reset_device)
                                .build()
                        )
                        .add(new Action.Builder(res, Action.ACTION_BACK)
                                .title(R.string.title_cancel)
                                .defaultSelection()
                                .build())
                )
                .add(new Action.Builder(res, Action.ACTION_BACK)
                        .title(R.string.title_cancel)
                        .defaultSelection()
                        .build());
    }

    @Override
    public void onActionClicked(Action action) {
        switch (action.getId()) {
            case ACTION_RESET_DEVICE:
                if (!ActivityManager.isUserAMonkey()) {
                    Intent resetIntent = new Intent("android.intent.action.MASTER_CLEAR");
                    if (getIntent().getBooleanExtra(SHUTDOWN_INTENT_EXTRA, false)) {
                        resetIntent.putExtra(SHUTDOWN_INTENT_EXTRA, true);
                    }
                    sendBroadcast(resetIntent);
                }
                break;
            case ACTION_CANCEL:
                goBackToTitle(getString(R.string.device_storage_reset));
                break;
            case ACTION_CLEAR_CACHE:
                final DialogFragment fragment = ConfirmClearCacheFragment.newInstance();
                fragment.show(getFragmentManager(), null);
                break;
            case ACTION_EJECT_PUBLIC:
                onBackPressed();
                onRequestUnmount(action.getData().getString(VolumeInfo.EXTRA_VOLUME_ID));
                break;
            case ACTION_EJECT_PRIVATE:
                onBackPressed();
                onRequestUnmount(action.getData().getString(VolumeInfo.EXTRA_VOLUME_ID));
                break;
            case ACTION_ERASE_PUBLIC: {
                // When we erase a public volume, we're intending to use it as a private volume,
                // so launch the format-as-private wizard.
                startActivity(FormatActivity.getFormatAsPrivateIntent(this,
                        action.getData().getString(DiskInfo.EXTRA_DISK_ID)));
                break;
            }
            case ACTION_ERASE_PRIVATE: {
                // When we erase a private volume, we're intending to use it as a public volume,
                // so launch the format-as-public wizard.
                startActivity(FormatActivity.getFormatAsPublicIntent(this,
                        action.getData().getString(DiskInfo.EXTRA_DISK_ID)));
            }
                break;
            case ACTION_FORGET:
                final Fragment f =
                        ForgetPrivateStepFragment.newInstance(
                                action.getData().getString(VolumeRecord.EXTRA_FS_UUID));
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, f)
                        .addToBackStack(FORGET_DIALOG_BACKSTACK_TAG)
                        .commit();
                break;
            default:
                final Intent intent = action.getIntent();
                if (intent != null) {
                    startActivity(intent);
                }
        }
    }

    private static long totalValues(HashMap<String, Long> map, String... keys) {
        long total = 0;
        if (map != null) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    total += map.get(key);
                }
            }
        } else {
            Log.w(TAG,
                    "MeasurementDetails mediaSize array does not have key for current user " +
                    ActivityManager.getCurrentUser());
        }
        return total;
    }

    private String formatSize(long size) {
        return (size == SIZE_CALCULATING) ? getString(R.string.storage_calculating_size)
                : Formatter.formatShortFileSize(this, size);
    }

    public void onRequestUnmount(String volumeId) {
        final VolumeInfo volumeInfo = mStorageManager.findVolumeById(volumeId);
        if (volumeInfo == null) {
            Toast.makeText(this, getString(R.string.storage_unmount_failure_cant_find),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        final String description = mStorageManager.getBestVolumeDescription(volumeInfo);
        final Intent intent = UnmountActivity.getIntent(this, volumeId, description);
        startActivity(intent);
    }

    /**
     * Dialog to request user confirmation before clearing all cache data.
     */
    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static ConfirmClearCacheFragment newInstance() {
            return new ConfirmClearCacheFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.device_storage_clear_cache_title);
            builder.setMessage(getString(R.string.device_storage_clear_cache_message));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final PackageManager pm = context.getPackageManager();
                    final List<PackageInfo> infos = pm.getInstalledPackages(0);
                    for (PackageInfo info : infos) {
                        pm.deleteApplicationCacheFiles(info.packageName, null);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    public static class MountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public MountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.mount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to mount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestForget(String fsUuid) {
        mStorageManager.forgetVolume(fsUuid);
        getFragmentManager().popBackStack(FORGET_DIALOG_BACKSTACK_TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onCancelForgetDialog() {
        getFragmentManager().popBackStack(FORGET_DIALOG_BACKSTACK_TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
