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

import static android.app.admin.DeviceAdminReceiver.ACTION_PROFILE_PROVISIONING_COMPLETE;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ACCOUNT_TO_MIGRATE;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME;
import static android.Manifest.permission.BIND_DEVICE_ADMIN;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;

import com.android.managedprovisioning.CrossProfileIntentFiltersHelper;
import com.android.managedprovisioning.task.DeleteNonRequiredAppsTask;
import com.android.managedprovisioning.task.DisableBluetoothSharingTask;
import com.android.managedprovisioning.task.DisableInstallShortcutListenersTask;

import java.io.IOException;

/**
 * Service that runs the profile owner provisioning.
 *
 * <p>This service is started from and sends updates to the {@link ProfileOwnerProvisioningActivity},
 * which contains the provisioning UI.
 */
public class ProfileOwnerProvisioningService extends Service {
    // Intent actions for communication with DeviceOwnerProvisioningService.
    public static final String ACTION_PROVISIONING_SUCCESS =
            "com.android.managedprovisioning.provisioning_success";
    public static final String ACTION_PROVISIONING_ERROR =
            "com.android.managedprovisioning.error";
    public static final String ACTION_PROVISIONING_CANCELLED =
            "com.android.managedprovisioning.cancelled";
    public static final String EXTRA_LOG_MESSAGE_KEY = "ProvisioingErrorLogMessage";

    // Status flags for the provisioning process.
    /** Provisioning not started. */
    private static final int STATUS_UNKNOWN = 0;
    /** Provisioning started, no errors or cancellation requested received. */
    private static final int STATUS_STARTED = 1;
    /** Provisioning in progress, but user has requested cancellation. */
    private static final int STATUS_CANCELLING = 2;
    // Final possible states for the provisioning process.
    /** Provisioning completed successfully. */
    private static final int STATUS_DONE = 3;
    /** Provisioning failed and cleanup complete. */
    private static final int STATUS_ERROR = 4;
    /** Provisioning cancelled and cleanup complete. */
    private static final int STATUS_CANCELLED = 5;

    private IPackageManager mIpm;
    private UserInfo mManagedProfileUserInfo;
    private AccountManager mAccountManager;
    private UserManager mUserManager;

    private AsyncTask<Intent, Void, Void> runnerTask;

    // MessageId of the last error message.
    private String mLastErrorMessage = null;

    // Current status of the provisioning process.
    private int mProvisioningStatus = STATUS_UNKNOWN;

    private ProvisioningParams mParams;

    private class RunnerTask extends AsyncTask<Intent, Void, Void> {
        @Override
        protected Void doInBackground(Intent ... intents) {
            // Atomically move to STATUS_STARTED at most once.
            synchronized (ProfileOwnerProvisioningService.this) {
                if (mProvisioningStatus == STATUS_UNKNOWN) {
                    mProvisioningStatus = STATUS_STARTED;
                } else {
                    // Process already started, don't start again.
                    return null;
                }
            }

            try {
                initialize(intents[0]);
                startManagedProfileProvisioning();
            } catch (ProvisioningException e) {
                // Handle internal errors.
                error(e.getMessage(), e);
                finish();
            } catch (Exception e) {
                // General catch-all to ensure process cleans up in all cases.
                error("Failed to initialize managed profile, aborting.", e);
                finish();
            }

            return null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mIpm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mAccountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);

        runnerTask = new RunnerTask();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (ProfileOwnerProvisioningActivity.ACTION_CANCEL_PROVISIONING.equals(intent.getAction())) {
            ProvisionLogger.logd("Cancelling profile owner provisioning service");
            cancelProvisioning();
            return START_NOT_STICKY;
        }

        ProvisionLogger.logd("Starting profile owner provisioning service");

        try {
            runnerTask.execute(intent);
        } catch (IllegalStateException e) {
            // runnerTask is either in progress, or finished.
            ProvisionLogger.logd(
                    "ProfileOwnerProvisioningService: Provisioning already started, "
                    + "second provisioning intent not being processed, only reporting status.");
            reportStatus();
        }
        return START_NOT_STICKY;
    }

    private void reportStatus() {
        synchronized (this) {
            switch (mProvisioningStatus) {
                case STATUS_DONE:
                    notifyActivityOfSuccess();
                    break;
                case STATUS_CANCELLED:
                    notifyActivityCancelled();
                    break;
                case STATUS_ERROR:
                    notifyActivityError();
                    break;
                case STATUS_UNKNOWN:
                case STATUS_STARTED:
                case STATUS_CANCELLING:
                    // Don't notify UI of status when just-started/in-progress.
                    break;
            }
        }
    }

    private void cancelProvisioning() {
        synchronized (this) {
            switch (mProvisioningStatus) {
                case STATUS_DONE:
                    // Process completed, we should honor user request to cancel
                    // though.
                    mProvisioningStatus = STATUS_CANCELLING;
                    cleanupUserProfile();
                    mProvisioningStatus = STATUS_CANCELLED;
                    reportStatus();
                    break;
                case STATUS_UNKNOWN:
                    // Process hasn't started, move straight to cancelled state.
                    mProvisioningStatus = STATUS_CANCELLED;
                    reportStatus();
                    break;
                case STATUS_STARTED:
                    // Process is mid-flow, flag up that the user has requested
                    // cancellation.
                    mProvisioningStatus = STATUS_CANCELLING;
                    break;
                case STATUS_CANCELLING:
                    // Cancellation already being processed.
                    break;
                case STATUS_CANCELLED:
                case STATUS_ERROR:
                    // Process already completed, nothing left to cancel.
                    break;
            }
        }
    }

    private void initialize(Intent intent) {
        // Load the ProvisioningParams (from message in Intent).
        mParams = (ProvisioningParams) intent.getParcelableExtra(
                ProvisioningParams.EXTRA_PROVISIONING_PARAMS);
        if (mParams.accountToMigrate != null) {
            ProvisionLogger.logi("Migrating account to managed profile");
        }
    }

    /**
     * This is the core method of this class. It goes through every provisioning step.
     */
    private void startManagedProfileProvisioning() throws ProvisioningException {

        ProvisionLogger.logd("Starting managed profile provisioning");

        // Work through the provisioning steps in their corresponding order
        createProfile(getString(R.string.default_managed_profile_name));
        if (mManagedProfileUserInfo != null) {

            final DeleteNonRequiredAppsTask deleteNonRequiredAppsTask;
            final DisableInstallShortcutListenersTask disableInstallShortcutListenersTask;
            final DisableBluetoothSharingTask disableBluetoothSharingTask;

            disableInstallShortcutListenersTask = new DisableInstallShortcutListenersTask(this,
                    mManagedProfileUserInfo.id);
            disableBluetoothSharingTask = new DisableBluetoothSharingTask(
                    mManagedProfileUserInfo.id);
            deleteNonRequiredAppsTask = new DeleteNonRequiredAppsTask(this,
                    mParams.deviceAdminPackageName,
                    DeleteNonRequiredAppsTask.PROFILE_OWNER, true /* creating new profile */,
                    mManagedProfileUserInfo.id, false /* delete non-required system apps */,
                    new DeleteNonRequiredAppsTask.Callback() {

                        @Override
                        public void onSuccess() {
                            // Need to explicitly handle exceptions here, as
                            // onError() is not invoked for failures in
                            // onSuccess().
                            try {
                                disableBluetoothSharingTask.run();
                                disableInstallShortcutListenersTask.run();
                                setUpProfile();
                            } catch (ProvisioningException e) {
                                error(e.getMessage(), e);
                            } catch (Exception e) {
                                error("Provisioning failed", e);
                            }
                            finish();
                        }

                        @Override
                        public void onError() {
                            // Raise an error with a tracing exception attached.
                            error("Delete non required apps task failed.", new Exception());
                            finish();
                        }
                    });

            deleteNonRequiredAppsTask.run();
        }
    }

    /**
     * Called when the new profile is ready for provisioning (the profile is created and all the
     * apps not needed have been deleted).
     */
    private void setUpProfile() throws ProvisioningException {
        installMdmOnManagedProfile();
        setMdmAsActiveAdmin();
        setMdmAsManagedProfileOwner();
        setDefaultUserRestrictions();
        CrossProfileIntentFiltersHelper.setFilters(
                getPackageManager(), getUserId(), mManagedProfileUserInfo.id);

        if (!startManagedProfile(mManagedProfileUserInfo.id)) {
            throw raiseError("Could not start user in background");
        }
        // Note: account migration must happen after setting the profile owner.
        // Otherwise, there will be a time interval where some apps may think that the account does
        // not have a profile owner.
        copyAccount();
    }

    /**
     * Notify the calling activity of our final status, perform any cleanup if
     * the process didn't succeed.
     */
    private void finish() {
        ProvisionLogger.logi("Finishing provisioing process, status: "
                             + mProvisioningStatus);
        // Reached the end of the provisioning process, take appropriate action
        // based on current mProvisioningStatus.
        synchronized (this) {
            switch (mProvisioningStatus) {
                case STATUS_STARTED:
                    // Provisioning process completed normally.
                    notifyMdmAndCleanup();
                    mProvisioningStatus = STATUS_DONE;
                    break;
                case STATUS_UNKNOWN:
                    // No idea how we could end up in finish() in this state,
                    // but for safety treat it as an error and fall-through to
                    // STATUS_ERROR.
                    mLastErrorMessage = "finish() invoked in STATUS_UNKNOWN";
                    mProvisioningStatus = STATUS_ERROR;
                    break;
                case STATUS_ERROR:
                    // Process errored out, cleanup partially created managed
                    // profile.
                    cleanupUserProfile();
                    break;
                case STATUS_CANCELLING:
                    // User requested cancellation during processing, remove
                    // the successfully created profile.
                    cleanupUserProfile();
                    mProvisioningStatus = STATUS_CANCELLED;
                    break;
                case STATUS_CANCELLED:
                case STATUS_DONE:
                    // Shouldn't be possible to already be in this state?!?
                    ProvisionLogger.logw("finish() invoked multiple times?");
                    break;
            }
        }

        ProvisionLogger.logi("Finished provisioing process, final status: "
                + mProvisioningStatus);

        // Notify UI activity of final status reached.
        reportStatus();
    }

    /**
     * Initialize the user that underlies the managed profile.
     * This is required so that the provisioning complete broadcast can be sent across to the
     * profile and apps can run on it.
     */
    private boolean startManagedProfile(int userId)  {
        ProvisionLogger.logd("Starting user in background");
        IActivityManager iActivityManager = ActivityManagerNative.getDefault();
        try {
            return iActivityManager.startUserInBackground(userId);
        } catch (RemoteException neverThrown) {
            // Never thrown, as we are making local calls.
            ProvisionLogger.loge("This should not happen.", neverThrown);
        }
        return false;
    }

    private void notifyActivityOfSuccess() {
        Intent successIntent = new Intent(ACTION_PROVISIONING_SUCCESS);
        LocalBroadcastManager.getInstance(ProfileOwnerProvisioningService.this)
                .sendBroadcast(successIntent);
    }

    /**
     * Notify the mdm that provisioning has completed. When the mdm has received the intent, stop
     * the service and notify the {@link ProfileOwnerProvisioningActivity} so that it can finish
     * itself.
     */
    private void notifyMdmAndCleanup() {

        Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE,
                1 /* true- > setup complete */, mManagedProfileUserInfo.id);

        UserHandle managedUserHandle = new UserHandle(mManagedProfileUserInfo.id);

        // Use an ordered broadcast, so that we only finish when the mdm has received it.
        // Avoids a lag in the transition between provisioning and the mdm.
        BroadcastReceiver mdmReceivedSuccessReceiver = new MdmReceivedSuccessReceiver(
                mParams.accountToMigrate, mParams.deviceAdminPackageName);

        Intent completeIntent = new Intent(ACTION_PROFILE_PROVISIONING_COMPLETE);
        completeIntent.setComponent(mParams.deviceAdminComponentName);
        completeIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES |
                Intent.FLAG_RECEIVER_FOREGROUND);
        if (mParams.adminExtrasBundle != null) {
            completeIntent.putExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                    mParams.adminExtrasBundle);
        }

        // If profile owner provisioning was started after user setup is completed, then we
        // can directly send the ACTION_PROFILE_PROVISIONING_COMPLETE broadcast to the MDM.
        // But if the provisioning was started as part of setup wizard flow, we shutdown the
        // Setup wizard at the end of provisioning which will result in a home intent. So, to
        // avoid the race condition, HomeReceiverActivity is enabled which will in turn send
        // the ACTION_PROFILE_PROVISIONING_COMPLETE broadcast.
        if (Utils.isUserSetupCompleted(this)) {
            sendOrderedBroadcastAsUser(completeIntent, managedUserHandle, null,
                    mdmReceivedSuccessReceiver, null, Activity.RESULT_OK, null, null);
            ProvisionLogger.logd("Provisioning complete broadcast has been sent to user "
                    + managedUserHandle.getIdentifier());
        } else {
            IntentStore store = BootReminder.getProfileOwnerFinalizingIntentStore(this);
            Bundle resumeBundle = new Bundle();
            (new MessageParser()).addProvisioningParamsToBundle(resumeBundle, mParams);
            store.save(resumeBundle);

            // Enable the HomeReceiverActivity, since the ProfileOwnerProvisioningActivity will
            // shutdown the Setup wizard soon, which will result in a home intent that should be
            // caught by the HomeReceiverActivity.
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(this, HomeReceiverActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }
    }

    private void copyAccount() {
        if (mParams.accountToMigrate == null) {
            ProvisionLogger.logd("No account to migrate to the managed profile.");
            return;
        }
        ProvisionLogger.logd("Attempting to copy account to user " + mManagedProfileUserInfo.id);
        try {
            if (mAccountManager.copyAccountToUser(mParams.accountToMigrate,
                    mManagedProfileUserInfo.getUserHandle(),
                    /* callback= */ null, /* handler= */ null).getResult()) {
                ProvisionLogger.logi("Copied account to user " + mManagedProfileUserInfo.id);
            } else {
                ProvisionLogger.loge("Could not copy account to user "
                        + mManagedProfileUserInfo.id);
            }
        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
            ProvisionLogger.logw("Exception copying account to user " + mManagedProfileUserInfo.id,
                    e);
        }
    }

    private void createProfile(String profileName) throws ProvisioningException {

        ProvisionLogger.logd("Creating managed profile with name " + profileName);

        mManagedProfileUserInfo = mUserManager.createProfileForUser(profileName,
                UserInfo.FLAG_MANAGED_PROFILE | UserInfo.FLAG_DISABLED,
                Process.myUserHandle().getIdentifier());

        if (mManagedProfileUserInfo == null) {
            throw raiseError("Couldn't create profile.");
        }
    }

    private void installMdmOnManagedProfile() throws ProvisioningException {
        ProvisionLogger.logd("Installing mobile device management app "
                + mParams.deviceAdminPackageName + " on managed profile");

        try {
            int status = mIpm.installExistingPackageAsUser(
                mParams.deviceAdminPackageName, mManagedProfileUserInfo.id);
            switch (status) {
              case PackageManager.INSTALL_SUCCEEDED:
                  return;
              case PackageManager.INSTALL_FAILED_USER_RESTRICTED:
                  // Should not happen because we're not installing a restricted user
                  throw raiseError("Could not install mobile device management app on managed "
                          + "profile because the user is restricted");
              case PackageManager.INSTALL_FAILED_INVALID_URI:
                  // Should not happen because we already checked
                  throw raiseError("Could not install mobile device management app on managed "
                          + "profile because the package could not be found");
              default:
                  throw raiseError("Could not install mobile device management app on managed "
                          + "profile. Unknown status: " + status);
            }
        } catch (RemoteException neverThrown) {
            // Never thrown, as we are making local calls.
            ProvisionLogger.loge("This should not happen.", neverThrown);
        }
    }

    private void setMdmAsManagedProfileOwner() throws ProvisioningException {
        ProvisionLogger.logd("Setting package " + mParams.deviceAdminPackageName
                + " as managed profile owner.");

        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!dpm.setProfileOwner(mParams.deviceAdminComponentName, mParams.deviceAdminPackageName,
                mManagedProfileUserInfo.id)) {
            ProvisionLogger.logw("Could not set profile owner.");
            throw raiseError("Could not set profile owner.");
        }
    }

    private void setMdmAsActiveAdmin() {
        ProvisionLogger.logd("Setting package " + mParams.deviceAdminPackageName
                + " as active admin.");

        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        dpm.setActiveAdmin(mParams.deviceAdminComponentName, true /* refreshing*/,
                mManagedProfileUserInfo.id);
    }

    private ProvisioningException raiseError(String message) throws ProvisioningException {
        throw new ProvisioningException(message);
    }

    /**
     * Record the fact that an error occurred, change mProvisioningStatus to
     * reflect the fact the provisioning process failed
     */
    private void error(String dialogMessage, Exception e) {
        synchronized (this) {
            // Only case where an error condition should be notified is if we
            // are in the normal flow for provisioning. If the process has been
            // cancelled or already completed, then the fact there is an error
            // is almost irrelevant.
            if (mProvisioningStatus == STATUS_STARTED) {
                mProvisioningStatus = STATUS_ERROR;
                mLastErrorMessage = dialogMessage;

                ProvisionLogger.logw(
                        "Error occured during provisioning process: "
                        + dialogMessage,
                        e);
            } else {
                ProvisionLogger.logw(
                        "Unexpected error occured in status ["
                        + mProvisioningStatus + "]: " + dialogMessage,
                        e);
            }
        }
    }

    private void setDefaultUserRestrictions() {
        mUserManager.setUserRestriction(UserManager.DISALLOW_WALLPAPER, true,
                mManagedProfileUserInfo.getUserHandle());
    }

    private void notifyActivityError() {
        Intent intent = new Intent(ACTION_PROVISIONING_ERROR);
        intent.putExtra(EXTRA_LOG_MESSAGE_KEY, mLastErrorMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void notifyActivityCancelled() {
        Intent cancelIntent = new Intent(ACTION_PROVISIONING_CANCELLED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(cancelIntent);
    }

    /**
     * Performs cleanup of any created user-profile on failure/cancellation.
     */
    private void cleanupUserProfile() {
        if (mManagedProfileUserInfo != null) {
            ProvisionLogger.logd("Removing managed profile");
            mUserManager.removeUser(mManagedProfileUserInfo.id);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Internal exception to allow provisioning process to terminal quickly and
     * cleanly on first error, rather than continuing to process despite errors
     * occurring.
     */
    private static class ProvisioningException extends Exception {
        public ProvisioningException(String detailMessage) {
            super(detailMessage);
        }
    }
}
