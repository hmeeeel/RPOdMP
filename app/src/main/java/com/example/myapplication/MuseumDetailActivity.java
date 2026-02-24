package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

public class MuseumDetailActivity extends BaseActivity {
    private Museum museum;
    private MuseumRepository repository;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        repository = MuseumRepository.getInstance(this);

        setupToolbar();

        ViewPager2 slider = findViewById(R.id.detailSlider);
        TextView name = findViewById(R.id.detailName);
        TextView description = findViewById(R.id.detailDescription);
        TextView phone = findViewById(R.id.detailPhone);
        TextView website = findViewById(R.id.detailWebsite);

        museum = getIntent().getParcelableExtra("museum");
        if (museum == null) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (museum != null){
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(museum.getImageIds());
            slider.setAdapter(sliderAdapter);
            name.setText(museum.getName());
            description.setText(museum.getDescriprion());
            if (museum.getPhone() != null && !museum.getPhone().isEmpty()) {
                phone.setVisibility(View.VISIBLE);
                phone.setText(getString(R.string.phone_label) + " " + museum.getPhone());
            } else {
                phone.setVisibility(View.GONE);
            }

            if (museum.getWebsite() != null && !museum.getWebsite().isEmpty()) {
                website.setVisibility(View.VISIBLE);
                website.setText(getString(R.string.website_label) + " " + museum.getWebsite());
            } else {
                website.setVisibility(View.GONE);
            }
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

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            editMuseum();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void editMuseum() {
        Intent intent = new Intent(this, AddMuseumActivity.class);
        intent.putExtra("museum", museum);
        startActivity(intent);
        finish();
    }
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_dialog))
                .setMessage(getString(R.string.delete_dialog_message, museum.getName()))
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMuseum();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
    private void deleteMuseum() {
        repository.deleteMuseum(museum, new MuseumRepository.DataCallback<Void>() {
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
