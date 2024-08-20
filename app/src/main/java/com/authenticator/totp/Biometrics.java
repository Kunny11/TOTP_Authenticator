package com.authenticator.totp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Biometrics extends AppCompatActivity {

    private Switch bioSwitch;
    private static final String PREFS_NAME = "biometric_prefs";
    private static final String BIOMETRICS_ENABLED_KEY = "biometrics_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biometrics);

        bioSwitch = findViewById(R.id.bioswitch);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isBiometricsEnabled = preferences.getBoolean(BIOMETRICS_ENABLED_KEY, false);
        bioSwitch.setChecked(isBiometricsEnabled);

        bioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(BIOMETRICS_ENABLED_KEY, isChecked);
            editor.apply();

            String message = isChecked ? "Biometrics enabled" : "Biometrics disabled";
            Toast.makeText(Biometrics.this, message, Toast.LENGTH_SHORT).show();
        });
    }
}
