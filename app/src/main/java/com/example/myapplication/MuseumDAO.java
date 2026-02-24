package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MuseumDAO {
    @Query("SELECT * FROM museums ORDER BY id DESC")
    List<Museum> getAllMuseums();

    @Query("SELECT * FROM museums WHERE id = :museumId")
    Museum getMuseumById(int museumId);

    @Insert
    long insertMuseum(Museum museum);

    @Update
    void updateMuseum(Museum museum);

    @Delete
    void deleteMuseum(Museum museum);
}
