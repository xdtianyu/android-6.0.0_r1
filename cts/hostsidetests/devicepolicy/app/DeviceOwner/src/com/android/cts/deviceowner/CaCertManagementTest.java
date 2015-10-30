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

import static com.android.cts.deviceowner.FakeKeys.FAKE_RSA_1;
import static com.android.cts.deviceowner.FakeKeys.FAKE_DSA_1;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.util.List;

public class CaCertManagementTest extends BaseDeviceOwnerTest {
    public void testCanRetrieveListOfInstalledCaCerts() {
        List<byte[]> caCerts = mDevicePolicyManager.getInstalledCaCerts(getWho());
        assertNotNull(caCerts);
    }

    public void testCanInstallAndUninstallACaCert()
    throws CertificateException {
        assertFalse(hasCaCertInstalled(FAKE_RSA_1.caCertificate));
        assertFalse(hasCaCertInstalled(FAKE_DSA_1.caCertificate));
        assertTrue(mDevicePolicyManager.installCaCert(getWho(), FAKE_RSA_1.caCertificate));
        assertTrue(hasCaCertInstalled(FAKE_RSA_1.caCertificate));
        assertFalse(hasCaCertInstalled(FAKE_DSA_1.caCertificate));
        mDevicePolicyManager.uninstallCaCert(getWho(), FAKE_RSA_1.caCertificate);
        assertFalse(hasCaCertInstalled(FAKE_RSA_1.caCertificate));
        assertFalse(hasCaCertInstalled(FAKE_DSA_1.caCertificate));
    }

    public void testUninstallationIsSelective() throws CertificateException {
        assertTrue(mDevicePolicyManager.installCaCert(getWho(), FAKE_RSA_1.caCertificate));
        assertTrue(mDevicePolicyManager.installCaCert(getWho(), FAKE_DSA_1.caCertificate));
        mDevicePolicyManager.uninstallCaCert(getWho(), FAKE_DSA_1.caCertificate);
        assertTrue(hasCaCertInstalled(FAKE_RSA_1.caCertificate));
        assertFalse(hasCaCertInstalled(FAKE_DSA_1.caCertificate));
        mDevicePolicyManager.uninstallCaCert(getWho(), FAKE_RSA_1.caCertificate);
    }

    public void testCanUninstallAllUserCaCerts() throws CertificateException {
        assertTrue(mDevicePolicyManager.installCaCert(getWho(), FAKE_RSA_1.caCertificate));
        assertTrue(mDevicePolicyManager.installCaCert(getWho(), FAKE_DSA_1.caCertificate));
        mDevicePolicyManager.uninstallAllUserCaCerts(getWho());
        assertFalse(hasCaCertInstalled(FAKE_RSA_1.caCertificate));
        assertFalse(hasCaCertInstalled(FAKE_DSA_1.caCertificate));
    }

    private boolean hasCaCertInstalled(byte [] caCert) throws CertificateException {
        boolean result = mDevicePolicyManager.hasCaCertInstalled(getWho(), caCert);
        assertEquals(result, containsCertificate(
            mDevicePolicyManager.getInstalledCaCerts(getWho()), caCert));
        return result;
    }

    private static boolean containsCertificate(List<byte[]> certificates, byte [] toMatch)
            throws CertificateException {
        Certificate certificateToMatch = readCertificate(toMatch);
        for (byte[] certBuffer : certificates) {
            Certificate cert = readCertificate(certBuffer);
            if (certificateToMatch.equals(cert)) {
                return true;
            }
        }
        return false;
    }

    private static Certificate readCertificate(byte[] certBuffer) throws CertificateException {
        final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return certFactory.generateCertificate(new ByteArrayInputStream(certBuffer));
    }
}
