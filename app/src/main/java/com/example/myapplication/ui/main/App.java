package com.example.myapplication.ui.main;

import android.app.Application;

import com.example.myapplication.data.repository.MuseumRepository;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MuseumRepository.getInstance(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        MuseumRepository.getInstance(this).shutdown();
    }
}
