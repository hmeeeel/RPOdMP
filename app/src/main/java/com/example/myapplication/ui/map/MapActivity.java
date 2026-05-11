package com.example.myapplication.ui.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.location.Purpose;
import com.yandex.mapkit.location.SubscriptionSettings;
import com.yandex.mapkit.location.UseInBackground;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends BaseActivity implements InputListener {

    private static final double DEFAULT_LAT  = 53.9000;
    private static final double DEFAULT_LON  = 27.5667;
    private static final float  DEFAULT_ZOOM = 12.0f;

    private MapView           mapView;
    private MapViewModel      viewModel;
    private MapObjectCollection markersCollection, userMarkerCollection,
            temporaryMarkerCollection, routePreviewCollection;

    private ImageProvider iconVisited, iconPlanned, iconApiResult,
            iconUserLocation, iconTemporary;
    private PlacemarkMapObject userLocationMarker, temporaryMarker;
    private Snackbar networkSnackbar, addPlaceSnackbar;
    private final List<MapObjectTapListener> tapListeners = new ArrayList<>();

    private LocationManager  locationManager;
    private LocationListener locationListener;
    private boolean locationReceived = false;

    private boolean isRoutePreviewMode = false;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean fine   = Boolean.TRUE.equals(
                                permissions.get(Manifest.permission.ACCESS_FINE_LOCATION));
                        boolean coarse = Boolean.TRUE.equals(
                                permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                        if (fine || coarse) startLocationUpdates();
                        else Toast.makeText(this,
                                getString(R.string.location_permission_denied),
                                Toast.LENGTH_SHORT).show();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_map);

        mapView   = findViewById(R.id.mapview);
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        Map map = mapView.getMapWindow().getMap();
        markersCollection         = map.getMapObjects().addCollection();
        userMarkerCollection      = map.getMapObjects().addCollection();
        temporaryMarkerCollection = map.getMapObjects().addCollection();
        routePreviewCollection    = map.getMapObjects().addCollection();

        iconVisited      = ImageProvider.fromResource(this, R.drawable.placeholder32);
        iconPlanned      = ImageProvider.fromResource(this, R.drawable.place_want);
        iconApiResult    = ImageProvider.fromResource(this, R.drawable.place_cash);
        iconUserLocation = ImageProvider.fromResource(this, R.drawable.my_location);
        iconTemporary    = ImageProvider.fromResource(this, R.drawable.pin);

        setupToolbar();
        setupMap();

        // ВЫБОР РЕЖИМА
        String mode = getIntent().getStringExtra("mode");
        isRoutePreviewMode = "route_preview".equals(mode);

        if (isRoutePreviewMode) {
            setupRoutePreviewMode();
        } else {
            setupBottomNavigation();
            observeViewModel();
            observeNetwork();
            locationManager = MapKitFactory.getInstance().createLocationManager();
            requestLocationPermission();
        }
    }


    // ПРОСМОТР МАРШРУТА
    private void setupRoutePreviewMode() {
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        if (bottomNav != null) bottomNav.setVisibility(android.view.View.GONE);

        // Получаем данные из Intent
        ArrayList<Double> latitudes  = (ArrayList<Double>) getIntent().getSerializableExtra("latitudes");
        ArrayList<Double> longitudes = (ArrayList<Double>) getIntent().getSerializableExtra("longitudes");
        ArrayList<String> names      = getIntent().getStringArrayListExtra("names");

        if (latitudes == null || latitudes.isEmpty()) {
            Toast.makeText(this, "Нет точек для отображения", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Строим список точек Яндекс MapKit
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < latitudes.size(); i++) {
            points.add(new Point(latitudes.get(i), longitudes.get(i)));
        }

        drawRouteOnMap(points, names != null ? names : new ArrayList<>());
    }

    private void drawRouteOnMap(List<Point> points, List<String> names) {
        if (points.isEmpty()) return;

        routePreviewCollection.clear();

        // 1. Линия маршрута
        if (points.size() > 1) {
            PolylineMapObject polyline = routePreviewCollection
                    .addPolyline(new Polyline(points));
            polyline.setStrokeColor(Color.parseColor("#4285F4"));
            polyline.setStrokeWidth(4.0f);
            polyline.setOutlineColor(Color.WHITE);
            polyline.setOutlineWidth(1.5f);
        }

        // 2. Нумерованные маркеры
        for (int i = 0; i < points.size(); i++) {
            PlacemarkMapObject marker = routePreviewCollection.addPlacemark();
            marker.setGeometry(points.get(i));
            marker.setIcon(createNumberedIcon(i + 1));
            marker.setZIndex(10f + i);

            // Снэкбар с названием при тапе
            final String name = (i < names.size()) ? names.get(i) : ("Точка " + (i + 1));
            final int order   = i + 1;
            MapObjectTapListener listener = (mapObject, point) -> {
                Snackbar.make(mapView,
                        order + ". " + name,
                        Snackbar.LENGTH_SHORT).show();
                return true;
            };
            tapListeners.add(listener);
            marker.addTapListener(listener);
        }

        moveCameraToFitRoute(points);
    }


    // белый круг и цифра внутри. синий фон белая цифра
    private ImageProvider createNumberedIcon(int number) {
        int sizePx  = dpToPx(48);
        int textPx  = dpToPx(18);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(60, 0, 0, 0));
        float cx = sizePx / 2f;
        float cy = sizePx / 2f;
        canvas.drawCircle(cx, cy + dpToPx(2), sizePx / 2f - dpToPx(2), shadowPaint);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#4285F4"));
        canvas.drawCircle(cx, cy, sizePx / 2f - dpToPx(3), circlePaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(2));
        canvas.drawCircle(cx, cy, sizePx / 2f - dpToPx(3), borderPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textPx);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        String text = String.valueOf(number);
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        float textY = cy + bounds.height() / 2f - bounds.bottom;
        canvas.drawText(text, cx, textY, textPaint);

        return ImageProvider.fromBitmap(bitmap);
    }

    private void moveCameraToFitRoute(List<Point> points) {
        if (points.isEmpty()) return;

        if (points.size() == 1) {
            mapView.getMapWindow().getMap().move(
                    new CameraPosition(points.get(0), 15.0f, 0f, 0f));
            return;
        }

        double minLat = points.get(0).getLatitude();
        double maxLat = points.get(0).getLatitude();
        double minLon = points.get(0).getLongitude();
        double maxLon = points.get(0).getLongitude();

        for (Point p : points) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }


        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;

        double latSpan = maxLat - minLat;
        double lonSpan = maxLon - minLon;
        double span    = Math.max(latSpan, lonSpan);

        float zoom;
        if      (span > 1.0)  zoom = 9.0f;
        else if (span > 0.5)  zoom = 10.0f;
        else if (span > 0.1)  zoom = 12.0f;
        else if (span > 0.05) zoom = 13.0f;
        else if (span > 0.01) zoom = 14.0f;
        else                  zoom = 15.0f;

        mapView.getMapWindow().getMap().move(
                new CameraPosition(new Point(centerLat, centerLon), zoom, 0f, 0f));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


    private void handleLongTap(Point point) {
        if (isRoutePreviewMode) return;

        removeTemporaryMarker();
        temporaryMarker = temporaryMarkerCollection.addPlacemark();
        temporaryMarker.setGeometry(point);
        temporaryMarker.setIcon(iconTemporary);
        temporaryMarker.setZIndex(50f);
        showAddPlaceSnackbar(point);
    }

    private void showAddPlaceSnackbar(Point point) {
        if (addPlaceSnackbar != null && addPlaceSnackbar.isShown()) {
            addPlaceSnackbar.dismiss();
        }
        addPlaceSnackbar = Snackbar.make(
                mapView, getString(R.string.add_place_question), Snackbar.LENGTH_LONG);
        addPlaceSnackbar.setAction(getString(R.string.add), v -> {
            openAddMuseumActivity(point.getLatitude(), point.getLongitude());
            removeTemporaryMarker();
        });
        addPlaceSnackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar bar, int event) {
                if (event != DISMISS_EVENT_ACTION) removeTemporaryMarker();
            }
        });
        addPlaceSnackbar.setAnchorView(R.id.menu_navigation);
        addPlaceSnackbar.show();
    }

    private void openAddMuseumActivity(double lat, double lon) {
        Intent intent = new Intent(this, AddMuseumActivity.class);
        intent.putExtra("latitude",  lat);
        intent.putExtra("longitude", lon);
        intent.putExtra("fromMap",   true);
        startActivity(intent);
    }

    private void removeTemporaryMarker() {
        if (temporaryMarker != null) {
            temporaryMarkerCollection.remove(temporaryMarker);
            temporaryMarker = null;
        }
    }

    private void requestLocationPermission() {
        boolean fine   = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) startLocationUpdates();
        else locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    private void startLocationUpdates() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationUpdated(Location location) {
                Point userPoint = location.getPosition();
                if (userLocationMarker == null) {
                    userLocationMarker = userMarkerCollection.addPlacemark();
                    userLocationMarker.setIcon(iconUserLocation);
                    userLocationMarker.setZIndex(100f);
                }
                userLocationMarker.setGeometry(userPoint);

                if (!locationReceived) {
                    locationReceived = true;
                    mapView.getMapWindow().getMap().move(
                            new CameraPosition(userPoint, DEFAULT_ZOOM, 0f, 0f));
                    viewModel.loadPlaces(
                            userPoint.getLatitude(), userPoint.getLongitude(),
                            Boolean.TRUE.equals(viewModel.networkMonitor.getValue()));
                }
            }

            @Override
            public void onLocationStatusUpdated(LocationStatus status) {
                if (status == LocationStatus.NOT_AVAILABLE && !locationReceived) {
                    Toast.makeText(MapActivity.this,
                            "Геолокация недоступна", Toast.LENGTH_SHORT).show();
                }
            }
        };

        locationManager.subscribeForLocationUpdates(
                new SubscriptionSettings(UseInBackground.DISALLOW, Purpose.GENERAL),
                locationListener);
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            locationManager.unsubscribe(locationListener);
        }
    }

    private void setupMap() {
        Map map = mapView.getMapWindow().getMap();
        map.setMapType(com.yandex.mapkit.map.MapType.VECTOR_MAP);
        map.setNightModeEnabled(settingsManager.isDarkTheme());
        map.move(new CameraPosition(
                new Point(DEFAULT_LAT, DEFAULT_LON), DEFAULT_ZOOM, 0f, 0f));
        map.addInputListener(this);
    }

    private void observeViewModel() {
        viewModel.markers.observe(this, this::updateMarkers);
        viewModel.snackbarMessage.observe(this, key -> {
            if (key == null) return;
            String msg;
            switch (key) {
                case "offline_cache": msg = getString(R.string.offline_cache); break;
                case "cache_updated": msg = getString(R.string.cache_updated); break;
                default:              msg = getString(R.string.no_internet);
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
                case SAVED_VISITED: placemark.setIcon(iconVisited);   break;
                case SAVED_PLANNED: placemark.setIcon(iconPlanned);   break;
                default:            placemark.setIcon(iconApiResult); break;
            }
            MapObjectTapListener listener = (obj, point) -> {
                Snackbar.make(mapView, marker.getSnackbarText(), Snackbar.LENGTH_LONG).show();
                return true;
            };
            tapListeners.add(listener);
            placemark.addTapListener(listener);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.mapToolbar);
        setSupportActionBar(toolbar);
        if (isRoutePreviewMode && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Предпросмотр маршрута");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
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


    @Override protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.getMapWindow().getMap().removeInputListener(this);
        stopLocationUpdates();
        if (markersCollection         != null) markersCollection.clear();
        if (userMarkerCollection      != null) userMarkerCollection.clear();
        if (temporaryMarkerCollection != null) temporaryMarkerCollection.clear();
        if (routePreviewCollection    != null) routePreviewCollection.clear();
        tapListeners.clear();
        userLocationMarker = null;
        locationListener   = null;
        removeTemporaryMarker();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isRoutePreviewMode) {
            removeTemporaryMarker();
            if (addPlaceSnackbar != null && addPlaceSnackbar.isShown()) {
                addPlaceSnackbar.dismiss();
            }
        }
    }

    @Override
    public void onMapTap(@NonNull Map map, @NonNull Point point) {
        if (isRoutePreviewMode) return;
        if (temporaryMarker != null) removeTemporaryMarker();
        if (addPlaceSnackbar != null && addPlaceSnackbar.isShown()) addPlaceSnackbar.dismiss();
    }

    @Override
    public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
        if (isRoutePreviewMode) return;
        handleLongTap(point);
    }
}