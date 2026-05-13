package com.example.myapplication.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.List;

public class UserStatsAdapter extends RecyclerView.Adapter<UserStatsAdapter.ViewHolder> {

    private final Context context;
    private final List<UserStats> items;

    public UserStatsAdapter(Context context, List<UserStats> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_user_stats, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        UserStats user = items.get(position);

        h.textRank.setText(String.valueOf(position + 1));
        h.textNickname.setText(user.getNickname());
        h.textCreated.setText(context.getString(R.string.routes_created_format, user.getRoutesCreatedCount()));
        h.textSaved.setText(context.getString(R.string.routes_saved_format, user.getRoutesSavedCount()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textRank, textNickname, textCreated, textSaved;

        ViewHolder(View v) {
            super(v);
            textRank = v.findViewById(R.id.textUserRank);
            textNickname = v.findViewById(R.id.textUserNickname);
            textCreated = v.findViewById(R.id.textUserRoutesCreated);
            textSaved = v.findViewById(R.id.textUserRoutesSaved);
        }
    }
}