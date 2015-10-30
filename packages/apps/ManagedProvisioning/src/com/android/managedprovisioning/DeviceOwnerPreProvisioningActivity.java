/*
 * Copyright 2015, The Android Open Source Project
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

package com.android.managedprovisioning;

import static android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;
import static android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.service.persistentdata.PersistentDataBlockManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.managedprovisioning.task.AddWifiNetworkTask;
import com.android.managedprovisioning.Utils.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.Utils.MdmPackageInfo;

/**
 * This activity makes necessary checks before starting {@link DeviceOwnerProvisioningActivity}.
 * It checks if the device or user is already setup. It makes sure the device is encrypted, the
 * provisioning intent extras are valid and the device is connected to a wifi network.
 */
public class DeviceOwnerPreProvisioningActivity extends SetupLayoutActivity
        implements UserConsentDialog.ConsentCallback {
    private static final boolean DEBUG = false; // To control logging.

    private static final String KEY_USER_CONSENTED = "user_consented";

    private static final int ENCRYPT_DEVICE_REQUEST_CODE = 1;
    private static final int WIFI_REQUEST_CODE = 2;
    private static final int PROVISIONING_REQUEST_CODE = 3;

    // Indicates whether user consented by clicking on positive button of consent dialog.
    private boolean mUserConsented = false;

    // Params that will be used after user consent.
    // Extracted from the starting intent.
    private ProvisioningParams mParams;

    // Legacy action, internal only, but that we still want to support.
    static final String LEGACY_ACTION_PROVISION_MANAGED_DEVICE
            = "com.android.managedprovisioning.ACTION_PROVISION_MANAGED_DEVICE";

    /**
     * Action used to start DeviceOwnerPreProvisioningActivity on a secondary user.
     */
    protected static final String ACTION_PROVISION_SECONDARY_USER
            = "com.android.managedprovisioning.ACTION_PROVISION_SECONDARY_USER";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) ProvisionLogger.logd("DeviceOwnerPreProvisioningActivity ONCREATE");

        if (savedInstanceState != null) {
            mUserConsented = savedInstanceState.getBoolean(KEY_USER_CONSENTED, false);
        }

        // Setup the UI.
        initializeLayoutParams(R.layout.user_consent, R.string.setup_work_device, false);
        configureNavigationButtons(R.string.set_up, View.INVISIBLE, View.VISIBLE);

        // Check whether we have already provisioned this user.
        if (Utils.isCurrentUserOwner()) {
            int provisioned =
                    Global.getInt(getContentResolver(), Global.DEVICE_PROVISIONED, 0 /*defaults*/);
            if (provisioned != 0) {
                showErrorAndClose(R.string.device_owner_error_already_provisioned,
                        "Device already provisioned.");
                return;
            }
        } else {
             int provisioned =
                    Secure.getInt(getContentResolver(), Secure.USER_SETUP_COMPLETE, 0 /*default*/);
            if (provisioned != 0) {
                showErrorAndClose(R.string.device_owner_error_already_provisioned_user,
                        "User already provisioned.");
                return;
            }
        }

        if (factoryResetProtected()) {
            showErrorAndClose(R.string.device_owner_error_frp,
                    "Factory reset protection blocks provisioning.");
            return;
        }

        // Parse the incoming intent.
        MessageParser parser = new MessageParser();
        try {
            mParams = parseIntentAndMaybeVerifyCaller(getIntent(),
                    getPackageName().equals(getCallingPackage()), parser);
        } catch (Utils.IllegalProvisioningArgumentException e) {
            showErrorAndClose(R.string.device_owner_error_general,
                    e.getMessage());
            return;
        }

        // Ask to encrypt the device before proceeding
        if (!(EncryptDeviceActivity.isDeviceEncrypted()
                        || SystemProperties.getBoolean("persist.sys.no_req_encrypt", false)
                        || mParams.skipEncryption)) {
            requestEncryption(parser, mParams);
            return;
            // System will reboot. Bootreminder will restart this activity.
        }

        // Have the user pick a wifi network if necessary.
        if (!NetworkMonitor.isConnectedToNetwork(this)
                && TextUtils.isEmpty(mParams.wifiInfo.ssid)
                // If a device initializer is installed, this shouldn't run on secondary users.
                && (!Utils.hasDeviceInitializer(this) || Utils.isCurrentUserOwner())) {
            requestWifiPick();
            return;
            // Wait for onActivityResult.
        }
        askForConsentOrStartProvisioning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(R.string.setup_device_start_setup);
    }

    private ProvisioningParams parseIntentAndMaybeVerifyCaller(Intent intent, boolean trusted,
            MessageParser parser) throws IllegalProvisioningArgumentException {
        if (intent.getAction() == null) {
            throw new IllegalProvisioningArgumentException("Null intent action.");
        } else if (intent.getAction().equals(ACTION_NDEF_DISCOVERED)) {
            return parser.parseNfcIntent(intent);
        } else if (intent.getAction().equals(LEGACY_ACTION_PROVISION_MANAGED_DEVICE)) {
            return parser.parseNonNfcIntent(intent, trusted);
        } else if (intent.getAction().equals(ACTION_PROVISION_SECONDARY_USER)) {
            if (Utils.isCurrentUserOwner()) {
                throw new IllegalProvisioningArgumentException("Permission denied. " +
                    "Was this activity started for a secondary user?");
            }
            return parser.parseNonNfcIntent(intent, trusted);
        } else if (intent.getAction().equals(ACTION_PROVISION_MANAGED_DEVICE)) {
            ProvisioningParams params = parser.parseMinimalistNonNfcIntent(intent);
            String callingPackage = getCallingPackage();
            if (callingPackage == null) {
                throw new IllegalProvisioningArgumentException("Calling package is null. " +
                    "Was startActivityForResult used to start this activity?");
            }
            if (!callingPackage.equals(params.inferDeviceAdminPackageName())) {
                throw new IllegalProvisioningArgumentException("Permission denied, "
                        + "calling package tried to set a different package as device owner. ");
            }
            return params;
        }
        throw new IllegalProvisioningArgumentException("Unknown intent action "
                + intent.getAction());
    }

    private boolean factoryResetProtected() {
        if (!Utils.isCurrentUserOwner()) {
            ProvisionLogger.logd("Reset protection check skipped on secondary users.");
            return false;
        }

        // If we are started by setup wizard, do not check for factory reset protection since setup
        // wizard must have already validated it. This avoids provisioning being blocked by FRP
        // if an account was added in the setup wizard on devices that enforce FRP as soon as the
        // account is added.
        if (getIntent() != null) {
            String action = getIntent().getAction();
            if (ACTION_PROVISION_MANAGED_DEVICE.equals(action) ||
                    LEGACY_ACTION_PROVISION_MANAGED_DEVICE.equals(action)) {
                ProvisionLogger.logd("FRP not required if started by SUW");
                return false;
            }
        }

        PersistentDataBlockManager pdbManager = (PersistentDataBlockManager)
                getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
        if (pdbManager == null) {
            ProvisionLogger.logd("Reset protection not supported.");
            return false;
        }
        int size = pdbManager.getDataBlockSize();
        ProvisionLogger.logd("Data block size: " + size);
        return size > 0;
    }

    private void askForConsentOrStartProvisioning() {
        // If we are started by Nfc and the device supports FRP, we need to ask for user consent
        // since FRP will not be activated at the end of the flow.
        if (mParams.startedByNfc && Utils.isFrpSupported(this)) {
            showUserConsentDialog();
        } else if (mUserConsented || mParams.startedByNfc || !Utils.isCurrentUserOwner()) {
            startDeviceOwnerProvisioning();
        } else {
            showStartProvisioningButton();
            TextView consentMessageTextView = (TextView) findViewById(R.id.user_consent_message);
            consentMessageTextView.setText(R.string.company_controls_device);
            TextView mdmInfoTextView = (TextView) findViewById(R.id.mdm_info_message);
            mdmInfoTextView.setText(R.string.the_following_is_your_mdm_for_device);
            setMdmInfo();
        }
    }

    private void startDeviceOwnerProvisioning() {
        Intent intent = new Intent(this, DeviceOwnerProvisioningActivity.class);
        intent.putExtra(ProvisioningParams.EXTRA_PROVISIONING_PARAMS, mParams);
        startActivityForResult(intent, PROVISIONING_REQUEST_CODE);
        // Set cross-fade transition animation into the interstitial progress activity.
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showErrorAndClose(int resourceId, String logText) {
                ProvisionLogger.loge(logText);
        new AlertDialog.Builder(this)
                .setTitle(R.string.provisioning_error_title)
                .setMessage(resourceId)
                .setCancelable(false)
                .setPositiveButton(R.string.device_owner_error_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,int id) {
                                // Close activity
                                DeviceOwnerPreProvisioningActivity.this.setResult(
                                        Activity.RESULT_CANCELED);
                                DeviceOwnerPreProvisioningActivity.this.finish();
                            }
                        })
                .show();
    }

    private void requestEncryption(MessageParser messageParser, ProvisioningParams params) {
        Intent encryptIntent = new Intent(this, EncryptDeviceActivity.class);

        Bundle resumeExtras = new Bundle();
        resumeExtras.putString(EncryptDeviceActivity.EXTRA_RESUME_TARGET,
                EncryptDeviceActivity.TARGET_DEVICE_OWNER);
        messageParser.addProvisioningParamsToBundle(resumeExtras, params);

        encryptIntent.putExtra(EncryptDeviceActivity.EXTRA_RESUME, resumeExtras);

        startActivityForResult(encryptIntent, ENCRYPT_DEVICE_REQUEST_CODE);
    }

    private void requestWifiPick() {
        startActivityForResult(AddWifiNetworkTask.getWifiPickIntent(), WIFI_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENCRYPT_DEVICE_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                ProvisionLogger.loge("User canceled device encryption.");
                setResult(RESULT_CANCELED);
                finish();
            }
        } else if (requestCode == WIFI_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                ProvisionLogger.loge("User canceled wifi picking.");
                setResult(RESULT_CANCELED);
                finish();
            } else if (resultCode == RESULT_OK) {
                if (DEBUG) ProvisionLogger.logd("Wifi request result is OK");
                if (NetworkMonitor.isConnectedToWifi(this)) {
                    askForConsentOrStartProvisioning();
                } else {
                    requestWifiPick();
                }
            }
        } else if (requestCode == PROVISIONING_REQUEST_CODE) {
            setResult(resultCode);
            finish();
        }
    }

    private void setMdmInfo() {
        ImageView imageView = (ImageView) findViewById(R.id.mdm_icon_view);
        TextView deviceManagerName = (TextView) findViewById(R.id.device_manager_name);
        String packageName = mParams.inferDeviceAdminPackageName();
        MdmPackageInfo packageInfo = Utils.getMdmPackageInfo(getPackageManager(), packageName);
        if (packageInfo != null) {
            String appLabel = packageInfo.getAppLabel();
            imageView.setImageDrawable(packageInfo.getPackageIcon());
            imageView.setContentDescription(
                    getResources().getString(R.string.mdm_icon_label, appLabel));
            deviceManagerName.setText(appLabel);
        } else {
            // Should never happen. Package will be on the device by now.
            deviceManagerName.setText(packageName);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_USER_CONSENTED, mUserConsented);
    }

    private void showStartProvisioningButton() {
        mNextButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDialogConsent() {
        // For accessibility purposes: we need to talk back only the title of the
        // next screen after user clicks ok.
        setTitle("");
        mUserConsented = true;
        startDeviceOwnerProvisioning();
    }

    @Override
    public void onDialogCancel() {
        // For Nfc provisioning, we automatically show the user consent dialog if applicable.
        // If the user then decides to cancel, we should finish the entire activity and exit.
        // For other cases, dismissing the consent dialog will lead back to
        // DeviceOwnerPreProvisioningActivity, and we do nothing here.
        if (mParams.startedByNfc) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onNavigateNext() {
        showUserConsentDialog();
    }

    /**
     * Notify the user that the admin will have full control over the device,
     * then start provisioning.
     */
    private void showUserConsentDialog() {
        UserConsentDialog.newInstance(UserConsentDialog.DEVICE_OWNER)
                .show(getFragmentManager(), "UserConsentDialogFragment");
    }
}
