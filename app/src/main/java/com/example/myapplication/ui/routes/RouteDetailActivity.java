package com.example.myapplication.ui.routes;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.supabase.RouteRepository;
import com.example.myapplication.ui.detail.MuseumDetailActivity;
import com.example.myapplication.ui.main.BaseActivity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RouteDetailActivity extends BaseActivity
        implements RouteDetailPointAdapter.OnPointClickListener {

    private RouteDetailViewModel viewModel;
    private RouteCard            routeCard;

    private TextView textTitle, textAuthor, textDescription, textStatus,
            textRating, textAdminNote, textEmpty;
    private MaterialButton      btnSave;
    private View                progressBar;
    private RecyclerView        rvPoints, rvReviews;
    private RouteDetailPointAdapter pointAdapter;

    private List<RoutePoint> points = new ArrayList<>();
    private List<RouteReview>               reviews = new ArrayList<>();
    private ActivityResultLauncher<Intent> detailLauncher;

    private MenuItem menuItemEdit;

    private ActivityResultLauncher<Intent> editLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        routeCard = getIntent().getParcelableExtra("route_card");
        if (routeCard == null) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(RouteDetailViewModel.class);

        // Launcher для возврата из MuseumDetailActivity
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> viewModel.reloadPoints(routeCard.getId())
        );


      /*  editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {

                        // ← ИСПОЛЬЗУЕМ reloadAllData() вместо loadDetail()
                        // чтобы не пересоздавать WebSocket подписку
                        viewModel.reloadAllData();

                        Intent data = result.getData();
                        if (data != null) {
                            String newTitle = data.getStringExtra("updated_title");
                            if (newTitle != null) {
                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(newTitle);
                                }
                                textTitle.setText(newTitle);
                                routeCard.setTitle(newTitle);
                            }
                        }
                    }
                }
        );*/
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // reloadAllData загрузит header через loadRouteHeader
                        // observer routeHeader автоматически обновит UI
                        viewModel.reloadAllData();
                        // ← Убираем ручное обновление textTitle и routeCard.setTitle
                        // это теперь делает observer routeHeader
                    }
                }
        );
        setupToolbar();
        bindHeader();
        setupRecyclerViews();
        observeData();
        viewModel.loadDetail(routeCard.getId());
    }

    @Override
    public void onPointClick(Place place) {
        Intent intent = new Intent(this, MuseumDetailActivity.class);
        intent.putExtra("place", place);
        detailLauncher.launch(intent);  //  launcher вместо startActivity
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.routeDetailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(routeCard.getTitle());
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.route_detail_menu, menu);
        menuItemEdit = menu.findItem(R.id.action_edit_route);

        String numericId = viewModel.getCurrentNumericUserId();
        String authorId  = routeCard.getAuthorId();

        android.util.Log.d("EDIT_BTN", "numericId = " + numericId);
        android.util.Log.d("EDIT_BTN", "authorId  = " + authorId);
        android.util.Log.d("EDIT_BTN", "isOwner   = " + (numericId != null && numericId.equals(authorId)));

        boolean isOwner = numericId != null && numericId.equals(authorId);
        if (menuItemEdit != null) {
            menuItemEdit.setVisible(isOwner);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_view_on_map) {
            openMapPreview();
            return true;
        }

        if (id == R.id.action_edit_route) {
            openEditScreen();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindHeader() {
        textTitle       = findViewById(R.id.textDetailRouteTitle);
        textAuthor      = findViewById(R.id.textDetailRouteAuthor);
        textDescription = findViewById(R.id.textDetailRouteDescription);
        textStatus      = findViewById(R.id.textDetailRouteStatus);
        textRating      = findViewById(R.id.textDetailRouteRating);
        textAdminNote   = findViewById(R.id.textDetailAdminNote);
        textEmpty       = findViewById(R.id.textDetailPointsEmpty);
        btnSave         = findViewById(R.id.btnDetailSaveRoute);
        progressBar     = findViewById(R.id.detailRouteProgress);

        // Заполняем данные
        textTitle.setText(routeCard.getTitle());
        textAuthor.setText(routeCard.getAuthorNickname());

        if (routeCard.getDescription() != null
                && !routeCard.getDescription().isEmpty()) {
            textDescription.setVisibility(View.VISIBLE);
            textDescription.setText(routeCard.getDescription());
        } else {
            textDescription.setVisibility(View.GONE);
        }

        textStatus.setText(getStatusText());

        if (routeCard.getReviewsCount() > 0) {
            textRating.setVisibility(View.VISIBLE);
            textRating.setText(String.format("★ %.1f · %d %s · ❤ %d",
                    routeCard.getAverageRating(),
                    routeCard.getReviewsCount(),
                    getString(R.string.reviews_label),
                    routeCard.getLikesCount()));
        } else {
            textRating.setVisibility(View.GONE);
        }

        if (routeCard.getAdminNote() != null
                && !routeCard.getAdminNote().isEmpty()) {
            textAdminNote.setVisibility(View.VISIBLE);
            textAdminNote.setText("💡 " + routeCard.getAdminNote());
        } else {
            textAdminNote.setVisibility(View.GONE);
        }

        // Кнопка "Сохранить" — только для чужих опубликованных маршрутов
        String  numericId = viewModel.getCurrentNumericUserId();
        boolean isOwner   = numericId != null
                && numericId.equals(routeCard.getAuthorId());

        if (isOwner || !"published".equals(routeCard.getStatusCode())) {
            btnSave.setVisibility(View.GONE);
        } else {
            btnSave.setVisibility(View.VISIBLE);
            updateSaveButton(routeCard.isSaved());
            btnSave.setOnClickListener(v -> viewModel.toggleSave(routeCard.getId()));
        }
    }


    private void openEditScreen() {
        Intent intent = new Intent(this, CreateRouteActivity.class);
        intent.putExtra("mode",        "edit");
        intent.putExtra("route_id",    routeCard.getId());
        intent.putExtra("route_title", routeCard.getTitle());
        intent.putExtra("route_desc",  routeCard.getDescription());
        intent.putExtra("is_public",
                "published".equals(routeCard.getStatusCode())
                        || "pending".equals(routeCard.getStatusCode()));
        editLauncher.launch(intent);
    }
    private void setupRecyclerViews() {
        rvPoints = findViewById(R.id.recyclerDetailPoints);
        rvPoints.setLayoutManager(new LinearLayoutManager(this));
        rvPoints.setNestedScrollingEnabled(false);
        pointAdapter = new RouteDetailPointAdapter(this, points, this);
        rvPoints.setAdapter(pointAdapter);

        rvReviews = findViewById(R.id.recyclerDetailReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setNestedScrollingEnabled(false);
    }

    private void observeData() {
        viewModel.isLoading.observe(this, loading -> {
            progressBar.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.points.observe(this, data -> {
            points.clear();
            if (data != null) points.addAll(data);
            pointAdapter.notifyDataSetChanged();
            textEmpty.setVisibility(points.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.reviews.observe(this, data -> {
            reviews.clear();
            if (data != null) reviews.addAll(data);
        });

        viewModel.isSaved.observe(this, saved -> {
            if (btnSave.getVisibility() == View.VISIBLE) {
                updateSaveButton(Boolean.TRUE.equals(saved));
            }
        });

        //  observer — обновляет header автоматически
        viewModel.routeHeader.observe(this, header -> {
            if (header == null) return;

            // Обновляем routeCard актуальными данными
            routeCard.setTitle(header.title);
            routeCard.setDescription(header.description);
            routeCard.setStatusCode(header.statusCode);
            routeCard.setAdminNote(header.adminNote);

            // Обновляем UI
            textTitle.setText(header.title);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(header.title);
            }

            if (header.description != null && !header.description.isEmpty()) {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(header.description);
            } else {
                textDescription.setVisibility(View.GONE);
            }

            textStatus.setText(getStatusText());

            if (header.adminNote != null && !header.adminNote.isEmpty()) {
                textAdminNote.setVisibility(View.VISIBLE);
                textAdminNote.setText("💡 " + header.adminNote);
            } else {
                textAdminNote.setVisibility(View.GONE);
            }
        });

        viewModel.error.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.event.observe(this, evt -> {
            if ("saved_ok".equals(evt)) {
                Toast.makeText(this, R.string.route_saved_to_favorites,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSaveButton(boolean saved) {
        btnSave.setText(saved ? R.string.btn_unsave_route : R.string.btn_save_route);
        btnSave.setIconResource(saved
                ? R.drawable.ic_bookmark_filled
                : R.drawable.ic_bookmark_border);
    }


    //  Карта
    private void openMapPreview() {
        if (points.isEmpty()) {
            Toast.makeText(this, R.string.error_route_points, Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Double> lats  = new ArrayList<>();
        ArrayList<Double> lons  = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();

        for (RoutePoint rp : points) {
            Place p = rp.getPlace();
            if (p != null && p.hasCoordinates()) {
                lats.add(p.getLatitude());
                lons.add(p.getLongitude());
                names.add(p.getName());
            }
        }

        if (lats.isEmpty()) {
            Toast.makeText(this, R.string.no_coordinates_message, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, com.example.myapplication.ui.map.MapActivity.class);
        intent.putExtra("mode",       "route_preview");
        intent.putExtra("latitudes",  lats);
        intent.putExtra("longitudes", lons);
        intent.putStringArrayListExtra("names", names);
        startActivity(intent);
    }

    private String getStatusText() {
        switch (routeCard.getStatusCode() != null ? routeCard.getStatusCode() : "") {
            case "published":   return getString(R.string.status_public);
            case "pending":     return getString(R.string.status_pending);
            case "draft":       return getString(R.string.status_draft);
            case "unavailable": return getString(R.string.status_unavailable);
            default: return "";
        }
    }
}