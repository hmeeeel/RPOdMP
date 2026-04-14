package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.settings.SettingsActivity;
import com.example.myapplication.ui.settings.SettingsManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends BaseActivity {

    private TextInputEditText editEmail, editPassword;
    private View progressBar;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AuthManager.getInstance().isLoggedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        editEmail    = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        progressBar  = findViewById(R.id.progressBar);

        Button btnAction = findViewById(R.id.btnAction);
        TextView tvToggle = findViewById(R.id.tvToggleMode);

        btnAction.setOnClickListener(v -> handleAction());

        tvToggle.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            btnAction.setText(isLoginMode ? R.string.login : R.string.register);
            tvToggle.setText(isLoginMode ? R.string.no_account : R.string.have_account);
        });
    }

    private void handleAction() {
        String email    = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        AuthManager.AuthCallback callback = new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                progressBar.setVisibility(View.GONE);
                if (user != null) {
                    SettingsManager sm = new SettingsManager(LoginActivity.this);
                    sm.saveUserProfile(user.getUid(),
                            user.getEmail(),
                            user.getDisplayName());
                    initFirestoreForUser(user.getUid());
                    goToMain();
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);

                String message = parseErrorMessage(e);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();

                android.util.Log.e("LoginActivity", "Auth failed", e);
            }
        };

        if (isLoginMode) {
            AuthManager.getInstance().signIn(email, password, callback);
        } else {
            AuthManager.getInstance().register(email, password, callback);
        }
    }
    private String parseErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) {
            return "Пользователь с таким email не найден.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Пожалуйста, попробуйте снова.";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Этот email уже зарегистрирован.";
        }

        if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "Неверный формат email адреса.";
                case "ERROR_WEAK_PASSWORD":
                    return "Слабый пароль. Пароль должен содержать не менее 6 символов.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Ошибка сети. Проверьте подключение к интернету.";
                case "ERROR_USER_DISABLED":
                    return "Этот аккаунт был отключен.";
                default:
                    return "Неизвестная ошибка авторизации: " + errorCode;
            }
        }

        return "Произошла непредвиденная ошибка.";
    }
    private void initFirestoreForUser(String uid) {
        com.example.myapplication.data.firestore.FirestoreRepository.getInstance(
                new com.example.myapplication.data.firestore.UserPathProvider(uid));
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
