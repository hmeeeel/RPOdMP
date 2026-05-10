package com.example.myapplication.ui.main;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.supabase.SupabaseClient;
import com.example.myapplication.data.supabase.SupabaseRealtimeClient;
import com.example.myapplication.data.supabase.SupabaseRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MuseumViewModel extends AndroidViewModel {

    public enum SortOption   { NEWEST, OLDEST, AZ, ZA }
    public enum FilterOption { ALL(-1), VISITED(1), PLANNED(0);
        public final int value;
        FilterOption(int v) { value = v; }
    }

    private static final String PREFS_NAME = "search_prefs";
    private static final String KEY_SORT   = "pref_sort";
    private static final String KEY_FILTER = "pref_filter";

    private final SupabaseRepository       repository;
    private final SupabaseRealtimeClient   realtimeClient;
    private final SharedPreferences        prefs;

    private List<Place> allPlaces    = new ArrayList<>();
    private String      currentQuery = "";
    private SortOption  currentSort;
    private FilterOption currentFilter;

    private final MutableLiveData<List<Place>> _places    = new MutableLiveData<>();
    private final MutableLiveData<String>      _error     = new MutableLiveData<>();
    private final MutableLiveData<Boolean>     _isLoading = new MutableLiveData<>(false);

    public final LiveData<List<Place>> museums   = _places;
    public final LiveData<String>      error     = _error;
    public final LiveData<Boolean>     isLoading = _isLoading;

    public MuseumViewModel(@NonNull Application application) {
        super(application);
        repository     = SupabaseRepository.getInstance();
        realtimeClient = new SupabaseRealtimeClient();
        prefs          = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        try { currentSort   = SortOption.valueOf(prefs.getString(KEY_SORT, SortOption.NEWEST.name())); }
        catch (Exception e) { currentSort = SortOption.NEWEST; }
        try { currentFilter = FilterOption.valueOf(prefs.getString(KEY_FILTER, FilterOption.ALL.name())); }
        catch (Exception e) { currentFilter = FilterOption.ALL; }

        subscribeToSupabase();
    }

    private void subscribeToSupabase() {
        _isLoading.setValue(true);
        String userId = SupabaseClient.getInstance().getUserId();
        if (userId == null) {
            _isLoading.setValue(false);
            _error.setValue("Пользователь не авторизован");
            return;
        }

        realtimeClient.subscribe(userId, new SupabaseRealtimeClient.RealtimeDataCallback() {

            @Override
            public void onData(List<Place> data) {
                allPlaces = data != null ? data : new ArrayList<>();
                _isLoading.setValue(false);
                applyFilterSortSearch();
            }

            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _error.setValue(e.getMessage());
            }
        });
    }

    // loadMuseums теперь принудительно идёт на сервер
    public void loadMuseums() {
        realtimeClient.refresh();
    }

   // public void loadMuseums() { applyFilterSortSearch(); }

    public void setQuery(String q) {
        currentQuery = q != null ? q.trim() : "";
        applyFilterSortSearch();
    }

    public void setSort(SortOption s) {
        currentSort = s;
        prefs.edit().putString(KEY_SORT, s.name()).apply();
        applyFilterSortSearch();
    }

    public void setFilter(FilterOption f) {
        currentFilter = f;
        prefs.edit().putString(KEY_FILTER, f.name()).apply();
        applyFilterSortSearch();
    }

    private void applyFilterSortSearch() {
        List<Place> filtered = new ArrayList<>();
        for (Place p : allPlaces) {
            if (currentFilter == FilterOption.ALL) filtered.add(p);
            else if (currentFilter == FilterOption.VISITED  &&  p.isVisited()) filtered.add(p);
            else if (currentFilter == FilterOption.PLANNED  && !p.isVisited()) filtered.add(p);
        }
        List<Place> searched = currentQuery.isEmpty() ? filtered : search(currentQuery, filtered);
        sort(searched);
        _places.setValue(searched);
    }

    private List<Place> search(String q, List<Place> src) {
        List<Place> r = likeSearch(q, src);
        return r.isEmpty() ? fuzzySearch(q, src) : r;
    }

    private List<Place> likeSearch(String query, List<Place> source) {
        String lq = query.toLowerCase(Locale.getDefault());
        List<Place> r = new ArrayList<>();
        for (Place p : source)
            if (contains(p.getName(), lq) || contains(p.getAddress(), lq) || contains(p.getDescription(), lq))
                r.add(p);
        return r;
    }

    private boolean contains(String field, String q) {
        return field != null && field.toLowerCase(Locale.getDefault()).contains(q);
    }

    private List<Place> fuzzySearch(String query, List<Place> source) {
        String lq = query.toLowerCase(Locale.getDefault());
        List<Place> r = new ArrayList<>();
        for (Place p : source) if (fuzzyMatch(lq, p)) r.add(p);
        return r;
    }

    private boolean fuzzyMatch(String q, Place p) {
        if (p.getName() == null) return false;
        for (String w : p.getName().toLowerCase(Locale.getDefault()).split("\\s+"))
            if (levenshtein(q, w) <= 2) return true;
        return false;
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1], curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) curr[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            System.arraycopy(curr, 0, prev, 0, curr.length);
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i-1) != b.charAt(j-1) ? 1 : 0;
                curr[j] = Math.min(Math.min(prev[j]+1, curr[j-1]+1), prev[j-1]+cost);
            }
        }
        return curr[b.length()];
    }

    private void sort(List<Place> list) {
        switch (currentSort) {
            case NEWEST: Collections.sort(list, (a,b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt())); break;
            case OLDEST: Collections.sort(list, (a,b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt())); break;
            case AZ: Collections.sort(list, (a,b) -> name(a).compareTo(name(b))); break;
            case ZA: Collections.sort(list, (a,b) -> name(b).compareTo(name(a))); break;
        }
    }

    private String name(Place p) {
        return p.getName() != null ? p.getName().toLowerCase(Locale.getDefault()) : "";
    }

    public void deletePlace(Place place) {
        repository.delete(place.getFirestoreId(), new com.example.myapplication.data.repository.PlaceRepository.DataCallback<Void>() {
            @Override public void onSuccess(Void v) {}
            @Override public void onError(Exception e) { _error.setValue(e.getMessage()); }
        });
    }

    @Override protected void onCleared() {
        super.onCleared();
        realtimeClient.remove();
    }

    public String       getCurrentQuery()  { return currentQuery; }
    public SortOption   getCurrentSort()   { return currentSort; }
    public FilterOption getCurrentFilter() { return currentFilter; }
}