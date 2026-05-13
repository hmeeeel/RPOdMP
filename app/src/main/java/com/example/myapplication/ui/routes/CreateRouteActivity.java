package com.example.myapplication.ui.routes;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.supabase.RouteRepository;
import com.example.myapplication.data.supabase.SupabaseClient;
import com.example.myapplication.data.supabase.SupabaseRepository;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.map.MapActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class CreateRouteActivity extends BaseActivity
        implements RoutePointItemTouchHelper.ItemTouchHelperAdapter {

    private TextInputEditText editRouteTitle, editRouteDescription;
    private AutoCompleteTextView spinnerCategory;
    private SwitchCompat switchPublic;
    private RecyclerView recyclerRoutePoints;
    private LinearLayout emptyStateLayout;
    private MaterialButton btnViewOnMap;

    private RoutePointsAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private List<RoutePoint> routePoints = new ArrayList<>();
    private PlaceRepository placeRepository;

    private boolean isEditMode  = false;
    private String  editRouteId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_route);

        placeRepository = PlaceRepository.getInstance(this);

        isEditMode  = "edit".equals(getIntent().getStringExtra("mode"));
        editRouteId = getIntent().getStringExtra("route_id");

        setupToolbar();
        initViews();
        setupCategorySpinner();
        setupRecyclerView();

        if (isEditMode) {
            // Режим редактирования — загружаем существующие данные
            prefillForEdit();
        } else {
            // Режим создания — загружаем выбранные места
            loadSelectedPlaces();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarCreateRoute);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(
                    isEditMode
                            ? getString(R.string.edit_route)
                            : getString(R.string.create_route)
            );
        }
    }

    //  предзаполнение полей при редактировании
    private void prefillForEdit() {
        // Заполняем текстовые поля из Intent
        String title   = getIntent().getStringExtra("route_title");
        String desc    = getIntent().getStringExtra("route_desc");
        boolean pub    = getIntent().getBooleanExtra("is_public", false);

        if (title != null) editRouteTitle.setText(title);
        if (desc  != null) editRouteDescription.setText(desc);
        switchPublic.setChecked(pub);

        // Загружаем точки маршрута из БД
        if (editRouteId != null) {
            loadRoutePointsForEdit(editRouteId);
        }
    }

    // загрузка точек маршрута для редактирования
    private void loadRoutePointsForEdit(String routeId) {
        RouteRepository.getInstance().getRoutePoints(
                routeId,
                new PlaceRepository.DataCallback<List<RoutePoint>>() {
                    @Override
                    public void onSuccess(List<RoutePoint> data) {
                        routePoints.clear();
                        if (data != null) routePoints.addAll(data);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(CreateRouteActivity.this,
                                "Ошибка загрузки точек: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveRoute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initViews() {
        editRouteTitle = findViewById(R.id.editRouteTitle);
        editRouteDescription = findViewById(R.id.editRouteDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchPublic = findViewById(R.id.switchPublic);
        recyclerRoutePoints = findViewById(R.id.recyclerRoutePoints);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);

        btnViewOnMap.setOnClickListener(v -> openMapPreview());
    }

    private void setupCategorySpinner() {
        String[] categories = getResources().getStringArray(R.array.route_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setText(categories[0], false);
    }

    private void setupRecyclerView() {
        adapter = new RoutePointsAdapter(this, routePoints);

        adapter.setOnItemActionListener(position -> {
            adapter.removeItem(position);
            updateEmptyState();
        });

        adapter.setOnStartDragListener(viewHolder -> {
            if (itemTouchHelper != null) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });

        recyclerRoutePoints.setLayoutManager(new LinearLayoutManager(this));
        recyclerRoutePoints.setAdapter(adapter);

        RoutePointItemTouchHelper callback = new RoutePointItemTouchHelper(this);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerRoutePoints);
    }

    private void loadSelectedPlaces() {
        // В1: Если передавали как Serializable
        ArrayList<Long> placeIds = (ArrayList<Long>) getIntent().getSerializableExtra("selected_place_ids");

        // В2: Если передавали как long[]
        // long[] placeIdsArray = getIntent().getLongArrayExtra("selected_place_ids");

        if (placeIds == null || placeIds.isEmpty()) {
            Toast.makeText(this, R.string.error_route_points, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPlacesFromDatabase(placeIds);
    }

    private void loadPlacesFromDatabase(List<Long> placeIds) {
        Log.e("CREATE_ROUTE_DEBUG", "Looking for placeIds: " + placeIds);

        // Загружаем из Supabase, а не из Room
        SupabaseRepository.getInstance().getAll(new PlaceRepository.DataCallback<List<Place>>() {
            @Override
            public void onSuccess(List<Place> allPlaces) {
                Log.e("CREATE_ROUTE_DEBUG", "Total places in Supabase: " + allPlaces.size());

                routePoints.clear();

                int order = 1;
                for (Long placeId : placeIds) {
                    boolean found = false;
                    for (Place place : allPlaces) {
                        // Сравниваем firestoreId (строка) с placeId (long)
                        String fsId = place.getFirestoreId();
                        if (fsId != null && !fsId.isEmpty()) {
                            try {
                                if (Long.parseLong(fsId) == placeId) {
                                    RoutePoint point = new RoutePoint(placeId, order);
                                    point.setPlace(place);
                                    routePoints.add(point);
                                    order++;
                                    Log.e("CREATE_ROUTE_DEBUG", "FOUND: " + place.getName());
                                    found = true;
                                    break;
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (!found) {
                        Log.e("CREATE_ROUTE_DEBUG", "NOT FOUND in Supabase: placeId=" + placeId);
                    }
                }

                adapter.notifyDataSetChanged();
                updateEmptyState();

                if (routePoints.isEmpty()) {
                    Toast.makeText(CreateRouteActivity.this,
                            "Места с такими ID не найдены в Supabase", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("ROUTE_DEBUG", "Error loading from Supabase", e);
                Toast.makeText(CreateRouteActivity.this,
                        "Ошибка загрузки из Supabase: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        adapter.onItemMove(fromPosition, toPosition);
    }

    private void updateEmptyState() {
        if (routePoints.isEmpty()) {
            recyclerRoutePoints.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            btnViewOnMap.setEnabled(false);
        } else {
            recyclerRoutePoints.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            btnViewOnMap.setEnabled(true);
        }
    }

    private void openMapPreview() {
        if (routePoints.isEmpty()) {
            Toast.makeText(this, R.string.error_route_points, Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Double> latitudes  = new ArrayList<>();
        ArrayList<Double> longitudes = new ArrayList<>();
        ArrayList<String> names      = new ArrayList<>();
        int skipped = 0;

        for (RoutePoint point : routePoints) {
            Place p = point.getPlace();
            if (p != null && p.hasCoordinates()) {
                latitudes.add(p.getLatitude());
                longitudes.add(p.getLongitude());
                names.add(p.getName());
            } else {
                skipped++;
            }
        }

        if (latitudes.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.no_coordinates_title))
                    .setMessage(getString(R.string.no_coordinates_message))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
            return;
        }

        if (skipped > 0) {
            Toast.makeText(this,
                    getString(R.string.some_points_no_coords, skipped),
                    Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("mode", "route_preview");
        intent.putExtra("latitudes",  latitudes);
        intent.putExtra("longitudes", longitudes);
        intent.putStringArrayListExtra("names", names);
        startActivity(intent);
    }
    private void saveRoute() {
        String title = editRouteTitle.getText().toString().trim();

        if (title.isEmpty()) {
            editRouteTitle.setError(getString(R.string.error_route_title));
            editRouteTitle.requestFocus();
            return;
        }
        if (routePoints.size() < 2) {
            Toast.makeText(this, R.string.error_route_points, Toast.LENGTH_SHORT).show();
            return;
        }

        String  description = editRouteDescription.getText().toString().trim();
        boolean isPublic    = switchPublic.isChecked();

        if (isEditMode && editRouteId != null) {
            // РЕЖИМ РЕДАКТИРОВАНИЯ — вызываем UPDATE
            updateExistingRoute(title, description, isPublic);
        } else {
            // РЕЖИМ СОЗДАНИЯ — оставляем как было
            createNewRoute(title, description, isPublic);
        }
    }

    private void updateExistingRoute(String title,
                                     String description,
                                     boolean isPublic) {
        RouteRepository.getInstance().updateRoute(
                editRouteId,
                title,
                description,
                isPublic,
                routePoints,
                new PlaceRepository.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        Toast.makeText(CreateRouteActivity.this,
                                R.string.route_updated,
                                Toast.LENGTH_SHORT).show();

                        // Возвращаем новое название в RouteDetailActivity
                        Intent result = new Intent();
                        result.putExtra("updated_title", title);
                        setResult(RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(CreateRouteActivity.this,
                                "Ошибка обновления: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void createNewRoute(String title,
                                String description,
                                boolean isPublic) {
        String category = spinnerCategory.getText().toString();
        String userId   = SupabaseClient.getInstance().getUserId();

        RouteRepository.getInstance().createRoute(
                userId, title, description, isPublic,
                category, routePoints,
                new PlaceRepository.DataCallback<String>() {
                    @Override
                    public void onSuccess(String routeId) {
                        Toast.makeText(CreateRouteActivity.this,
                                R.string.route_saved, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(CreateRouteActivity.this,
                                "Ошибка сохранения: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
    }
