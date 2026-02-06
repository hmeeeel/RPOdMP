package com.example.myapplication;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recView;
    private ArrayList<Museum> museums = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        initializationData();
        recView = findViewById(R.id.recView);
        MuseumAdapter museumAdapter = new MuseumAdapter(this,museums);
        recView.setLayoutManager(new LinearLayoutManager(this));
        recView.setAdapter(museumAdapter);

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
                Toast.makeText(this, "Избранное", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializationData(){
        museums.add(new Museum("Национальный художественный музей", R.drawable.natioanal_hud_museum_1920x1280));
        museums.add(new Museum("2", R.drawable.natioanal_hud_museum_1920x1280));
        museums.add(new Museum("3", R.drawable.natioanal_hud_museum_1920x1280));
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
}