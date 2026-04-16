package com.example.myapplication.ui.detail;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;           // ShareCompat из androidx.core
import androidx.core.content.FileProvider;       // FileProvider из androidx.core
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.data.firestore.FirestoreRepository;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.serviceImage.ImageStorageService;
import com.example.myapplication.ui.add.AddMuseumActivity;
import com.example.myapplication.ui.main.BaseActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MuseumDetailActivity extends BaseActivity {

    private Place place;
    private FirestoreRepository repository;
    private PlaceRepository roomRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        roomRepository = PlaceRepository.getInstance(this);
        repository = FirestoreRepository.getInstance();
        setupToolbar();

        place = getIntent().getParcelableExtra("place");
        if (place == null) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
    }

    // SHARE
    private void sharePlace() {
        String shareText = buildShareText();
        Uri imageUri     = getFirstImageUri();

        ShareCompat.IntentBuilder builder = new ShareCompat.IntentBuilder(this)
                .setSubject(place.getName())
                .setChooserTitle(getString(R.string.share_via));

        if (imageUri != null) {
            builder.setType("image/*")
                    .addStream(imageUri)
                    .setText(shareText);
        } else {
            builder.setType("text/plain")
                    .setText(shareText);
        }

        builder.startChooser();
    }

    private String buildShareText() {
        StringBuilder sb = new StringBuilder();

        sb.append(place.getName()).append("\n");
        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            sb.append(getString(R.string.address_label))
                    .append(" ").append(place.getAddress()).append("\n");
        }

        if (place.getPhone() != null && !place.getPhone().isEmpty()) {
            sb.append(getString(R.string.phone_label))
                    .append(" ").append(place.getPhone()).append("\n");
        }

        if (place.getWorkingHours() != null && !place.getWorkingHours().isEmpty()) {
            sb.append(getString(R.string.working_hours_label))
                    .append(" ").append(place.getWorkingHours()).append("\n");
        }

        if (place.hasCoordinates()) {
            sb.append("https://yandex.com/maps/?q=")
                    .append(place.getLatitude()).append(",")
                    .append(place.getLongitude()).append("\n");
        }

        if (place.getWebsite() != null && !place.getWebsite().isEmpty()) {
            sb.append(place.getWebsite()).append("\n");
        }

        if (place.getDescription() != null && !place.getDescription().isEmpty()) {
            sb.append("\n").append(place.getDescription());
        }

        return sb.toString();
    }

    private Uri getFirstImageUri() {
        if (place.getImageIds() == null || place.getImageIds().isEmpty()) return null;

        String fileName = place.getImageIds().get(0);

        if (fileName == null || fileName.equals("no_image")) return null;

        ImageStorageService imageService = new ImageStorageService(this);
        String path = imageService.getImagePath(fileName);
        if (path == null) return null;

        File file = new File(path);
        if (!file.exists()) return null;

        try {
            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );
        } catch (IllegalArgumentException e) {
            return null;
        }
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

        showOrHide(R.id.detailDescription, place.getDescription(), place.getDescription());
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
        } else if (id == R.id.action_share) {
            sharePlace();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editPlace() {
        Intent intent = new Intent(this, AddMuseumActivity.class);
        intent.putExtra("place", place);
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
            int color = settingsManager.isDarkTheme()
                    ? androidx.core.content.ContextCompat.getColor(this, R.color.light)
                    : androidx.core.content.ContextCompat.getColor(this, R.color.dark);
            positiveButton.setTextColor(color);
        }

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            int color = settingsManager.isDarkTheme()
                    ? androidx.core.content.ContextCompat.getColor(this, R.color.light)
                    : androidx.core.content.ContextCompat.getColor(this, R.color.dark);
            negativeButton.setTextColor(color);
        }
    }

    private void deletePlace() {
        repository.delete(place.getFirestoreId(), new PlaceRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                roomRepository.deletePlace(place, new PlaceRepository.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void v) { }
                    @Override
                    public void onError(Exception e) { }
                });
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