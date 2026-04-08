package com.example.myapplication.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.map.MapActivity;
import com.example.myapplication.ui.notification.NotificationScheduler;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends BaseActivity {

    private RadioGroup   languageRadioGroup;
    private RadioButton  radioRussian, radioEnglish;
    private SwitchCompat themeSwitch;

    private SwitchCompat notificationSwitch;
    private TextView     textNotificationDay;
    private TextView     textNotificationTime;

    private static final int[] DAY_OF_WEEK_VALUES = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    };

    // Запрос разрешения POST_NOTIFICATIONS
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            applyNotificationEnabled(true);
                        } else {
                            notificationSwitch.setChecked(false);
                            Toast.makeText(this,
                                    getString(R.string.notification_permission_denied),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();
        bindViews();
        loadCurrentSettings();
        setupListeners();
        setupBottomNavigation();
    }

    private void bindViews() {
        languageRadioGroup  = findViewById(R.id.languageRadioGroup);
        radioRussian        = findViewById(R.id.radioRussian);
        radioEnglish        = findViewById(R.id.radioEnglish);
        themeSwitch         = findViewById(R.id.themeSwitch);
        notificationSwitch  = findViewById(R.id.notificationSwitch);
        textNotificationDay  = findViewById(R.id.textNotificationDay);
        textNotificationTime = findViewById(R.id.textNotificationTime);
    }

    private void loadCurrentSettings() {
        // Язык
        if (SettingsManager.LANGUAGE_RUSSIAN.equals(settingsManager.getLang())) {
            radioRussian.setChecked(true);
        } else {
            radioEnglish.setChecked(true);
        }

        // Тема
        themeSwitch.setChecked(settingsManager.isDarkTheme());

        // Уведомления
        boolean enabled = settingsManager.isNotificationsEnabled();
        notificationSwitch.setChecked(enabled);
        updateDayLabel(settingsManager.getNotificationDayOfWeek());
        updateTimeLabel(settingsManager.getNotificationHour(),
                settingsManager.getNotificationMinute());
        setNotificationPickersEnabled(enabled);
    }


    private void setupListeners() {
        // Язык
        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newLang = (checkedId == R.id.radioRussian)
                    ? SettingsManager.LANGUAGE_RUSSIAN
                    : SettingsManager.LANGUAGE_ENGLISH;
            if (!newLang.equals(settingsManager.getLang())) {
                settingsManager.setLang(newLang);
                recreate();
            }
        });

        // Тема
        themeSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            String newTheme = isChecked ? SettingsManager.THEME_DARK : SettingsManager.THEME_LIGHT;
            if (!newTheme.equals(settingsManager.getTheme())) {
                settingsManager.setTheme(newTheme);
                recreate();
            }
        });

        // Уведомления
        notificationSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                requestNotificationPermissionIfNeeded();
            } else {
                applyNotificationEnabled(false);
            }
        });
        textNotificationDay.setOnClickListener(v -> showDayPickerDialog());
        textNotificationTime.setOnClickListener(v -> showTimePicker());
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        applyNotificationEnabled(true);
    }

    private void applyNotificationEnabled(boolean enabled) {
        settingsManager.setNotificationsEnabled(enabled);
        setNotificationPickersEnabled(enabled);

        if (enabled) {
            NotificationScheduler.schedule(
                    this,
                    settingsManager.getNotificationDayOfWeek(),
                    settingsManager.getNotificationHour(),
                    settingsManager.getNotificationMinute()
            );
        } else {
            NotificationScheduler.cancel(this);
        }
    }

    private void showDayPickerDialog() {
        String[] dayNames = getResources().getStringArray(R.array.days_of_week);
        int currentDay    = settingsManager.getNotificationDayOfWeek();
        int currentIndex  = dayOfWeekToIndex(currentDay);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.notification_pick_day))
                .setSingleChoiceItems(dayNames, currentIndex, (dialog, which) -> {
                    int selectedDay = DAY_OF_WEEK_VALUES[which];
                    settingsManager.setNotificationDayOfWeek(selectedDay);
                    updateDayLabel(selectedDay);
                    rescheduleIfEnabled();
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showTimePicker() {
        int hour   = settingsManager.getNotificationHour();
        int minute = settingsManager.getNotificationMinute();

        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            settingsManager.setNotificationHour(selectedHour);
            settingsManager.setNotificationMinute(selectedMinute);
            updateTimeLabel(selectedHour, selectedMinute);
            rescheduleIfEnabled();
        }, hour, minute, true).show();
    }

    private void rescheduleIfEnabled() {
        if (settingsManager.isNotificationsEnabled()) {
            NotificationScheduler.schedule(
                    this,
                    settingsManager.getNotificationDayOfWeek(),
                    settingsManager.getNotificationHour(),
                    settingsManager.getNotificationMinute()
            );
        }
    }

    private void updateDayLabel(int dayOfWeek) {
        String[] dayNames = getResources().getStringArray(R.array.days_of_week);
        int index = dayOfWeekToIndex(dayOfWeek);
        textNotificationDay.setText(
                getString(R.string.notification_day_label) + " " + dayNames[index]);
    }

    private void updateTimeLabel(int hour, int minute) {
        textNotificationTime.setText(
                getString(R.string.notification_time_label)
                        + " " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    private void setNotificationPickersEnabled(boolean enabled) {
        textNotificationDay.setEnabled(enabled);
        textNotificationTime.setEnabled(enabled);
        float alpha = enabled ? 1f : 0.4f;
        textNotificationDay.setAlpha(alpha);
        textNotificationTime.setAlpha(alpha);
    }

    private int dayOfWeekToIndex(int calendarDay) {
        for (int i = 0; i < DAY_OF_WEEK_VALUES.length; i++) {
            if (DAY_OF_WEEK_VALUES[i] == calendarDay) return i;
        }
        return 5; // суббота
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                return true;
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, AddMuseumActivity.class));
                return true;
            }
            return id == R.id.nav_settings;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_settings);
    }
}