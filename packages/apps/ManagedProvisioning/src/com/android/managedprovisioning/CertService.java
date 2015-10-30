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

import android.app.admin.DevicePolicyManager;
import android.app.Notification;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.UserHandle;

/**
 * Service for handling CA certs.
 *
 */
public class CertService extends IntentService {
    public static final String EXTRA_CA_CERT = "cacert";
    public static final String EXTRA_REQUESTING_USER = "reqUser";
    private static final String TAG = "CertService";

    private static final int FOREGROUND_ID = 0xedc82;

    private DevicePolicyManager mDevicePolicyManager;

    public CertService() {
        super(TAG);
    }

    public static void startService(Context context, Intent callingIntent) {
        ProvisionLogger.logd("InstallCertService.startService");
        Intent intent = new Intent(context, CertService.class);
        intent.setAction(callingIntent.getAction());
        intent.putExtras(callingIntent);
        context.startService(intent);
    }

    /**
     * Set this as a foreground service. This is done to help ensure that the
     * service isn't killed by the OS.
     */
    private void startServiceInForeground() {
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.provisioning))
                .setContentText(getString(R.string.copying_certs))
                .setSmallIcon(R.drawable.quantum_ic_https_white_24)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(FOREGROUND_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onHandleIntent(final Intent intent) {
        ProvisionLogger.logd("InstallCertService.onHandleIntent");

        startServiceInForeground();

        mDevicePolicyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (InstallCertReceiver.INSTALL_CERT_ACTION.equals(intent.getAction())) {
            installCert(intent.getByteArrayExtra(EXTRA_CA_CERT));
        } else if (InstallCertRequestReceiver.REQUEST_CERT_ACTION.equals(
                intent.getAction())) {
            sendCerts((UserHandle) intent.getParcelableExtra(EXTRA_REQUESTING_USER));
        }

        stopForeground(true);
    }

    /**
     * Installs the given ca cert.
     */
    private void installCert(final byte[] certToInstall) {
        ProvisionLogger.logi("Installing CA cert");

        mDevicePolicyManager.installCaCert(null, certToInstall);
    }

    /**
     * Sends all user installed ca certs for this user to the given user.
     */
    private void sendCerts(final UserHandle handle) {
        ProvisionLogger.logi("Sending CA certs");

        for (byte[] cert : mDevicePolicyManager.getInstalledCaCerts(null)) {
            Intent intent = new Intent(InstallCertReceiver.INSTALL_CERT_ACTION);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtra(EXTRA_CA_CERT, cert);
            sendBroadcastAsUser(intent, handle);
        }

    }
}