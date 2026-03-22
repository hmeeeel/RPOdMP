package com.example.myapplication.ui.map;

import com.example.myapplication.data.model.CachedPlace;
import com.example.myapplication.data.model.Place;

/** Карта:
 *   - Place (сохранённые пользователем места из БД)
 *   - CachedPlace (результаты поиска Яндекс API)
 */
public class MapMarker {
    public enum MarkerType {SAVED_VISITED, SAVED_PLANNED, API_RESULT}

    private final String name;
    private final String address;
    private final String extraInfo;
    private final double latitude;
    private final double longitude;
    private final MarkerType markerType;
    private final Place savedPlace;

    // сохран места (Place из БД). isVisited
    public static MapMarker fromPlace(Place place) {
        MarkerType type = place.isVisited()
                ? MarkerType.SAVED_VISITED
                : MarkerType.SAVED_PLANNED;

        String extra = "";
        if (place.getWorkingHours() != null && !place.getWorkingHours().isEmpty()) {
            extra = place.getWorkingHours();
        } else if (place.getPhone() != null && !place.getPhone().isEmpty()) {
            extra = place.getPhone();
        }

        return new MapMarker(
                place.getName(),
                place.getAddress() != null ? place.getAddress() : "",
                extra,
                place.getLatitude(),
                place.getLongitude(),
                type,
                place
        );
    }

    // CachedPlace - из рез API ().не сохранено пол
    public static MapMarker fromCachedPlace(CachedPlace cached) {
        String extra = "";
        if (cached.getWorkingHours() != null && !cached.getWorkingHours().isEmpty()) {
            extra = cached.getWorkingHours();
        } else if (cached.getPhone() != null && !cached.getPhone().isEmpty()) {
            extra = cached.getPhone();
        }

        return new MapMarker(
                cached.getName(),
                cached.getAddress() != null ? cached.getAddress() : "",
                extra,
                cached.getLatitude(),
                cached.getLongitude(),
                MarkerType.API_RESULT,
                null // не сохранён
        );
    }

    private MapMarker(String name, String address, String extraInfo,
                      double latitude, double longitude,
                      MarkerType markerType, Place savedPlace) {
        this.name = name;
        this.address = address;
        this.extraInfo = extraInfo;
        this.latitude = latitude;
        this.longitude = longitude;
        this.markerType = markerType;
        this.savedPlace = savedPlace;
    }


    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getExtraInfo() { return extraInfo; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public MarkerType getMarkerType() { return markerType; }

    public Place getSavedPlace() { return savedPlace; }

    public boolean isSaved() { return savedPlace != null; }

    public String getSnackbarText() {
        StringBuilder sb = new StringBuilder(name);
        if (!address.isEmpty()) sb.append("\n").append(address);
        if (!extraInfo.isEmpty()) sb.append("\n").append(extraInfo);
        return sb.toString();
    }
}