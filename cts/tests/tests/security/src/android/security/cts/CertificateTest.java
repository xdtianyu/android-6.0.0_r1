/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.security.cts;

import android.content.res.AssetManager;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CertificateTest extends InstrumentationTestCase {

    public void testNoRemovedCertificates() throws Exception {
        Set<String> expectedCertificates = new HashSet<String>(
                Arrays.asList(CertificateData.CERTIFICATE_DATA));
        Set<String> deviceCertificates = getDeviceCertificates();
        expectedCertificates.removeAll(deviceCertificates);
        assertEquals("Missing CA certificates", Collections.EMPTY_SET, expectedCertificates);
    }

    /**
     * If you fail CTS as a result of adding a root CA that is not part of the Android root CA
     * store, please see the following.
     *
     * First, this test exists because adding untrustworthy root CAs to a device has a very
     * significant security impact. In the worst case, adding a rogue CA can permanently compromise
     * the confidentiality and integrity of your users' network traffic. Because of this risk,
     * adding new certificates should be done sparingly and as a last resort -- never as a first
     * response or short term fix. Before attempting to modify this test, please consider whether
     * adding a new certificate authority is in your users' best interests.
     *
     * Second, because the addition of a new root CA by an OEM can have such dire consequences for
     * so many people it is imperative that it be done transparently and in the open. Any request to
     * modify the certificate list used by this test must have a corresponding change in AOSP
     * (one certificate per change) authored by the OEM in question and including:
     *
     *     - the certificate in question:
     *       - The certificate must be in a file under
     *         cts/tests/tests/security/assets/oem_cacerts, in PEM (Privacy-enhanced Electronic
     *         Mail) format, with the textual representation of the certificate following the PEM
     *         section.
     *       - The file name must be in the format of <hash>.<n> where "hash" is the subject hash
     *         produced by:
     *           openssl x509 -in cert_file -subject_hash -noout
     *         and the "n" is a unique integer identifier starting at 0 to deal with collisions.
     *         See OpenSSL's c_rehash manpage for details.
     *       - cts/tests/tests/security/tools/format_cert.sh helps meet the above requirements.
     *
     *     - information about who created and maintains both the certificate and the corresponding
     *       keypair.
     *
     *     - information about what the certificate is to be used for and why the certificate is
     *       appropriate for inclusion.
     *
     *     - a statement from the OEM indicating that they have sufficient confidence in the
     *       security of the key, the security practices of the issuer, and the validity of the
     *       intended use that they believe adding the certificate is not detrimental to the
     *       security of the user.
     *
     * Finally, please note that this is not the usual process for adding root CAs to Android. If
     * you have a certificate that you believe should be present on all Android devices, please file
     * a public bug at https://code.google.com/p/android/issues/entry or http://b.android.com to
     * seek resolution.
     *
     * For questions, comments, and code reviews please contact security@android.com.
     */
    public void testNoAddedCertificates() throws Exception {
        Set<String> oemWhitelistedCertificates = getOemWhitelistedCertificates();
        Set<String> expectedCertificates = new HashSet<String>(
                Arrays.asList(CertificateData.CERTIFICATE_DATA));
        Set<String> deviceCertificates = getDeviceCertificates();
        deviceCertificates.removeAll(expectedCertificates);
        deviceCertificates.removeAll(oemWhitelistedCertificates);
        assertEquals("Unknown CA certificates", Collections.EMPTY_SET, deviceCertificates);
    }

    public void testBlockCertificates() throws Exception {
        Set<String> blockCertificates = new HashSet<String>();
        blockCertificates.add("C0:60:ED:44:CB:D8:81:BD:0E:F8:6C:0B:A2:87:DD:CF:81:67:47:8C");

        Set<String> deviceCertificates = getDeviceCertificates();
        deviceCertificates.retainAll(blockCertificates);
        assertEquals("Blocked CA certificates", Collections.EMPTY_SET, deviceCertificates);
    }

    private Set<String> getDeviceCertificates() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidCAStore");
        keyStore.load(null, null);

        List<String> aliases = Collections.list(keyStore.aliases());
        assertFalse(aliases.isEmpty());

        Set<String> certificates = new HashSet<String>();
        for (String alias : aliases) {
            assertTrue(keyStore.isCertificateEntry(alias));
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            assertEquals(certificate.getSubjectUniqueID(), certificate.getIssuerUniqueID());
            assertNotNull(certificate.getSubjectDN());
            assertNotNull(certificate.getIssuerDN());
            String fingerprint = getFingerprint(certificate);
            certificates.add(fingerprint);
        }
        return certificates;
    }

    private static final String ASSETS_DIR_OEM_CERTS = "oem_cacerts";

    private Set<String> getOemWhitelistedCertificates() throws Exception {
        Set<String> certificates = new HashSet<String>();
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        AssetManager assetManager = getInstrumentation().getContext().getAssets();
        for (String path : assetManager.list(ASSETS_DIR_OEM_CERTS)) {
            File certAssetFile = new File(ASSETS_DIR_OEM_CERTS, path);
            InputStream in = null;
            try {
                in = assetManager.open(certAssetFile.toString());
                X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(in);
                certificates.add(getFingerprint(certificate));
            } catch (Exception e) {
                throw new Exception("Failed to load certificate from asset: " + certAssetFile, e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        return certificates;
    }

    private String getFingerprint(X509Certificate certificate) throws CertificateEncodingException,
            NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        messageDigest.update(certificate.getEncoded());
        byte[] sha1 = messageDigest.digest();
        return convertToHexFingerprint(sha1);
    }

    private String convertToHexFingerprint(byte[] sha1) {
        StringBuilder fingerprint = new StringBuilder();
        for (int i = 0; i < sha1.length; i++) {
            fingerprint.append(String.format("%02X", sha1[i]));
            if (i + 1 < sha1.length) {
                fingerprint.append(":");
            }
        }
        return fingerprint.toString();
    }
}
