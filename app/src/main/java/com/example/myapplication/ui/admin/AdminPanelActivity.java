package com.example.myapplication.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.data.supabase.SupabaseClient;
import com.example.myapplication.ui.auth.AuthManager;
import com.example.myapplication.ui.auth.LoginActivity;
import com.example.myapplication.ui.main.BaseActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// ✅ ПРАВИЛЬНЫЕ импорты okhttp3
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AdminPanelActivity extends BaseActivity {

    private static final String TAG = "AdminPanelActivity";
    private static final int ADMIN_ROLE_ID = 1;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  Залогинен?
        if (!SupabaseClient.getInstance().isLoggedIn()) {
            Log.w(TAG, "❌ Не авторизован");
            Toast.makeText(this, R.string.access_denied, Toast.LENGTH_LONG).show();
            navigateToMain();
            return;
        }

        // Email = admin@gmail.com?
        String email = SupabaseClient.getInstance().getUserEmail();
        Log.d(TAG, "Проверка доступа для email: " + email);

        if (!"admin@gmail.com".equalsIgnoreCase(email)) {
            Log.w(TAG, "❌ Доступ запрещён для " + email);
            Toast.makeText(this, R.string.access_denied, Toast.LENGTH_LONG).show();
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_admin_panel);
        setupToolbar();
        initViews();
        setupViewPager();
    }

    private void initViews() {
        viewPager   = findViewById(R.id.adminViewPager);
        tabLayout   = findViewById(R.id.adminTabLayout);
       // progressBar = findViewById(R.id.adminProgressBar);
    }

    // Проверяет роль пользователя через БД.  numericUserId который уже загружен при логине в AuthManager.
    private void checkAdminRoleFromDatabase() {
        Long numericId = SupabaseClient.getInstance().getNumericUserId();

        Log.d(TAG, "checkAdminRole: numericUserId = " + numericId);

        if (numericId == null) {
            Log.e(TAG, "numericUserId == null, пользователь не авторизован корректно");
            handleAccessDenied();
            return;
        }

        // Запрос: получаем role_id для текущего пользователя
        String query = "select=id,role_id,email&id=eq." + numericId;
        Request request = SupabaseClient.getInstance()
                .dbRequest("users", query)
                .get()
                .build();

        SupabaseClient.getInstance().getHttpClient()
                .newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Ошибка сети при проверке роли", e);
                        runOnUiThread(() -> {
                            Toast.makeText(AdminPanelActivity.this,
                                    "Ошибка сети. Попробуйте позже.", Toast.LENGTH_SHORT).show();
                            handleAccessDenied();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body().string();
                        Log.d(TAG, "checkRole HTTP " + response.code() + " body: " + body);

                        boolean isAdmin = false;

                        try {
                            JsonArray array = JsonParser.parseString(body).getAsJsonArray();
                            if (array.size() > 0) {
                                JsonObject userObj = array.get(0).getAsJsonObject();
                                int roleId = userObj.get("role_id").getAsInt();
                                String email = userObj.has("email") ?
                                        userObj.get("email").getAsString() : "unknown";

                                Log.d(TAG, "email=" + email + " role_id=" + roleId
                                        + " ADMIN_ROLE_ID=" + ADMIN_ROLE_ID);

                                isAdmin = (roleId == ADMIN_ROLE_ID);
                            } else {
                                Log.w(TAG, "Пользователь не найден в таблице users");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка парсинга ответа", e);
                        }

                        final boolean adminConfirmed = isAdmin;
                        runOnUiThread(() -> {
                            showLoadingState(false);
                            if (adminConfirmed) {
                                Log.d(TAG, "✅ Доступ администратора подтверждён");
                                setupViewPager();
                            } else {
                                Log.d(TAG, "❌ Доступ запрещён — не администратор");
                                handleAccessDenied();
                            }
                        });
                    }
                });
    }

    private void handleAccessDenied() {
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.access_denied, Toast.LENGTH_LONG).show();
            navigateToMain();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void showLoadingState(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (viewPager != null) {
            viewPager.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
        if (tabLayout != null) {
            tabLayout.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.adminToolbar);
        if (toolbar == null) return;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.admin_panel_title);
        }
    }

    private void setupViewPager() {
        if (viewPager == null || tabLayout == null) return;

        AdminPagerAdapter adapter = new AdminPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_moderation);
                    tab.setIcon(R.drawable.ic_pending_24dp);
                    break;
                case 1:
                    tab.setText(R.string.tab_users);
                    tab.setIcon(R.drawable.ic_people_24dp);
                    break;
                case 2:
                    tab.setText(R.string.tab_analytics);
                    tab.setIcon(R.drawable.ic_analytics_24dp);
                    break;
            }
        }).attach();
    }

    // Стрелка назад вызывает диалог выхода
    @Override
    public boolean onSupportNavigateUp() {
        showLogoutConfirmation();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            // Выход из аккаунта
            showLogoutConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
        showLogoutConfirmation();
    }
    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Вы уверены, что хотите выйти из аккаунта администратора?")
                .setPositiveButton("Выйти", (dialog, which) -> {
                    // выход
                    AuthManager.getInstance().signOut();

                    // Переход на экран логина
                    Intent intent = new Intent(AdminPanelActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}