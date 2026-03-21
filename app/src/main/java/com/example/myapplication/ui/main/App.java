package com.example.myapplication.ui.main;

import android.app.Application;

import com.example.myapplication.data.repository.MuseumRepository;
import com.yandex.mapkit.MapKitFactory;
import com.example.myapplication.BuildConfig;
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY);
        MuseumRepository.getInstance(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        MuseumRepository.getInstance(this).shutdown();
    }
}
