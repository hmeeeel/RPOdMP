package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();

        RadioGroup languageRadioGroup = findViewById(R.id.languageRadioGroup);
        RadioButton radioRussian = findViewById(R.id.radioRussian);
        RadioButton radioEnglish = findViewById(R.id.radioEnglish);
        SwitchCompat themeSwitch = findViewById(R.id.themeSwitch);

        setupBottomNavigation();

    }
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setSelectedItemId(R.id.nav_settings);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                Toast.makeText(this, "Карта", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_favorites) {
                Toast.makeText(this, "Добавить", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }

            return false;
        });
    }

    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
