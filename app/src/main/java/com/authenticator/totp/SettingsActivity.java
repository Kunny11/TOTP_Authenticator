package com.authenticator.totp;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // Import Page
        LinearLayout impPage = findViewById(R.id.imp_page);
        impPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent importIntent = new Intent(SettingsActivity.this, ImportPage.class);
                startActivity(importIntent);
            }
        });

        // Export Page
        LinearLayout expPage = findViewById(R.id.exp_page);
        expPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent exportIntent = new Intent(SettingsActivity.this, ExportPage.class);
                startActivity(exportIntent);
            }
        });

        // Transfer with Qr code
        LinearLayout expQr = findViewById(R.id.qr_exp);
        expQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent transferIntent = new Intent(SettingsActivity.this, TransferPage.class);
                startActivity(transferIntent);
            }
        });

        // Enable/Disable Biometrics
        LinearLayout bio = findViewById(R.id.bio);
        bio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent transferIntent = new Intent(SettingsActivity.this, Biometrics.class);
                startActivity(transferIntent);
            }
        });
    }
}
