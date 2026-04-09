package com.example.myapplication.data.serviceImage;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class ImagePickerHelper {

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia;
    private final IImageSelectionListener listener;

    public ImagePickerHelper(AppCompatActivity activity, IImageSelectionListener listener) {
        this.listener = listener;

        this.pickMultipleMedia = activity.registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(5),
                uris -> {
                    if (uris != null && !uris.isEmpty()) listener.onImagesSelected(uris);
                     else listener.onImageSelectionError("Не выбрано фото");
                }
        );
    }

    public void pickImages() {
        pickMultipleMedia.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
    }
}