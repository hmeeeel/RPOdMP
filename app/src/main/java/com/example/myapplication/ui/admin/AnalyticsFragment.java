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

import java.util.ArrayList;
import java.util.List;

public class AnalyticsFragment extends Fragment {

    private static final String TAG = "AnalyticsFragment";

    private RecyclerView recyclerView;
    private TopPlaceAdapter adapter;
    private View progressBar;
    private View layoutEmpty;
    private TextView textEmpty;
    private List<TopPlace> places = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        try {
            recyclerView = view.findViewById(R.id.recyclerAnalytics);
            progressBar = view.findViewById(R.id.analyticsProgress);
            layoutEmpty = view.findViewById(R.id.layoutAnalyticsEmpty);
            textEmpty = view.findViewById(R.id.textAnalyticsEmpty);

            if (recyclerView == null || progressBar == null) {
                Log.e(TAG, " Views == null!");
                return;
            }

            setupRecyclerView();
            loadTopPlaces();

        } catch (Exception e) {
            Log.e(TAG, " EXCEPTION в onViewCreated", e);
            Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView");
        adapter = new TopPlaceAdapter(requireContext(), places);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadTopPlaces() {
        Log.d(TAG, "=== loadTopPlaces START ===");

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);

        AdminRepository.getInstance().getTopPlaces(
                new PlaceRepository.DataCallback<List<TopPlace>>() {
                    @Override
                    public void onSuccess(List<TopPlace> data) {
                        Log.d(TAG, " onSuccess: получено " +
                                (data != null ? data.size() : 0) + " мест");

                        if (getActivity() == null || !isAdded()) return;

                        progressBar.setVisibility(View.GONE);
                        places.clear();
                        if (data != null) places.addAll(data);

                        if (places.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                        } else {
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
}