package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

public class MuseumDetailActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        setupToolbar();

        ViewPager2 slider = findViewById(R.id.detailSlider);
        TextView name = findViewById(R.id.detailName);
        TextView description = findViewById(R.id.detailDescription);
        TextView phone = findViewById(R.id.detailPhone);
        TextView website = findViewById(R.id.detailWebsite);

        Museum museum = getIntent().getParcelableExtra("museum");

        if (museum != null){
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(museum.getImageIds());
            slider.setAdapter(sliderAdapter);
            name.setText(museum.getName());
            description.setText(museum.getDescriprion());
            phone.setText("Телефон: " + museum.getPhone());
            website.setText("Сайт: " + museum.getWebsite());
        }
    }
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
