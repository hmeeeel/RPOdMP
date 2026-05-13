package com.example.myapplication.ui.routes;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.supabase.AppRealtimeClient;
import com.example.myapplication.data.supabase.RouteRepository;
import com.example.myapplication.data.supabase.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

public class RouteDetailViewModel extends AndroidViewModel {

    private static final String TAG = "RouteDetailVM";

    private final RouteRepository   repository;
    private final AppRealtimeClient realtimeClient;
    private final String            currentUserId;
    private final Long              numericUserId;

    // ID маршрута — нужен для перезагрузки по Realtime
    private String currentRouteId;
    private boolean isSubscribed = false;
    private final MutableLiveData<List<RoutePoint>>  _points    = new MutableLiveData<>();
    private final MutableLiveData<List<RouteReview>> _reviews   = new MutableLiveData<>();
    private final MutableLiveData<Boolean>           _isSaved   = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>           _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String>            _error     = new MutableLiveData<>();
    private final MutableLiveData<String>            _event     = new MutableLiveData<>();

    public final LiveData<List<RoutePoint>>  points    = _points;
    public final LiveData<List<RouteReview>> reviews   = _reviews;
    public final LiveData<Boolean>           isSaved   = _isSaved;
    public final LiveData<Boolean>           isLoading = _isLoading;
    public final LiveData<String>            error     = _error;
    public final LiveData<String>            event     = _event;
    private final MutableLiveData<RouteHeaderData> _routeHeader = new MutableLiveData<>();
    public final LiveData<RouteHeaderData> routeHeader = _routeHeader;

    // Простой класс-контейнер — добавь внутри RouteDetailViewModel
    public static class RouteHeaderData {
        public final String title;
        public final String description;
        public final String statusCode;
        public final String adminNote;

        public RouteHeaderData(String title, String description,
                               String statusCode, String adminNote) {
            this.title       = title;
            this.description = description;
            this.statusCode  = statusCode;
            this.adminNote   = adminNote;
        }
    }

    public void loadRouteHeader(String routeId) {
        String query = "select=title,description,admin_note,"
                + "route_statuses!status_id(code)"
                + "&id=eq." + routeId;

        RouteRepository.getInstance().fetchRouteHeader(
                routeId,
                new PlaceRepository.DataCallback<RouteHeaderData>() {
                    @Override
                    public void onSuccess(RouteHeaderData data) {
                        _routeHeader.setValue(data);
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.w(TAG, "loadRouteHeader error: " + e.getMessage());
                    }
                });
    }
    public RouteDetailViewModel(@NonNull Application application) {
        super(application);
        repository     = RouteRepository.getInstance();
        realtimeClient = new AppRealtimeClient();
        currentUserId  = SupabaseClient.getInstance().getUserId();
        numericUserId  = SupabaseClient.getInstance().getNumericUserId();
    }

    public String getCurrentUserId() { return currentUserId; }

    // Первичная загрузка
    public void loadDetail(String routeId) {
        this.currentRouteId = routeId;
        _isLoading.setValue(true);
        loadPoints(routeId);
        loadReviews(routeId);
        checkSavedStatus(routeId);

        // ПОДПИСКА ТОЛЬКО ОДИН РАЗ
        if (!isSubscribed) {
            isSubscribed = true;
            subscribeToRouteChanges(routeId);
        }
    }
    //Перезагрузка без переподписки
    // Вызывается из editLauncher после редактирования
    public void reloadAllData() {
        if (currentRouteId == null) return;
        _isLoading.setValue(true);
        loadPoints(currentRouteId);
        loadReviews(currentRouteId);
        checkSavedStatus(currentRouteId);
        loadRouteHeader(currentRouteId); // ← ДОБАВЬ
    }
    private void subscribeToRouteChanges(String routeId) {
        List<AppRealtimeClient.Subscription> subs = new ArrayList<>();

        subs.add(AppRealtimeClient.Subscription.of(
                "route_points", "route_id=eq." + routeId));
        subs.add(AppRealtimeClient.Subscription.of(
                "routes", "id=eq." + routeId));
        subs.add(AppRealtimeClient.Subscription.of(
                "route_reviews", "route_id=eq." + routeId));
        if (numericUserId != null) {
            subs.add(AppRealtimeClient.Subscription.of(
                    "user_places", "user_id=eq." + numericUserId));
        }
        subs.add(AppRealtimeClient.Subscription.of("places"));

        realtimeClient.subscribe(subs, new AppRealtimeClient.TableChangeCallback() {
            @Override
            public void onChange(String table, String type) {
                switch (table) {
                    case "route_points":
                    case "user_places":
                    case "places":
                        reloadPoints(routeId);
                        break;
                    case "routes":
                        // ← ТЕПЕРЬ загружаем актуальные данные вместо просто события
                        loadRouteHeader(routeId);
                        _event.setValue("route_status_changed");
                        break;
                    case "route_reviews":
                        loadReviews(routeId);
                        break;
                }
            }
        });
    }

    public void reloadPoints(String routeId) {
        repository.getRoutePoints(
                routeId,
                numericUserId,
                new PlaceRepository.DataCallback<List<RoutePoint>>() {
                    @Override public void onSuccess(List<RoutePoint> data) {
                        _points.setValue(data);
                    }
                    @Override public void onError(Exception e) {
                        Log.w(TAG, "reloadPoints error: " + e.getMessage());
                    }
                });
    }

    private void loadPoints(String routeId) {
        repository.getRoutePoints(
                routeId,
                numericUserId,
                new PlaceRepository.DataCallback<List<RoutePoint>>() {
                    @Override public void onSuccess(List<RoutePoint> data) {
                        _points.setValue(data);
                        _isLoading.setValue(false);
                    }
                    @Override public void onError(Exception e) {
                        _isLoading.setValue(false);
                        _error.setValue(e.getMessage());
                    }
                });
    }

    private void loadReviews(String routeId) {
        repository.getReviews(routeId,
                new PlaceRepository.DataCallback<List<RouteReview>>() {
                    @Override public void onSuccess(List<RouteReview> data) {
                        _reviews.setValue(data);
                    }
                    @Override public void onError(Exception e) { /* не критично */ }
                });
    }

    public void checkSavedStatus(String routeId) {
        if (numericUserId == null) return;
        repository.checkSaved(String.valueOf(numericUserId), routeId,
                new PlaceRepository.DataCallback<Boolean>() {
                    @Override public void onSuccess(Boolean saved) { _isSaved.setValue(saved); }
                    @Override public void onError(Exception e)     { _isSaved.setValue(false); }
                });
    }

    public void toggleSave(String routeId) {
        String uid = numericUserId != null
                ? String.valueOf(numericUserId)
                : currentUserId;

        Boolean saved = _isSaved.getValue();
        if (Boolean.TRUE.equals(saved)) {
            repository.unsaveRoute(uid, routeId,
                    new PlaceRepository.DataCallback<Void>() {
                        @Override public void onSuccess(Void v) { _isSaved.setValue(false); }
                        @Override public void onError(Exception e) { _error.setValue(e.getMessage()); }
                    });
        } else {
            repository.saveRoute(uid, routeId,
                    new PlaceRepository.DataCallback<Void>() {
                        @Override public void onSuccess(Void v) {
                            _isSaved.setValue(true);
                            _event.setValue("saved_ok");
                        }
                        @Override public void onError(Exception e) { _error.setValue(e.getMessage()); }
                    });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        realtimeClient.remove();
    }

    public String getCurrentNumericUserId() {
        return numericUserId != null ? String.valueOf(numericUserId) : null;
    }
}