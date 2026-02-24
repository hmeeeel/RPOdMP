package com.example.myapplication.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.example.myapplication.R;

import java.util.Locale;

public class SettingsManager {
    public static final String LANGUAGE_RUSSIAN = "ru";
    public static final String LANGUAGE_ENGLISH = "en";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    private final SharedPreferences preferences;

    public SettingsManager(Context context){
        this.preferences = context.getSharedPreferences("App", Context.MODE_PRIVATE);
    }
    public void setLang(String language){
        preferences.edit().putString("language", language).apply();
    }
    public String getLang() {
        return preferences.getString("language", "ru");
    }

    public void setTheme(String theme){
        preferences.edit().putString("theme", theme).apply();
    }
    public String getTheme(){
        return preferences.getString("theme","light");
    }

    public void applyLang(Context context){
        String lang = getLang();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        context.createConfigurationContext(configuration);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

    }
    public int getThemeId(){
        String theme = getTheme();
        if ("dark".equals(theme)) return R.style.AppTheme_Dark;
        return R.style.AppTheme_Light;
    }

    public boolean isDarkTheme() {
        return "dark".equals(getTheme());
    }

}
