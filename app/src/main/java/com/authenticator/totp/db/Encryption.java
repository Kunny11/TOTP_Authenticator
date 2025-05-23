package com.authenticator.totp.db;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class Encryption {

    private static final String KEY_ALIAS = "otp_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;

    // Method for generating key and storing it in the Android Keystore
    public static void generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        keyGenerator.generateKey();
    }

    //Encrypts data
    public static String encrypt(String data) throws Exception {
        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        byte[] cipherText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        byte[] encryptedData = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, encryptedData, 0, iv.length);
        System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);

        String encryptedBase64 = Base64.encodeToString(encryptedData, Base64.DEFAULT);

        // Log encryption output
        Log.d("Encryption", "Generated IV: " + Base64.encodeToString(iv, Base64.DEFAULT));
        Log.d("Encryption", "Generated CipherText: " + Base64.encodeToString(cipherText, Base64.DEFAULT));
        Log.d("Encryption", "Final Encrypted Data: " + encryptedBase64);

        return encryptedBase64;
    }

    //Decrypts data
    public static String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            throw new IllegalArgumentException("Encrypted data is null or empty");
        }

        // Debugging log
        Log.d("checky","Encrypted Data: " + encryptedData);

        byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);

        if (decodedData.length < 12) {
            throw new IllegalArgumentException("Invalid encrypted data length");
        }

        SecretKey key = getSecretKey();

        byte[] iv = new byte[12]; // GCM IV length
        System.arraycopy(decodedData, 0, iv, 0, iv.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] cipherText = new byte[decodedData.length - iv.length];
        System.arraycopy(decodedData, iv.length, cipherText, 0, cipherText.length);

        Log.d("Decryption", "IV: " + Base64.encodeToString(iv, Base64.DEFAULT));
        Log.d("Decryption", "CipherText: " + Base64.encodeToString(cipherText, Base64.DEFAULT));

        byte[] plainText = cipher.doFinal(cipherText);
        String decryptedText = new String(plainText, StandardCharsets.UTF_8);
        Log.d("Decryption", "Decrypted Text: " + decryptedText);

        return decryptedText;
    }

    // Retrieve the secret key from Keystore
    private static SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return ((SecretKey) keyStore.getKey(KEY_ALIAS, null));
    }
}
