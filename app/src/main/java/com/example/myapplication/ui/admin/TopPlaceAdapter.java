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

public class TopPlaceAdapter extends RecyclerView.Adapter<TopPlaceAdapter.ViewHolder> {

    private final Context context;
    private final List<TopPlace> items;

    public TopPlaceAdapter(Context context, List<TopPlace> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_top_place, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        TopPlace place = items.get(position);

        h.textRank.setText(String.valueOf(position + 1));
        h.textName.setText(place.getPlaceName());

        if (place.getPlaceAddress() != null && !place.getPlaceAddress().isEmpty()) {
            h.textAddress.setVisibility(View.VISIBLE);
            h.textAddress.setText(place.getPlaceAddress());
        } else {
            h.textAddress.setVisibility(View.GONE);
        }

        if (place.getCategoryName() != null && !place.getCategoryName().isEmpty()) {
            h.textCategory.setVisibility(View.VISIBLE);
            h.textCategory.setText(place.getCategoryName());
        } else {
            h.textCategory.setVisibility(View.GONE);
        }

        h.textCount.setText(context.getString(R.string.times_in_routes,
                place.getTotalTimesInRoutes()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textRank, textName, textAddress, textCategory, textCount;

        ViewHolder(View v) {
            super(v);
            textRank = v.findViewById(R.id.textPlaceRank);
            textName = v.findViewById(R.id.textPlaceName);
            textAddress = v.findViewById(R.id.textPlaceAddress);
            textCategory = v.findViewById(R.id.textPlaceCategory);
            textCount = v.findViewById(R.id.textPlaceCount);
        }
    }
}