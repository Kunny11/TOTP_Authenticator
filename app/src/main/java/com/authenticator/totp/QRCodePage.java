package com.authenticator.totp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.authenticator.totp.otp.Generator;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRCodePage extends AppCompatActivity {

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
                    Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
