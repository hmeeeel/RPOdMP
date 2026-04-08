package com.example.myapplication.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.example.myapplication.R;

import java.util.Locale;
import java.util.Calendar;
public class SettingsManager {

    public static final String LANGUAGE_RUSSIAN = "ru";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK  = "dark";

    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_NOTIFICATION_DAY      = "notification_day";
    private static final String KEY_NOTIFICATION_HOUR     = "notification_hour";
    private static final String KEY_NOTIFICATION_MINUTE   = "notification_minute";

    private static final int DEFAULT_DAY    = Calendar.SATURDAY;
    private static final int DEFAULT_HOUR   = 10;
    private static final int DEFAULT_MINUTE = 0;

    private final SharedPreferences preferences;

    public SettingsManager(Context context) {
        this.preferences = context.getSharedPreferences("App", Context.MODE_PRIVATE);
    }

    public void setLang(String language) {
        preferences.edit().putString("language", language).apply();
    }

    public String getLang() {
        return preferences.getString("language", "ru");
    }

    public void applyLang(Context context) {
        String lang = getLang();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.setLocale(locale);

        context.createConfigurationContext(configuration);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public void setTheme(String theme) {
        preferences.edit().putString("theme", theme).apply();
    }

    public String getTheme() {
        return preferences.getString("theme", "light");
    }

    public int getThemeId() {
        return "dark".equals(getTheme()) ? R.style.AppTheme_Dark : R.style.AppTheme_Light;
    }

    public boolean isDarkTheme() {
        return "dark".equals(getTheme());
    }

    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);
    }
    public void setNotificationDayOfWeek(int day) {
        preferences.edit().putInt(KEY_NOTIFICATION_DAY, day).apply();
    }

    public int getNotificationDayOfWeek() {
        return preferences.getInt(KEY_NOTIFICATION_DAY, DEFAULT_DAY);
    }

    public void setNotificationHour(int hour) {
        preferences.edit().putInt(KEY_NOTIFICATION_HOUR, hour).apply();
    }

    public int getNotificationHour() {
        return preferences.getInt(KEY_NOTIFICATION_HOUR, DEFAULT_HOUR);
    }

    public void setNotificationMinute(int minute) {
        preferences.edit().putInt(KEY_NOTIFICATION_MINUTE, minute).apply();
    }

    public int getNotificationMinute() {
        return preferences.getInt(KEY_NOTIFICATION_MINUTE, DEFAULT_MINUTE);
    }
}