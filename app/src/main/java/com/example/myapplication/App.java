package com.example.myapplication;

import android.app.Application;

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
