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
 * limitations under the License.
 */

package com.android.cts.verifier.managedprovisioning;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.IntentDrivenTestActivity;
import com.android.cts.verifier.IntentDrivenTestActivity.ButtonInfo;
import com.android.cts.verifier.IntentDrivenTestActivity.TestInfo;
import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter.TestListItem;

/**
 * Activity that lists all device owner negative tests.
 */
public class DeviceOwnerNegativeTestActivity extends PassFailButtons.TestListActivity {

    private static final String ACTION_PROVISION_MANAGED_DEVICE
        = "com.android.managedprovisioning.ACTION_PROVISION_MANAGED_DEVICE";
    private static final Intent PROVISION_DEVICE_INTENT =
            new Intent(ACTION_PROVISION_MANAGED_DEVICE);

    private static final String DEVICE_OWNER_NEGATIVE_TEST = "DEVICE_OWNER_PROVISIONING_NEGATIVE";
    private static final TestInfo DEVICE_OWNER_NEGATIVE_TEST_INFO = new TestInfo(
                    DEVICE_OWNER_NEGATIVE_TEST,
                    R.string.device_owner_negative_test,
                    R.string.device_owner_negative_test_info,
                    new ButtonInfo(
                            R.string.start_device_owner_provisioning_button,
                            PROVISION_DEVICE_INTENT));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_list);
        setInfoResources(R.string.device_owner_provisioning_tests,
                R.string.device_owner_provisioning_tests_info, 0);
        setPassFailButtonClickListeners();

        final ArrayTestListAdapter adapter = new ArrayTestListAdapter(this);
        adapter.add(TestListItem.newCategory(this, R.string.device_owner_provisioning_category));

        Intent startTestIntent = new Intent(this, IntentDrivenTestActivity.class)
                    .putExtra(IntentDrivenTestActivity.EXTRA_ID,
                            DEVICE_OWNER_NEGATIVE_TEST_INFO.getTestId())
                    .putExtra(IntentDrivenTestActivity.EXTRA_TITLE,
                            DEVICE_OWNER_NEGATIVE_TEST_INFO.getTitle())
                    .putExtra(IntentDrivenTestActivity.EXTRA_INFO,
                            DEVICE_OWNER_NEGATIVE_TEST_INFO.getInfoText())
                    .putExtra(IntentDrivenTestActivity.EXTRA_BUTTONS,
                            DEVICE_OWNER_NEGATIVE_TEST_INFO.getButtons());


        adapter.add(TestListItem.newTest(this, DEVICE_OWNER_NEGATIVE_TEST_INFO.getTitle(),
                DEVICE_OWNER_NEGATIVE_TEST_INFO.getTestId(), startTestIntent, null));

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updatePassButton();
            }
        });

        setTestListAdapter(adapter);
    }

    /**
     * Enable Pass Button when the all tests passed.
     */
    private void updatePassButton() {
        getPassButton().setEnabled(mAdapter.allTestsPassed());
    }
}

