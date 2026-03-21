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
import com.example.myapplication.data.network.NetworkMonitor;
import com.example.myapplication.data.repository.YandexSearchRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapViewModel extends AndroidViewModel {

    private static final long CACHE_TTL_MS = 6 * 60 * 60 * 1000L; // 6 часов
    private static final long MAX_CACHE_AGE_MS = 30L * 24 * 60 * 60 * 1000L;// 30 дней

    private final YandexSearchRepository searchRepository;
    private final MuseumDB db;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public final NetworkMonitor networkMonitor;

    private final MutableLiveData<List<CachedPlace>> _places = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _snackbarMessage = new MutableLiveData<>();

    public final LiveData<List<CachedPlace>> places = _places;
    public final LiveData<Boolean> isLoading = _isLoading;
    public final LiveData<String> snackbarMessage = _snackbarMessage;

    public MapViewModel(@NonNull Application application) {
        super(application);
        searchRepository = YandexSearchRepository.getInstance();
        db = MuseumDB.getInstance(application);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        networkMonitor = new NetworkMonitor(application);
    }

    public void loadPlaces(double lat, double lon, boolean isOnline) {
        _isLoading.setValue(true);

        executor.execute(() -> {

            //  Сразу кеш
            List<CachedPlace> cached = db.cachedPlaceDAO()
                    .getCachedPlaces(YandexSearchRepository.SEARCH_QUERY);

            if (!cached.isEmpty()) {
                mainHandler.post(() -> _places.setValue(cached));
            }

            // Очистка кеша старше 30 дней
            long expiryTime = System.currentTimeMillis() - MAX_CACHE_AGE_MS;
            db.cachedPlaceDAO().deleteExpiredCache(expiryTime);

            // Кеш < 6 часов
            long lastUpdate = db.cachedPlaceDAO()
                    .getLastUpdateTime(YandexSearchRepository.SEARCH_QUERY);
            boolean isCacheStale = (System.currentTimeMillis() - lastUpdate) > CACHE_TTL_MS;

            if (isOnline && (isCacheStale || cached.isEmpty())) {

                // инет и кеш устарел — идём в API
                mainHandler.post(() -> {
                    searchRepository.searchMuseums(lat, lon,
                            new YandexSearchRepository.SearchCallback() {

                                @Override
                                public void onSuccess(List<CachedPlace> fresh) {
                                    // Сохр в Room
                                    executor.execute(() -> {
                                        db.cachedPlaceDAO().deleteByQuery(
                                                YandexSearchRepository.SEARCH_QUERY);
                                        db.cachedPlaceDAO().insertAll(fresh);
                                        mainHandler.post(() -> {
                                            _places.setValue(fresh);
                                            _isLoading.setValue(false);
                                            _snackbarMessage.setValue("cache_updated");
                                        });
                                    });
                                }

                                @Override
                                public void onError(String errorMsg) {
                                    // сразу кеш старый
                                    mainHandler.post(() -> {
                                        _isLoading.setValue(false);
                                        if (cached.isEmpty()) {
                                            _snackbarMessage.setValue("no_data");
                                        } else {
                                            _snackbarMessage.setValue("offline_cache");
                                        }
                                    });
                                }
                            });
                });

            } else if (!isOnline) {
                // Нет инета — используем кеш
                mainHandler.post(() -> {
                    _isLoading.setValue(false);
                    _snackbarMessage.setValue(
                            cached.isEmpty() ? "no_data" : "offline_cache");
                });

            } else {
                // Кеш свежий  - не тр
                mainHandler.post(() -> _isLoading.setValue(false));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        searchRepository.cancel();
        executor.shutdown();
    }
}
