package com.example.myapplication.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myapplication.data.model.CachedPlace;

import java.util.List;

@Dao
public interface CachedPlaceDAO {

    //кеш
    @Query("SELECT * FROM cached_places WHERE searchQuery = :query ORDER BY name ASC")
    List<CachedPlace> getCachedPlaces(String query);

    // Вернёт 0 если кеша нет. время обн
    @Query("SELECT COALESCE(MAX(lastUpdated), 0) FROM cached_places WHERE searchQuery = :query")
    long getLastUpdateTime(String query);

    // от апи
    @Insert
    void insertAll(List<CachedPlace> places);

    @Query("DELETE FROM cached_places WHERE searchQuery = :query")
    void deleteByQuery(String query);

    @Query("DELETE FROM cached_places WHERE lastUpdated < :threshold")
    void deleteExpiredCache(long threshold);
}
