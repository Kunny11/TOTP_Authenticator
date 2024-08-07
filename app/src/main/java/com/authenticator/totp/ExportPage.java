package com.authenticator.totp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.authenticator.totp.db.OtpDatabaseHelper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;

public class ExportPage extends AppCompatActivity {

    private static final String TAG = "ExportPage";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private OtpDatabaseHelper otpDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_page);

        otpDatabaseHelper = new OtpDatabaseHelper(this);

        Button generateButton = findViewById(R.id.export_details);
        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(ExportPage.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    showFormatSelectionDialog();
                } else {
                    ActivityCompat.requestPermissions(ExportPage.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                }
            }
        });
    }

    private void showFormatSelectionDialog() {
        final String[] formats = {"TXT", "JSON", "HTML"};

        new AlertDialog.Builder(this)
                .setTitle("Choose file format")
                .setItems(formats, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                generateAndSaveFile("txt");
                                break;
                            case 1:
                                generateAndSaveFile("json");
                                break;
                            case 2:
                                generateAndSaveFile("html");
                                break;
                        }
                    }
                })
                .show();
    }

    private void generateAndSaveFile(String format) {
        // Retrieve OTP info from the database
        List<OtpInfo> otpInfoList = otpDatabaseHelper.getAllOtpInfo();
        if (otpInfoList == null || otpInfoList.isEmpty()) {
            Toast.makeText(this, "No OTP details to export", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String content = "";
        switch (format) {
            case "txt":
                StringBuilder textBuilder = new StringBuilder();
                for (OtpInfo otpInfo : otpInfoList) {
                    String totpUri = String.format("otpauth://totp/%s%%3A%s?period=%s&digits=%s&algorithm=%s&secret=%s&issuer=%s",
                            otpInfo.getIssuer(), otpInfo.getAccountName(), otpInfo.getUserTimeStep(), otpInfo.getOtpLength(), otpInfo.getAlgorithm(), otpInfo.getSecret(), otpInfo.getIssuer());
                    textBuilder.append(totpUri).append("\n");
                }
                content = textBuilder.toString();
                break;

            case "json":
                content = gson.toJson(otpInfoList);
                break;

            case "html":
                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<html><body>");
                for (OtpInfo otpInfo : otpInfoList) {
                    htmlBuilder.append("<p>")
                            .append("Name: ").append(otpInfo.getAccountName()).append("<br>")
                            .append("Issuer: ").append(otpInfo.getIssuer()).append("<br>")
                            .append("Secret: ").append(otpInfo.getSecret()).append("<br>")
                            .append("Period: ").append(otpInfo.getUserTimeStep()).append("<br>")
                            .append("Digits: ").append(otpInfo.getOtpLength()).append("<br>")
                            .append("Algorithm: ").append(otpInfo.getAlgorithm()).append("<br>")
                            .append("QR Code: <img src='").append(generateQrCode(otpInfo)).append("'><br>")
                            .append("</p>");
                }
                htmlBuilder.append("</body></html>");
                content = htmlBuilder.toString();
                break;
        }

        saveFile(content, format);
    }

    private void saveFile(String content, String format) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "otp_info." + format);
            values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(format));
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

            try (OutputStream fos = getContentResolver().openOutputStream(uri)) {
                if (fos != null) {
                    fos.write(content.getBytes());
                    fos.flush();
                    Toast.makeText(this, "File saved to Downloads", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving file: " + e.getMessage(), e);
                Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "otp_info." + format);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes());
                fos.flush();
                Toast.makeText(this, "File saved to Downloads", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e(TAG, "Error saving file: " + e.getMessage(), e);
                Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getMimeType(String format) {
        switch (format) {
            case "json":
                return "application/json";
            case "html":
                return "text/html";
            default:
                return "text/plain";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showFormatSelectionDialog();
            } else {
                Toast.makeText(this, "Storage permission is required to save the file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String generateQrCode(OtpInfo otpInfo) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        String qrContent = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                otpInfo.getIssuer(), otpInfo.getAccountName(), otpInfo.getSecret(), otpInfo.getIssuer());

        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (int y = 0; y < bitMatrix.getHeight(); y++) {
                for (int x = 0; x < bitMatrix.getWidth(); x++) {
                    byteArrayOutputStream.write(bitMatrix.get(x, y) ? 0 : 1);
                }
            }
            return "data:image/png;base64," + android.util.Base64.encodeToString(byteArrayOutputStream.toByteArray(), android.util.Base64.DEFAULT);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code: " + e.getMessage(), e);
            return "";
        }
    }

}
