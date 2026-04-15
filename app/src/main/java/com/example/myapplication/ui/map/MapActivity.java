package com.example.myapplication.ui.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;

import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.location.Purpose;
import com.yandex.mapkit.location.SubscriptionSettings;
import com.yandex.mapkit.location.UseInBackground;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends BaseActivity {

    private static final double DEFAULT_LAT = 53.9000;
    private static final double DEFAULT_LON = 27.5667;
    private static final float  DEFAULT_ZOOM = 12.0f;

    private MapView mapView;
    private MapViewModel viewModel;
    private MapObjectCollection markersCollection;

    private ImageProvider iconVisited, iconPlanned, iconApiResult, iconUserLocation;

    private PlacemarkMapObject userLocationMarker;
    private Snackbar networkSnackbar;
    private final List<MapObjectTapListener> tapListeners = new ArrayList<>();

    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean locationReceived = false;

    //  запрос разрешения геолокации
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean fineGranted  = Boolean.TRUE.equals(
                                permissions.get(Manifest.permission.ACCESS_FINE_LOCATION));
                        boolean coarseGranted = Boolean.TRUE.equals(
                                permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                        if (fineGranted || coarseGranted) {
                            startLocationUpdates();
                        } else {
                            // остаёмся на Минске
                            Toast.makeText(this,
                                    getString(R.string.location_permission_denied),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_map);

        mapView      = findViewById(R.id.mapview);
        viewModel    = new ViewModelProvider(this).get(MapViewModel.class);
        markersCollection = mapView.getMapWindow().getMap()
                .getMapObjects().addCollection();

        iconVisited   = ImageProvider.fromResource(this, R.drawable.placeholder32);
        iconPlanned   = ImageProvider.fromResource(this, R.drawable.place_want);
        iconApiResult = ImageProvider.fromResource(this, R.drawable.place_cash);
        iconUserLocation = ImageProvider.fromResource(this, R.drawable.place_cash);

        setupToolbar();
        setupBottomNavigation();
        setupMap();
        observeViewModel();
        observeNetwork();

        locationManager = MapKitFactory.getInstance().createLocationManager();

        requestLocationPermission();
    }


    // 1 запрос разрешения
    private void requestLocationPermission() {
        boolean fineGranted  = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            startLocationUpdates();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    // 2 подписка на обновления
    private void startLocationUpdates() {
        locationListener = new LocationListener() {

            // при получении новой локации
            @Override
            public void onLocationUpdated(Location location) {
                if (locationReceived) return; // центрируем только один раз
                locationReceived = true;

                Point userPoint = location.getPosition();

                if (userLocationMarker == null) {
                    userLocationMarker = markersCollection.addPlacemark();
                    userLocationMarker.setIcon(iconUserLocation);
                    userLocationMarker.setZIndex(100f);
                }

                mapView.getMapWindow().getMap().move(
                        new CameraPosition(userPoint, DEFAULT_ZOOM, 0.0f, 0.0f)
                );

                viewModel.loadPlaces(
                        userPoint.getLatitude(),
                        userPoint.getLongitude(),
                        Boolean.TRUE.equals(viewModel.networkMonitor.getValue())
                );
            }

            // при изменении  локации
            @Override
            public void onLocationStatusUpdated(LocationStatus locationStatus) {
                // LocationStatus.NOT_AVAILABLE — источник недоступен
                // карта уже показывает Минск по умолчанию
            }
        };

        SubscriptionSettings subscriptionSettings = new SubscriptionSettings(
                UseInBackground.DISALLOW,
                Purpose.GENERAL
        );
        locationManager.subscribeForLocationUpdates(
                subscriptionSettings,
                locationListener
        );
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            locationManager.unsubscribe(locationListener);
        }
    }

    private void setupMap() {
        var map = mapView.getMapWindow().getMap();
        map.setMapType(MapType.VECTOR_MAP);
        map.setNightModeEnabled(settingsManager.isDarkTheme());
        map.move(new CameraPosition(
                new Point(DEFAULT_LAT, DEFAULT_LON),
                DEFAULT_ZOOM, 0.0f, 0.0f
        ));
    }

    private void observeViewModel() {
        viewModel.markers.observe(this, this::updateMarkers);

        viewModel.snackbarMessage.observe(this, key -> {
            if (key == null) return;
            String msg;
            switch (key) {
                case "offline_cache": msg = getString(R.string.offline_cache); break;
                case "cache_updated": msg = getString(R.string.cache_updated); break;
                default: msg = getString(R.string.no_internet);
            }
            Snackbar.make(mapView, msg, Snackbar.LENGTH_SHORT).show();
        });
    }

    private void observeNetwork() {
        viewModel.networkMonitor.observe(this, isOnline -> {
            if (Boolean.FALSE.equals(isOnline)) {
                networkSnackbar = Snackbar.make(
                        mapView, getString(R.string.no_internet), Snackbar.LENGTH_INDEFINITE);
                networkSnackbar.setAnchorView(R.id.menu_navigation);
                networkSnackbar.show();
                viewModel.loadPlaces(DEFAULT_LAT, DEFAULT_LON, false);
            } else if (Boolean.TRUE.equals(isOnline)) {
                if (networkSnackbar != null) networkSnackbar.dismiss();
                viewModel.loadPlaces(DEFAULT_LAT, DEFAULT_LON, true);
            }
        });
    }

    private void updateMarkers(List<MapMarker> markers) {
        markersCollection.clear();
        tapListeners.clear();

        for (MapMarker marker : markers) {
            if (marker.getLatitude() == 0 && marker.getLongitude() == 0) continue;

            PlacemarkMapObject placemark = markersCollection.addPlacemark();
            placemark.setGeometry(new Point(marker.getLatitude(), marker.getLongitude()));

            switch (marker.getMarkerType()) {
                case SAVED_VISITED: placemark.setIcon(iconVisited); break;
                case SAVED_PLANNED: placemark.setIcon(iconPlanned); break;
                case API_RESULT:
                default:            placemark.setIcon(iconApiResult); break;
            }

            MapObjectTapListener listener = (mapObject, point) -> {
                Snackbar.make(mapView, marker.getSnackbarText(), Snackbar.LENGTH_LONG).show();
                return true;
            };
            tapListeners.add(listener);
            placemark.addTapListener(listener);
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

    @Override
    protected void onDestroy() {
        stopLocationUpdates(); // освобождаем ресурс
        super.onDestroy();
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
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
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