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

import android.content.Context;
import android.os.Bundle;

import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.provider.Policy;
import com.android.emailcommon.service.EmailServiceProxy;
import com.android.exchange.CommandStatusException;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.FolderSyncParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.mail.utils.LogUtils;

import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * Implements the EAS FolderSync command. We use this both to actually do a folder sync, and also
 * during account adding flow as a convenient command to validate the account settings (e.g. since
 * it needs to login and will tell us about provisioning requirements).
 * TODO: Doing validation here is kind of wonky. There must be a better way.
 * TODO: Add the use of the Settings command during validation.
 *
 * See http://msdn.microsoft.com/en-us/library/ee237648(v=exchg.80).aspx for more details.
 */
public class EasFolderSync extends EasOperation {

    /** Result code indicating the sync completed correctly. */
    public static final int RESULT_OK = 1;
    /**
     * Result code indicating that this object was constructed for sync and was asked to validate,
     * or vice versa.
     */
    public static final int RESULT_WRONG_OPERATION = 2;

    /** Indicates whether this object is for validation rather than sync. */
    private final boolean mStatusOnly;

    /** During validation, this holds the policy we must enforce. */
    private Policy mPolicy;

    /** During validation, this holds the result. */
    private Bundle mValidationResult;

    /**
     * Constructor for actually doing folder sync.
     * @param context
     * @param account
     */
    public EasFolderSync(final Context context, final Account account) {
        super(context, account);
        mStatusOnly = false;
        mPolicy = null;
    }

    private static Account makeAccount(final HostAuth hostAuth) {
        final Account account = new Account();
        account.mHostAuthRecv = hostAuth;
        account.mEmailAddress = hostAuth.mLogin;
        return account;
    }

    /**
     * Constructor for account validation.
     * @param context
     * @param hostAuth
     */
    public EasFolderSync(final Context context, final HostAuth hostAuth) {
        super(context, makeAccount(hostAuth), hostAuth);
        mStatusOnly = true;
    }

    @Override
    public int performOperation() {
        if (mStatusOnly) {
            return validate();
        } else {
            LogUtils.d(LOG_TAG, "Performing FolderSync for account %d", getAccountId());
            return super.performOperation();
        }
    }

    /**
     * Returns the validation results after this operation has been performed.
     * @return The validation results.
     */
    public Bundle getValidationResult() {
        return mValidationResult;
    }

    /**
     * Helper function for {@link #performOperation} -- do some initial checks and, if they pass,
     * perform a folder sync to verify that we can. This sets {@link #mValidationResult} as a side
     * effect which holds the result details needed by the UI.
     * @return A result code, either from above or from the base class.
     */
    private int validate() {
        mValidationResult = new Bundle(3);
        if (!mStatusOnly) {
            writeResultCode(mValidationResult, RESULT_OTHER_FAILURE);
            return RESULT_OTHER_FAILURE;
        }
        LogUtils.d(LOG_TAG, "Performing validation");

        if (!registerClientCert()) {
            mValidationResult.putInt(EmailServiceProxy.VALIDATE_BUNDLE_RESULT_CODE,
                    MessagingException.CLIENT_CERTIFICATE_ERROR);
            return RESULT_CLIENT_CERTIFICATE_REQUIRED;
        }

        if (shouldGetProtocolVersion()) {
            final EasOptions options = new EasOptions(this);
            final int result = options.getProtocolVersionFromServer();
            if (result != EasOptions.RESULT_OK) {
                writeResultCode(mValidationResult, result);
                return result;
            }
            final String protocolVersion = options.getProtocolVersionString();
            setProtocolVersion(protocolVersion);
            mValidationResult.putString(EmailServiceProxy.VALIDATE_BUNDLE_PROTOCOL_VERSION,
                    protocolVersion);
        }

        // This is intentionally a call to super.performOperation. This is a helper function for
        // our version of perfomOperation so calling that function would infinite loop.
        final int result = super.performOperation();
        writeResultCode(mValidationResult, result);
        return result;
    }

    @Override
    protected String getCommand() {
        return "FolderSync";
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        final String syncKey = mAccount.mSyncKey != null ? mAccount.mSyncKey : "0";
        final Serializer s = new Serializer();
        s.start(Tags.FOLDER_FOLDER_SYNC).start(Tags.FOLDER_SYNC_KEY).text(syncKey)
            .end().end().done();
        return makeEntity(s);
    }

    @Override
    protected int handleResponse(final EasResponse response)
            throws IOException, CommandStatusException {
        if (!response.isEmpty()) {
            new FolderSyncParser(mContext, mContext.getContentResolver(),
                    response.getInputStream(), mAccount, mStatusOnly).parse();
        }
        return RESULT_OK;
    }

    @Override
    protected boolean handleForbidden() {
        return mStatusOnly;
    }

    @Override
    protected boolean handleProvisionError() {
        if (mStatusOnly) {
            final EasProvision provisionOperation = new EasProvision(this);
            mPolicy = provisionOperation.test();
            // Regardless of whether the policy is supported, we return false because there's
            // no need to re-run the operation.
            return false;
        }
        return super.handleProvisionError();
    }

    /**
     * Translate {@link EasOperation} result codes to the values needed by the RPC, and write
     * them to the {@link Bundle}.
     * @param bundle The {@link Bundle} to return to the RPC.
     * @param resultCode The result code for this operation.
     */
    private void writeResultCode(final Bundle bundle, final int resultCode) {
        final int messagingExceptionCode;
        switch (resultCode) {
            case RESULT_ABORT:
                messagingExceptionCode = MessagingException.IOERROR;
                break;
            case RESULT_RESTART:
                messagingExceptionCode = MessagingException.IOERROR;
                break;
            case RESULT_TOO_MANY_REDIRECTS:
                messagingExceptionCode = MessagingException.UNSPECIFIED_EXCEPTION;
                break;
            case RESULT_NETWORK_PROBLEM:
                messagingExceptionCode = MessagingException.IOERROR;
                break;
            case RESULT_FORBIDDEN:
                messagingExceptionCode = MessagingException.ACCESS_DENIED;
                break;
            case RESULT_PROVISIONING_ERROR:
                if (mPolicy == null) {
                    messagingExceptionCode = MessagingException.UNSPECIFIED_EXCEPTION;
                } else {
                    bundle.putParcelable(EmailServiceProxy.VALIDATE_BUNDLE_POLICY_SET, mPolicy);
                    messagingExceptionCode = mPolicy.mProtocolPoliciesUnsupported == null ?
                            MessagingException.SECURITY_POLICIES_REQUIRED :
                            MessagingException.SECURITY_POLICIES_UNSUPPORTED;
                }
                break;
            case RESULT_AUTHENTICATION_ERROR:
                messagingExceptionCode = MessagingException.AUTHENTICATION_FAILED;
                break;
            case RESULT_CLIENT_CERTIFICATE_REQUIRED:
                messagingExceptionCode = MessagingException.CLIENT_CERTIFICATE_REQUIRED;
                break;
            case RESULT_PROTOCOL_VERSION_UNSUPPORTED:
                messagingExceptionCode = MessagingException.PROTOCOL_VERSION_UNSUPPORTED;
                break;
            case RESULT_OTHER_FAILURE:
                messagingExceptionCode = MessagingException.UNSPECIFIED_EXCEPTION;
                break;
            case RESULT_OK:
                messagingExceptionCode = MessagingException.NO_ERROR;
                break;
            default:
                messagingExceptionCode = MessagingException.UNSPECIFIED_EXCEPTION;
                break;
        }
        bundle.putInt(EmailServiceProxy.VALIDATE_BUNDLE_RESULT_CODE, messagingExceptionCode);
    }
}
