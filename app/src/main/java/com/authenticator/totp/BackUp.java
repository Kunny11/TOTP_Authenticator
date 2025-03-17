package com.authenticator.totp;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.authenticator.totp.db.OtpDatabaseHelper;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.util.List;

public class BackUp extends AppCompatActivity {

    private static final String TAG = "BackUp";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private OtpDatabaseHelper otpDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup_page);

        otpDatabaseHelper = new OtpDatabaseHelper(this);

        Button backupButton = findViewById(R.id.backup_button);
        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(BackUp.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    showPasswordDialog();
                } else {
                    ActivityCompat.requestPermissions(BackUp.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                }
            }
        });
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter password for backup");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                if (!password.isEmpty()) {
                    exportAndEncryptJsonFile(password);
                } else {
                    Toast.makeText(BackUp.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void exportAndEncryptJsonFile(String password) {
        List<OtpInfo> otpInfoList = otpDatabaseHelper.getAllOtpInfo();
        if (otpInfoList == null || otpInfoList.isEmpty()) {
            Toast.makeText(this, "No OTP details to backup", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String jsonContent = gson.toJson(otpInfoList);

        String encryptedContent = "";
        try {
            PassEncryp passEncryp = new PassEncryp();
            encryptedContent = passEncryp.encryptContent(jsonContent, password);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed: " + e.getMessage(), e);
            Toast.makeText(this, "Error encrypting backup", Toast.LENGTH_SHORT).show();
            return;
        }

        String wrappedEncryptedJson = gson.toJson(new EncryptedJson(true, encryptedContent));
        saveBackupFile(wrappedEncryptedJson);
    }

    private void saveBackupFile(String encryptedContent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "otp_backup.json");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            try (OutputStream fos = getContentResolver().openOutputStream(getContentResolver().insert(MediaStore.Files.getContentUri("external"), values))) {
                if (fos != null) {
                    fos.write(encryptedContent.getBytes());
                    fos.flush();
                    Toast.makeText(this, "Backup saved to Downloads", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error saving backup file", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving backup file: " + e.getMessage(), e);
                Toast.makeText(this, "Error saving backup file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "This Android version is not supported for automatic backup", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPasswordDialog();
            } else {
                Toast.makeText(this, "Storage permission is required to save the backup file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
