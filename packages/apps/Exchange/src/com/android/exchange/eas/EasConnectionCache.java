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
import android.text.format.DateUtils;

import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.utility.EmailClientConnectionManager;
import com.android.exchange.Eas;
import com.android.mail.utils.LogUtils;

import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import java.security.cert.CertificateException;
import java.util.HashMap;

/**
 * Manage all {@link EmailClientConnectionManager}s used by Exchange operations.
 * When making connections for persisted accounts, this class will cache and reuse connections
 * as much as possible. All access of connection objects should accordingly go through this class.
 *
 * We use {@link HostAuth}'s id as the cache key. Multiple calls to {@link #getConnectionManager}
 * with {@link HostAuth} objects with the same id will get the same connection object returned,
 * i.e. we assume that the rest of the contents of the {@link HostAuth} objects are also the same,
 * not just the id. If the {@link HostAuth} changes or is deleted, {@link #uncacheConnectionManager}
 * must be called.
 *
 * This cache is a singleton since the whole point is to not have multiples.
 */
public class EasConnectionCache {

    /** The max length of time we want to keep a connection in the cache. */
    private static final long MAX_LIFETIME = 10 * DateUtils.MINUTE_IN_MILLIS;

    private final HashMap<Long, EmailClientConnectionManager> mConnectionMap;
    /** The creation time of connections in mConnectionMap. */
    private final HashMap<Long, Long> mConnectionCreationTimes;

    private static final ConnPerRoute sConnPerRoute = new ConnPerRoute() {
        @Override
        public int getMaxForRoute(final HttpRoute route) {
            return 8;
        }
    };

    /** The singleton instance of the cache. */
    private static EasConnectionCache sCache = null;

    /** Accessor for the cache singleton. */
    public static EasConnectionCache instance() {
        if (sCache == null) {
            sCache = new EasConnectionCache();
        }
        return sCache;
    }

    private EasConnectionCache() {
        mConnectionMap = new HashMap<Long, EmailClientConnectionManager>();
        mConnectionCreationTimes = new HashMap<Long, Long>();
    }

    /**
     * Create an {@link EmailClientConnectionManager} for this {@link HostAuth}.
     * @param context The {@link Context}.
     * @param hostAuth The {@link HostAuth} to which we want to connect.
     * @return The {@link EmailClientConnectionManager} for hostAuth.
     */
    private EmailClientConnectionManager createConnectionManager(final Context context,
            final HostAuth hostAuth)
            throws CertificateException {
        LogUtils.d(Eas.LOG_TAG, "Creating new connection manager for HostAuth %d", hostAuth.mId);
        final HttpParams params = new BasicHttpParams();
        params.setIntParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 25);
        params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, sConnPerRoute);
        final EmailClientConnectionManager mgr =
                EmailClientConnectionManager.newInstance(context, params, hostAuth);

        mgr.registerClientCert(context, hostAuth);

        return mgr;
    }

    /**
     * Get the correct {@link EmailClientConnectionManager} for a {@link HostAuth} from our cache.
     * If it's not in the cache, create and add it.
     * If the cached connection is old, recreate it.
     * @param context The {@link Context}.
     * @param hostAuth The {@link HostAuth} to which we want to connect.
     * @return The {@link EmailClientConnectionManager} for hostAuth.
     */
    private synchronized EmailClientConnectionManager getCachedConnectionManager(
            final Context context, final HostAuth hostAuth)
            throws CertificateException {
        EmailClientConnectionManager connectionManager = mConnectionMap.get(hostAuth.mId);
        final long now = System.currentTimeMillis();
        if (connectionManager != null) {
            final long lifetime = now - mConnectionCreationTimes.get(hostAuth.mId);
            if (lifetime > MAX_LIFETIME) {
                LogUtils.d(Eas.LOG_TAG, "Aging out connection manager for HostAuth %d",
                        hostAuth.mId);
                uncacheConnectionManager(hostAuth);
                connectionManager = null;
            }
        }
        if (connectionManager == null) {
            connectionManager = createConnectionManager(context, hostAuth);
            mConnectionMap.put(hostAuth.mId, connectionManager);
            mConnectionCreationTimes.put(hostAuth.mId, now);
        } else {
            LogUtils.d(Eas.LOG_TAG, "Reusing cached connection manager for HostAuth %d",
                    hostAuth.mId);
        }
        return connectionManager;
    }

    /**
     * Get the correct {@link EmailClientConnectionManager} for a {@link HostAuth}. If the
     * {@link HostAuth} is persistent, then use the cache for this request.
     * @param context The {@link Context}.
     * @param hostAuth The {@link HostAuth} to which we want to connect.
     * @return The {@link EmailClientConnectionManager} for hostAuth.
     */
    public EmailClientConnectionManager getConnectionManager(
            final Context context, final HostAuth hostAuth)
            throws CertificateException {
        final EmailClientConnectionManager connectionManager;
        // We only cache the connection manager for persisted HostAuth objects, i.e. objects
        // whose ids are permanent and won't get reused by other transient HostAuth objects.
        if (hostAuth.isSaved()) {
            connectionManager = getCachedConnectionManager(context, hostAuth);
        } else {
            connectionManager = createConnectionManager(context, hostAuth);
        }
        return connectionManager;
    }

    /**
     * Remove a connection manager from the cache. This is necessary when a {@link HostAuth} is
     * redirected or otherwise altered. It's not strictly necessary but good to also call this
     * when a {@link HostAuth} is deleted, i.e. when an account is removed.
     * @param hostAuth The {@link HostAuth} whose connection manager should be deleted.
     */
    public synchronized void uncacheConnectionManager(final HostAuth hostAuth) {
        LogUtils.d(Eas.LOG_TAG, "Uncaching connection manager for HostAuth %d", hostAuth.mId);
        EmailClientConnectionManager connectionManager = mConnectionMap.get(hostAuth.mId);
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
        mConnectionMap.remove(hostAuth.mId);
        mConnectionCreationTimes.remove(hostAuth.mId);
    }
}
