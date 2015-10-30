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

package com.android.exchange.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.util.LongSparseArray;
import android.text.format.DateUtils;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.Eas;
import com.android.exchange.eas.EasPing;
import com.android.mail.utils.LogUtils;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bookkeeping for handling synchronization between pings and other sync related operations.
 * "Ping" refers to a hanging POST or GET that is used to receive push notifications. Ping is
 * the term for the Exchange command, but this code should be generic enough to be extended to IMAP.
 *
 * Basic rules of how these interact (note that all rules are per account):
 * - Only one operation (ping or other active sync operation) may run at a time.
 * - For shorthand, this class uses "sync" to mean "non-ping operation"; most such operations are
 *   sync ops, but some may not be (e.g. EAS Settings).
 * - Syncs can come from many sources concurrently; this class must serialize them.
 *
 * WHEN A SYNC STARTS:
 * - If nothing is running, proceed.
 * - If something is already running: wait until it's done.
 * - If the running thing is a ping task: interrupt it.
 *
 * WHEN A SYNC ENDS:
 * - If there are waiting syncs: signal one to proceed.
 * - If there are no waiting syncs and this account is configured for push: start a ping.
 * - Otherwise: This account is now idle.
 *
 * WHEN A PING TASK ENDS:
 * - A ping task loops until either it's interrupted by a sync (in which case, there will be one or
 *   more waiting syncs when the ping terminates), or encounters an error.
 * - If there are waiting syncs, and we were interrupted: signal one to proceed.
 * - If there are waiting syncs, but the ping terminated with an error: TODO: How to handle?
 * - If there are no waiting syncs and this account is configured for push: This means the ping task
 *   was terminated due to an error. Handle this by sending a sync request through the SyncManager
 *   that doesn't actually do any syncing, and whose only effect is to restart the ping.
 * - Otherwise: This account is now idle.
 *
 * WHEN AN ACCOUNT WANTS TO START OR CHANGE ITS PUSH BEHAVIOR:
 * - If nothing is running, start a new ping task.
 * - If a ping task is currently running, restart it with the new settings.
 * - If a sync is currently running, do nothing.
 *
 * WHEN AN ACCOUNT WANTS TO STOP GETTING PUSH:
 * - If nothing is running, do nothing.
 * - If a ping task is currently running, interrupt it.
 */
public class PingSyncSynchronizer {

    private static final String TAG = Eas.LOG_TAG;

    private static final long SYNC_ERROR_BACKOFF_MILLIS =  DateUtils.MINUTE_IN_MILLIS;

    // Enable this to make pings get automatically renewed every hour. This
    // should not be needed, but if there is a software error that results in
    // the ping being lost, this is a fallback to make sure that messages are
    // not delayed more than an hour.
    private static final boolean SCHEDULE_KICK = true;
    private static final long KICK_SYNC_INTERVAL_SECONDS =
            DateUtils.HOUR_IN_MILLIS / DateUtils.SECOND_IN_MILLIS;

    /**
     * This class handles bookkeeping for a single account.
     */
    private class AccountSyncState {
        /** The currently running {@link PingTask}, or null if we aren't in the middle of a Ping. */
        private PingTask mPingTask;

        // Values for mPushEnabled.
        public static final int PUSH_UNKNOWN = 0;
        public static final int PUSH_ENABLED = 1;
        public static final int PUSH_DISABLED = 2;

        /**
         * Tracks whether this account wants to get push notifications, based on calls to
         * {@link #pushModify} and {@link #pushStop} (i.e. it tracks the last requested push state).
         */
        private int mPushEnabled;

        /**
         * The number of syncs that are blocked waiting for the current operation to complete.
         * Unlike Pings, sync operations do not start their own tasks and are assumed to run in
         * whatever thread calls into this class.
         */
        private int mSyncCount;

        /** The condition on which to block syncs that need to wait. */
        private Condition mCondition;

        /** The accountId for this accountState, used for logging */
        private long mAccountId;

        public AccountSyncState(final Lock lock, final long accountId) {
            mPingTask = null;
            // We don't yet have enough information to know whether or not push should be enabled.
            // We need to look up the account and it's folders, which won't yet exist for a newly
            // created account.
            mPushEnabled = PUSH_UNKNOWN;
            mSyncCount = 0;
            mCondition = lock.newCondition();
            mAccountId = accountId;
        }

        /**
         * Update bookkeeping for a new sync:
         * - Stop the Ping if there is one.
         * - Wait until there's nothing running for this account before proceeding.
         */
        public void syncStart() {
            ++mSyncCount;
            if (mPingTask != null) {
                // Syncs are higher priority than Ping -- terminate the Ping.
                LogUtils.i(TAG, "PSS Sync is pre-empting a ping acct:%d", mAccountId);
                mPingTask.stop();
            }
            if (mPingTask != null || mSyncCount > 1) {
                // There’s something we need to wait for before we can proceed.
                try {
                    LogUtils.i(TAG, "PSS Sync needs to wait: Ping: %s, Pending tasks: %d acct: %d",
                            mPingTask != null ? "yes" : "no", mSyncCount, mAccountId);
                    mCondition.await();
                } catch (final InterruptedException e) {
                    // TODO: Handle this properly. Not catching it might be the right answer.
                    LogUtils.i(TAG, "PSS InterruptedException acct:%d", mAccountId);
                }
            }
        }

        /**
         * Update bookkeeping when a sync completes. This includes signaling pending ops to
         * go ahead, or starting the ping if appropriate and there are no waiting ops.
         * @return Whether this account is now idle.
         */
        public boolean syncEnd(final boolean lastSyncHadError, final Account account,
                               final PingSyncSynchronizer synchronizer) {
            --mSyncCount;
            if (mSyncCount > 0) {
                LogUtils.i(TAG, "PSS Signalling a pending sync to proceed acct:%d.",
                        account.getId());
                mCondition.signal();
                return false;
            } else {
                if (mPushEnabled == PUSH_UNKNOWN) {
                    LogUtils.i(TAG, "PSS push enabled is unknown");
                    mPushEnabled = (EasService.pingNeededForAccount(mService, account) ?
                            PUSH_ENABLED : PUSH_DISABLED);
                }
                if (mPushEnabled == PUSH_ENABLED) {
                    if (lastSyncHadError) {
                        LogUtils.i(TAG, "PSS last sync had error, scheduling delayed ping acct:%d.",
                                account.getId());
                        scheduleDelayedPing(synchronizer.getContext(), account);
                        return true;
                    } else {
                        LogUtils.i(TAG, "PSS last sync succeeded, starting new ping acct:%d.",
                                account.getId());
                        final android.accounts.Account amAccount =
                                new android.accounts.Account(account.mEmailAddress,
                                        Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
                        mPingTask = new PingTask(synchronizer.getContext(), account, amAccount,
                                synchronizer);
                        mPingTask.start();
                        return false;
                    }
                }
            }
            LogUtils.i(TAG, "PSS no push enabled acct:%d.", account.getId());
            return true;
        }

        /**
         * Update bookkeeping when the ping task terminates, including signaling any waiting ops.
         * @return Whether this account is now idle.
         */
        private boolean pingEnd(final android.accounts.Account amAccount) {
            mPingTask = null;
            if (mSyncCount > 0) {
                LogUtils.i(TAG, "PSS pingEnd, syncs still in progress acct:%d.", mAccountId);
                mCondition.signal();
                return false;
            } else {
                if (mPushEnabled == PUSH_ENABLED || mPushEnabled == PUSH_UNKNOWN) {
                    if (mPushEnabled == PUSH_UNKNOWN) {
                        // This should not occur. If mPushEnabled is unknown, we should not
                        // have started a ping. Still, we'd rather err on the side of restarting
                        // the ping, so log an error and request a new ping. Eventually we should
                        // do a sync, and then we'll correctly initialize mPushEnabled in
                        // syncEnd().
                        LogUtils.e(TAG, "PSS pingEnd, with mPushEnabled UNKNOWN?");
                    }
                    LogUtils.i(TAG, "PSS pingEnd, starting new ping acct:%d.", mAccountId);
                    /**
                     * This situation only arises if we encountered some sort of error that
                     * stopped our ping but not due to a sync interruption. In this scenario
                     * we'll leverage the SyncManager to request a push only sync that will
                     * restart the ping when the time is right. */
                    EasPing.requestPing(amAccount);
                    return false;
                }
            }
            LogUtils.i(TAG, "PSS pingEnd, no longer need ping acct:%d.", mAccountId);
            return true;
        }

        private void scheduleDelayedPing(final Context context,
                                         final Account account) {
            LogUtils.i(TAG, "PSS Scheduling a delayed ping acct:%d.", account.getId());
            final Intent intent = new Intent(context, EasService.class);
            intent.setAction(Eas.EXCHANGE_SERVICE_INTENT_ACTION);
            intent.putExtra(EasService.EXTRA_START_PING, true);
            intent.putExtra(EasService.EXTRA_PING_ACCOUNT, account);

            final PendingIntent pi = PendingIntent.getService(context, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);
            final AlarmManager am = (AlarmManager)context.getSystemService(
                    Context.ALARM_SERVICE);
            final long atTime = SystemClock.elapsedRealtime() + SYNC_ERROR_BACKOFF_MILLIS;
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, atTime, pi);
        }

        /**
         * Modifies or starts a ping for this account if no syncs are running.
         */
        public void pushModify(final Account account, final PingSyncSynchronizer synchronizer) {
            LogUtils.i(LogUtils.TAG, "PSS pushModify acct:%d", account.getId());
            mPushEnabled = PUSH_ENABLED;
            final android.accounts.Account amAccount =
                    new android.accounts.Account(account.mEmailAddress,
                            Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE);
            if (mSyncCount == 0) {
                if (mPingTask == null) {
                    // No ping, no running syncs -- start a new ping.
                    LogUtils.i(LogUtils.TAG, "PSS starting ping task acct:%d", account.getId());
                    mPingTask = new PingTask(synchronizer.getContext(), account, amAccount,
                            synchronizer);
                    mPingTask.start();
                } else {
                    // Ping is already running, so tell it to restart to pick up any new params.
                    LogUtils.i(LogUtils.TAG, "PSS restarting ping task acct:%d", account.getId());
                    mPingTask.restart();
                }
            } else {
                LogUtils.i(LogUtils.TAG, "PSS syncs still in progress acct:%d", account.getId());
            }
            if (SCHEDULE_KICK) {
                final Bundle extras = new Bundle(1);
                extras.putBoolean(Mailbox.SYNC_EXTRA_PUSH_ONLY, true);
                ContentResolver.addPeriodicSync(amAccount, EmailContent.AUTHORITY, extras,
                        KICK_SYNC_INTERVAL_SECONDS);
            }
        }

        /**
         * Stop the currently running ping.
         */
        public void pushStop() {
            LogUtils.i(LogUtils.TAG, "PSS pushStop acct:%d", mAccountId);
            mPushEnabled = PUSH_DISABLED;
            if (mPingTask != null) {
                mPingTask.stop();
            }
        }
    }

    /**
     * Lock for access to {@link #mAccountStateMap}, also used to create the {@link Condition}s for
     * each Account.
     */
    private final ReentrantLock mLock;

    /**
     * Map from account ID -> {@link AccountSyncState} for accounts with a running operation.
     * An account is in this map only when this account is active, i.e. has a ping or sync running
     * or pending. If an account is not in the middle of a sync and is not configured for push,
     * it will not be here. This allows to use emptiness of this map to know whether the service
     * needs to be running, and is also handy when debugging.
     */
    private final LongSparseArray<AccountSyncState> mAccountStateMap;

    /** The {@link Service} that this object is managing. */
    private final Service mService;

    public PingSyncSynchronizer(final Service service) {
        mLock = new ReentrantLock();
        mAccountStateMap = new LongSparseArray<AccountSyncState>();
        mService = service;
    }

    public Context getContext() {
        return mService;
    }

    /**
     * Gets the {@link AccountSyncState} for an account.
     * The caller must hold {@link #mLock}.
     * @param accountId The id for the account we're interested in.
     * @param createIfNeeded If true, create the account state if it's not already there.
     * @return The {@link AccountSyncState} for that account, or null if the account is idle and
     *         createIfNeeded is false.
     */
    private AccountSyncState getAccountState(final long accountId, final boolean createIfNeeded) {
        assert mLock.isHeldByCurrentThread();
        AccountSyncState state = mAccountStateMap.get(accountId);
        if (state == null && createIfNeeded) {
            LogUtils.i(TAG, "PSS adding account state for acct:%d", accountId);
            state = new AccountSyncState(mLock, accountId);
            mAccountStateMap.put(accountId, state);
            // TODO: Is this too late to startService?
            if (mAccountStateMap.size() == 1) {
                LogUtils.i(TAG, "PSS added first account, starting service");
                mService.startService(new Intent(mService, mService.getClass()));
            }
        }
        return state;
    }

    /**
     * Remove an account from the map. If this was the last account, then also stop this service.
     * The caller must hold {@link #mLock}.
     * @param accountId The id for the account we're removing.
     */
    private void removeAccount(final long accountId) {
        assert mLock.isHeldByCurrentThread();
        LogUtils.i(TAG, "PSS removing account state for acct:%d", accountId);
        mAccountStateMap.delete(accountId);
        if (mAccountStateMap.size() == 0) {
            LogUtils.i(TAG, "PSS removed last account; stopping service.");
            mService.stopSelf();
        }
    }

    public void syncStart(final long accountId) {
        mLock.lock();
        try {
            LogUtils.i(TAG, "PSS syncStart for account acct:%d", accountId);
            final AccountSyncState accountState = getAccountState(accountId, true);
            accountState.syncStart();
        } finally {
            mLock.unlock();
        }
    }

    public void syncEnd(final boolean lastSyncHadError, final Account account) {
        mLock.lock();
        try {
            final long accountId = account.getId();
            LogUtils.i(TAG, "PSS syncEnd for account acct:%d", account.getId());
            final AccountSyncState accountState = getAccountState(accountId, false);
            if (accountState == null) {
                LogUtils.w(TAG, "PSS syncEnd for account %d but no state found", accountId);
                return;
            }
            if (accountState.syncEnd(lastSyncHadError, account, this)) {
                removeAccount(accountId);
            }
        } finally {
            mLock.unlock();
        }
    }

    public void pingEnd(final long accountId, final android.accounts.Account amAccount) {
        mLock.lock();
        try {
            LogUtils.i(TAG, "PSS pingEnd for account %d", accountId);
            final AccountSyncState accountState = getAccountState(accountId, false);
            if (accountState == null) {
                LogUtils.w(TAG, "PSS pingEnd for account %d but no state found", accountId);
                return;
            }
            if (accountState.pingEnd(amAccount)) {
                removeAccount(accountId);
            }
        } finally {
            mLock.unlock();
        }
    }

    public void pushModify(final Account account) {
        mLock.lock();
        try {
            final long accountId = account.getId();
            LogUtils.i(TAG, "PSS pushModify acct:%d", accountId);
            final AccountSyncState accountState = getAccountState(accountId, true);
            accountState.pushModify(account, this);
        } finally {
            mLock.unlock();
        }
    }

    public void pushStop(final long accountId) {
        mLock.lock();
        try {
            LogUtils.i(TAG, "PSS pushStop acct:%d", accountId);
            final AccountSyncState accountState = getAccountState(accountId, false);
            if (accountState != null) {
                accountState.pushStop();
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Stops our service if our map contains no active accounts.
     */
    public void stopServiceIfIdle() {
        mLock.lock();
        try {
            LogUtils.i(TAG, "PSS stopIfIdle");
            if (mAccountStateMap.size() == 0) {
                LogUtils.i(TAG, "PSS has no active accounts; stopping service.");
                mService.stopSelf();
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Tells all running ping tasks to stop.
     */
    public void stopAllPings() {
        mLock.lock();
        try {
            for (int i = 0; i < mAccountStateMap.size(); ++i) {
                mAccountStateMap.valueAt(i).pushStop();
            }
        } finally {
            mLock.unlock();
        }
    }
}
