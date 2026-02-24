package com.example.myapplication.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.ui.settings.SettingsManager;

public abstract class BaseActivity extends AppCompatActivity {
    protected SettingsManager settingsManager;
    private String Language;
    private String Theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settingsManager = new SettingsManager(getApplicationContext());
        Language = settingsManager.getLang();
        Theme = settingsManager.getTheme();

        settingsManager.applyLang(this);
        setTheme(settingsManager.getThemeId());

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SettingsManager current = new SettingsManager(getApplicationContext());

        String newLanguage = current.getLang();
        String newTheme = current.getTheme();

        boolean languageChanged = !newLanguage.equals(Language);
        boolean themeChanged = !newTheme.equals(Theme);

        if (languageChanged || themeChanged) recreate();
    }
    
}
