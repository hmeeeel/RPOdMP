package com.example.myapplication.ui.routes;

import android.app.Application;
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
import java.util.Locale;

public class RoutesViewModel extends AndroidViewModel {

    private final RouteRepository   repository;
    private final AppRealtimeClient realtimeClient;
    private final String            currentUserId;

    private List<RouteCard> allCards      = new ArrayList<>();
    private String          searchQuery   = "";
    private RouteFilter     currentFilter = RouteFilter.ALL;

    private final MutableLiveData<List<RouteCard>> _routes    = new MutableLiveData<>();
    private final MutableLiveData<Boolean>         _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String>          _error     = new MutableLiveData<>();
    private final MutableLiveData<String>          _event     = new MutableLiveData<>();

    public final LiveData<List<RouteCard>> routes    = _routes;
    public final LiveData<Boolean>         isLoading = _isLoading;
    public final LiveData<String>          error     = _error;
    public final LiveData<String>          event     = _event;

    public RoutesViewModel(@NonNull Application application) {
        super(application);
        repository     = RouteRepository.getInstance();
        realtimeClient = new AppRealtimeClient();

        Long numericId = SupabaseClient.getInstance().getNumericUserId();
        currentUserId  = numericId != null ? String.valueOf(numericId) : "1";

        subscribeToChanges();
        loadRoutes(RouteFilter.ALL);
    }

    private void subscribeToChanges() {
        List<AppRealtimeClient.Subscription> subs = new ArrayList<>();

        // routes — без фильтра (нужны все: свои + публичные)
        subs.add(AppRealtimeClient.Subscription.of("routes"));

        // user_saved_routes — только строки текущего пользователя
        subs.add(AppRealtimeClient.Subscription.of(
                "user_saved_routes",
                "user_id=eq." + currentUserId
        ));

        // route_statistic — обновление рейтингов/лайков на карточках
        subs.add(AppRealtimeClient.Subscription.of("route_statistic"));

        realtimeClient.subscribe(subs, (table, type) -> {
            // Любое изменение = перезагружаем список
            refresh();
        });
    }

    public void loadRoutes(RouteFilter filter) {
        this.currentFilter = filter;
        _isLoading.setValue(true);

        repository.getAllRouteCards(currentUserId,
                new PlaceRepository.DataCallback<List<RouteCard>>() {
                    @Override public void onSuccess(List<RouteCard> data) {
                        allCards = data != null ? data : new ArrayList<>();
                        _isLoading.setValue(false);
                        applyFilterAndSearch();
                    }
                    @Override public void onError(Exception e) {
                        _isLoading.setValue(false);
                        _error.setValue(e.getMessage());
                    }
                });
    }

    public void refresh() { loadRoutes(currentFilter); }

    public void setFilter(RouteFilter filter) {
        this.currentFilter = filter;
        applyFilterAndSearch();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim() : "";
        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        List<RouteCard> filtered = applyFilter(allCards, currentFilter);
        List<RouteCard> result   = applySearch(filtered, searchQuery);
        _routes.setValue(result);
    }

    private List<RouteCard> applyFilter(List<RouteCard> src, RouteFilter filter) {
        List<RouteCard> out = new ArrayList<>();
        for (RouteCard c : src) {
            switch (filter) {
                case ALL:
                    if ("published".equals(c.getStatusCode()) || isMyRoute(c))
                        out.add(c);
                    break;
                case PUBLIC:
                    if ("published".equals(c.getStatusCode())) out.add(c);
                    break;
                case MY:
                    if (isMyRoute(c)) out.add(c);
                    break;
                case SAVED:
                    if (c.isSaved() && !isMyRoute(c)) out.add(c);
                    break;
            }
        }
        return out;
    }

    private List<RouteCard> applySearch(List<RouteCard> src, String query) {
        if (query.isEmpty()) return src;
        String lq = query.toLowerCase(Locale.getDefault());
        List<RouteCard> out = new ArrayList<>();
        for (RouteCard c : src) {
            if (c.getTitle() != null
                    && c.getTitle().toLowerCase(Locale.getDefault()).contains(lq))
                out.add(c);
        }
        return out;
    }

    private boolean isMyRoute(RouteCard c) {
        return currentUserId != null && currentUserId.equals(c.getAuthorId());
    }

    public void toggleSaveRoute(String routeId) {
        RouteCard target = null;
        for (RouteCard c : allCards) {
            if (c.getId().equals(routeId)) { target = c; break; }
        }
        if (target == null) return;

        boolean wasSaved = target.isSaved();

        if (wasSaved) {
            repository.unsaveRoute(currentUserId, routeId,
                    new PlaceRepository.DataCallback<Void>() {
                        @Override public void onSuccess(Void v) {
                            updateCardSavedState(routeId, false);
                        }
                        @Override public void onError(Exception e) {
                            _error.setValue(e.getMessage());
                        }
                    });
        } else {
            repository.saveRoute(currentUserId, routeId,
                    new PlaceRepository.DataCallback<Void>() {
                        @Override public void onSuccess(Void v) {
                            updateCardSavedState(routeId, true);
                            _event.setValue("show_progress_dialog:" + routeId);
                        }
                        @Override public void onError(Exception e) {
                            _error.setValue(e.getMessage());
                        }
                    });
        }
    }

    private void updateCardSavedState(String routeId, boolean saved) {
        for (RouteCard c : allCards) {
            if (c.getId().equals(routeId)) { c.setSaved(saved); break; }
        }
        applyFilterAndSearch();
    }

    public RouteFilter getCurrentFilter() { return currentFilter; }
    public String      getCurrentUserId() { return currentUserId; }

    @Override
    protected void onCleared() {
        super.onCleared();
        realtimeClient.remove();
    }
}