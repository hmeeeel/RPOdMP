package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MuseumRepository {
    private MuseumDAO museumDAO;
    private ExecutorService executorService;
    private Handler mainHandler;
    public MuseumRepository(Context context){
        MuseumDB db = MuseumDB.getInstance(context);
        museumDAO = db.museumDAO();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }
    public void getAllMuseums(DataCallback<List<Museum>> callback) {
        executorService.execute(() -> {
            try {
                List<Museum> museums = museumDAO.getAllMuseums();
                mainHandler.post(() -> callback.onSuccess(museums));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getMuseumById(int id, DataCallback<Museum> callback) {
        executorService.execute(() -> {
            try {
                Museum museum = museumDAO.getMuseumById(id);
                mainHandler.post(() -> callback.onSuccess(museum));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void insertMuseum(Museum museum, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = museumDAO.insertMuseum(museum);
                mainHandler.post(() -> callback.onSuccess(id));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    public void updateMuseum(Museum museum, DataCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                museumDAO.updateMuseum(museum);
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void deleteMuseum(Museum museum, DataCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                museumDAO.deleteMuseum(museum);
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}
