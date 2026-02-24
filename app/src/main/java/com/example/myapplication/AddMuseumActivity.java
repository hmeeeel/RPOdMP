package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;


import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;

public class AddMuseumActivity extends BaseActivity {
    private EditText editName;
    private EditText editDescription;
    private EditText editPhone;
    private EditText editWebsite;

    private MuseumRepository repository;
    private Museum editingMuseum = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        repository = MuseumRepository.getInstance(this);

        setupToolbar();
        initializeViews();
        checkEditMode();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.addEditToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    private void initializeViews() {
        editName = findViewById(R.id.editName);
        editDescription = findViewById(R.id.editDescription);
        editPhone = findViewById(R.id.editPhone);
        editWebsite = findViewById(R.id.editWebsite);
    }

    private void checkEditMode() {
        editingMuseum = getIntent().getParcelableExtra("museum");

        if (editingMuseum != null) {
            isEditMode = true;
            setTitle(getString(R.string.edit_museum));
            fillFormWithData(editingMuseum);
        } else {
            isEditMode = false;
            setTitle(getString(R.string.add_museum));
        }
    }

    private void fillFormWithData(Museum museum) {
        editName.setText(museum.getName());
        editDescription.setText(museum.getDescriprion());
        editPhone.setText(museum.getPhone());
        editWebsite.setText(museum.getWebsite());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save) {
            saveMuseum();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveMuseum() {
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String website = editWebsite.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editName.setError(getString(R.string.error_empty_name));
            editName.requestFocus();
            return;
        }

        ArrayList<String> defaultImages = new ArrayList<>();
        defaultImages.add("natioanal_hud_museum_1920x1280");

        if (isEditMode && editingMuseum != null) {
            editingMuseum.setName(name);
            editingMuseum.setDescriprion(description);
            editingMuseum.setPhone(phone);
            editingMuseum.setWebsite(website);

            repository.updateMuseum(editingMuseum, new MuseumRepository.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    Toast.makeText(AddMuseumActivity.this,
                            getString(R.string.museum_updated), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AddMuseumActivity.this, MuseumDetailActivity.class);
                    intent.putExtra("museum", editingMuseum);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(AddMuseumActivity.this,
                            getString(R.string.error_add) + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Museum newMuseum = new Museum(name, defaultImages, description, phone, website);

            repository.insertMuseum(newMuseum, new MuseumRepository.DataCallback<Long>() {
                @Override
                public void onSuccess(Long id) {
                    Toast.makeText(AddMuseumActivity.this,
                            getString(R.string.museum_added), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddMuseumActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(AddMuseumActivity.this,
                            getString(R.string.error_add) + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
