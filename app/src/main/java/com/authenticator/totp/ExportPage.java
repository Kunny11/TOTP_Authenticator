package com.authenticator.totp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.authenticator.totp.db.OtpDatabaseHelper;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ExportPage extends AppCompatActivity {

    private static final String TAG = "ExportPage";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private OtpDatabaseHelper otpDatabaseHelper;
    private boolean shouldEncrypt = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_page);

        otpDatabaseHelper = new OtpDatabaseHelper(this);

        Button generateButton = findViewById(R.id.export_details);
        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(ExportPage.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    showFormatSelectionDialog();
                } else {
                    ActivityCompat.requestPermissions(ExportPage.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                }
            }
        });
    }

    private void showFormatSelectionDialog() {
        final String[] formats = {"TXT", "JSON", "HTML"};

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final CheckBox encryptCheckbox = new CheckBox(this);
        encryptCheckbox.setText("Encrypt file");

        layout.addView(encryptCheckbox);

        new AlertDialog.Builder(this)
                .setTitle("Choose file format")
                .setSingleChoiceItems(formats, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (formats[which].equals("JSON")) {
                            encryptCheckbox.setEnabled(true);
                            encryptCheckbox.setChecked(false);
                        } else {
                            encryptCheckbox.setEnabled(false);
                            encryptCheckbox.setChecked(false);
                        }
                    }
                })

                .setView(layout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        if (selectedPosition >= 0) {
                            shouldEncrypt = encryptCheckbox.isChecked();
                            String selectedFormat = formats[selectedPosition];
                            if (shouldEncrypt && selectedFormat.equals("JSON")) {
                                showPasswordDialog(selectedFormat);
                            } else {
                                generateAndSaveFile(selectedFormat.toLowerCase(), null);
                            }
                        } else {
                            Toast.makeText(ExportPage.this, "Please select a format", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPasswordDialog(final String format) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter password");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                if (!password.isEmpty()) {
                    generateAndSaveFile(format.toLowerCase(), password);
                } else {
                    Toast.makeText(ExportPage.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
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

    private void generateAndSaveFile(String format, String password) {
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
                try {
                    // Convert OTP list to JSON string
                    String otpJson = gson.toJson(otpInfoList);

                    if (shouldEncrypt) {
                        // Encrypt content if needed
                        PassEncryp passEncryp = new PassEncryp();
                        String encryptedContent = passEncryp.encryptContent(otpJson, password);

                        // Wrap encrypted content in the required JSON structure
                        content = gson.toJson(new EncryptedJson(true, encryptedContent));
                    } else {
                        // Wrap unencrypted content in the required JSON structure
                        content = gson.toJson(new EncryptedJson(false, otpJson));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error preparing JSON content: " + e.getMessage(), e);
                    Toast.makeText(this, "Error preparing JSON content", Toast.LENGTH_SHORT).show();
                    return;
                }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];

            // Converting BitMatrix to pixels array
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                }
            }

            // Creating a Bitmap from pixels array
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            // Converting Bitmap to PNG
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

            // Encode PNG to base64
            return "data:image/png;base64," + android.util.Base64.encodeToString(byteArrayOutputStream.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code: " + e.getMessage(), e);
            return "";
        }
    }


}
