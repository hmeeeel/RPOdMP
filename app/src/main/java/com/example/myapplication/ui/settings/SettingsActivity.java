package com.example.myapplication.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.example.myapplication.data.db.MuseumDB;
import com.example.myapplication.data.firestore.FirestoreRepository;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.auth.AuthManager;
import com.example.myapplication.ui.auth.LoginActivity;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.map.MapActivity;
import com.example.myapplication.ui.notification.NotificationScheduler;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends BaseActivity {

    private RadioGroup   languageRadioGroup;
    private RadioButton  radioRussian, radioEnglish;
    private SwitchCompat themeSwitch;

    private SwitchCompat notificationSwitch;
    private TextView     textNotificationDay;
    private TextView     textNotificationTime;
    Button btnLogout, btnDeleteAccount;
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

         btnLogout = findViewById(R.id.btnLogout);
         btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        if (AuthManager.getInstance().isLoggedIn()) btnDeleteAccount.setVisibility(View.VISIBLE);
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

        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }
    private void showLogoutDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.logout_confirm))
                .setPositiveButton(getString(R.string.yes), (d, w) -> performLogout())
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            int color = settingsManager.isDarkTheme() ?
                    ContextCompat.getColor(this, R.color.light) :
                    ContextCompat.getColor(this, R.color.dark);
            positiveButton.setTextColor(color);
        }

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            int color = settingsManager.isDarkTheme() ?
                    ContextCompat.getColor(this, R.color.light) :
                    ContextCompat.getColor(this, R.color.dark);
            negativeButton.setTextColor(color);
        }
    }

    private void performLogout() {
        settingsManager.saveLogoutTime(System.currentTimeMillis()); // для 7-дневного бездействия

        AuthManager.getInstance().signOut();

        new Thread(() -> {
            com.example.myapplication.data.db.MuseumDB.getInstance(this)
                    .clearAllTables();
        }).start();

        Intent intent = new Intent(this,
                com.example.myapplication.ui.auth.LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDeleteAccountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reauth, null);
        TextInputEditText editEmail    = dialogView.findViewById(R.id.editReauthEmail);
        TextInputEditText editPassword = dialogView.findViewById(R.id.editReauthPassword);

        // Применяем тему к полям ввода внутри диалога
        dialogView.getContext().setTheme(R.style.TextInputTheme);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_account_confirm))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.delete), (d, w) -> {
                    String email = editEmail.getText().toString().trim();
                    String pass  = editPassword.getText().toString().trim();
                    if (email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reauthAndDelete(email, pass);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        dialog.show();

        // Точно так же меняем цвет кнопок
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            // Для кнопки удаления лучше использовать красный цвет
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        }

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            int color = settingsManager.isDarkTheme() ?
                    ContextCompat.getColor(this, R.color.light) :
                    ContextCompat.getColor(this, R.color.dark);
            negativeButton.setTextColor(color);
        }
    }

    private void reauthAndDelete(String email, String password) {
        AuthManager.getInstance().reauthenticate(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                FirestoreRepository repo = FirestoreRepository.getInstance();
                repo.getAll(new PlaceRepository.DataCallback<java.util.List<Place>>() {
                    @Override
                    public void onSuccess(java.util.List<Place> places) {
                        for (Place p : places) {
                            if (p.getFirestoreId() != null)
                                repo.delete(p.getFirestoreId(), new PlaceRepository.DataCallback<Void>() {
                                    public void onSuccess(Void v) {}
                                    public void onError(Exception e) {}
                                });
                        }
                        AuthManager.getInstance().deleteAccount(new AuthManager.AuthCallback() {
                            @Override
                            public void onSuccess(com.google.firebase.auth.FirebaseUser u) {
                                settingsManager.clearUserProfile();
                                new Thread(() -> MuseumDB.getInstance(SettingsActivity.this)
                                        .clearAllTables()).start();
                                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    @Override
                    public void onError(Exception e) { /* Firestore недоступен - всё равно удаляем аккаунт */ }
                });
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(SettingsActivity.this,
                        getString(R.string.error_reauth), Toast.LENGTH_LONG).show();
            }
        });
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

        AlertDialog d = new AlertDialog.Builder(this)
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

        Button negativeButton = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            int color = settingsManager.isDarkTheme() ?
                    ContextCompat.getColor(this, R.color.light) :
                    ContextCompat.getColor(this, R.color.dark);
            negativeButton.setTextColor(color);
        }
    }

    private void showTimePicker() {
        int hour   = settingsManager.getNotificationHour();
        int minute = settingsManager.getNotificationMinute();

        TimePickerDialog dialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    settingsManager.setNotificationHour(selectedHour);
                    settingsManager.setNotificationMinute(selectedMinute);
                    updateTimeLabel(selectedHour, selectedMinute);
                    rescheduleIfEnabled();
                }, hour, minute, true);

        dialog.show();

        Button positiveButton = dialog.getButton(TimePickerDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            int color = settingsManager.isDarkTheme() ?
                    ContextCompat.getColor(this, R.color.light) :
                    ContextCompat.getColor(this, R.color.dark);
            positiveButton.setTextColor(color);
        }

        Button negativeButton = dialog.getButton(TimePickerDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            int color = settingsManager.isDarkTheme() ?
                    ContextCompat.getColor(this, R.color.light) :
                    ContextCompat.getColor(this, R.color.dark);
            negativeButton.setTextColor(color);
        }
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