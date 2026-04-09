package com.example.myapplication.ui.add;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Place;

import com.example.myapplication.data.serviceImage.IImageSelectionListener;
import com.example.myapplication.data.serviceImage.ImagePickerHelper;
import com.example.myapplication.data.serviceImage.ImagePreviewAdapter;
import com.example.myapplication.data.serviceImage.ImageStorageService;
import com.example.myapplication.ui.detail.MuseumDetailActivity;
import com.example.myapplication.ui.main.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddMuseumActivity extends BaseActivity implements IImageSelectionListener {
    private TextInputEditText editName, editAddress, editPhone, editWebsite, editWorkingHours;
    private TextInputEditText editDescription, editLatitude, editLongitude;
    private TextInputLayout   layoutLatitude, layoutLongitude;
    private SwitchCompat switchVisited;
    private TextView     textVisitedLabel, textVisitDate;
    private View         visitDateBlock, ratingBlock;
    private RatingBar    ratingBar;
    private RecyclerView imageRecyclerView;
    private ImagePreviewAdapter imagePreviewAdapter;
    private ArrayList<String> selectedImageFileNames = new ArrayList<>();
    private MaterialButton btnAddImages;
    private ImagePickerHelper imagePickerHelper;
    private ImageStorageService imageStorageService;

    private long selectedVisitDate = 0;
    private Place editingPlace = null;
    private boolean isEditMode = false;
    private AddMuseumViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        viewModel = new ViewModelProvider(this).get(AddMuseumViewModel.class);
        imageStorageService = new ImageStorageService(this);
        imagePickerHelper = new ImagePickerHelper(this, this);

        setupToolbar();
        initViews();
        setupImageRecyclerView();
        setupSwitchListener();
        setupDatePicker();
        checkEditMode();
        observeViewModel();
    }

    private void initViews() {
        editName         = findViewById(R.id.editName);
        editAddress      = findViewById(R.id.editAddress);
        editPhone        = findViewById(R.id.editPhone);
        editWebsite      = findViewById(R.id.editWebsite);
        editWorkingHours = findViewById(R.id.editWorkingHours);
        editDescription  = findViewById(R.id.editDescription);

        editLatitude     = findViewById(R.id.editLatitude);
        editLongitude    = findViewById(R.id.editLongitude);
        layoutLatitude   = findViewById(R.id.layoutLatitude);
        layoutLongitude  = findViewById(R.id.layoutLongitude);

        switchVisited    = findViewById(R.id.switchVisited);
        textVisitedLabel = findViewById(R.id.textVisitedLabel);
        visitDateBlock   = findViewById(R.id.visitDateBlock);
        ratingBlock      = findViewById(R.id.ratingBlock);
        textVisitDate    = findViewById(R.id.textVisitDate);
        ratingBar        = findViewById(R.id.ratingBar);

        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        btnAddImages = findViewById(R.id.btnAddImages);

        btnAddImages.setOnClickListener(v -> imagePickerHelper.pickImages());
    }

    private void setupImageRecyclerView() {
        imagePreviewAdapter = new ImagePreviewAdapter(
                this,
                selectedImageFileNames,
                this::removeImage
        );

        imageRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        imageRecyclerView.setAdapter(imagePreviewAdapter);
    }

    @Override
    public void onImagesSelected(List<Uri> imageUris) {
        for (Uri uri : imageUris) {
            String fileName = imageStorageService.saveImageToInternalStorage(uri);
            if (fileName != null) {
                selectedImageFileNames.add(fileName);
            } else {
                Toast.makeText(this, getString(R.string.error_saving_image), Toast.LENGTH_SHORT).show();
            }
        }
        imagePreviewAdapter.notifyDataSetChanged();
        updateImageVisibility();
    }

    @Override
    public void onImageSelectionError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    private void removeImage(int position) {
        if (position >= 0 && position < selectedImageFileNames.size()) {
            String fileName = selectedImageFileNames.get(position);
            imageStorageService.deleteImage(fileName);
            selectedImageFileNames.remove(position);
            imagePreviewAdapter.notifyItemRemoved(position);
            updateImageVisibility();
        }
    }

    private void updateImageVisibility() {
        if (selectedImageFileNames.isEmpty()) {
            imageRecyclerView.setVisibility(View.GONE);
        } else {
            imageRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupSwitchListener() {
        switchVisited.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                textVisitedLabel.setText(R.string.status_visited);
                visitDateBlock.setVisibility(View.VISIBLE);
                ratingBlock.setVisibility(View.VISIBLE);
            } else {
                textVisitedLabel.setText(R.string.status_planned);
                visitDateBlock.setVisibility(View.GONE);
                ratingBlock.setVisibility(View.GONE);
                selectedVisitDate = 0;
                textVisitDate.setText(R.string.visit_date_label);
            }
        });
    }

    private void setupDatePicker() {
        findViewById(R.id.btnPickDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (selectedVisitDate > 0) cal.setTimeInMillis(selectedVisitDate);

            new DatePickerDialog(this,
                    (picker, year, month, day) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, day);
                        selectedVisitDate = selected.getTimeInMillis();
                        String formatted = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                .format(selected.getTime());
                        textVisitDate.setText(getString(R.string.visit_date_label) + " " + formatted);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void checkEditMode() {
        editingPlace = getIntent().getParcelableExtra("place");
        if (editingPlace != null) {
            isEditMode = true;
            setTitle(getString(R.string.edit_museum));
            fillFormWithData(editingPlace);
        } else {
            isEditMode = false;
            setTitle(getString(R.string.add_museum));
        }
    }

    private void fillFormWithData(Place place) {
        editName.setText(place.getName());
        editAddress.setText(place.getAddress());
        editPhone.setText(place.getPhone());
        editWebsite.setText(place.getWebsite());
        editWorkingHours.setText(place.getWorkingHours());
        editDescription.setText(place.getDescription());

        if (place.hasCoordinates()) {
            editLatitude.setText(String.valueOf(place.getLatitude()));
            editLongitude.setText(String.valueOf(place.getLongitude()));
        }

        switchVisited.setChecked(place.isVisited());

        if (place.getVisitDate() > 0) {
            selectedVisitDate = place.getVisitDate();
            String formatted = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(place.getVisitDate());
            textVisitDate.setText(getString(R.string.visit_date_label) + " " + formatted);
        }

        ratingBar.setRating(place.getRating());

        if (place.getImageIds() != null && !place.getImageIds().isEmpty()) {
            selectedImageFileNames.clear();
            selectedImageFileNames.addAll(place.getImageIds());
            imagePreviewAdapter.notifyDataSetChanged();
            updateImageVisibility();
        }
    }

    private void observeViewModel() {
        viewModel.saved.observe(this, isSaved -> {
            if (Boolean.TRUE.equals(isSaved)) {
                Toast.makeText(this,
                        isEditMode ? getString(R.string.museum_updated) : getString(R.string.museum_added),
                        Toast.LENGTH_SHORT).show();
                if (isEditMode && editingPlace != null) {
                    Intent intent = new Intent(this, MuseumDetailActivity.class);
                    intent.putExtra("place", editingPlace);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                finish();
            }
        });

        viewModel.error.observe(this, errorMsg -> {
            if (errorMsg != null) {
                Toast.makeText(this, getString(R.string.error_add) + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void savePlace() {
        layoutLatitude.setError(null);
        layoutLongitude.setError(null);

        String name = editName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            editName.setError(getString(R.string.error_empty_name));
            editName.requestFocus();
            return;
        }

        String latStr = editLatitude.getText().toString().trim();
        String lonStr = editLongitude.getText().toString().trim();

        double latitude  = 0;
        double longitude = 0;
        boolean hasCoords = !latStr.isEmpty() || !lonStr.isEmpty();

        if (hasCoords) {
            if (latStr.isEmpty()) {
                layoutLatitude.setError(getString(R.string.error_latitude_required));
                editLatitude.requestFocus();
                return;
            }
            if (lonStr.isEmpty()) {
                layoutLongitude.setError(getString(R.string.error_longitude_required));
                editLongitude.requestFocus();
                return;
            }

            try {
                latitude = Double.parseDouble(latStr);
            } catch (NumberFormatException e) {
                layoutLatitude.setError(getString(R.string.error_invalid_number));
                editLatitude.requestFocus();
                return;
            }

            try {
                longitude = Double.parseDouble(lonStr);
            } catch (NumberFormatException e) {
                layoutLongitude.setError(getString(R.string.error_invalid_number));
                editLongitude.requestFocus();
                return;
            }

            if (latitude < -90 || latitude > 90) {
                layoutLatitude.setError(getString(R.string.error_latitude_range));
                editLatitude.requestFocus();
                return;
            }

            if (longitude < -180 || longitude > 180) {
                layoutLongitude.setError(getString(R.string.error_longitude_range));
                editLongitude.requestFocus();
                return;
            }
        }

        String address      = editAddress.getText().toString().trim();
        String phone        = editPhone.getText().toString().trim();
        String website      = editWebsite.getText().toString().trim();
        String workingHours = editWorkingHours.getText().toString().trim();
        String description  = editDescription.getText().toString().trim();
        boolean isVisited   = switchVisited.isChecked();
        float rating        = ratingBar.getRating();

        // Если изображений нет, добавляем дефолтное
       /* if (selectedImageFileNames.isEmpty()) {
            selectedImageFileNames.add("natioanal_hud_museum_1920x1280");
        }
*/
        if (isEditMode && editingPlace != null) {
            ArrayList<String> oldImages = editingPlace.getImageIds();
            if (oldImages != null) {
                for (String oldImage : oldImages) {
                    if (!selectedImageFileNames.contains(oldImage)) {
                        imageStorageService.deleteImage(oldImage);
                    }
                }
            }

            editingPlace.setName(name);
            editingPlace.setAddress(address);
            editingPlace.setPhone(phone);
            editingPlace.setWebsite(website);
            editingPlace.setWorkingHours(workingHours);
            editingPlace.setDescription(description);
            editingPlace.setLatitude(latitude);
            editingPlace.setLongitude(longitude);
            editingPlace.setVisited(isVisited);
            editingPlace.setRating(rating);
            editingPlace.setVisitDate(isVisited ? selectedVisitDate : 0);
           // editingPlace.setImageIds(new ArrayList<>(selectedImageFileNames));

            editingPlace.setImageIds(
                    selectedImageFileNames.isEmpty()
                            ? new ArrayList<>()
                            : new ArrayList<>(selectedImageFileNames)
            );

            viewModel.updatePlace(editingPlace);
        } else {
            Place newPlace = Place.createManual(name, description, phone, website);
            newPlace.setAddress(address);
            newPlace.setWorkingHours(workingHours);
            newPlace.setLatitude(latitude);
            newPlace.setLongitude(longitude);
            newPlace.setVisited(isVisited);
            newPlace.setRating(rating);
            newPlace.setVisitDate(isVisited ? selectedVisitDate : 0);
          //  newPlace.setImageIds(new ArrayList<>(selectedImageFileNames));
            newPlace.setImageIds(
                    selectedImageFileNames.isEmpty()
                            ? new ArrayList<>()
                            : new ArrayList<>(selectedImageFileNames)
            );
            viewModel.insertPlace(newPlace);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.addEditToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            savePlace();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }


   @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isEditMode && !Boolean.TRUE.equals(viewModel.saved.getValue())) {
            for (String fileName : selectedImageFileNames) {
                if (fileName.startsWith("img_")) {
                    imageStorageService.deleteImage(fileName);
                }
            }
        }
    }
}