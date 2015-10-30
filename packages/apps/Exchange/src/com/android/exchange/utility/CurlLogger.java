// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.exchange.utility;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.android.exchange.Eas;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

/**
 * Logs cURL commands equivalent to requests.
 * Curl Logging is copied over from AndroidHttpClient. Just switching to AndroidHttpClient is
 * not trivial so it's easier to borrow the curl logging code this way.
 */
public class CurlLogger implements HttpRequestInterceptor {
    private static final String TAG = Eas.LOG_TAG;

    @Override
    public void process(HttpRequest request, HttpContext context) throws IOException {
        if (request instanceof HttpUriRequest) {
            if ((Build.TYPE.equals("userdebug") || Build.TYPE.equals("eng"))
                    &&  Log.isLoggable(TAG, Log.VERBOSE)) {
                // Allow us to log auth token on dev devices - this is not a big security risk
                // because dev devices have a readable account.db file where all the auth tokens
                // are stored.
                Log.d(TAG, toCurl((HttpUriRequest) request, true));
            } else  if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, toCurl((HttpUriRequest) request, false));
            }
        }
    }

    /**
     * Generates a cURL command equivalent to the given request.
     */
    private static String toCurl(HttpUriRequest request, boolean logAuthToken) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append("curl ");

        for (Header header: request.getAllHeaders()) {
            builder.append("--header \"");
            if (!logAuthToken
                    && (header.getName().equals("Authorization") ||
                    header.getName().equals("Cookie"))) {

                builder.append(header.getName()).append(": ").append("${token}");
            } else {
                builder.append(header.toString().trim());
            }
            builder.append("\" ");
        }

        URI uri = request.getURI();

        // If this is a wrapped request, use the URI from the original
        // request instead. getURI() on the wrapper seems to return a
        // relative URI. We want an absolute URI.
        if (request instanceof RequestWrapper) {
            HttpRequest original = ((RequestWrapper) request).getOriginal();
            if (original instanceof HttpUriRequest) {
                uri = ((HttpUriRequest) original).getURI();
            }
        }

        builder.append("\"");
        builder.append(uri);
        builder.append("\"");

        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest entityRequest =
                    (HttpEntityEnclosingRequest) request;
            HttpEntity entity = entityRequest.getEntity();
            if (entity != null && entity.isRepeatable()) {
                if (entity.getContentLength() < 1024) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    entity.writeTo(stream);

                    String base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
                    builder.insert(0, "echo '" + base64 + "' | base64 -d > /tmp/$$.bin; ");
                    builder.append(" --data-binary @/tmp/$$.bin");
                } else {
                    builder.append(" [TOO MUCH DATA TO INCLUDE]");
                }
            }
        }

        return builder.toString();
    }

}
