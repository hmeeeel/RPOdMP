package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.data.supabase.SupabaseUser;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.settings.SettingsManager;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends BaseActivity {

    private TextInputEditText editEmail, editPassword;
    private View progressBar;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AuthManager.getInstance().isLoggedIn()) { goToMain(); return; }

        setContentView(R.layout.activity_login);

        editEmail   = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        progressBar  = findViewById(R.id.progressBar);

        Button     btnAction = findViewById(R.id.btnAction);
        TextView   tvToggle  = findViewById(R.id.tvToggleMode);

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
            public void onSuccess(SupabaseUser user) {
                progressBar.setVisibility(View.GONE);
                if (user == null) return;

                // Сохранить сессию в SharedPreferences
                SettingsManager sm = new SettingsManager(LoginActivity.this);
                sm.saveUserProfile(user.getUid(), user.getEmail(), user.getDisplayName());
                // Сохранить токены для восстановления после перезапуска
                com.example.myapplication.data.supabase.SupabaseClient sc =
                        com.example.myapplication.data.supabase.SupabaseClient.getInstance();
                sm.saveSession(sc.getAccessToken(), sc.getRefreshToken(), user.getUid());

                goToMain();
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                String msg = parseErrorMessage(e.getMessage());
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        };

        if (isLoginMode) AuthManager.getInstance().signIn(email, password, callback);
        else             AuthManager.getInstance().register(email, password, callback);
    }

    private String parseErrorMessage(String error) {
        if (error == null) return "Произошла ошибка";
        if (error.contains("email_confirmation_required"))
            return "Подтвердите email. Письмо отправлено на " + editEmail.getText();
        if (error.contains("Invalid login credentials"))
            return "Неверный email или пароль.";
        if (error.contains("User already registered"))
            return "Этот email уже зарегистрирован.";
        if (error.contains("Unable to validate"))
            return "Неверный email или пароль.";
        return error;
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
