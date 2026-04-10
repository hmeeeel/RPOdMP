package com.example.myapplication.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.myapplication.data.model.Place;

import java.util.List;

@Dao
public interface PlaceDAO {

    @Query("SELECT * FROM places ORDER BY createdAt DESC")
    List<Place> getAllPlaces();

    @Query("SELECT * FROM places WHERE id = :id")
    Place getPlaceById(int id);

    @Query("SELECT * FROM places WHERE latitude != 0 OR longitude != 0")
    List<Place> getPlacesWithCoordinates();

    @Query("SELECT * FROM places WHERE (latitude != 0 OR longitude != 0) AND isVisited = 1")
    List<Place> getVisitedPlacesWithCoordinates();

    @Query("SELECT COUNT(*) FROM places WHERE " +
            "ABS(latitude - :lat) < 0.0001 AND ABS(longitude - :lon) < 0.0001")
    int countByCoordinates(double lat, double lon);

    @Insert
    long insertPlace(Place place);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Place> places);

    @Update
    void updatePlace(Place place);

    @Delete
    void deletePlace(Place place);

    @Query("SELECT * FROM places WHERE ABS(latitude - :lat) < 0.0001 AND ABS(longitude - :lon) < 0.0001 LIMIT 1")
    Place findByCoordinates(double lat, double lon);

    @Query("SELECT * FROM places WHERE name LIKE :name LIMIT 1")
    Place findByName(String name);
    @Query("SELECT * FROM places WHERE firestoreId = :firestoreId LIMIT 1")
    Place findByFirestoreId(String firestoreId);

    @RawQuery(observedEntities = Place.class)
    List<Place> getPlacesRaw(SupportSQLiteQuery query);
}