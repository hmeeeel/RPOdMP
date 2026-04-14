package com.example.myapplication.data.firestore;

import android.os.Handler;
import android.os.Looper;

import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.repository.PlaceRepository;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreRepository implements PlaceDataSource {

    private static volatile FirestoreRepository instance;

    private final FirebaseFirestore db;
    private final PathProvider pathProvider;
    private final Handler mainHandler;

    private FirestoreRepository(PathProvider pathProvider) {
        this.pathProvider = pathProvider;
        this.db = FirebaseFirestore.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized void getInstance(PathProvider pathProvider) {
        instance = new FirestoreRepository(pathProvider);
    }

    public static FirestoreRepository getInstance() {
        if (instance == null) throw new IllegalStateException("FirestoreRepository не инициализирован");
        return instance;
    }

    private CollectionReference collection() {
        return db.collection(pathProvider.getPlacesPath());
    }

    @Override
    public void getAll(PlaceRepository.DataCallback<List<Place>> callback) {
        collection().get()
                .addOnSuccessListener(snapshot -> {
                    List<Place> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Place p = fromSnapshot(doc);
                        if (p != null) result.add(p);
                    }
                    mainHandler.post(() -> callback.onSuccess(result));
                })
                .addOnFailureListener(e ->
                        mainHandler.post(() -> callback.onError(e)));
    }

    @Override
    public void getById(String id, PlaceRepository.DataCallback<Place> callback) {
        collection().document(id).get()
                .addOnSuccessListener(doc ->
                        mainHandler.post(() -> callback.onSuccess(fromSnapshot(doc))))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> callback.onError(e)));
    }

    @Override
    public void insert(Place place, PlaceRepository.DataCallback<String> callback) {
        // Генерируем ID до записи
        String placeId = collection().document().getId();
        place.setFirestoreId(placeId);

        collection().document(placeId).set(toMap(place))
                .addOnSuccessListener(aVoid ->
                        mainHandler.post(() -> callback.onSuccess(placeId)))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> callback.onError(e)));
    }

    @Override
    public void update(Place place, PlaceRepository.DataCallback<Void> callback) {
        collection().document(place.getFirestoreId()).set(toMap(place))
                .addOnSuccessListener(aVoid ->
                        mainHandler.post(() -> callback.onSuccess(null)))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> callback.onError(e)));
    }

    @Override
    public void delete(String id, PlaceRepository.DataCallback<Void> callback) {
        collection().document(id).delete()
                .addOnSuccessListener(aVoid ->
                        mainHandler.post(() -> callback.onSuccess(null)))
                .addOnFailureListener(e ->
                        mainHandler.post(() -> callback.onError(e)));
    }

    private Map<String, Object> toMap(Place place) {
        Map<String, Object> map = new HashMap<>();
        map.put("placeId",       safe(place.getFirestoreId()));
        map.put("name",          safe(place.getName()));
        map.put("address",       safe(place.getAddress()));
        map.put("phone",         safe(place.getPhone()));
        map.put("workingHours",  safe(place.getWorkingHours()));
        map.put("latitude",      place.getLatitude());
        map.put("longitude",     place.getLongitude());
        map.put("description",   safe(place.getDescription()));
        map.put("website",       safe(place.getWebsite()));
        map.put("rating",        (double) place.getRating());
        map.put("visitDate",     place.getVisitDate());
        map.put("imageIds",      place.getImageIds() != null ? place.getImageIds() : new ArrayList<String>());
        map.put("isVisited",     place.isVisited());
        map.put("source",        safe(place.getSource()));
        map.put("createdAt",     place.getCreatedAt());
        return map;
    }
    private String safe(String s) {
        return s != null ? s : "";
    }

    private Place fromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        Place place = new Place();
        place.setFirestoreId(doc.getId());

        place.setName(str(doc, "name"));
        place.setAddress(str(doc, "address"));
        place.setPhone(str(doc, "phone"));
        place.setWorkingHours(str(doc, "workingHours"));
        place.setLatitude(dbl(doc, "latitude"));
        place.setLongitude(dbl(doc, "longitude"));
        place.setDescription(str(doc, "description"));
        place.setWebsite(str(doc, "website"));
        place.setRating((float) dbl(doc, "rating"));
        place.setVisitDate(lng(doc, "visitDate"));
        place.setVisited(bool(doc, "isVisited"));
        place.setSource(str(doc, "source"));
        place.setCreatedAt(lng(doc, "createdAt"));

        List<String> imageIds = (List<String>) doc.get("imageIds");
        place.setImageIds(imageIds != null ? new ArrayList<>(imageIds) : new ArrayList<>());

        return place;
    }

    private String str(DocumentSnapshot doc, String key) {
        String v = doc.getString(key);
        return v != null ? v : "";
    }

    private double dbl(DocumentSnapshot doc, String key) {
        Double v = doc.getDouble(key);
        return v != null ? v : 0.0;
    }

    private long lng(DocumentSnapshot doc, String key) {
        Long v = doc.getLong(key);
        return v != null ? v : 0L;
    }

    private boolean bool(DocumentSnapshot doc, String key) {
        Boolean v = doc.getBoolean(key);
        return v != null ? v : false;
    }
}
