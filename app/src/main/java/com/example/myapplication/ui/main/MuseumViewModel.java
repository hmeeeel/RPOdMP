package com.example.myapplication.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.model.Museum;
import com.example.myapplication.data.repository.MuseumRepository;

import java.util.List;

public class MuseumViewModel extends AndroidViewModel {

    private final MuseumRepository repository;

    private final MutableLiveData<List<Museum>> _museums = new MutableLiveData<>();
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public final LiveData<List<Museum>> museums = _museums;
    public final LiveData<String> error = _error;
    public final LiveData<Boolean> isLoading = _isLoading;

    public MuseumViewModel(@NonNull Application application) {
        super(application);
        repository = MuseumRepository.getInstance(application);
    }
    public void loadMuseums() {
        _isLoading.setValue(true);
        repository.getAllMuseums(new MuseumRepository.DataCallback<List<Museum>>() {
            @Override
            public void onSuccess(List<Museum> data) {
                _isLoading.setValue(false);
                _museums.setValue(data);
            }
            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _error.setValue(e.getMessage());
            }
        });
    }

    public void deleteMuseum(Museum museum) {
        repository.deleteMuseum(museum, new MuseumRepository.DataCallback<Void>() {
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