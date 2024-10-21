package com.authenticator.totp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.authenticator.totp.db.UserDatabaseHelper;

public class RegisterPage extends AppCompatActivity {

    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private UserDatabaseHelper db;
    private ImageView ivTogglePassword;
    private ImageView ivToggleConfirmPassword;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        db = new UserDatabaseHelper(this);

        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            etPassword.setInputType(isPasswordVisible ?
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setSelection(etPassword.length());
            ivTogglePassword.setImageResource(isPasswordVisible ?
                    R.drawable.eyeclose : R.drawable.eyeopen); // Change to your icons
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            etConfirmPassword.setInputType(isConfirmPasswordVisible ?
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etConfirmPassword.setSelection(etConfirmPassword.length());
            ivToggleConfirmPassword.setImageResource(isConfirmPasswordVisible ?
                    R.drawable.eyeclose : R.drawable.eyeopen);
        });

        btnRegister.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (db.isPasswordRegistered()) {
                Toast.makeText(RegisterPage.this, "A password has already been registered.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isPasswordStrong(password)) {
                Toast.makeText(RegisterPage.this, "Password must be at least 8 characters long and contain both letters and numbers.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.equals(confirmPassword)) {
                Log.d("RegisterPage", "Passwords match, attempting to insert password.");
                if (db.insertPassword(password)) {
                    Toast.makeText(RegisterPage.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterPage.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterPage.this, "Failed to register password.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(RegisterPage.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isPasswordStrong(String password) {
        return password.length() >= 8 && password.matches(".*\\d.*") && password.matches(".*[a-zA-Z].*");
    }
}
