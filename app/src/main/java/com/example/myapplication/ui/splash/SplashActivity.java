package com.example.myapplication.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.myapplication.ui.admin.AdminPanelActivity;
import com.example.myapplication.ui.auth.AuthManager;
import com.example.myapplication.ui.auth.LoginActivity;
import com.example.myapplication.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> true);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Если не залогинен — на LoginActivity
            if (!AuthManager.getInstance().isLoggedIn()) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            // Залогинен — проверяем email
            String email = com.example.myapplication.data.supabase.SupabaseClient
                    .getInstance().getUserEmail();

            Log.d("SplashActivity", "Залогинен: " + email);

            Class<?> next;
            if ("admin@gmail.com".equalsIgnoreCase(email)) {
                Log.d("SplashActivity", "→ AdminPanelActivity");
                next = AdminPanelActivity.class;
            } else {
                Log.d("SplashActivity", "→ MainActivity");
                next = MainActivity.class;
            }

            startActivity(new Intent(this, next));
            finish();

        }, 2000);
    }
}
