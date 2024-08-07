package com.authenticator.totp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.authenticator.totp.db.UserDatabaseHelper;

public class RegisterPage extends AppCompatActivity {

    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private UserDatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        db = new UserDatabaseHelper(this);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = etPassword.getText().toString();
                String confirmPassword = etConfirmPassword.getText().toString();

                if (db.isPasswordRegistered()) {
                    Toast.makeText(RegisterPage.this, "A password has already been registered.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.equals(confirmPassword)) {
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
            }
        });
    }
}
