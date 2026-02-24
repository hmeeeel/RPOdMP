package com.example.myapplication;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
@Database(entities = {Museum.class}, version = 2)
public abstract class MuseumDB extends RoomDatabase {
    private static volatile MuseumDB instance;
    public abstract MuseumDAO museumDAO();
    public static MuseumDB getInstance(Context context) {
        if (instance == null) {
            synchronized (MuseumDB.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MuseumDB.class, "museum_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
