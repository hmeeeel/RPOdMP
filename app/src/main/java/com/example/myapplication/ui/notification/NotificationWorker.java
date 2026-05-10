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
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.supabase.SupabaseClient;
import com.example.myapplication.data.supabase.SupabaseRepository;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.settings.SettingsManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationWorker extends Worker {

    public static final String CHANNEL_ID = "museum_reminders";
    private static final int   NOTIF_ID   = 1001;
    private static final String TAG        = "NotificationWorker";

    public NotificationWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // Восстановить сессию из SharedPreferences
        SettingsManager sm = new SettingsManager(ctx);
        String token  = sm.getAccessToken();
        String userId = sm.getUserId();
        if (token == null || userId == null) {
            Log.d(TAG, "No session — skipping notification");
            return Result.success();
        }
        SupabaseClient.getInstance().setSession(token, sm.getRefreshToken(), userId);

        int count = countUnvisited();
        if (count == 0) return Result.success();

        sendNotification(ctx, count, buildPendingIntent(ctx));
        return Result.success();
    }

    private int countUnvisited() {
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        SupabaseRepository.getInstance().getAll(new PlaceRepository.DataCallback<List<Place>>() {
            @Override public void onSuccess(List<Place> places) {
                for (Place p : places) if (!p.isVisited()) count.incrementAndGet();
                latch.countDown();
            }
            @Override public void onError(Exception e) {
                Log.e(TAG, "getAll error", e);
                latch.countDown();
            }
        });

        try { latch.await(15, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return count.get();
    }

    private PendingIntent buildPendingIntent(Context ctx) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TaskStackBuilder stack = TaskStackBuilder.create(ctx);
        stack.addNextIntentWithParentStack(intent);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return stack.getPendingIntent(0, flags);
    }

    private void sendNotification(Context ctx, int count, PendingIntent pi) {
        String text = ctx.getResources().getQuantityString(R.plurals.notification_text, count, count);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home_24dp)
                .setContentTitle(ctx.getString(R.string.notification_title))
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager mgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mgr != null) mgr.notify(NOTIF_ID, builder.build());
    }
}