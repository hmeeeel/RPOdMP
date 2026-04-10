package com.example.myapplication.ui.map;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.db.MuseumDB;
import com.example.myapplication.data.firestore.FirestoreRepository;
import com.example.myapplication.data.model.CachedPlace;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.network.NetworkMonitor;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.repository.YandexSearchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapViewModel extends AndroidViewModel {

    private static final long CACHE_TTL_MS = 6 * 60 * 60 * 1000L; // 6 часов
    private static final long MAX_CACHE_AGE_MS = 30L * 24 * 60 * 60 * 1000L;// 30 дней

    private final YandexSearchRepository searchRepository;
    private final PlaceRepository        placeRepository;
    private final FirestoreRepository    firestoreRepository;
    private final MuseumDB               db;
    private final ExecutorService        executor;
    private final Handler                mainHandler;

    public final NetworkMonitor networkMonitor;

    private final MutableLiveData<List<MapMarker>> _markers         = new MutableLiveData<>();
    private final MutableLiveData<Boolean>         _isLoading       = new MutableLiveData<>(false);
    private final MutableLiveData<String>          _snackbarMessage = new MutableLiveData<>();

    public final LiveData<List<MapMarker>> markers         = _markers;
    public final LiveData<Boolean>         isLoading       = _isLoading;
    public final LiveData<String>          snackbarMessage = _snackbarMessage;

    public MapViewModel(@NonNull Application application) {
        super(application);
        searchRepository     = YandexSearchRepository.getInstance();
        placeRepository      = PlaceRepository.getInstance(application);
        firestoreRepository  = FirestoreRepository.getInstance();
        db                   = MuseumDB.getInstance(application);
        executor             = Executors.newSingleThreadExecutor();
        mainHandler          = new Handler(Looper.getMainLooper());
        networkMonitor       = new NetworkMonitor(application);
    }

    public void loadPlaces(double lat, double lon, boolean isOnline) {
        _isLoading.setValue(true);

        // 1 Загружаем пользовательские места из Firestore
        firestoreRepository.getAll(new PlaceRepository.DataCallback<List<Place>>() {
            @Override
            public void onSuccess(List<Place> firestorePlaces) {
                //  Фильтруем только места с координатами
                List<Place> withCoords = new ArrayList<>();
                for (Place p : firestorePlaces) {
                    if (p.hasCoordinates()) {
                        withCoords.add(p);
                    }
                }

                // Сразу отображаем на карте (пользовательские маркеры) Конвертируем Place in MapMarker
                List<MapMarker> userMarkers = toMarkers(withCoords);
                _markers.setValue(userMarkers); // без янд

                //  2 Если онлайн, загружаем Yandex API
                if (isOnline) {
                    loadYandexPlaces(lat, lon, withCoords, userMarkers);
                } else {
                    _isLoading.setValue(false);
                    if (withCoords.isEmpty()) {
                        _snackbarMessage.setValue("no_data");
                    } else {
                        _snackbarMessage.setValue("offline_cache");
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _snackbarMessage.setValue("error_loading");
            }
        });
    }

    private void loadYandexPlaces(double lat, double lon,
                                  List<Place> existingPlaces,
                                  List<MapMarker> userMarkers) {
        executor.execute(() -> {
            // Чистим устаревший кэш 30 дн и 6ч
            long expiryTime = System.currentTimeMillis() - MAX_CACHE_AGE_MS;
            db.cachedPlaceDAO().deleteExpiredCache(expiryTime);

            long lastUpdate = db.cachedPlaceDAO().getLastUpdateTime(
                    YandexSearchRepository.SEARCH_QUERY);
            boolean isCacheStale = (System.currentTimeMillis() - lastUpdate) > CACHE_TTL_MS;

            if (isCacheStale) {// утсарел
                // Запрос к Yandex API
                mainHandler.post(() ->
                        searchRepository.searchMuseums(lat, lon,
                                new YandexSearchRepository.SearchCallback() {
                                    @Override
                                    public void onSuccess(List<CachedPlace> fresh) {
                                        executor.execute(() -> {
                                            // Обновляем кэш Яндекс API
                                            db.cachedPlaceDAO().deleteByQuery(
                                                    YandexSearchRepository.SEARCH_QUERY);
                                            db.cachedPlaceDAO().insertAll(fresh);

                                            // Фильтруем новые места
                                            List<CachedPlace> newPlaces =
                                                    filterNewPlaces(fresh, existingPlaces);

                                            // Объединяем маркеры
                                            List<MapMarker> allMarkers = new ArrayList<>(userMarkers);
                                            allMarkers.addAll(toMarkersFromCache(newPlaces));

                                            mainHandler.post(() -> {
                                                _markers.setValue(allMarkers);
                                                _isLoading.setValue(false);
                                                if (!newPlaces.isEmpty()) {
                                                    _snackbarMessage.setValue("cache_updated");
                                                }
                                            });
                                        });
                                    }

                                    @Override
                                    public void onError(String errorMsg) {
                                        mainHandler.post(() -> {
                                            _isLoading.setValue(false);
                                            _snackbarMessage.setValue("offline_cache");
                                        });
                                    }
                                })
                );
            } else {
                // Используем кэш
                List<CachedPlace> cached = db.cachedPlaceDAO().getCachedPlaces(
                        YandexSearchRepository.SEARCH_QUERY);
                List<CachedPlace> newPlaces = filterNewPlaces(cached, existingPlaces);

                List<MapMarker> allMarkers = new ArrayList<>(userMarkers);
                allMarkers.addAll(toMarkersFromCache(newPlaces));

                mainHandler.post(() -> {
                    _markers.setValue(allMarkers);
                    _isLoading.setValue(false);
                });
            }
        });
    }

    // Фильтрация: убираем дубликаты (уже сохранённые в Firestore)
    private List<CachedPlace> filterNewPlaces(List<CachedPlace> cached,
                                              List<Place> existing) {
        List<CachedPlace> newPlaces = new ArrayList<>();

        for (CachedPlace c : cached) {
            boolean isDuplicate = false;

            for (Place p : existing) {
                if (Math.abs(p.getLatitude() - c.getLatitude()) < 0.0001 &&
                        Math.abs(p.getLongitude() - c.getLongitude()) < 0.0001) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                newPlaces.add(c);
            }
        }

        return newPlaces;
    }

    // Конвертация CachedPlace в MapMarker (БЕЗ сохранения в БД!)
    private List<MapMarker> toMarkersFromCache(List<CachedPlace> cached) {
        List<MapMarker> markers = new ArrayList<>();
        for (CachedPlace c : cached) {
            markers.add(MapMarker.fromCachedPlace(c));
        }
        return markers;
    }


    private void saveNewPlacesToFirestore(List<Place> places) {
        for (Place place : places) {
            firestoreRepository.insert(place, new PlaceRepository.DataCallback<String>() {
                @Override
                public void onSuccess(String firestoreId) {
                    // firestoreId установлен в place.setFirestoreId() внутри insert()
                }

                @Override
                public void onError(Exception e) {
                    // Ошибка Firestore не влияет на карту — Room уже записан
                }
            });
        }
    }

    private List<MapMarker> toMarkers(List<Place> places) {
        List<MapMarker> markers = new ArrayList<>();
        for (Place p : places) {
            markers.add(MapMarker.fromPlace(p));
        }
        return markers;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        searchRepository.cancel();
        executor.shutdown();
    }
}