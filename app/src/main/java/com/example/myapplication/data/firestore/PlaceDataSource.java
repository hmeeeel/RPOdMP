package com.example.myapplication.data.firestore;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;

import java.util.List;

 // ViewModel работает только через это!
public interface PlaceDataSource {
    void getAll(PlaceRepository.DataCallback<List<Place>> callback);
    void getById(String id, PlaceRepository.DataCallback<Place> callback);
    void insert(Place place, PlaceRepository.DataCallback<String> callback);
    void update(Place place, PlaceRepository.DataCallback<Void> callback);
    void delete(String id, PlaceRepository.DataCallback<Void> callback);
}
