package com.example.myapplication.ui.routes;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.supabase.RouteRepository;
import com.example.myapplication.data.supabase.SupabaseClient;

import java.util.List;

public class RouteDetailViewModel extends AndroidViewModel {

    private final RouteRepository repository;
    private final String          currentUserId;

    private final MutableLiveData<List<RoutePoint>> _points    = new MutableLiveData<>();
    private final MutableLiveData<List<RouteReview>> _reviews  = new MutableLiveData<>();
    private final MutableLiveData<Boolean>           _isSaved  = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>           _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String>            _error    = new MutableLiveData<>();
    private final MutableLiveData<String>            _event    = new MutableLiveData<>();

    public final LiveData<List<RoutePoint>>  points    = _points;
    public final LiveData<List<RouteReview>> reviews   = _reviews;
    public final LiveData<Boolean>           isSaved   = _isSaved;
    public final LiveData<Boolean>           isLoading = _isLoading;
    public final LiveData<String>            error     = _error;
    public final LiveData<String>            event     = _event;
    private final Long            numericUserId;
    public RouteDetailViewModel(@NonNull Application application) {
        super(application);
        repository    = RouteRepository.getInstance();
        currentUserId = SupabaseClient.getInstance().getUserId();
        numericUserId = SupabaseClient.getInstance().getNumericUserId();

    }

    public String getCurrentUserId() { return currentUserId; }

    public void loadDetail(String routeId) {
        _isLoading.setValue(true);
        loadPoints(routeId);
        loadReviews(routeId);
        checkSavedStatus(routeId);
    }
    public void reloadPoints(String routeId) {
        repository.getRoutePoints(
                routeId,
                new PlaceRepository.DataCallback<List<RoutePoint>>() {
                    @Override
                    public void onSuccess(List<RoutePoint> data) {
                        _points.setValue(data);
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.w("RouteDetailVM", "reloadPoints error: " + e.getMessage());
                    }
                });
    }
    private void loadPoints(String routeId) {
        // numericUserId = Long (bigint) — соответствует user_places.user_id
        repository.getRoutePoints(
                routeId,
                numericUserId,  // Long, не String
                new PlaceRepository.DataCallback<List<RoutePoint>>() {
                    @Override
                    public void onSuccess(List<RoutePoint> data) {
                        _points.setValue(data);
                        _isLoading.setValue(false);
                    }
                    @Override
                    public void onError(Exception e) {
                        _isLoading.setValue(false);
                        _error.setValue(e.getMessage());
                    }
                });
    }

    private void loadReviews(String routeId) {
        repository.getReviews(routeId,
                new PlaceRepository.DataCallback<List<RouteReview>>() {
                    @Override
                    public void onSuccess(List<RouteReview> data) {
                        _reviews.setValue(data);
                    }
                    @Override
                    public void onError(Exception e) { /* не критично */ }
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
        // Используем numericUserId для user_saved_routes.user_id (bigint)
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
}