package com.example.myapplication.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

public class MapActivity extends BaseActivity {

    private MapView mapView;

    //  MapKit держит только слабые ссылки
    private MapObjectTapListener placemarkTapListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapKitFactory.initialize(this);

        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapview);

        setupToolbar();
        setupBottomNavigation();
        setupMap();
    }

    private void setupMap() {
        var map = mapView.getMapWindow().getMap();
      //  map.setNightModeEnabled(true);
        map.setMapType(MapType.VECTOR_MAP);
        // zoom 12.0f — уровень города (17.0f — уровень улицы)
        // azimuth 0.0f — север сверху
        // tilt 0.0f — вид сверху без наклона
        mapView.getMapWindow().getMap().move(
                new CameraPosition(
                        new Point(53.89995033440668, 27.555852963255198),
                        12.0f,
                        0.0f,
                        0.0f
                )
        );

        addMuseumPlacemark();
    }

    private void addMuseumPlacemark() {
        ImageProvider imageProvider = ImageProvider.fromResource(
                this, R.drawable.placeholder64
        );

        var placemark = mapView.getMapWindow().getMap()
                .getMapObjects()
                .addPlacemark();

        placemark.setGeometry(new Point(53.89928882920487, 27.56084839352829));
        placemark.setIcon(imageProvider);

        placemarkTapListener = (mapObject, point) -> {
            Toast.makeText(
                    MapActivity.this,
                    "1\n" + point.getLatitude() + ", " + point.getLongitude(),
                    Toast.LENGTH_SHORT
            ).show();
            return true; // true = событие обработано, не передавать дальше
        };

        placemark.addTapListener(placemarkTapListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart(); //!!!
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        mapView.onStop(); // обрт
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
            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                return true;
            } else if (id == R.id.nav_favorites) {
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
