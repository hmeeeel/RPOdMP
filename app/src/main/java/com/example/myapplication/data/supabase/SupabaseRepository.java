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
import java.util.ArrayList;
import java.util.List;

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


    public void getAll(PlaceRepository.DataCallback<List<Place>> callback) {
        client.getHttpClient()
                .newCall(client.dbRequest(TABLE, "select=*&order=created_at.desc").get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(new Exception("getAll " + response.code() + ": " + body)));
                            return;
                        }
                        mainHandler.post(() -> callback.onSuccess(parsePlaceList(body)));
                    }
                });
    }


    public void insert(Place place, PlaceRepository.DataCallback<String> callback) {
        JsonObject json = placeToJson(place);
        String uid = client.getUserId();
        if (uid != null) json.addProperty("user_id", uid);

        RequestBody body = RequestBody.create(json.toString(), SupabaseClient.JSON);

        client.getHttpClient()
                .newCall(client.dbRequest(TABLE, null).post(body).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                    @Override public void onResponse(Call call, Response response) throws IOException {
                        String rb = response.body().string();
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(new Exception("insert " + response.code() + ": " + rb)));
                            return;
                        }
                        try {
                            JsonArray arr = JsonParser.parseString(rb).getAsJsonArray();
                            String id = arr.get(0).getAsJsonObject().get("id").getAsString();
                            mainHandler.post(() -> callback.onSuccess(id));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    }
                });
    }


    public void update(Place place, PlaceRepository.DataCallback<Void> callback) {
        String id   = place.getFirestoreId();           // firestoreId хранит Supabase UUID
        JsonObject json = placeToJson(place);
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
                            mainHandler.post(() -> callback.onError(new Exception("update " + response.code() + ": " + rb)));
                            return;
                        }
                        mainHandler.post(() -> callback.onSuccess(null));
                    }
                });
    }


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
                            mainHandler.post(() -> callback.onError(new Exception("delete " + response.code() + ": " + rb)));
                            return;
                        }
                        mainHandler.post(() -> callback.onSuccess(null));
                    }
                });
    }


    public void deleteAll(PlaceRepository.DataCallback<Void> callback) {
        String uid = client.getUserId();
        if (uid == null) { mainHandler.post(() -> callback.onSuccess(null)); return; }
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


    private JsonObject placeToJson(Place place) {
        JsonObject j = new JsonObject();
        j.addProperty("name",          safe(place.getName()));
        j.addProperty("address",       safe(place.getAddress()));
        j.addProperty("phone",         safe(place.getPhone()));
        j.addProperty("working_hours", safe(place.getWorkingHours()));
        j.addProperty("latitude",      place.getLatitude());
        j.addProperty("longitude",     place.getLongitude());
        j.addProperty("description",   safe(place.getDescription()));
        j.addProperty("website",       safe(place.getWebsite()));
        j.addProperty("rating",        (double) place.getRating());
        j.addProperty("visit_date",    place.getVisitDate());
        j.addProperty("is_visited",    place.isVisited());
        j.addProperty("source",        safe(place.getSource()));
        j.addProperty("created_at",    place.getCreatedAt());
        j.addProperty("yandex_id",     safe(place.getYandexId()));
        JsonArray imgs = new JsonArray();
        if (place.getImageIds() != null)
            for (String s : place.getImageIds()) imgs.add(s);
        j.add("image_ids", imgs);
        return j;
    }

    private Place jsonToPlace(JsonObject j) {
        Place p = new Place();
        p.setFirestoreId(str(j, "id"));         // Supabase UUID -> firestoreId
        p.setName(str(j, "name"));
        p.setAddress(str(j, "address"));
        p.setPhone(str(j, "phone"));
        p.setWorkingHours(str(j, "working_hours"));
        p.setLatitude(dbl(j, "latitude"));
        p.setLongitude(dbl(j, "longitude"));
        p.setDescription(str(j, "description"));
        p.setWebsite(str(j, "website"));
        p.setRating((float) dbl(j, "rating"));
        p.setVisitDate(lng(j, "visit_date"));
        p.setVisited(bool(j, "is_visited"));
        p.setSource(str(j, "source"));
        p.setCreatedAt(lng(j, "created_at"));
        p.setYandexId(str(j, "yandex_id"));
        ArrayList<String> imgs = new ArrayList<>();
        if (j.has("image_ids") && !j.get("image_ids").isJsonNull()) {
            for (JsonElement el : j.getAsJsonArray("image_ids"))
                imgs.add(el.getAsString());
        }
        p.setImageIds(imgs);
        return p;
    }

    private List<Place> parsePlaceList(String json) {
        List<Place> list = new ArrayList<>();
        try {
            for (JsonElement el : JsonParser.parseString(json).getAsJsonArray())
                list.add(jsonToPlace(el.getAsJsonObject()));
        } catch (Exception e) { Log.e(TAG, "parsePlaceList", e); }
        return list;
    }

    private String  safe(String s) { return s != null ? s : ""; }
    private String  str(JsonObject j, String k) { return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : ""; }
    private double  dbl(JsonObject j, String k) { return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsDouble() : 0; }
    private long    lng(JsonObject j, String k) { return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsLong()   : 0; }
    private boolean bool(JsonObject j, String k){ return j.has(k) && !j.get(k).isJsonNull() && j.get(k).getAsBoolean(); }
}