package com.authenticator.totp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        Button importButton = findViewById(R.id.import_button);
        Button exportButton = findViewById(R.id.export_button);

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent importIntent = new Intent(SettingsActivity.this, ImportPage.class);
                startActivity(importIntent);
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent exportIntent = new Intent(SettingsActivity.this, ExportPage.class);
                startActivity(exportIntent);
            }
        });
    }
}
