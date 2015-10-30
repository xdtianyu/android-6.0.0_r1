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

package com.android.exchange.utility;

import android.util.Base64;
import android.util.Log;

import com.android.exchange.Eas;
import com.android.mail.utils.LogUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Dumps the wbxml in base64 (much like {@link CurlLogger}) so that the
 * response from Exchange can be viewed for debugging purposes.
 */
public class WbxmlResponseLogger implements HttpResponseInterceptor {
    private static final String TAG = Eas.LOG_TAG;
    protected static final int MAX_LENGTH = 1024;

    protected static boolean shouldLogResponse(final long contentLength) {
        // Not going to bother if there is a lot of content since most of that information
        // will probably just be message contents anyways.
        return contentLength < MAX_LENGTH;
    }

    protected static String processContentEncoding(final Header encodingHeader) {
        if (encodingHeader != null) {
            final String encodingValue = encodingHeader.getValue();
            return (encodingValue == null) ? "UTF-8" : encodingValue;
        }
        return "UTF-8";
    }

    protected static byte[] getContentAsByteArray(InputStream is, int batchSize)
        throws IOException {
        // Start building our byte array to encode and dump.
        int count;
        final byte[] data = new byte[batchSize];
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while ((count = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, count);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws IOException {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            // Wrap the HttpEntity so the response InputStream can be requested and processed
            // numerous times.
            response.setEntity(new BufferedHttpEntity(response.getEntity()));

            // Now grab the wrapped HttpEntity so that you safely can process the response w/o
            // affecting the core response processing module.
            final HttpEntity entity = response.getEntity();
            if (!shouldLogResponse(entity.getContentLength())) {
                LogUtils.d(TAG, "wbxml response: [TOO MUCH DATA TO INCLUDE]");
                return;
            }

            // We need to figure out the encoding in the case that it is gzip and we need to
            // inflate it during processing.
            final Header encodingHeader = entity.getContentEncoding();
            final String encoding = processContentEncoding(encodingHeader);

            final InputStream is;
            if (encoding.equals("gzip")) {
                // We need to inflate this first.
                final InputStream unwrappedIs = response.getEntity().getContent();
                is = new GZIPInputStream(unwrappedIs);
            } else {
                is = response.getEntity().getContent();
            }

            final byte currentXMLBytes[] = getContentAsByteArray(is, MAX_LENGTH);

            // Now let's dump out the base 64 encoded bytes and the rest of the command that will
            // tell us what the response is.
            final String base64 = Base64.encodeToString(currentXMLBytes, Base64.NO_WRAP);
            LogUtils.d(TAG, "wbxml response: echo '%s' | base64 -d | wbxml", base64);
        }
    }

}
