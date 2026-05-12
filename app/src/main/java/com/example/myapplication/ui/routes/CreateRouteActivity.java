package com.example.myapplication.ui.routes;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_route);

        placeRepository = PlaceRepository.getInstance(this);

        setupToolbar();
        initViews();
        setupCategorySpinner();
        setupRecyclerView();
        loadSelectedPlaces();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarCreateRoute);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
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

    private void loadPlacesFromDatabase(List<Long> placeIds) {  // ← Long
        Log.e("CREATE_ROUTE_DEBUG", "Looking for placeIds: " + placeIds);

        placeRepository.getAllPlaces(new PlaceRepository.DataCallback<List<Place>>() {
            @Override
            public void onSuccess(List<Place> allPlaces) {
                Log.e("CREATE_ROUTE_DEBUG", "Total places in DB: " + allPlaces.size());

                routePoints.clear();

                int order = 1;
                for (Long placeId : placeIds) {  // ← Long
                    for (Place place : allPlaces) {
                        Log.e("CREATE_ROUTE_DEBUG", "Comparing: placeId=" + placeId
                                + " vs place.getId()=" + place.getId()
                                + " | match=" + (place.getId() == placeId));

                        if (place.getId() == placeId) {  // сравнение long
                            RoutePoint point = new RoutePoint(place.getId(), order);
                            point.setPlace(place);
                            routePoints.add(point);
                            order++;

                            Log.e("CREATE_ROUTE_DEBUG", "FOUND: " + place.getName());

                            break;
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                Log.e("ROUTE_DEBUG", "Error loading places", e);
                Toast.makeText(CreateRouteActivity.this,
                        "Error loading places: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
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

        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("mode", "route_preview");

        // передача
        ArrayList<Double> latitudes = new ArrayList<>();
        ArrayList<Double> longitudes = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();

        for (RoutePoint point : routePoints) {
            if (point.getPlace() != null) {
                latitudes.add(point.getPlace().getLatitude());
                longitudes.add(point.getPlace().getLongitude());
                names.add(point.getPlace().getName());
            }
        }

        intent.putExtra("latitudes", latitudes);
        intent.putExtra("longitudes", longitudes);
        intent.putStringArrayListExtra("names", names);

        startActivity(intent);
    }
    private void saveRoute() {
        String title = editRouteTitle.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editRouteTitle.setError(getString(R.string.error_route_title));
            editRouteTitle.requestFocus();
            return;
        }
        if (routePoints.size() < 2) {
            Toast.makeText(this, R.string.error_route_points, Toast.LENGTH_SHORT).show();
            return;
        }

        String  description = editRouteDescription.getText().toString().trim();
        String  category    = spinnerCategory.getText().toString();
        boolean isPublic    = switchPublic.isChecked();
        String  userId      = SupabaseClient.getInstance().getUserId();

        // routePoints — уже List<RoutePoint> (ui.routes.RoutePoint)
        RouteRepository.getInstance().createRoute(
                userId,
                title,
                description,
                isPublic,
                category,
                routePoints,
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
