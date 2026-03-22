package com.example.myapplication.ui.map;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.db.MuseumDB;
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
    private final PlaceRepository placeRepository;
    private final MuseumDB db;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public final NetworkMonitor networkMonitor;

    private final MutableLiveData<List<MapMarker>> _markers = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _snackbarMessage = new MutableLiveData<>();

    public final LiveData<List<MapMarker>> markers = _markers;
    public final LiveData<Boolean> isLoading = _isLoading;
    public final LiveData<String> snackbarMessage = _snackbarMessage;

    public MapViewModel(@NonNull Application application) {
        super(application);
        searchRepository = YandexSearchRepository.getInstance();
        placeRepository = PlaceRepository.getInstance(application);
        db = MuseumDB.getInstance(application);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        networkMonitor = new NetworkMonitor(application);
    }

    public void loadPlaces(double lat, double lon, boolean isOnline) {
        _isLoading.setValue(true);
        executor.execute(() -> {

            // посещённые места = маркер на карте. координаты есть И isVisited = true
            List<Place> saved = db.placeDAO().getVisitedPlacesWithCoordinates();

            if (!saved.isEmpty()) {
                mainHandler.post(() -> _markers.setValue(toMarkers(saved)));
            }

            long expiryTime = System.currentTimeMillis() - MAX_CACHE_AGE_MS;
            db.cachedPlaceDAO().deleteExpiredCache(expiryTime);

            long lastUpdate = db.cachedPlaceDAO()
                    .getLastUpdateTime(YandexSearchRepository.SEARCH_QUERY);
            boolean isCacheStale = (System.currentTimeMillis() - lastUpdate) > CACHE_TTL_MS;

            if (isOnline && (isCacheStale || saved.isEmpty())) {
                mainHandler.post(() ->
                        searchRepository.searchMuseums(lat, lon,
                                new YandexSearchRepository.SearchCallback() {
                                    @Override
                                    public void onSuccess(List<CachedPlace> fresh) {
                                        executor.execute(() -> {
                                            db.cachedPlaceDAO().deleteByQuery(
                                                    YandexSearchRepository.SEARCH_QUERY);
                                            db.cachedPlaceDAO().insertAll(fresh);

                                            List<Place> newPlaces = new ArrayList<>();
                                            for (CachedPlace cached : fresh) {
                                                boolean alreadyExists =
                                                        db.placeDAO().countByCoordinates(
                                                                cached.getLatitude(),
                                                                cached.getLongitude()) > 0;
                                                if (!alreadyExists) {
                                                    newPlaces.add(Place.fromCachedPlace(cached));
                                                }
                                            }
                                            if (!newPlaces.isEmpty()) {
                                                db.placeDAO().insertAll(newPlaces);
                                            }

                                            List<Place> updated =
                                                    db.placeDAO().getVisitedPlacesWithCoordinates();

                                            mainHandler.post(() -> {
                                                _markers.setValue(toMarkers(updated));
                                                _isLoading.setValue(false);
                                                _snackbarMessage.setValue("cache_updated");
                                            });
                                        });
                                    }

                                    @Override
                                    public void onError(String errorMsg) {
                                        mainHandler.post(() -> {
                                            _isLoading.setValue(false);
                                            _snackbarMessage.setValue(
                                                    saved.isEmpty() ? "no_data" : "offline_cache");
                                        });
                                    }
                                })
                );
            } else if (!isOnline) {
                mainHandler.post(() -> {
                    _isLoading.setValue(false);
                    _snackbarMessage.setValue(
                            saved.isEmpty() ? "no_data" : "offline_cache");
                });
            } else {
                mainHandler.post(() -> _isLoading.setValue(false));
            }
        });
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