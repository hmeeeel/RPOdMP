package com.example.myapplication.ui.detail;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.main.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class MuseumDetailActivity extends BaseActivity {

    private Place place;
    private PlaceRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        repository = PlaceRepository.getInstance(this);
        setupToolbar();

        place = getIntent().getParcelableExtra("place");
        if (place == null) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
    }

    private void bindViews() {
        ViewPager2 slider = findViewById(R.id.detailSlider);
        ArrayList<String> images = place.getImageIds();
        if (images == null || images.isEmpty()) {
            images = new ArrayList<>();
            images.add("no_image");
        }
        slider.setAdapter(new ImageSliderAdapter(this, images));

        TextView name = findViewById(R.id.detailName);
        name.setText(place.getName());

        showOrHide(R.id.detailAddress,
                place.getAddress(),
                getString(R.string.address_label) + " " + place.getAddress());

        if (place.hasCoordinates()) {
            TextView coordView = findViewById(R.id.detailCoordinates);
            coordView.setVisibility(View.VISIBLE);
            coordView.setText(getString(R.string.coordinates_label) + " " + place.getCoordinatesDisplay());
        } else {
            findViewById(R.id.detailCoordinates).setVisibility(View.GONE);
        }

        showOrHide(R.id.detailPhone,
                place.getPhone(),
                getString(R.string.phone_label) + " " + place.getPhone());

        showOrHide(R.id.detailWebsite,
                place.getWebsite(),
                getString(R.string.website_label) + " " + place.getWebsite());

        showOrHide(R.id.detailWorkingHours,
                place.getWorkingHours(),
                getString(R.string.working_hours_label) + " " + place.getWorkingHours());

        TextView statusView = findViewById(R.id.detailStatus);
        if (place.isVisited()) {
            statusView.setText(R.string.status_visited);
            statusView.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            statusView.setText(R.string.status_planned);
            statusView.setTextColor(getColor(android.R.color.darker_gray));
        }

        TextView visitDateView = findViewById(R.id.detailVisitDate);
        if (place.isVisited() && place.getVisitDate() > 0) {
            visitDateView.setVisibility(View.VISIBLE);
            String dateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(new Date(place.getVisitDate()));
            visitDateView.setText(getString(R.string.visit_date_label) + " " + dateStr);
        } else {
            visitDateView.setVisibility(View.GONE);
        }

        showOrHide(R.id.detailDescription,
                place.getDescription(),
                place.getDescription());
    }

    private void showOrHide(int viewId, String value, String text) {
        TextView view = findViewById(viewId);
        if (value != null && !value.isEmpty()) {
            view.setVisibility(View.VISIBLE);
            view.setText(text);
        } else {
            view.setVisibility(View.GONE);
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            editPlace();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editPlace() {
        Intent intent = new Intent(this, AddMuseumActivity.class);
        intent.putExtra("place", place); // ИЗМЕНЕНО: ключ "place"
        startActivity(intent);
        finish();
    }

    private void showDeleteConfirmation() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_dialog))
                .setMessage(getString(R.string.delete_dialog_message, place.getName()))
                .setPositiveButton(getString(R.string.delete), (d, which) -> deletePlace())
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            int color = settingsManager.isDarkTheme() ?
                    ContextCompat.getColor(this, R.color.light) :
                    ContextCompat.getColor(this, R.color.dark);
            positiveButton.setTextColor(color);
        }

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            int color = settingsManager.isDarkTheme() ?
                    ContextCompat.getColor(this, R.color.light) :
                    ContextCompat.getColor(this, R.color.dark);
            negativeButton.setTextColor(color);
        }
    }

    private void deletePlace() {
        repository.deletePlace(place, new PlaceRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(MuseumDetailActivity.this,
                        getString(R.string.museum_deleted), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MuseumDetailActivity.this,
                        getString(R.string.error_delete) + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}