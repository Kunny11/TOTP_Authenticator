package com.authenticator.totp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.authenticator.totp.otp.Generator;

public class ManualPage extends AppCompatActivity {

    private EditText accountnameEditText, issuerEditText, seed32EditText, timeStepEditText, totpLengthEditText, algorithmEditText;
    private Button generateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_page);

        accountnameEditText = findViewById(R.id.accountnameEditText);
        issuerEditText = findViewById(R.id.issuerEditText);
        seed32EditText = findViewById(R.id.seed32EditText);
        timeStepEditText = findViewById(R.id.timeStepEditText);
        totpLengthEditText = findViewById(R.id.totpLengthEditText);
        algorithmEditText = findViewById(R.id.algorithmEditText);
        generateButton = findViewById(R.id.generateButton);

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateOTP();
            }
        });
    }

    private void generateOTP() {
        String accountName = accountnameEditText.getText().toString();
        String issuer = issuerEditText.getText().toString();
        String seed32 = seed32EditText.getText().toString();;
        String timeStepString = timeStepEditText.getText().toString().trim();
        String totpLengthString = totpLengthEditText.getText().toString().trim();
        String algorithm = algorithmEditText.getText().toString().trim();

        // Check for empty fields
        if (accountName.isEmpty() || issuer.isEmpty() || seed32.isEmpty() || timeStepString.isEmpty() || totpLengthString.isEmpty() || algorithm.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate that the time step and OTP length are valid integers
        int totpLength, timeStep;
        try {
            totpLength = Integer.parseInt(totpLengthString);
            timeStep = Integer.parseInt(timeStepString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid TOTP Length or Time Step", Toast.LENGTH_SHORT).show();
            return;
        }

        // Standardize algorithm name and validate it
        algorithm = getStandardizedAlgorithm(algorithm);
        if (algorithm == null) {
            Toast.makeText(this, "Invalid Algorithm", Toast.LENGTH_SHORT).show();
            return;
        }

        Generator generator = new Generator();
        String otp = generator.generateOTP(seed32, totpLength, timeStep, algorithm);

        // Navigate back to MainActivity with data
        Intent intent = new Intent(ManualPage.this, HomePage.class);
        intent.putExtra("account_name", accountName);
        intent.putExtra("issuer", issuer);
        intent.putExtra("generated_otp", otp);
        intent.putExtra("secret", seed32);
        intent.putExtra("userTimeStep", String.valueOf(timeStep));
        intent.putExtra("userOtpLength", String.valueOf(totpLength));
        intent.putExtra("userAlgorithm", algorithm);
        startActivity(intent);
    }

    private String getStandardizedAlgorithm(String inputAlgorithm) {
        switch (inputAlgorithm.toLowerCase()) {
            case "sha1":
            case "Sha1":
            case "SHA1":
            case "HMACSHA1":
            case "hmacsha1":
                return "HmacSHA1";
            case "sha256":
            case "Sha256":
            case "SHA256":
            case "HMACSHA256":
            case "hmacsha256":
                return "HmacSHA256";
            case "sha512":
            case "Sha512":
            case "SHA512":
            case "HMACSHA512":
            case "hmacsha512":
                return "HmacSHA512";
            default:
                return "HmacSHA1";
        }
    }
}
