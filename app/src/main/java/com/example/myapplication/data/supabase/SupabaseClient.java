package com.example.myapplication.data.supabase;


import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class SupabaseClient {

    public static final String SUPABASE_URL = "https://cvheyhcknzfpgjpbhvnz.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImN2aGV5aGNrbnpmcGdqcGJodm56Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgyMDY1ODQsImV4cCI6MjA5Mzc4MjU4NH0.rcE7ndulcSPVaZndCb9CPyUK2QYVhTmM45-Y4Un3kqw";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static volatile SupabaseClient instance;
    private final OkHttpClient httpClient;

    private String accessToken;
    private String refreshToken;
    private String userId;

    private SupabaseClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static SupabaseClient getInstance() {
        if (instance == null) {
            synchronized (SupabaseClient.class) {
                if (instance == null) instance = new SupabaseClient();
            }
        }
        return instance;
    }

    public void setSession(String accessToken, String refreshToken, String userId) {
        this.accessToken  = accessToken;
        this.refreshToken = refreshToken;
        this.userId       = userId;
    }

    public void clearSession() {
        accessToken  = null;
        refreshToken = null;
        userId       = null;
    }

    public String getAccessToken()  { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getUserId()       { return userId; }
    public boolean isLoggedIn()     { return accessToken != null && userId != null; }
    public OkHttpClient getHttpClient() { return httpClient; }

    public Request.Builder authRequest(String path) {
        return new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1" + path)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json");
    }

    public Request.Builder dbRequest(String table, String query) {
        String url = SUPABASE_URL + "/rest/v1/" + table;
        if (query != null && !query.isEmpty()) url += "?" + query;

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation");

        if (accessToken != null)
            builder.header("Authorization", "Bearer " + accessToken);

        return builder;
    }
}