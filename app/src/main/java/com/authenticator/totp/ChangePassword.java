package com.authenticator.totp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.authenticator.totp.db.UserDatabaseHelper;

public class ChangePassword extends AppCompatActivity {

    private EditText newPasswordInput, confirmNewPasswordInput;
    private Button changePasswordButton;
    private UserDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_pass);

        dbHelper = new UserDatabaseHelper(this);

        newPasswordInput = findViewById(R.id.new_Pass);
        confirmNewPasswordInput = findViewById(R.id.confirmNewPass);
        changePasswordButton = findViewById(R.id.buttonChangePassword);

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = newPasswordInput.getText().toString();
                String confirmNewPassword = confirmNewPasswordInput.getText().toString();

                if (!newPassword.equals(confirmNewPassword)) {
                    Toast.makeText(ChangePassword.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean result = changePass(newPassword);

                if (result) {
                    Toast.makeText(ChangePassword.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChangePassword.this, "Failed to change password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Method to change the password.
     * @param newPassword The new password the user wants to set.
     * @return true if the password was changed successfully, false otherwise.
     */
    private boolean changePass(String newPassword) {
        boolean isPasswordChanged = dbHelper.updatePassword(newPassword); // Use updatePassword instead

        if (isPasswordChanged) {
            Log.d("ChangePasswordActivity", "Password changed successfully");
            return true;
        } else {
            Log.e("ChangePasswordActivity", "Failed to change password");
            return false;
        }
    }
}
