package com.example.myapplication.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;

import java.util.List;

public class MuseumViewModel extends AndroidViewModel {

    private final PlaceRepository repository;

    private final MutableLiveData<List<Place>> _places = new MutableLiveData<>();
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public final LiveData<List<Place>> museums = _places;
    public final LiveData<String> error = _error;
    public final LiveData<Boolean> isLoading = _isLoading;

    public MuseumViewModel(@NonNull Application application) {
        super(application);
        repository = PlaceRepository.getInstance(application);
    }

    public void loadMuseums() {
        _isLoading.setValue(true);
        repository.getAllPlaces(new PlaceRepository.DataCallback<List<Place>>() {
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
        });
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
}