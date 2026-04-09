package com.example.myapplication.ui.main;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.ui.notification.NotificationScheduler;
import com.example.myapplication.ui.notification.NotificationWorker;
import com.example.myapplication.ui.settings.SettingsManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yandex.mapkit.MapKitFactory;
import com.example.myapplication.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY);
        PlaceRepository.getInstance(this);

        createNotificationChannel();
        restoreNotificationSchedule();

        FirebaseApp.initializeApp(this);
        Map<String, Object> testData = new HashMap<>();
        testData.put("name", "test_connection");

        FirebaseFirestore.getInstance()
                .collection("test")
                .document()
                .set(testData);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        PlaceRepository.getInstance(this).shutdown();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(com.example.myapplication.R.string.notification_channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(NotificationWorker.CHANNEL_ID, name, importance);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void restoreNotificationSchedule() {
        SettingsManager settings = new SettingsManager(this);
        if (settings.isNotificationsEnabled()) {
            NotificationScheduler.schedule(
                    this,
                    settings.getNotificationDayOfWeek(),
                    settings.getNotificationHour(),
                    settings.getNotificationMinute()
            );
        }
    }
}