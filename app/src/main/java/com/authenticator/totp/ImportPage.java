package com.authenticator.totp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.authenticator.totp.db.OtpDatabaseHelper;
import com.authenticator.totp.OtpInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImportPage extends AppCompatActivity {

    private static final String TAG = "ImportPage";
    private static final int REQUEST_READ_STORAGE = 113;
    private static final int PICK_TEXT_FILE = 114;
    private static final int PICK_JSON_FILE = 115;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_page);

        Button importButton = findViewById(R.id.import_details);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFormatSelectionDialog();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
        } else {
            showFormatSelectionDialog();
        }
    }

    private void showFormatSelectionDialog() {
        final String[] formats = {"TXT", "JSON"};

        new AlertDialog.Builder(this)
                .setTitle("Choose file format")
                .setItems(formats, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                openFilePicker("text/plain", PICK_TEXT_FILE);
                                break;
                            case 1:
                                openFilePicker("application/json", PICK_JSON_FILE);
                                break;
                        }
                    }
                })
                .show();
    }



    private void openFilePicker(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == PICK_TEXT_FILE) {
                    readFile(uri);
                } else if (requestCode == PICK_JSON_FILE) {
                    readJsonFile(uri);
                }
            }
        }
    }

    private void readFile(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            String content = stringBuilder.toString().trim();
            parseTOTPURI(content);
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + e.getMessage(), e);
            Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void readJsonFile(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            parseJson(stringBuilder.toString().trim());
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON file: " + e.getMessage(), e);
            Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void parseTOTPURI(String totpUri) {
        try {
            Uri uri = Uri.parse(totpUri);

            if (!"otpauth".equals(uri.getScheme())) {
                throw new IllegalArgumentException("Invalid scheme in TOTP URI");
            }

            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("Invalid path in TOTP URI");
            }

            String accountName = path.substring(1);

            String issuer = uri.getQueryParameter("issuer");
            if (issuer == null) {
                issuer = "";
            }

            String[] accountParts = accountName.split(":");
            if (accountParts.length == 2) {
                issuer = accountParts[0];
                accountName = accountParts[1];
            }

            String secret = uri.getQueryParameter("secret");
            if (secret == null) {
                throw new IllegalArgumentException("Missing 'secret' parameter in TOTP URI");
            }

            String otpLengthStr = uri.getQueryParameter("otp_length");
            String userTimeStepStr = uri.getQueryParameter("user_time_step");
            String algorithm = uri.getQueryParameter("algorithm");

            int otpLength = otpLengthStr != null ? Integer.parseInt(otpLengthStr) : 6;
            int userTimeStep = userTimeStepStr != null ? Integer.parseInt(userTimeStepStr) : 30;

            OtpInfo otpInfo = new OtpInfo(accountName, issuer, secret, otpLength, userTimeStep, algorithm);
            OtpDatabaseHelper dbHelper = new OtpDatabaseHelper(this);
            dbHelper.addOtpInfo(otpInfo);

            Toast.makeText(this, "OTP imported successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ImportPage.this, HomePage.class);
            startActivity(intent);
            finish();
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing number from TOTP URI: " + e.getMessage(), e);
            Toast.makeText(this, "Error parsing TOTP URI: Invalid number format", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error parsing TOTP URI: " + e.getMessage(), e);
            Toast.makeText(this, "Error parsing TOTP URI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing TOTP URI: " + e.getMessage(), e);
            Toast.makeText(this, "Error parsing TOTP URI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void parseJson(String json) {
        try {
            Object jsonObj = new JSONTokener(json).nextValue();

            if (jsonObj instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) jsonObj;

                if (jsonObject.has("encrypted") && jsonObject.getBoolean("encrypted")) {
                    // Encrypted JSON
                    showPasswordDialog(jsonObject);
                } else if (jsonObject.has("content")) {
                    // Unencrypted JSON
                    String content = jsonObject.getString("content");
                    parseContent(content);
                } else {
                    throw new IllegalArgumentException("Invalid JSON structure: Missing 'content' field.");
                }
            } else {
                throw new IllegalArgumentException("Invalid JSON format: Root must be a JSONObject.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON file: " + e.getMessage(), e);
            Toast.makeText(this, "Error parsing JSON file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPasswordDialog(JSONObject encryptedJsonObject) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Password");

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = passwordInput.getText().toString();
            try {
                String encryptedContent = encryptedJsonObject.getString("content");
                byte[] encryptedData = Base64.decode(encryptedContent, Base64.DEFAULT);
                String decryptedContent = PassEncryp.decryptContent(encryptedData, password);
                parseContent(decryptedContent);
            } catch (Exception e) {
                Log.e(TAG, "Error decrypting content: " + e.getMessage(), e);
                Toast.makeText(ImportPage.this, "Error decrypting file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    private void parseContent(String content) {
        try {
            Object contentObj = new JSONTokener(content).nextValue();

            if (contentObj instanceof JSONArray) {
                JSONArray entriesArray = (JSONArray) contentObj;
                List<OtpInfo> otpInfos = new ArrayList<>();
                parseJsonArray(entriesArray, otpInfos);
            } else {
                throw new IllegalArgumentException("Invalid content structure: Expected a JSON array.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing content: " + e.getMessage(), e);
            Toast.makeText(this, "Error parsing content: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void parseJsonArray(JSONArray jsonArray, List<OtpInfo> otpInfos) {
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject entryObject = jsonArray.getJSONObject(i);

                String accountName = entryObject.optString("accountName", "");
                String issuer = entryObject.optString("issuer", "");
                String secret = entryObject.optString("secret", "");
                int otpLength = entryObject.optInt("otpLength", 6);
                int userTimeStep = entryObject.optInt("userTimeStep", 30);
                String algorithm = entryObject.optString("algorithm", "HmacSHA1");

                algorithm = translateAlgorithm(algorithm);

                Log.d(TAG, "Parsed OTP Info - Account Name: " + accountName + ", Issuer: " + issuer +
                        ", Secret: " + secret + ", Algorithm: " + algorithm + ", Digits: " + otpLength +
                        ", Period: " + userTimeStep);

                OtpInfo otpInfo = new OtpInfo(accountName, issuer, secret, otpLength, userTimeStep, algorithm);
                otpInfos.add(otpInfo);
            }
            saveToDatabase(otpInfos);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON array: " + e.getMessage(), e);
            Toast.makeText(this, "Error parsing JSON array: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private String translateAlgorithm(String algorithm) {
        switch (algorithm) {
            case "SHA1":
                return "HmacSHA1";
            case "SHA2":
                return "HmacSHA256";
            case "SHA3":
                return "HmacSHA3";
            default:
                return algorithm;
        }
    }

    private void saveToDatabase(List<OtpInfo> otpInfos) {
        OtpDatabaseHelper dbHelper = new OtpDatabaseHelper(this);
        for (OtpInfo otpInfo : otpInfos) {
            dbHelper.addOtpInfo(otpInfo);
        }
        Toast.makeText(this, "OTP Accounts Imported Successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ImportPage.this, HomePage.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Open the file picker once permission is granted
                showFormatSelectionDialog();
            } else {
                Toast.makeText(this, "Storage permission is required to import the file", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}