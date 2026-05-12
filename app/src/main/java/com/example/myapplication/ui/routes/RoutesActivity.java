package com.example.myapplication.ui.routes;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.map.MapActivity;
import com.example.myapplication.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class RoutesActivity extends BaseActivity implements RouteAdapter.OnRouteClickListener {

    private RoutesViewModel viewModel;
    private RouteAdapter    adapter;
    private List<RouteCard> displayedCards = new ArrayList<>();

    private ChipGroup chipGroupFilter;
    private TextView  textEmpty;
    private View      progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);

        viewModel = new ViewModelProvider(this).get(RoutesViewModel.class);

        setupToolbar();
        setupBottomNavigation();
        setupChips();
        setupRecyclerView();
        observeData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.routesToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.routes_menu, menu);
        MenuItem   searchItem = menu.findItem(R.id.search_routes);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_routes_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String q) {
                    searchView.clearFocus(); return true;
                }
                @Override public boolean onQueryTextChange(String q) {
                    viewModel.setSearchQuery(q); return true;
                }
            });
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override public boolean onMenuItemActionExpand(MenuItem i)  { return true; }
                @Override public boolean onMenuItemActionCollapse(MenuItem i) {
                    viewModel.setSearchQuery(""); return true;
                }
            });
        }
        return true;
    }

    private void setupChips() {
        chipGroupFilter = findViewById(R.id.chipGroupRouteFilter);
        textEmpty       = findViewById(R.id.textRoutesEmpty);
        progressBar     = findViewById(R.id.routesProgressBar);

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chipRouteAll)    viewModel.setFilter(RouteFilter.ALL);
            else if (id == R.id.chipRoutePublic) viewModel.setFilter(RouteFilter.PUBLIC);
            else if (id == R.id.chipRouteMy)     viewModel.setFilter(RouteFilter.MY);
            else if (id == R.id.chipRouteSaved)  viewModel.setFilter(RouteFilter.SAVED);
        });
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.recyclerRoutes);
        adapter = new RouteAdapter(this, displayedCards, viewModel.getCurrentUserId(), this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void observeData() {


        viewModel.isLoading.observe(this, loading -> {
            progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.routes.observe(this, cards -> {
            displayedCards.clear();
            if (cards != null) displayedCards.addAll(cards);
            adapter.notifyDataSetChanged();
            textEmpty.setVisibility(displayedCards.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });


        viewModel.event.observe(this, evt -> {
            if (evt != null && evt.startsWith("show_progress_dialog:")) {
                Toast.makeText(this, R.string.route_saved_to_favorites, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRouteClick(RouteCard card) {
        Intent intent = new Intent(this, RouteDetailActivity.class);
        intent.putExtra("route_card", card);
        startActivity(intent);
    }

    @Override
    public void onSaveClick(RouteCard card) {
        viewModel.toggleSaveRoute(card.getId());
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.menu_navigation);
        nav.setSelectedItemId(R.id.nav_routes);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                return true;
            } else if (id == R.id.nav_routes) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView nav = findViewById(R.id.menu_navigation);
        if (nav != null) nav.setSelectedItemId(R.id.nav_routes);
    }
}