package com.example.myapplication.ui.notification;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private static final String WORK_NAME = "museum_weekly_reminder";

    private NotificationScheduler() {}

    public static void schedule(Context context, int dayOfWeek, int hour, int minute) {
        long initialDelay = calculateInitialDelay(dayOfWeek, hour, minute);

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NotificationWorker.class, 7, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest
        );
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    private static long calculateInitialDelay(int dayOfWeek, int hour, int minute) {
        Calendar target = Calendar.getInstance();
        target.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        long now = System.currentTimeMillis();
        long targetTime = target.getTimeInMillis();

        if (targetTime <= now) targetTime += TimeUnit.DAYS.toMillis(7);

        return targetTime - now;
    }
}
