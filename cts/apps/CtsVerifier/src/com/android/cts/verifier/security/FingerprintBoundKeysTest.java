/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.cts.verifier.security;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class FingerprintBoundKeysTest extends PassFailButtons.Activity {
    private static final String TAG = "FingerprintBoundKeysTest";

    /** Alias for our key in the Android Key Store. */
    private static final String KEY_NAME = "my_key";
    private static final byte[] SECRET_BYTE_ARRAY = new byte[] {1, 2, 3, 4, 5, 6};
    private static final int AUTHENTICATION_DURATION_SECONDS = 5;
    private static final int CONFIRM_CREDENTIALS_REQUEST_CODE = 1;
    private static final int FINGERPRINT_PERMISSION_REQUEST_CODE = 0;

    private FingerprintManager mFingerprintManager;
    private KeyguardManager mKeyguardManager;
    private FingerprintAuthDialogFragment mFingerprintDialog;
    private Cipher mCipher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sec_screen_lock_keys_main);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.sec_fingerprint_bound_key_test, R.string.sec_fingerprint_bound_key_test_info, -1);
        getPassButton().setEnabled(false);
        requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT},
                FINGERPRINT_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] state) {
        if (requestCode == FINGERPRINT_PERMISSION_REQUEST_CODE && state[0] == PackageManager.PERMISSION_GRANTED) {
            mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
            mKeyguardManager = (KeyguardManager) getSystemService(KeyguardManager.class);

            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);
                mCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

                mCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            } catch (KeyPermanentlyInvalidatedException e) {
                createKey();
                showToast("The key has been invalidated, please try again.\n");
            } catch (NoSuchPaddingException | KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                    | NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("Failed to init Cipher", e);
            }

            Button startTestButton = (Button) findViewById(R.id.sec_start_test_button);
            startTestButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tryEncrypt()) {
                        showToast("Test failed. Key accessible without auth.");
                    } else {
                        showAuthenticationScreen();
                    }
                }

            });

            if (!mKeyguardManager.isKeyguardSecure()) {
                // Show a message that the user hasn't set up a lock screen.
                showToast( "Secure lock screen hasn't been set up.\n"
                                + "Go to 'Settings -> Security -> Screen lock' to set up a lock screen");
                startTestButton.setEnabled(false);
            } else if (!mFingerprintManager.hasEnrolledFingerprints()) {
                showToast("No fingerprints enrolled.\n"
                                + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint");
                startTestButton.setEnabled(false);
            } else {
                createKey();
            }
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with device credentials within the last X seconds.
     */
    private void createKey() {
        // Generate a key to decrypt payment credentials, tokens, etc.
        // This will most likely be a registration step for the user when they are setting up your app.
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | KeyStoreException
                | CertificateException | IOException e) {
            throw new RuntimeException("Failed to create a symmetric key", e);
        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which is
     * only works if the user has just authenticated via device credentials.
     */
    private boolean tryEncrypt() {
        try {
            mCipher.doFinal(SECRET_BYTE_ARRAY);
            return true;
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            return false;
        }
    }

    private void showAuthenticationScreen() {
        mFingerprintDialog = new FingerprintAuthDialogFragment();
        mFingerprintDialog.show(getFragmentManager(), "fingerprint_dialog");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG)
            .show();
    }

    public class FingerprintAuthDialogFragment extends DialogFragment {

        private CancellationSignal mCancellationSignal;
        private FingerprintManagerCallback mFingerprintManagerCallback;
        private boolean mSelfCancelled;

        class FingerprintManagerCallback extends FingerprintManager.AuthenticationCallback {
            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                if (!mSelfCancelled) {
                    showToast(errString.toString());
                }
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                showToast(helpString.toString());
            }

            @Override
            public void onAuthenticationFailed() {
                showToast(getString(R.string.sec_fp_auth_failed));
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                if (tryEncrypt()) {
                    showToast("Test passed.");
                    getPassButton().setEnabled(true);
                    FingerprintAuthDialogFragment.this.dismiss();
                } else {
                    showToast("Test failed. Key not accessible after auth");
                }
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            mCancellationSignal.cancel();
            mSelfCancelled = true;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mCancellationSignal = new CancellationSignal();
            mSelfCancelled = false;
            mFingerprintManagerCallback = new FingerprintManagerCallback();
            mFingerprintManager.authenticate(new FingerprintManager.CryptoObject(mCipher),
                    mCancellationSignal, 0, mFingerprintManagerCallback, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.sec_fp_dialog_message);
            return builder.create();
        }

    }
}


