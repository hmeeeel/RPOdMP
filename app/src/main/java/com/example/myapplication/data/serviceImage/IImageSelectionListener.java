package com.example.myapplication.data.serviceImage;

import android.net.Uri;
import java.util.List;

public interface IImageSelectionListener {
    void onImagesSelected(List<Uri> imageUris);
    void onImageSelectionError(String error);
}