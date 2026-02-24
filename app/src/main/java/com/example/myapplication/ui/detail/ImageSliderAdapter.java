package com.example.myapplication.ui.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {
    private final List<String> imageIds;
    private final IImageClick click;

    public ImageSliderAdapter(List<String> imageIds) {
        this.imageIds = imageIds;
        this.click = null;
    }

    public ImageSliderAdapter(List<String> imageIds, IImageClick clickListener) {
        this.imageIds = imageIds;
        this.click = clickListener;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.slider_item, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SliderViewHolder holder, int position) {
        String imageName = imageIds.get(position);
        Context context = holder.imageView.getContext();

        int resId = context.getResources().getIdentifier(
                imageName, "drawable", context.getPackageName()
        );

        if (resId != 0) {
            holder.imageView.setImageResource(resId);
        } else {
            holder.imageView.setImageResource(R.drawable.natioanal_hud_museum_1920x1280);
        }

        holder.imageView.setOnClickListener(v -> {
            if (click != null) {
                click.onImageClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageIds.size();
    }

    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImage);
        }
    }
}
