package com.example.myapplication.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.myapplication.data.model.CachedPlace;
import com.example.myapplication.data.model.Place;

@Database(entities = {Place.class, CachedPlace.class}, version = 12)
@TypeConverters(Converters.class)
public abstract class MuseumDB extends RoomDatabase {
    private static volatile MuseumDB instance;

    public abstract PlaceDAO placeDAO();
    public abstract CachedPlaceDAO cachedPlaceDAO();

    public static MuseumDB getInstance(Context context) {
        if (instance == null) {
            synchronized (MuseumDB.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MuseumDB.class,
                                    "museum_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}