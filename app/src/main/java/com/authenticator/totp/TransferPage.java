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
import org.apache.commons.codec.binary.Base32;

import com.google.authenticator.migration.MigrationPayloadWrapper.MigrationPayload;
import com.authenticator.totp.db.OtpDatabaseHelper;
import com.google.protobuf.ByteString;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.StandardCharsets;
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

        Log.d("infocheck", "OTP Info List: " + otpInfoList);

        String qrContent = generateMigrationQrContent(otpInfoList);
        Log.d("qr", "Generated QR Content: " + qrContent);
        if (qrContent.isEmpty()) {
            Toast.makeText(this, "Error generating QR code content", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("qr", "Generated QR Content: " + qrContent);

        ImageView qrCodeImageView = findViewById(R.id.qr_code_image);
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

            BitMatrix bitMatrix = new QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, 900, 900);
            qrCodeImageView.setImageBitmap(toBitmap(bitMatrix));
        } catch (WriterException e) {
            Log.e("TAG", "Error generating QR code: " + e.getMessage(), e);
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateMigrationQrContent(List<OtpInfo> otpInfoList) {
        MigrationPayload.Builder migrationBuilder = MigrationPayload.newBuilder();
        Base32 base32 = new Base32();

        for (OtpInfo otpInfo : otpInfoList) {
            MigrationPayload.OTPParameters.Builder otpParametersBuilder = MigrationPayload.OTPParameters.newBuilder();

            Log.d(TAG, "Processing OTP Entry: " + otpInfo.toString());

            try {
                MigrationPayload.OTPParameters.Algorithm algorithmEnum = getAlgorithmEnum(otpInfo.getAlgorithm());

                String base32Secret = otpInfo.getSecret();
                Log.d("sec", "Base32 Secret: " + base32Secret);

                byte[] secretBytes = base32.decode(base32Secret);
                Log.d("bytes", "Secret: " + ByteString.copyFrom(secretBytes));

                Log.d("name", "AccountName: " + otpInfo.getAccountName());
                Log.d("issuer", "Issuer: " + otpInfo.getIssuer());
                Log.d("alg", "Algorithm: " + algorithmEnum);
                Log.d("digit", "Length: " + otpInfo.getOtpLength());

                otpParametersBuilder.setIssuer(otpInfo.getIssuer())
                        .setAccountName(otpInfo.getAccountName())
                        .setSecret(ByteString.copyFrom(secretBytes))
                        .setAlgorithm(algorithmEnum)
                        .setDigits(getDigitCountEnum(otpInfo.getOtpLength()));

                migrationBuilder.addOtpParameters(otpParametersBuilder);
            } catch (Exception e) {
                Log.e(TAG, "Error processing OTP for account: " + otpInfo.getAccountName(), e);
                Toast.makeText(this, "Error processing OTP for " + otpInfo.getAccountName(), Toast.LENGTH_SHORT).show();
            }
        }


        MigrationPayload migrationPayload = migrationBuilder.build();
        String encodedPayload = Base64.encodeToString(migrationPayload.toByteArray(),Base64.NO_WRAP);

        Log.d("Encoded", "Encoded Payload: " + encodedPayload);
        Log.d("PayloadSize", "Encoded Payload Length: " + encodedPayload.length());

        byte[] decodedPayload = Base64.decode(encodedPayload, Base64.NO_WRAP);
        try {
            MigrationPayload payload = MigrationPayload.parseFrom(decodedPayload);
            for (MigrationPayload.OTPParameters otp : payload.getOtpParametersList()) {
                Log.d("issue","Issuer: " + otp.getIssuer());
                Log.d("accnam","Account Name: " + otp.getAccountName());
                Log.d("see","Secret: " + otp.getSecret().toStringUtf8());
                Log.d("algg","Algorithm: " + otp.getAlgorithm());
                Log.d("dii","Digits: " + otp.getDigits());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
}
