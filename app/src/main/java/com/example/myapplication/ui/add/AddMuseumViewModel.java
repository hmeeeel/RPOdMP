package com.example.myapplication.ui.add;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.firestore.FirestoreRepository;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;

public class AddMuseumViewModel extends AndroidViewModel {

    private final FirestoreRepository firestoreRepository;
    private final PlaceRepository     roomRepository;

    private final MutableLiveData<Boolean> _saved = new MutableLiveData<>(false);
    private final MutableLiveData<String>  _error = new MutableLiveData<>();

    public final LiveData<Boolean> saved = _saved;
    public final LiveData<String>  error = _error;

    public AddMuseumViewModel(@NonNull Application application) {
        super(application);
        firestoreRepository = FirestoreRepository.getInstance();
        roomRepository      = PlaceRepository.getInstance(application);
    }

    public void insertPlace(Place place) {
        firestoreRepository.insert(place, new PlaceRepository.DataCallback<String>() {
            @Override
            public void onSuccess(String firestoreId) {
                place.setFirestoreId(firestoreId);
                // firestoreId теперь хранится в Room - upsert найдёт или создаст запись
                roomRepository.upsertFromFirestore(place, new PlaceRepository.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void v) { _saved.setValue(true); }
                    @Override
                    public void onError(Exception e) { _saved.setValue(true); }
                });
            }
            @Override
            public void onError(Exception e) { _error.setValue(e.getMessage()); }
        });
    }

    public void updatePlace(Place place) {
        firestoreRepository.update(place, new PlaceRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                // upsert найдёт запись по firestoreId и обновит её
                roomRepository.upsertFromFirestore(place, new PlaceRepository.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void v2) { _saved.setValue(true); }
                    @Override
                    public void onError(Exception e) { _saved.setValue(true); }
                });
            }
            @Override
            public void onError(Exception e) { _error.setValue(e.getMessage()); }
        });
    }
}