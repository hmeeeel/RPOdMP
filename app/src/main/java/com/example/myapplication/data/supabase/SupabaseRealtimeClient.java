package com.example.myapplication.data.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;

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

public class SupabaseRealtimeClient {

    private static final String TAG              = "SupabaseRealtime";
    private static final long   HEARTBEAT_MS     = 25_000;
    private static final long   RECONNECT_DELAY_MS = 5_000;

    private WebSocket webSocket;
    private Timer heartbeatTimer;
    private final AtomicInteger refCount = new AtomicInteger(0);
    private volatile boolean    active   = false;
    private RealtimeDataCallback callback;
    private String userId;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RealtimeDataCallback {
        void onData(List<Place> places);
        void onError(Exception e);
    }

    public void subscribe(String userId, RealtimeDataCallback callback) {
        this.userId   = userId;
        this.callback = callback;
        this.active   = true;

        fetchAll();
        connectWebSocket();
    }

    public void refresh() {
        fetchAll();
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
        if (token == null) {
            Log.w(TAG, "No access token, skipping WebSocket");
            return;
        }

        String wsUrl = SupabaseClient.SUPABASE_URL
                .replace("https://", "wss://")
                .replace("http://",  "ws://")
                + "/realtime/v1/websocket"
                + "?apikey=" + SupabaseClient.SUPABASE_ANON_KEY
                + "&vsn=1.0.0";

        Request request = new Request.Builder().url(wsUrl).build();

        webSocket = SupabaseClient.getInstance().getHttpClient()
                .newWebSocket(request, new WebSocketListener() {

                    @Override
                    public void onOpen(WebSocket ws, Response response) {
                        Log.d(TAG, "WebSocket opened");
                        sendJoin(ws);
                        startHeartbeat(ws);
                    }

                    @Override
                    public void onMessage(WebSocket ws, String text) {
                        Log.v(TAG, "WS message: " + text);
                        handleMessage(text);
                    }

                    @Override
                    public void onClosing(WebSocket ws, int code, String reason) {
                        ws.close(1000, null);
                        stopHeartbeat();
                    }

                    @Override
                    public void onFailure(WebSocket ws, Throwable t, Response response) {
                        Log.w(TAG, "WebSocket failure: " + t.getMessage());
                        stopHeartbeat();
                        if (active) {
                            mainHandler.postDelayed(
                                    SupabaseRealtimeClient.this::connectWebSocket,
                                    RECONNECT_DELAY_MS);
                        }
                    }
                });
    }

    private void handleMessage(String text) {
        Log.d(TAG, "RAW MESSAGE: " + text);
        try {
            JSONObject msg = new JSONObject(text);
            String event = msg.optString("event", "");

            if ("postgres_changes".equals(event)) {
                Log.d(TAG, "🔥🔥🔥 DATABASE CHANGE RECEIVED! 🔥🔥🔥");
                JSONObject payload = msg.optJSONObject("payload");
                if (payload != null) {
                    JSONObject data = payload.optJSONObject("data");
                    if (data != null) {
                        String type = data.optString("type");
                        Log.d(TAG, "Change type: " + type); // INSERT, UPDATE, DELETE
                    }
                }
                fetchAll();
                return;
            }

            switch (event) {
                case "phx_reply":
                    JSONObject payload = msg.optJSONObject("payload");
                    if (payload != null) {
                        String status = payload.optString("status", "");
                        Log.d(TAG, "phx_reply status: " + status);
                    }
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "handleMessage error", e);
        }
    }

    private void sendJoin(WebSocket ws) {
        try {
            JSONObject changeFilter = new JSONObject();
            changeFilter.put("event", "*");
            changeFilter.put("schema", "public");
            changeFilter.put("table", "places");

           if (userId != null && !userId.isEmpty()) {
                changeFilter.put("filter", "user_id=eq." + userId);
            }

            JSONArray postgresChanges = new JSONArray();
            postgresChanges.put(changeFilter);

            JSONObject config = new JSONObject();
            config.put("postgres_changes", postgresChanges);

            JSONObject joinPayload = new JSONObject();
            joinPayload.put("config", config);

            String token = SupabaseClient.getInstance().getAccessToken();
            if (token != null) {
                joinPayload.put("access_token", token);
            }

            JSONObject joinMsg = new JSONObject();
            joinMsg.put("topic", "realtime:public");
            joinMsg.put("event", "phx_join");
            joinMsg.put("payload", joinPayload);
            joinMsg.put("ref", String.valueOf(refCount.incrementAndGet()));

            Log.d(TAG, "📡 Sending phx_join: " + joinMsg.toString());
            ws.send(joinMsg.toString());

        } catch (Exception e) {
            Log.e(TAG, "sendJoin error", e);
        }
    }

    private void startHeartbeat(WebSocket ws) {
        stopHeartbeat();
        heartbeatTimer = new Timer("realtime-heartbeat", true);
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

    private void fetchAll() {
        SupabaseRepository.getInstance().getAll(
                new PlaceRepository.DataCallback<List<Place>>() {
                    @Override
                    public void onSuccess(List<Place> data) {
                        if (active && callback != null) {
                            mainHandler.post(() -> {
                                if (active && callback != null) callback.onData(data);
                            });
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        if (active && callback != null) {
                            mainHandler.post(() -> {
                                if (active && callback != null) callback.onError(e);
                            });
                        }
                    }
                });
    }
}