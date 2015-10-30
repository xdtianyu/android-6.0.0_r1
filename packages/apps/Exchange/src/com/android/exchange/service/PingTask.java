/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.exchange.service;

import android.content.Context;
import android.os.AsyncTask;

import com.android.emailcommon.provider.Account;
import com.android.exchange.Eas;
import com.android.exchange.adapter.PingParser;
import com.android.exchange.eas.EasOperation;
import com.android.exchange.eas.EasPing;
import com.android.mail.utils.LogUtils;

/**
 * Thread management class for Ping operations.
 */
public class PingTask extends AsyncTask<Void, Void, Void> {
    private final EasPing mOperation;
    private final PingSyncSynchronizer mPingSyncSynchronizer;

    private static final String TAG = Eas.LOG_TAG;


    public PingTask(final Context context, final Account account,
            final android.accounts.Account amAccount,
            final PingSyncSynchronizer pingSyncSynchronizer) {
        assert pingSyncSynchronizer != null;
        mOperation = new EasPing(context, account, amAccount);
        mPingSyncSynchronizer = pingSyncSynchronizer;
    }

    /** Start the ping loop. */
    public void start() {
        executeOnExecutor(THREAD_POOL_EXECUTOR);
    }

    /** Abort the ping loop (used when another operation interrupts the ping). */
    public void stop() {
        mOperation.abort();
    }

    /** Restart the ping loop (used when a ping request happens during a ping). */
    public void restart() {
        mOperation.restart();
    }

    @Override
    protected Void doInBackground(Void... params) {
        LogUtils.i(TAG, "Ping task starting for %d", mOperation.getAccountId());
        int pingStatus;
        try {
            do {
                pingStatus = mOperation.doPing();
            } while (PingParser.shouldPingAgain(pingStatus));
        } catch (final Exception e) {
            // TODO: This is hacky, try to be cleaner.
            // If we get any sort of exception here, treat it like the ping returned a connection
            // failure.
            LogUtils.e(TAG, e, "Ping exception for account %d", mOperation.getAccountId());
            pingStatus = EasOperation.RESULT_NETWORK_PROBLEM;
        }
        LogUtils.i(TAG, "Ping task ending with status: %d", pingStatus);

        mPingSyncSynchronizer.pingEnd(mOperation.getAccountId(), mOperation.getAmAccount());
        return null;
    }

    @Override
    protected void onCancelled (Void result) {
        // TODO: This is also hacky, should have a separate result code at minimum.
        // If the ping is cancelled, make sure it reports something to the sync adapter.
        LogUtils.w(TAG, "Ping cancelled for %d", mOperation.getAccountId());
        mPingSyncSynchronizer.pingEnd(mOperation.getAccountId(), mOperation.getAmAccount());
    }
}
