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

package com.android.server.telecom;

import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.MODIFY_PHONE_STATE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE;
import static android.Manifest.permission.REGISTER_SIM_SUBSCRIPTION;
import static android.Manifest.permission.WRITE_SECURE_SETTINGS;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

// TODO: Needed for move to system service: import com.android.internal.R;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.components.UserCallIntentProcessor;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of the ITelecom interface.
 */
public class TelecomServiceImpl {
    private static final String PERMISSION_PROCESS_PHONE_ACCOUNT_REGISTRATION =
            "android.permission.PROCESS_PHONE_ACCOUNT_REGISTRATION";

    private final ITelecomService.Stub mBinderImpl = new ITelecomService.Stub() {
        @Override
        public PhoneAccountHandle getDefaultOutgoingPhoneAccount(String uriScheme,
                String callingPackage) {
            synchronized (mLock) {
                if (!canReadPhoneState(callingPackage, "getDefaultOutgoingPhoneAccount")) {
                    return null;
                }

                long token = Binder.clearCallingIdentity();
                try {
                    PhoneAccountHandle defaultOutgoingPhoneAccount =
                            mPhoneAccountRegistrar.getOutgoingPhoneAccountForScheme(uriScheme);
                    // Make sure that the calling user can see this phone account.
                    // TODO: Does this isVisible check actually work considering we are clearing
                    // the calling identity?
                    if (defaultOutgoingPhoneAccount != null
                            && !isVisibleToCaller(defaultOutgoingPhoneAccount)) {
                        Log.w(this, "No account found for the calling user");
                        return null;
                    }
                    return defaultOutgoingPhoneAccount;
                } catch (Exception e) {
                    Log.e(this, e, "getDefaultOutgoingPhoneAccount");
                    throw e;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        @Override
        public PhoneAccountHandle getUserSelectedOutgoingPhoneAccount() {
            synchronized (mLock) {
                try {
                    PhoneAccountHandle userSelectedOutgoingPhoneAccount =
                            mPhoneAccountRegistrar.getUserSelectedOutgoingPhoneAccount();
                    // Make sure that the calling user can see this phone account.
                    if (!isVisibleToCaller(userSelectedOutgoingPhoneAccount)) {
                        Log.w(this, "No account found for the calling user");
                        return null;
                    }
                    return userSelectedOutgoingPhoneAccount;
                } catch (Exception e) {
                    Log.e(this, e, "getUserSelectedOutgoingPhoneAccount");
                    throw e;
                }
            }
        }

        @Override
        public void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle accountHandle) {
            synchronized (mLock) {
                enforceModifyPermission();

                long token = Binder.clearCallingIdentity();
                try {
                    mPhoneAccountRegistrar.setUserSelectedOutgoingPhoneAccount(accountHandle);
                } catch (Exception e) {
                    Log.e(this, e, "setUserSelectedOutgoingPhoneAccount");
                    throw e;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        @Override
        public List<PhoneAccountHandle> getCallCapablePhoneAccounts(
                boolean includeDisabledAccounts, String callingPackage) {
            if (!canReadPhoneState(callingPackage, "getDefaultOutgoingPhoneAccount")) {
                return Collections.emptyList();
            }

            synchronized (mLock) {
                long token = Binder.clearCallingIdentity();
                try {
                    // TODO: Does this isVisible check actually work considering we are clearing
                    // the calling identity?
                    return filterForAccountsVisibleToCaller(
                            mPhoneAccountRegistrar.getCallCapablePhoneAccounts(
                                    null, includeDisabledAccounts));
                } catch (Exception e) {
                    Log.e(this, e, "getCallCapablePhoneAccounts");
                    throw e;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        @Override
        public List<PhoneAccountHandle> getPhoneAccountsSupportingScheme(String uriScheme,
                String callingPackage) {
            synchronized (mLock) {
                if (!canReadPhoneState(callingPackage, "getPhoneAccountsSupportingScheme")) {
                    return Collections.emptyList();
                }

                long token = Binder.clearCallingIdentity();
                try {
                    // TODO: Does this isVisible check actually work considering we are clearing
                    // the calling identity?
                    return filterForAccountsVisibleToCaller(
                            mPhoneAccountRegistrar.getCallCapablePhoneAccounts(uriScheme, false));
                } catch (Exception e) {
                    Log.e(this, e, "getPhoneAccountsSupportingScheme %s", uriScheme);
                    throw e;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        @Override
        public List<PhoneAccountHandle> getPhoneAccountsForPackage(String packageName) {
            synchronized (mLock) {
                try {
                    return filterForAccountsVisibleToCaller(
                            mPhoneAccountRegistrar.getPhoneAccountsForPackage(packageName));
                } catch (Exception e) {
                    Log.e(this, e, "getPhoneAccountsForPackage %s", packageName);
                    throw e;
                }
            }
        }

        @Override
        public PhoneAccount getPhoneAccount(PhoneAccountHandle accountHandle) {
            synchronized (mLock) {
                try {
                    if (!isVisibleToCaller(accountHandle)) {
                        Log.d(this, "%s is not visible for the calling user [gPA]", accountHandle);
                        return null;
                    }
                    // TODO: Do we really want to return for *any* user?
                    return mPhoneAccountRegistrar.getPhoneAccount(accountHandle);
                } catch (Exception e) {
                    Log.e(this, e, "getPhoneAccount %s", accountHandle);
                    throw e;
                }
            }
        }

        @Override
        public int getAllPhoneAccountsCount() {
            synchronized (mLock) {
                try {
                    // This list is pre-filtered for the calling user.
                    return getAllPhoneAccounts().size();
                } catch (Exception e) {
                    Log.e(this, e, "getAllPhoneAccountsCount");
                    throw e;
                }
            }
        }

        @Override
        public List<PhoneAccount> getAllPhoneAccounts() {
            synchronized (mLock) {
                try {
                    List<PhoneAccount> allPhoneAccounts = mPhoneAccountRegistrar
                            .getAllPhoneAccounts();
                    List<PhoneAccount> profilePhoneAccounts = new ArrayList<>(
                            allPhoneAccounts.size());
                    for (PhoneAccount phoneAccount : profilePhoneAccounts) {
                        if (isVisibleToCaller(phoneAccount)) {
                            profilePhoneAccounts.add(phoneAccount);
                        }
                    }
                    return profilePhoneAccounts;
                } catch (Exception e) {
                    Log.e(this, e, "getAllPhoneAccounts");
                    throw e;
                }
            }
        }

        @Override
        public List<PhoneAccountHandle> getAllPhoneAccountHandles() {
            synchronized (mLock) {
                try {
                    return filterForAccountsVisibleToCaller(
                            mPhoneAccountRegistrar.getAllPhoneAccountHandles());
                } catch (Exception e) {
                    Log.e(this, e, "getAllPhoneAccounts");
                    throw e;
                }
            }
        }

        @Override
        public PhoneAccountHandle getSimCallManager() {
            long token  = Binder.clearCallingIdentity();
            int user;
            try {
                user = ActivityManager.getCurrentUser();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
            return getSimCallManagerForUser(user);
        }

        @Override
        public PhoneAccountHandle getSimCallManagerForUser(int user) {
            synchronized (mLock) {
                try {
                    PhoneAccountHandle accountHandle = null;

                    long token = Binder.clearCallingIdentity();
                    try {
                        accountHandle = mPhoneAccountRegistrar.getSimCallManager(user);
                    } finally {
                        // We restore early so that isVisibleToCaller invocation below uses the
                        // right user context.
                        Binder.restoreCallingIdentity(token);
                    }

                    if (!isVisibleToCaller(accountHandle)) {
                        Log.d(this, "%s is not visible for the calling user [gsCM]", accountHandle);
                        return null;
                    }
                    return accountHandle;
                } catch (Exception e) {
                    Log.e(this, e, "getSimCallManager");
                    throw e;
                }
            }
        }

        @Override
        public void registerPhoneAccount(PhoneAccount account) {
            synchronized (mLock) {
                if (!mContext.getApplicationContext().getResources().getBoolean(
                        com.android.internal.R.bool.config_voice_capable)) {
                    Log.w(this, "registerPhoneAccount not allowed on non-voice capable device.");
                    return;
                }
                try {
                    enforcePhoneAccountModificationForPackage(
                            account.getAccountHandle().getComponentName().getPackageName());
                    if (account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
                        enforceRegisterSimSubscriptionPermission();
                    }
                    if (account.hasCapabilities(PhoneAccount.CAPABILITY_MULTI_USER)) {
                        enforceRegisterMultiUser();
                    }
                    enforceUserHandleMatchesCaller(account.getAccountHandle());

                    mPhoneAccountRegistrar.registerPhoneAccount(account);

                    // Broadcast an intent indicating the phone account which was registered.
                    long token = Binder.clearCallingIdentity();
                    try {
                        Intent intent = new Intent(TelecomManager.ACTION_PHONE_ACCOUNT_REGISTERED);
                        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                                account.getAccountHandle());
                        Log.i(this, "Sending phone-account intent as user");
                        mContext.sendBroadcastAsUser(intent, UserHandle.ALL,
                                PERMISSION_PROCESS_PHONE_ACCOUNT_REGISTRATION);
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } catch (Exception e) {
                    Log.e(this, e, "registerPhoneAccount %s", account);
                    throw e;
                }
            }
        }

        @Override
        public void unregisterPhoneAccount(PhoneAccountHandle accountHandle) {
            synchronized (mLock) {
                try {
                    enforcePhoneAccountModificationForPackage(
                            accountHandle.getComponentName().getPackageName());
                    enforceUserHandleMatchesCaller(accountHandle);
                    mPhoneAccountRegistrar.unregisterPhoneAccount(accountHandle);
                } catch (Exception e) {
                    Log.e(this, e, "unregisterPhoneAccount %s", accountHandle);
                    throw e;
                }
            }
        }

        @Override
        public void clearAccounts(String packageName) {
            synchronized (mLock) {
                try {
                    enforcePhoneAccountModificationForPackage(packageName);
                    mPhoneAccountRegistrar
                            .clearAccounts(packageName, Binder.getCallingUserHandle());
                } catch (Exception e) {
                    Log.e(this, e, "clearAccounts %s", packageName);
                    throw e;
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#isVoiceMailNumber
         */
        @Override
        public boolean isVoiceMailNumber(PhoneAccountHandle accountHandle, String number,
                String callingPackage) {
            synchronized (mLock) {
                if (!canReadPhoneState(callingPackage, "isVoiceMailNumber")) {
                    return false;
                }

                if (!isVisibleToCaller(accountHandle)) {
                    Log.d(this, "%s is not visible for the calling user [iVMN]", accountHandle);
                    return false;
                }

                long token = Binder.clearCallingIdentity();
                try {
                    return mPhoneAccountRegistrar.isVoiceMailNumber(accountHandle, number);
                } catch (Exception e) {
                    Log.e(this, e, "getSubscriptionIdForPhoneAccount");
                    throw e;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#getVoiceMailNumber
         */
        @Override
        public String getVoiceMailNumber(PhoneAccountHandle accountHandle, String callingPackage) {
            synchronized (mLock) {
                if (!canReadPhoneState(callingPackage, "getVoiceMailNumber")) {
                    return null;
                }

                try {
                    if (!isVisibleToCaller(accountHandle)) {
                        Log.d(this, "%s is not visible for the calling user [gVMN]", accountHandle);
                        return null;
                    }

                    int subId = SubscriptionManager.getDefaultVoiceSubId();
                    if (accountHandle != null) {
                        subId = mPhoneAccountRegistrar
                                .getSubscriptionIdForPhoneAccount(accountHandle);
                    }
                    return getTelephonyManager().getVoiceMailNumber(subId);
                } catch (Exception e) {
                    Log.e(this, e, "getSubscriptionIdForPhoneAccount");
                    throw e;
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#getLine1Number
         */
        @Override
        public String getLine1Number(PhoneAccountHandle accountHandle, String callingPackage) {
            if (!canReadPhoneState(callingPackage, "getLine1Number")) {
                return null;
            }

            synchronized (mLock) {
                if (!isVisibleToCaller(accountHandle)) {
                    Log.d(this, "%s is not visible for the calling user [gL1N]", accountHandle);
                    return null;
                }

                long token = Binder.clearCallingIdentity();
                try {
                    int subId =
                            mPhoneAccountRegistrar.getSubscriptionIdForPhoneAccount(accountHandle);
                    return getTelephonyManager().getLine1NumberForSubscriber(subId);
                } catch (Exception e) {
                    Log.e(this, e, "getSubscriptionIdForPhoneAccount");
                    throw e;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#silenceRinger
         */
        @Override
        public void silenceRinger(String callingPackage) {
            synchronized (mLock) {
                enforcePermissionOrPrivilegedDialer(MODIFY_PHONE_STATE, callingPackage);

                long token = Binder.clearCallingIdentity();
                try {
                    Log.i(this, "Silence Ringer requested by %s", callingPackage);
                    mCallsManager.getRinger().silence();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#getDefaultPhoneApp
         * @deprecated - Use {@link android.telecom.TelecomManager#getDefaultDialerPackage()}
         *         instead.
         */
        @Override
        public ComponentName getDefaultPhoneApp() {
            // No need to synchronize
            Resources resources = mContext.getResources();
            return new ComponentName(
                    resources.getString(R.string.ui_default_package),
                    resources.getString(R.string.dialer_default_class));
        }

        /**
         * @return the package name of the current user-selected default dialer. If no default
         *         has been selected, the package name of the system dialer is returned. If
         *         neither exists, then {@code null} is returned.
         * @see android.telecom.TelecomManager#getDefaultDialerPackage
         */
        @Override
        public String getDefaultDialerPackage() {
            final long token = Binder.clearCallingIdentity();
            try {
                return DefaultDialerManager.getDefaultDialerApplication(mContext);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /**
         * @see android.telecom.TelecomManager#getSystemDialerPackage
         */
        @Override
        public String getSystemDialerPackage() {
            return mContext.getResources().getString(R.string.ui_default_package);
        }

        /**
         * @see android.telecom.TelecomManager#isInCall
         */
        @Override
        public boolean isInCall(String callingPackage) {
            if (!canReadPhoneState(callingPackage, "isInCall")) {
                return false;
            }

            synchronized (mLock) {
                final int callState = mCallsManager.getCallState();
                return callState == TelephonyManager.CALL_STATE_OFFHOOK
                        || callState == TelephonyManager.CALL_STATE_RINGING;
            }
        }

        /**
         * @see android.telecom.TelecomManager#isRinging
         */
        @Override
        public boolean isRinging(String callingPackage) {
            if (!canReadPhoneState(callingPackage, "isRinging")) {
                return false;
            }

            synchronized (mLock) {
                return mCallsManager.getCallState() == TelephonyManager.CALL_STATE_RINGING;
            }
        }

        /**
         * @see TelecomManager#getCallState
         */
        @Override
        public int getCallState() {
            synchronized (mLock) {
                return mCallsManager.getCallState();
            }
        }

        /**
         * @see android.telecom.TelecomManager#endCall
         */
        @Override
        public boolean endCall() {
            synchronized (mLock) {
                enforceModifyPermission();

                long token = Binder.clearCallingIdentity();
                try {
                    return endCallInternal();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#acceptRingingCall
         */
        @Override
        public void acceptRingingCall() {
            synchronized (mLock) {
                enforceModifyPermission();

                long token = Binder.clearCallingIdentity();
                try {
                    acceptRingingCallInternal();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#showInCallScreen
         */
        @Override
        public void showInCallScreen(boolean showDialpad, String callingPackage) {
            if (!canReadPhoneState(callingPackage, "showInCallScreen")) {
                return;
            }

            synchronized (mLock) {

                long token = Binder.clearCallingIdentity();
                try {
                    mCallsManager.getInCallController().bringToForeground(showDialpad);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#cancelMissedCallsNotification
         */
        @Override
        public void cancelMissedCallsNotification(String callingPackage) {
            synchronized (mLock) {
                enforcePermissionOrPrivilegedDialer(MODIFY_PHONE_STATE, callingPackage);
                long token = Binder.clearCallingIdentity();
                try {
                    mCallsManager.getMissedCallNotifier().clearMissedCalls();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#handleMmi
         */
        @Override
        public boolean handlePinMmi(String dialString, String callingPackage) {
            synchronized (mLock) {
                enforcePermissionOrPrivilegedDialer(MODIFY_PHONE_STATE, callingPackage);

                // Switch identity so that TelephonyManager checks Telecom's permissions instead.
                long token = Binder.clearCallingIdentity();
                boolean retval = false;
                try {
                    retval = getTelephonyManager().handlePinMmi(dialString);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }

                return retval;
            }
        }

        /**
         * @see android.telecom.TelecomManager#handleMmi
         */
        @Override
        public boolean handlePinMmiForPhoneAccount(
                PhoneAccountHandle accountHandle,
                String dialString,
                String callingPackage) {
            synchronized (mLock) {
                enforcePermissionOrPrivilegedDialer(MODIFY_PHONE_STATE, callingPackage);

                if (!isVisibleToCaller(accountHandle)) {
                    Log.d(this, "%s is not visible for the calling user [hMMI]", accountHandle);
                    return false;
                }

                // Switch identity so that TelephonyManager checks Telecom's permissions instead.
                long token = Binder.clearCallingIdentity();
                boolean retval = false;
                try {
                    int subId = mPhoneAccountRegistrar
                            .getSubscriptionIdForPhoneAccount(accountHandle);
                    retval = getTelephonyManager().handlePinMmiForSubscriber(subId, dialString);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }

                return retval;
            }
        }

        /**
         * @see android.telecom.TelecomManager#getAdnUriForPhoneAccount
         */
        @Override
        public Uri getAdnUriForPhoneAccount(PhoneAccountHandle accountHandle,
                String callingPackage) {
            synchronized (mLock) {
                enforcePermissionOrPrivilegedDialer(MODIFY_PHONE_STATE, callingPackage);

                if (!isVisibleToCaller(accountHandle)) {
                    Log.d(this, "%s is not visible for the calling user [gA4PA]", accountHandle);
                    return null;
                }

                // Switch identity so that TelephonyManager checks Telecom's permissions instead.
                long token = Binder.clearCallingIdentity();
                String retval = "content://icc/adn/";
                try {
                    long subId = mPhoneAccountRegistrar
                            .getSubscriptionIdForPhoneAccount(accountHandle);
                    retval = retval + "subId/" + subId;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }

                return Uri.parse(retval);
            }
        }

        /**
         * @see android.telecom.TelecomManager#isTtySupported
         */
        @Override
        public boolean isTtySupported(String callingPackage) {
            if (!canReadPhoneState(callingPackage, "hasVoiceMailNumber")) {
                return false;
            }

            synchronized (mLock) {
                return mCallsManager.isTtySupported();
            }
        }

        /**
         * @see android.telecom.TelecomManager#getCurrentTtyMode
         */
        @Override
        public int getCurrentTtyMode(String callingPackage) {
            if (!canReadPhoneState(callingPackage, "getCurrentTtyMode")) {
                return TelecomManager.TTY_MODE_OFF;
            }

            synchronized (mLock) {
                return mCallsManager.getCurrentTtyMode();
            }
        }

        /**
         * @see android.telecom.TelecomManager#addNewIncomingCall
         */
        @Override
        public void addNewIncomingCall(PhoneAccountHandle phoneAccountHandle, Bundle extras) {
            synchronized (mLock) {
                Log.i(this, "Adding new incoming call with phoneAccountHandle %s",
                        phoneAccountHandle);
                if (phoneAccountHandle != null && phoneAccountHandle.getComponentName() != null) {
                    // TODO(sail): Add unit tests for adding incoming calls from a SIM call manager.
                    if (isCallerSimCallManager() && TelephonyUtil.isPstnComponentName(
                            phoneAccountHandle.getComponentName())) {
                        Log.v(this, "Allowing call manager to add incoming call with PSTN handle");
                    } else {
                        mAppOpsManager.checkPackage(
                                Binder.getCallingUid(),
                                phoneAccountHandle.getComponentName().getPackageName());
                        // Make sure it doesn't cross the UserHandle boundary
                        enforceUserHandleMatchesCaller(phoneAccountHandle);
                    }

                    long token = Binder.clearCallingIdentity();
                    try {
                        Intent intent = new Intent(TelecomManager.ACTION_INCOMING_CALL);
                        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                            phoneAccountHandle);
                        intent.putExtra(CallIntentProcessor.KEY_IS_INCOMING_CALL, true);
                        if (extras != null) {
                            intent.putExtra(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, extras);
                        }
                        CallIntentProcessor.processIncomingCallIntent(mCallsManager, intent);
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    Log.w(this,
                            "Null phoneAccountHandle. Ignoring request to add new incoming call");
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#addNewUnknownCall
         */
        @Override
        public void addNewUnknownCall(PhoneAccountHandle phoneAccountHandle, Bundle extras) {
            synchronized (mLock) {
                if (phoneAccountHandle != null && phoneAccountHandle.getComponentName() != null) {
                    mAppOpsManager.checkPackage(
                            Binder.getCallingUid(),
                            phoneAccountHandle.getComponentName().getPackageName());

                    // Make sure it doesn't cross the UserHandle boundary
                    enforceUserHandleMatchesCaller(phoneAccountHandle);
                    long token = Binder.clearCallingIdentity();

                    try {
                        Intent intent = new Intent(TelecomManager.ACTION_NEW_UNKNOWN_CALL);
                        intent.putExtras(extras);
                        intent.putExtra(CallIntentProcessor.KEY_IS_UNKNOWN_CALL, true);
                        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                            phoneAccountHandle);
                        CallIntentProcessor.processUnknownCallIntent(mCallsManager, intent);
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    Log.i(this,
                            "Null phoneAccountHandle or not initiated by Telephony. " +
                            "Ignoring request to add new unknown call.");
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#placeCall
         */
        @Override
        public void placeCall(Uri handle, Bundle extras, String callingPackage) {
            enforceCallingPackage(callingPackage);
            if (!canCallPhone(callingPackage, "placeCall")) {
                throw new SecurityException("Package " + callingPackage
                        + " is not allowed to place phone calls");
            }

            // Note: we can still get here for the default/system dialer, even if the Phone
            // permission is turned off. This is because the default/system dialer is always
            // allowed to attempt to place a call (regardless of permission state), in case
            // it turns out to be an emergency call. If the permission is denied and the
            // call is being made to a non-emergency number, the call will be denied later on
            // by {@link UserCallIntentProcessor}.

            final boolean hasCallAppOp = mAppOpsManager.noteOp(AppOpsManager.OP_CALL_PHONE,
                    Binder.getCallingUid(), callingPackage) == AppOpsManager.MODE_ALLOWED;

            final boolean hasCallPermission = mContext.checkCallingPermission(CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED;

            synchronized (mLock) {
                final UserHandle userHandle = Binder.getCallingUserHandle();
                long token = Binder.clearCallingIdentity();
                try {
                    final Intent intent = new Intent(Intent.ACTION_CALL, handle);
                    intent.putExtras(extras);
                    new UserCallIntentProcessor(mContext, userHandle).processIntent(intent,
                            callingPackage, hasCallAppOp && hasCallPermission);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * @see android.telecom.TelecomManager#enablePhoneAccount
         */
        @Override
        public boolean enablePhoneAccount(PhoneAccountHandle accountHandle, boolean isEnabled) {
            enforceModifyPermission();
            synchronized (mLock) {
                long token  = Binder.clearCallingIdentity();
                try {
                    // enable/disable phone account
                    return mPhoneAccountRegistrar.enablePhoneAccount(accountHandle, isEnabled);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        @Override
        public boolean setDefaultDialer(String packageName) {
            enforcePermission(MODIFY_PHONE_STATE);
            enforcePermission(WRITE_SECURE_SETTINGS);
            synchronized (mLock) {
                long token  = Binder.clearCallingIdentity();
                try {
                    final boolean result =
                            DefaultDialerManager.setDefaultDialerApplication(mContext, packageName);
                    if (result) {
                        final Intent intent =
                                new Intent(TelecomManager.ACTION_DEFAULT_DIALER_CHANGED);
                        intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                                packageName);
                        mContext.sendBroadcastAsUser(intent,
                                new UserHandle(ActivityManager.getCurrentUser()));
                    }
                    return result;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        /**
         * Dumps the current state of the TelecomService.  Used when generating problem reports.
         *
         * @param fd The file descriptor.
         * @param writer The print writer to dump the state to.
         * @param args Optional dump arguments.
         */
        @Override
        protected void dump(FileDescriptor fd, final PrintWriter writer, String[] args) {
            if (mContext.checkCallingOrSelfPermission(
                    android.Manifest.permission.DUMP)
                    != PackageManager.PERMISSION_GRANTED) {
                writer.println("Permission Denial: can't dump TelecomService " +
                        "from from pid=" + Binder.getCallingPid() + ", uid=" +
                        Binder.getCallingUid());
                return;
            }

            final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            if (mCallsManager != null) {
                pw.println("CallsManager: ");
                pw.increaseIndent();
                mCallsManager.dump(pw);
                pw.decreaseIndent();

                pw.println("PhoneAccountRegistrar: ");
                pw.increaseIndent();
                mPhoneAccountRegistrar.dump(pw);
                pw.decreaseIndent();
            }

            Log.dumpCallEvents(pw);
        }
    };

    private Context mContext;
    private AppOpsManager mAppOpsManager;
    private UserManager mUserManager;
    private PackageManager mPackageManager;
    private CallsManager mCallsManager;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final TelecomSystem.SyncRoot mLock;

    public TelecomServiceImpl(
            Context context,
            CallsManager callsManager,
            PhoneAccountRegistrar phoneAccountRegistrar,
            TelecomSystem.SyncRoot lock) {
        mContext = context;
        mAppOpsManager = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);

        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mPackageManager = mContext.getPackageManager();

        mCallsManager = callsManager;
        mLock = lock;
        mPhoneAccountRegistrar = phoneAccountRegistrar;
    }

    public ITelecomService.Stub getBinder() {
        return mBinderImpl;
    }

    //
    // Supporting methods for the ITelecomService interface implementation.
    //

    private boolean isVisibleToCaller(PhoneAccountHandle accountHandle) {
        if (accountHandle == null) {
            return false;
        }
        return isVisibleToCaller(mPhoneAccountRegistrar.getPhoneAccount(accountHandle));
    }

    private boolean isVisibleToCaller(PhoneAccount account) {
        if (account == null) {
            return false;
        }

        // If this PhoneAccount has CAPABILITY_MULTI_USER, it should be visible to all users and
        // all profiles. Only Telephony and SIP accounts should have this capability.
        if (account.hasCapabilities(PhoneAccount.CAPABILITY_MULTI_USER)) {
            return true;
        }

        UserHandle phoneAccountUserHandle = account.getAccountHandle().getUserHandle();
        if (phoneAccountUserHandle == null) {
            return false;
        }

        if (phoneAccountUserHandle.equals(Binder.getCallingUserHandle())) {
            return true;
        }

        List<UserHandle> profileUserHandles;
        if (UserHandle.getCallingUserId() == UserHandle.USER_OWNER) {
            profileUserHandles = mUserManager.getUserProfiles();
        } else {
            // Otherwise, it has to be owned by the current caller's profile.
            profileUserHandles = new ArrayList<>(1);
            profileUserHandles.add(Binder.getCallingUserHandle());
        }

        return profileUserHandles.contains(phoneAccountUserHandle);
    }

    /**
     * Given a list of {@link PhoneAccountHandle}s, filter them to the ones that the calling
     * user can see.
     *
     * @param phoneAccountHandles Unfiltered list of account handles.
     *
     * @return {@link PhoneAccountHandle}s visible to the calling user and its profiles.
     */
    private List<PhoneAccountHandle> filterForAccountsVisibleToCaller(
            List<PhoneAccountHandle> phoneAccountHandles) {
        List<PhoneAccountHandle> profilePhoneAccountHandles =
                new ArrayList<>(phoneAccountHandles.size());
        for (PhoneAccountHandle phoneAccountHandle : phoneAccountHandles) {
            if (isVisibleToCaller(phoneAccountHandle)) {
                profilePhoneAccountHandles.add(phoneAccountHandle);
            }
        }
        return profilePhoneAccountHandles;
    }

    private boolean isCallerSystemApp() {
        int uid = Binder.getCallingUid();
        String[] packages = mPackageManager.getPackagesForUid(uid);
        for (String packageName : packages) {
            if (isPackageSystemApp(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPackageSystemApp(String packageName) {
        try {
            ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    private void acceptRingingCallInternal() {
        Call call = mCallsManager.getFirstCallWithState(CallState.RINGING);
        if (call != null) {
            call.answer(call.getVideoState());
        }
    }

    private boolean endCallInternal() {
        // Always operate on the foreground call if one exists, otherwise get the first call in
        // priority order by call-state.
        Call call = mCallsManager.getForegroundCall();
        if (call == null) {
            call = mCallsManager.getFirstCallWithState(
                    CallState.ACTIVE,
                    CallState.DIALING,
                    CallState.RINGING,
                    CallState.ON_HOLD);
        }

        if (call != null) {
            if (call.getState() == CallState.RINGING) {
                call.reject(false /* rejectWithMessage */, null);
            } else {
                call.disconnect();
            }
            return true;
        }

        return false;
    }

    private void enforcePhoneAccountModificationForPackage(String packageName) {
        // TODO: Use a new telecomm permission for this instead of reusing modify.

        int result = mContext.checkCallingOrSelfPermission(MODIFY_PHONE_STATE);

        // Callers with MODIFY_PHONE_STATE can use the PhoneAccount mechanism to implement
        // built-in behavior even when PhoneAccounts are not exposed as a third-part API. They
        // may also modify PhoneAccounts on behalf of any 'packageName'.

        if (result != PackageManager.PERMISSION_GRANTED) {
            // Other callers are only allowed to modify PhoneAccounts if the relevant system
            // feature is enabled ...
            enforceConnectionServiceFeature();
            // ... and the PhoneAccounts they refer to are for their own package.
            enforceCallingPackage(packageName);
        }
    }

    private void enforcePermissionOrPrivilegedDialer(String permission, String packageName) {
        if (!isPrivilegedDialerCalling(packageName)) {
            try {
                enforcePermission(permission);
            } catch (SecurityException e) {
                Log.e(this, e, "Caller must be the default or system dialer, or have the permission"
                        + " %s to perform this operation.", permission);
                throw e;
            }
        }
    }

    private void enforceCallingPackage(String packageName) {
        mAppOpsManager.checkPackage(Binder.getCallingUid(), packageName);
    }

    private void enforceConnectionServiceFeature() {
        enforceFeature(PackageManager.FEATURE_CONNECTION_SERVICE);
    }

    private void enforceRegisterSimSubscriptionPermission() {
        enforcePermission(REGISTER_SIM_SUBSCRIPTION);
    }

    private void enforceModifyPermission() {
        enforcePermission(MODIFY_PHONE_STATE);
    }

    private void enforcePermission(String permission) {
        mContext.enforceCallingOrSelfPermission(permission, null);
    }

    private void enforceRegisterMultiUser() {
        if (!isCallerSystemApp()) {
            throw new SecurityException("CAPABILITY_MULTI_USER is only available to system apps.");
        }
    }

    private void enforceUserHandleMatchesCaller(PhoneAccountHandle accountHandle) {
        if (!Binder.getCallingUserHandle().equals(accountHandle.getUserHandle())) {
            throw new SecurityException("Calling UserHandle does not match PhoneAccountHandle's");
        }
    }

    private void enforceFeature(String feature) {
        PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(feature)) {
            throw new UnsupportedOperationException(
                    "System does not support feature " + feature);
        }
    }

    private boolean canReadPhoneState(String callingPackage, String message) {
        // The system/default dialer can always read phone state - so that emergency calls will
        // still work.
        if (isPrivilegedDialerCalling(callingPackage)) {
            return true;
        }

        try {
            mContext.enforceCallingOrSelfPermission(READ_PRIVILEGED_PHONE_STATE, message);
            // SKIP checking run-time OP_READ_PHONE_STATE since caller or self has PRIVILEGED
            // permission
            return true;
        } catch (SecurityException e) {
            // Accessing phone state is gated by a special permission.
            mContext.enforceCallingOrSelfPermission(READ_PHONE_STATE, message);

            // Some apps that have the permission can be restricted via app ops.
            return mAppOpsManager.noteOp(AppOpsManager.OP_READ_PHONE_STATE,
                    Binder.getCallingUid(), callingPackage) == AppOpsManager.MODE_ALLOWED;
        }
    }

    private boolean canCallPhone(String callingPackage, String message) {
        // The system/default dialer can always read phone state - so that emergency calls will
        // still work.
        if (isPrivilegedDialerCalling(callingPackage)) {
            return true;
        }

        // Accessing phone state is gated by a special permission.
        mContext.enforceCallingOrSelfPermission(CALL_PHONE, message);

        // Some apps that have the permission can be restricted via app ops.
        return mAppOpsManager.noteOp(AppOpsManager.OP_CALL_PHONE,
                Binder.getCallingUid(), callingPackage) == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isCallerSimCallManager() {
        PhoneAccountHandle accountHandle = null;
        long token = Binder.clearCallingIdentity();
        try {
            accountHandle = mPhoneAccountRegistrar.getSimCallManager();
        } finally {
            Binder.restoreCallingIdentity(token);
        }

        if (accountHandle != null) {
            try {
                mAppOpsManager.checkPackage(
                        Binder.getCallingUid(), accountHandle.getComponentName().getPackageName());
                return true;
            } catch (SecurityException e) {
            }
        }
        return false;
    }

    private boolean isPrivilegedDialerCalling(String callingPackage) {
        mAppOpsManager.checkPackage(Binder.getCallingUid(), callingPackage);
        return DefaultDialerManager.isDefaultOrSystemDialer(mContext, callingPackage);
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }
}
