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

package com.android.tv.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.SyncStatusObserver;
import android.content.pm.ProviderInfo;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Displays the sync settings for a given account.
 */
public class AccountSyncSettings extends DialogActivity implements OnAccountsUpdateListener {

    private static final String TAG = "AccountSyncSettings";

    private static final String KEY_SYNC_NOW = "KEY_SYNC_NOW";
    private static final String KEY_CANCEL_SYNC = "KEY_SYNC_CANCEL";

    private static final String EXTRA_ONE_TIME_SYNC = "one_time_sync";
    private static final String EXTRA_ACCOUNT = "account";

    private AccountManager mAccountManager;
    private Account mAccount;
    private AuthenticatorHelper mHelper;

    /**
     * Adapters which are invisible. Store them so that sync now syncs everything.
     */
    private List<SyncAdapterType> mInvisibleAdapters;

    private Account[] mAccounts;

    private boolean mSyncIsFailing;

    private Object mStatusChangeListenerHandle;

    private ActionFragment mActionFragment;

    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {

        @Override
        public void onStatusChanged(int which) {
            onSyncStateUpdated();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new AuthenticatorHelper();
        mAccountManager = AccountManager.get(this);
        mInvisibleAdapters = new ArrayList<SyncAdapterType>();
        String accountName = getIntent().getStringExtra(AccountSettingsActivity.EXTRA_ACCOUNT);
        if (!TextUtils.isEmpty(accountName)) {
            // Search for the account.
            for (Account account : mAccountManager.getAccounts()) {
                if (account.name.equals(accountName)) {
                    mAccount = account;
                    break;
                }
            }
        }
        if (mAccount == null) {
            finish();
            return;
        }
        mActionFragment = ActionFragment.newInstance(getActions(mAccountManager.getAccounts()));
        // Start with an empty list and then fill in with the sync adapters.
        setContentAndActionFragments(ContentFragment.newInstance(
                accountName, mAccount.type, "", R.drawable.ic_settings_sync,
                getResources().getColor(R.color.icon_background)), mActionFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccountManager.get(this).addOnAccountsUpdatedListener(this, null, false);
        updateAuthDescriptions();
        onAccountsUpdated(AccountManager.get(this).getAccounts());
        mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
                | ContentResolver.SYNC_OBSERVER_TYPE_STATUS
                | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncStatusObserver);
    }

    @Override
    public void onPause() {
        ContentResolver.removeStatusChangeListener(mStatusChangeListenerHandle);
        AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        super.onPause();
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        mAccounts = accounts;
        loadSyncActions(accounts);
    }

    @Override
    public void onActionClicked(Action action) {
        String key = action.getKey();
        if (KEY_SYNC_NOW.equals(key)) {
            startSyncForEnabledProviders();
        } else if (KEY_CANCEL_SYNC.equals(key)) {
            cancelSyncForEnabledProviders();
        } else {
            // This is a specific sync adapter.
            Account account = action.getIntent().getParcelableExtra(EXTRA_ACCOUNT);
            String authority = action.getKey();
            boolean syncAutomatically = ContentResolver.getSyncAutomatically(account, authority);
            if (action.getIntent().getBooleanExtra(EXTRA_ONE_TIME_SYNC, false)) {
                requestOrCancelSync(account, authority, true);
            } else {
                boolean syncOn = !action.isChecked(); // toggle
                boolean oldSyncState = syncAutomatically;
                if (syncOn != oldSyncState) {
                    // if we're enabling sync, this will request a sync as well
                    ContentResolver.setSyncAutomatically(account, authority, syncOn);
                    // if the master sync switch is off, the request above will
                    // get dropped.  when the user clicks on this toggle,
                    // we want to force the sync, however.
                    if (!ContentResolver.getMasterSyncAutomatically() || !syncOn) {
                        requestOrCancelSync(account, authority, syncOn);
                    }
                }
                if (mAccounts != null) {
                    loadSyncActions(mAccounts);
                }
            }
        }
    }

    private void onSyncStateUpdated() {
        if (!isResumed() || mAccounts == null) {
            return;
        }
        loadSyncActions(mAccounts);
    }

    private void updateAuthDescriptions() {
        mHelper.updateAuthDescriptions(this);
        onAuthDescriptionsUpdated();
    }

    private void onAuthDescriptionsUpdated() {
        ((ContentFragment) getContentFragment()).setBreadCrumbText(
                mHelper.getLabelForType(this, mAccount.type).toString());
    }


    private void loadSyncActions(Account[] accounts) {
        new LoadActionsTask().execute(accounts);
    }

    private ArrayList<Action> getActions(Account[] accounts) {
        ArrayList<Action> actions = new ArrayList<Action>();

        Date date = new Date();
        List<SyncInfo> currentSyncs = ContentResolver.getCurrentSyncs();
        mSyncIsFailing = false;

        mInvisibleAdapters.clear();

        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypes();
        HashMap<String, ArrayList<String>> accountTypeToAuthorities =
                new HashMap<String, ArrayList<String>>();
        for (int i = 0, n = syncAdapters.length; i < n; i++) {
            final SyncAdapterType sa = syncAdapters[i];
            if (sa.isUserVisible()) {
                ArrayList<String> authorities = accountTypeToAuthorities.get(sa.accountType);
                if (authorities == null) {
                    authorities = new ArrayList<String>();
                    accountTypeToAuthorities.put(sa.accountType, authorities);
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.d(TAG, "onAccountUpdated: added authority " + sa.authority
                            + " to accountType " + sa.accountType);
                }
                authorities.add(sa.authority);
            } else {
                // keep track of invisible sync adapters, so sync now forces
                // them to sync as well.
                mInvisibleAdapters.add(sa);
            }
        }

        for (int i = 0, n = accounts.length; i < n; i++) {
            final Account account = accounts[i];
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "looking for sync adapters that match account " + account);
            }
            final ArrayList<String> authorities = accountTypeToAuthorities.get(account.type);
            if (authorities != null && (mAccount == null || mAccount.equals(account))) {
                for (int j = 0, m = authorities.size(); j < m; j++) {
                    final String authority = authorities.get(j);
                    // We could check services here....
                    int syncState = ContentResolver.getIsSyncable(account, authority);
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.d(TAG, "  found authority " + authority + " " + syncState);
                    }
                    if (syncState > 0) {
                        Action action = getAction(account, authority, currentSyncs);
                        if (action != null) {
                            actions.add(action);
                        }
                    }
                }
            }
        }

        Collections.sort(actions, ADAPTER_COMPARATOR);
        // Always add a "Sync now | cancel sync" action at the beginning.
        boolean syncActive = false;
        List<SyncInfo> syncList = ContentResolver.getCurrentSyncs();
        for (SyncInfo info : syncList) {
            if (mAccount.equals(info.account)) {
                syncActive = true;
                break;
            }
        }

        actions.add(0, new Action.Builder()
                    .key(!syncActive ? KEY_SYNC_NOW : KEY_CANCEL_SYNC)
                    .title(getString(!syncActive ? R.string.sync_now : R.string.sync_cancel))
                    .build());
        return actions;
    }

    /**
     * Gets an action item with the appropriate description / checkmark / drawable.
     * <p>
     * Returns null if the provider can't be shown for some reason.
     */
    private Action getAction(Account account, String authority, List<SyncInfo> currentSyncs) {
        final ProviderInfo providerInfo = getPackageManager().resolveContentProvider(authority, 0);
        if (providerInfo == null) {
            return null;
        }
        CharSequence providerLabel = providerInfo.loadLabel(getPackageManager());
        if (TextUtils.isEmpty(providerLabel)) {
            return null;
        }
        String description;
        boolean isSyncing;
        Date date = new Date();
        SyncStatusInfo status = ContentResolver.getSyncStatus(account, authority);
        boolean syncEnabled = ContentResolver.getSyncAutomatically(account, authority);
        boolean authorityIsPending = status == null ? false : status.pending;
        boolean initialSync = status == null ? false : status.initialize;

        boolean activelySyncing = isSyncing(currentSyncs, account, authority);
        boolean lastSyncFailed = status != null
                && status.lastFailureTime != 0
                && status.getLastFailureMesgAsInt(0)
                   != ContentResolver.SYNC_ERROR_SYNC_ALREADY_IN_PROGRESS;
        if (!syncEnabled) lastSyncFailed = false;
        if (lastSyncFailed && !activelySyncing && !authorityIsPending) {
            mSyncIsFailing = true;
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.d(TAG, "Update sync status: " + account + " " + authority +
                    " active = " + activelySyncing + " pend =" +  authorityIsPending);
        }

        final long successEndTime = (status == null) ? 0 : status.lastSuccessTime;
        if (!syncEnabled) {
            description = getString(R.string.sync_disabled);
        } else if (activelySyncing) {
            description = getString(R.string.sync_in_progress);
        } else if (successEndTime != 0) {
            date.setTime(successEndTime);
            final String timeString = formatSyncDate(date);
            description = getString(R.string.last_synced, timeString);
        } else {
            description = "";
        }
        int syncState = ContentResolver.getIsSyncable(account, authority);

        boolean pending = authorityIsPending && (syncState >= 0) && !initialSync;
        boolean active = activelySyncing && (syncState >= 0) && !initialSync;
        boolean activeVisible = pending || active;
        // TODO: set drawable based on these flags.

        ConnectivityManager connManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final boolean masterSyncAutomatically = ContentResolver.getMasterSyncAutomatically();
        final boolean backgroundDataEnabled = connManager.getBackgroundDataSetting();
        final boolean oneTimeSyncMode = !masterSyncAutomatically || !backgroundDataEnabled;
        boolean checked = oneTimeSyncMode || syncEnabled;

        // Store extras in the intent
        Intent intent = new Intent()
                .putExtra(EXTRA_ONE_TIME_SYNC, oneTimeSyncMode)
                .putExtra(EXTRA_ACCOUNT, account);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.d(TAG, "Creating action " + providerLabel.toString() + " " + description);
        }
        return new Action.Builder()
                .key(authority)
                .title(providerLabel.toString())
                .description(description)
                .checked(checked)
                .intent(intent)
                .build();
    }

    private void startSyncForEnabledProviders() {
        requestOrCancelSyncForEnabledProviders(true /* start them */);
    }

    private void cancelSyncForEnabledProviders() {
        requestOrCancelSyncForEnabledProviders(false /* cancel them */);
    }

    private void requestOrCancelSyncForEnabledProviders(boolean startSync) {
        // sync everything that the user has enabled
        int count = mActionFragment.getAdapter().getCount();
        for (int i = 0; i < count; i++) {
            Action action = (Action) mActionFragment.getAdapter().getItem(i);
            if (action.getIntent() == null) {
                continue;
            }
            if (!action.isChecked()) {
                continue;
            }
            Account account = action.getIntent().getParcelableExtra(EXTRA_ACCOUNT);
            requestOrCancelSync(account, action.getKey(), startSync);
        }
        // plus whatever the system needs to sync, e.g., invisible sync adapters
        if (mAccount != null) {
            // Make a copy of these in case we update while calling this.
            List<SyncAdapterType> invisibleAdapters = new ArrayList<SyncAdapterType>(
                    mInvisibleAdapters);
            int size = invisibleAdapters.size();
            for (int index = 0; index < size; ++index) {
                SyncAdapterType syncAdapter = invisibleAdapters.get(index);
                // invisible sync adapters' account type should be same as current account type
                if (syncAdapter.accountType.equals(mAccount.type)) {
                    requestOrCancelSync(mAccount, syncAdapter.authority, startSync);
                }
            }
        }
    }

    private void requestOrCancelSync(Account account, String authority, boolean sync) {
        if (sync) {
            Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(account, authority, extras);
        } else {
            ContentResolver.cancelSync(account, authority);
        }
    }

    private boolean isSyncing(List<SyncInfo> currentSyncs, Account account, String authority) {
        for (SyncInfo syncInfo : currentSyncs) {
            if (syncInfo.account.equals(account) && syncInfo.authority.equals(authority)) {
                return true;
            }
        }
        return false;
    }

    protected String formatSyncDate(Date date) {
        return DateUtils.formatDateTime(this, date.getTime(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
    }

    private class LoadActionsTask extends AsyncTask<Account[], Void, ArrayList<Action>> {

        @Override
        protected ArrayList<Action> doInBackground(Account[]... params) {
            return getActions(params[0]);
        }

        @Override
        protected void onPostExecute(ArrayList<Action> result) {
            // Set the icon based on whether sync is failing.
            ContentFragment contentFragment = ((ContentFragment) getContentFragment());
            if (contentFragment != null) {
                contentFragment.setIcon(mSyncIsFailing ? R.drawable.ic_settings_sync_error :
                        R.drawable.ic_settings_sync);
                contentFragment.setDescriptionText(mSyncIsFailing ?
                        getString(R.string.sync_is_failing) : "");
            }
            ((ActionAdapter) mActionFragment.getAdapter()).setActions(result);
        }
    }

    private static final Comparator<Action> ADAPTER_COMPARATOR = new Comparator<Action>() {

        @Override
        public int compare(Action lhs, Action rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            }
            if (lhs != null && rhs == null) {
                return 1;
            }
            if (rhs != null && lhs == null) {
                return -1;
            }
            return lhs.getTitle().compareTo(rhs.getTitle());
        }
    };
}
