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
 * limitations under the License
 */
package com.android.phone.vvm.omtp.sync;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.provider.VoicemailContract;
import android.telecom.PhoneAccountHandle;
import android.telecom.Voicemail;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.phone.PhoneUtils;
import com.android.phone.settings.VisualVoicemailSettingsUtil;
import com.android.phone.vvm.omtp.LocalLogHelper;
import com.android.phone.vvm.omtp.OmtpVvmCarrierConfigHelper;
import com.android.phone.vvm.omtp.imap.ImapHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sync OMTP visual voicemail.
 */
public class OmtpVvmSyncService extends IntentService {
    private static final String TAG = OmtpVvmSyncService.class.getSimpleName();

    /** Signifies a sync with both uploading to the server and downloading from the server. */
    public static final String SYNC_FULL_SYNC = "full_sync";
    /** Only upload to the server. */
    public static final String SYNC_UPLOAD_ONLY = "upload_only";
    /** Only download from the server. */
    public static final String SYNC_DOWNLOAD_ONLY = "download_only";
    /** The account to sync. */
    public static final String EXTRA_PHONE_ACCOUNT = "phone_account";

    // Timeout used to call ConnectivityManager.requestNetwork
    private static final int NETWORK_REQUEST_TIMEOUT_MILLIS = 60 * 1000;

    // Minimum time allowed between full syncs
    private static final int MINIMUM_FULL_SYNC_INTERVAL_MILLIS = 60 * 1000;

    // Number of retries
    private static final int NETWORK_RETRY_COUNT = 6;

    private VoicemailsQueryHelper mQueryHelper;
    private ConnectivityManager mConnectivityManager;

    public OmtpVvmSyncService() {
        super("OmtpVvmSyncService");
    }

    public static Intent getSyncIntent(Context context, String action,
            PhoneAccountHandle phoneAccount, boolean firstAttempt) {
        if (firstAttempt) {
            if (phoneAccount != null) {
                VisualVoicemailSettingsUtil.resetVisualVoicemailRetryInterval(context,
                        phoneAccount);
            } else {
                OmtpVvmSourceManager vvmSourceManager =
                        OmtpVvmSourceManager.getInstance(context);
                Set<PhoneAccountHandle> sources = vvmSourceManager.getOmtpVvmSources();
                for (PhoneAccountHandle source : sources) {
                    VisualVoicemailSettingsUtil.resetVisualVoicemailRetryInterval(context, source);
                }
            }
        }

        Intent serviceIntent = new Intent(context, OmtpVvmSyncService.class);
        serviceIntent.setAction(action);
        if (phoneAccount != null) {
            serviceIntent.putExtra(EXTRA_PHONE_ACCOUNT, phoneAccount);
        }

        cancelRetriesForIntent(context, serviceIntent);
        return serviceIntent;
    }

    /**
     * Cancel all retry syncs for an account.
     * @param context The context the service runs in.
     * @param phoneAccount The phone account for which to cancel syncs.
     */
    public static void cancelAllRetries(Context context, PhoneAccountHandle phoneAccount) {
        cancelRetriesForIntent(context, getSyncIntent(context, SYNC_FULL_SYNC, phoneAccount,
                false));
    }

    /**
     * A helper method to cancel all pending alarms for intents that would be identical to the given
     * intent.
     * @param context The context the service runs in.
     * @param intent The intent to search and cancel.
     */
    private static void cancelRetriesForIntent(Context context, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(PendingIntent.getService(context, 0, intent, 0));

        Intent copyIntent = new Intent(intent);
        if (SYNC_FULL_SYNC.equals(copyIntent.getAction())) {
            // A full sync action should also cancel both of the other types of syncs
            copyIntent.setAction(SYNC_DOWNLOAD_ONLY);
            alarmManager.cancel(PendingIntent.getService(context, 0, copyIntent, 0));
            copyIntent.setAction(SYNC_UPLOAD_ONLY);
            alarmManager.cancel(PendingIntent.getService(context, 0, copyIntent, 0));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mQueryHelper = new VoicemailsQueryHelper(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "onHandleIntent: could not handle null intent");
            return;
        }

        String action = intent.getAction();

        PhoneAccountHandle phoneAccount = intent.getParcelableExtra(EXTRA_PHONE_ACCOUNT);

        LocalLogHelper.log(TAG, "Sync requested: " + action +
                " for all accounts: " + String.valueOf(phoneAccount == null));

        if (phoneAccount != null) {
            Log.v(TAG, "Sync requested: " + action + " - for account: " + phoneAccount);
            setupAndSendRequest(phoneAccount, action);
        } else {
            Log.v(TAG, "Sync requested: " + action + " - for all accounts");
            OmtpVvmSourceManager vvmSourceManager =
                    OmtpVvmSourceManager.getInstance(this);
            Set<PhoneAccountHandle> sources = vvmSourceManager.getOmtpVvmSources();
            for (PhoneAccountHandle source : sources) {
                setupAndSendRequest(source, action);
            }
        }
    }

    private void setupAndSendRequest(PhoneAccountHandle phoneAccount, String action) {
        if (!VisualVoicemailSettingsUtil.isVisualVoicemailEnabled(this, phoneAccount)) {
            Log.v(TAG, "Sync requested for disabled account");
            return;
        }

        if (SYNC_FULL_SYNC.equals(action)) {
            long lastSyncTime = VisualVoicemailSettingsUtil.getVisualVoicemailLastFullSyncTime(
                    this, phoneAccount);
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSyncTime < MINIMUM_FULL_SYNC_INTERVAL_MILLIS) {
                // If it's been less than a minute since the last sync, bail.
                Log.v(TAG, "Avoiding duplicate full sync: synced recently for "
                        + phoneAccount.getId());
                return;
            }
            VisualVoicemailSettingsUtil.setVisualVoicemailLastFullSyncTime(
                    this, phoneAccount, currentTime);
        }

        int subId = PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccount);
        OmtpVvmCarrierConfigHelper carrierConfigHelper =
                new OmtpVvmCarrierConfigHelper(this, subId);

        if (TelephonyManager.VVM_TYPE_CVVM.equals(carrierConfigHelper.getVvmType())) {
            doSync(null, null, phoneAccount, action);
        } else {
            OmtpVvmNetworkRequestCallback networkCallback = new OmtpVvmNetworkRequestCallback(
                    phoneAccount, action);
            requestNetwork(networkCallback);
        }
    }

    private class OmtpVvmNetworkRequestCallback extends ConnectivityManager.NetworkCallback {
        PhoneAccountHandle mPhoneAccount;
        String mAction;
        NetworkRequest mNetworkRequest;

        public OmtpVvmNetworkRequestCallback(PhoneAccountHandle phoneAccount,
                String action) {
            mPhoneAccount = phoneAccount;
            mAction = action;
            mNetworkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(
                            Integer.toString(
                                    PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccount)))
                    .build();
        }

        public NetworkRequest getNetworkRequest() {
            return mNetworkRequest;
        }

        @Override
        public void onAvailable(final Network network) {
            doSync(network, this, mPhoneAccount, mAction);
        }

        @Override
        public void onLost(Network network) {
            releaseNetwork(this);
        }

        @Override
        public void onUnavailable() {
            releaseNetwork(this);
        }
    }

    private void doSync(Network network, OmtpVvmNetworkRequestCallback callback,
            PhoneAccountHandle phoneAccount, String action) {
        int retryCount = NETWORK_RETRY_COUNT;

        boolean uploadSuccess;
        boolean downloadSuccess;

        while (retryCount > 0) {
            uploadSuccess = true;
            downloadSuccess = true;

            ImapHelper imapHelper = new ImapHelper(this, phoneAccount, network);
            if (!imapHelper.isSuccessfullyInitialized()) {
                Log.w(TAG, "Can't retrieve Imap credentials.");
                releaseNetwork(callback);
                VisualVoicemailSettingsUtil.resetVisualVoicemailRetryInterval(this,
                        phoneAccount);
                return;
            }

            if (SYNC_FULL_SYNC.equals(action) || SYNC_UPLOAD_ONLY.equals(action)) {
                uploadSuccess = upload(imapHelper);
            }
            if (SYNC_FULL_SYNC.equals(action) || SYNC_DOWNLOAD_ONLY.equals(action)) {
                downloadSuccess = download(imapHelper);
            }

            Log.v(TAG, "upload succeeded: ["+  String.valueOf(uploadSuccess)
                    + "] download succeeded: [" + String.valueOf(downloadSuccess) + "]");

            // Need to check again for whether visual voicemail is enabled because it could have
            // been disabled while waiting for the response from the network.
            if (VisualVoicemailSettingsUtil.isVisualVoicemailEnabled(this, phoneAccount) &&
                    (!uploadSuccess || !downloadSuccess)) {
                retryCount--;
                // Re-adjust so that only the unsuccessful action needs to be retried.
                // No need to re-adjust if both are unsuccessful. It means the full sync
                // failed so the action remains unchanged.
                if (uploadSuccess) {
                    action = SYNC_DOWNLOAD_ONLY;
                } else if (downloadSuccess) {
                    action = SYNC_UPLOAD_ONLY;
                }

                Log.v(TAG, "Retrying " + action);
                LocalLogHelper.log(TAG, "Immediately retrying " + action);
            } else {
                // Nothing more to do here, just exit.
                releaseNetwork(callback);

                VisualVoicemailSettingsUtil.resetVisualVoicemailRetryInterval(this, phoneAccount);
                return;
            }
        }

        releaseNetwork(callback);
        setRetryAlarm(phoneAccount, action);
    }

    private void requestNetwork(OmtpVvmNetworkRequestCallback networkCallback) {
        getConnectivityManager().requestNetwork(networkCallback.getNetworkRequest(),
                networkCallback, NETWORK_REQUEST_TIMEOUT_MILLIS);
    }

    private void releaseNetwork(NetworkCallback networkCallback) {
        if (networkCallback != null) {
            getConnectivityManager().unregisterNetworkCallback(networkCallback);
        }
    }

    private ConnectivityManager getConnectivityManager() {
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) this.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
        }
        return mConnectivityManager;
    }

    private void setRetryAlarm(PhoneAccountHandle phoneAccount, String action) {
        Intent serviceIntent = new Intent(this, OmtpVvmSyncService.class);
        serviceIntent.setAction(action);
        serviceIntent.putExtra(OmtpVvmSyncService.EXTRA_PHONE_ACCOUNT, phoneAccount);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, serviceIntent, 0);
        long retryInterval = VisualVoicemailSettingsUtil.getVisualVoicemailRetryInterval(this,
                phoneAccount);

        Log.v(TAG, "Retrying "+ action + " in " + retryInterval + "ms");
        LocalLogHelper.log(TAG, "Retrying "+ action + " in " + retryInterval + "ms");

        AlarmManager alarmManager = (AlarmManager)
                this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + retryInterval,
                pendingIntent);

        VisualVoicemailSettingsUtil.setVisualVoicemailRetryInterval(this, phoneAccount,
                retryInterval * 2);
    }

    private boolean upload(ImapHelper imapHelper) {
        List<Voicemail> readVoicemails = mQueryHelper.getReadVoicemails();
        List<Voicemail> deletedVoicemails = mQueryHelper.getDeletedVoicemails();

        boolean success = true;

        if (deletedVoicemails.size() > 0) {
            if (imapHelper.markMessagesAsDeleted(deletedVoicemails)) {
                // We want to delete selectively instead of all the voicemails for this provider
                // in case the state changed since the IMAP query was completed.
                mQueryHelper.deleteFromDatabase(deletedVoicemails);
            } else {
                success = false;
            }
        }

        if (readVoicemails.size() > 0) {
            if (imapHelper.markMessagesAsRead(readVoicemails)) {
                mQueryHelper.markReadInDatabase(readVoicemails);
            } else {
                success = false;
            }
        }

        return success;
    }

    private boolean download(ImapHelper imapHelper) {
        List<Voicemail> serverVoicemails = imapHelper.fetchAllVoicemails();
        List<Voicemail> localVoicemails = mQueryHelper.getAllVoicemails();

        if (localVoicemails == null || serverVoicemails == null) {
            // Null value means the query failed.
            return false;
        }

        Map<String, Voicemail> remoteMap = buildMap(serverVoicemails);

        // Go through all the local voicemails and check if they are on the server.
        // They may be read or deleted on the server but not locally. Perform the
        // appropriate local operation if the status differs from the server. Remove
        // the messages that exist both locally and on the server to know which server
        // messages to insert locally.
        for (int i = 0; i < localVoicemails.size(); i++) {
            Voicemail localVoicemail = localVoicemails.get(i);
            Voicemail remoteVoicemail = remoteMap.remove(localVoicemail.getSourceData());
            if (remoteVoicemail == null) {
                mQueryHelper.deleteFromDatabase(localVoicemail);
            } else {
                if (remoteVoicemail.isRead() != localVoicemail.isRead()) {
                    mQueryHelper.markReadInDatabase(localVoicemail);
                }
            }
        }

        // The leftover messages are messages that exist on the server but not locally.
        for (Voicemail remoteVoicemail : remoteMap.values()) {
            VoicemailContract.Voicemails.insert(this, remoteVoicemail);
        }

        return true;
    }

    /**
     * Builds a map from provider data to message for the given collection of voicemails.
     */
    private Map<String, Voicemail> buildMap(List<Voicemail> messages) {
        Map<String, Voicemail> map = new HashMap<String, Voicemail>();
        for (Voicemail message : messages) {
            map.put(message.getSourceData(), message);
        }
        return map;
    }
}
