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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives an 'install cert' broadcast and starts a service to install the ca cert. Runs on
 * secondary users on EDU devices.
 */
public class InstallCertReceiver extends BroadcastReceiver {
    public static final String INSTALL_CERT_ACTION =
            "com.android.managedprovisioning.INSTALL_CERT_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        ProvisionLogger.logi("Got install CA cert broadcast");
        if (!Utils.isCurrentUserOwner() && INSTALL_CERT_ACTION.equals(intent.getAction())) {
            CertService.startService(context, intent);
        }
    }
}
