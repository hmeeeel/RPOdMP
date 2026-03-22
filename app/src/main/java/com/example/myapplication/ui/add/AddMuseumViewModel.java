package com.example.myapplication.ui.add;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;

public class AddMuseumViewModel extends AndroidViewModel {

    private final PlaceRepository repository;

    private final MutableLiveData<Boolean> _saved = new MutableLiveData<>(false);
    private final MutableLiveData<String> _error = new MutableLiveData<>();

    public final LiveData<Boolean> saved = _saved;
    public final LiveData<String> error = _error;

    public AddMuseumViewModel(@NonNull Application application) {
        super(application);
        repository = PlaceRepository.getInstance(application);
    }

    public void insertPlace(Place place) {
        repository.insertPlace(place, new PlaceRepository.DataCallback<Long>() {
            @Override
            public void onSuccess(Long id) { _saved.setValue(true); }

            @Override
            public void onError(Exception e) { _error.setValue(e.getMessage()); }
        });
    }

    public void updatePlace(Place place) {
        repository.updatePlace(place, new PlaceRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void v) { _saved.setValue(true); }

            @Override
            public void onError(Exception e) { _error.setValue(e.getMessage()); }
        });
    }
}