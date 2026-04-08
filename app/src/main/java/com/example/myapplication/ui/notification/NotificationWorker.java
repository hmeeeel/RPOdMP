package com.example.myapplication.ui.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.R;
import com.example.myapplication.data.db.MuseumDB;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.ui.main.MainActivity;

import java.util.List;

public class NotificationWorker extends Worker {

    public static final String CHANNEL_ID = "museum_reminders";
    private static final int NOTIFICATION_ID = 1001;

    public NotificationWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        int unvisitedCount = countUnvisitedPlaces(context);

        if (unvisitedCount == 0) return Result.success();

        PendingIntent pendingIntent = buildPendingIntent(context);
        sendNotification(context, unvisitedCount, pendingIntent);

        return Result.success();
    }
    private int countUnvisitedPlaces(Context context) {
        List<Place> all = MuseumDB.getInstance(context).placeDAO().getAllPlaces();
        int count = 0;
        for (Place place : all) {
            if (!place.isVisited()) count++;
        }
        return count;
    }

    private PendingIntent buildPendingIntent(Context context) {
        Intent openAppIntent = new Intent(context, MainActivity.class);

        // FLAG_ACTIVITY_SINGLE_TOP - не создавать новый экземпляр, а вызвать у существующего
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(openAppIntent);

        // FLAG_UPDATE_CURRENT — обновить вместо создания дубликата.
        // FLAG_IMMUTABLE — запрет на изм Intent после создания PendingIntent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

        return stackBuilder.getPendingIntent(0, flags);
    }


    private void sendNotification(Context context, int unvisitedCount, PendingIntent pendingIntent) {
        String text = context.getResources().getQuantityString(R.plurals.notification_text, unvisitedCount, unvisitedCount);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home_24dp)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text)
                .setContentIntent(pendingIntent) // что происходит при нажатии на уведомление
                .setAutoCancel(true) // убирает уведомление после нажатия
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, builder.build());
    }
}
