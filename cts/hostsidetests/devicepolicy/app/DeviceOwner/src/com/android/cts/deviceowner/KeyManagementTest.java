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
package com.android.cts.deviceowner;

import static com.android.cts.deviceowner.BaseDeviceOwnerTest.getWho;
import static com.android.cts.deviceowner.FakeKeys.FAKE_RSA_1;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.net.Uri;
import android.os.Handler;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.test.ActivityInstrumentationTestCase2;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.ComponentName;
import android.content.Context;

public class KeyManagementTest extends
        ActivityInstrumentationTestCase2<KeyManagementActivity> {

    private static final int KEYCHAIN_TIMEOUT_MS = 8000;
    private DevicePolicyManager mDevicePolicyManager;

    public KeyManagementTest() {
        super(KeyManagementActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Confirm our DeviceOwner is set up
        mDevicePolicyManager = (DevicePolicyManager)
                getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        BaseDeviceOwnerTest.assertDeviceOwner(mDevicePolicyManager);

        // Enable credential storage by setting a nonempty password.
        assertTrue(mDevicePolicyManager.resetPassword("test", 0));
    }

    @Override
    protected void tearDown() throws Exception {
        // Delete all keys by resetting our password to null, which clears the keystore.
        mDevicePolicyManager.setPasswordQuality(getWho(),
                DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
        mDevicePolicyManager.setPasswordMinimumLength(getWho(), 0);
        assertTrue(mDevicePolicyManager.resetPassword("", 0));
        super.tearDown();
    }

    public void testCanInstallValidRsaKeypair()
            throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException,
                    KeyChainException, InterruptedException, UnsupportedEncodingException {
        final String alias = "com.android.test.valid-rsa-key-1";
        final PrivateKey privKey = getPrivateKey(FAKE_RSA_1.privateKey , "RSA");
        final Certificate cert = getCertificate(FAKE_RSA_1.caCertificate);
        assertTrue(mDevicePolicyManager.installKeyPair(getWho(), privKey, cert, alias));

        assertEquals(alias, new KeyChainAliasFuture(alias).get());
        final PrivateKey retrievedKey = KeyChain.getPrivateKey(getActivity(), alias);
        assertEquals(retrievedKey.getAlgorithm(), "RSA");
    }

    public void testNullKeyParamsFailPredictably()
            throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
        final String alias = "com.android.test.null-key-1";
        final PrivateKey privKey = getPrivateKey(FAKE_RSA_1.privateKey, "RSA");
        final Certificate cert = getCertificate(FAKE_RSA_1.caCertificate);
        try {
            mDevicePolicyManager.installKeyPair(getWho(), null, cert, alias);
            fail("Exception should have been thrown for null PrivateKey");
        } catch (NullPointerException expected) {
        }
        try {
            mDevicePolicyManager.installKeyPair(getWho(), privKey, null, alias);
            fail("Exception should have been thrown for null Certificate");
        } catch (NullPointerException expected) {
        }
    }

    public void testNullAdminComponentIsDenied()
            throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
        final String alias = "com.android.test.null-admin-1";
        final PrivateKey privKey = getPrivateKey(FAKE_RSA_1.privateKey, "RSA");
        final Certificate cert = getCertificate(FAKE_RSA_1.caCertificate);
        try {
            mDevicePolicyManager.installKeyPair(null, privKey, cert, alias);
            fail("Exception should have been thrown for null ComponentName");
        } catch (SecurityException expected) {
        }
    }

    private static PrivateKey getPrivateKey(final byte[] key, String type)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance(type).generatePrivate(
                new PKCS8EncodedKeySpec(key));
    }

    private static Certificate getCertificate(byte[] cert) throws CertificateException {
        return CertificateFactory.getInstance("X.509").generateCertificate(
                new ByteArrayInputStream(cert));
    }

    private class KeyChainAliasFuture implements KeyChainAliasCallback {
        private final CountDownLatch mLatch = new CountDownLatch(1);
        private String mChosenAlias = null;

        @Override
        public void alias(final String chosenAlias) {
            mChosenAlias = chosenAlias;
            mLatch.countDown();
        }

        public KeyChainAliasFuture(String alias) throws UnsupportedEncodingException {
            /* Pass the alias as a GET to an imaginary server instead of explicitly asking for it,
             * to make sure the DPC actually has to do some work to grant the cert.
             */
            final Uri uri =
                    Uri.parse("https://example.org/?alias=" + URLEncoder.encode(alias, "UTF-8"));
            KeyChain.choosePrivateKeyAlias(getActivity(), this,
                    null /* keyTypes */, null /* issuers */, uri, null /* alias */);
        }

        public String get() throws InterruptedException {
            assertTrue("Chooser timeout", mLatch.await(KEYCHAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            return mChosenAlias;
        }
    };
}
