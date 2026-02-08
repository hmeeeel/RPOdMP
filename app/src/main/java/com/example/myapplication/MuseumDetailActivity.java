package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MuseumDetailActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ImageView imageId = findViewById(R.id.detailImage);
        TextView name = findViewById(R.id.detailName);
        TextView description = findViewById(R.id.detailDescription);
        TextView phone = findViewById(R.id.detailPhone);
        TextView website = findViewById(R.id.detailWebsite);

        Museum museum = getIntent().getParcelableExtra("museum");

        if (museum != null){
        imageId.setImageResource(museum.getImageId());
        name.setText(museum.getName());
        description.setText(museum.getDescriprion());
        phone.setText("Телефон: " + museum.getPhone());
        website.setText("Сайт: " + museum.getWebsite());
        }
    }
}
