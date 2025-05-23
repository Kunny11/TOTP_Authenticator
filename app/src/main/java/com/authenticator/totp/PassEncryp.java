package com.authenticator.totp;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PassEncryp {

    // Constants for PBKDF2
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 10000;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // AES/GCM Encryption Settings
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;  // Standard IV length for GCM (12 bytes)
    private static final int SALT_LENGTH = 16;
    private static final int TAG_LENGTH = 128;

    // Key Deriving Method
    public static SecretKey deriveKeyFromPassword(String password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    // Encryption Method
    public static String encryptContent(String content, String password) throws GeneralSecurityException {
        byte[] salt = generateSaltBytes();

        SecretKey secretKey = deriveKeyFromPassword(password, salt);

        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

        byte[] encryptedContent = new byte[SALT_LENGTH + IV_LENGTH + encryptedBytes.length];

        System.arraycopy(salt, 0, encryptedContent, 0, SALT_LENGTH);
        System.arraycopy(iv, 0, encryptedContent, SALT_LENGTH, IV_LENGTH);
        System.arraycopy(encryptedBytes, 0, encryptedContent, SALT_LENGTH + IV_LENGTH, encryptedBytes.length);

        return Base64.encodeToString(encryptedContent, Base64.DEFAULT);
    }

    // Decryption Method
    public static String decryptContent(byte[] encryptedContent, String password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LENGTH];
        System.arraycopy(encryptedContent, 0, salt, 0, SALT_LENGTH);

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(encryptedContent, SALT_LENGTH, iv, 0, IV_LENGTH);

        byte[] cipherText = new byte[encryptedContent.length - SALT_LENGTH - IV_LENGTH];
        System.arraycopy(encryptedContent, SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherText.length);

        SecretKey secretKey = deriveKeyFromPassword(password, salt);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        byte[] decryptedBytes = cipher.doFinal(cipherText);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // Method to generate a random salt
    public static byte[] generateSaltBytes() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }
}
