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

    private static final String TAG = "SupabaseRealtime";
    private static final long HEARTBEAT_MS = 25_000;
    private static final long RECONNECT_DELAY_MS = 5_000;

    private WebSocket webSocket;
    private Timer heartbeatTimer;
    private final AtomicInteger refCount = new AtomicInteger(0);
    private volatile boolean active = false;
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

        // 1. Немедленная начальная загрузка данных
        fetchAll();

        // 2. Открыть WebSocket для realtime обновлений
        connectWebSocket();
    }

    // после add/update/delete
    public void refresh() {
        fetchAll();
    }

    // Отписка
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

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

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
                        Log.d(TAG, "WebSocket closing: " + code + " " + reason);
                        ws.close(1000, null);
                        stopHeartbeat();
                    }

                    @Override
                    public void onFailure(WebSocket ws, Throwable t, Response response) {
                        Log.w(TAG, "WebSocket failure: " + t.getMessage()
                                + (response != null ? " code=" + response.code() : ""));
                        stopHeartbeat();
                        // Переподключение через 5 секунд
                        if (active) {
                            mainHandler.postDelayed(() -> connectWebSocket(), RECONNECT_DELAY_MS);
                        }
                    }
                });
    }


    private void handleMessage(String text) {
        try {
            JSONObject msg = new JSONObject(text);
            String event   = msg.optString("event", "");

            switch (event) {
                case "phx_reply":
                    // Ответ на join — проверяем успех
                    JSONObject payload = msg.optJSONObject("payload");
                    if (payload != null) {
                        String status = payload.optString("status", "");
                        Log.d(TAG, "phx_reply status: " + status);
                        if ("ok".equals(status)) {
                            Log.d(TAG, "Realtime subscription confirmed");
                        }
                    }
                    break;

                case "postgres_changes":
                    // Изменение в таблице — перезагрузить данные
                    Log.d(TAG, "postgres_changes detected, refreshing...");
                    fetchAll();
                    break;

                case "phx_error":
                    Log.w(TAG, "phx_error received, reconnecting...");
                    if (active) {
                        mainHandler.postDelayed(() -> connectWebSocket(), RECONNECT_DELAY_MS);
                    }
                    break;

                case "heartbeat":
                    // Игнорируем ответы на heartbeat
                    break;

                default:
                    // Также проверяем payload на тип postgres_changes
                    // (некоторые версии Supabase отправляют иначе)
                    JSONObject p = msg.optJSONObject("payload");
                    if (p != null && p.has("data")) {
                        JSONObject data = p.optJSONObject("data");
                        if (data != null && data.has("type")) {
                            Log.d(TAG, "Change in payload.data.type, refreshing...");
                            fetchAll();
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "handleMessage error", e);
        }
    }


    private void sendJoin(WebSocket ws) {
        try {
            // Фильтр по user_id чтобы слушать только свои записи
            JSONObject changeFilter = new JSONObject();
            changeFilter.put("event",  "*");          // INSERT, UPDATE, DELETE
            changeFilter.put("schema", "public");
            changeFilter.put("table",  "places");
            if (userId != null && !userId.isEmpty()) {
                changeFilter.put("filter", "user_id=eq." + userId);
            }

            JSONArray changes = new JSONArray();
            changes.put(changeFilter);

            JSONObject broadcastConfig = new JSONObject();
            broadcastConfig.put("ack",  false);
            broadcastConfig.put("self", false);

            JSONObject presenceConfig = new JSONObject();
            presenceConfig.put("key", "");

            JSONObject config = new JSONObject();
            config.put("broadcast",       broadcastConfig);
            config.put("presence",        presenceConfig);
            config.put("postgres_changes", changes);

            JSONObject payload = new JSONObject();
            payload.put("config", config);

            // Передаём access token в payload для авторизации
            String token = SupabaseClient.getInstance().getAccessToken();
            if (token != null) {
                payload.put("access_token", token);
            }

            JSONObject msg = new JSONObject();
            msg.put("topic",   "realtime:public:places");
            msg.put("event",   "phx_join");
            msg.put("payload", payload);
            msg.put("ref",     String.valueOf(refCount.incrementAndGet()));

            boolean sent = ws.send(msg.toString());
            Log.d(TAG, "phx_join sent: " + sent);

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