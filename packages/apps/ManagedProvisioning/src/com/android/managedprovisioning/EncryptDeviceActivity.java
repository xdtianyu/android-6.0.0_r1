/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.managedprovisioning;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;

/**
 * Activity to ask for permission to activate full-filesystem encryption.
 *
 * Pressing 'settings' will launch settings to prompt the user to encrypt
 * the device.
 */
public class EncryptDeviceActivity extends SetupLayoutActivity {
    protected static final String EXTRA_RESUME = "com.android.managedprovisioning.RESUME";
    protected static final String EXTRA_RESUME_TARGET =
            "com.android.managedprovisioning.RESUME_TARGET";
    protected static final String TARGET_PROFILE_OWNER = "profile_owner";
    protected static final String TARGET_DEVICE_OWNER = "device_owner";

    private Bundle mResumeInfo;
    private String mResumeTarget;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResumeInfo = getIntent().getBundleExtra(EXTRA_RESUME);
        mResumeTarget = mResumeInfo.getString(EXTRA_RESUME_TARGET);

        if (TARGET_PROFILE_OWNER.equals(mResumeTarget)) {
            initializeLayoutParams(R.layout.encrypt_device, R.string.setup_work_profile, false);
            setTitle(R.string.setup_profile_encryption);
            ((TextView) findViewById(R.id.encrypt_main_text)).setText(
                    R.string.encrypt_device_text_for_profile_owner_setup);
        } else if (TARGET_DEVICE_OWNER.equals(mResumeTarget)) {
            initializeLayoutParams(R.layout.encrypt_device, R.string.setup_work_device, false);
            setTitle(R.string.setup_device_encryption);
            ((TextView) findViewById(R.id.encrypt_main_text)).setText(
                    R.string.encrypt_device_text_for_device_owner_setup);
        }
        configureNavigationButtons(R.string.encrypt_device_launch_settings,
            View.VISIBLE, View.VISIBLE);
    }

    public static boolean isDeviceEncrypted() {
        IMountService mountService = IMountService.Stub.asInterface(
                ServiceManager.getService("mount"));

        try {
            return (mountService.getEncryptionState() == IMountService.ENCRYPTION_STATE_OK);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public void onNavigateNext() {
        BootReminder.setProvisioningReminder(EncryptDeviceActivity.this, mResumeInfo);
        // Use settings so user confirms password/pattern and its passed
        // to encryption tool.
        Intent intent = new Intent();
        intent.setAction(DevicePolicyManager.ACTION_START_ENCRYPTION);
        startActivity(intent);
    }
}