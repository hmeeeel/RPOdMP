package com.example.myapplication.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.network.NetworkMonitor;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.detail.IMuseumClick;
import com.example.myapplication.ui.detail.MuseumDetailActivity;
import com.example.myapplication.ui.map.MapActivity;
import com.example.myapplication.ui.routes.CreateRouteActivity;
import com.example.myapplication.ui.routes.RoutesActivity;
import com.example.myapplication.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends BaseActivity implements IMuseumClick {

    private MuseumAdapter museumAdapter;
    private final ArrayList<Place> places = new ArrayList<>();

    private MuseumViewModel viewModel;
    private NetworkMonitor networkMonitor;
    private Snackbar networkSnackbar;

    private ChipGroup chipGroupSort, chipGroupFilter;
    private Chip chipNewest, chipOldest, chipAZ, chipZA, chipAll, chipVisited, chipPlanned;

    private TextView textEmpty;
    private boolean updatingChips = false;
    private boolean isSelectionMode = false;
    private Menu currentMenu;
    private Toolbar toolbar;
    private ActivityResultLauncher<Intent> createRouteLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MuseumViewModel.class);

        setupToolBar();
        setupBottomNavigation();
        setupRecyclerView();
        setupFAB();
        setupChips();
        observeData();
        setupNetworkMonitoring();

        createRouteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == RESULT_OK) {
                        exitSelectionMode(); // очистка когда маршрут сохр
                    }

                    // Если RESULT_CANCELED - выбор сохраняется
                }
        );
    }

    private void setupToolBar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        currentMenu = menu;

        if (isSelectionMode) {
            getMenuInflater().inflate(R.menu.menu_selection, menu);
            updateSelectionTitle(museumAdapter.getSelectedPlaceIds().size());
            return true;
        }

        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setMaxWidth(Integer.MAX_VALUE);

            String savedQuery = viewModel.getCurrentQuery();
            if (savedQuery != null && !savedQuery.isEmpty()) {
                searchItem.expandActionView();
                searchView.setQuery(savedQuery, false);
                searchView.clearFocus();
            }

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    viewModel.setQuery(newText);
                    return true;
                }
            });

            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    viewModel.setQuery("");
                    return true;
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            exitSelectionMode();
            return true;
        } else if (id == R.id.action_create_route) {
            createRouteFromSelection();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        museumAdapter.clearSelectionSilently();
        invalidateOptionsMenu();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("");
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        invalidateOptionsMenu();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_24dp);
        }
    }

    private void updateSelectionTitle(int count) {
        if (getSupportActionBar() != null && isSelectionMode) {
            getSupportActionBar().setTitle(getString(R.string.selected_count, count));
        }
    }

    private void createRouteFromSelection() {
        Set<Long> selectedIds = museumAdapter.getSelectedPlaceIds();

        Log.e("DEBUG_SELECT", "=====================================");
        Log.e("DEBUG_SELECT", "Selection mode: " + museumAdapter.isSelectionMode());
        Log.e("DEBUG_SELECT", "Selected IDs size: " + selectedIds.size());

        if (selectedIds.isEmpty()) {
            Log.e("DEBUG_SELECT", "No places selected at all!");
            Toast.makeText(this, "Вы не выбрали ни одного места! Долгое нажатие для выбора", Toast.LENGTH_LONG).show();
            return;
        }

        int i = 0;
        for (Long id : selectedIds) {
            Log.e("DEBUG_SELECT", "Selected[" + i++ + "]: " + id);
        }

        if (selectedIds.size() < 2) {
            Log.e("DEBUG_SELECT", "Only " + selectedIds.size() + " place(s) selected, need 2+");
            Toast.makeText(this, "Выбрано " + selectedIds.size() + " мест. Нужно минимум 2", Toast.LENGTH_LONG).show();
            return;
        }

        Log.e("DEBUG_SELECT", "SUCCESS! Opening CreateRouteActivity...");

        Intent intent = new Intent(this, CreateRouteActivity.class);
        ArrayList<Long> idsList = new ArrayList<>(selectedIds);
        intent.putExtra("selected_place_ids", idsList);
       // startActivity(intent);
       // exitSelectionMode();

        createRouteLauncher.launch(intent);
    }
    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    private void setupChips() {
        chipGroupSort   = findViewById(R.id.chipGroupSort);
        chipNewest      = findViewById(R.id.chipNewest);
        chipOldest      = findViewById(R.id.chipOldest);
        chipAZ          = findViewById(R.id.chipAZ);
        chipZA          = findViewById(R.id.chipZA);

        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        chipAll         = findViewById(R.id.chipAll);
        chipVisited     = findViewById(R.id.chipVisited);
        chipPlanned     = findViewById(R.id.chipPlanned);

        textEmpty = findViewById(R.id.textEmpty);

        applySortToChips(viewModel.getCurrentSort());
        applyFilterToChips(viewModel.getCurrentFilter());

        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (updatingChips) return;
            if (checkedIds.isEmpty()) return;

            int id = checkedIds.get(0);
            if      (id == R.id.chipNewest) viewModel.setSort(MuseumViewModel.SortOption.NEWEST);
            else if (id == R.id.chipOldest) viewModel.setSort(MuseumViewModel.SortOption.OLDEST);
            else if (id == R.id.chipAZ)     viewModel.setSort(MuseumViewModel.SortOption.AZ);
            else if (id == R.id.chipZA)     viewModel.setSort(MuseumViewModel.SortOption.ZA);
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (updatingChips) return;
            if (checkedIds.isEmpty()) return;

            int id = checkedIds.get(0);
            if      (id == R.id.chipAll)     viewModel.setFilter(MuseumViewModel.FilterOption.ALL);
            else if (id == R.id.chipVisited) viewModel.setFilter(MuseumViewModel.FilterOption.VISITED);
            else if (id == R.id.chipPlanned) viewModel.setFilter(MuseumViewModel.FilterOption.PLANNED);
        });
    }

    private void applySortToChips(MuseumViewModel.SortOption sort) {
        updatingChips = true;
        chipNewest.setChecked(sort == MuseumViewModel.SortOption.NEWEST);
        chipOldest.setChecked(sort == MuseumViewModel.SortOption.OLDEST);
        chipAZ.setChecked(sort == MuseumViewModel.SortOption.AZ);
        chipZA.setChecked(sort == MuseumViewModel.SortOption.ZA);
        updatingChips = false;
    }

    private void applyFilterToChips(MuseumViewModel.FilterOption filter) {
        updatingChips = true;
        chipAll.setChecked(filter == MuseumViewModel.FilterOption.ALL);
        chipVisited.setChecked(filter == MuseumViewModel.FilterOption.VISITED);
        chipPlanned.setChecked(filter == MuseumViewModel.FilterOption.PLANNED);
        updatingChips = false;
    }

    private void observeData() {
        viewModel.museums.observe(this, placeList -> {
            places.clear();
            places.addAll(placeList);
            museumAdapter.notifyDataSetChanged();

            boolean isEmpty = placeList.isEmpty();
            textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(this, errorMsg -> {
            if (errorMsg != null) {
                Toast.makeText(this, getString(R.string.error_load) + errorMsg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNetworkMonitoring() {
        networkMonitor = new NetworkMonitor(this);
        networkMonitor.observe(this, isOnline -> {
            if (Boolean.FALSE.equals(isOnline)) {
                networkSnackbar = Snackbar.make(
                        findViewById(R.id.main),
                        getString(R.string.no_internet),
                        Snackbar.LENGTH_LONG);
                networkSnackbar.show();
            } else {
                if (networkSnackbar != null) networkSnackbar.dismiss();
            }
        });
    }

    private void setupRecyclerView() {
        RecyclerView recView = findViewById(R.id.recView);
        museumAdapter = new MuseumAdapter(this, places, this);

        museumAdapter.setSelectionModeListener(new MuseumAdapter.SelectionModeListener() {
            @Override
            public void onSelectionChanged(int count) {
                updateSelectionTitle(count);
                if (currentMenu != null) {
                    MenuItem createItem = currentMenu.findItem(R.id.action_create_route);
                    if (createItem != null) {
                        createItem.setEnabled(count >= 2);
                        createItem.getIcon().setAlpha(count >= 2 ? 255 : 128);
                    }
                }
            }

            @Override
            public void onSelectionModeToggled(boolean enabled) {
                if (enabled) {
                    enterSelectionMode();
                } else {
                    exitSelectionMode(); // теперь вызывает clearSelectionSilently()
                }
            }
        });

        recView.setLayoutManager(new LinearLayoutManager(this));
        recView.setAdapter(museumAdapter);
    }

    private void setupFAB() {
        FloatingActionButton fab = findViewById(R.id.fab_add_museum);
        fab.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddMuseumActivity.class)));
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(MainActivity.this, MapActivity.class));
                return true;
            } else if (id == R.id.nav_routes) {
                //startActivity(new Intent(MainActivity.this, AddMuseumActivity.class));
                startActivity(new Intent(this, RoutesActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onMuseumClick(Place place) {
        if (isSelectionMode) return;

        Intent intent = new Intent(this, MuseumDetailActivity.class);
        intent.putExtra("place", place);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadMuseums();

        applySortToChips(viewModel.getCurrentSort());
        applyFilterToChips(viewModel.getCurrentFilter());

        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}