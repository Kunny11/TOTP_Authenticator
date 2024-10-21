package com.authenticator.totp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.authenticator.totp.db.Hashing;
import com.authenticator.totp.db.UserDatabaseHelper;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private TextView biometrics;
    private UserDatabaseHelper db;
    private BiometricPrompt biometricPrompt;
    private static final String PREFS_NAME = "biometric_prefs";
    private static final String BIOMETRICS_ENABLED_KEY = "biometrics_enabled";
    private static final String PREFS_ENCRYPTION_KEY = "encryption_prefs";
    private static final String KEY_GENERATED = "key_generated";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        biometrics = findViewById(R.id.biometrics);
        db = new UserDatabaseHelper(this);

        db.logDatabaseContent();

        //Initialises the encryption key once during app launch
        SharedPreferences encryptionPrefs = getSharedPreferences(PREFS_ENCRYPTION_KEY, MODE_PRIVATE);
        boolean isKeyGenerated = encryptionPrefs.getBoolean(KEY_GENERATED, false);

        if (!isKeyGenerated) {
            try {
                Hashing.generateKey();
                SharedPreferences.Editor editor = encryptionPrefs.edit();
                editor.putBoolean(KEY_GENERATED, true);
                editor.apply();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error generating encryption key", Toast.LENGTH_SHORT).show();
            }
        }


        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isBiometricsEnabled = preferences.getBoolean(BIOMETRICS_ENABLED_KEY, true);

        if (isBiometricsEnabled) {
            setupBiometricLogin();
        } else {
            biometrics.setVisibility(View.GONE);
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredPassword = etPassword.getText().toString().trim();
                if (enteredPassword.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                } else {
                    if (db.verifyPassword(enteredPassword)) {
                        Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, HomePage.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!db.isPasswordRegistered()) {
                    Intent intent = new Intent(MainActivity.this, RegisterPage.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Password already set. Use password or biometrics to log in.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void setupBiometricLogin() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomePage.class);
                startActivity(intent);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build();

        biometrics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                biometricPrompt.authenticate(promptInfo);
            }
        });

        // checks for biometric hardware availability
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                biometrics.setVisibility(View.VISIBLE);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                biometrics.setVisibility(View.GONE);
                break;
        }
    }
}