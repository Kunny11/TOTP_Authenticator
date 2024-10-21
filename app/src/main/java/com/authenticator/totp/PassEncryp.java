package com.authenticator.totp;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PassEncryp {

    // Constants for PBKDF2
    private static final int KEY_LENGTH = 256; // AES-256 key length
    private static final int ITERATION_COUNT = 10000; //Iteration count for security(Adjustable)
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // AES Encryption Settings
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;
    private static final int SALT_LENGTH = 16;

    //Key Deriving Method
    public static SecretKey deriveKeyFromPassword(String password, byte[] salt, int iterations) throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);

        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    //Encryption Method
    public static byte[] encryptContent(String content, String password) throws GeneralSecurityException {
        byte[] salt = generateSaltBytes();

        SecretKey secretKey = deriveKeyFromPassword(password, salt, ITERATION_COUNT);

        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        try {
            byte[] encryptedBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedContent = new byte[SALT_LENGTH + 4 + IV_LENGTH + encryptedBytes.length];

            System.arraycopy(salt, 0, encryptedContent, 0, SALT_LENGTH);
            System.arraycopy(intToByteArray(ITERATION_COUNT), 0, encryptedContent, SALT_LENGTH, 4);
            System.arraycopy(iv, 0, encryptedContent, SALT_LENGTH + 4, IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, encryptedContent, SALT_LENGTH + 4 + IV_LENGTH, encryptedBytes.length);

            return encryptedContent;
        } catch (Exception e) {
            throw new GeneralSecurityException("Error encrypting content", e);
        }
    }

    //Decryption Method
    public static String decryptContent(byte[] encryptedContent, String password) throws GeneralSecurityException {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(encryptedContent, 0, salt, 0, SALT_LENGTH);

            byte[] iterationBytes = new byte[4];
            System.arraycopy(encryptedContent, SALT_LENGTH, iterationBytes, 0, 4);
            int iterations = byteArrayToInt(iterationBytes);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encryptedContent, SALT_LENGTH + 4, iv, 0, IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] cipherText = new byte[encryptedContent.length - SALT_LENGTH - 4 - IV_LENGTH];
            System.arraycopy(encryptedContent, SALT_LENGTH + 4 + IV_LENGTH, cipherText, 0, cipherText.length);

            SecretKey secretKey = deriveKeyFromPassword(password, salt, iterations);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherText);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new GeneralSecurityException("Error decrypting content", e);
        }
    }

    //Method to generate a random salt
    public static byte[] generateSaltBytes() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    //Method to convert int to byte array (for storing iteration count)
    private static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value
        };
    }

    //Method to convert byte array to int (for reading iteration count)
    private static int byteArrayToInt(byte[] bytes) {
        return   bytes[0] << 24
                | (bytes[1] & 0xFF) << 16
                | (bytes[2] & 0xFF) << 8
                | (bytes[3] & 0xFF);
    }
}
