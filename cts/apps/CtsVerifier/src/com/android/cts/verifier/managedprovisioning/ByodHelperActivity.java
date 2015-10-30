/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.verifier.managedprovisioning;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import static android.provider.Settings.Secure.INSTALL_NON_MARKET_APPS;

import java.io.File;
import java.util.ArrayList;

import com.android.cts.verifier.R;
import com.android.cts.verifier.managedprovisioning.ByodPresentMediaDialog.DialogCallback;

/**
 * A helper activity from the managed profile side that responds to requests from CTS verifier in
 * primary user. Profile owner APIs are accessible inside this activity (given this activity is
 * started within the work profile). Its current functionalities include making sure the profile
 * owner is setup correctly, removing the work profile upon request, and verifying the image and
 * video capture functionality.
 *
 * Note: We have to use a dummy activity because cross-profile intents only work for activities.
 */
public class ByodHelperActivity extends Activity implements DialogCallback {
    static final String TAG = "ByodHelperActivity";

    // Primary -> managed intent: query if the profile owner has been set up.
    public static final String ACTION_QUERY_PROFILE_OWNER = "com.android.cts.verifier.managedprovisioning.BYOD_QUERY";
    // Managed -> primary intent: update profile owner test status in primary's CtsVerifer
    public static final String ACTION_PROFILE_OWNER_STATUS = "com.android.cts.verifier.managedprovisioning.BYOD_STATUS";
    // Primary -> managed intent: request to delete the current profile
    public static final String ACTION_REMOVE_PROFILE_OWNER = "com.android.cts.verifier.managedprovisioning.BYOD_REMOVE";
    // Managed -> managed intent: provisioning completed successfully
    public static final String ACTION_PROFILE_PROVISIONED = "com.android.cts.verifier.managedprovisioning.BYOD_PROVISIONED";
    // Primage -> managed intent: request to capture and check an image
    public static final String ACTION_CAPTURE_AND_CHECK_IMAGE = "com.android.cts.verifier.managedprovisioning.BYOD_CAPTURE_AND_CHECK_IMAGE";
    // Primage -> managed intent: request to capture and check a video
    public static final String ACTION_CAPTURE_AND_CHECK_VIDEO = "com.android.cts.verifier.managedprovisioning.BYOD_CAPTURE_AND_CHECK_VIDEO";
    // Primage -> managed intent: request to capture and check an audio recording
    public static final String ACTION_CAPTURE_AND_CHECK_AUDIO = "com.android.cts.verifier.managedprovisioning.BYOD_CAPTURE_AND_CHECK_AUDIO";
    public static final String ACTION_KEYGUARD_DISABLED_FEATURES =
            "com.android.cts.verifier.managedprovisioning.BYOD_KEYGUARD_DISABLED_FEATURES";
    public static final String ACTION_LOCKNOW =
            "com.android.cts.verifier.managedprovisioning.BYOD_LOCKNOW";
    public static final String ACTION_TEST_NFC_BEAM = "com.android.cts.verifier.managedprovisioning.TEST_NFC_BEAM";

    public static final String EXTRA_PROVISIONED = "extra_provisioned";
    public static final String EXTRA_PARAMETER_1 = "extra_parameter_1";

    // Primary -> managed intent: set unknown sources restriction and install package
    public static final String ACTION_INSTALL_APK = "com.android.cts.verifier.managedprovisioning.BYOD_INSTALL_APK";
    public static final String EXTRA_ALLOW_NON_MARKET_APPS = INSTALL_NON_MARKET_APPS;

    // Primary -> managed intent: check if the required cross profile intent filters are set.
    public static final String ACTION_CHECK_INTENT_FILTERS =
            "com.android.cts.verifier.managedprovisioning.action.CHECK_INTENT_FILTERS";

    // Primary -> managed intent: will send a cross profile intent and check if the user sees an
    // intent picker dialog and can open the apps.
    public static final String ACTION_TEST_CROSS_PROFILE_INTENTS_DIALOG =
            "com.android.cts.verifier.managedprovisioning.action.TEST_CROSS_PROFILE_INTENTS_DIALOG";

    // Primary -> managed intent: will send an app link intent and check if the user sees a
    // dialog and can open the apps. This test is extremely similar to
    // ACTION_TEST_CROSS_PROFILE_INTENTS_DIALOG, but the intent used is a web intent, and there is
    // some behavior which is specific to web intents.
    public static final String ACTION_TEST_APP_LINKING_DIALOG =
            "com.android.cts.verifier.managedprovisioning.action.TEST_APP_LINKING_DIALOG";

    public static final int RESULT_FAILED = RESULT_FIRST_USER;

    private static final int REQUEST_INSTALL_PACKAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_VIDEO_CAPTURE = 3;
    private static final int REQUEST_AUDIO_CAPTURE = 4;

    private static final String ORIGINAL_SETTINGS_NAME = "original settings";
    private Bundle mOriginalSettings;

    private ComponentName mAdminReceiverComponent;
    private DevicePolicyManager mDevicePolicyManager;

    private Uri mImageUri;
    private Uri mVideoUri;

    private ArrayList<File> mTempFiles = new ArrayList<File>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Log.w(TAG, "Restored state");
            mOriginalSettings = savedInstanceState.getBundle(ORIGINAL_SETTINGS_NAME);
        } else {
            mOriginalSettings = new Bundle();
        }

        mAdminReceiverComponent = new ComponentName(this, DeviceAdminTestReceiver.class.getName());
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        Intent intent = getIntent();
        String action = intent.getAction();
        Log.d(TAG, "ByodHelperActivity.onCreate: " + action);

        // we are explicitly started by {@link DeviceAdminTestReceiver} after a successful provisioning.
        if (action.equals(ACTION_PROFILE_PROVISIONED)) {
            // Jump back to CTS verifier with result.
            Intent response = new Intent(ACTION_PROFILE_OWNER_STATUS);
            response.putExtra(EXTRA_PROVISIONED, isProfileOwner());
            startActivityInPrimary(response);
            // Queried by CtsVerifier in the primary side using startActivityForResult.
        } else if (action.equals(ACTION_QUERY_PROFILE_OWNER)) {
            Intent response = new Intent();
            response.putExtra(EXTRA_PROVISIONED, isProfileOwner());
            setResult(RESULT_OK, response);
            // Request to delete work profile.
        } else if (action.equals(ACTION_REMOVE_PROFILE_OWNER)) {
            if (isProfileOwner()) {
                mDevicePolicyManager.wipeData(0);
                showToast(R.string.provisioning_byod_profile_deleted);
            }
        } else if (action.equals(ACTION_INSTALL_APK)) {
            boolean allowNonMarket = intent.getBooleanExtra(EXTRA_ALLOW_NON_MARKET_APPS, false);
            boolean wasAllowed = getAllowNonMarket();

            // Update permission to install non-market apps
            setAllowNonMarket(allowNonMarket);
            mOriginalSettings.putBoolean(INSTALL_NON_MARKET_APPS, wasAllowed);

            // Request to install a non-market application- easiest way is to reinstall ourself
            final Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                    .setData(Uri.parse("package:" + getPackageName()))
                    .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    .putExtra(Intent.EXTRA_RETURN_RESULT, true);
            startActivityForResult(installIntent, REQUEST_INSTALL_PACKAGE);

            // Not yet ready to finish- wait until the result comes back
            return;
            // Queried by CtsVerifier in the primary side using startActivityForResult.
        } else if (action.equals(ACTION_CHECK_INTENT_FILTERS)) {
            final boolean intentFiltersSetForManagedIntents =
                    new IntentFiltersTestHelper(this).checkCrossProfileIntentFilters(
                            IntentFiltersTestHelper.FLAG_INTENTS_FROM_MANAGED);
            setResult(intentFiltersSetForManagedIntents? RESULT_OK : RESULT_FAILED, null);
        } else if (action.equals(ACTION_CAPTURE_AND_CHECK_IMAGE)) {
            // We need the camera permission to send the image capture intent.
            grantCameraPermissionToSelf();
            Intent captureImageIntent = getCaptureImageIntent();
            mImageUri = getTempUri("image.jpg");
            captureImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
            if (captureImageIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(captureImageIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Log.e(TAG, "Capture image intent could not be resolved in managed profile.");
                showToast(R.string.provisioning_byod_capture_media_error);
                finish();
            }
            return;
        } else if (action.equals(ACTION_CAPTURE_AND_CHECK_VIDEO)) {
            // We need the camera permission to send the video capture intent.
            grantCameraPermissionToSelf();
            Intent captureVideoIntent = getCaptureVideoIntent();
            mVideoUri = getTempUri("video.mp4");
            captureVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mVideoUri);
            if (captureVideoIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(captureVideoIntent, REQUEST_VIDEO_CAPTURE);
            } else {
                Log.e(TAG, "Capture video intent could not be resolved in managed profile.");
                showToast(R.string.provisioning_byod_capture_media_error);
                finish();
            }
            return;
        } else if (action.equals(ACTION_CAPTURE_AND_CHECK_AUDIO)) {
            Intent captureAudioIntent = getCaptureAudioIntent();
            if (captureAudioIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(captureAudioIntent, REQUEST_AUDIO_CAPTURE);
            } else {
                Log.e(TAG, "Capture audio intent could not be resolved in managed profile.");
                showToast(R.string.provisioning_byod_capture_media_error);
                finish();
            }
            return;
        } else if (ACTION_KEYGUARD_DISABLED_FEATURES.equals(action)) {
            final int value = intent.getIntExtra(EXTRA_PARAMETER_1,
                    DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE);
            ComponentName admin = DeviceAdminTestReceiver.getReceiverComponentName();
            mDevicePolicyManager.setKeyguardDisabledFeatures(admin, value);
        } else if (ACTION_LOCKNOW.equals(action)) {
            mDevicePolicyManager.lockNow();
            setResult(RESULT_OK);
        } else if (action.equals(ACTION_TEST_NFC_BEAM)) {
            Intent testNfcBeamIntent = new Intent(this, NfcTestActivity.class);
            testNfcBeamIntent.putExtras(intent);
            startActivity(testNfcBeamIntent);
            finish();
            return;
        } else if (action.equals(ACTION_TEST_CROSS_PROFILE_INTENTS_DIALOG)) {
            sendIntentInsideChooser(new Intent(
                    CrossProfileTestActivity.ACTION_CROSS_PROFILE_TO_PERSONAL));
        } else if (action.equals(ACTION_TEST_APP_LINKING_DIALOG)) {
            mDevicePolicyManager.addUserRestriction(
                    DeviceAdminTestReceiver.getReceiverComponentName(),
                    UserManager.ALLOW_PARENT_PROFILE_APP_LINKING);
            Intent toSend = new Intent(Intent.ACTION_VIEW);
            toSend.setData(Uri.parse("http://com.android.cts.verifier"));
            sendIntentInsideChooser(toSend);
        }
        // This activity has no UI and is only used to respond to CtsVerifier in the primary side.
        finish();
    }

    @Override
    protected void onSaveInstanceState(final Bundle savedState) {
        super.onSaveInstanceState(savedState);

        savedState.putBundle(ORIGINAL_SETTINGS_NAME, mOriginalSettings);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_INSTALL_PACKAGE: {
                Log.w(TAG, "Received REQUEST_INSTALL_PACKAGE, resultCode = " + resultCode);
                if (mOriginalSettings.containsKey(INSTALL_NON_MARKET_APPS)) {
                    // Restore original setting
                    setAllowNonMarket(mOriginalSettings.getBoolean(INSTALL_NON_MARKET_APPS));
                    mOriginalSettings.remove(INSTALL_NON_MARKET_APPS);
                }
                finish();
                break;
            }
            case REQUEST_IMAGE_CAPTURE: {
                if (resultCode == RESULT_OK) {
                    ByodPresentMediaDialog.newImageInstance(mImageUri)
                            .show(getFragmentManager(), "ViewImageDialogFragment");
                } else {
                    // Failed capturing image.
                    finish();
                }
                break;
            }
            case REQUEST_VIDEO_CAPTURE: {
                if (resultCode == RESULT_OK) {
                    ByodPresentMediaDialog.newVideoInstance(mVideoUri)
                            .show(getFragmentManager(), "PlayVideoDialogFragment");
                } else {
                    // Failed capturing video.
                    finish();
                }
                break;
            }
            case REQUEST_AUDIO_CAPTURE: {
                if (resultCode == RESULT_OK) {
                    ByodPresentMediaDialog.newAudioInstance(data.getData())
                            .show(getFragmentManager(), "PlayAudioDialogFragment");
                } else {
                    // Failed capturing audio.
                    finish();
                }
                break;
            }
            default: {
                Log.wtf(TAG, "Unknown requestCode " + requestCode + "; data = " + data);
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        cleanUpTempUris();
        super.onDestroy();
    }

    public static Intent getCaptureImageIntent() {
        return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    }

    public static Intent getCaptureVideoIntent() {
        return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    }

    public static Intent getCaptureAudioIntent() {
        return new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
    }

    public static Intent createLockIntent() {
        return new Intent(ACTION_LOCKNOW);
    }

    private Uri getTempUri(String fileName) {
        final File file = new File(getFilesDir() + File.separator + "images"
                + File.separator + fileName);
        file.getParentFile().mkdirs(); //if the folder doesn't exists it is created
        mTempFiles.add(file);
        return FileProvider.getUriForFile(this,
                    "com.android.cts.verifier.managedprovisioning.fileprovider", file);
    }

    private void cleanUpTempUris() {
        for (File file : mTempFiles) {
            file.delete();
        }
    }

    private boolean isProfileOwner() {
        return mDevicePolicyManager.isAdminActive(mAdminReceiverComponent) &&
                mDevicePolicyManager.isProfileOwnerApp(mAdminReceiverComponent.getPackageName());
    }

    private boolean getAllowNonMarket() {
        String value = Settings.Secure.getString(getContentResolver(), INSTALL_NON_MARKET_APPS);
        return "1".equals(value);
    }

    private void setAllowNonMarket(boolean allow) {
        mDevicePolicyManager.setSecureSetting(mAdminReceiverComponent, INSTALL_NON_MARKET_APPS,
                (allow ? "1" : "0"));
    }

    private void startActivityInPrimary(Intent intent) {
        // Disable app components in the current profile, so only the counterpart in the other
        // profile can respond (via cross-profile intent filter)
        getPackageManager().setComponentEnabledSetting(new ComponentName(
                this, ByodFlowTestActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        startActivity(intent);
    }

    private void showToast(int messageId) {
        String message = getString(messageId);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void grantCameraPermissionToSelf() {
        mDevicePolicyManager.setPermissionGrantState(mAdminReceiverComponent, getPackageName(),
                android.Manifest.permission.CAMERA,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
    }

    private void sendIntentInsideChooser(Intent toSend) {
        toSend.putExtra(CrossProfileTestActivity.EXTRA_STARTED_FROM_WORK, true);
        Intent chooser = Intent.createChooser(toSend,
                getResources().getString(R.string.provisioning_cross_profile_chooser));
        startActivity(chooser);
    }

    @Override
    public void onDialogClose() {
        finish();
    }
}
