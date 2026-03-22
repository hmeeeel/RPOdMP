package com.example.myapplication.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.splashscreen.SplashScreen;
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
import com.example.myapplication.ui.settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements IMuseumClick {

    private MuseumAdapter museumAdapter;
    private final ArrayList<Place> places = new ArrayList<>();

    private MuseumViewModel viewModel;
    private NetworkMonitor networkMonitor;
    private Snackbar networkSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MuseumViewModel.class);

        setupToolBar();
        setupBottomNavigation();
        setupRecyclerView();
        setupFAB();
        observeData();
        setupNetworkMonitoring();
    }

    private void observeData() {
        viewModel.museums.observe(this, placeList -> {
            places.clear();
            places.addAll(placeList);
            museumAdapter.notifyDataSetChanged();
        });

        viewModel.error.observe(this, errorMsg -> {
            if (errorMsg != null) {
                Toast.makeText(this,
                        getString(R.string.error_load) + errorMsg,
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

    private void setupToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupRecyclerView() {
        RecyclerView recView = findViewById(R.id.recView);
        museumAdapter = new MuseumAdapter(this, places, this); // List<Place>
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
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(MainActivity.this, AddMuseumActivity.class));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            Toast.makeText(this, getString(R.string.search), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMuseumClick(Place place) {
        Intent intent = new Intent(this, MuseumDetailActivity.class);
        intent.putExtra("place", place);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadMuseums();
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}