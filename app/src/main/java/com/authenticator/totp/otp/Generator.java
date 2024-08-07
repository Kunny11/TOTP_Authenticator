package com.authenticator.totp.otp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Generator {

    private static final int[] DIGITS_POWER = {
            1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
    };

    public String generateOTP(String base32Seed, int otpLength, int timeStep, String algorithm) {
        try {
/*
            if (!algorithm.equalsIgnoreCase("HmacSHA1") &&
                    !algorithm.equalsIgnoreCase("HmacSHA256") &&
                    !algorithm.equalsIgnoreCase("HmacSHA512")) {
                throw new NoSuchAlgorithmException("Invalid algorithm: " + algorithm);
            }
*/

            long counter = System.currentTimeMillis() / (timeStep * 1000); // Convert milliseconds to seconds
            byte[] counterBytes = longToBytes(counter);

            byte[] keyBytes = base32Decode(base32Seed);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, algorithm);
            Mac hmac = Mac.getInstance(algorithm);
            hmac.init(keySpec);

            byte[] hash = hmac.doFinal(counterBytes);
            int offset = hash[hash.length - 1] & 0xF;
            int truncatedHash = hashToInt(hash, offset) & 0x7FFFFFFF;
            int pinValue = truncatedHash % DIGITS_POWER[otpLength];
            return padOutput(pinValue, otpLength);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "";
        }
    }

    private int hashToInt(byte[] bytes, int start) {
        int val = 0;
        for (int i = 0; i < 4; i++) {
            val <<= 8;
            val |= bytes[start + i] & 0xFF;
        }
        return val;
    }

    private String padOutput(int value, int otpLength) {
        String result = Integer.toString(value);
        while (result.length() < otpLength) {
            result = "0" + result;
        }
        return result;
    }

    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private byte[] base32Decode(String base32Seed) {
        String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        base32Seed = base32Seed.toUpperCase();
        byte[] bytes = new byte[base32Seed.length() * 5 / 8];
        int buffer = 0, next = 0, bitsLeft = 0;
        for (char c : base32Seed.toCharArray()) {
            int index = base32Chars.indexOf(c);
            buffer <<= 5;
            buffer |= index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[next++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return bytes;
    }

}