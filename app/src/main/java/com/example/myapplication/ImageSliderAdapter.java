package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {
    private List<Integer> imageIds;
    private IImageClick click;

    public ImageSliderAdapter(List<Integer> imageIds) {
        this.imageIds = imageIds;
        this.click = null;
    }

    public ImageSliderAdapter(List<Integer> imageIds, IImageClick clickListener) {
        this.imageIds = imageIds;
        this.click = clickListener;
    }

    @Override
    public SliderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.slider_item, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SliderViewHolder holder, int position) {
        holder.imageView.setImageResource(imageIds.get(position));
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (click != null) {
                    click.onImageClick();
                }
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
