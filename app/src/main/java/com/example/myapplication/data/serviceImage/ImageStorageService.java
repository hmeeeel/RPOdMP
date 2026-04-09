package com.example.myapplication.data.serviceImage;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

public class ImageStorageService {

    private static final String TAG = "ImageStorageService";
    private static final String IMAGE_DIRECTORY = "museum_images";

    private final Context context;

    public ImageStorageService(Context context) {
        this.context = context.getApplicationContext();
    }

    public String saveImageToInternalStorage(Uri sourceUri) {
        try {

            // 1. создаем папку
            File directory = new File(context.getFilesDir(), IMAGE_DIRECTORY);
            if (!directory.exists()) directory.mkdirs();

            // 2. ген имя - Universally Unique Identifier 128
            String fileName = "img_" + UUID.randomUUID().toString() + ".jpg";
            File destinationFile = new File(directory, fileName);

            // 3. открываем поток чтения из Uri
            // 4. открываем поток записи в новый файл
            try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

                if (inputStream == null) {
                    Log.e(TAG, "Cannot open input stream");
                    return null;
                }

                // 5. коп данные
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

            Log.d(TAG, "Image saved: " + fileName);
            return fileName;

        } catch (Exception e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    public boolean deleteImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;

        if (isDrawableResource(fileName)) return false;

        try {
            File directory = new File(context.getFilesDir(), IMAGE_DIRECTORY);
            File file = new File(directory, fileName);

            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d(TAG, "Image deleted: " + fileName + " = " + deleted);
                return deleted;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image", e);
        }

        return false;
    }

    public String getImagePath(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;

        if (isDrawableResource(fileName)) return fileName;

        File directory = new File(context.getFilesDir(), IMAGE_DIRECTORY);
        File file = new File(directory, fileName);

        if (file.exists()) return file.getAbsolutePath();

        return null;
    }

    private boolean isDrawableResource(String fileName) {
        return fileName != null && !fileName.startsWith("img_");
    }

    public void deleteImages(ArrayList<String> fileNames) {
        if (fileNames == null) return;

        for (String fileName : fileNames) {
            deleteImage(fileName);
        }
    }
}