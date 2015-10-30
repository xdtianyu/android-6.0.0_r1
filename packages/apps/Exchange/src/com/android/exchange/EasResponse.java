/*
 * Copyright (C) 2008-2009 Marc Blank
 * Licensed to The Android Open Source Project.
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

package com.android.exchange;

import android.net.Uri;

import com.android.emailcommon.utility.EmailClientConnectionManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Encapsulate a response to an HTTP POST
 */
public class EasResponse {
    // MSFT's custom HTTP result code indicating the need to provision
    static private final int HTTP_NEED_PROVISIONING = 449;

    // Microsoft-defined HTTP response indicating a redirect to a "better" server.
    // Why is this a 4xx instead of 3xx? Because EAS considers this a "Device misconfigured" error.
    static private final int HTTP_REDIRECT = 451;

    private final HttpResponse mResponse;
    private final HttpEntity mEntity;
    private final int mLength;
    private InputStream mInputStream;
    private boolean mClosed;

    private final int mStatus;

    /**
     * Whether or not a certificate was requested by the server and missing.
     * If this is set, it is essentially a 403 whereby the failure was due
     */
    private final boolean mClientCertRequested;

    private EasResponse(final HttpResponse response,
            final EmailClientConnectionManager connManager, final long reqTime) {
        mResponse = response;
        mEntity = (response == null) ? null : mResponse.getEntity();
        if (mEntity !=  null) {
            mLength = (int) mEntity.getContentLength();
        } else {
            mLength = 0;
        }
        int status = response.getStatusLine().getStatusCode();
        mClientCertRequested =
                isAuthError(status) && connManager.hasDetectedUnsatisfiedCertReq(reqTime);
        if (mClientCertRequested) {
            status = HttpStatus.SC_UNAUTHORIZED;
            mClosed = true;
        }
        mStatus = status;
    }

    public static EasResponse fromHttpRequest(
            EmailClientConnectionManager connManager, HttpClient client, HttpUriRequest request)
            throws IOException {
        final long reqTime = System.currentTimeMillis();
        final HttpResponse response = client.execute(request);
        return new EasResponse(response, connManager, reqTime);
    }

    public boolean isSuccess() {
        return mStatus == HttpStatus.SC_OK;
    }

    public boolean isForbidden() {
        return mStatus == HttpStatus.SC_FORBIDDEN;
    }

    /**
     * @return Whether this response indicates an authentication error.
     */
    public boolean isAuthError() {
        return mStatus == HttpStatus.SC_UNAUTHORIZED;
    }

    /**
     * @return Whether this response indicates a provisioning error.
     */
    public boolean isProvisionError() {
        return (mStatus == HTTP_NEED_PROVISIONING) || isForbidden();
    }

    /**
     * @return Whether this response indicates a redirect error.
     */
    public boolean isRedirectError() {
        return mStatus == HTTP_REDIRECT;
    }

    /**
     * Determine whether an HTTP code represents an authentication error
     * @param code the HTTP code returned by the server
     * @return whether or not the code represents an authentication error
     */
    private static boolean isAuthError(int code) {
        return (code == HttpStatus.SC_UNAUTHORIZED) || (code == HttpStatus.SC_FORBIDDEN);
    }

    /**
     * Read the redirect address from this response, if it's present.
     * @return The new host address, or null if it's not there.
     */
    public String getRedirectAddress() {
        final Header locHeader = getHeader("X-MS-Location");
        if (locHeader != null) {
            return Uri.parse(locHeader.getValue()).getHost();
        }
        return null;
    }

    /**
     * Return an appropriate input stream for the response, either a GZIPInputStream, for
     * compressed data, or a generic InputStream otherwise
     * @return the input stream for the response
     */
    public InputStream getInputStream() {
        if (mInputStream != null || mClosed) {
            throw new IllegalStateException("Can't reuse stream or get closed stream");
        } else if (mEntity == null) {
            throw new IllegalStateException("Can't get input stream without entity");
        }
        InputStream is = null;
        try {
            // Get the default input stream for the entity
            is = mEntity.getContent();
            Header ceHeader = mResponse.getFirstHeader("Content-Encoding");
            if (ceHeader != null) {
                String encoding = ceHeader.getValue();
                // If we're gzip encoded, wrap appropriately
                if (encoding.toLowerCase().equals("gzip")) {
                    is = new GZIPInputStream(is);
                }
            }
        } catch (IllegalStateException e1) {
        } catch (IOException e1) {
        }
        mInputStream = is;
        return is;
    }

    public boolean isEmpty() {
        return mLength == 0;
    }

    public int getStatus() {
        return mStatus;
    }

    public boolean isMissingCertificate() {
        return mClientCertRequested;
    }

    public Header getHeader(String name) {
        return (mResponse == null) ? null : mResponse.getFirstHeader(name);
    }

    public int getLength() {
        return mLength;
    }

    public void close() {
        if (!mClosed) {
            if (mEntity != null) {
                try {
                    mEntity.consumeContent();
                } catch (IOException e) {
                    // No harm, no foul
                }
            }
            if (mInputStream instanceof GZIPInputStream) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    // We tried
                }
            }
            mClosed = true;
        }
    }
}