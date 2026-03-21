package com.example.myapplication.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.data.network.NetworkMonitor;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.detail.IMuseumClick;
import com.example.myapplication.ui.detail.MuseumDetailActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.map.MapActivity;
import com.example.myapplication.ui.settings.SettingsActivity;
import com.example.myapplication.data.model.Museum;
import com.example.myapplication.data.repository.MuseumRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import androidx.core.splashscreen.SplashScreen;
public class MainActivity extends BaseActivity implements IMuseumClick {

    private MuseumAdapter museumAdapter;
    private final ArrayList<Museum> museums = new ArrayList<>();
    //private MuseumRepository repository;

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
        viewModel.museums.observe(this, museumList -> {
            museums.clear();
            museums.addAll(museumList);
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
                        Snackbar.LENGTH_LONG
                );
              //  networkSnackbar.setAnchorView(R.id.menu_navigation);
                networkSnackbar.show();
            } else {
                if (networkSnackbar != null) {
                    networkSnackbar.dismiss();
                }
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
        museumAdapter = new MuseumAdapter(this, museums, this);
        recView.setLayoutManager(new LinearLayoutManager(this));
        recView.setAdapter(museumAdapter);
    }

    private void setupFAB() {
        FloatingActionButton fab = findViewById(R.id.fab_add_museum);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddMuseumActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_map) {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_favorites) {
                Intent intent = new Intent(MainActivity.this, AddMuseumActivity.class);
                startActivity(intent);
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

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.search) {
            Toast.makeText(this, getString(R.string.search), Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onMuseumClick(Museum museum) {
        Intent intent = new Intent(this, MuseumDetailActivity.class);
        intent.putExtra("museum", museum);
        startActivity(intent);
    }

    protected void onResume() {
        super.onResume();
        viewModel.loadMuseums();
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}
