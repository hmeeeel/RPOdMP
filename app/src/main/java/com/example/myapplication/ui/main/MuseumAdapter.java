package com.example.myapplication.ui.main;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.ui.detail.IMuseumClick;
import com.example.myapplication.ui.detail.ImageSliderAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MuseumAdapter extends RecyclerView.Adapter<MuseumAdapter.ViewHolder> {

    private final LayoutInflater inflater;
    private final List<Place> places;
    private final IMuseumClick click;
    private final Context context;

    private boolean selectionMode = false;
    private final Set<Long> selectedPlaceIds = new HashSet<>();
    private SelectionModeListener selectionModeListener;
    private boolean isUpdatingSelection = false;

    public interface SelectionModeListener {
        void onSelectionChanged(int count);
        void onSelectionModeToggled(boolean enabled);
    }

    MuseumAdapter(Context context, List<Place> places, IMuseumClick click) {
        this.inflater = LayoutInflater.from(context);
        this.places = places;
        this.click = click;
        this.context = context;
    }

    public void setSelectionModeListener(SelectionModeListener listener) {
        this.selectionModeListener = listener;
    }

    public void setSelectionMode(boolean enabled) {
        if (isUpdatingSelection) return;

        this.selectionMode = enabled;
        if (!enabled) {
            selectedPlaceIds.clear();
        }
        notifyDataSetChanged();

        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeToggled(enabled);
        }
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public Set<Long> getSelectedPlaceIds() {
        return new HashSet<>(selectedPlaceIds);
    }

    public void clearSelectionSilently() {
        isUpdatingSelection = true;
        try {
            selectedPlaceIds.clear();
            selectionMode = false;
            notifyDataSetChanged();
        } finally {
            isUpdatingSelection = false;
        }
    }

    public void clearSelection() {
        if (isUpdatingSelection) return;

        selectedPlaceIds.clear();
        selectionMode = false;
        notifyDataSetChanged();

        if (selectionModeListener != null) {
            selectionModeListener.onSelectionChanged(0);
            selectionModeListener.onSelectionModeToggled(false);
        }
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
        Log.e("PLACE_ID_DEBUG", "Position: " + position
                + " | place.getId(): " + place.getId()
                + " | place.getFirestoreId(): " + place.getFirestoreId()
                + " | place.getName(): " + place.getName());

        holder.name.setText(place.getName());

        if (place.isVisited()) {
            holder.statusBadge.setText(R.string.status_visited);
            holder.statusBadge.setTextColor(
                    context.getColor(android.R.color.holo_green_dark));
        } else {
            holder.statusBadge.setText(R.string.status_planned);
            holder.statusBadge.setTextColor(
                    context.getColor(android.R.color.darker_gray));
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
            images.add("no_image");
        }

        List<String> finalImages = images;
        ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(
                context, finalImages,
                () -> {
                    if (!selectionMode && click != null) {
                        click.onMuseumClick(place);
                    }
                }
        );
        holder.museumImage.setAdapter(sliderAdapter);

        boolean isSelected = selectedPlaceIds.contains(place.getId());
        holder.checkboxSelect.setVisibility(selectionMode ? View.VISIBLE : View.GONE);

        holder.checkboxSelect.setOnCheckedChangeListener(null);
        holder.checkboxSelect.setChecked(isSelected);

        holder.checkboxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (!selectionMode) {
                setSelectionMode(true);
            }

            toggleSelection(place.getId(), holder);
        });

        if (selectionMode && isSelected) {
            int highlightColor = context.getResources().getBoolean(R.bool.is_dark_theme)
                    ? ContextCompat.getColor(context, R.color.selection_highlight_dark)
                    : ContextCompat.getColor(context, R.color.selection_highlight_selector_dark);
            holder.cardView.setCardBackgroundColor(highlightColor);
        } else {
          //  holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.selection_highlight));
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(place.getId(), holder);
            } else if (click != null) {
                click.onMuseumClick(place);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {

            Log.e("LONGCLICK_DEBUG", "Long click on: " + place.getName()
                    + " | id: " + place.getId());

            if (!selectionMode) {
                setSelectionMode(true);
            }

            toggleSelection(place.getId(), holder);

            return true;
        });
    }

    private void toggleSelection(long placeId, ViewHolder holder) {
        Log.e("TOGGLE_DEBUG", "toggleSelection called with placeId: " + placeId);
        Log.e("TOGGLE_DEBUG", "Current selectedPlaceIds: " + selectedPlaceIds);

        if (selectedPlaceIds.contains(placeId)) {
            selectedPlaceIds.remove(placeId);
            Log.e("TOGGLE_DEBUG", "REMOVED. Size now: " + selectedPlaceIds.size());
        } else {
            selectedPlaceIds.add(placeId);
            Log.e("TOGGLE_DEBUG", "ADDED. Size now: " + selectedPlaceIds.size());
            animateSelection(holder.itemView);
        }

        notifyItemChanged(holder.getAdapterPosition());

        if (selectionModeListener != null) {
            selectionModeListener.onSelectionChanged(selectedPlaceIds.size());
        }

        if (selectedPlaceIds.isEmpty()) {
            setSelectionMode(false);
        }
    }

    private void animateSelection(View view) {
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.95f);
        scaleDown.setDuration(100);

        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1.0f);
        scaleUp.setDuration(100);
        scaleUp.setStartDelay(100);

        scaleDown.start();
        scaleUp.start();
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView name;
        TextView statusBadge;
        TextView address;
        ViewPager2 museumImage;
        CheckBox checkboxSelect;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            name = itemView.findViewById(R.id.name);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            address = itemView.findViewById(R.id.itemAddress);
            museumImage = itemView.findViewById(R.id.museumImage);
            checkboxSelect = itemView.findViewById(R.id.checkboxSelect);
        }
    }
}