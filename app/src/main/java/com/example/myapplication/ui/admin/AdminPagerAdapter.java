package com.example.myapplication.ui.admin;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminPagerAdapter extends FragmentStateAdapter {

    public AdminPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new ModerationFragment();
            case 1: return new UsersFragment();
            case 2: return new AnalyticsFragment();
            default: return new ModerationFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}