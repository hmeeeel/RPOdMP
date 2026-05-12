package com.example.myapplication.data.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.ui.routes.RouteCard;
import com.example.myapplication.ui.routes.RoutePoint;
import com.example.myapplication.ui.routes.RouteReview;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RouteRepository {

    private static final String TAG = "RouteRepository";

    private static volatile RouteRepository instance;
    private final SupabaseClient client;
    private final Handler        mainHandler;

    interface MapCallback<K, V> {
        void onResult(Map<K, V> map);
        void onError(Exception e);
    }

    interface SetCallback {
        void onResult(Set<String> set);
        void onError(Exception e);
    }

    private RouteRepository() {
        client      = SupabaseClient.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static RouteRepository getInstance() {
        if (instance == null) {
            synchronized (RouteRepository.class) {
                if (instance == null) instance = new RouteRepository();
            }
        }
        return instance;
    }

    //  СПИСОК КАРТОЧЕК МАРШРУТОВ
    public void getAllRouteCards(String userId,
                                 PlaceRepository.DataCallback<List<RouteCard>> callback) {
        String query = "select=id,title,description,author_id,is_author_completed,"
                + "admin_note,created_at,"
                + "route_statuses!status_id(code),"
                + "route_statistic(average_rating,likes_count,reviews_count)"
                + "&order=created_at.desc";

        client.getHttpClient()
                .newCall(client.dbRequest("routes", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(
                                    new Exception("routes " + response.code() + ": " + body)));
                            return;
                        }
                        List<RouteCard> cards = parseRouteCards(body, userId);
                        if (cards.isEmpty()) {
                            mainHandler.post(() -> callback.onSuccess(cards));
                            return;
                        }
                        enrichCards(cards, userId, callback);
                    }
                });
    }

    private List<RouteCard> parseRouteCards(String json, String userId) {
        List<RouteCard> list = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject j    = el.getAsJsonObject();
                RouteCard  card = new RouteCard();
                card.setId(str(j, "id"));
                card.setTitle(str(j, "title"));
                card.setDescription(str(j, "description"));
                card.setAuthorId(str(j, "author_id"));
                card.setAdminNote(str(j, "admin_note"));
                card.setAuthorCompleted(bool(j, "is_author_completed"));
                card.setCreatedAt(str(j, "created_at"));

                if (j.has("route_statuses") && !j.get("route_statuses").isJsonNull()) {
                    card.setStatusCode(str(j.getAsJsonObject("route_statuses"), "code"));
                }

                if (j.has("route_statistic") && !j.get("route_statistic").isJsonNull()) {
                    JsonObject st = j.getAsJsonObject("route_statistic");
                    card.setAverageRating(dbl(st, "average_rating"));
                    card.setLikesCount((int) lng(st, "likes_count"));
                    card.setReviewsCount((int) lng(st, "reviews_count"));
                }

                String  status  = card.getStatusCode();
                boolean visible = "published".equals(status)
                        || card.getAuthorId().equals(userId);
                if (visible) list.add(card);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseRouteCards error", e);
        }
        return list;
    }


    private void enrichCards(List<RouteCard> cards,
                             String userId,
                             PlaceRepository.DataCallback<List<RouteCard>> callback) {

        AtomicInteger pending = new AtomicInteger(3);

        Runnable checkDone = () -> {
            if (pending.decrementAndGet() == 0) {
                mainHandler.post(() -> callback.onSuccess(cards));
            }
        };

        List<String> routeIds  = new ArrayList<>();
        List<String> authorIds = new ArrayList<>();
        for (RouteCard c : cards) {
            routeIds.add(c.getId());
            if (!authorIds.contains(c.getAuthorId())) authorIds.add(c.getAuthorId());
        }

        // 2a. Количество точек
        fetchPointCounts(routeIds, new MapCallback<String, Integer>() {
            @Override
            public void onResult(Map<String, Integer> countMap) {
                for (RouteCard c : cards) {
                    Integer cnt = countMap.get(c.getId());
                    if (cnt != null) c.setPointsCount(cnt);
                }
                checkDone.run();
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "pointCounts error", e);
                checkDone.run();  // не блокируем — продолжаем без счётчика
            }
        });

        // 2b. Никнеймы авторов
        fetchProfiles(authorIds, new MapCallback<String, String>() {
            @Override
            public void onResult(Map<String, String> profileMap) {
                for (RouteCard c : cards) {
                    String nick = profileMap.get(c.getAuthorId());
                    c.setAuthorNickname(nick != null ? nick : "Путешественник");
                }
                checkDone.run();
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "profiles error", e);
                checkDone.run();
            }
        });

        // 2c. Флаги сохранения
        fetchSavedRouteIds(userId, routeIds, new SetCallback() {
            @Override
            public void onResult(Set<String> savedIds) {
                for (RouteCard c : cards) {
                    c.setSaved(savedIds.contains(c.getId()));
                }
                checkDone.run();
            }
            @Override
            public void onError(Exception e) {
                Log.w(TAG, "savedRoutes error", e);
                checkDone.run();
            }
        });
    }


    private void fetchPointCounts(List<String> routeIds,
                                  MapCallback<String, Integer> callback) {
        if (routeIds.isEmpty()) {
            callback.onResult(new HashMap<>());
            return;
        }
        String query = "select=route_id&route_id=in.(" + buildInClause(routeIds) + ")";

        client.getHttpClient()
                .newCall(client.dbRequest("route_points", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        Map<String, Integer> map = new HashMap<>();
                        try {
                            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                            for (JsonElement el : arr) {
                                String rid = str(el.getAsJsonObject(), "route_id");
                                map.put(rid, map.getOrDefault(rid, 0) + 1);
                            }
                        } catch (Exception ignored) {}
                        callback.onResult(map);
                    }
                });
    }


    void fetchProfiles(List<String> userIds, MapCallback<String, String> callback) {
        if (userIds.isEmpty()) {
            callback.onResult(new HashMap<>());
            return;
        }
        String query = "select=user_id,nickname&user_id=in.(" + buildInClause(userIds) + ")";

        client.getHttpClient()
                .newCall(client.dbRequest("profiles", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        Map<String, String> map = new HashMap<>();
                        try {
                            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                            for (JsonElement el : arr) {
                                JsonObject j = el.getAsJsonObject();
                                map.put(str(j, "user_id"), str(j, "nickname"));
                            }
                        } catch (Exception ignored) {}
                        callback.onResult(map);
                    }
                });
    }

    private void fetchSavedRouteIds(String userId,
                                    List<String> routeIds,
                                    SetCallback callback) {
        if (userId == null || routeIds.isEmpty()) {
            callback.onResult(new HashSet<>());
            return;
        }
        String query = "select=route_id&user_id=eq." + userId
                + "&route_id=in.(" + buildInClause(routeIds) + ")";

        client.getHttpClient()
                .newCall(client.dbRequest("user_saved_routes", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError(e);
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        Set<String> set = new HashSet<>();
                        try {
                            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                            for (JsonElement el : arr)
                                set.add(str(el.getAsJsonObject(), "route_id"));
                        } catch (Exception ignored) {}
                        callback.onResult(set);
                    }
                });
    }

    //  ТОЧКИ МАРШРУТА
   /* public void getRoutePoints(String routeId,
                               PlaceRepository.DataCallback<List<RoutePoint>> callback) {
        String query = "select=id,point_order,"
                + "places!place_id(id,name,address,latitude,longitude,"
                + "image_ids,description,phone,working_hours)"
                + "&route_id=eq." + routeId
                + "&order=point_order.asc";

        client.getHttpClient()
                .newCall(client.dbRequest("route_points", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        if (!r.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(
                                    new Exception("routePoints " + r.code() + ": " + body)));
                            return;
                        }
                        mainHandler.post(() -> callback.onSuccess(parseRoutePoints(body)));
                    }
                });
    }*/
    public void getRoutePoints(String routeId,
                               PlaceRepository.DataCallback<List<RoutePoint>> callback) {
        getRoutePoints(routeId, null, callback);
    }
    public void getRoutePoints(String routeId,
                               Long numericUserId,
                               PlaceRepository.DataCallback<List<RoutePoint>> callback) {

        // Добавляем is_visited прямо в SELECT из places
        String query = "select=id,point_order,"
                + "places!place_id(id,name,address,latitude,longitude,"
                + "image_ids,description,phone,working_hours,is_visited)"
                + "&route_id=eq." + routeId
                + "&order=point_order.asc";

        client.getHttpClient()
                .newCall(client.dbRequest("route_points", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        if (!r.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(
                                    new Exception("routePoints " + r.code() + ": " + body)));
                            return;
                        }

                        // Просто парсим — is_visited  внутри
                        List<RoutePoint> points = parseRoutePoints(body);
                        mainHandler.post(() -> callback.onSuccess(points));

                    }
                });
    }


    /**
     * user_places.user_id = bigint  принимаем Long
     * user_places.place_id = bigint  в запросе числа, не строки
     * places.id = bigint  getFirestoreId() содержит строку числа ("123")
     */
    private void enrichWithVisitedStatus(
            List<RoutePoint> points,
            Long numericUserId,
            PlaceRepository.DataCallback<List<RoutePoint>> callback) {

        // ВРЕМЕННЫЙ ЛОГ
        Log.e("ENRICH_DEBUG", "=== enrichWithVisitedStatus called ===");
        Log.e("ENRICH_DEBUG", "numericUserId: " + numericUserId);
        Log.e("ENRICH_DEBUG", "points count: " + points.size());

        for (RoutePoint rp : points) {
            Place p = rp.getPlace();
            Log.e("ENRICH_DEBUG", "  point: place="
                    + (p != null ? p.getName() : "NULL")
                    + " firestoreId=" + (p != null ? p.getFirestoreId() : "NULL"));
        }
        // Собираем bigint place_id как числа
        // places.id = bigint, firestoreId хранит его как строку числа
        List<Long> placeIdLongs = new ArrayList<>();
        Map<Long, RoutePoint> pointByPlaceId = new HashMap<>();

        for (RoutePoint rp : points) {
            Place p = rp.getPlace();
            if (p == null) continue;

            String fid = p.getFirestoreId(); // "123" — строка bigint
            if (fid == null || fid.isEmpty()) continue;

            try {
                long pid = Long.parseLong(fid);
                placeIdLongs.add(pid);
                pointByPlaceId.put(pid, rp);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Cannot parse place firestoreId as Long: " + fid);
            }
        }

        if (placeIdLongs.isEmpty()) {
            mainHandler.post(() -> callback.onSuccess(points));
            return;
        }

        // Строим IN-клаузу из чисел: (1,2,3) — без кавычек, bigint!
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < placeIdLongs.size(); i++) {
            if (i > 0) inClause.append(',');
            inClause.append(placeIdLongs.get(i)); // числа, не строки
        }

        // user_places.user_id = bigint  numericUserId (Long)
        // user_places.place_id = bigint  числа в IN()
        String query = "select=place_id,is_visited"
                + "&user_id=eq." + numericUserId   // bigint
                + "&place_id=in.(" + inClause + ")"; // bigint IN

        Log.d(TAG, "enrichWithVisitedStatus: userId=" + numericUserId
                + " placeIds=" + inClause);

        client.getHttpClient()
                .newCall(client.dbRequest("user_places", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.w(TAG, "user_places fetch failed", e);
                        mainHandler.post(() -> callback.onSuccess(points));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        Log.e("ENRICH_DEBUG", "user_places response code: " + r.code());
                        Log.e("ENRICH_DEBUG", "user_places response body: " + body);
                        Log.d(TAG, "user_places response " + r.code() + ": " + body);

                        if (!r.isSuccessful()) {
                            mainHandler.post(() -> callback.onSuccess(points));
                            return;
                        }

                        try {
                            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                            for (JsonElement el : arr) {
                                JsonObject obj = el.getAsJsonObject();

                                // place_id в ответе = bigint  читаем как long
                                long    pid     = lng(obj, "place_id");
                                boolean visited = bool(obj, "is_visited");

                                RoutePoint rp = pointByPlaceId.get(pid);
                                if (rp != null && rp.getPlace() != null) {
                                    rp.getPlace().setVisited(visited);
                                    Log.d(TAG, "  place_id=" + pid
                                            + " name=" + rp.getPlace().getName()
                                            + " visited=" + visited);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "parse user_places error", e);
                        }

                        mainHandler.post(() -> callback.onSuccess(points));
                    }
                });
    }
    private List<RoutePoint> parseRoutePoints(String json) {
        List<RoutePoint> list = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject j  = el.getAsJsonObject();
                // Используем UI-модель RoutePoint(placeId, pointOrder)
                long placeId = 0L;
                int  order   = (int) lng(j, "point_order");

                Place place = null;
                if (j.has("places") && !j.get("places").isJsonNull()) {
                    place   = parsePlaceFromJson(j.getAsJsonObject("places"));
                    String pidStr = str(j.getAsJsonObject("places"), "id");
                    if (!pidStr.isEmpty()) {
                        try { placeId = Long.parseLong(pidStr); }
                        catch (NumberFormatException ignored) {}
                    }
                }

                RoutePoint rp = new RoutePoint(placeId, order);
                if (place != null) rp.setPlace(place);

                String idStr = str(j, "id");
                if (!idStr.isEmpty()) {
                    try { rp.setId(Long.parseLong(idStr)); }
                    catch (NumberFormatException ignored) {}
                }

                list.add(rp);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseRoutePoints error", e);
        }
        return list;
    }

    private Place parsePlaceFromJson(JsonObject j) {
        Place p = new Place();
        p.setFirestoreId(str(j, "id"));
        p.setName(str(j, "name"));
        p.setAddress(str(j, "address"));
        p.setLatitude(dbl(j, "latitude"));
        p.setLongitude(dbl(j, "longitude"));
        p.setDescription(str(j, "description"));
        p.setPhone(str(j, "phone"));
        p.setWorkingHours(str(j, "working_hours"));
        p.setVisited(bool(j, "is_visited"));  // ← ДОБАВИТЬ ЭТУ СТРОКУ

        ArrayList<String> imgs = new ArrayList<>();
        if (j.has("image_ids") && !j.get("image_ids").isJsonNull()) {
            try {
                for (JsonElement el : j.getAsJsonArray("image_ids"))
                    imgs.add(el.getAsString());
            } catch (Exception ignored) {}
        }
        p.setImageIds(imgs);
        return p;
    }

    //  ОТЗЫВЫ
    public void getReviews(String routeId,
                           PlaceRepository.DataCallback<List<RouteReview>> callback) {
        String query = "select=*&route_id=eq." + routeId + "&order=created_at.desc";

        client.getHttpClient()
                .newCall(client.dbRequest("route_reviews", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        if (!r.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(
                                    new Exception("reviews " + r.code())));
                            return;
                        }
                        List<RouteReview> reviews = parseReviews(body);

                        List<String> userIds = new ArrayList<>();
                        for (RouteReview rv : reviews)
                            if (!userIds.contains(rv.getUserId())) userIds.add(rv.getUserId());

                        fetchProfiles(userIds, new MapCallback<String, String>() {
                            @Override
                            public void onResult(Map<String, String> profileMap) {
                                for (RouteReview rv : reviews) {
                                    String nick = profileMap.get(rv.getUserId());
                                    rv.setAuthorNickname(nick != null ? nick : "Путешественник");
                                }
                                mainHandler.post(() -> callback.onSuccess(reviews));
                            }
                            @Override
                            public void onError(Exception e) {
                                mainHandler.post(() -> callback.onSuccess(reviews));
                            }
                        });
                    }
                });
    }

    private List<RouteReview> parseReviews(String json) {
        List<RouteReview> list = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject  j  = el.getAsJsonObject();
                RouteReview rv = new RouteReview();
                rv.setId(str(j, "id"));
                rv.setUserId(str(j, "user_id"));
                rv.setRouteId(str(j, "route_id"));
                rv.setRating((int) lng(j, "rating"));
                rv.setComment(str(j, "comment"));
                rv.setCreatedAt(str(j, "created_at"));
                list.add(rv);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseReviews error", e);
        }
        return list;
    }

    //  СОХРАНЕНИЕ / ОТМЕНА
    public void checkSaved(String userId, String routeId,
                           PlaceRepository.DataCallback<Boolean> callback) {
        String query = "select=route_id&user_id=eq." + userId + "&route_id=eq." + routeId;
        client.getHttpClient()
                .newCall(client.dbRequest("user_saved_routes", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        try {
                            boolean saved =
                                    JsonParser.parseString(body).getAsJsonArray().size() > 0;
                            mainHandler.post(() -> callback.onSuccess(saved));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onSuccess(false));
                        }
                    }
                });
    }

    public void saveRoute(String userId, String routeId,
                          PlaceRepository.DataCallback<Void> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id",  userId);
        body.addProperty("route_id", routeId);

        RequestBody rb = RequestBody.create(body.toString(), SupabaseClient.JSON);
        client.getHttpClient()
                .newCall(client.dbRequest("user_saved_routes", null)
                        .addHeader("Prefer", "return=minimal")
                        .post(rb).build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        if (r.isSuccessful()) {
                            addPlacesFromRoute(userId, routeId, callback);
                        } else {
                            String b = r.body().string();
                            mainHandler.post(() -> callback.onError(
                                    new Exception("saveRoute " + r.code() + ": " + b)));
                        }
                    }
                });
    }

    private void addPlacesFromRoute(String userId, String routeId,
                                    PlaceRepository.DataCallback<Void> callback) {
        String query = "select=place_id&route_id=eq." + routeId;
        client.getHttpClient()
                .newCall(client.dbRequest("route_points", query).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onSuccess(null));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        try {
                            JsonArray arr     = JsonParser.parseString(body).getAsJsonArray();
                            JsonArray upserts = new JsonArray();
                            for (JsonElement el : arr) {
                                String     placeId = str(el.getAsJsonObject(), "place_id");
                                JsonObject row     = new JsonObject();
                                row.addProperty("user_id",             userId);
                                row.addProperty("place_id",            placeId);
                                row.addProperty("is_visited",          false);
                                row.addProperty("added_from_route_id", routeId);
                                upserts.add(row);
                            }
                            if (upserts.size() == 0) {
                                mainHandler.post(() -> callback.onSuccess(null));
                                return;
                            }
                            RequestBody rb = RequestBody.create(
                                    upserts.toString(), SupabaseClient.JSON);
                            client.getHttpClient()
                                    .newCall(client.dbRequest("user_places", null)
                                            .addHeader("Prefer",
                                                    "resolution=ignore-duplicates,return=minimal")
                                            .post(rb).build())
                                    .enqueue(new Callback() {
                                        @Override
                                        public void onFailure(Call call, IOException e) {
                                            mainHandler.post(() -> callback.onSuccess(null));
                                        }
                                        @Override
                                        public void onResponse(Call call, Response r2) {
                                            mainHandler.post(() -> callback.onSuccess(null));
                                        }
                                    });
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onSuccess(null));
                        }
                    }
                });
    }

    public void unsaveRoute(String userId, String routeId,
                            PlaceRepository.DataCallback<Void> callback) {
        String query = "user_id=eq." + userId + "&route_id=eq." + routeId;
        client.getHttpClient()
                .newCall(client.dbRequest("user_saved_routes", query).delete().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) {
                        mainHandler.post(() -> callback.onSuccess(null));
                    }
                });
    }

    //  СОЗДАНИЕ МАРШРУТА
    public void createRoute(String userId, String title, String description,
                            boolean isPublic, String category,
                            List<RoutePoint> points,
                            PlaceRepository.DataCallback<String> callback) {

        // 1. Достаем email из токена
        String email = SupabaseClient.getInstance().getUserEmail();
        if (email == null || email.isEmpty()) {
            mainHandler.post(() -> callback.onError(new Exception("Не удалось получить email")));
            return;
        }

        // 2. Ищем числовой ID (bigint) в таблице users по email
        String userQuery = "select=id&email=eq." + email;
        client.getHttpClient()
                .newCall(client.dbRequest("users", userQuery).get().build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) { mainHandler.post(() -> callback.onError(e)); }
                    @Override public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        try {
                            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                            if (arr.size() == 0) {
                                mainHandler.post(() -> callback.onError(new Exception("Пользователь не найден в users")));
                                return;
                            }
                            // Получаем числовой ID
                            long authorId = arr.get(0).getAsJsonObject().get("id").getAsLong();
                            Log.d(TAG, " Найден authorId (bigint): " + authorId);

                            // 3. Передаем числовой ID в сохранение
                            fetchStatusAndInsert(authorId, title, description, isPublic, category, points, callback);
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    }
                });
    }

    private void fetchStatusAndInsert(long authorId, String title, String description,
                                      boolean isPublic, String category,
                                      List<RoutePoint> points,
                                      PlaceRepository.DataCallback<String> callback) {
        String statusCode  = isPublic ? "pending" : "draft";
        String statusQuery = "select=id&code=eq." + statusCode;

        client.getHttpClient()
                .newCall(client.dbRequest("route_statuses", statusQuery).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String body = r.body().string();
                        int statusId = 1; // По умолчанию 1
                        try {
                            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                            if (arr.size() > 0) {
                                statusId = arr.get(0).getAsJsonObject().get("id").getAsInt();
                            }
                        } catch (Exception ignored) {}

                        // Вызываем вставку с правильными числовыми типами
                        insertRoute(authorId, title, description, statusId, points, callback);
                    }
                });
    }
    private void insertRoute(long authorId, String title, String description,
                             int statusId, List<RoutePoint> points,
                             PlaceRepository.DataCallback<String> callback) {

        JsonObject body = new JsonObject();
        body.addProperty("author_id", authorId);
        body.addProperty("status_id", statusId);
        body.addProperty("title", title);
        body.addProperty("description", description);

        RequestBody rb = RequestBody.create(body.toString(), SupabaseClient.JSON);
        client.getHttpClient()
                .newCall(client.dbRequest("routes", null)
                        .addHeader("Prefer", "return=representation")
                        .post(rb).build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        String b = r.body().string();
                        if (!r.isSuccessful()) {
                            mainHandler.post(() -> callback.onError(
                                    new Exception("createRoute " + r.code() + ": " + b)));
                            return;
                        }
                        try {
                            String routeId = JsonParser.parseString(b)
                                    .getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("id").getAsString();
                            insertRoutePoints(routeId, points, callback);
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    }
                });
    }


    private void insertRoutePoints(String routeId,
                                   List<RoutePoint> points,
                                   PlaceRepository.DataCallback<String> callback) {
        JsonArray arr = new JsonArray();
        for (RoutePoint rp : points) {
            JsonObject row = new JsonObject();


            try {
                row.addProperty("route_id", Long.parseLong(routeId));
            } catch (NumberFormatException e) {
                row.addProperty("route_id", routeId); // fallback
            }

            row.addProperty("place_id", rp.getPlaceId());
            row.addProperty("point_order", rp.getPointOrder());

            arr.add(row);
        }

        RequestBody rb = RequestBody.create(arr.toString(), SupabaseClient.JSON);
        client.getHttpClient()
                .newCall(client.dbRequest("route_points", null)
                        .addHeader("Prefer", "return=minimal")
                        .post(rb).build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError(e));
                    }

                    @Override
                    public void onResponse(Call call, Response r) throws IOException {
                        if (!r.isSuccessful()) {
                            String b = r.body().string();
                            mainHandler.post(() -> callback.onError(
                                    new Exception("insertPoints " + r.code() + ": " + b)));
                            return;
                        }
                        mainHandler.post(() -> callback.onSuccess(routeId));
                    }
                });
    }


    private String buildInClause(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    private String  str(JsonObject j, String k) {
        return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsString() : "";
    }
    private double  dbl(JsonObject j, String k) {
        return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsDouble() : 0.0;
    }
    private long    lng(JsonObject j, String k) {
        return j.has(k) && !j.get(k).isJsonNull() ? j.get(k).getAsLong() : 0L;
    }
    private boolean bool(JsonObject j, String k) {
        return j.has(k) && !j.get(k).isJsonNull() && j.get(k).getAsBoolean();
    }

}