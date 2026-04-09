package com.example.myapplication.data.serviceImage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;


import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    private final Context context;
    private final List<String> imageFileNames;
    private final OnImageRemoveListener removeListener;
    private final ImageStorageService imageService;

    public ImagePreviewAdapter(Context context, List<String> imageFileNames, OnImageRemoveListener removeListener) {
        this.context = context;
        this.imageFileNames = imageFileNames;
        this.removeListener = removeListener;
        this.imageService = new ImageStorageService(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String fileName = imageFileNames.get(position);
        String imagePath = imageService.getImagePath(fileName);
        if (imagePath == null) imagePath = fileName;

        ImageLoader.loadImage(context, imagePath, holder.imageView);

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageFileNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.previewImage);
            btnRemove = itemView.findViewById(R.id.btnRemoveImage);
        }
    }
}