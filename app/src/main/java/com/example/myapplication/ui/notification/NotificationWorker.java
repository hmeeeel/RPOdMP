package com.example.myapplication.ui.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.R;
import com.example.myapplication.data.firestore.FirestoreRepository;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationWorker extends Worker {

    public static final String CHANNEL_ID = "museum_reminders";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "NotificationWorker";

    public NotificationWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "Пользователь не залогинен - уведомление не отправляется");
            return Result.success();
        }

        Log.d(TAG, "Проверка непосещённых мест для пользователя: " + currentUser.getEmail());

        int unvisitedCount = countUnvisitedPlacesFromFirestore(context);

        if (unvisitedCount == 0) {
            Log.d(TAG, "Нет непосещённых мест");
            return Result.success();
        }

        PendingIntent pendingIntent = buildPendingIntent(context);
        sendNotification(context, unvisitedCount, pendingIntent);

        Log.d(TAG, "Уведомление отправлено: " + unvisitedCount + " мест");
        return Result.success();
    }

    private int countUnvisitedPlacesFromFirestore(Context context) {
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        FirestoreRepository repository = FirestoreRepository.getInstance();

        repository.getAll(new PlaceRepository.DataCallback<List<Place>>() {
            @Override
            public void onSuccess(List<Place> places) {
                int unvisited = 0;
                for (Place place : places) {
                    if (!place.isVisited()) {
                        unvisited++;
                    }
                }
                count.set(unvisited);
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки мест из Firestore", e);
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timeout при загрузке данных", e);
        }

        return count.get();
    }

    private PendingIntent buildPendingIntent(Context context) {
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(openAppIntent);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return stackBuilder.getPendingIntent(0, flags);
    }

    private void sendNotification(Context context, int unvisitedCount, PendingIntent pendingIntent) {
        String text = context.getResources().getQuantityString(
                R.plurals.notification_text,
                unvisitedCount,
                unvisitedCount
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home_24dp)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}