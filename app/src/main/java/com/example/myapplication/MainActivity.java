package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import androidx.core.splashscreen.SplashScreen;
public class MainActivity extends BaseActivity implements IMuseumClick {

    private RecyclerView recView;
    private MuseumAdapter museumAdapter;
    private ArrayList<Museum> museums = new ArrayList<>();
    private MuseumRepository repository;
    private boolean isAppReady = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        splashScreen.setKeepOnScreenCondition(() -> !isAppReady); //сплеш, пока  false

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isAppReady = true;
        }, 2000);

        repository = MuseumRepository.getInstance(this);

        setupToolBar();
        setupBottomNavigation();
        setupRecyclerView();
        setupFAB();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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
        recView = findViewById(R.id.recView);
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

    private void loadMuseums() {
        repository.getAllMuseums(new MuseumRepository.DataCallback<List<Museum>>() {
            @Override
            public void onSuccess(List<Museum> data) {
                if (isDestroyed() || isFinishing()) return;
                museums.clear();
                museums.addAll(data);
                museumAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                if (isDestroyed() || isFinishing()) return;
                Toast.makeText(MainActivity.this,
                        getString(R.string.error_load) + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_map) {
                Toast.makeText(this, getString(R.string.map), Toast.LENGTH_SHORT).show();
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
        loadMuseums();
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}
