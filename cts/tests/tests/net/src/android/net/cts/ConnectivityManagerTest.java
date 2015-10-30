/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.net.cts;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.test.AndroidTestCase;
import android.util.Log;
import android.os.SystemProperties;

import com.android.internal.telephony.PhoneConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ConnectivityManagerTest extends AndroidTestCase {

    private static final String TAG = ConnectivityManagerTest.class.getSimpleName();

    private static final String FEATURE_ENABLE_HIPRI = "enableHIPRI";

    public static final int TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE;
    public static final int TYPE_WIFI = ConnectivityManager.TYPE_WIFI;
    private static final int HOST_ADDRESS = 0x7f000001;// represent ip 127.0.0.1

    // Action sent to ConnectivityActionReceiver when a network callback is sent via PendingIntent.
    private static final String NETWORK_CALLBACK_ACTION =
            "ConnectivityManagerTest.NetworkCallbackAction";

    // device could have only one interface: data, wifi.
    private static final int MIN_NUM_NETWORK_TYPES = 1;

    private ConnectivityManager mCm;
    private WifiManager mWifiManager;
    private PackageManager mPackageManager;
    private final HashMap<Integer, NetworkConfig> mNetworks =
            new HashMap<Integer, NetworkConfig>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        mPackageManager = getContext().getPackageManager();

        // Get com.android.internal.R.array.networkAttributes
        int resId = getContext().getResources().getIdentifier("networkAttributes", "array", "android");
        String[] naStrings = getContext().getResources().getStringArray(resId);
        //TODO: What is the "correct" way to determine if this is a wifi only device?
        boolean wifiOnly = SystemProperties.getBoolean("ro.radio.noril", false);
        for (String naString : naStrings) {
            try {
                NetworkConfig n = new NetworkConfig(naString);
                if (wifiOnly && ConnectivityManager.isNetworkTypeMobile(n.type)) {
                    continue;
                }
                mNetworks.put(n.type, n);
            } catch (Exception e) {}
        }
    }

    public void testIsNetworkTypeValid() {
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_WIFI));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE_MMS));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE_SUPL));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE_DUN));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE_HIPRI));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_WIMAX));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_BLUETOOTH));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_DUMMY));
        assertTrue(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_ETHERNET));
        assertTrue(mCm.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE_FOTA));
        assertTrue(mCm.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE_IMS));
        assertTrue(mCm.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE_CBS));
        assertTrue(mCm.isNetworkTypeValid(ConnectivityManager.TYPE_WIFI_P2P));
        assertTrue(mCm.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE_IA));
        assertFalse(mCm.isNetworkTypeValid(-1));
        assertTrue(mCm.isNetworkTypeValid(0));
        assertTrue(mCm.isNetworkTypeValid(ConnectivityManager.MAX_NETWORK_TYPE));
        assertFalse(ConnectivityManager.isNetworkTypeValid(ConnectivityManager.MAX_NETWORK_TYPE+1));

        NetworkInfo[] ni = mCm.getAllNetworkInfo();

        for (NetworkInfo n: ni) {
            assertTrue(ConnectivityManager.isNetworkTypeValid(n.getType()));
        }

    }

    public void testSetNetworkPreference() {
        // getNetworkPreference() and setNetworkPreference() are both deprecated so they do
        // not preform any action.  Verify they are at least still callable.
        mCm.setNetworkPreference(mCm.getNetworkPreference());
    }

    public void testGetActiveNetworkInfo() {
        NetworkInfo ni = mCm.getActiveNetworkInfo();

        assertNotNull("You must have an active network connection to complete CTS", ni);
        assertTrue(ConnectivityManager.isNetworkTypeValid(ni.getType()));
        assertTrue(ni.getState() == State.CONNECTED);
    }

    public void testGetActiveNetwork() {
        Network network = mCm.getActiveNetwork();
        assertNotNull("You must have an active network connection to complete CTS", network);

        NetworkInfo ni = mCm.getNetworkInfo(network);
        assertNotNull("Network returned from getActiveNetwork was invalid", ni);

        // Similar to testGetActiveNetworkInfo above.
        assertTrue(ConnectivityManager.isNetworkTypeValid(ni.getType()));
        assertTrue(ni.getState() == State.CONNECTED);
    }

    public void testGetNetworkInfo() {
        for (int type = -1; type <= ConnectivityManager.MAX_NETWORK_TYPE+1; type++) {
            if (isSupported(type)) {
                NetworkInfo ni = mCm.getNetworkInfo(type);
                assertTrue("Info shouldn't be null for " + type, ni != null);
                State state = ni.getState();
                assertTrue("Bad state for " + type, State.UNKNOWN.ordinal() >= state.ordinal()
                           && state.ordinal() >= State.CONNECTING.ordinal());
                DetailedState ds = ni.getDetailedState();
                assertTrue("Bad detailed state for " + type,
                           DetailedState.FAILED.ordinal() >= ds.ordinal()
                           && ds.ordinal() >= DetailedState.IDLE.ordinal());
            } else {
                assertNull("Info should be null for " + type, mCm.getNetworkInfo(type));
            }
        }
    }

    public void testGetAllNetworkInfo() {
        NetworkInfo[] ni = mCm.getAllNetworkInfo();
        assertTrue(ni.length >= MIN_NUM_NETWORK_TYPES);
        for (int type = 0; type <= ConnectivityManager.MAX_NETWORK_TYPE; type++) {
            int desiredFoundCount = (isSupported(type) ? 1 : 0);
            int foundCount = 0;
            for (NetworkInfo i : ni) {
                if (i.getType() == type) foundCount++;
            }
            if (foundCount != desiredFoundCount) {
                Log.e(TAG, "failure in testGetAllNetworkInfo.  Dump of returned NetworkInfos:");
                for (NetworkInfo networkInfo : ni) Log.e(TAG, "  " + networkInfo);
            }
            assertTrue("Unexpected foundCount of " + foundCount + " for type " + type,
                    foundCount == desiredFoundCount);
        }
    }

    private void assertStartUsingNetworkFeatureUnsupported(int networkType, String feature) {
        try {
            mCm.startUsingNetworkFeature(networkType, feature);
            fail("startUsingNetworkFeature is no longer supported in the current API version");
        } catch (UnsupportedOperationException expected) {}
    }

    private void assertStopUsingNetworkFeatureUnsupported(int networkType, String feature) {
        try {
            mCm.startUsingNetworkFeature(networkType, feature);
            fail("stopUsingNetworkFeature is no longer supported in the current API version");
        } catch (UnsupportedOperationException expected) {}
    }

    private void assertRequestRouteToHostUnsupported(int networkType, int hostAddress) {
        try {
            mCm.requestRouteToHost(networkType, hostAddress);
            fail("requestRouteToHost is no longer supported in the current API version");
        } catch (UnsupportedOperationException expected) {}
    }

    public void testStartUsingNetworkFeature() {

        final String invalidateFeature = "invalidateFeature";
        final String mmsFeature = "enableMMS";
        final int failureCode = -1;
        final int wifiOnlyStartFailureCode = PhoneConstants.APN_REQUEST_FAILED;
        final int wifiOnlyStopFailureCode = -1;

        assertStartUsingNetworkFeatureUnsupported(TYPE_MOBILE, invalidateFeature);
        assertStopUsingNetworkFeatureUnsupported(TYPE_MOBILE, invalidateFeature);
        assertStartUsingNetworkFeatureUnsupported(TYPE_WIFI, mmsFeature);
    }

    private boolean isSupported(int networkType) {
        // Change-Id I02eb5f22737720095f646f8db5c87fd66da129d6 added VPN support
        // to all devices directly in software, independent of any external
        // configuration.
        return mNetworks.containsKey(networkType) ||
               (networkType == ConnectivityManager.TYPE_VPN);
    }

    public void testIsNetworkSupported() {
        for (int type = -1; type <= ConnectivityManager.MAX_NETWORK_TYPE; type++) {
            boolean supported = mCm.isNetworkSupported(type);
            if (isSupported(type)) {
                assertTrue(supported);
            } else {
                assertFalse(supported);
            }
        }
    }

    public void testRequestRouteToHost() {
        for (int type = -1 ; type <= ConnectivityManager.MAX_NETWORK_TYPE; type++) {
            assertRequestRouteToHostUnsupported(type, HOST_ADDRESS);
        }
    }

    public void testTest() {
        mCm.getBackgroundDataSetting();
    }

    /**
     * Exercises both registerNetworkCallback and unregisterNetworkCallback. This checks to
     * see if we get a callback for the TRANSPORT_WIFI transport type being available.
     *
     * <p>In order to test that a NetworkCallback occurs, we need some change in the network
     * state (either a transport or capability is now available). The most straightforward is
     * WiFi. We could add a version that uses the telephony data connection but it's not clear
     * that it would increase test coverage by much (how many devices have 3G radio but not Wifi?).
     */
    public void testRegisterNetworkCallback() {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            Log.i(TAG, "testRegisterNetworkCallback cannot execute unless device supports WiFi");
            return;
        }

        // We will register for a WIFI network being available or lost.
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        TestNetworkCallback callback = new TestNetworkCallback();
        mCm.registerNetworkCallback(request, callback);

        boolean previousWifiEnabledState = mWifiManager.isWifiEnabled();

        try {
            // Make sure WiFi is connected to an access point to start with.
            if (!previousWifiEnabledState) {
                connectToWifi();
            }

            // Now we should expect to get a network callback about availability of the wifi
            // network even if it was already connected as a state-based action when the callback
            // is registered.
            assertTrue("Did not receive NetworkCallback.onAvailable for TRANSPORT_WIFI",
                    callback.waitForAvailable());
        } catch (InterruptedException e) {
            fail("Broadcast receiver or NetworkCallback wait was interrupted.");
        } finally {
            mCm.unregisterNetworkCallback(callback);

            // Return WiFI to its original enabled/disabled state.
            if (!previousWifiEnabledState) {
                disconnectFromWifi();
            }
        }
    }

    /**
     * Tests both registerNetworkCallback and unregisterNetworkCallback similarly to
     * {@link #testRegisterNetworkCallback} except that a {@code PendingIntent} is used instead
     * of a {@code NetworkCallback}.
     */
    public void testRegisterNetworkCallback_withPendingIntent() {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            Log.i(TAG, "testRegisterNetworkCallback cannot execute unless device supports WiFi");
            return;
        }

        // Create a ConnectivityActionReceiver that has an IntentFilter for our locally defined
        // action, NETWORK_CALLBACK_ACTION.
        IntentFilter filter = new IntentFilter();
        filter.addAction(NETWORK_CALLBACK_ACTION);

        ConnectivityActionReceiver receiver = new ConnectivityActionReceiver(
                ConnectivityManager.TYPE_WIFI, NetworkInfo.State.CONNECTED);
        mContext.registerReceiver(receiver, filter);

        // Create a broadcast PendingIntent for NETWORK_CALLBACK_ACTION.
        Intent intent = new Intent(NETWORK_CALLBACK_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // We will register for a WIFI network being available or lost.
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        mCm.registerNetworkCallback(request, pendingIntent);

        boolean previousWifiEnabledState = mWifiManager.isWifiEnabled();

        try {
            // Make sure WiFi is connected to an access point to start with.
            if (!previousWifiEnabledState) {
                connectToWifi();
            }

            // Now we expect to get the Intent delivered notifying of the availability of the wifi
            // network even if it was already connected as a state-based action when the callback
            // is registered.
            assertTrue("Did not receive expected Intent " + intent + " for TRANSPORT_WIFI",
                    receiver.waitForState());
        } catch (InterruptedException e) {
            fail("Broadcast receiver or NetworkCallback wait was interrupted.");
        } finally {
            mCm.unregisterNetworkCallback(pendingIntent);
            pendingIntent.cancel();
            mContext.unregisterReceiver(receiver);

            // Return WiFI to its original enabled/disabled state.
            if (!previousWifiEnabledState) {
                disconnectFromWifi();
            }
        }
    }

    /** Enable WiFi and wait for it to become connected to a network. */
    private void connectToWifi() {
        ConnectivityActionReceiver receiver = new ConnectivityActionReceiver(
                ConnectivityManager.TYPE_WIFI, NetworkInfo.State.CONNECTED);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(receiver, filter);

        boolean connected = false;
        try {
            assertTrue(mWifiManager.setWifiEnabled(true));
            connected = receiver.waitForState();
        } catch (InterruptedException ex) {
            fail("connectToWifi was interrupted");
        } finally {
            mContext.unregisterReceiver(receiver);
        }

        assertTrue("Wifi must be configured to connect to an access point for this test.",
                connected);
    }

    /** Disable WiFi and wait for it to become disconnected from the network. */
    private void disconnectFromWifi() {
        ConnectivityActionReceiver receiver = new ConnectivityActionReceiver(
                ConnectivityManager.TYPE_WIFI, NetworkInfo.State.DISCONNECTED);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(receiver, filter);

        boolean disconnected = false;
        try {
            assertTrue(mWifiManager.setWifiEnabled(false));
            disconnected = receiver.waitForState();
        } catch (InterruptedException ex) {
            fail("disconnectFromWifi was interrupted");
        } finally {
            mContext.unregisterReceiver(receiver);
        }

        assertTrue("Wifi failed to reach DISCONNECTED state.", disconnected);
    }

    /**
     * Receiver that captures the last connectivity change's network type and state. Recognizes
     * both {@code CONNECTIVITY_ACTION} and {@code NETWORK_CALLBACK_ACTION} intents.
     */
    private class ConnectivityActionReceiver extends BroadcastReceiver {

        private final CountDownLatch mReceiveLatch = new CountDownLatch(1);

        private final int mNetworkType;
        private final NetworkInfo.State mNetState;

        ConnectivityActionReceiver(int networkType, NetworkInfo.State netState) {
            mNetworkType = networkType;
            mNetState = netState;
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            NetworkInfo networkInfo = null;

            // When receiving ConnectivityManager.CONNECTIVITY_ACTION, the NetworkInfo parcelable
            // is stored in EXTRA_NETWORK_INFO. With a NETWORK_CALLBACK_ACTION, the Network is
            // sent in EXTRA_NETWORK and we need to ask the ConnectivityManager for the NetworkInfo.
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                networkInfo = intent.getExtras()
                        .getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
                assertNotNull("ConnectivityActionReceiver expected EXTRA_NETWORK_INFO", networkInfo);
            } else if (NETWORK_CALLBACK_ACTION.equals(action)) {
                Network network = intent.getExtras()
                        .getParcelable(ConnectivityManager.EXTRA_NETWORK);
                assertNotNull("ConnectivityActionReceiver expected EXTRA_NETWORK", network);
                networkInfo = mCm.getNetworkInfo(network);
                if (networkInfo == null) {
                    // When disconnecting, it seems like we get an intent sent with an invalid
                    // Network; that is, by the time we call ConnectivityManager.getNetworkInfo(),
                    // it is invalid. Ignore these.
                    Log.i(TAG, "ConnectivityActionReceiver NETWORK_CALLBACK_ACTION ignoring "
                            + "invalid network");
                    return;
                }
            } else {
                fail("ConnectivityActionReceiver received unxpected intent action: " + action);
            }

            assertNotNull("ConnectivityActionReceiver didn't find NetworkInfo", networkInfo);
            int networkType = networkInfo.getType();
            State networkState = networkInfo.getState();
            Log.i(TAG, "Network type: " + networkType + " state: " + networkState);
            if (networkType == mNetworkType && networkInfo.getState() == mNetState) {
                mReceiveLatch.countDown();
            }
        }

        public boolean waitForState() throws InterruptedException {
            return mReceiveLatch.await(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Callback used in testRegisterNetworkCallback that allows caller to block on
     * {@code onAvailable}.
     */
    private static class TestNetworkCallback extends ConnectivityManager.NetworkCallback {
        private final CountDownLatch mAvailableLatch = new CountDownLatch(1);

        public boolean waitForAvailable() throws InterruptedException {
            return mAvailableLatch.await(30, TimeUnit.SECONDS);
        }

        @Override
        public void onAvailable(Network network) {
            mAvailableLatch.countDown();
        }
    }
}
