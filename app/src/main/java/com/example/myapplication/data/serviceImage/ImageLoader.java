package com.example.myapplication.data.serviceImage;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.R;

import java.io.File;

public class ImageLoader {

    public static void loadImage(Context context, String imagePath, ImageView imageView) {
        if (imagePath == null || imagePath.isEmpty()) {
            loadDefaultImage(context, imageView);
            return;
        }

        if (!imagePath.startsWith("img_") && !imagePath.startsWith("/")) {
            int resId = context.getResources().getIdentifier(
                    imagePath, "drawable", context.getPackageName()
            );

            if (resId != 0) {
                Glide.with(context)
                        .load(resId)
                        .placeholder(R.drawable.bg_minsk_light)
                        .error(R.drawable.no_photo)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(imageView);
            } else {
                loadDefaultImage(context, imageView);
            }
            return;
        }

        if (imagePath.startsWith("/")) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Glide.with(context)
                        .load(imageFile)
                        .placeholder(R.drawable.bg_minsk_light)
                        .error(R.drawable.no_photo)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(imageView);
            } else {
                loadDefaultImage(context, imageView);
            }
            return;
        }

        if (imagePath.startsWith("img_")) {
            ImageStorageService imageService = new ImageStorageService(context);
            String fullPath = imageService.getImagePath(imagePath);

            if (fullPath != null) {
                File imageFile = new File(fullPath);
                if (imageFile.exists()) {
                    Glide.with(context)
                            .load(imageFile)
                            .placeholder(R.drawable.bg_minsk_light)
                            .error(R.drawable.no_photo)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(imageView);
                    return;
                }
            }
        }

        loadDefaultImage(context, imageView);
    }

    private static void loadDefaultImage(Context context, ImageView imageView) {
        Glide.with(context)
                .load(R.drawable.no_photo)
                .into(imageView);
    }
}