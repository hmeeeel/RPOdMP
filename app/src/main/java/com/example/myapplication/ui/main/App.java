package com.example.myapplication.ui.main;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.example.myapplication.data.firestore.AnonymousPathProvider;
import com.example.myapplication.data.firestore.FirestoreRepository;
import com.example.myapplication.data.firestore.UserPathProvider;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.ui.auth.AuthManager;
import com.example.myapplication.ui.notification.NotificationScheduler;
import com.example.myapplication.ui.notification.NotificationWorker;
import com.example.myapplication.ui.settings.SettingsManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.yandex.mapkit.MapKitFactory;
import com.example.myapplication.BuildConfig;

public class App extends Application {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        AuthManager.getInstance().init(this);
        // 1. Яндекс карты
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY);

        // 2. Firebase
        FirebaseApp.initializeApp(this);

        // 3. Офлайн-персистентность Firestore
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        // 4. Инициализация FirestoreRepository
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(TAG, "User authenticated: " + user.getUid());
            FirestoreRepository.getInstance(new UserPathProvider(user.getUid()));
        } else {
            Log.d(TAG, "No authenticated user, using anonymous path");
            FirestoreRepository.getInstance(new AnonymousPathProvider());
        }

        // Room оставлен для карты и кэша
        PlaceRepository.getInstance(this);

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
            CharSequence name = getString(com.example.myapplication.R.string.notification_channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(NotificationWorker.CHANNEL_ID, name, importance);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
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
            Log.d(TAG, "Notification schedule restored: day=" + settings.getNotificationDayOfWeek() +
                    ", time=" + settings.getNotificationHour() + ":" + settings.getNotificationMinute());
        } else {
            Log.d(TAG, "Notifications disabled in settings");
        }
    }
}