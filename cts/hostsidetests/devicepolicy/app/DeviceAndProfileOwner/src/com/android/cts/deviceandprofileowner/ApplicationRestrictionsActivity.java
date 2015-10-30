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
package com.android.cts.deviceandprofileowner;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;

/**
 * Test activity for setApplicationRestrictions().
 *
 * The actual test will set restrictions for this package, and the purpose of this
 * activity is to listen for the ACTION_APPLICATION_RESTRICTIONS_CHANGED broadcast
 * and relay the retrieved restriction bundle back to the test for validation.
 */
public class ApplicationRestrictionsActivity extends Activity {

    // Incoming intent type
    public static final String FINISH = "finishActivity";

    // Outgoing broadcast
    public static final String REGISTERED_ACTION =
            "com.android.cts.deviceandprofileowner.APP_RESTRICTION_REGISTERED";
    public static final String RESTRICTION_ACTION =
            "com.android.cts.deviceandprofileowner.APP_RESTRICTION_VALUE";

    private UserManager mUserManager;

    private final BroadcastReceiver mAppRestrictionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            broadcastRestriction();
        }
    };

    private void broadcastRestriction() {
        Bundle restrictions = mUserManager.getApplicationRestrictions(getPackageName());
        Intent intent = new Intent(RESTRICTION_ACTION);
        intent.putExtra("value", restrictions);
        sendBroadcast(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);
        IntentFilter filter = new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED);
        registerReceiver(mAppRestrictionReceiver, filter);
        sendBroadcast(new Intent(REGISTERED_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mAppRestrictionReceiver);
    }

    private void handleIntent(Intent intent) {
        if (intent.getBooleanExtra(FINISH, false)) {
            finish();
        }
    }

}
