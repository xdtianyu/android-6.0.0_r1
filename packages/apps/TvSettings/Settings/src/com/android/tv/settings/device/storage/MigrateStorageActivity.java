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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.view.View;
import android.widget.Toast;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.ProgressDialogFragment;

import java.util.List;

public class MigrateStorageActivity extends Activity {

    public static final String EXTRA_SHOW_CONFIRMATION =
            "com.android.tv.settings.device.storage.MigrateStorageActivity.SHOW_CONFIRMATION";

    private static final String SAVE_STATE_MOVE_ID = "MigrateStorageActivity.MOVE_ID";

    private VolumeInfo mVolumeInfo;
    private String mVolumeDesc;
    private int mMoveId = -1;
    private final Handler mHandler = new Handler();
    private PackageManager mPackageManager;
    private final PackageManager.MoveCallback mMoveCallback = new PackageManager.MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (moveId != mMoveId || !PackageManager.isMoveStatusFinished(status)) {
                return;
            }
            if (status == PackageManager.MOVE_SUCCEEDED) {
                Toast.makeText(MigrateStorageActivity.this,
                        getString(R.string.storage_wizard_migrate_toast_success, mVolumeDesc),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MigrateStorageActivity.this,
                        getString(R.string.storage_wizard_migrate_toast_failure, mVolumeDesc),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    };

    public static Intent getLaunchIntent(Context context, String volumeId,
            boolean showConfirmation) {
        final Intent i = new Intent(context, MigrateStorageActivity.class);
        i.putExtra(VolumeInfo.EXTRA_VOLUME_ID, volumeId);
        i.putExtra(EXTRA_SHOW_CONFIRMATION, showConfirmation);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String volumeId = intent.getStringExtra(VolumeInfo.EXTRA_VOLUME_ID);
        final StorageManager storageManager = getSystemService(StorageManager.class);
        mVolumeInfo = storageManager.findVolumeById(volumeId);
        if (mVolumeInfo == null) {
            finish();
            return;
        }
        mVolumeDesc = storageManager.getBestVolumeDescription(mVolumeInfo);

        if (intent.getBooleanExtra(EXTRA_SHOW_CONFIRMATION, true)) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content,
                            MigrateConfirmationStepFragment.newInstance(mVolumeDesc))
                    .commit();
        } else {
            onConfirmProceed();
        }

        mPackageManager = getPackageManager();
        mPackageManager.registerMoveCallback(mMoveCallback, mHandler);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_MOVE_ID, mMoveId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getPackageManager().unregisterMoveCallback(mMoveCallback);
    }

    private void onConfirmCancel() {
        finish();
    }

    private void onConfirmProceed() {
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content,
                        MigrateProgressFragment.newInstance(mVolumeDesc))
                .commit();
        getFragmentManager().executePendingTransactions();
        mMoveId = mPackageManager.movePrimaryStorage(mVolumeInfo);
    }

    public static class MigrateConfirmationStepFragment extends StorageGuidedStepFragment {
        private static final String ARG_VOLUME_DESC = "volumeDesc";

        private static final int ACTION_CONFIRM = 1;
        private static final int ACTION_LATER = 2;

        public static MigrateConfirmationStepFragment newInstance(String volumeDescription) {
            final MigrateConfirmationStepFragment fragment = new MigrateConfirmationStepFragment();
            final Bundle b = new Bundle(1);
            b.putString(ARG_VOLUME_DESC, volumeDescription);
            fragment.setArguments(b);
            return fragment;
        }

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            final String driveDesc = getArguments().getString(ARG_VOLUME_DESC);
            return new GuidanceStylist.Guidance(
                    getString(R.string.storage_wizard_migrate_confirm_title, driveDesc),
                    getString(R.string.storage_wizard_migrate_confirm_description, driveDesc),
                    null,
                    getActivity().getDrawable(R.drawable.ic_settings_storage));
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions,
                Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder()
                    .id(ACTION_CONFIRM)
                    .title(getString(R.string.storage_wizard_migrate_confirm_action_move_now))
                    .build());
            actions.add(new GuidedAction.Builder()
                    .id(ACTION_LATER)
                    .title(getString(R.string.storage_wizard_migrate_confirm_action_move_later))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            final int id = (int) action.getId();
            switch (id) {
                case ACTION_CONFIRM:
                    ((MigrateStorageActivity) getActivity()).onConfirmProceed();
                    break;
                case ACTION_LATER:
                    ((MigrateStorageActivity) getActivity()).onConfirmCancel();
                    break;
            }
        }
    }

    public static class MigrateProgressFragment extends ProgressDialogFragment {
        private static final String ARG_VOLUME_DESC = "volumeDesc";

        public static MigrateProgressFragment newInstance(String volumeDescription) {
            final MigrateProgressFragment fragment = new MigrateProgressFragment();
            final Bundle b = new Bundle(1);
            b.putString(ARG_VOLUME_DESC, volumeDescription);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setTitle(getActivity().getString(R.string.storage_wizard_migrate_progress_title,
                    getArguments().getString(ARG_VOLUME_DESC)));
            setSummary(getActivity()
                    .getString(R.string.storage_wizard_migrate_progress_description));
        }

    }
}
