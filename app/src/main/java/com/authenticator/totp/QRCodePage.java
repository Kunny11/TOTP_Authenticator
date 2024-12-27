package com.authenticator.totp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.authenticator.totp.db.OtpDatabaseHelper;
import com.authenticator.totp.otp.Generator;
import com.google.authenticator.migration.MigrationPayloadWrapper.MigrationPayload;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

public class QRCodePage extends AppCompatActivity {

    private static final int OTP_LENGTH = 6;
    private static final int TIME_STEP = 30;
    private static final String ALGORITHM = "HmacSHA1";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_code);

        // Starts QR code scanning
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                String qrCodeContents = result.getContents();
                Toast.makeText(this, "Scanned: " + qrCodeContents, Toast.LENGTH_LONG).show();

                try {
                    if (qrCodeContents.startsWith("otpauth://totp/")) {
                        handleTotpUri(qrCodeContents);
                    } else if (qrCodeContents.startsWith("otpauth-migration://offline")) {
                        handleMigrationUri(qrCodeContents);
                    } else {
                        Toast.makeText(this, "Unsupported QR Code Format", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error processing QR Code", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleTotpUri(String qrCodeContents) {
        String secret = null, accountName = null, issuer = null;

        try {
            String decodedUrl = URLDecoder.decode(qrCodeContents, "UTF-8");

            Pattern pattern = Pattern.compile("otpauth://totp/([^:]+):([^?]+)\\?secret=([^&]+)&issuer=([^&]+)");
            Matcher matcher = pattern.matcher(decodedUrl);

            if (matcher.find()) {
                issuer = matcher.group(1);
                accountName = matcher.group(2);
                secret = matcher.group(3);
                String qrIssuer = matcher.group(4);

                if (issuer == null) {
                    issuer = qrIssuer;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (secret != null && accountName != null && issuer != null) {
            // Generate OTP using the Generator class
            Generator otpGenerator = new Generator();
            String otp = otpGenerator.generateOTP(secret, 6, 30, "HmacSHA1");

            // Pass the generated OTP and other details to MainActivity
            Intent intent = new Intent(QRCodePage.this, HomePage.class);
            intent.putExtra("qr_code_contents", qrCodeContents);
            intent.putExtra("account_name", accountName);
            intent.putExtra("issuer", issuer);
            intent.putExtra("secret", secret);
            intent.putExtra("otp", otp);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Invalid TOTP QR Code", Toast.LENGTH_LONG).show();
        }
    }

    private void handleMigrationUri(String qrCodeContents) {
        try {
            String decodedUrl = URLDecoder.decode(qrCodeContents, "UTF-8");

            // Extract the `data` parameter from the migration URI
            Pattern pattern = Pattern.compile("otpauth-migration://offline\\?data=([^&]+)");
            Matcher matcher = pattern.matcher(decodedUrl);

            if (matcher.find()) {
                String base64Data = matcher.group(1);
                byte[] decodedData = Base64.getDecoder().decode(base64Data);

                MigrationPayload migrationPayload = MigrationPayload.parseFrom(decodedData);

                List<OtpInfo> otpInfoList = new ArrayList<>();

                // Loop through all OTP parameters in the migration payload
                for (MigrationPayload.OTPParameters otp : migrationPayload.getOtpParametersList()) {
                    String accountName = otp.getAccountName();
                    String issuer = otp.getIssuer();
                    String secret = base32Encode(otp.getSecret().toByteArray());

                    OtpInfo otpInfo = new OtpInfo(accountName, issuer, secret, OTP_LENGTH, TIME_STEP, ALGORITHM);
                    otpInfoList.add(otpInfo);
                }

                // Insert all OTP info in batch
                OtpDatabaseHelper otpDatabaseHelper = new OtpDatabaseHelper(this);
                otpDatabaseHelper.addOtpInfoBatch(otpInfoList);

                Toast.makeText(this, "Accounts imported successfully", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(QRCodePage.this, HomePage.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Invalid Migration QR Code", Toast.LENGTH_LONG).show();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error decoding URL", Toast.LENGTH_LONG).show();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error decoding migration QR Code", Toast.LENGTH_LONG).show();
        }
    }

    private String base32Encode(byte[] data) {
        String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder encoded = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer <<= 8;
            buffer |= (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                encoded.append(base32Chars.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            buffer <<= (5 - bitsLeft);
            encoded.append(base32Chars.charAt(buffer & 0x1F));
        }
        return encoded.toString();
    }
}
