package com.example.myapplication.data.repository;

import androidx.annotation.NonNull;

import com.example.myapplication.data.model.CachedPlace;
import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.GeoObjectCollection;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.search.BusinessObjectMetadata;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.SearchType;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.List;

public class YandexSearchRepository {

    public static final String SEARCH_QUERY = "музей";

    private final SearchManager searchManager;
    private Session activeSession;
    private static volatile YandexSearchRepository instance;

    private YandexSearchRepository() {
        searchManager = SearchFactory.getInstance()
                .createSearchManager(SearchManagerType.COMBINED);
    }

    public static YandexSearchRepository getInstance() {
        if (instance == null) {
            synchronized (YandexSearchRepository.class) {
                if (instance == null) {
                    instance = new YandexSearchRepository();
                }
            }
        }
        return instance;
    }

    public interface SearchCallback {
        void onSuccess(List<CachedPlace> results);
        void onError(String message);
    }

    public void searchMuseums(double lat, double lon, SearchCallback callback) {
        if (activeSession != null) {
            activeSession.cancel();
        }

        SearchOptions options = new SearchOptions();
        options.setSearchTypes(SearchType.BIZ.value);
        options.setResultPageSize(20);

        activeSession = searchManager.submit(
                SEARCH_QUERY,
                Geometry.fromPoint(new Point(lat, lon)),
                options,
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull Response response) {
                        callback.onSuccess(parseResponse(response));
                    }

                    @Override
                    public void onSearchError(@NonNull Error error) {
                        callback.onError(error.toString());
                    }
                }
        );
    }

    private List<CachedPlace> parseResponse(Response response) {
        List<CachedPlace> results = new ArrayList<>();

        for (GeoObjectCollection.Item item : response.getCollection().getChildren()) {
            GeoObject obj = item.getObj();
            if (obj == null) continue;

            String name = obj.getName() != null ? obj.getName() : "";
            double lat = 0, lon = 0;

            if (!obj.getGeometry().isEmpty()
                    && obj.getGeometry().get(0).getPoint() != null) {
                Point point = obj.getGeometry().get(0).getPoint();
                lat = point.getLatitude();
                lon = point.getLongitude();
            }

            BusinessObjectMetadata meta = obj.getMetadataContainer()
                    .getItem(BusinessObjectMetadata.class);

            String address = "", phone = "", hours = "";

            if (meta != null) {
                if (meta.getAddress() != null) {
                    address = meta.getAddress().getFormattedAddress();
                }
                if (!meta.getPhones().isEmpty()) {
                    phone = meta.getPhones().get(0).getFormattedNumber();
                }
                if (meta.getWorkingHours() != null) {
                    hours = meta.getWorkingHours().getText();
                }
            }

            results.add(new CachedPlace(SEARCH_QUERY, name, address, lat, lon, phone, hours));
        }

        return results;
    }

    public void cancel() {
        if (activeSession != null) {
            activeSession.cancel();
            activeSession = null;
        }
    }
}
