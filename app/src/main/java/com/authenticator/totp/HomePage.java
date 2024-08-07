package com.authenticator.totp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.authenticator.totp.otp.Generator;
import com.authenticator.totp.db.OtpDatabaseHelper;
import com.authenticator.totp.ExportPage;
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
import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private static final int OTP_LENGTH = 6;
    private static final int TIME_STEP = 30;
    private static final String ALGORITHM = "HmacSHA1";

    private static final String TAG = "HomePage";

    private static final int REQUEST_WRITE_STORAGE = 112;

    private List<OtpInfo> otpInfoList = new ArrayList<>();

    private List<OtpInfo> selectedOtpInfoList = new ArrayList<>();
    private Handler handler = new Handler();
    private OtpAdapter otpAdapter;
    private OtpDatabaseHelper otpDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        otpDatabaseHelper = new OtpDatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });

        ImageView settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsMenu(v);
            }
        });

        RecyclerView otpRecyclerView = findViewById(R.id.otp_recycler_view);
        otpRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        otpAdapter = new OtpAdapter(otpInfoList);
        otpRecyclerView.setAdapter(otpAdapter);

        otpAdapter.setOnOtpItemLongClickListener(new OtpAdapter.OnOtpItemLongClickListener() {
            @Override
            public void onItemLongClick(int position) {
                showDeleteConfirmationDialog(position);
            }
        });

        // Loads OTP info from the database
        otpInfoList.addAll(otpDatabaseHelper.getAllOtpInfo());
        otpAdapter.notifyDataSetChanged();

        Intent intent = getIntent();
        String accountName = intent.getStringExtra("account_name");
        String issuer = intent.getStringExtra("issuer");
        String secret = intent.getStringExtra("secret");
        String generatedOTP = intent.getStringExtra("generated_otp");
        String userTimeStepString = intent.getStringExtra("userTimeStep");
        String userOtpLengthString = intent.getStringExtra("userOtpLength");
        String userAlgorithm = intent.getStringExtra("userAlgorithm");

        int userTimeStep = userTimeStepString != null ? Integer.parseInt(userTimeStepString) : TIME_STEP;
        int userOtpLength = userOtpLengthString != null ? Integer.parseInt(userOtpLengthString) : OTP_LENGTH;
        String algorithm = userAlgorithm != null ? userAlgorithm : ALGORITHM;

        Log.d(TAG, "Received Intent Data - Account Name: " + accountName + ", Issuer: " + issuer + ", Secret: " + secret + ", Generated OTP: " + generatedOTP);

        if (accountName != null && issuer != null && secret != null && generatedOTP == null) {
            try {
                Generator generator = new Generator();
                String otp = generator.generateOTP(secret, userOtpLength, userTimeStep, algorithm);
                updateOtpInfo(accountName, issuer, otp, secret, false, userOtpLength, userTimeStep, algorithm);
            } catch (Exception e) {
                Log.e(TAG, "Error generating OTP", e);
            }
        } else if (generatedOTP != null) {
            updateOtpInfo(accountName, issuer, generatedOTP, secret, true, userOtpLength, userTimeStep, algorithm);
        }


        handler.post(updateOtpTask);
    }

    private void showDeleteConfirmationDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account")
                .setMessage("Are you sure you want to delete this account?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAccount(position);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAccount(int position) {
        OtpInfo otpInfo = otpInfoList.get(position);
        otpDatabaseHelper.deleteOtpInfo(otpInfo.getId());
        otpInfoList.remove(position);
        otpAdapter.notifyItemRemoved(position);
    }


    private void showSettingsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(HomePage.this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.settings_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.settings_option) {
                    Intent settingsIntent = new Intent(HomePage.this, SettingsActivity.class);
                    startActivity(settingsIntent);
                    return true;
                } else if (id == R.id.select_option) {
                    showSelectAccountsDialog();
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }



    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(HomePage.this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.pop_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.manual) {
                    Intent manualIntent = new Intent(HomePage.this, ManualPage.class);
                    startActivity(manualIntent);
                    return true;
                } else if (id == R.id.qr_code) {
                    Intent scanIntent = new Intent(HomePage.this, QRCodePage.class);
                    startActivity(scanIntent);
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void showSelectAccountsDialog() {
        boolean[] checkedItems = new boolean[otpInfoList.size()];
        String[] accountNames = new String[otpInfoList.size()];

        for (int i = 0; i < otpInfoList.size(); i++) {
            accountNames[i] = otpInfoList.get(i).getAccountName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select OTP Accounts")
                .setMultiChoiceItems(accountNames, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Export", (dialog, which) -> {
                    exportSelectedAccounts(checkedItems);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void exportSelectedAccounts(boolean[] checkedItems) {
        selectedOtpInfoList.clear();

        for (int i = 0; i < checkedItems.length; i++) {
            if (checkedItems[i]) {
                selectedOtpInfoList.add(otpInfoList.get(i));
            }
        }

        if (selectedOtpInfoList.isEmpty()) {
            Toast.makeText(this, "No accounts selected", Toast.LENGTH_SHORT).show();
            return;
        }

        showFormatSelectionDialog(selectedOtpInfoList);
    }


    private void showFormatSelectionDialog(List<OtpInfo> selectedOtpInfoList) {
        final String[] formats = {"TXT", "JSON", "HTML"};

        new AlertDialog.Builder(this)
                .setTitle("Choose file format")
                .setItems(formats, (dialog, which) -> {
                    String format = formats[which].toLowerCase();
                    generateAndSaveFile(selectedOtpInfoList, format);
                })
                .show();
    }

    private void generateAndSaveFile(List<OtpInfo> selectedOtpInfoList, String format) {
        if (selectedOtpInfoList == null || selectedOtpInfoList.isEmpty()) {
            Toast.makeText(this, "No OTP details to export", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String content = "";
        switch (format) {
            case "txt":
                StringBuilder textBuilder = new StringBuilder();
                for (OtpInfo otpInfo : selectedOtpInfoList) {
                    String totpUri = String.format("otpauth://totp/%s%%3A%s?period=%s&digits=%s&algorithm=%s&secret=%s&issuer=%s",
                            otpInfo.getIssuer(), otpInfo.getAccountName(), otpInfo.getUserTimeStep(), otpInfo.getOtpLength(), otpInfo.getAlgorithm(), otpInfo.getSecret(), otpInfo.getIssuer());
                    textBuilder.append(totpUri).append("\n");
                }
                content = textBuilder.toString();
                break;

            case "json":
                List<SelectedAcc> otpInfoDTOList = new ArrayList<>();
                for (OtpInfo otpInfo : selectedOtpInfoList) {
                    otpInfoDTOList.add(new SelectedAcc(
                            otpInfo.getAccountName(),
                            otpInfo.getAlgorithm(),
                            otpInfo.getIssuer(),
                            otpInfo.getOtpLength(),
                            otpInfo.getSecret(),
                            otpInfo.getUserTimeStep()
                    ));
                }
                content = gson.toJson(otpInfoDTOList);
                break;


            case "html":
                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<html><body>");
                for (OtpInfo otpInfo : selectedOtpInfoList) {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showFormatSelectionDialog(selectedOtpInfoList);
            } else {
                Toast.makeText(this, "Storage permission is required to save the file", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void updateOtpInfo(String accountName, String issuer, String generatedOTP, String secret, boolean isManual, int otpLength, int userTimeStep, String algorithm) {
        // Check for duplicates
        for (OtpInfo otpInfo : otpInfoList) {
            if (otpInfo.getAccountName().equals(accountName) && otpInfo.getIssuer().equals(issuer)) {
                otpInfo.setGeneratedOTP(generatedOTP);
                otpInfo.setOtpLength(otpLength);
                otpInfo.setUserTimeStep(userTimeStep);
                otpInfo.setAlgorithm(algorithm);
                otpDatabaseHelper.updateOtpInfo(otpInfo);
                otpAdapter.notifyDataSetChanged();
                return;
            }
        }

        // If there are no duplicates, adds a new OTP info
        OtpInfo otpInfo = new OtpInfo(accountName, issuer, secret, otpLength, userTimeStep, algorithm);
        otpInfo.setGeneratedOTP(generatedOTP);
        otpInfoList.add(otpInfo);
        otpDatabaseHelper.addOtpInfo(otpInfo);
        otpAdapter.notifyItemInserted(otpInfoList.size() - 1);
    }


    private final Runnable updateOtpTask = new Runnable() {
        @Override
        public void run() {
            for (OtpInfo otpInfo : otpInfoList) {
                try {
                    Generator generator = new Generator();
                    String otp = generator.generateOTP(otpInfo.getSecret(), otpInfo.getOtpLength(), otpInfo.getUserTimeStep(), otpInfo.getAlgorithm());
                    otpInfo.setGeneratedOTP(otp);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating OTP", e);
                }
            }
            otpAdapter.notifyDataSetChanged();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateOtpTask);
        super.onDestroy();
    }
}

