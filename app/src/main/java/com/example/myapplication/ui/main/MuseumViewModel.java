package com.example.myapplication.ui.main;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;

import java.util.List;

public class MuseumViewModel extends AndroidViewModel {
    public enum SortOption {
        NEWEST(" ORDER BY createdAt DESC"),
        OLDEST(" ORDER BY createdAt ASC"),
        AZ(" ORDER BY LOWER(name) ASC"),
        ZA(" ORDER BY LOWER(name) DESC");

        public final String sql; //фрагмент, который вставляется в конец

        SortOption(String sql) {
            this.sql = sql;
        }
    }

    public enum FilterOption {
        ALL(-1),  VISITED(1),  PLANNED(0);

        public final int value;

        FilterOption(int value) {
            this.value = value;
        }
    }

    private static final String PREFS_NAME   = "search_prefs";
    private static final String KEY_SORT     = "pref_sort";
    private static final String KEY_FILTER   = "pref_filter";
    private static final String KEY_QUERY    = "pref_search_query";


    private final PlaceRepository repository;
    private final SharedPreferences prefs;

    private String      currentQuery;
    private SortOption  currentSort;
    private FilterOption currentFilter;

    private final MutableLiveData<List<Place>> _places    = new MutableLiveData<>();
    private final MutableLiveData<String>      _error     = new MutableLiveData<>();
    private final MutableLiveData<Boolean>     _isLoading = new MutableLiveData<>(false);

    public final LiveData<List<Place>>  museums   = _places;
    public final LiveData<String>       error     = _error;
    public final LiveData<Boolean>      isLoading = _isLoading;

    public MuseumViewModel(@NonNull Application application) {
        super(application);
        repository = PlaceRepository.getInstance(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
      //  currentQuery = prefs.getString(KEY_QUERY, "");
        currentQuery = "";

        String savedSort = prefs.getString(KEY_SORT, SortOption.NEWEST.name());
        try {
            currentSort = SortOption.valueOf(savedSort);
        } catch (IllegalArgumentException e) {
            currentSort = SortOption.NEWEST;
        }

        String savedFilter = prefs.getString(KEY_FILTER, FilterOption.ALL.name());
        try {
            currentFilter = FilterOption.valueOf(savedFilter);
        } catch (IllegalArgumentException e) {
            currentFilter = FilterOption.ALL;
        }

        loadMuseums();
    }

    // Методы изменения состояния
    public void setQuery(String query) {
        currentQuery = query != null ? query.trim() : "";
       // prefs.edit().putString(KEY_QUERY, currentQuery).apply();
        loadMuseums();
    }

    public void setSort(SortOption sort) {
        currentSort = sort;
        prefs.edit().putString(KEY_SORT, sort.name()).apply();
        loadMuseums();
    }

    public void setFilter(FilterOption filter) {
        currentFilter = filter;
        prefs.edit().putString(KEY_FILTER, filter.name()).apply();
        loadMuseums();
    }


    public void loadMuseums() {
        _isLoading.setValue(true);
        repository.searchPlaces(
                currentQuery,
                currentSort.sql,
                currentFilter.value,
                new PlaceRepository.DataCallback<List<Place>>() {
                    @Override
                    public void onSuccess(List<Place> data) {
                        _isLoading.setValue(false);
                        _places.setValue(data);
                    }

                    @Override
                    public void onError(Exception e) {
                        _isLoading.setValue(false);
                        _error.setValue(e.getMessage());
                    }
                }
        );
    }

    public void deletePlace(Place place) {
        repository.deletePlace(place, new PlaceRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                loadMuseums();
            }

            @Override
            public void onError(Exception e) {
                _error.setValue(e.getMessage());
            }
        });
    }

    public String       getCurrentQuery()  { return currentQuery; }
    public SortOption   getCurrentSort()   { return currentSort; }
    public FilterOption getCurrentFilter() { return currentFilter; }
}