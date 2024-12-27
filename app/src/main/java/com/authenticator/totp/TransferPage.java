package com.authenticator.totp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.authenticator.totp.db.OtpDatabaseHelper;
import com.google.authenticator.migration.MigrationPayloadWrapper.MigrationPayload;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferPage extends AppCompatActivity {

    private static final String TAG = "TransferPage";
    private OtpDatabaseHelper otpDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transfer_page);

        otpDatabaseHelper = new OtpDatabaseHelper(this);

        List<OtpInfo> otpInfoList = otpDatabaseHelper.getAllOtpInfo();
        if (otpInfoList == null || otpInfoList.isEmpty()) {
            Toast.makeText(this, "No OTP details to transfer", Toast.LENGTH_SHORT).show();
            return;
        }

        String qrContent = generateMigrationQrContent(otpInfoList);
        if (qrContent.isEmpty()) {
            Toast.makeText(this, "Error generating QR code content", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageView qrCodeImageView = findViewById(R.id.qr_code_image);
        try {
            int qrCodeSize = Math.max(300, Math.min(900, qrContent.length() * 10)); // Dynamically set size
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // Medium error correction

            BitMatrix bitMatrix = new QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize, hints);
            qrCodeImageView.setImageBitmap(toBitmap(bitMatrix));
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code: " + e.getMessage(), e);
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateMigrationQrContent(List<OtpInfo> otpInfoList) {
        MigrationPayload.Builder migrationBuilder = MigrationPayload.newBuilder();

        for (OtpInfo otpInfo : otpInfoList) {
            MigrationPayload.OTPParameters.Builder otpParametersBuilder = MigrationPayload.OTPParameters.newBuilder();

            try {
                String accountName = otpInfo.getAccountName();
                String issuer = otpInfo.getIssuer();
                String base32Secret = otpInfo.getSecret();

                if (accountName == null || issuer == null || base32Secret == null || base32Secret.isEmpty()) {
                    Log.e(TAG, "Skipping invalid OTP entry: " + otpInfo);
                    continue;
                }

                byte[] secretBytes = base32Decode(base32Secret);

                otpParametersBuilder
                        .setIssuer(issuer)
                        .setAccountName(accountName)
                        .setSecret(ByteString.copyFrom(secretBytes))
                        .setAlgorithm(getAlgorithmEnum(otpInfo.getAlgorithm()))
                        .setDigits(getDigitCountEnum(otpInfo.getOtpLength()))
                        .setType(MigrationPayload.OTPParameters.OtpType.OTP_TYPE_TOTP)
                        .setCounter(0);

                migrationBuilder.addOtpParameters(otpParametersBuilder);
            } catch (Exception e) {
                Log.e(TAG, "Error processing OTP for account: " + otpInfo.getAccountName(), e);
            }
        }

        MigrationPayload migrationPayload = migrationBuilder.build();
        String encodedPayload = Base64.encodeToString(migrationPayload.toByteArray(), Base64.NO_WRAP);

        // Construct the URI
        Uri exportUri = new Uri.Builder()
                .scheme("otpauth-migration")
                .authority("offline")
                .appendQueryParameter("data", encodedPayload)
                .build();

        return exportUri.toString();
    }

    private MigrationPayload.OTPParameters.Algorithm getAlgorithmEnum(String algorithm) {
        switch (algorithm.trim()) {
            case "HmacSHA1":
            case "HMACSHA1":
            case "SHA1":
                return MigrationPayload.OTPParameters.Algorithm.ALGORITHM_SHA1;
            case "HmacSHA256":
            case "HMACSHA256":
            case "SHA256":
                return MigrationPayload.OTPParameters.Algorithm.ALGORITHM_SHA256;
            case "HmacSHA512":
            case "HMACSHA512":
            case "SHA512":
                return MigrationPayload.OTPParameters.Algorithm.ALGORITHM_SHA512;
            case "MD5":
                return MigrationPayload.OTPParameters.Algorithm.ALGORITHM_MD5;
            default:
                Log.e(TAG, "Unsupported algorithm: " + algorithm);
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    private MigrationPayload.OTPParameters.DigitCount getDigitCountEnum(int otpLength) {
        switch (otpLength) {
            case 6:
                return MigrationPayload.OTPParameters.DigitCount.DIGIT_COUNT_SIX;
            case 8:
                return MigrationPayload.OTPParameters.DigitCount.DIGIT_COUNT_EIGHT;
            default:
                Log.e(TAG, "Unsupported OTP length: " + otpLength);
                throw new IllegalArgumentException("Unsupported OTP length: " + otpLength);
        }
    }

    private MigrationPayload.OTPParameters.OtpType getOtpTypeEnum(String type) {
        switch (type.trim().toUpperCase()) {
            case "TOTP":
                return MigrationPayload.OTPParameters.OtpType.OTP_TYPE_TOTP;
            case "HOTP":
                return MigrationPayload.OTPParameters.OtpType.OTP_TYPE_HOTP;
            default:
                Log.e(TAG, "Unsupported OTP type: " + type);
                throw new IllegalArgumentException("Unsupported OTP type: " + type);
        }
    }

    private Bitmap toBitmap(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
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
