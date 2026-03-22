package com.example.myapplication.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.ui.detail.IMuseumClick;
import com.example.myapplication.ui.detail.ImageSliderAdapter;

import java.util.ArrayList;
import java.util.List;

public class MuseumAdapter extends RecyclerView.Adapter<MuseumAdapter.ViewHolder> {

    private final LayoutInflater inflater;
    private final List<Place> places;
    private final IMuseumClick click;

    MuseumAdapter(Context context, List<Place> places, IMuseumClick click) {
        this.inflater = LayoutInflater.from(context);
        this.places = places;
        this.click = click;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.museum_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Place place = places.get(position);

        holder.name.setText(place.getName());

        if (place.isVisited()) {
            holder.statusBadge.setText(R.string.status_visited);
            holder.statusBadge.setTextColor(
                    holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        } else {
            holder.statusBadge.setText(R.string.status_planned);
            holder.statusBadge.setTextColor(
                    holder.itemView.getContext().getColor(android.R.color.darker_gray));
        }

        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            holder.address.setVisibility(View.VISIBLE);
            holder.address.setText(place.getAddress());
        } else {
            holder.address.setVisibility(View.GONE);
        }

        List<String> images = place.getImageIds();
        if (images == null || images.isEmpty()) {
            images = new ArrayList<>();
            images.add("natioanal_hud_museum_1920x1280");
        }

        List<String> finalImages = images;
        ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(finalImages, () -> {
            if (click != null) click.onMuseumClick(place);
        });
        holder.museumImage.setAdapter(sliderAdapter);

        holder.name.setOnClickListener(v -> {
            if (click != null) click.onMuseumClick(place);
        });
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView statusBadge;
        TextView address;
        ViewPager2 museumImage;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            address = itemView.findViewById(R.id.itemAddress);
            museumImage = itemView.findViewById(R.id.museumImage);
        }
    }
}