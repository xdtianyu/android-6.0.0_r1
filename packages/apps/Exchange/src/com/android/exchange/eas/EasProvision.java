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

import android.content.ContentValues;
import android.content.Context;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.Policy;
import com.android.emailcommon.service.PolicyServiceProxy;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.ProvisionParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;
import com.android.exchange.service.EasServerConnection;
import com.android.mail.utils.LogUtils;

import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * Implements the EAS Provision protocol.
 *
 * Provisioning actually consists of two server interactions:
 * 1) Ask the server for the required policies.
 * 2) Acknowledge our disposition for enforcing those policies.
 *
 * The structure of the requests and response are essentially the same for both, so we use the
 * same code and vary slightly based on which one we're doing. Also, provisioning responses can tell
 * us to wipe the device, so we need to handle that too.
 * TODO: Make it possible to ack separately, possibly by splitting into separate operations.
 * See http://msdn.microsoft.com/en-us/library/ee203567(v=exchg.80).aspx for more details.
 */
public class EasProvision extends EasOperation {

    private static final String LOG_TAG = Eas.LOG_TAG;

    /** The policy type for versions of EAS prior to 2007. */
    public static final String EAS_2_POLICY_TYPE = "MS-WAP-Provisioning-XML";
    /** The policy type for versions of EAS starting with 2007. */
    public static final String EAS_12_POLICY_TYPE = "MS-EAS-Provisioning-WBXML";

    /** The EAS protocol Provision status for "we implement all of the policies" */
    static final String PROVISION_STATUS_OK = "1";
    /** The EAS protocol Provision status meaning "we partially implement the policies" */
    static final String PROVISION_STATUS_PARTIAL = "2";

    /** Value for {@link #mPhase} indicating we're performing the initial request. */
    static final int PHASE_INITIAL = 0;
    /** Value for {@link #mPhase} indicating we're performing the acknowledgement request. */
    static final int PHASE_ACKNOWLEDGE = 1;
    /** Value for {@link #mPhase} indicating we're performing the acknowledgement for a wipe. */
    static final int PHASE_WIPE = 2;

    /**
     * This operation doesn't use public result codes because ultimately the operation answers
     * a yes/no question. These result codes are used internally only to communicate from
     * {@link #handleResponse}.
     */

    /** Result code indicating the server's policy can be fully supported. */
    private static final int RESULT_POLICY_SUPPORTED = 1;
    /** Result code indicating the server's policy cannot be fully supported. */
    private static final int RESULT_POLICY_UNSUPPORTED = 2;
    /** Result code indicating the server sent a remote wipe directive. */
    private static final int RESULT_REMOTE_WIPE = 3;

    private Policy mPolicy;
    private String mPolicyKey;
    private String mStatus;

    /**
     * Because this operation supports variants of the request and parsing, and {@link EasOperation}
     * has no way to communicate this into {@link #performOperation}, we use this member variable
     * to vary how {@link #getRequestEntity} and {@link #handleResponse} work.
     */
    private int mPhase;

    public EasProvision(final Context context, final Account account,
            final EasServerConnection connection) {
        super(context, account, connection);
        mPolicy = null;
        mPolicyKey = null;
        mStatus = null;
        mPhase = 0;
    }

    public EasProvision(final EasOperation parentOperation) {
        super(parentOperation);
        mPolicy = null;
        mPolicyKey = null;
        mStatus = null;
        mPhase = 0;
    }

    private int performInitialRequest() {
        mPhase = PHASE_INITIAL;
        return performOperation();
    }

    private void performAckRequestForWipe() {
        mPhase = PHASE_WIPE;
        performOperation();
    }

    private int performAckRequest(final boolean isPartial) {
        mPhase = PHASE_ACKNOWLEDGE;
        mStatus = isPartial ? PROVISION_STATUS_PARTIAL : PROVISION_STATUS_OK;
        return performOperation();
    }

    /**
     * Make the provisioning calls to determine if we can handle the required policy.
     * @return The {@link Policy} if we support it, or null otherwise.
     */
    public final Policy test() {
        int result = performInitialRequest();
        if (result == RESULT_POLICY_UNSUPPORTED) {
            // Check if the server will permit partial policies.
            result = performAckRequest(true);
        }
        if (result == RESULT_POLICY_SUPPORTED) {
            // The server is ok with us not supporting everything, so clear the unsupported ones.
            mPolicy.mProtocolPoliciesUnsupported = null;
        }
        return (result == RESULT_POLICY_SUPPORTED || result == RESULT_POLICY_UNSUPPORTED)
                ? mPolicy : null;
    }

    /**
     * Write the max attachment size that came out of the policy to the Account table in the db.
     * Once this value is written, the mapping to Account.Settings.MAX_ATTACHMENT_SIZE was
     * added to point to this column in this table.
     * @param maxAttachmentSize The max attachment size value that we want to write to the db.
     */
    private void storeMaxAttachmentSize(final int maxAttachmentSize) {
        final ContentValues values = new ContentValues(1);
        values.put(EmailContent.AccountColumns.MAX_ATTACHMENT_SIZE, maxAttachmentSize);
        Account.update(mContext, Account.CONTENT_URI, getAccountId(), values);
    }

    /**
     * Get the required policy from the server and enforce it.
     * @return Whether we succeeded in provisioning this account.
     */
    public final boolean provision() {
        final int result = performInitialRequest();
        final long accountId = getAccountId();

        if (result < 0) {
            return false;
        }

        if (result == RESULT_REMOTE_WIPE) {
            performAckRequestForWipe();
            LogUtils.i(LOG_TAG, "Executing remote wipe");
            PolicyServiceProxy.remoteWipe(mContext);
            return false;
        }

        // Even before the policy is accepted, we can honor this setting since it has nothing
        // to do with the device policy manager and is requested by the Exchange server.
        // TODO: This was an error, this is minimum change to disable it.
        //storeMaxAttachmentSize(mPolicy.mMaxAttachmentSize);

        // Apply the policies (that we support) with the temporary key.
        if (mPolicy != null) {
            mPolicy.mProtocolPoliciesUnsupported = null;
        }
        PolicyServiceProxy.setAccountPolicy(mContext, accountId, mPolicy, null);
        if (!PolicyServiceProxy.isActive(mContext, mPolicy)) {
            return false;
        }

        // Acknowledge to the server and make sure all's well.
        if (performAckRequest(result == RESULT_POLICY_UNSUPPORTED) == RESULT_POLICY_UNSUPPORTED) {
            return false;
        }

        // Write the final policy key to the Account.
        PolicyServiceProxy.setAccountPolicy(mContext, accountId, mPolicy, mPolicyKey);

        // For 12.1 and 14.0, after provisioning we need to also send the device information via
        // the Settings command.
        // See the comments for EasSettings for more details.
        final double version = getProtocolVersion();
        if (version == Eas.SUPPORTED_PROTOCOL_EX2007_SP1_DOUBLE
                || version == Eas.SUPPORTED_PROTOCOL_EX2010_DOUBLE) {
            final EasSettings settingsOperation = new EasSettings(this);
            if (!settingsOperation.sendDeviceInformation()) {
                // TODO: Do something more useful when the settings command fails.
                // The consequence here is that the server will not have device info.
                // However, this is NOT a provisioning failure.
            }
        }

        return true;
    }

    @Override
    protected String getCommand() {
        return "Provision";
    }

    /**
     * Add the device information to the current request.
     * @param context The {@link Context} for the current device.
     * @param userAgent The user agent string that our connection uses.
     * @param policyKey EAS specific tag for Provision requests.
     * @param policyType EAS specific tag for Provision requests.
     * @param status The status value that we are sending to the server in our request.
     * @param phase The phase of the provisioning process this requests is built for.
     * @param protocolVersion The version of the EAS protocol that we should speak.
     * @return The {@link Serializer} containing the payload for this request.
     */
    protected static Serializer generateRequestEntitySerializer(
            final Context context, final String userAgent, final String policyKey,
            final String policyType, final String status, final int phase,
            final double protocolVersion) throws IOException {
        final Serializer s = new Serializer();
        s.start(Tags.PROVISION_PROVISION);

        // When requesting the policy in 14.1, we also need to send device information.
        if (phase == PHASE_INITIAL &&
                protocolVersion >= Eas.SUPPORTED_PROTOCOL_EX2010_SP1_DOUBLE) {
            // The "inner" version of this function is being used because it is
            // re-entrant and can be unit tested easier.  Until we are unit testing
            // everything, the other version of this function still lives so that
            // we are disrupting as little code as possible for now.
            expandedAddDeviceInformationToSerializer(s, context, userAgent);
        }
        if (phase == PHASE_WIPE) {
            s.start(Tags.PROVISION_REMOTE_WIPE);
            s.data(Tags.PROVISION_STATUS, PROVISION_STATUS_OK);
            s.end(); // PROVISION_REMOTE_WIPE
        } else {
            s.start(Tags.PROVISION_POLICIES);
            s.start(Tags.PROVISION_POLICY);
            s.data(Tags.PROVISION_POLICY_TYPE, policyType);
            // When acknowledging a policy, we tell the server whether we applied the policy.
            if (phase == PHASE_ACKNOWLEDGE) {
                s.data(Tags.PROVISION_POLICY_KEY, policyKey);
                s.data(Tags.PROVISION_STATUS, status);
            }
            s.end().end(); // PROVISION_POLICY, PROVISION_POLICIES,
        }
        s.end().done(); // PROVISION_PROVISION
        return s;
    }

    /**
     * Generates a request entity based on the type of request and our current context.
     * @return The {@link HttpEntity} that was generated for this request.
     */
    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        final String policyType = getPolicyType();
        final String userAgent = getUserAgent();
        final double protocolVersion = getProtocolVersion();
        final Serializer s = generateRequestEntitySerializer(mContext, userAgent, mPolicyKey,
                policyType, mStatus, mPhase, protocolVersion);
        return makeEntity(s);
    }

    @Override
    protected int handleResponse(final EasResponse response) throws IOException {
        final ProvisionParser pp = new ProvisionParser(mContext, response.getInputStream());
        // If this is the response for a remote wipe ack, it doesn't have anything useful in it.
        // Just go ahead and return now.
        if (mPhase == PHASE_WIPE) {
            return RESULT_REMOTE_WIPE;
        }

        if (!pp.parse()) {
            throw new IOException("Error while parsing response");
        }

        // What we care about in the response depends on what phase we're in.
        if (mPhase == PHASE_INITIAL) {
            if (pp.getRemoteWipe()) {
                return RESULT_REMOTE_WIPE;
            }
            mPolicy = pp.getPolicy();
            mPolicyKey = pp.getSecuritySyncKey();

            return (pp.hasSupportablePolicySet()
                    ? RESULT_POLICY_SUPPORTED : RESULT_POLICY_UNSUPPORTED);
        }

        if (mPhase == PHASE_ACKNOWLEDGE) {
            mPolicyKey = pp.getSecuritySyncKey();
            return (mPolicyKey != null ? RESULT_POLICY_SUPPORTED : RESULT_POLICY_UNSUPPORTED);
        }

        // Note: this should be unreachable, but the compiler doesn't know it.
        // If we somehow get here, act like we can't do anything.
        return RESULT_POLICY_UNSUPPORTED;
    }

    @Override
    protected boolean handleProvisionError() {
        // If we get a provisioning error while doing provisioning, we should not recurse.
        return false;
    }

    /**
     * @return The policy type for this connection.
     */
    private final String getPolicyType() {
        return (getProtocolVersion() >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) ?
                EAS_12_POLICY_TYPE : EAS_2_POLICY_TYPE;
    }
}
