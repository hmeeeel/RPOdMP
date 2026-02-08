package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements IMuseumClick{

    private RecyclerView recView;
    private ArrayList<Museum> museums = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setupToolBar();
        setupBottomNavigation();

        initializationMuseum();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupToolBar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
    private void initializationMuseum(){
        initializationData();
        recView = findViewById(R.id.recView);
        MuseumAdapter museumAdapter = new MuseumAdapter(this,museums, this);
        recView.setLayoutManager(new LinearLayoutManager(this));
        recView.setAdapter(museumAdapter);
    }
    private void setupBottomNavigation(){
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Toast.makeText(this, "Главная", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_map) {
                Toast.makeText(this, "Карта", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_favorites) {
                Toast.makeText(this, "Добавить", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                return true;
            }

            return false;
        });

    }
    private void initializationData(){
        ArrayList<Integer> images1 = new ArrayList<>();
        images1.add(R.drawable.natioanal_hud_museum_1920x1280);
        images1.add(R.drawable.natioanal_hud_museum_1920x1280);
        images1.add(R.drawable.natioanal_hud_museum_1920x1280);
        museums.add(new Museum("Национальный художественный музей",
                images1,
                "Национальный исторический музей Республики Беларусь, до 15 сентября 2009 — Национальный музей истории и культуры Беларуси — крупнейший по числу единиц хранения музей Республики Беларусь.",
                "8 017 327-36-65",
                "histmuseum.by"));
        ArrayList<Integer> images2 = new ArrayList<>();
        images2.add(R.drawable.natioanal_hud_museum_1920x1280);
        museums.add(new Museum("2", images2));
        ArrayList<Integer> images3 = new ArrayList<>();
        images3.add(R.drawable.natioanal_hud_museum_1920x1280);
        museums.add(new Museum("3", images3));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.search) {
            Toast.makeText(this, "Поиск", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onMuseumClick(Museum museum){
        Intent intent = new Intent(this, MuseumDetailActivity.class);
        intent.putExtra("museum", museum);
        startActivity(intent);
    }
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.menu_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}

