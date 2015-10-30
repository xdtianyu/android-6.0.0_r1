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

import android.app.ActivityManager;
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telecom.ConnectionService;
import android.telecom.DefaultDialerManager;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.Xml;

// TODO: Needed for move to system service: import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Integer;
import java.lang.SecurityException;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles writing and reading PhoneAccountHandle registration entries. This is a simple verbatim
 * delegate for all the account handling methods on {@link android.telecom.TelecomManager} as
 * implemented in {@link TelecomServiceImpl}, with the notable exception that
 * {@link TelecomServiceImpl} is responsible for security checking to make sure that the caller has
 * proper authority over the {@code ComponentName}s they are declaring in their
 * {@code PhoneAccountHandle}s.
 *
 *
 *  -- About Users and Phone Accounts --
 *
 * We store all phone accounts for all users in a single place, which means that there are three
 * users that we have to deal with in code:
 * 1) The Android User that is currently active on the device.
 * 2) The user which owns/registers the phone account.
 * 3) The user running the app that is requesting the phone account information.
 *
 * For example, I have a device with 2 users, primary (A) and secondary (B), and the secondary user
 * has a work profile running as another user (B2). Lets say that user B opens the phone settings
 * (not currently supported, but theoretically speaking), and phone settings queries for a phone
 * account list. Lets also say that an app running in the work profile has registered a phone
 * account. This means that:
 *
 * Since phone settings always runs as the primary user, We have the following situation:
 * User A (settings) is requesting a list of phone accounts while the active user is User B, and
 * that list contains a phone account for profile User B2.
 *
 * In practice, (2) is stored with the phone account handle and is part of the handle's ID. (1) is
 * saved in {@link #mCurrentUserHandle} and (3) we get from Binder.getCallingUser(). We check these
 * users for visibility before returning any phone accounts.
 */
public final class PhoneAccountRegistrar {

    public static final PhoneAccountHandle NO_ACCOUNT_SELECTED =
            new PhoneAccountHandle(new ComponentName("null", "null"), "NO_ACCOUNT_SELECTED");

    public abstract static class Listener {
        public void onAccountsChanged(PhoneAccountRegistrar registrar) {}
        public void onDefaultOutgoingChanged(PhoneAccountRegistrar registrar) {}
        public void onSimCallManagerChanged(PhoneAccountRegistrar registrar) {}
    }

    private static final String FILE_NAME = "phone-account-registrar-state.xml";
    @VisibleForTesting
    public static final int EXPECTED_STATE_VERSION = 8;

    /** Keep in sync with the same in SipSettings.java */
    private static final String SIP_SHARED_PREFERENCES = "SIP_PREFERENCES";

    private final List<Listener> mListeners = new CopyOnWriteArrayList<>();
    private final AtomicFile mAtomicFile;
    private final Context mContext;
    private final UserManager mUserManager;
    private final SubscriptionManager mSubscriptionManager;
    private State mState;
    private UserHandle mCurrentUserHandle;

    @VisibleForTesting
    public PhoneAccountRegistrar(Context context) {
        this(context, FILE_NAME);
    }

    @VisibleForTesting
    public PhoneAccountRegistrar(Context context, String fileName) {
        // TODO: This file path is subject to change -- it is storing the phone account registry
        // state file in the path /data/system/users/0/, which is likely not correct in a
        // multi-user setting.
        /** UNCOMMENT_FOR_MOVE_TO_SYSTEM_SERVICE
        String filePath = Environment.getUserSystemDirectory(UserHandle.myUserId()).
                getAbsolutePath();
        mAtomicFile = new AtomicFile(new File(filePath, fileName));
         UNCOMMENT_FOR_MOVE_TO_SYSTEM_SERVICE */
        mAtomicFile = new AtomicFile(new File(context.getFilesDir(), fileName));

        mState = new State();
        mContext = context;
        mUserManager = UserManager.get(context);
        mSubscriptionManager = SubscriptionManager.from(mContext);
        mCurrentUserHandle = Process.myUserHandle();
        read();
    }

    /**
     * Retrieves the subscription id for a given phone account if it exists. Subscription ids
     * apply only to PSTN/SIM card phone accounts so all other accounts should not have a
     * subscription id.
     * @param accountHandle The handle for the phone account for which to retrieve the
     * subscription id.
     * @return The value of the subscription id or -1 if it does not exist or is not valid.
     */
    public int getSubscriptionIdForPhoneAccount(PhoneAccountHandle accountHandle) {
        PhoneAccount account = getPhoneAccountCheckCallingUser(accountHandle);

        if (account != null && account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
            TelephonyManager tm =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getSubIdForPhoneAccount(account);
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    /**
     * Retrieves the default outgoing phone account supporting the specified uriScheme. Note that if
     * {@link #mCurrentUserHandle} does not have visibility into the current default, {@code null}
     * will be returned.
     *
     * @param uriScheme The URI scheme for the outgoing call.
     * @return The {@link PhoneAccountHandle} to use.
     */
    public PhoneAccountHandle getOutgoingPhoneAccountForScheme(String uriScheme) {
        final PhoneAccountHandle userSelected = getUserSelectedOutgoingPhoneAccount();

        if (userSelected != null) {
            // If there is a default PhoneAccount, ensure it supports calls to handles with the
            // specified uriScheme.
            final PhoneAccount userSelectedAccount = getPhoneAccountCheckCallingUser(userSelected);
            if (userSelectedAccount.supportsUriScheme(uriScheme)) {
                return userSelected;
            }
        }

        List<PhoneAccountHandle> outgoing = getCallCapablePhoneAccounts(uriScheme, false);
        switch (outgoing.size()) {
            case 0:
                // There are no accounts, so there can be no default
                return null;
            case 1:
                // There is only one account, which is by definition the default.
                return outgoing.get(0);
            default:
                // There are multiple accounts with no selected default
                return null;
        }
    }

    /**
     * @return The user-selected outgoing {@link PhoneAccount}, or null if it hasn't been set (or
     *      if it was set by another user).
     */
    PhoneAccountHandle getUserSelectedOutgoingPhoneAccount() {
        PhoneAccount account = getPhoneAccountCheckCallingUser(mState.defaultOutgoing);
        if (account != null) {
            return mState.defaultOutgoing;
        }
        return null;
    }

    /**
     * Sets the phone account with which to place all calls by default. Set by the user
     * within phone settings.
     */
    public void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle accountHandle) {
        if (accountHandle == null) {
            // Asking to clear the default outgoing is a valid request
            mState.defaultOutgoing = null;
        } else {
            // TODO: Do we really want to return for *any* user?
            PhoneAccount account = getPhoneAccount(accountHandle);
            if (account == null) {
                Log.w(this, "Trying to set nonexistent default outgoing %s",
                        accountHandle);
                return;
            }

            if (!account.hasCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)) {
                Log.w(this, "Trying to set non-call-provider default outgoing %s",
                        accountHandle);
                return;
            }

            if (account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
                // If the account selected is a SIM account, propagate down to the subscription
                // record.
                int subId = getSubscriptionIdForPhoneAccount(accountHandle);
                mSubscriptionManager.setDefaultVoiceSubId(subId);
            }

            mState.defaultOutgoing = accountHandle;
        }

        write();
        fireDefaultOutgoingChanged();
    }

    boolean isUserSelectedSmsPhoneAccount(PhoneAccountHandle accountHandle) {
        return getSubscriptionIdForPhoneAccount(accountHandle) ==
                SubscriptionManager.getDefaultSmsSubId();
    }

    /**
     * Returns the {@link PhoneAccountHandle} corresponding to the currently active SIM Call
     * Manager. SIM Call Manager returned corresponds to the following priority order:
     * 1. If a SIM Call Manager {@link PhoneAccount} is registered for the same package as the
     * default dialer, then that one is returned.
     * 2. If there is a SIM Call Manager {@link PhoneAccount} registered which matches the
     * carrier configuration's default, then that one is returned.
     * 3. Otherwise, we return null.
     */
    public PhoneAccountHandle getSimCallManager() {
        long token = Binder.clearCallingIdentity();
        int user;
        try {
            user = ActivityManager.getCurrentUser();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        return getSimCallManager(user);
    }

    /**
     * Returns the {@link PhoneAccountHandle} corresponding to the currently active SIM Call
     * Manager. SIM Call Manager returned corresponds to the following priority order:
     * 1. If a SIM Call Manager {@link PhoneAccount} is registered for the same package as the
     * default dialer, then that one is returned.
     * 2. If there is a SIM Call Manager {@link PhoneAccount} registered which matches the
     * carrier configuration's default, then that one is returned.
     * 3. Otherwise, we return null.
     */
    public PhoneAccountHandle getSimCallManager(int user) {
        // Get the default dialer in case it has a connection manager associated with it.
        String dialerPackage = DefaultDialerManager.getDefaultDialerApplication(mContext, user);

        // Check carrier config.
        String defaultSimCallManager = null;
        CarrierConfigManager configManager = (CarrierConfigManager) mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle configBundle = configManager.getConfig();
        if (configBundle != null) {
            defaultSimCallManager = configBundle.getString(
                    CarrierConfigManager.KEY_DEFAULT_SIM_CALL_MANAGER_STRING);
        }

        ComponentName systemSimCallManagerComponent = TextUtils.isEmpty(defaultSimCallManager) ?
                null : ComponentName.unflattenFromString(defaultSimCallManager);

        PhoneAccountHandle dialerSimCallManager = null;
        PhoneAccountHandle systemSimCallManager = null;

        if (!TextUtils.isEmpty(dialerPackage) || systemSimCallManagerComponent != null) {
            // loop through and look for any connection manager in the same package.
            List<PhoneAccountHandle> allSimCallManagers = getPhoneAccountHandles(
                    PhoneAccount.CAPABILITY_CONNECTION_MANAGER, null, null,
                    true /* includeDisabledAccounts */);
            for (PhoneAccountHandle accountHandle : allSimCallManagers) {
                ComponentName component = accountHandle.getComponentName();

                // Store the system connection manager if found
                if (systemSimCallManager == null
                        && Objects.equals(component, systemSimCallManagerComponent)
                        && !resolveComponent(accountHandle).isEmpty()) {
                    systemSimCallManager = accountHandle;

                // Store the dialer connection manager if found
                } else if (dialerSimCallManager == null
                        && Objects.equals(component.getPackageName(), dialerPackage)
                        && !resolveComponent(accountHandle).isEmpty()) {
                    dialerSimCallManager = accountHandle;
                }
            }
        }

        PhoneAccountHandle retval = dialerSimCallManager != null ?
                dialerSimCallManager : systemSimCallManager;

        Log.i(this, "SimCallManager queried, returning: %s", retval);

        return retval;
    }

    /**
     * Update the current UserHandle to track when users are switched. This will allow the
     * PhoneAccountRegistar to self-filter the PhoneAccounts to make sure we don't leak anything
     * across users.
     * We cannot simply check the calling user because that would always return the primary user for
     * all invocations originating with the system process.
     *
     * @param userHandle The {@link UserHandle}, as delivered by
     *          {@link Intent#ACTION_USER_SWITCHED}.
     */
    public void setCurrentUserHandle(UserHandle userHandle) {
        if (userHandle == null) {
            Log.d(this, "setCurrentUserHandle, userHandle = null");
            userHandle = Process.myUserHandle();
        }
        Log.d(this, "setCurrentUserHandle, %s", userHandle);
        mCurrentUserHandle = userHandle;
    }

    /**
     * @return {@code true} if the phone account was successfully enabled/disabled, {@code false}
     *         otherwise.
     */
    public boolean enablePhoneAccount(PhoneAccountHandle accountHandle, boolean isEnabled) {
        PhoneAccount account = getPhoneAccount(accountHandle);
        if (account == null) {
            Log.w(this, "Could not find account to enable: " + accountHandle);
            return false;
        } else if (account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
            // We never change the enabled state of SIM-based accounts.
            Log.w(this, "Could not change enable state of SIM account: " + accountHandle);
            return false;
        }

        if (account.isEnabled() != isEnabled) {
            account.setIsEnabled(isEnabled);
            write();
            fireAccountsChanged();
        }
        return true;
    }

    private boolean isVisibleForUser(PhoneAccount account) {
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

        if (mCurrentUserHandle == null) {
            Log.d(this, "Current user is null; assuming true");
            return true;
        }

        if (phoneAccountUserHandle.equals(Binder.getCallingUserHandle())) {
            return true;
        }

        // Special check for work profiles.
        // Unlike in TelecomServiceImpl, we only care about *profiles* here. We want to make sure
        // that we don't resolve PhoneAccount across *users*, but resolving across *profiles* is
        // fine.
        if (UserHandle.getCallingUserId() == UserHandle.USER_OWNER) {
            List<UserInfo> profileUsers =
                    mUserManager.getProfiles(mCurrentUserHandle.getIdentifier());
            for (UserInfo profileInfo : profileUsers) {
                if (profileInfo.getUserHandle().equals(phoneAccountUserHandle)) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<ResolveInfo> resolveComponent(PhoneAccountHandle phoneAccountHandle) {
        return resolveComponent(phoneAccountHandle.getComponentName(),
                    phoneAccountHandle.getUserHandle());
    }

    private List<ResolveInfo> resolveComponent(ComponentName componentName,
            UserHandle userHandle) {
        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(ConnectionService.SERVICE_INTERFACE);
        intent.setComponent(componentName);
        try {
            if (userHandle != null) {
                return pm.queryIntentServicesAsUser(intent, 0, userHandle.getIdentifier());
            } else {
                return pm.queryIntentServices(intent, 0);
            }
        } catch (SecurityException e) {
            Log.e(this, e, "%s is not visible for the calling user", componentName);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Retrieves a list of all {@link PhoneAccountHandle}s registered.
     * Only returns accounts which are enabled.
     *
     * @return The list of {@link PhoneAccountHandle}s.
     */
    public List<PhoneAccountHandle> getAllPhoneAccountHandles() {
        return getPhoneAccountHandles(0, null, null, false);
    }

    public List<PhoneAccount> getAllPhoneAccounts() {
        return getPhoneAccounts(0, null, null, false);
    }

    /**
     * Retrieves a list of all phone account call provider phone accounts supporting the
     * specified URI scheme.
     *
     * @param uriScheme The URI scheme.
     * @return The phone account handles.
     */
    public List<PhoneAccountHandle> getCallCapablePhoneAccounts(
            String uriScheme, boolean includeDisabledAccounts) {
        return getPhoneAccountHandles(
                PhoneAccount.CAPABILITY_CALL_PROVIDER, uriScheme, null, includeDisabledAccounts);
    }

    /**
     * Retrieves a list of all the SIM-based phone accounts.
     */
    public List<PhoneAccountHandle> getSimPhoneAccounts() {
        return getPhoneAccountHandles(
                PhoneAccount.CAPABILITY_CALL_PROVIDER | PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION,
                null, null, false);
    }

    /**
     * Retrieves a list of all phone accounts registered by a specified package.
     *
     * @param packageName The name of the package that registered the phone accounts.
     * @return The phone account handles.
     */
    public List<PhoneAccountHandle> getPhoneAccountsForPackage(String packageName) {
        return getPhoneAccountHandles(0, null, packageName, false);
    }

    // TODO: Should we implement an artificial limit for # of accounts associated with a single
    // ComponentName?
    public void registerPhoneAccount(PhoneAccount account) {
        // Enforce the requirement that a connection service for a phone account has the correct
        // permission.
        if (!phoneAccountRequiresBindPermission(account.getAccountHandle())) {
            Log.w(this,
                    "Phone account %s does not have BIND_TELECOM_CONNECTION_SERVICE permission.",
                    account.getAccountHandle());
            throw new SecurityException("PhoneAccount connection service requires "
                    + "BIND_TELECOM_CONNECTION_SERVICE permission.");
        }

        addOrReplacePhoneAccount(account);
    }

    /**
     * Adds a {@code PhoneAccount}, replacing an existing one if found.
     *
     * @param account The {@code PhoneAccount} to add or replace.
     */
    private void addOrReplacePhoneAccount(PhoneAccount account) {
        Log.d(this, "addOrReplacePhoneAccount(%s -> %s)",
                account.getAccountHandle(), account);

        // Start _enabled_ property as false.
        // !!! IMPORTANT !!! It is important that we do not read the enabled state that the
        // source app provides or else an third party app could enable itself.
        boolean isEnabled = false;

        PhoneAccount oldAccount = getPhoneAccount(account.getAccountHandle());
        if (oldAccount != null) {
            mState.accounts.remove(oldAccount);
            isEnabled = oldAccount.isEnabled();
            Log.i(this, getAccountDiffString(account, oldAccount));
        } else {
            Log.i(this, "New phone account registered: " + account);
        }

        mState.accounts.add(account);
        // Reset enabled state to whatever the value was if the account was already registered,
        // or _true_ if this is a SIM-based account.  All SIM-based accounts are always enabled.
        account.setIsEnabled(
                isEnabled || account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION));

        write();
        fireAccountsChanged();
    }

    public void unregisterPhoneAccount(PhoneAccountHandle accountHandle) {
        PhoneAccount account = getPhoneAccount(accountHandle);
        if (account != null) {
            if (mState.accounts.remove(account)) {
                write();
                fireAccountsChanged();
            }
        }
    }

    /**
     * Un-registers all phone accounts associated with a specified package.
     *
     * @param packageName The package for which phone accounts will be removed.
     * @param userHandle The {@link UserHandle} the package is running under.
     */
    public void clearAccounts(String packageName, UserHandle userHandle) {
        boolean accountsRemoved = false;
        Iterator<PhoneAccount> it = mState.accounts.iterator();
        while (it.hasNext()) {
            PhoneAccount phoneAccount = it.next();
            PhoneAccountHandle handle = phoneAccount.getAccountHandle();
            if (Objects.equals(packageName, handle.getComponentName().getPackageName())
                    && Objects.equals(userHandle, handle.getUserHandle())) {
                Log.i(this, "Removing phone account " + phoneAccount.getLabel());
                mState.accounts.remove(phoneAccount);
                accountsRemoved = true;
            }
        }

        if (accountsRemoved) {
            write();
            fireAccountsChanged();
        }
    }

    public boolean isVoiceMailNumber(PhoneAccountHandle accountHandle, String number) {
        int subId = getSubscriptionIdForPhoneAccount(accountHandle);
        return PhoneNumberUtils.isVoiceMailNumber(mContext, subId, number);
    }

    public void addListener(Listener l) {
        mListeners.add(l);
    }

    public void removeListener(Listener l) {
        if (l != null) {
            mListeners.remove(l);
        }
    }

    private void fireAccountsChanged() {
        for (Listener l : mListeners) {
            l.onAccountsChanged(this);
        }
    }

    private void fireDefaultOutgoingChanged() {
        for (Listener l : mListeners) {
            l.onDefaultOutgoingChanged(this);
        }
    }

    private void fireSimCallManagerChanged() {
        for (Listener l : mListeners) {
            l.onSimCallManagerChanged(this);
        }
    }

    private String getAccountDiffString(PhoneAccount account1, PhoneAccount account2) {
        if (account1 == null || account2 == null) {
            return "Diff: " + account1 + ", " + account2;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("[").append(account1.getAccountHandle());
        appendDiff(sb, "addr", Log.piiHandle(account1.getAddress()),
                Log.piiHandle(account2.getAddress()));
        appendDiff(sb, "cap", account1.getCapabilities(), account2.getCapabilities());
        appendDiff(sb, "hl", account1.getHighlightColor(), account2.getHighlightColor());
        appendDiff(sb, "icon", account1.getIcon(), account2.getIcon());
        appendDiff(sb, "lbl", account1.getLabel(), account2.getLabel());
        appendDiff(sb, "desc", account1.getShortDescription(), account2.getShortDescription());
        appendDiff(sb, "subAddr", Log.piiHandle(account1.getSubscriptionAddress()),
                Log.piiHandle(account2.getSubscriptionAddress()));
        appendDiff(sb, "uris", account1.getSupportedUriSchemes(),
                account2.getSupportedUriSchemes());
        sb.append("]");
        return sb.toString();
    }

    private void appendDiff(StringBuffer sb, String attrName, Object obj1, Object obj2) {
        if (!Objects.equals(obj1, obj2)) {
            sb.append("(")
                .append(attrName)
                .append(": ")
                .append(obj1)
                .append(" -> ")
                .append(obj2)
                .append(")");
        }
    }

    /**
     * Determines if the connection service specified by a {@link PhoneAccountHandle} requires the
     * {@link Manifest.permission#BIND_TELECOM_CONNECTION_SERVICE} permission.
     *
     * @param phoneAccountHandle The phone account to check.
     * @return {@code True} if the phone account has permission.
     */
    public boolean phoneAccountRequiresBindPermission(PhoneAccountHandle phoneAccountHandle) {
        List<ResolveInfo> resolveInfos = resolveComponent(phoneAccountHandle);
        if (resolveInfos.isEmpty()) {
            Log.w(this, "phoneAccount %s not found", phoneAccountHandle.getComponentName());
            return false;
        }
        for (ResolveInfo resolveInfo : resolveInfos) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (serviceInfo == null) {
                return false;
            }

            if (!Manifest.permission.BIND_CONNECTION_SERVICE.equals(serviceInfo.permission) &&
                    !Manifest.permission.BIND_TELECOM_CONNECTION_SERVICE.equals(
                            serviceInfo.permission)) {
                // The ConnectionService must require either the deprecated BIND_CONNECTION_SERVICE,
                // or the public BIND_TELECOM_CONNECTION_SERVICE permissions, both of which are
                // system/signature only.
                return false;
            }
        }
        return true;
    }

    //
    // Methods for retrieving PhoneAccounts and PhoneAccountHandles
    //

    /**
     * Returns the PhoneAccount for the specified handle.  Does no user checking.
     *
     * @param handle
     * @return The corresponding phone account if one exists.
     */
    PhoneAccount getPhoneAccount(PhoneAccountHandle handle) {
        for (PhoneAccount m : mState.accounts) {
            if (Objects.equals(handle, m.getAccountHandle())) {
                return m;
            }
        }
        return null;
    }

    /**
     * Like getPhoneAccount, but checks to see if the current user is allowed to see the phone
     * account before returning it. The current user is the active user on the actual android
     * device.
     */
    public PhoneAccount getPhoneAccountCheckCallingUser(PhoneAccountHandle handle) {
        PhoneAccount account = getPhoneAccount(handle);
        if (account != null && isVisibleForUser(account)) {
            return account;
        }
        return null;
    }

    /**
     * Returns a list of phone account handles with the specified capabilities, uri scheme,
     * and package name.
     */
    private List<PhoneAccountHandle> getPhoneAccountHandles(
            int capabilities,
            String uriScheme,
            String packageName,
            boolean includeDisabledAccounts) {
        List<PhoneAccountHandle> handles = new ArrayList<>();

        for (PhoneAccount account : getPhoneAccounts(
                capabilities, uriScheme, packageName, includeDisabledAccounts)) {
            handles.add(account.getAccountHandle());
        }
        return handles;
    }

    /**
     * Returns a list of phone account handles with the specified flag, supporting the specified
     * URI scheme, within the specified package name.
     *
     * @param capabilities Capabilities which the {@code PhoneAccount} must have. Ignored if 0.
     * @param uriScheme URI schemes the PhoneAccount must handle.  {@code null} bypasses the
     *                  URI scheme check.
     * @param packageName Package name of the PhoneAccount. {@code null} bypasses packageName check.
     */
    private List<PhoneAccount> getPhoneAccounts(
            int capabilities,
            String uriScheme,
            String packageName,
            boolean includeDisabledAccounts) {
        List<PhoneAccount> accounts = new ArrayList<>(mState.accounts.size());
        for (PhoneAccount m : mState.accounts) {
            if (!(m.isEnabled() || includeDisabledAccounts)) {
                // Do not include disabled accounts.
                continue;
            }

            if (capabilities != 0 && !m.hasCapabilities(capabilities)) {
                // Account doesn't have the right capabilities; skip this one.
                continue;
            }
            if (uriScheme != null && !m.supportsUriScheme(uriScheme)) {
                // Account doesn't support this URI scheme; skip this one.
                continue;
            }
            PhoneAccountHandle handle = m.getAccountHandle();

            if (resolveComponent(handle).isEmpty()) {
                // This component cannot be resolved anymore; skip this one.
                continue;
            }
            if (packageName != null &&
                    !packageName.equals(handle.getComponentName().getPackageName())) {
                // Not the right package name; skip this one.
                continue;
            }
            if (!isVisibleForUser(m)) {
                // Account is not visible for the current user; skip this one.
                continue;
            }
            accounts.add(m);
        }
        return accounts;
    }

    //
    // State Implementation for PhoneAccountRegistrar
    //

    /**
     * The state of this {@code PhoneAccountRegistrar}.
     */
    @VisibleForTesting
    public static class State {
        /**
         * The account selected by the user to be employed by default for making outgoing calls.
         * If the user has not made such a selection, then this is null.
         */
        public PhoneAccountHandle defaultOutgoing = null;

        /**
         * The complete list of {@code PhoneAccount}s known to the Telecom subsystem.
         */
        public final List<PhoneAccount> accounts = new CopyOnWriteArrayList<>();

        /**
         * The version number of the State data.
         */
        public int versionNumber;
    }

    /**
     * Dumps the state of the {@link CallsManager}.
     *
     * @param pw The {@code IndentingPrintWriter} to write the state to.
     */
    public void dump(IndentingPrintWriter pw) {
        if (mState != null) {
            pw.println("xmlVersion: " + mState.versionNumber);
            pw.println("defaultOutgoing: " + (mState.defaultOutgoing == null ? "none" :
                    mState.defaultOutgoing));
            pw.println("simCallManager: " + getSimCallManager());
            pw.println("phoneAccounts:");
            pw.increaseIndent();
            for (PhoneAccount phoneAccount : mState.accounts) {
                pw.println(phoneAccount);
            }
            pw.decreaseIndent();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // State management
    //

    private void write() {
        final FileOutputStream os;
        try {
            os = mAtomicFile.startWrite();
            boolean success = false;
            try {
                XmlSerializer serializer = new FastXmlSerializer();
                serializer.setOutput(new BufferedOutputStream(os), "utf-8");
                writeToXml(mState, serializer, mContext);
                serializer.flush();
                success = true;
            } finally {
                if (success) {
                    mAtomicFile.finishWrite(os);
                } else {
                    mAtomicFile.failWrite(os);
                }
            }
        } catch (IOException e) {
            Log.e(this, e, "Writing state to XML file");
        }
    }

    private void read() {
        final InputStream is;
        try {
            is = mAtomicFile.openRead();
        } catch (FileNotFoundException ex) {
            return;
        }

        boolean versionChanged = false;

        XmlPullParser parser;
        try {
            parser = Xml.newPullParser();
            parser.setInput(new BufferedInputStream(is), null);
            parser.nextTag();
            mState = readFromXml(parser, mContext);
            versionChanged = mState.versionNumber < EXPECTED_STATE_VERSION;

        } catch (IOException | XmlPullParserException e) {
            Log.e(this, e, "Reading state from XML file");
            mState = new State();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(this, e, "Closing InputStream");
            }
        }

        // Verify all of the UserHandles.
        List<PhoneAccount> badAccounts = new ArrayList<>();
        for (PhoneAccount phoneAccount : mState.accounts) {
            UserHandle userHandle = phoneAccount.getAccountHandle().getUserHandle();
            if (userHandle == null) {
                Log.w(this, "Missing UserHandle for %s", phoneAccount);
                badAccounts.add(phoneAccount);
            } else if (mUserManager.getSerialNumberForUser(userHandle) == -1) {
                Log.w(this, "User does not exist for %s", phoneAccount);
                badAccounts.add(phoneAccount);
            }
        }
        mState.accounts.removeAll(badAccounts);

        // If an upgrade occurred, write out the changed data.
        if (versionChanged || !badAccounts.isEmpty()) {
            write();
        }
    }

    private static void writeToXml(State state, XmlSerializer serializer, Context context)
            throws IOException {
        sStateXml.writeToXml(state, serializer, context);
    }

    private static State readFromXml(XmlPullParser parser, Context context)
            throws IOException, XmlPullParserException {
        State s = sStateXml.readFromXml(parser, 0, context);
        return s != null ? s : new State();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // XML serialization
    //

    @VisibleForTesting
    public abstract static class XmlSerialization<T> {
        private static final String LENGTH_ATTRIBUTE = "length";
        private static final String VALUE_TAG = "value";

        /**
         * Write the supplied object to XML
         */
        public abstract void writeToXml(T o, XmlSerializer serializer, Context context)
                throws IOException;

        /**
         * Read from the supplied XML into a new object, returning null in case of an
         * unrecoverable schema mismatch or other data error. 'parser' must be already
         * positioned at the first tag that is expected to have been emitted by this
         * object's writeToXml(). This object tries to fail early without modifying
         * 'parser' if it does not recognize the data it sees.
         */
        public abstract T readFromXml(XmlPullParser parser, int version, Context context)
                throws IOException, XmlPullParserException;

        protected void writeTextIfNonNull(String tagName, Object value, XmlSerializer serializer)
                throws IOException {
            if (value != null) {
                serializer.startTag(null, tagName);
                serializer.text(Objects.toString(value));
                serializer.endTag(null, tagName);
            }
        }

        /**
         * Serializes a string array.
         *
         * @param tagName The tag name for the string array.
         * @param values The string values to serialize.
         * @param serializer The serializer.
         * @throws IOException
         */
        protected void writeStringList(String tagName, List<String> values,
                XmlSerializer serializer)
                throws IOException {

            serializer.startTag(null, tagName);
            if (values != null) {
                serializer.attribute(null, LENGTH_ATTRIBUTE, Objects.toString(values.size()));
                for (String toSerialize : values) {
                    serializer.startTag(null, VALUE_TAG);
                    if (toSerialize != null ){
                        serializer.text(toSerialize);
                    }
                    serializer.endTag(null, VALUE_TAG);
                }
            } else {
                serializer.attribute(null, LENGTH_ATTRIBUTE, "0");
            }
            serializer.endTag(null, tagName);
        }

        protected void writeIconIfNonNull(String tagName, Icon value, XmlSerializer serializer)
                throws IOException {
            if (value != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                value.writeToStream(stream);
                byte[] iconByteArray = stream.toByteArray();
                String text = Base64.encodeToString(iconByteArray, 0, iconByteArray.length, 0);

                serializer.startTag(null, tagName);
                serializer.text(text);
                serializer.endTag(null, tagName);
            }
        }

        protected void writeLong(String tagName, long value, XmlSerializer serializer)
                throws IOException {
            serializer.startTag(null, tagName);
            serializer.text(Long.valueOf(value).toString());
            serializer.endTag(null, tagName);
        }

        /**
         * Reads a string array from the XML parser.
         *
         * @param parser The XML parser.
         * @return String array containing the parsed values.
         * @throws IOException Exception related to IO.
         * @throws XmlPullParserException Exception related to parsing.
         */
        protected List<String> readStringList(XmlPullParser parser)
                throws IOException, XmlPullParserException {

            int length = Integer.parseInt(parser.getAttributeValue(null, LENGTH_ATTRIBUTE));
            List<String> arrayEntries = new ArrayList<String>(length);
            String value = null;

            if (length == 0) {
                return arrayEntries;
            }

            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (parser.getName().equals(VALUE_TAG)) {
                    parser.next();
                    value = parser.getText();
                    arrayEntries.add(value);
                }
            }

            return arrayEntries;
        }

        protected Bitmap readBitmap(XmlPullParser parser) {
            byte[] imageByteArray = Base64.decode(parser.getText(), 0);
            return BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
        }

        protected Icon readIcon(XmlPullParser parser) throws IOException {
            byte[] iconByteArray = Base64.decode(parser.getText(), 0);
            ByteArrayInputStream stream = new ByteArrayInputStream(iconByteArray);
            return Icon.createFromStream(stream);
        }
    }

    @VisibleForTesting
    public static final XmlSerialization<State> sStateXml =
            new XmlSerialization<State>() {
        private static final String CLASS_STATE = "phone_account_registrar_state";
        private static final String DEFAULT_OUTGOING = "default_outgoing";
        private static final String ACCOUNTS = "accounts";
        private static final String VERSION = "version";

        @Override
        public void writeToXml(State o, XmlSerializer serializer, Context context)
                throws IOException {
            if (o != null) {
                serializer.startTag(null, CLASS_STATE);
                serializer.attribute(null, VERSION, Objects.toString(EXPECTED_STATE_VERSION));

                if (o.defaultOutgoing != null) {
                    serializer.startTag(null, DEFAULT_OUTGOING);
                    sPhoneAccountHandleXml.writeToXml(o.defaultOutgoing, serializer, context);
                    serializer.endTag(null, DEFAULT_OUTGOING);
                }

                serializer.startTag(null, ACCOUNTS);
                for (PhoneAccount m : o.accounts) {
                    sPhoneAccountXml.writeToXml(m, serializer, context);
                }
                serializer.endTag(null, ACCOUNTS);

                serializer.endTag(null, CLASS_STATE);
            }
        }

        @Override
        public State readFromXml(XmlPullParser parser, int version, Context context)
                throws IOException, XmlPullParserException {
            if (parser.getName().equals(CLASS_STATE)) {
                State s = new State();

                String rawVersion = parser.getAttributeValue(null, VERSION);
                s.versionNumber = TextUtils.isEmpty(rawVersion) ? 1 :
                        Integer.parseInt(rawVersion);

                int outerDepth = parser.getDepth();
                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                    if (parser.getName().equals(DEFAULT_OUTGOING)) {
                        parser.nextTag();
                        s.defaultOutgoing = sPhoneAccountHandleXml.readFromXml(parser,
                                s.versionNumber, context);
                    } else if (parser.getName().equals(ACCOUNTS)) {
                        int accountsDepth = parser.getDepth();
                        while (XmlUtils.nextElementWithin(parser, accountsDepth)) {
                            PhoneAccount account = sPhoneAccountXml.readFromXml(parser,
                                    s.versionNumber, context);

                            if (account != null && s.accounts != null) {
                                s.accounts.add(account);
                            }
                        }
                    }
                }
                return s;
            }
            return null;
        }
    };

    @VisibleForTesting
    public static final XmlSerialization<PhoneAccount> sPhoneAccountXml =
            new XmlSerialization<PhoneAccount>() {
        private static final String CLASS_PHONE_ACCOUNT = "phone_account";
        private static final String ACCOUNT_HANDLE = "account_handle";
        private static final String ADDRESS = "handle";
        private static final String SUBSCRIPTION_ADDRESS = "subscription_number";
        private static final String CAPABILITIES = "capabilities";
        private static final String ICON_RES_ID = "icon_res_id";
        private static final String ICON_PACKAGE_NAME = "icon_package_name";
        private static final String ICON_BITMAP = "icon_bitmap";
        private static final String ICON_TINT = "icon_tint";
        private static final String HIGHLIGHT_COLOR = "highlight_color";
        private static final String LABEL = "label";
        private static final String SHORT_DESCRIPTION = "short_description";
        private static final String SUPPORTED_URI_SCHEMES = "supported_uri_schemes";
        private static final String ICON = "icon";
        private static final String ENABLED = "enabled";

        @Override
        public void writeToXml(PhoneAccount o, XmlSerializer serializer, Context context)
                throws IOException {
            if (o != null) {
                serializer.startTag(null, CLASS_PHONE_ACCOUNT);

                if (o.getAccountHandle() != null) {
                    serializer.startTag(null, ACCOUNT_HANDLE);
                    sPhoneAccountHandleXml.writeToXml(o.getAccountHandle(), serializer, context);
                    serializer.endTag(null, ACCOUNT_HANDLE);
                }

                writeTextIfNonNull(ADDRESS, o.getAddress(), serializer);
                writeTextIfNonNull(SUBSCRIPTION_ADDRESS, o.getSubscriptionAddress(), serializer);
                writeTextIfNonNull(CAPABILITIES, Integer.toString(o.getCapabilities()), serializer);
                writeIconIfNonNull(ICON, o.getIcon(), serializer);
                writeTextIfNonNull(HIGHLIGHT_COLOR,
                        Integer.toString(o.getHighlightColor()), serializer);
                writeTextIfNonNull(LABEL, o.getLabel(), serializer);
                writeTextIfNonNull(SHORT_DESCRIPTION, o.getShortDescription(), serializer);
                writeStringList(SUPPORTED_URI_SCHEMES, o.getSupportedUriSchemes(), serializer);
                writeTextIfNonNull(ENABLED, o.isEnabled() ? "true" : "false" , serializer);

                serializer.endTag(null, CLASS_PHONE_ACCOUNT);
            }
        }

        public PhoneAccount readFromXml(XmlPullParser parser, int version, Context context)
                throws IOException, XmlPullParserException {
            if (parser.getName().equals(CLASS_PHONE_ACCOUNT)) {
                int outerDepth = parser.getDepth();
                PhoneAccountHandle accountHandle = null;
                Uri address = null;
                Uri subscriptionAddress = null;
                int capabilities = 0;
                int iconResId = PhoneAccount.NO_RESOURCE_ID;
                String iconPackageName = null;
                Bitmap iconBitmap = null;
                int iconTint = PhoneAccount.NO_ICON_TINT;
                int highlightColor = PhoneAccount.NO_HIGHLIGHT_COLOR;
                String label = null;
                String shortDescription = null;
                List<String> supportedUriSchemes = null;
                Icon icon = null;
                boolean enabled = false;

                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                    if (parser.getName().equals(ACCOUNT_HANDLE)) {
                        parser.nextTag();
                        accountHandle = sPhoneAccountHandleXml.readFromXml(parser, version,
                                context);
                    } else if (parser.getName().equals(ADDRESS)) {
                        parser.next();
                        address = Uri.parse(parser.getText());
                    } else if (parser.getName().equals(SUBSCRIPTION_ADDRESS)) {
                        parser.next();
                        String nextText = parser.getText();
                        subscriptionAddress = nextText == null ? null : Uri.parse(nextText);
                    } else if (parser.getName().equals(CAPABILITIES)) {
                        parser.next();
                        capabilities = Integer.parseInt(parser.getText());
                    } else if (parser.getName().equals(ICON_RES_ID)) {
                        parser.next();
                        iconResId = Integer.parseInt(parser.getText());
                    } else if (parser.getName().equals(ICON_PACKAGE_NAME)) {
                        parser.next();
                        iconPackageName = parser.getText();
                    } else if (parser.getName().equals(ICON_BITMAP)) {
                        parser.next();
                        iconBitmap = readBitmap(parser);
                    } else if (parser.getName().equals(ICON_TINT)) {
                        parser.next();
                        iconTint = Integer.parseInt(parser.getText());
                    } else if (parser.getName().equals(HIGHLIGHT_COLOR)) {
                        parser.next();
                        highlightColor = Integer.parseInt(parser.getText());
                    } else if (parser.getName().equals(LABEL)) {
                        parser.next();
                        label = parser.getText();
                    } else if (parser.getName().equals(SHORT_DESCRIPTION)) {
                        parser.next();
                        shortDescription = parser.getText();
                    } else if (parser.getName().equals(SUPPORTED_URI_SCHEMES)) {
                        supportedUriSchemes = readStringList(parser);
                    } else if (parser.getName().equals(ICON)) {
                        parser.next();
                        icon = readIcon(parser);
                    } else if (parser.getName().equals(ENABLED)) {
                        parser.next();
                        enabled = "true".equalsIgnoreCase(parser.getText());
                    }
                }

                ComponentName pstnComponentName = new ComponentName("com.android.phone",
                        "com.android.services.telephony.TelephonyConnectionService");
                ComponentName sipComponentName = new ComponentName("com.android.phone",
                        "com.android.services.telephony.sip.SipConnectionService");

                // Upgrade older phone accounts to specify the supported URI schemes.
                if (version < 2) {
                    supportedUriSchemes = new ArrayList<>();

                    // Handle the SIP connection service.
                    // Check the system settings to see if it also should handle "tel" calls.
                    if (accountHandle.getComponentName().equals(sipComponentName)) {
                        boolean useSipForPstn = useSipForPstnCalls(context);
                        supportedUriSchemes.add(PhoneAccount.SCHEME_SIP);
                        if (useSipForPstn) {
                            supportedUriSchemes.add(PhoneAccount.SCHEME_TEL);
                        }
                    } else {
                        supportedUriSchemes.add(PhoneAccount.SCHEME_TEL);
                        supportedUriSchemes.add(PhoneAccount.SCHEME_VOICEMAIL);
                    }
                }

                // Upgrade older phone accounts with explicit package name
                if (version < 5) {
                    if (iconBitmap == null) {
                        iconPackageName = accountHandle.getComponentName().getPackageName();
                    }
                }

                if (version < 6) {
                    // Always enable all SIP accounts on upgrade to version 6
                    if (accountHandle.getComponentName().equals(sipComponentName)) {
                        enabled = true;
                    }
                }
                if (version < 7) {
                    // Always enabled all PSTN acocunts on upgrade to version 7
                    if (accountHandle.getComponentName().equals(pstnComponentName)) {
                        enabled = true;
                    }
                }
                if (version < 8) {
                    // Migrate the SIP account handle ids to use SIP username instead of SIP URI.
                    if (accountHandle.getComponentName().equals(sipComponentName)) {
                        Uri accountUri = Uri.parse(accountHandle.getId());
                        if (accountUri.getScheme() != null &&
                            accountUri.getScheme().equals(PhoneAccount.SCHEME_SIP)) {
                            accountHandle = new PhoneAccountHandle(accountHandle.getComponentName(),
                                    accountUri.getSchemeSpecificPart(),
                                    accountHandle.getUserHandle());
                        }
                    }
                }

                PhoneAccount.Builder builder = PhoneAccount.builder(accountHandle, label)
                        .setAddress(address)
                        .setSubscriptionAddress(subscriptionAddress)
                        .setCapabilities(capabilities)
                        .setShortDescription(shortDescription)
                        .setSupportedUriSchemes(supportedUriSchemes)
                        .setHighlightColor(highlightColor)
                        .setIsEnabled(enabled);

                if (icon != null) {
                    builder.setIcon(icon);
                } else if (iconBitmap != null) {
                    builder.setIcon(Icon.createWithBitmap(iconBitmap));
                } else if (!TextUtils.isEmpty(iconPackageName)) {
                    builder.setIcon(Icon.createWithResource(iconPackageName, iconResId));
                    // TODO: Need to set tint.
                }

                return builder.build();
            }
            return null;
        }

        /**
         * Determines if the SIP call settings specify to use SIP for all calls, including PSTN
         * calls.
         *
         * @param context The context.
         * @return {@code True} if SIP should be used for all calls.
         */
        private boolean useSipForPstnCalls(Context context) {
            String option = Settings.System.getString(context.getContentResolver(),
                    Settings.System.SIP_CALL_OPTIONS);
            option = (option != null) ? option : Settings.System.SIP_ADDRESS_ONLY;
            return option.equals(Settings.System.SIP_ALWAYS);
        }
    };

    @VisibleForTesting
    public static final XmlSerialization<PhoneAccountHandle> sPhoneAccountHandleXml =
            new XmlSerialization<PhoneAccountHandle>() {
        private static final String CLASS_PHONE_ACCOUNT_HANDLE = "phone_account_handle";
        private static final String COMPONENT_NAME = "component_name";
        private static final String ID = "id";
        private static final String USER_SERIAL_NUMBER = "user_serial_number";

        @Override
        public void writeToXml(PhoneAccountHandle o, XmlSerializer serializer, Context context)
                throws IOException {
            if (o != null) {
                serializer.startTag(null, CLASS_PHONE_ACCOUNT_HANDLE);

                if (o.getComponentName() != null) {
                    writeTextIfNonNull(
                            COMPONENT_NAME, o.getComponentName().flattenToString(), serializer);
                }

                writeTextIfNonNull(ID, o.getId(), serializer);

                if (o.getUserHandle() != null && context != null) {
                    UserManager userManager = UserManager.get(context);
                    writeLong(USER_SERIAL_NUMBER,
                            userManager.getSerialNumberForUser(o.getUserHandle()), serializer);
                }

                serializer.endTag(null, CLASS_PHONE_ACCOUNT_HANDLE);
            }
        }

        @Override
        public PhoneAccountHandle readFromXml(XmlPullParser parser, int version, Context context)
                throws IOException, XmlPullParserException {
            if (parser.getName().equals(CLASS_PHONE_ACCOUNT_HANDLE)) {
                String componentNameString = null;
                String idString = null;
                String userSerialNumberString = null;
                int outerDepth = parser.getDepth();

                UserManager userManager = UserManager.get(context);

                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                    if (parser.getName().equals(COMPONENT_NAME)) {
                        parser.next();
                        componentNameString = parser.getText();
                    } else if (parser.getName().equals(ID)) {
                        parser.next();
                        idString = parser.getText();
                    } else if (parser.getName().equals(USER_SERIAL_NUMBER)) {
                        parser.next();
                        userSerialNumberString = parser.getText();
                    }
                }
                if (componentNameString != null) {
                    UserHandle userHandle = null;
                    if (userSerialNumberString != null) {
                        try {
                            long serialNumber = Long.parseLong(userSerialNumberString);
                            userHandle = userManager.getUserForSerialNumber(serialNumber);
                        } catch (NumberFormatException e) {
                            Log.e(this, e, "Could not parse UserHandle " + userSerialNumberString);
                        }
                    }
                    return new PhoneAccountHandle(
                            ComponentName.unflattenFromString(componentNameString),
                            idString,
                            userHandle);
                }
            }
            return null;
        }
    };
}
