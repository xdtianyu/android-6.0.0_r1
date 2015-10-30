/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.net.http.cts;

import android.net.http.X509TrustManagerExtensions;
import android.util.Base64;

import java.io.File;
import java.io.ByteArrayInputStream;

import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import junit.framework.TestCase;

import com.android.org.conscrypt.TrustedCertificateStore;
import com.android.org.conscrypt.TrustManagerImpl;

public class X509TrustManagerExtensionsTest extends TestCase {

    public void testIsUserAddedCert() throws Exception {
        final String testCert =
            "MIICfjCCAeegAwIBAgIJAMefIzKHY5H4MA0GCSqGSIb3DQEBBQUAMFgxCzAJBgNV" +
            "BAYTAlVTMQswCQYDVQQIDAJDQTEWMBQGA1UEBwwNTW91bnRhaW4gVmlldzEPMA0G" +
            "A1UECgwGR2V3Z3VsMRMwEQYDVQQDDApnZXdndWwuY29tMB4XDTEzMTEwNTAwNDE0" +
            "MFoXDTEzMTIwNTAwNDE0MFowWDELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAkNBMRYw" +
            "FAYDVQQHDA1Nb3VudGFpbiBWaWV3MQ8wDQYDVQQKDAZHZXdndWwxEzARBgNVBAMM" +
            "Cmdld2d1bC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKpc/I0Ss4sm" +
            "yV2iX5xRMM7+XXAhiWrceGair4MpvDrGIa1kFj2phtx4IqTfDnNU7AhRJYkDYmJQ" +
            "fUJ8i6F+I08uNiGVO4DtPJbZcBXg9ME9EMaJCslm995ueeNWSw1Ky8zM0tt4p+94" +
            "BcXJ7PC3N2WgkvtE8xwNbaeUfhGPzJKXAgMBAAGjUDBOMB0GA1UdDgQWBBQQ/iW7" +
            "JCkSI2sbn4nTBiZ9PSiO8zAfBgNVHSMEGDAWgBQQ/iW7JCkSI2sbn4nTBiZ9PSiO" +
            "8zAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4GBABQBrUOWTCSIl3vkRR3w" +
            "3bPzh3BpqDmxH9xe4rZr+MVKKjpGjY1z2m2EEtyNz3tbgVQym5+si00DUHFL0IP1" +
            "SuRULmPyEpTBVbV+PA5Kc967ZcDgYt4JtdMcCeKbIFaU6r8oEYEL2PTlNZmgbunM" +
            "pXktkhVvNxZeSa8yM9bPhXkN";

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)cf.generateCertificate(
            new ByteArrayInputStream(Base64.decode(testCert, Base64.DEFAULT)));

        // Test without adding cert to keystore.
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        X509TrustManagerExtensions tmeNegative =
            new X509TrustManagerExtensions(new TrustManagerImpl(keyStore));
        assertEquals(false, tmeNegative.isUserAddedCertificate(cert));

        // Test with cert added to keystore.
        final File DIR_TEMP = new File(System.getProperty("java.io.tmpdir"));
        final File DIR_TEST = new File(DIR_TEMP, "test");
        final File system = new File(DIR_TEST, "system-test");
        final File added = new File(DIR_TEST, "added-test");
        final File deleted = new File(DIR_TEST, "deleted-test");

        TrustedCertificateStore tcs = new TrustedCertificateStore(system, added, deleted);
        added.mkdirs();
        tcs.installCertificate(cert);
        X509TrustManagerExtensions tmePositive =
            new X509TrustManagerExtensions(new TrustManagerImpl(keyStore, null, tcs));
        assertEquals(true, tmePositive.isUserAddedCertificate(cert));
    }
}
