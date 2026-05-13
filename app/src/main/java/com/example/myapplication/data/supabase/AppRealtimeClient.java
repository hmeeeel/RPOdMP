package com.example.myapplication.data.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class AppRealtimeClient {

    private static final String TAG          = "AppRealtime";
    private static final long   HEARTBEAT_MS = 25_000;
    private static final long   RECONNECT_MS = 5_000;

    // Простой интерфейс — таблица + тип изменения
    public interface TableChangeCallback {
        void onChange(String table, String type);
    }

    // Описание одной подписки
    public static class Subscription {
        final String table;
        final String filter;

        public Subscription(String table, String filter) {
            this.table  = table;
            this.filter = filter;
        }

        // без фильтра
        public static Subscription of(String table) {
            return new Subscription(table, null);
        }

        // с фильтром
        public static Subscription of(String table, String filter) {
            return new Subscription(table, filter);
        }
    }

    private WebSocket           webSocket;
    private Timer               heartbeatTimer;
    private volatile boolean    active      = false;
    private TableChangeCallback callback;
    private List<Subscription>  subscriptions;

    private final AtomicInteger refCount    = new AtomicInteger(0);
    private final Handler       mainHandler = new Handler(Looper.getMainLooper());

    public void subscribe(List<Subscription> subs, TableChangeCallback callback) {
        this.subscriptions = subs;
        this.callback      = callback;
        this.active        = true;
        connectWebSocket();
    }

    public void remove() {
        active   = false;
        callback = null;
        stopHeartbeat();
        if (webSocket != null) {
            webSocket.close(1000, "disconnect");
            webSocket = null;
        }
    }

    private void connectWebSocket() {
        if (!active) return;

        String token = SupabaseClient.getInstance().getAccessToken();
        if (token == null) return;

        String wsUrl = SupabaseClient.SUPABASE_URL
                .replace("https://", "wss://")
                + "/realtime/v1/websocket?apikey="
                + SupabaseClient.SUPABASE_ANON_KEY
                + "&vsn=1.0.0";

        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = SupabaseClient.getInstance().getHttpClient()
                .newWebSocket(request, new WebSocketListener() {

                    @Override
                    public void onOpen(WebSocket ws, Response response) {
                        Log.d(TAG, "WebSocket opened");
                        for (Subscription sub : subscriptions) {
                            sendJoin(ws, sub);
                        }
                        startHeartbeat(ws);
                    }

                    @Override
                    public void onMessage(WebSocket ws, String text) {
                        handleMessage(text);
                    }

                    @Override
                    public void onClosing(WebSocket ws, int code, String reason) {
                        ws.close(1000, null);
                        stopHeartbeat();
                    }

                    @Override
                    public void onFailure(WebSocket ws, Throwable t, Response r) {
                        Log.w(TAG, "WS failure: " + t.getMessage());
                        stopHeartbeat();
                        if (active) {
                            mainHandler.postDelayed(
                                    AppRealtimeClient.this::connectWebSocket,
                                    RECONNECT_MS
                            );
                        }
                    }
                });
    }

    private void handleMessage(String text) {
        try {
            JSONObject msg   = new JSONObject(text);
            String     event = msg.optString("event", "");

            if ("postgres_changes".equals(event)) {
                JSONObject payload = msg.optJSONObject("payload");
                if (payload == null) return;

                JSONObject data = payload.optJSONObject("data");
                if (data == null) return;

                final String table = data.optString("table", "");
                final String type  = data.optString("type",  "");

                Log.d(TAG, "Change: table=" + table + " type=" + type);

                if (!table.isEmpty() && active && callback != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (active && callback != null) {
                                callback.onChange(table, type);
                            }
                        }
                    });
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "handleMessage error", e);
        }
    }

    private void sendJoin(WebSocket ws, Subscription sub) {
        try {
            JSONObject changeFilter = new JSONObject();
            changeFilter.put("event",  "*");
            changeFilter.put("schema", "public");
            changeFilter.put("table",  sub.table);
            if (sub.filter != null && !sub.filter.isEmpty()) {
                changeFilter.put("filter", sub.filter);
            }

            JSONArray changes = new JSONArray();
            changes.put(changeFilter);

            JSONObject config = new JSONObject();
            config.put("broadcast",
                    new JSONObject().put("ack", false).put("self", false));
            config.put("presence",
                    new JSONObject().put("key", ""));
            config.put("postgres_changes", changes);

            JSONObject payload = new JSONObject();
            payload.put("config", config);
            String token = SupabaseClient.getInstance().getAccessToken();
            if (token != null) payload.put("access_token", token);

            // Уникальный топик для каждой таблицы
            String topic = "realtime:public:" + sub.table;
            if (sub.filter != null) topic += ":" + sub.filter;

            JSONObject joinMsg = new JSONObject();
            joinMsg.put("topic",   topic);
            joinMsg.put("event",   "phx_join");
            joinMsg.put("payload", payload);
            joinMsg.put("ref",     String.valueOf(refCount.incrementAndGet()));

            Log.d(TAG, "JOIN → " + sub.table);
            ws.send(joinMsg.toString());

        } catch (Exception e) {
            Log.e(TAG, "sendJoin error", e);
        }
    }

    private void startHeartbeat(WebSocket ws) {
        stopHeartbeat();
        heartbeatTimer = new Timer("app-realtime-hb", true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                try {
                    JSONObject hb = new JSONObject();
                    hb.put("topic",   "phoenix");
                    hb.put("event",   "heartbeat");
                    hb.put("payload", new JSONObject());
                    hb.put("ref",     String.valueOf(refCount.incrementAndGet()));
                    ws.send(hb.toString());
                } catch (Exception e) {
                    Log.e(TAG, "heartbeat error", e);
                }
            }
        }, HEARTBEAT_MS, HEARTBEAT_MS);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }
}