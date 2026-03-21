package com.example.myapplication.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.data.model.CachedPlace;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import java.util.List;

public class MapActivity extends BaseActivity {

    private static final double DEFAULT_LAT = 53.9000;
    private static final double DEFAULT_LON = 27.5667;

    private MapView mapView;
    private MapViewModel viewModel;
    private MapObjectCollection markersCollection;
    private ImageProvider markerIcon;

    private MapObjectTapListener placemarkTapListener;
    private Snackbar networkSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapview);
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        markersCollection = mapView.getMapWindow().getMap().getMapObjects().addCollection();

        markerIcon = ImageProvider.fromResource(this, R.drawable.placeholder64);

        setupToolbar();
        setupBottomNavigation();
        setupMap();
        observeViewModel();
        observeNetwork();
    }

    private void setupMap() {
        var map = mapView.getMapWindow().getMap();
        map.setMapType(MapType.VECTOR_MAP);
        map.setNightModeEnabled(settingsManager.isDarkTheme());

        // zoom 12.0f — уровень города (17.0f — уровень улицы)
        // azimuth 0.0f — север сверху
        // tilt 0.0f — вид сверху без наклона
        map.move(new CameraPosition(
                new Point(DEFAULT_LAT, DEFAULT_LON),
                12.0f, 0.0f, 0.0f
        ));
    }

    private void observeViewModel() {
        viewModel.places.observe(this, this::updateMarkers);

        viewModel.snackbarMessage.observe(this, key -> {
            if (key == null) return;
            String msg;
            switch (key) {
                case "offline_cache":
                    msg = getString(R.string.offline_cache); break;
                case "cache_updated":
                    msg = getString(R.string.cache_updated); break;
                case "no_data":
                    msg = getString(R.string.no_internet); break;
                default:
                    msg = key;
            }
            Snackbar.make(mapView, msg, Snackbar.LENGTH_SHORT).show();
        });
    }

    private void observeNetwork() {
        viewModel.networkMonitor.observe(this, isOnline -> {
            if (Boolean.FALSE.equals(isOnline)) {
                // нет инета
                networkSnackbar = Snackbar.make(
                        mapView,
                        getString(R.string.no_internet),
                        Snackbar.LENGTH_INDEFINITE
                );
                networkSnackbar.setAnchorView(R.id.menu_navigation);
                networkSnackbar.show();
                viewModel.loadPlaces(DEFAULT_LAT, DEFAULT_LON, false);

            } else if (Boolean.TRUE.equals(isOnline)) {
                // инет
                if (networkSnackbar != null) networkSnackbar.dismiss();
                viewModel.loadPlaces(DEFAULT_LAT, DEFAULT_LON, true);
            }
        });
    }

    private void updateMarkers(List<CachedPlace> places) {
        markersCollection.clear();

        for (CachedPlace place : places) {
            if (place.getLatitude() == 0 && place.getLongitude() == 0) continue;

            PlacemarkMapObject marker = markersCollection.addPlacemark();
            marker.setGeometry(new Point(place.getLatitude(), place.getLongitude()));
            marker.setIcon(markerIcon);

            placemarkTapListener = (mapObject, point) -> {
                String info = place.getName()
                        + (place.getAddress().isEmpty() ? "" : "\n" + place.getAddress())
                        + (place.getWorkingHours().isEmpty() ? "" : "\n" + place.getWorkingHours());
                Snackbar.make(mapView, info, Snackbar.LENGTH_LONG).show();
                return true;
            };

            marker.addTapListener(placemarkTapListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.mapToolbar);
        setSupportActionBar(toolbar);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setSelectedItemId(R.id.nav_map);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { finish(); return true; }
            else if (id == R.id.nav_map) { return true; }
            else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, AddMuseumActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }
}