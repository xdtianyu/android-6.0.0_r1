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

package com.android.exchange.eas;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.CommandStatusException;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.service.EasServerConnection;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogUtils;
import com.google.common.annotations.VisibleForTesting;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * Base class for all Exchange operations that use a POST to talk to the server.
 *
 * The core of this class is {@link #performOperation}, which provides the skeleton of making
 * a request, handling common errors, and setting fields on the {@link SyncResult} if there is one.
 * This class abstracts the connection handling from its subclasses and callers.
 *
 * {@link #performOperation} calls various abstract functions to create the request and parse the
 * response. For the most part subclasses can implement just these bits of functionality and rely
 * on {@link #performOperation} to do all the boilerplate etc.
 *
 * There are also a set of functions that a subclass may override if it's substantially
 * different from the "normal" operation (e.g. autodiscover deviates from the standard URI since
 * it's not account-specific so it needs to override {@link #getRequestUri()}), but the default
 * implementations of these functions should suffice for most operations.
 *
 * Some subclasses may need to override {@link #performOperation} to add validation and results
 * processing around a call to super.performOperation. Subclasses should avoid doing too much more
 * than wrapping some handling around the chained call; if you find that's happening, it's likely
 * a sign that the base class needs to be enhanced.
 *
 * One notable reason this wrapping happens is for operations that need to return a result directly
 * to their callers (as opposed to simply writing the results to the provider, as is common with
 * sync operations). This happens for example in
 * {@link com.android.emailcommon.service.IEmailService} message handlers. In such cases, due to
 * how {@link com.android.exchange.service.EasService} uses this class, the subclass needs to
 * store the result as a member variable and then provide an accessor to read the result. Since
 * different operations have different results (or none at all), there is no function in the base
 * class for this.
 *
 * Note that it is not practical to avoid the race between when an operation loads its account data
 * and when it uses it, as that would require some form of locking in the provider. There are three
 * interesting situations where this might happen, and that this class must handle:
 *
 * 1) Deleted from provider: Any subsequent provider access should return an error. Operations
 *    must detect this and terminate with an error.
 * 2) Account sync settings change: Generally only affects Ping. We interrupt the operation and
 *    load the new settings before proceeding.
 * 3) Sync suspended due to hold: A special case of the previous, and affects all operations, but
 *    fortunately doesn't need special handling here. Correct provider functionality must generate
 *    write failures, so the handling for #1 should cover this case as well.
 *
 * This class attempts to defer loading of account data as long as possible -- ideally we load
 * immediately before the network request -- but does not proactively check for changes after that.
 * This approach is a a practical balance between minimizing the race without adding too much
 * complexity beyond what's required.
 */
public abstract class EasOperation {
    public static final String LOG_TAG = LogUtils.TAG;

    /** The maximum number of server redirects we allow before returning failure. */
    private static final int MAX_REDIRECTS = 3;

    /** Message MIME type for EAS version 14 and later. */
    private static final String EAS_14_MIME_TYPE = "application/vnd.ms-sync.wbxml";

    /**
     * EasOperation error codes below.  All subclasses should try to create error codes
     * that do not overlap these codes or the codes of other subclasses. The error
     * code values for each subclass should start in a different 100 range (i.e. -100,
     * -200, etc...).
     */

    /** Minimum value for any non failure result. There may be multiple different non-failure
     * results, if so they should all be greater than or equal to this value. */
    public static final int RESULT_MIN_OK_RESULT = 0;
    /** Error code indicating the operation was cancelled via {@link #abort}. */
    public static final int RESULT_ABORT = -1;
    /** Error code indicating the operation was cancelled via {@link #restart}. */
    public static final int RESULT_RESTART = -2;
    /** Error code indicating the Exchange servers redirected too many times. */
    public static final int RESULT_TOO_MANY_REDIRECTS = -3;
    /** Error code indicating the request failed due to a network problem. */
    public static final int RESULT_NETWORK_PROBLEM = -4;
    /** Error code indicating a 403 (forbidden) error. */
    public static final int RESULT_FORBIDDEN = -5;
    /** Error code indicating an unresolved provisioning error. */
    public static final int RESULT_PROVISIONING_ERROR = -6;
    /** Error code indicating an authentication problem. */
    public static final int RESULT_AUTHENTICATION_ERROR = -7;
    /** Error code indicating the client is missing a certificate. */
    public static final int RESULT_CLIENT_CERTIFICATE_REQUIRED = -8;
    /** Error code indicating we don't have a protocol version in common with the server. */
    public static final int RESULT_PROTOCOL_VERSION_UNSUPPORTED = -9;
    /** Error code indicating a hard error when initializing the operation. */
    public static final int RESULT_INITIALIZATION_FAILURE = -10;
    /** Error code indicating a hard data layer error. */
    public static final int RESULT_HARD_DATA_FAILURE = -11;
    /** Error code indicating that this operation failed, but we should not abort the sync */
    /** TODO: This is currently only used in EasOutboxSync, no other place handles it correctly */
    public static final int RESULT_NON_FATAL_ERROR = -12;
    /** Error code indicating some other failure. */
    public static final int RESULT_OTHER_FAILURE = -99;
    /** Constant to delimit where op specific error codes begin. */
    public static final int RESULT_OP_SPECIFIC_ERROR_RESULT = -100;

    protected final Context mContext;

    /** The cached {@link Account} state; can be null if it hasn't been loaded yet. */
    protected final Account mAccount;

    /** The connection to use for this operation. This is created when {@link #mAccount} is set. */
    protected EasServerConnection mConnection;

    public class MessageInvalidException extends Exception {
        public MessageInvalidException(final String message) {
            super(message);
        }
    }

    public static boolean isFatal(int result) {
        return result < RESULT_MIN_OK_RESULT;
    }

    protected EasOperation(final Context context, @NonNull final Account account,
            final EasServerConnection connection) {
        mContext = context;
        mAccount = account;
        mConnection = connection;
        if (account == null) {
            throw new IllegalStateException("Null account in EasOperation");
        }
    }

    protected EasOperation(final Context context, final Account account, final HostAuth hostAuth) {
        this(context, account, new EasServerConnection(context, account, hostAuth));
    }

    protected EasOperation(final Context context, final Account account) {
        this(context, account, account.getOrCreateHostAuthRecv(context));
    }

    /**
     * This constructor is for use by operations that are created by other operations, e.g.
     * {@link EasProvision}. It reuses the account and connection of its parent.
     * @param parentOperation The {@link EasOperation} that is creating us.
     */
    protected EasOperation(final EasOperation parentOperation) {
        mContext = parentOperation.mContext;
        mAccount = parentOperation.mAccount;
        mConnection = parentOperation.mConnection;
    }

    /**
     * This will always be called at the begining of performOperation and can be overridden
     * to do whatever setup is needed.
     * @return true if initialization succeeded, false otherwise.
     */
    public boolean init() {
        return true;
    }

    public final long getAccountId() {
        return mAccount.getId();
    }

    public final Account getAccount() {
        return mAccount;
    }

    /**
     * Request that this operation terminate. Intended for use by the sync service to interrupt
     * running operations, primarily Ping.
     */
    public final void abort() {
        mConnection.stop(EasServerConnection.STOPPED_REASON_ABORT);
    }

    /**
     * Request that this operation restart. Intended for use by the sync service to interrupt
     * running operations, primarily Ping.
     */
    public final void restart() {
        mConnection.stop(EasServerConnection.STOPPED_REASON_RESTART);
    }

    /**
     * Should return true if the last operation encountered an error. Default implementation
     * always returns false, child classes can override.
     */
    public final boolean lastSyncHadError() { return false; }

    /**
     * The skeleton of performing an operation. This function handles all the common code and
     * error handling, calling into virtual functions that are implemented or overridden by the
     * subclass to do the operation-specific logic.
     *
     * The result codes work as follows:
     * - Negative values indicate common error codes and are defined above (the various RESULT_*
     *   constants).
     * - Non-negative values indicate the result of {@link #handleResponse}. These are obviously
     *   specific to the subclass, and may indicate success or error conditions.
     *
     * The common error codes primarily indicate conditions that occur when performing the POST
     * itself, such as network errors and handling of the HTTP response. However, some errors that
     * can be indicated in the HTTP response code can also be indicated in the payload of the
     * response as well, so {@link #handleResponse} should in those cases return the appropriate
     * negative result code, which will be handled the same as if it had been indicated in the HTTP
     * response code.
     *
     * @return A result code for the outcome of this operation, as described above.
     */
    public int performOperation() {
        if (!init()) {
            LogUtils.i(LOG_TAG, "Failed to initialize %d before sending request for operation %s",
                    getAccountId(), getCommand());
            return RESULT_INITIALIZATION_FAILURE;
        }
        try {
            return performOperationInternal();
        } finally {
            onRequestComplete();
        }
    }

    private int performOperationInternal() {
        // We handle server redirects by looping, but we need to protect against too much looping.
        int redirectCount = 0;

        do {
            // Perform the HTTP request and handle exceptions.
            final EasResponse response;
            try {
                try {
                    response = mConnection.executeHttpUriRequest(makeRequest(), getTimeout());
                } finally {
                    onRequestMade();
                }
            } catch (final IOException e) {
                // If we were stopped, return the appropriate result code.
                switch (mConnection.getStoppedReason()) {
                    case EasServerConnection.STOPPED_REASON_ABORT:
                        return RESULT_ABORT;
                    case EasServerConnection.STOPPED_REASON_RESTART:
                        return RESULT_RESTART;
                    default:
                        break;
                }
                // If we're here, then we had a IOException that's not from a stop request.
                String message = e.getMessage();
                if (message == null) {
                    message = "(no message)";
                }
                LogUtils.i(LOG_TAG, "IOException while sending request: %s", message);
                return RESULT_NETWORK_PROBLEM;
            } catch (final CertificateException e) {
                LogUtils.i(LOG_TAG, "CertificateException while sending request: %s",
                        e.getMessage());
                return RESULT_CLIENT_CERTIFICATE_REQUIRED;
            } catch (final MessageInvalidException e) {
                // This indicates that there is something wrong with the message locally, and it
                // cannot be sent. We don't want to return success, because that's misleading,
                // but on the other hand, we don't want to abort the sync, because that would
                // prevent other messages from being sent.
                LogUtils.d(LOG_TAG, "Exception sending request %s", e.getMessage());
                return RESULT_NON_FATAL_ERROR;
            } catch (final IllegalStateException e) {
                // Subclasses use ISE to signal a hard error when building the request.
                // TODO: Switch away from ISEs.
                LogUtils.e(LOG_TAG, e, "Exception while sending request");
                return RESULT_HARD_DATA_FAILURE;
            }

            // The POST completed, so process the response.
            try {
                final int result;
                // First off, the success case.
                if (response.isSuccess()) {
                    int responseResult;
                    try {
                        responseResult = handleResponse(response);
                    } catch (final IOException e) {
                        LogUtils.e(LOG_TAG, e, "Exception while handling response");
                        return RESULT_NETWORK_PROBLEM;
                    } catch (final CommandStatusException e) {
                        // For some operations (notably Sync & FolderSync), errors are signaled in
                        // the payload of the response. These will have a HTTP 200 response, and the
                        // error condition is only detected during response parsing.
                        // The various parsers handle this by throwing a CommandStatusException.
                        // TODO: Consider having the parsers return the errors instead of throwing.
                        final int status = e.mStatus;
                        LogUtils.e(LOG_TAG, "CommandStatusException: %s, %d", getCommand(), status);
                        if (CommandStatusException.CommandStatus.isNeedsProvisioning(status)) {
                            responseResult = RESULT_PROVISIONING_ERROR;
                        } else if (CommandStatusException.CommandStatus.isDeniedAccess(status)) {
                            responseResult = RESULT_FORBIDDEN;
                        } else {
                            responseResult = RESULT_OTHER_FAILURE;
                        }
                    }
                    result = responseResult;
                } else {
                    result = handleHttpError(response.getStatus());
                }

                // Non-negative results indicate success. Return immediately and bypass the error
                // handling.
                if (result >= EasOperation.RESULT_MIN_OK_RESULT) {
                    return result;
                }

                // If this operation has distinct handling for 403 errors, do that.
                if (result == RESULT_FORBIDDEN || (response.isForbidden() && handleForbidden())) {
                    LogUtils.e(LOG_TAG, "Forbidden response");
                    return RESULT_FORBIDDEN;
                }

                // Handle provisioning errors.
                if (result == RESULT_PROVISIONING_ERROR || response.isProvisionError()) {
                    if (handleProvisionError()) {
                        // The provisioning error has been taken care of, so we should re-do this
                        // request.
                        LogUtils.d(LOG_TAG, "Provisioning error handled during %s, retrying",
                                getCommand());
                        continue;
                    }
                    return RESULT_PROVISIONING_ERROR;
                }

                // Handle authentication errors.
                if (response.isAuthError()) {
                    LogUtils.e(LOG_TAG, "Authentication error");
                    if (response.isMissingCertificate()) {
                        return RESULT_CLIENT_CERTIFICATE_REQUIRED;
                    }
                    return RESULT_AUTHENTICATION_ERROR;
                }

                // Handle redirects.
                if (response.isRedirectError()) {
                    ++redirectCount;
                    mConnection.redirectHostAuth(response.getRedirectAddress());
                    // Note that unlike other errors, we do NOT return here; we just keep looping.
                } else {
                    // All other errors.
                    LogUtils.e(LOG_TAG, "Generic error for operation %s: status %d, result %d",
                            getCommand(), response.getStatus(), result);
                    // TODO: This probably should return result.
                    return RESULT_OTHER_FAILURE;
                }
            } finally {
                response.close();
            }
        } while (redirectCount < MAX_REDIRECTS);

        // Non-redirects return immediately after handling, so the only way to reach here is if we
        // looped too many times.
        LogUtils.e(LOG_TAG, "Too many redirects");
        return RESULT_TOO_MANY_REDIRECTS;
    }

    protected void onRequestMade() {
        // This can be overridden to do any cleanup that must happen after the request has
        // been sent. It will always be called, regardless of the status of the request.
    }

    protected void onRequestComplete() {
        // This can be overridden to do any cleanup that must happen after the request has
        // finished. (i.e. either the response has come back and been processed, or some error
        // has occurred and we have given up.
        // It will always be called, regardless of the status of the response.
    }

    protected int handleHttpError(final int httpStatus) {
        // This function can be overriden if the child class needs to change the result code
        // based on the http response status.
        return RESULT_OTHER_FAILURE;
    }

    /**
     * Reset the protocol version to use for this connection. If it's changed, and our account is
     * persisted, also write back the changes to the DB. Note that this function is called at
     * the time of Account creation but does not update the Account object with the various flags
     * at that point in time.
     * TODO: Make sure that the Account flags are set properly in this function or a similar
     * function in the future. Right now the Account setup activity sets the flags, this is not
     * the right design.
     * @param protocolVersion The new protocol version to use, as a string.
     */
    protected final void setProtocolVersion(final String protocolVersion) {
        final long accountId = getAccountId();
        if (mConnection.setProtocolVersion(protocolVersion) && accountId != Account.NOT_SAVED) {
            final Uri uri = ContentUris.withAppendedId(Account.CONTENT_URI, accountId);
            final ContentValues cv = new ContentValues(2);
            if (getProtocolVersion() >= 12.0) {
                final int oldFlags = Utility.getFirstRowInt(mContext, uri,
                        Account.ACCOUNT_FLAGS_PROJECTION, null, null, null,
                        Account.ACCOUNT_FLAGS_COLUMN_FLAGS, 0);
                final int newFlags = oldFlags |
                        Account.FLAGS_SUPPORTS_GLOBAL_SEARCH | Account.FLAGS_SUPPORTS_SEARCH |
                                Account.FLAGS_SUPPORTS_SMART_FORWARD;
                if (oldFlags != newFlags) {
                    cv.put(EmailContent.AccountColumns.FLAGS, newFlags);
                }
            }
            cv.put(EmailContent.AccountColumns.PROTOCOL_VERSION, protocolVersion);
            mContext.getContentResolver().update(uri, cv, null, null);
        }
    }

    /**
     * Create the request object for this operation.
     * The default is to use a POST, but some use other request types (e.g. Options).
     * @return An {@link HttpUriRequest}.
     * @throws IOException
     */
    protected HttpUriRequest makeRequest() throws IOException, MessageInvalidException {
        final String requestUri = getRequestUri();
        HttpUriRequest req = mConnection.makePost(requestUri, getRequestEntity(),
                getRequestContentType(), addPolicyKeyHeaderToRequest());
        return req;
    }

    /**
     * The following functions MUST be overridden by subclasses; these are things that are unique
     * to each operation.
     */

    /**
     * Get the name of the operation, used as the "Cmd=XXX" query param in the request URI. Note
     * that if you override {@link #getRequestUri}, then this function may be unused for normal
     * operation, but all subclasses should return something non-null for use with logging.
     * @return The name of the command for this operation as defined by the EAS protocol, or for
     *         commands that don't need it, a suitable descriptive name for logging.
     */
    protected abstract String getCommand();

    /**
     * Build the {@link HttpEntity} which is used to construct the POST. Typically this function
     * will build the Exchange request using a {@link Serializer} and then call {@link #makeEntity}.
     * If the subclass is not using a POST, then it should override this to return null.
     * @return The {@link HttpEntity} to pass to {@link com.android.exchange.service.EasServerConnection#makePost}.
     * @throws IOException
     */
    protected abstract HttpEntity getRequestEntity() throws IOException, MessageInvalidException;

    /**
     * Parse the response from the Exchange perform whatever actions are dictated by that.
     * @param response The {@link EasResponse} to our request.
     * @return A result code. Non-negative values are returned directly to the caller; negative
     *         values
     *
     * that is returned to the caller of {@link #performOperation}.
     * @throws IOException
     */
    protected abstract int handleResponse(final EasResponse response)
            throws IOException, CommandStatusException;

    /**
     * The following functions may be overriden by a subclass, but most operations will not need
     * to do so.
     */

    /**
     * Get the URI for the Exchange server and this operation. Most (signed in) operations need
     * not override this; the notable operation that needs to override it is auto-discover.
     * @return
     */
    protected String getRequestUri() {
        return mConnection.makeUriString(getCommand());
    }

    /**
     * @return Whether to set the X-MS-PolicyKey header. Only Ping does not want this header.
     */
    protected boolean addPolicyKeyHeaderToRequest() {
        return true;
    }

    /**
     * @return The content type of this request.
     */
    protected String getRequestContentType() {
        return EAS_14_MIME_TYPE;
    }

    /**
     * @return The timeout to use for the POST.
     */
    protected long getTimeout() {
        return 30 * DateUtils.SECOND_IN_MILLIS;
    }

    /**
     * If 403 responses should be handled in a special way, this function should be overridden to
     * do that.
     * @return Whether we handle 403 responses; if false, then treat 403 as a provisioning error.
     */
    protected boolean handleForbidden() {
        return false;
    }

    /**
     * Handle a provisioning error. Subclasses may override this to do something different, e.g.
     * to validate rather than actually do the provisioning.
     * @return
     */
    protected boolean handleProvisionError() {
        final EasProvision provisionOperation = new EasProvision(this);
        return provisionOperation.provision();
    }

    /**
     * Convenience methods for subclasses to use.
     */

    /**
     * Convenience method to make an {@link HttpEntity} from {@link Serializer}.
     */
    protected final HttpEntity makeEntity(final Serializer s) {
        return new ByteArrayEntity(s.toByteArray());
    }

    /**
     * Check whether we should ask the server what protocol versions it supports and set this
     * account to use that version.
     * @return Whether we need a new protocol version from the server.
     */
    protected final boolean shouldGetProtocolVersion() {
        // TODO: Find conditions under which we should check other than not having one yet.
        return !mConnection.isProtocolVersionSet();
    }

    /**
     * @return The protocol version to use.
     */
    protected final double getProtocolVersion() {
        return mConnection.getProtocolVersion();
    }

    /**
     * @return Our useragent.
     */
    protected final String getUserAgent() {
        return mConnection.getUserAgent();
    }

    /**
     * @return Whether we succeeeded in registering the client cert.
     */
    protected final boolean registerClientCert() {
        return mConnection.registerClientCert();
    }

    /**
     * Add the device information to the current request.
     * @param s The {@link Serializer} for our current request.
     * @param context The {@link Context} for current device.
     * @param userAgent The user agent string that our connection use.
     */
    protected static void expandedAddDeviceInformationToSerializer(final Serializer s,
            final Context context, final String userAgent) throws IOException {
        final String deviceId;
        final String phoneNumber;
        final String operator;
        final TelephonyManager tm = (TelephonyManager)context.getSystemService(
                Context.TELEPHONY_SERVICE);
        if (tm != null) {
            deviceId = tm.getDeviceId();
            phoneNumber = tm.getLine1Number();
            // TODO: This is not perfect and needs to be improved, for at least two reasons:
            // 1) SIM cards can override this name.
            // 2) We don't resend this info to the server when we change networks.
            final String operatorName = tm.getNetworkOperatorName();
            final String operatorNumber = tm.getNetworkOperator();
            if (!TextUtils.isEmpty(operatorName) && !TextUtils.isEmpty(operatorNumber)) {
                operator = operatorName + " (" + operatorNumber + ")";
            } else if (!TextUtils.isEmpty(operatorName)) {
                operator = operatorName;
            } else {
                operator = operatorNumber;
            }
        } else {
            deviceId = null;
            phoneNumber = null;
            operator = null;
        }

        // TODO: Right now, we won't send this information unless the device is provisioned again.
        // Potentially, this means that our phone number could be out of date if the user
        // switches sims. Is there something we can do to force a reprovision?
        s.start(Tags.SETTINGS_DEVICE_INFORMATION).start(Tags.SETTINGS_SET);
        s.data(Tags.SETTINGS_MODEL, Build.MODEL);
        if (deviceId != null) {
            s.data(Tags.SETTINGS_IMEI, tm.getDeviceId());
        }
        // Set the device friendly name, if we have one.
        // TODO: Longer term, this should be done without a provider call.
        final Bundle deviceName = context.getContentResolver().call(
                EmailContent.CONTENT_URI, EmailContent.DEVICE_FRIENDLY_NAME, null, null);
        if (deviceName != null) {
            final String friendlyName = deviceName.getString(EmailContent.DEVICE_FRIENDLY_NAME);
            if (!TextUtils.isEmpty(friendlyName)) {
                s.data(Tags.SETTINGS_FRIENDLY_NAME, friendlyName);
            }
        }
        s.data(Tags.SETTINGS_OS, "Android " + Build.VERSION.RELEASE);
        if (phoneNumber != null) {
            s.data(Tags.SETTINGS_PHONE_NUMBER, phoneNumber);
        }
        // TODO: Consider setting this, but make sure we know what it's used for.
        // If the user changes the device's locale and we don't do a reprovision, the server's
        // idea of the language will be wrong. Since we're not sure what this is used for,
        // right now we're leaving it out.
        //s.data(Tags.SETTINGS_OS_LANGUAGE, Locale.getDefault().getDisplayLanguage());
        s.data(Tags.SETTINGS_USER_AGENT, userAgent);
        if (operator != null) {
            s.data(Tags.SETTINGS_MOBILE_OPERATOR, operator);
        }
        s.end().end();  // SETTINGS_SET, SETTINGS_DEVICE_INFORMATION
    }

    /**
     * Add the device information to the current request.
     * @param s The {@link Serializer} that contains the payload for this request.
     */
    protected final void addDeviceInformationToSerializer(final Serializer s)
            throws IOException {
        final String userAgent = getUserAgent();
        expandedAddDeviceInformationToSerializer(s, mContext, userAgent);
    }

    /**
     * Convenience method for adding a Message to an account's outbox
     * @param account The {@link Account} from which to send the message.
     * @param msg the message to send
     */
    protected final void sendMessage(final Account account, final EmailContent.Message msg) {
        long mailboxId = Mailbox.findMailboxOfType(mContext, account.mId, Mailbox.TYPE_OUTBOX);
        // TODO: Improve system mailbox handling.
        if (mailboxId == Mailbox.NO_MAILBOX) {
            LogUtils.d(LOG_TAG, "No outbox for account %d, creating it", account.mId);
            final Mailbox outbox =
                    Mailbox.newSystemMailbox(mContext, account.mId, Mailbox.TYPE_OUTBOX);
            outbox.save(mContext);
            mailboxId = outbox.mId;
        }
        msg.mMailboxKey = mailboxId;
        msg.mAccountKey = account.mId;
        msg.save(mContext);
        requestSyncForMailbox(new android.accounts.Account(account.mEmailAddress,
                Eas.EXCHANGE_ACCOUNT_MANAGER_TYPE), mailboxId);
    }

    /**
     * Issue a {@link android.content.ContentResolver#requestSync} for a specific mailbox.
     * @param amAccount The {@link android.accounts.Account} for the account we're pinging.
     * @param mailboxId The id of the mailbox that needs to sync.
     */
    protected static void requestSyncForMailbox(final android.accounts.Account amAccount,
            final long mailboxId) {
        final Bundle extras = Mailbox.createSyncBundle(mailboxId);
        ContentResolver.requestSync(amAccount, EmailContent.AUTHORITY, extras);
        LogUtils.i(LOG_TAG, "requestSync EasOperation requestSyncForMailbox %s, %s",
                amAccount.toString(), extras.toString());
    }

    protected static void requestSyncForMailboxes(final android.accounts.Account amAccount,
            final String authority, final ArrayList<Long> mailboxIds) {
        final Bundle extras = Mailbox.createSyncBundle(mailboxIds);
        /**
         * TODO: Right now, this function is only called by EasPing, should this function be
         * moved there?
         */
        ContentResolver.requestSync(amAccount, authority, extras);
        LogUtils.i(LOG_TAG, "EasOperation requestSyncForMailboxes  %s, %s",
                amAccount.toString(), extras.toString());
    }

    public static int translateSyncResultToUiResult(final int result) {
        switch (result) {
              case RESULT_TOO_MANY_REDIRECTS:
                return UIProvider.LastSyncResult.INTERNAL_ERROR;
            case RESULT_NETWORK_PROBLEM:
                return UIProvider.LastSyncResult.CONNECTION_ERROR;
            case RESULT_FORBIDDEN:
            case RESULT_PROVISIONING_ERROR:
            case RESULT_AUTHENTICATION_ERROR:
            case RESULT_CLIENT_CERTIFICATE_REQUIRED:
                return UIProvider.LastSyncResult.AUTH_ERROR;
            case RESULT_PROTOCOL_VERSION_UNSUPPORTED:
                // Only used in validate, so there's never a syncResult to write to here.
                break;
            case RESULT_INITIALIZATION_FAILURE:
            case RESULT_HARD_DATA_FAILURE:
                return UIProvider.LastSyncResult.INTERNAL_ERROR;
            case RESULT_OTHER_FAILURE:
                return UIProvider.LastSyncResult.INTERNAL_ERROR;
        }
        return UIProvider.LastSyncResult.SUCCESS;
    }
}
