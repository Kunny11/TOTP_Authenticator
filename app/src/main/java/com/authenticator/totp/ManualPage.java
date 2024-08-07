package com.authenticator.totp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
        String seed32 = seed32EditText.getText().toString();
        int totpLength = Integer.parseInt(totpLengthEditText.getText().toString());
        int timeStep = Integer.parseInt(timeStepEditText.getText().toString()); // Get user-entered time step
        String algorithm = algorithmEditText.getText().toString();

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

}
