package com.example.myapplication.ui.main;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.firestore.FirestoreRepository;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MuseumViewModel extends AndroidViewModel {

    public enum SortOption { NEWEST, OLDEST, AZ, ZA }

    public enum FilterOption {
        ALL(-1), VISITED(1), PLANNED(0);
        public final int value;
        FilterOption(int value) { this.value = value; }
    }

    private static final String PREFS_NAME   = "search_prefs";
    private static final String KEY_SORT     = "pref_sort";
    private static final String KEY_FILTER   = "pref_filter";
    private static final String KEY_QUERY    = "pref_search_query";

    private final FirestoreRepository repository;
    private final SharedPreferences   prefs;

    private String       currentQuery;
    private SortOption   currentSort;
    private FilterOption currentFilter;

    private final MutableLiveData<List<Place>> _places    = new MutableLiveData<>();
    private final MutableLiveData<String>      _error     = new MutableLiveData<>();
    private final MutableLiveData<Boolean>     _isLoading = new MutableLiveData<>(false);

    public final LiveData<List<Place>> museums   = _places;
    public final LiveData<String>      error     = _error;
    public final LiveData<Boolean>     isLoading = _isLoading;

    public MuseumViewModel(@NonNull Application application) {
        super(application);
        repository = FirestoreRepository.getInstance();
        prefs      = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        currentQuery = "";

        String savedSort = prefs.getString(KEY_SORT, SortOption.NEWEST.name());
        try { currentSort = SortOption.valueOf(savedSort); }
        catch (IllegalArgumentException e) { currentSort = SortOption.NEWEST; }

        String savedFilter = prefs.getString(KEY_FILTER, FilterOption.ALL.name());
        try { currentFilter = FilterOption.valueOf(savedFilter); }
        catch (IllegalArgumentException e) { currentFilter = FilterOption.ALL; }

        loadMuseums();
    }

    // Изменение состояния без SQL
    public void setQuery(String query) {
        currentQuery = query != null ? query.trim() : "";
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

        repository.getAll(new PlaceRepository.DataCallback<List<Place>>() {
            @Override
            public void onSuccess(List<Place> data) {
                List<Place> result = applyFilterSortSearch(data);
                _isLoading.setValue(false);
                _places.setValue(result);// передача livedata
            }

            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _error.setValue(e.getMessage());
            }
        });
    }

     // Фильтрация, поиск и сортировка выполняются в памяти
    private List<Place> applyFilterSortSearch(List<Place> all) {
        // 1. Фильтр по статусу
        List<Place> filtered = new ArrayList<>();
        for (Place p : all) {
            if (currentFilter == FilterOption.ALL) {
                filtered.add(p);
            } else if (currentFilter == FilterOption.VISITED && p.isVisited()) {
                filtered.add(p);
            } else if (currentFilter == FilterOption.PLANNED && !p.isVisited()) {
                filtered.add(p);
            }
        }

        // 2. Поиск по запросу
        List<Place> searched;
        if (currentQuery.isEmpty()) {
            searched = filtered;
        } else {
            searched = likeSearch(currentQuery, filtered);
            if (searched.isEmpty()) {
                searched = fuzzySearch(currentQuery, filtered);
            }
        }

        // 3. Сортировка
        sort(searched);

        return searched;
    }

    // runLikeQuery
    private List<Place> likeSearch(String query, List<Place> source) {
        String lq = query.toLowerCase(Locale.getDefault());
        List<Place> result = new ArrayList<>();
        for (Place p : source) {
            if (contains(p.getName(), lq)
                    || contains(p.getAddress(), lq)
                    || contains(p.getDescription(), lq)) {
                result.add(p);
            }
        }
        return result;
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase(Locale.getDefault()).contains(query);
    }

    //runFuzzySearch
    private List<Place> fuzzySearch(String query, List<Place> source) {
        String lq = query.toLowerCase(Locale.getDefault());
        List<Place> result = new ArrayList<>();
        for (Place p : source) {
            if (fuzzyMatch(lq, p)) result.add(p);
        }
        return result;
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


    private void sort(List<Place> list) {
        switch (currentSort) {
            case NEWEST:
                Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            case OLDEST:
                Collections.sort(list, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                break;
            case AZ:
                Collections.sort(list, (a, b) -> {
                    String na = a.getName() != null ? a.getName().toLowerCase(Locale.getDefault()) : "";
                    String nb = b.getName() != null ? b.getName().toLowerCase(Locale.getDefault()) : "";
                    return na.compareTo(nb);
                });
                break;
            case ZA:
                Collections.sort(list, (a, b) -> {
                    String na = a.getName() != null ? a.getName().toLowerCase(Locale.getDefault()) : "";
                    String nb = b.getName() != null ? b.getName().toLowerCase(Locale.getDefault()) : "";
                    return nb.compareTo(na);
                });
                break;
        }
    }

    public void deletePlace(Place place) {
        repository.delete(place.getFirestoreId(), new PlaceRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void v) { loadMuseums(); }

            @Override
            public void onError(Exception e) { _error.setValue(e.getMessage()); }
        });
    }

    public String       getCurrentQuery()  { return currentQuery; }
    public SortOption   getCurrentSort()   { return currentSort; }
    public FilterOption getCurrentFilter() { return currentFilter; }
}