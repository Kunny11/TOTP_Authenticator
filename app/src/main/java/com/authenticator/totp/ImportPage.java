package com.authenticator.totp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
                                if (ContextCompat.checkSelfPermission(ImportPage.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                    openFilePicker("text/plain", PICK_TEXT_FILE);
                                } else {
                                    ActivityCompat.requestPermissions(ImportPage.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
                                }
                                break;
                            case 1:
                                if (ContextCompat.checkSelfPermission(ImportPage.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                    openFilePicker("application/json", PICK_JSON_FILE);
                                } else {
                                    ActivityCompat.requestPermissions(ImportPage.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
                                }
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
            List<OtpInfo> otpInfos = new ArrayList<>();

            if (jsonObj instanceof JSONObject) {
                parseJsonObject((JSONObject) jsonObj, otpInfos);
            } else if (jsonObj instanceof JSONArray) {
                parseJsonArray((JSONArray) jsonObj, otpInfos);
            }

            OtpDatabaseHelper dbHelper = new OtpDatabaseHelper(this);
            for (OtpInfo otpInfo : otpInfos) {
                Log.d(TAG, "Adding OTP Info to DB: " + otpInfo.toString());
                dbHelper.addOtpInfo(otpInfo);
            }

            Toast.makeText(this, "OTP imported successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ImportPage.this, HomePage.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON file: " + e.getMessage(), e);
            Toast.makeText(this, "Error parsing JSON file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void parseJsonObject(JSONObject jsonObject, List<OtpInfo> otpInfos) {
        try {
            if (jsonObject.has("db")) {
                JSONObject dbObject = jsonObject.getJSONObject("db");
                JSONArray entriesArray = dbObject.getJSONArray("entries");
                parseJsonArray(entriesArray, otpInfos);
            } else if (jsonObject.has("accountName")) {
                String accountName = jsonObject.optString("accountName", "");
                String issuer = jsonObject.optString("issuer", "");
                String secret = jsonObject.optString("secret", "");
                int otpLength = jsonObject.optInt("otpLength", 6);
                int userTimeStep = jsonObject.optInt("userTimeStep", 30);
                String algorithm = jsonObject.optString("algorithm", "HmacSHA1");

                Log.d("kk", "Parsed OTP Info - Account Name: " + accountName + ", Issuer: " + issuer +
                        ", Secret: " + secret + ", Algorithm: " + algorithm + ", Digits: " + otpLength +
                        ", Period: " + userTimeStep);

                OtpInfo otpInfo = new OtpInfo(accountName, issuer, secret, otpLength, userTimeStep, algorithm);
                otpInfos.add(otpInfo);
            } else {
                parseFallback(jsonObject, otpInfos);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON object: " + e.getMessage(), e);
        }
    }

    private void parseJsonArray(JSONArray jsonArray, List<OtpInfo> otpInfos) {
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                Object value = jsonArray.get(i);
                if (value instanceof JSONObject) {
                    JSONObject entryObject = (JSONObject) value;
                    String accountName = entryObject.optString("name", "");
                    String issuer = entryObject.optString("issuer", "");

                    JSONObject infoObject = entryObject.getJSONObject("info");
                    String secret = infoObject.optString("secret", "");
                    int otpLength = infoObject.optInt("digits", 6);
                    int userTimeStep = infoObject.optInt("period", 30);
                    String algorithm = infoObject.optString("algo", "SHA1");

                    algorithm = translateAlgorithm(algorithm);

                    Log.d(TAG, "Parsed OTP Info - Account Name: " + accountName + ", Issuer: " + issuer +
                            ", Secret: " + secret + ", Algorithm: " + algorithm + ", Digits: " + otpLength +
                            ", Period: " + userTimeStep);

                    OtpInfo otpInfo = new OtpInfo(accountName, issuer, secret, otpLength, userTimeStep, algorithm);
                    otpInfos.add(otpInfo);
                } else if (value instanceof JSONArray) {
                    parseJsonArray((JSONArray) value, otpInfos);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON array: " + e.getMessage(), e);
        }
    }

    private void parseFallback(JSONObject jsonObject, List<OtpInfo> otpInfos) {
        try {
            String accountName = jsonObject.optString("accountName", jsonObject.optString("name", ""));
            String issuer = jsonObject.optString("issuer", "");
            String secret = jsonObject.optString("secret", "");
            int otpLength = jsonObject.optInt("otpLength", jsonObject.optInt("digits", 6));
            int userTimeStep = jsonObject.optInt("userTimeStep", jsonObject.optInt("period", 30));
            String algorithm = jsonObject.optString("algorithm", jsonObject.optString("algo", "SHA1"));

            algorithm = translateAlgorithm(algorithm);

            Log.d(TAG, "Parsed OTP Info - Account Name: " + accountName + ", Issuer: " + issuer +
                    ", Secret: " + secret + ", Algorithm: " + algorithm + ", Digits: " + otpLength +
                    ", Period: " + userTimeStep);

            OtpInfo otpInfo = new OtpInfo(accountName, issuer, secret, otpLength, userTimeStep, algorithm);
            otpInfos.add(otpInfo);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing fallback JSON object: " + e.getMessage(), e);
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


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showFormatSelectionDialog();
            } else {
                Toast.makeText(this, "Storage permission is required to import the file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
