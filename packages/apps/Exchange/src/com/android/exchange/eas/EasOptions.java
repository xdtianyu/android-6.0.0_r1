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

import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.mail.utils.LogUtils;
import com.google.common.collect.Sets;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.util.HashSet;

/**
 * Performs an HTTP Options request to the Exchange server, in order to get the protocol
 * version.
 */
public class EasOptions extends EasOperation {
    private static final String LOG_TAG = Eas.LOG_TAG;

    /** Result code indicating we successfully got a protocol version. */
    public static final int RESULT_OK = 1;

    /** Set of Exchange protocol versions we understand. */
    private static final HashSet<String> SUPPORTED_PROTOCOL_VERSIONS = Sets.newHashSet(
            Eas.SUPPORTED_PROTOCOL_EX2003,
            Eas.SUPPORTED_PROTOCOL_EX2007, Eas.SUPPORTED_PROTOCOL_EX2007_SP1,
            Eas.SUPPORTED_PROTOCOL_EX2010, Eas.SUPPORTED_PROTOCOL_EX2010_SP1);

    private String mProtocolVersion = null;

    public EasOptions(final EasOperation parentOperation) {
        super(parentOperation);
    }

    /**
     * Perform the server request. If successful, callers should use
     * {@link #getProtocolVersionString} to get the actual protocol version value.
     * @return A result code; {@link #RESULT_OK} is the only value that indicates success.
     */
    public int getProtocolVersionFromServer() {
        return performOperation();
    }

    /**
     * @return The protocol version to use, or null if we did not successfully get one.
     */
    public String getProtocolVersionString() {
        return mProtocolVersion;
    }

    /**
     * Note that this operation does not actually use this name when forming the request.
     * @return A useful name for logging this operation.
     */
    @Override
    protected String getCommand() {
        return "OPTIONS";
    }

    @Override
    protected HttpEntity getRequestEntity() {
        return null;
    }

    @Override
    protected int handleResponse(final EasResponse response) {
        final Header commands = response.getHeader("MS-ASProtocolCommands");
        final Header versions = response.getHeader("ms-asprotocolversions");
        final boolean hasProtocolVersion;
        if (commands == null || versions == null) {
            LogUtils.e(LOG_TAG, "OPTIONS response without commands or versions");
            hasProtocolVersion = false;
        } else {
            mProtocolVersion = getProtocolVersionFromHeader(versions);
            hasProtocolVersion = (mProtocolVersion != null);
        }
        if (!hasProtocolVersion) {
            return RESULT_PROTOCOL_VERSION_UNSUPPORTED;
        }

        return RESULT_OK;
    }

    @Override
    protected String getRequestUri() {
        return null;
    }

    protected HttpUriRequest makeRequest() throws IOException, MessageInvalidException {
        return mConnection.makeOptions();
    }
    /**
     * Find the best protocol version to use from the header.
     * @param versionHeader The {@link Header} for the server's supported versions.
     * @return The best protocol version we mutually support, or null if none found.
     */
    private String getProtocolVersionFromHeader(final Header versionHeader) {
        // The string is a comma separated list of EAS versions in ascending order
        // e.g. 1.0,2.0,2.5,12.0,12.1,14.0,14.1
        final String supportedVersions = versionHeader.getValue();
        LogUtils.d(LOG_TAG, "Server supports versions: %s", supportedVersions);
        final String[] supportedVersionsArray = supportedVersions.split(",");
        // Find the most recent version we support
        String newProtocolVersion = null;
        for (final String version: supportedVersionsArray) {
            if (SUPPORTED_PROTOCOL_VERSIONS.contains(version)) {
                newProtocolVersion = version;
            }
        }
        return newProtocolVersion;
    }
}
