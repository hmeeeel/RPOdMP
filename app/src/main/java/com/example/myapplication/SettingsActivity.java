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

public class SettingsActivity extends BaseActivity {
    private RadioGroup languageRadioGroup;
    private RadioButton radioRussian, radioEnglish;
    private SwitchCompat themeSwitch;
    private boolean isInitialized = false;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();

        languageRadioGroup = findViewById(R.id.languageRadioGroup);
        radioRussian = findViewById(R.id.radioRussian);
        radioEnglish = findViewById(R.id.radioEnglish);
        themeSwitch = findViewById(R.id.themeSwitch);

        loadCurSettings();
        setupListeners();

        setupBottomNavigation();

        isInitialized = true;
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
                Toast.makeText(this, getString(R.string.map), Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_favorites) {
                Intent intent = new Intent(this, AddMuseumActivity.class);
                startActivity(intent);
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

    private void loadCurSettings() {
        String currentLanguage = settingsManager.getLang();
        if (SettingsManager.LANGUAGE_RUSSIAN.equals(currentLanguage)) {
            radioRussian.setChecked(true);
        } else {
            radioEnglish.setChecked(true);
        }

        boolean isDark = settingsManager.isDarkTheme();
        themeSwitch.setChecked(isDark);
    }

    private void setupListeners() {
        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isInitialized) return;

            String newLanguage;
            if (checkedId == R.id.radioRussian) {
                newLanguage = SettingsManager.LANGUAGE_RUSSIAN;
            } else {
                newLanguage = SettingsManager.LANGUAGE_ENGLISH;
            }

            if (!newLanguage.equals(settingsManager.getLang())) {
                settingsManager.setLang(newLanguage);
                recreate();
            }
        });

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitialized) return;

            String newTheme = isChecked ?
                    SettingsManager.THEME_DARK : SettingsManager.THEME_LIGHT;

            if (!newTheme.equals(settingsManager.getTheme())) {
                settingsManager.setTheme(newTheme);
                recreate();
            }
        });
    }

    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_settings);
        }
    }
}
