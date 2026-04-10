package com.example.myapplication.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.sqlite.db.SimpleSQLiteQuery;

import com.example.myapplication.data.db.MuseumDB;
import com.example.myapplication.data.db.PlaceDAO;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.serviceImage.ImageStorageService;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaceRepository {

    private final PlaceDAO placeDAO;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Context context;

    private static volatile PlaceRepository instance;

    private PlaceRepository(Context context) {
        this.context = context.getApplicationContext();
        MuseumDB db = MuseumDB.getInstance(this.context);
        placeDAO = db.placeDAO();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static PlaceRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (PlaceRepository.class) {
                if (instance == null) {
                    instance = new PlaceRepository(context);
                }
            }
        }
        return instance;
    }

    public void shutdown() {
        executorService.shutdown();
    }
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }

    public void getAllPlaces(DataCallback<List<Place>> callback) {
        executorService.execute(() -> {
            try {
                List<Place> places = placeDAO.getAllPlaces();
                mainHandler.post(() -> callback.onSuccess(places));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getPlaceById(int id, DataCallback<Place> callback) {
        executorService.execute(() -> {
            try {
                Place place = placeDAO.getPlaceById(id);
                mainHandler.post(() -> callback.onSuccess(place));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getPlacesWithCoordinates(DataCallback<List<Place>> callback) {
        executorService.execute(() -> {
            try {
                List<Place> places = placeDAO.getPlacesWithCoordinates();
                mainHandler.post(() -> callback.onSuccess(places));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getVisitedPlacesWithCoordinates(DataCallback<List<Place>> callback) {
        executorService.execute(() -> {
            try {
                List<Place> places = placeDAO.getVisitedPlacesWithCoordinates();
                mainHandler.post(() -> callback.onSuccess(places));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void insertPlace(Place place, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = placeDAO.insertPlace(place);
                mainHandler.post(() -> callback.onSuccess(id));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void updatePlace(Place place, DataCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                placeDAO.updatePlace(place);
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void deletePlace(Place place, DataCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                ImageStorageService imageService = new ImageStorageService(context);
                imageService.deleteImages(place.getImageIds());

                placeDAO.deletePlace(place);
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void findDuplicate(String name, double latitude, double longitude,
                              DataCallback<Place> callback) {
        executorService.execute(() -> {
            try {
                Place byCoords = null;
                Place byName = null;
                if (latitude != 0 || longitude != 0) {
                    byCoords = placeDAO.findByCoordinates(latitude, longitude);
                }
                if (name != null && !name.isEmpty()) {
                    byName = placeDAO.findByName(name);
                }
                Place duplicate = byCoords != null ? byCoords : byName;
                Place finalDuplicate = duplicate;
                mainHandler.post(() -> callback.onSuccess(finalDuplicate));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void searchPlaces(String rawQuery, String sortSql, int filterValue, DataCallback<List<Place>> callback) {
        executorService.execute(() -> {
            try {
                String trimmed = rawQuery != null ? rawQuery.trim() : "";

                // LIKE
                List<Place> results = runLikeQuery(trimmed, sortSql, filterValue);

                // Левенштейн
                if (results.isEmpty() && !trimmed.isEmpty()) {
                    results = runFuzzySearch(trimmed, sortSql, filterValue);
                }

                List<Place> finalResults = results;
                mainHandler.post(() -> callback.onSuccess(finalResults));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private List<Place> runLikeQuery(String query, String sortSql, int filterValue) {
        StringBuilder sql = new StringBuilder("SELECT * FROM places WHERE 1=1");
        List<Object> args = new ArrayList<>();

        if (filterValue >= 0) {
            sql.append(" AND isVisited = ?");
            args.add(filterValue);
        }

        if (!query.isEmpty()) {
            sql.append(" AND (name LIKE '%' || ? || '%'")
                    .append(" OR address LIKE '%' || ? || '%'")
                    .append(" OR description LIKE '%' || ? || '%')");
            args.add(query);
            args.add(query);
            args.add(query);
        }

        sql.append(sortSql);

        SimpleSQLiteQuery sqlQuery = new SimpleSQLiteQuery(sql.toString(), args.toArray());
        return placeDAO.getPlacesRaw(sqlQuery);
    }
    private List<Place> runFuzzySearch(String query, String sortSql, int filterValue) {
        StringBuilder sql = new StringBuilder("SELECT * FROM places WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (filterValue >= 0) {
            sql.append(" AND isVisited = ?");
            args.add(filterValue);
        }
        sql.append(sortSql);

        SimpleSQLiteQuery sqlQuery = new SimpleSQLiteQuery(sql.toString(), args.toArray());
        List<Place> all = placeDAO.getPlacesRaw(sqlQuery);

        String lowerQuery = query.toLowerCase(Locale.getDefault());

        List<Place> fuzzyResults = new ArrayList<>();
        for (Place place : all) {
            if (fuzzyMatch(lowerQuery, place)) {
                fuzzyResults.add(place);
            }
        }
        return fuzzyResults;
    }

    private boolean fuzzyMatch(String query, Place place) {
        if (place.getName() == null || place.getName().isEmpty()) return false;

        String[] words = place.getName()
                .toLowerCase(Locale.getDefault())
                .split("\\s+");

        for (String word : words) {
            if (levenshtein(query, word) <= 2) return true;
        }
        return false;
    }

    private int levenshtein(String str1, String str2) {
        int[] Di_1 = new int[str2.length() + 1];
        int[] Di = new int[str2.length() + 1];

        for (int j = 0; j <= str2.length(); j++) {
            Di[j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            System.arraycopy(Di, 0, Di_1, 0, Di_1.length);

            Di[0] = i; // (j == 0)
            for (int j = 1; j <= str2.length(); j++) {
                int cost = (str1.charAt(i - 1) != str2.charAt(j - 1)) ? 1 : 0;
                Di[j] = min(
                        Di_1[j] + 1,       // удаление
                        Di[j - 1] + 1,     // вставка
                        Di_1[j - 1] + cost // замена
                );
            }
        }

        return Di[Di.length - 1];
    }

    private int min(int n1, int n2, int n3) {
        return Math.min(Math.min(n1, n2), n3);
    }

    //  из Firestore в Room
    public void upsertFromFirestore(Place place, DataCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                String fid = place.getFirestoreId();
                if (fid != null && !fid.isEmpty()) {
                    Place existing = placeDAO.findByFirestoreId(fid);
                    if (existing != null) {
                        // Запись уже есть - сохраняем Room-id и обновляем
                        place.setId(existing.getId());
                        placeDAO.updatePlace(place);
                    } else {
                        placeDAO.insertPlace(place);//новая
                    }
                } else {
                    placeDAO.insertPlace(place);
                }
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

}