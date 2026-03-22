package com.example.myapplication.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.myapplication.data.db.MuseumDB;
import com.example.myapplication.data.db.PlaceDAO;
import com.example.myapplication.data.model.Place;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaceRepository {

    private final PlaceDAO placeDAO;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private static volatile PlaceRepository instance;

    private PlaceRepository(Context context) {
        MuseumDB db = MuseumDB.getInstance(context);
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


    // Все места — для главного экрана
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

    // Место по ID — для экрана деталей
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


     //Места с координатами — для карты. lat/lon
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


    // Посещённые места с координатами — только для маркеров на карте. isVisited = true И есть lat/lon
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

    // + новое места
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

    // Обновление
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

    // Удаление
    public void deletePlace(Place place, DataCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                placeDAO.deletePlace(place);
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }


    // Проверка дублирования перед ручным добавлением
    public void findDuplicate(String name, double latitude, double longitude,
                              DataCallback<Place> callback) {
        executorService.execute(() -> {
            try {
                Place byCoords = null;
                Place byName   = null;

                //  совпадение координат
                if (latitude != 0 || longitude != 0) {
                    byCoords = placeDAO.findByCoordinates(latitude, longitude);
                }

                // названий
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
}