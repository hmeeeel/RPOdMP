package com.example.myapplication.ui.add;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.supabase.SupabaseRealtimeClient;
import com.example.myapplication.data.supabase.SupabaseRepository;

public class AddMuseumViewModel extends AndroidViewModel {

    private final SupabaseRepository supabaseRepo;
    private final PlaceRepository    roomRepo;

    private final MutableLiveData<Boolean> _saved = new MutableLiveData<>(false);
    private final MutableLiveData<String>  _error = new MutableLiveData<>();

    public final LiveData<Boolean> saved = _saved;
    public final LiveData<String>  error = _error;
    private SupabaseRealtimeClient realtimeClient;
    public AddMuseumViewModel(@NonNull Application application) {
        super(application);
        supabaseRepo = SupabaseRepository.getInstance();
        roomRepo     = PlaceRepository.getInstance(application);
    }

    public void setRealtimeClient(SupabaseRealtimeClient client) {
        this.realtimeClient = client;
    }

    public void insertPlace(Place place) {
        if (place.hasCoordinates()) {
            roomRepo.findDuplicate(
                    place.getName(),
                    place.getLatitude(),
                    place.getLongitude(),
                    new PlaceRepository.DataCallback<Place>() {
                        @Override
                        public void onSuccess(Place duplicate) {
                            if (duplicate != null) {
                                _error.setValue("Место с такими координатами уже добавлено: "
                                        + duplicate.getName());
                            } else {
                                doInsert(place);
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            doInsert(place);
                        }
                    }
            );
        } else {
            doInsert(place);
        }
    }

    private void doInsert(Place place) {
        supabaseRepo.insert(place, new PlaceRepository.DataCallback<String>() {
            @Override public void onSuccess(String supabaseId) {
                place.setFirestoreId(supabaseId);
                roomRepo.upsertFromFirestore(place, new PlaceRepository.DataCallback<Void>() {
                    @Override public void onSuccess(Void v) { _saved.setValue(true); }
                    @Override public void onError(Exception e) { _saved.setValue(true); }
                });
            }
            @Override public void onError(Exception e) { _error.setValue(e.getMessage()); }
        });
    }


    public void updatePlace(Place place) {
        supabaseRepo.update(place, new PlaceRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                roomRepo.upsertFromFirestore(place, new PlaceRepository.DataCallback<Void>() {
                    @Override public void onSuccess(Void v2) {
                        _saved.setValue(true);
                        if (realtimeClient != null) realtimeClient.refresh(); // ← уведомить
                    }
                    @Override public void onError(Exception e) { _saved.setValue(true); }
                });
            }
            @Override
            public void onError(Exception e) { _error.setValue(e.getMessage()); }
        });
    }
}