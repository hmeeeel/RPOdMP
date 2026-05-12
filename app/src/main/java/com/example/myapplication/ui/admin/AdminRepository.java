package com.example.myapplication.ui.admin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.supabase.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminRepository {

    private static final String TAG = "AdminRepository";
    private static volatile AdminRepository instance;

    private final SupabaseClient client;
    private final Handler        mainHandler;

    interface MapCallback<K, V> {
        void onResult(Map<K, V> result);
        void onError(Exception e);
    }

    private AdminRepository() {
        client      = SupabaseClient.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static AdminRepository getInstance() {
        if (instance == null) {
            synchronized (AdminRepository.class) {
                if (instance == null) instance = new AdminRepository();
            }
        }
        return instance;
    }

    //  MODERATION
    public void getPendingRoutes(PlaceRepository.DataCallback<List<ModerationRoute>> callback) {
        client.getHttpClient()
                .newCall(client.dbRequest("route_statuses", "select=id&code=eq.pending").get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        int pid = 0;
                        try {
                            JsonArray a = JsonParser.parseString(r.body().string()).getAsJsonArray();
                            if (a.size() > 0) pid = a.get(0).getAsJsonObject().get("id").getAsInt();
                        } catch (Exception ignored) {}
                        if (pid == 0) { mainHandler.post(() -> callback.onSuccess(new ArrayList<>())); return; }
                        fetchPending(pid, callback);
                    }
                });
    }

    private void fetchPending(int statusId, PlaceRepository.DataCallback<List<ModerationRoute>> cb) {
        String q = "select=id,title,description,admin_note,created_at,author_id"
                + "&status_id=eq." + statusId + "&order=created_at.asc";
        client.getHttpClient()
                .newCall(client.dbRequest("routes", q).get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> cb.onError(e));
                    }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        if (!r.isSuccessful()) {
                            mainHandler.post(() -> cb.onError(new Exception("routes " + r.code())));
                            return;
                        }
                        List<ModerationRoute> list = parseModerationRoutes(body);
                        if (list.isEmpty()) { mainHandler.post(() -> cb.onSuccess(list)); return; }
                        enrichRoutes(list, cb);
                    }
                });
    }

    private List<ModerationRoute> parseModerationRoutes(String json) {
        List<ModerationRoute> list = new ArrayList<>();
        try {
            for (JsonElement el : JsonParser.parseString(json).getAsJsonArray()) {
                JsonObject j = el.getAsJsonObject();
                ModerationRoute mr = new ModerationRoute();
                mr.setRouteId(str(j, "id"));
                mr.setTitle(str(j, "title"));
                mr.setDescription(str(j, "description"));
                mr.setPreviousAdminNote(str(j, "admin_note"));
                mr.setCreatedAt(str(j, "created_at"));
                mr.setAuthorId(str(j, "author_id"));
                mr.setStatusCode("pending");
                list.add(mr);
            }
        } catch (Exception e) { Log.e(TAG, "parseModerationRoutes", e); }
        return list;
    }

    private void enrichRoutes(List<ModerationRoute> list, PlaceRepository.DataCallback<List<ModerationRoute>> cb) {
        AtomicInteger cnt = new AtomicInteger(3);
        Runnable done = () -> { if (cnt.decrementAndGet() == 0) mainHandler.post(() -> cb.onSuccess(list)); };

        List<String> routeIds = new ArrayList<>();
        List<String> authorIds = new ArrayList<>();
        for (ModerationRoute mr : list) {
            routeIds.add(mr.getRouteId());
            if (!authorIds.contains(mr.getAuthorId())) authorIds.add(mr.getAuthorId());
        }

        fetchNicknames(authorIds, new MapCallback<String, String>() {
            @Override public void onResult(Map<String, String> m) {
                for (ModerationRoute mr : list) {
                    String n = m.get(mr.getAuthorId());
                    mr.setAuthorNickname(n != null ? n : "Путешественник");
                }
                done.run();
            }
            @Override public void onError(Exception e) { done.run(); }
        });

        fetchPointCounts(routeIds, new MapCallback<String, Integer>() {
            @Override public void onResult(Map<String, Integer> m) {
                for (ModerationRoute mr : list) {
                    Integer c = m.get(mr.getRouteId());
                    mr.setPointsCount(c != null ? c : 0);
                }
                done.run();
            }
            @Override public void onError(Exception e) { done.run(); }
        });

        fetchPreviewNames(routeIds, new MapCallback<String, String>() {
            @Override public void onResult(Map<String, String> m) {
                for (ModerationRoute mr : list) {
                    String p = m.get(mr.getRouteId());
                    mr.setPointsPreview(p != null ? p : "");
                }
                done.run();
            }
            @Override public void onError(Exception e) { done.run(); }
        });
    }

    private void fetchNicknames(List<String> ids, MapCallback<String, String> cb) {
        if (ids.isEmpty()) { cb.onResult(new HashMap<>()); return; }
        client.getHttpClient()
                .newCall(client.dbRequest("profiles",
                        "select=user_id,nickname&user_id=in.(" + csv(ids) + ")").get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { cb.onError(e); }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        Map<String, String> map = new HashMap<>();
                        try {
                            for (JsonElement el : JsonParser.parseString(r.body().string()).getAsJsonArray()) {
                                JsonObject j = el.getAsJsonObject();
                                map.put(str(j, "user_id"), str(j, "nickname"));
                            }
                        } catch (Exception ignored) {}
                        cb.onResult(map);
                    }
                });
    }

    private void fetchPointCounts(List<String> routeIds, MapCallback<String, Integer> cb) {
        if (routeIds.isEmpty()) { cb.onResult(new HashMap<>()); return; }
        client.getHttpClient()
                .newCall(client.dbRequest("route_points",
                        "select=route_id&route_id=in.(" + csv(routeIds) + ")").get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { cb.onError(e); }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        Map<String, Integer> map = new HashMap<>();
                        try {
                            for (JsonElement el : JsonParser.parseString(r.body().string()).getAsJsonArray()) {
                                String rid = str(el.getAsJsonObject(), "route_id");
                                map.put(rid, map.getOrDefault(rid, 0) + 1);
                            }
                        } catch (Exception ignored) {}
                        cb.onResult(map);
                    }
                });
    }

    private void fetchPreviewNames(List<String> routeIds, MapCallback<String, String> cb) {
        if (routeIds.isEmpty()) { cb.onResult(new HashMap<>()); return; }
        String q = "select=route_id,places!place_id(name)&route_id=in.(" + csv(routeIds) + ")&order=point_order.asc";
        client.getHttpClient()
                .newCall(client.dbRequest("route_points", q).get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { cb.onError(e); }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        Map<String, List<String>> namesMap = new HashMap<>();
                        try {
                            for (JsonElement el : JsonParser.parseString(r.body().string()).getAsJsonArray()) {
                                JsonObject j   = el.getAsJsonObject();
                                String     rid = str(j, "route_id");
                                String     nm  = (j.has("places") && !j.get("places").isJsonNull())
                                        ? str(j.getAsJsonObject("places"), "name") : "";
                                namesMap.computeIfAbsent(rid, k -> new ArrayList<>()).add(nm);
                            }
                        } catch (Exception ignored) {}
                        Map<String, String> preview = new HashMap<>();
                        for (Map.Entry<String, List<String>> e : namesMap.entrySet()) {
                            List<String> names = e.getValue();
                            StringBuilder sb   = new StringBuilder();
                            for (int i = 0; i < Math.min(3, names.size()); i++) {
                                if (i > 0) sb.append(", ");
                                sb.append(names.get(i));
                            }
                            preview.put(e.getKey(), sb.toString());
                        }
                        cb.onResult(preview);
                    }
                });
    }

    // APPROVE / REJECT
    public void approveRoute(String routeId, String adminNote, PlaceRepository.DataCallback<Void> cb) {
        doModerate(routeId, "published", "approve", adminNote, cb);
    }

    public void rejectRoute(String routeId, String adminNote, PlaceRepository.DataCallback<Void> cb) {
        doModerate(routeId, "draft", "reject", adminNote, cb);
    }

    private void doModerate(String routeId, String code, String action, String note,
                            PlaceRepository.DataCallback<Void> cb) {
        client.getHttpClient()
                .newCall(client.dbRequest("route_statuses", "select=id&code=eq." + code).get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> cb.onError(e));
                    }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        int sid = 0;
                        try {
                            JsonArray a = JsonParser.parseString(r.body().string()).getAsJsonArray();
                            if (a.size() > 0) sid = a.get(0).getAsJsonObject().get("id").getAsInt();
                        } catch (Exception ignored) {}
                        if (sid == 0) { mainHandler.post(() -> cb.onError(new Exception("status not found"))); return; }
                        patchRoute(routeId, sid, action, note, cb);
                    }
                });
    }

    private void patchRoute(String routeId, int statusId, String action, String note,
                            PlaceRepository.DataCallback<Void> cb) {
        JsonObject body = new JsonObject();
        body.addProperty("status_id", statusId);
        if (note != null && !note.trim().isEmpty()) body.addProperty("admin_note", note.trim());
        RequestBody rb = RequestBody.create(body.toString(), SupabaseClient.JSON);
        client.getHttpClient()
                .newCall(client.dbRequest("routes", "id=eq." + routeId)
                        .addHeader("Prefer", "return=minimal").patch(rb).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> cb.onError(e));
                    }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        if (!r.isSuccessful()) {
                            String b = r.body().string();
                            mainHandler.post(() -> cb.onError(new Exception("patch " + r.code() + ": " + b)));
                            return;
                        }
                        writeLog(routeId, action, statusId);
                        mainHandler.post(() -> cb.onSuccess(null));
                    }
                });
    }

    private void writeLog(String routeId, String action, int newStatusId) {
        Long adminId = SupabaseClient.getInstance().getNumericUserId();
        if (adminId == null) return;
        JsonObject body = new JsonObject();
        body.addProperty("admin_id",      adminId);
        body.addProperty("route_id",      Long.parseLong(routeId));
        body.addProperty("action_type",   action);
        body.addProperty("new_status_id", newStatusId);
        RequestBody rb = RequestBody.create(body.toString(), SupabaseClient.JSON);
        client.getHttpClient()
                .newCall(client.dbRequest("admin_moderation_log", null)
                        .addHeader("Prefer", "return=minimal").post(rb).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { Log.w(TAG, "log fail", e); }
                    @Override public void onResponse(Call call, Response r) throws IOException { Log.d(TAG, "log " + r.code()); }
                });
    }

    // USER STATS
    public void getUserStats(PlaceRepository.DataCallback<List<UserStats>> cb) {
        client.getHttpClient()
                .newCall(client.dbRequest("profiles",
                        "select=user_id,nickname,avatar_url&order=user_id.asc").get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> cb.onError(e));
                    }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        List<UserStats> users = new ArrayList<>();
                        List<String>    ids   = new ArrayList<>();
                        try {
                            for (JsonElement el : JsonParser.parseString(r.body().string()).getAsJsonArray()) {
                                JsonObject j = el.getAsJsonObject();
                                UserStats  u = new UserStats();
                                u.setUserId(str(j, "user_id"));
                                u.setNickname(str(j, "nickname"));
                                u.setAvatarUrl(str(j, "avatar_url"));
                                users.add(u); ids.add(u.getUserId());
                            }
                        } catch (Exception e) { Log.e(TAG, "parse profiles", e); }
                        enrichUserStats(users, ids, cb);
                    }
                });
    }

    private void enrichUserStats(List<UserStats> users, List<String> ids,
                                 PlaceRepository.DataCallback<List<UserStats>> cb) {
        if (ids.isEmpty()) { mainHandler.post(() -> cb.onSuccess(users)); return; }
        String q = "select=user_id,routes_created_count,routes_saved_count&user_id=in.(" + csv(ids) + ")";
        client.getHttpClient()
                .newCall(client.dbRequest("user_statistic", q).get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> cb.onSuccess(users));
                    }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        Map<String, int[]> statMap = new HashMap<>();
                        try {
                            for (JsonElement el : JsonParser.parseString(r.body().string()).getAsJsonArray()) {
                                JsonObject j = el.getAsJsonObject();
                                statMap.put(str(j, "user_id"), new int[]{
                                        (int) lng(j, "routes_created_count"),
                                        (int) lng(j, "routes_saved_count")
                                });
                            }
                        } catch (Exception ignored) {}
                        for (UserStats u : users) {
                            int[] s = statMap.get(u.getUserId());
                            if (s != null) { u.setRoutesCreatedCount(s[0]); u.setRoutesSavedCount(s[1]); }
                        }
                        users.sort((a, b) -> Integer.compare(b.getRoutesCreatedCount(), a.getRoutesCreatedCount()));
                        mainHandler.post(() -> cb.onSuccess(users));
                    }
                });
    }

    // TOP PLACES
    public void getTopPlaces(PlaceRepository.DataCallback<List<TopPlace>> cb) {
        client.getHttpClient()
                .newCall(client.dbRequest("place_statistic",
                                "select=place_id,total_in_routes_count&order=total_in_routes_count.desc&limit=10")
                        .get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> cb.onError(e));
                    }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        Map<String, Integer> totalMap = new HashMap<>();
                        List<String> pids = new ArrayList<>();
                        try {
                            for (JsonElement el : JsonParser.parseString(r.body().string()).getAsJsonArray()) {
                                JsonObject j = el.getAsJsonObject();
                                String pid   = str(j, "place_id");
                                pids.add(pid);
                                totalMap.put(pid, (int) lng(j, "total_in_routes_count"));
                            }
                        } catch (Exception e) { Log.e(TAG, "parse statistic", e); }
                        if (pids.isEmpty()) { mainHandler.post(() -> cb.onSuccess(new ArrayList<>())); return; }
                        fetchPlaceDetails(pids, totalMap, cb);
                    }
                });
    }

    private void fetchPlaceDetails(List<String> pids, Map<String, Integer> totalMap,
                                   PlaceRepository.DataCallback<List<TopPlace>> cb) {
        String q = "select=id,name,address,place_categories!category_id(name)&id=in.(" + csv(pids) + ")";
        client.getHttpClient()
                .newCall(client.dbRequest("places", q).get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> cb.onError(e));
                    }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        List<TopPlace> places = new ArrayList<>();
                        try {
                            for (JsonElement el : JsonParser.parseString(r.body().string()).getAsJsonArray()) {
                                JsonObject j = el.getAsJsonObject();
                                String pid   = str(j, "id");
                                TopPlace tp  = new TopPlace();
                                tp.setPlaceId(pid);
                                tp.setPlaceName(str(j, "name"));
                                tp.setPlaceAddress(str(j, "address"));
                                if (j.has("place_categories") && !j.get("place_categories").isJsonNull())
                                    tp.setCategoryName(str(j.getAsJsonObject("place_categories"), "name"));
                                int total = totalMap.getOrDefault(pid, 0);
                                tp.setTotalTimesInRoutes(total);
                                tp.setTimesInRoutes30Days(total);
                                places.add(tp);
                            }
                        } catch (Exception e) { Log.e(TAG, "parse places", e); }
                        places.sort((a, b) -> Integer.compare(b.getTotalTimesInRoutes(), a.getTotalTimesInRoutes()));
                        mainHandler.post(() -> cb.onSuccess(places));
                    }
                });
    }


    private String csv(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) { if (i > 0) sb.append(','); sb.append(ids.get(i)); }
        return sb.toString();
    }
    private String str(JsonObject j, String k) {
        return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : "";
    }
    private long lng(JsonObject j, String k) {
        return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsLong() : 0L;
    }
}