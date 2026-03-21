package com.example.myapplication.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.myapplication.data.model.CachedPlace;
import com.example.myapplication.data.model.Museum;

@Database(entities = {Museum.class, CachedPlace.class}, version = 3)
public abstract class MuseumDB extends RoomDatabase {

    private static volatile MuseumDB instance;

    public abstract MuseumDAO museumDAO();
    public abstract CachedPlaceDAO cachedPlaceDAO();

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_places` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`searchQuery` TEXT, " +
                            "`name` TEXT, " +
                            "`address` TEXT, " +
                            "`latitude` REAL NOT NULL, " +
                            "`longitude` REAL NOT NULL, " +
                            "`phone` TEXT, " +
                            "`workingHours` TEXT, " +
                            "`lastUpdated` INTEGER NOT NULL)"
            );
        }
    };

    public static MuseumDB getInstance(Context context) {
        if (instance == null) {
            synchronized (MuseumDB.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MuseumDB.class, "museum_db")
                            .addMigrations(MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance;
    }
}
