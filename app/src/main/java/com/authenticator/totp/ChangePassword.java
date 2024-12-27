package com.authenticator.totp;

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

public class ChangePassword extends AppCompatActivity {

    private EditText newPasswordInput, confirmNewPasswordInput;
    private Button changePasswordButton;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private UserDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_pass);

        dbHelper = new UserDatabaseHelper(this);

        newPasswordInput = findViewById(R.id.new_Pass);
        confirmNewPasswordInput = findViewById(R.id.confirmNewPass);
        changePasswordButton = findViewById(R.id.buttonChangePassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);  // Add the eye icon for new password
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);  // Add the eye icon for confirm password

        // Toggle visibility for the new password field
        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            newPasswordInput.setInputType(isPasswordVisible ?
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            newPasswordInput.setSelection(newPasswordInput.length());
            ivTogglePassword.setImageResource(isPasswordVisible ?
                    R.drawable.eyeclose : R.drawable.eyeopen); // Update with your drawable
        });

        // Toggle visibility for the confirm password field
        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            confirmNewPasswordInput.setInputType(isConfirmPasswordVisible ?
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            confirmNewPasswordInput.setSelection(confirmNewPasswordInput.length());
            ivToggleConfirmPassword.setImageResource(isConfirmPasswordVisible ?
                    R.drawable.eyeclose : R.drawable.eyeopen); // Update with your drawable
        });

        changePasswordButton.setOnClickListener(v -> {
            String newPassword = newPasswordInput.getText().toString();
            String confirmNewPassword = confirmNewPasswordInput.getText().toString();

            // Validate password strength
            if (!isPasswordStrong(newPassword)) {
                Toast.makeText(ChangePassword.this, "Password must be at least 8 characters long and contain both letters and numbers.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate if new passwords match
            if (!newPassword.equals(confirmNewPassword)) {
                Toast.makeText(ChangePassword.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Attempt to change the password
            boolean result = changePass(newPassword);

            if (result) {
                Toast.makeText(ChangePassword.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChangePassword.this, "Failed to change password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Password strength validation (same as RegisterPage)
    private boolean isPasswordStrong(String password) {
        return password.length() >= 8 && password.matches(".*\\d.*") && password.matches(".*[a-zA-Z].*");
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
