package com.example.myapplication.data.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import org.json.JSONArray;
import org.json.JSONObject;

//Realtime-подписка для таблицы routes и user_saved_routes. Точная копия паттерна SupabaseRealtimeClient.
public class RouteRealtimeClient {

    private static final String TAG            = "RouteRealtime";
    private static final long   HEARTBEAT_MS   = 25_000;
    private static final long   RECONNECT_MS   = 5_000;

    public interface ChangeCallback { void onChange(); }

    private WebSocket     webSocket;
    private Timer         heartbeatTimer;
    private volatile boolean active = false;

    private ChangeCallback routeChangeCallback;
    private ChangeCallback savedChangeCallback;
    private String userId;

    private final AtomicInteger refCount  = new AtomicInteger(0);
    private final Handler       mainHandler = new Handler(Looper.getMainLooper());

    public void subscribe(String userId,
                          ChangeCallback onRouteChange,
                          ChangeCallback onSavedChange) {
        this.userId              = userId;
        this.routeChangeCallback = onRouteChange;
        this.savedChangeCallback = onSavedChange;
        this.active              = true;
        connectWebSocket();
    }

    public void remove() {
        active               = false;
        routeChangeCallback  = null;
        savedChangeCallback  = null;
        stopHeartbeat();
        if (webSocket != null) { webSocket.close(1000, "disconnect"); webSocket = null; }
    }

    private void connectWebSocket() {
        if (!active) return;
        String token = SupabaseClient.getInstance().getAccessToken();
        if (token == null) return;

        String wsUrl = SupabaseClient.SUPABASE_URL
                .replace("https://", "wss://")
                + "/realtime/v1/websocket?apikey=" + SupabaseClient.SUPABASE_ANON_KEY + "&vsn=1.0.0";

        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = SupabaseClient.getInstance().getHttpClient()
                .newWebSocket(request, new WebSocketListener() {
                    @Override public void onOpen(WebSocket ws, Response response) {
                        sendJoin(ws, "routes",            null);
                        sendJoin(ws, "user_saved_routes", "user_id=eq." + userId);
                        startHeartbeat(ws);
                    }
                    @Override
                    public void onMessage(WebSocket ws, String text) {
                        Log.d(TAG, "RAW: " + text);  // ← добавь эту строку
                        handleMessage(text);
                    }
                    @Override public void onClosing(WebSocket ws, int code, String reason) { ws.close(1000, null); stopHeartbeat(); }
                    @Override public void onFailure(WebSocket ws, Throwable t, Response r) {
                        Log.w(TAG, "WS failure: " + t.getMessage());
                        stopHeartbeat();
                        if (active) mainHandler.postDelayed(RouteRealtimeClient.this::connectWebSocket, RECONNECT_MS);
                    }
                });
    }

    private void handleMessage(String text) {
        try {
            JSONObject msg = new JSONObject(text);
            String event = msg.optString("event", "");

            Log.d(TAG, "RAW: " + text); // временно для диагностики

            if ("postgres_changes".equals(event)) {
                JSONObject payload = msg.optJSONObject("payload");
                if (payload == null) return;

                // payload -> data -> table
                JSONObject data = payload.optJSONObject("data");
                if (data == null) return;

                String table = data.optString("table", "");
                String type  = data.optString("type", "");

                Log.d(TAG, "Change: table=" + table + " type=" + type);

                if ("routes".equals(table)) {
                    if (active && routeChangeCallback != null)
                        mainHandler.post(routeChangeCallback::onChange);

                } else if ("user_saved_routes".equals(table)) {
                    if (active && savedChangeCallback != null)
                        mainHandler.post(savedChangeCallback::onChange);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "handleMessage error", e);
        }
    }

    private void sendJoin(WebSocket ws, String table, String filter) {
        try {
            JSONObject changeFilter = new JSONObject();
            changeFilter.put("event",  "*");
            changeFilter.put("schema", "public");
            changeFilter.put("table",  table);
            if (filter != null) changeFilter.put("filter", filter);

            JSONArray changes = new JSONArray();
            changes.put(changeFilter);

            JSONObject config = new JSONObject();
            config.put("broadcast",        new JSONObject().put("ack", false).put("self", false));
            config.put("presence",         new JSONObject().put("key", ""));
            config.put("postgres_changes", changes);

            JSONObject payload = new JSONObject();
            payload.put("config", config);
            String token = SupabaseClient.getInstance().getAccessToken();
            if (token != null) payload.put("access_token", token);

            JSONObject msg = new JSONObject();
            msg.put("topic",   "realtime:public:" + table);
            msg.put("event",   "phx_join");
            msg.put("payload", payload);
            msg.put("ref",     String.valueOf(refCount.incrementAndGet()));

            ws.send(msg.toString());
        } catch (Exception e) {
            Log.e(TAG, "sendJoin error", e);
        }
    }

    private void startHeartbeat(WebSocket ws) {
        stopHeartbeat();
        heartbeatTimer = new Timer("route-realtime-hb", true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (!active) { cancel(); return; }
                try {
                    JSONObject hb = new JSONObject();
                    hb.put("topic",   "phoenix");
                    hb.put("event",   "heartbeat");
                    hb.put("payload", new JSONObject());
                    hb.put("ref",     String.valueOf(refCount.incrementAndGet()));
                    ws.send(hb.toString());
                } catch (Exception e) { Log.e(TAG, "heartbeat error", e); }
            }
        }, HEARTBEAT_MS, HEARTBEAT_MS);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) { heartbeatTimer.cancel(); heartbeatTimer = null; }
    }
}