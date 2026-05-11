package com.example.myapplication.data.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseRepository {

    private static final String TAG   = "SupabaseRepository";
    private static final String TABLE = "places";

    private static volatile SupabaseRepository instance;
    private final SupabaseClient client;
    private final Handler mainHandler;

    private SupabaseRepository() {
        client      = SupabaseClient.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static SupabaseRepository getInstance() {
        if (instance == null) {
            synchronized (SupabaseRepository.class) {
                if (instance == null) instance = new SupabaseRepository();
            }
        }
        return instance;
    }

    // Получить все места текущего пользователя. Фильтрация по user_id (UUID из Supabase Auth).
    public void getAll(PlaceRepository.DataCallback<List<Place>> callback) {
        String uid = client.getUserId();
        String query = (uid != null && !uid.isEmpty())
                ? "select=*&user_id=eq." + uid + "&order=created_at.desc"
                : "select=*&order=created_at.desc";

        client.getHttpClient()
                .newCall(client.dbRequest(TABLE, query).get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(
                                    new Exception("getAll " + response.code() + ": " + body)));
                            return;
                        }
                        mainHandler.post(() -> callback.onSuccess(parsePlaceList(body)));
                    }
                });
    }

    // Вставить новое место. Возвращает Supabase UUID вставленной записи.
    public void insert(Place place, PlaceRepository.DataCallback<String> callback) {
        JsonObject json = placeToJson(place);

        String uid = client.getUserId();
        if (uid != null && !uid.isEmpty()) {
            json.addProperty("user_id", uid);
        }

        RequestBody body = RequestBody.create(json.toString(), SupabaseClient.JSON);

        client.getHttpClient()
                .newCall(client.dbRequest(TABLE, null)
                        .addHeader("Prefer", "return=representation") // ← ОБЯЗАТЕЛЬНО
                        .post(body)
                        .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String rb = response.body().string();
                        Log.d(TAG, "INSERT response " + response.code() + ": " + rb);

                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(
                                    new Exception("insert " + response.code() + ": " + rb)));
                            return;
                        }
                        try {
                            JsonArray arr = JsonParser.parseString(rb).getAsJsonArray();
                            String id = arr.get(0).getAsJsonObject()
                                    .get("id").getAsString();
                            mainHandler.post(() -> callback.onSuccess(id));
                        } catch (Exception e) {
                            Log.e(TAG, "parse insert response error", e);
                            mainHandler.post(() -> callback.onError(e));
                        }
                    }
                });
    }

    // Обновить место. ID берётся из firestoreId (хранит Supabase UUID).
    public void update(Place place, PlaceRepository.DataCallback<Void> callback) {
        String id = place.getFirestoreId();
        if (id == null || id.isEmpty()) {
            mainHandler.post(() -> callback.onError(new Exception("firestoreId is null")));
            return;
        }

        JsonObject json = placeToJson(place);
        json.remove("created_at");

        RequestBody body = RequestBody.create(json.toString(), SupabaseClient.JSON);

        client.getHttpClient()
                .newCall(client.dbRequest(TABLE, "id=eq." + id).patch(body).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            String rb = response.body().string();
                            mainHandler.post(() -> callback.onError(
                                    new Exception("update " + response.code() + ": " + rb)));
                            return;
                        }
                        mainHandler.post(() -> callback.onSuccess(null));
                    }
                });
    }

    // Удалить место по Supabase UUID.
    public void delete(String id, PlaceRepository.DataCallback<Void> callback) {
        client.getHttpClient()
                .newCall(client.dbRequest(TABLE, "id=eq." + id).delete().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            String rb = response.body().string();
                            mainHandler.post(() -> callback.onError(
                                    new Exception("delete " + response.code() + ": " + rb)));
                            return;
                        }
                        mainHandler.post(() -> callback.onSuccess(null));
                    }
                });
    }

    // Удалить все места текущего пользователя.
    public void deleteAll(PlaceRepository.DataCallback<Void> callback) {
        String uid = client.getUserId();
        if (uid == null || uid.isEmpty()) {
            mainHandler.post(() -> callback.onSuccess(null));
            return;
        }
        client.getHttpClient()
                .newCall(client.dbRequest(TABLE, "user_id=eq." + uid).delete().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        mainHandler.post(() -> callback.onSuccess(null));
                    }
                });
    }



     //  таблица places
    private JsonObject placeToJson(Place place) {
        JsonObject j = new JsonObject();

        j.addProperty("name",      safe(place.getName()));
        j.addProperty("latitude",  place.getLatitude());
        j.addProperty("longitude", place.getLongitude());

        j.addProperty("address",       safe(place.getAddress()));
        j.addProperty("phone",         safe(place.getPhone()));
        j.addProperty("working_hours", safe(place.getWorkingHours()));
        j.addProperty("description",   safe(place.getDescription()));

        j.addProperty("website",    safe(place.getWebsite()));
        j.addProperty("rating",     (double) place.getRating());
        j.addProperty("visit_date", place.getVisitDate());
        j.addProperty("is_visited", place.isVisited());
        j.addProperty("source",     safe(place.getSource()));
        j.addProperty("yandex_id",  safe(place.getYandexId()));

        JsonArray imgs = new JsonArray();
        if (place.getImageIds() != null) {
            for (String s : place.getImageIds()) {
                if (s != null) imgs.add(s);
            }
        }
        j.add("image_ids", imgs);

        return j;
    }


    private Place jsonToPlace(JsonObject j) {
        Place p = new Place();

        String rawId = str(j, "id");
        p.setFirestoreId(rawId);

        try {
            if (rawId != null && !rawId.isEmpty()) {
                p.setId(Long.parseLong(rawId));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Cannot parse place id: " + rawId);
        }

        p.setName(str(j, "name"));
        p.setAddress(str(j, "address"));
        p.setPhone(str(j, "phone"));
        p.setWorkingHours(str(j, "working_hours"));
        p.setLatitude(dbl(j, "latitude"));
        p.setLongitude(j.has("longitude") && !j.get("longitude").isJsonNull()
                ? j.get("longitude").getAsDouble() : 0.0);
        p.setDescription(str(j, "description"));
        p.setWebsite(str(j, "website"));
        p.setRating((float) dbl(j, "rating"));
        p.setVisitDate(lng(j, "visit_date"));
        p.setVisited(bool(j, "is_visited"));
        p.setSource(str(j, "source"));
        p.setYandexId(str(j, "yandex_id"));

        p.setCreatedAt(parseIso8601(str(j, "created_at")));

        ArrayList<String> imgs = new ArrayList<>();
        if (j.has("image_ids") && !j.get("image_ids").isJsonNull()) {
            try {
                for (JsonElement el : j.getAsJsonArray("image_ids")) {
                    imgs.add(el.getAsString());
                }
            } catch (Exception e) {
                Log.w(TAG, "image_ids parse error", e);
            }
        }
        p.setImageIds(imgs);

        return p;
    }

    private List<Place> parsePlaceList(String json) {
        List<Place> list = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                list.add(jsonToPlace(el.getAsJsonObject()));
            }
        } catch (Exception e) {
            Log.e(TAG, "parsePlaceList error", e);
        }
        return list;
    }

    private String toIso8601(long millis) {
        if (millis <= 0) millis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }

    private long parseIso8601(String iso) {
        if (iso == null || iso.isEmpty()) return System.currentTimeMillis();
        try {
            String normalized = iso;
            normalized = normalized.replace("+00:00", "Z");
            normalized = normalized.replaceAll("(\\.\\d{3})\\d+Z", "$1Z");
            if (!normalized.endsWith("Z")) normalized += "Z";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = sdf.parse(normalized);
            return parsed != null ? parsed.getTime() : System.currentTimeMillis();

        } catch (Exception e) {
            Log.w(TAG, "parseIso8601 failed for: " + iso);
            return System.currentTimeMillis();
        }
    }

    private String  safe(String s) { return s != null ? s : ""; }
    private String  str(JsonObject j, String k)  { return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : ""; }
    private double  dbl(JsonObject j, String k)  { return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsDouble() : 0.0; }
    private long    lng(JsonObject j, String k)  { return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsLong()   : 0L;  }
    private boolean bool(JsonObject j, String k) { return j.has(k) && !j.get(k).isJsonNull() && j.get(k).getAsBoolean(); }
}