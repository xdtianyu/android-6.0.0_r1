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
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.ProgressDialogFragment;
import com.android.tv.settings.util.SettingsAsyncTaskLoader;

import java.util.List;

public class UnmountActivity extends Activity {

    private static final String TAG = "UnmountActivity";

    public static final String EXTRA_VOLUME_DESC = "UnmountActivity.volumeDesc";

    private static final int LOADER_UNMOUNT = 0;

    private String mUnmountVolumeId;
    private String mUnmountVolumeDesc;
    // True if we're waiting for an unmount loader to complete
    private boolean mUnmounting;

    private final Handler mHandler = new Handler();

    public static Intent getIntent(Context context, String volumeId, String volumeDesc) {
        final Intent i = new Intent(context, UnmountActivity.class);
        i.putExtra(VolumeInfo.EXTRA_VOLUME_ID, volumeId);
        i.putExtra(EXTRA_VOLUME_DESC, volumeDesc);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUnmountVolumeId = getIntent().getStringExtra(VolumeInfo.EXTRA_VOLUME_ID);
        mUnmountVolumeDesc = getIntent().getStringExtra(EXTRA_VOLUME_DESC);

        if (savedInstanceState == null) {
            final StorageManager storageManager = getSystemService(StorageManager.class);
            final VolumeInfo volumeInfo = storageManager.findVolumeById(mUnmountVolumeId);

            if (volumeInfo == null) {
                // Unmounted already, just bail
                finish();
                return;
            }

            if (volumeInfo.getType() == VolumeInfo.TYPE_PRIVATE) {
                final Fragment fragment = UnmountInternalStepFragment.newInstance(mUnmountVolumeId);
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .commit();
            } else {
                // Jump straight to unmounting
                onRequestUnmount();
            }
        }
    }

    public void onRequestUnmount() {
        mUnmounting = true;
        final Fragment fragment = UnmountProgressFragment.newInstance(mUnmountVolumeDesc);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
        kickUnmountLoader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        kickUnmountLoader();
    }

    private void kickUnmountLoader() {
        if (mUnmounting) {
            getLoaderManager().initLoader(LOADER_UNMOUNT, null,
                    new UnmountLoaderCallback(mUnmountVolumeId, mUnmountVolumeDesc));
        }
    }

    private static class UnmountTaskLoader extends SettingsAsyncTaskLoader<Boolean> {

        private final StorageManager mStorageManager;
        private final String mVolumeId;

        public UnmountTaskLoader(Context context, String volumeId) {
            super(context);
            mStorageManager = context.getSystemService(StorageManager.class);
            mVolumeId = volumeId;
        }

        @Override
        protected void onDiscardResult(Boolean result) {}

        @Override
        public Boolean loadInBackground() {
            try {
                final long minTime = System.currentTimeMillis() + 3000;

                mStorageManager.unmount(mVolumeId);

                long waitTime = minTime - System.currentTimeMillis();
                while (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    waitTime = minTime - System.currentTimeMillis();
                }
                return true;
            } catch (Exception e) {
                Log.d(TAG, "Could not unmount", e);
                return false;
            }
        }
    }

    private class UnmountLoaderCallback implements LoaderManager.LoaderCallbacks<Boolean> {

        private final String mVolumeId;
        private final String mVolumeDescription;

        public UnmountLoaderCallback(String volumeId, String volumeDescription) {
            mVolumeId = volumeId;
            mVolumeDescription = volumeDescription;
        }

        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            return new UnmountTaskLoader(UnmountActivity.this, mVolumeId);
        }

        @Override
        public void onLoadFinished(Loader<Boolean> loader, final Boolean success) {
            if (success == null) {
                // No results yet, wait for something interesting to come in.
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isResumed() && TextUtils.equals(mUnmountVolumeId, mVolumeId)) {
                        if (success) {
                            Toast.makeText(UnmountActivity.this,
                                    getString(R.string.storage_unmount_success, mVolumeDescription),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UnmountActivity.this,
                                    getString(R.string.storage_unmount_failure, mVolumeDescription),
                                    Toast.LENGTH_SHORT).show();
                        }

                        mUnmountVolumeId = null;
                        mUnmountVolumeDesc = null;
                        getLoaderManager().destroyLoader(LOADER_UNMOUNT);

                        finish();
                    }
                }
            });
        }

        @Override
        public void onLoaderReset(Loader<Boolean> loader) {}
    }

    public static class UnmountInternalStepFragment extends StorageGuidedStepFragment {

        private static final int ACTION_ID_CANCEL = 0;
        private static final int ACTION_ID_UNMOUNT = 1;

        public static UnmountInternalStepFragment newInstance(String volumeId) {
            final UnmountInternalStepFragment fragment = new UnmountInternalStepFragment();
            final Bundle b = new Bundle(1);
            b.putString(VolumeInfo.EXTRA_VOLUME_ID, volumeId);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public @NonNull
        GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            return new GuidanceStylist.Guidance(
                    getString(R.string.storage_wizard_eject_internal_title),
                    getString(R.string.storage_wizard_eject_internal_description), "",
                    getActivity().getDrawable(R.drawable.ic_settings_storage));
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            actions.add(new GuidedAction.Builder()
                    .id(ACTION_ID_CANCEL)
                    .title(getString(android.R.string.cancel))
                    .build());
            actions.add(new GuidedAction.Builder()
                    .id(ACTION_ID_UNMOUNT)
                    .title(getString(R.string.storage_eject))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            final long id = action.getId();

            if (id == ACTION_ID_CANCEL) {
                getFragmentManager().popBackStack();
            } else if (id == ACTION_ID_UNMOUNT) {
                ((UnmountActivity) getActivity()).onRequestUnmount();
            }
        }
    }

    public static class UnmountProgressFragment extends ProgressDialogFragment {

        private static final String ARG_DESCRIPTION = "description";

        public static UnmountProgressFragment newInstance(CharSequence description) {
            final Bundle b = new Bundle(1);
            b.putCharSequence(ARG_DESCRIPTION, description);
            final UnmountProgressFragment fragment = new UnmountProgressFragment();
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            final CharSequence description = getArguments().getCharSequence(ARG_DESCRIPTION);
            // TODO: fix this string name
            setTitle(getActivity().getString(R.string.sotrage_wizard_eject_progress_title,
                    description));
        }
    }
}