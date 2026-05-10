package com.example.myapplication.ui.main;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.example.myapplication.data.supabase.SupabaseClient;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.ui.auth.AuthManager;
import com.example.myapplication.ui.notification.NotificationScheduler;
import com.example.myapplication.ui.notification.NotificationWorker;
import com.example.myapplication.ui.settings.SettingsManager;
import com.yandex.mapkit.MapKitFactory;
import com.example.myapplication.BuildConfig;

public class App extends Application {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Yandex MapKit
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY);

        // 2. AuthManager init
        AuthManager.getInstance().init(this);

        // 3. Восстановить сессию Supabase из SharedPreferences
        SettingsManager settings = new SettingsManager(this);
        String token   = settings.getAccessToken();
        String refresh = settings.getRefreshToken();
        String userId  = settings.getUserId();
        if (token != null && userId != null) {
            Log.d(TAG, "Restoring Supabase session for user: " + userId);
            SupabaseClient.getInstance().setSession(token, refresh, userId);
        }

        // 4. Room (для карты и кэша)
        PlaceRepository.getInstance(this);

        // 5. Уведомления
        createNotificationChannel();
        restoreNotificationSchedule();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        PlaceRepository.getInstance(this).shutdown();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NotificationWorker.CHANNEL_ID,
                    getString(com.example.myapplication.R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void restoreNotificationSchedule() {
        SettingsManager settings = new SettingsManager(this);
        if (settings.isNotificationsEnabled()) {
            NotificationScheduler.schedule(this,
                    settings.getNotificationDayOfWeek(),
                    settings.getNotificationHour(),
                    settings.getNotificationMinute());
        }
    }
}