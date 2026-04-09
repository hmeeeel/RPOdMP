package com.example.myapplication.ui.detail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.serviceImage.ImageLoader;
import com.example.myapplication.data.serviceImage.ImageStorageService;


import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private final List<String> imageFileNames;
    private final IImageClick clickListener;
    private final ImageStorageService imageService;

    public ImageSliderAdapter(Context context, List<String> imageFileNames) {
        this(context, imageFileNames, null);
    }

    public ImageSliderAdapter(Context context, List<String> imageFileNames, IImageClick clickListener) {
        this.imageFileNames = imageFileNames;
        this.clickListener = clickListener;
        this.imageService = new ImageStorageService(context);
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
        String fileName = imageFileNames.get(position);
        Context context = holder.imageView.getContext();

        String imagePath = imageService.getImagePath(fileName);
        if (imagePath == null) {
            imagePath = fileName;
        }

        ImageLoader.loadImage(context, imagePath, holder.imageView);

        holder.imageView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onImageClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageFileNames.size();
    }

    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImage);
        }
    }
}