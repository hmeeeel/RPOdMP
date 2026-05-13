package com.example.myapplication.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.repository.PlaceRepository;
import com.example.myapplication.data.supabase.RouteRealtimeClient;

import java.util.ArrayList;
import java.util.List;

public class ModerationFragment extends Fragment {

    private static final String TAG = "ModerationFragment";

    private RecyclerView recyclerView;
    private ModerationAdapter adapter;
    private View progressBar;
    private View layoutEmpty;
    private TextView textEmpty;
    private List<ModerationRoute> routes = new ArrayList<>();
    private RouteRealtimeClient realtimeClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_moderation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        try {
            recyclerView = view.findViewById(R.id.recyclerModeration);
            progressBar = view.findViewById(R.id.moderationProgress);
            layoutEmpty = view.findViewById(R.id.layoutModerationEmpty);
            textEmpty = view.findViewById(R.id.textModerationEmpty);

            if (recyclerView == null) {
                Log.e(TAG, " recyclerModeration == null!");
                return;
            }
            if (progressBar == null) {
                Log.e(TAG, " moderationProgress == null!");
                return;
            }

            setupRecyclerView();
            setupRealtime();
            loadPendingRoutes();

        } catch (Exception e) {
            Log.e(TAG, " EXCEPTION в onViewCreated", e);
            Toast.makeText(requireContext(), "Ошибка инициализации: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView");
        try {
            adapter = new ModerationAdapter(requireContext(), routes, this::handleModeration);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(adapter);
            Log.d(TAG, " RecyclerView настроен");
        } catch (Exception e) {
            Log.e(TAG, " Ошибка setupRecyclerView", e);
        }
    }

    private void setupRealtime() {
        try {
            realtimeClient = new RouteRealtimeClient();
            String uid = com.example.myapplication.data.supabase.SupabaseClient
                    .getInstance().getUserId();
            Log.d(TAG, "setupRealtime для uid: " + uid);
            realtimeClient.subscribe(uid, this::loadPendingRoutes, this::loadPendingRoutes);
        } catch (Exception e) {
            Log.e(TAG, " Ошибка setupRealtime", e);
        }
    }

    private void loadPendingRoutes() {
        Log.d(TAG, "=== loadPendingRoutes START ===");

        if (progressBar == null) {
            Log.e(TAG, " progressBar == null в loadPendingRoutes");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);

        AdminRepository.getInstance().getPendingRoutes(
                new PlaceRepository.DataCallback<List<ModerationRoute>>() {
                    @Override
                    public void onSuccess(List<ModerationRoute> data) {
                        Log.d(TAG, " onSuccess: получено " +
                                (data != null ? data.size() : 0) + " маршрутов");

                        if (getActivity() == null || !isAdded()) {
                            Log.w(TAG, "Fragment не attached, пропускаем обновление UI");
                            return;
                        }

                        progressBar.setVisibility(View.GONE);
                        routes.clear();
                        if (data != null) routes.addAll(data);

                        Log.d(TAG, "Список routes: " + routes.size());

                        if (routes.isEmpty()) {
                            Log.d(TAG, "Список пуст → показываем Empty");
                            recyclerView.setVisibility(View.GONE);
                            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                        } else {
                            Log.d(TAG, "Есть данные → показываем RecyclerView");
                            recyclerView.setVisibility(View.VISIBLE);
                            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, " onError: " + e.getMessage(), e);

                        if (getActivity() == null || !isAdded()) return;

                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleModeration(ModerationRoute route, String action, String note) {
        Log.d(TAG, "handleModeration: " + action + " для route " + route.getRouteId());
        progressBar.setVisibility(View.VISIBLE);

        PlaceRepository.DataCallback<Void> callback = new PlaceRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                if (getActivity() == null || !isAdded()) return;

                progressBar.setVisibility(View.GONE);
                String msg = "approve".equals(action)
                        ? getString(R.string.route_approved)
                        : getString(R.string.route_rejected);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                loadPendingRoutes();
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null || !isAdded()) return;

                Log.e(TAG, "Ошибка модерации", e);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        if ("approve".equals(action)) {
            AdminRepository.getInstance().approveRoute(route.getRouteId(), note, callback);
        } else {
            AdminRepository.getInstance().rejectRoute(route.getRouteId(), note, callback);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        if (realtimeClient != null) realtimeClient.remove();
    }
}