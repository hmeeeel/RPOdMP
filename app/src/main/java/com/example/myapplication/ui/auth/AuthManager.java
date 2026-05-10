package com.example.myapplication.ui.auth;

import android.content.Context;
import android.util.Log;

import androidx.work.WorkManager;

import com.example.myapplication.data.supabase.SupabaseClient;
import com.example.myapplication.data.supabase.SupabaseUser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthManager {

    private static final String TAG = "AuthManager";
    private static AuthManager instance;
    private Context appContext;

    private AuthManager() {}

    public static AuthManager getInstance() {
        if (instance == null) instance = new AuthManager();
        return instance;
    }

    public void init(Context context) {
        appContext = context.getApplicationContext();
    }


    public boolean isLoggedIn() {
        return SupabaseClient.getInstance().isLoggedIn();
    }

    public SupabaseUser getCurrentUser() {
        SupabaseClient c = SupabaseClient.getInstance();
        if (!c.isLoggedIn()) return null;
        return new SupabaseUser(c.getUserId(), "");
    }


    // ДОРАБОТАТЬ С ТОКЕНОМ
    public void signIn(String email, String password, AuthCallback callback) {
        String bodyJson = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(bodyJson, SupabaseClient.JSON);

        SupabaseClient.getInstance().getHttpClient()
                .newCall(SupabaseClient.getInstance()
                        .authRequest("/token?grant_type=password").post(body).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        postError(callback, e);
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        handleAuthResponse(response, callback);
                    }
                });
    }


    public void register(String email, String password, AuthCallback callback) {
        String bodyJson = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(bodyJson, SupabaseClient.JSON);

        SupabaseClient.getInstance().getHttpClient()
                .newCall(SupabaseClient.getInstance()
                        .authRequest("/signup").post(body).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        postError(callback, e);
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        handleAuthResponse(response, callback);
                    }
                });
    }

    public void signOut() {
        if (appContext != null)
            WorkManager.getInstance(appContext).cancelAllWorkByTag("weekly_reminder");

        String token = SupabaseClient.getInstance().getAccessToken();
        if (token != null) {
            RequestBody empty = RequestBody.create("{}", SupabaseClient.JSON);
            SupabaseClient.getInstance().getHttpClient()
                    .newCall(SupabaseClient.getInstance()
                            .authRequest("/logout")
                            .header("Authorization", "Bearer " + token)
                            .post(empty).build())
                    .enqueue(new Callback() {
                        @Override public void onFailure(Call call, IOException e) { Log.w(TAG, "logout network", e); }
                        @Override public void onResponse(Call call, Response response) { Log.d(TAG, "logout " + response.code()); }
                    });
        }
        SupabaseClient.getInstance().clearSession();
    }


    public void reauthenticate(String email, String password, AuthCallback callback) {
        signIn(email, password, callback);
    }

    // Удаление аккаунта
    // Supabase удаление auth-пользователя требует service_role key (не для клиента).
    // Поэтому удаляем все данные из таблицы и выходим.
    // Полное удаление аккаунта из auth делается через Supabase Edge Function.
    public void deleteAccount(AuthCallback callback) {
        if (appContext != null)
            WorkManager.getInstance(appContext).cancelAllWorkByTag("weekly_reminder");

        // Уведомляем об успехе (данные уже удалены вызывающей стороной через SupabaseRepository)
        SupabaseClient.getInstance().clearSession();
        new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> callback.onSuccess(null));
    }


    private void handleAuthResponse(Response response, AuthCallback callback) throws IOException {
        String rb = response.body().string();
        if (!response.isSuccessful()) {
            String msg = extractError(rb);
            postError(callback, new Exception(msg));
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(rb).getAsJsonObject();

            // Supabase /signup возвращает user без access_token если email-подтверждение включено
            String accessToken  = json.has("access_token")  ? json.get("access_token").getAsString()  : null;
            String refreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;

            JsonObject userObj  = json.has("user") ? json.getAsJsonObject("user") : null;
            String uid   = userObj != null && userObj.has("id")    ? userObj.get("id").getAsString()    : null;
            String email = userObj != null && userObj.has("email") ? userObj.get("email").getAsString() : null;

            if (accessToken == null || uid == null) {
                // email-подтверждение включено — пользователь создан, нужно подтвердить email
                postError(callback, new Exception("email_confirmation_required"));
                return;
            }

            SupabaseClient.getInstance().setSession(accessToken, refreshToken, uid);
            SupabaseUser user = new SupabaseUser(uid, email != null ? email : "");
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onSuccess(user));

        } catch (Exception e) {
            postError(callback, e);
        }
    }

    private String extractError(String json) {
        try {
            JsonObject j = JsonParser.parseString(json).getAsJsonObject();
            if (j.has("error_description")) return j.get("error_description").getAsString();
            if (j.has("msg"))               return j.get("msg").getAsString();
            if (j.has("message"))           return j.get("message").getAsString();
        } catch (Exception ignored) {}
        return "Auth error";
    }

    private void postError(AuthCallback callback, Exception e) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> callback.onError(e));
    }


    public interface AuthCallback {
        void onSuccess(SupabaseUser user);
        void onError(Exception e);
    }
}