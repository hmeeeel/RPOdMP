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

public class UsersFragment extends Fragment {

    private static final String TAG = "UsersFragment";

    private RecyclerView recyclerView;
    private UserStatsAdapter adapter;
    private View progressBar;
    private View layoutEmpty;
    private TextView textEmpty;
    private List<UserStats> users = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        try {
            recyclerView = view.findViewById(R.id.recyclerUsers);
            progressBar = view.findViewById(R.id.usersProgress);
            layoutEmpty = view.findViewById(R.id.layoutUsersEmpty);
            textEmpty = view.findViewById(R.id.textUsersEmpty);

            if (recyclerView == null || progressBar == null) {
                Log.e(TAG, " Views == null!");
                return;
            }

            setupRecyclerView();
            loadUsers();

        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION в onViewCreated", e);
            Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView");
        adapter = new UserStatsAdapter(requireContext(), users);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadUsers() {
        Log.d(TAG, "=== loadUsers START ===");

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);

        AdminRepository.getInstance().getUserStats(
                new PlaceRepository.DataCallback<List<UserStats>>() {
                    @Override
                    public void onSuccess(List<UserStats> data) {
                        Log.d(TAG, " onSuccess: получено " +
                                (data != null ? data.size() : 0) + " пользователей");

                        if (getActivity() == null || !isAdded()) return;

                        progressBar.setVisibility(View.GONE);
                        users.clear();
                        if (data != null) users.addAll(data);

                        if (users.isEmpty()) {
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