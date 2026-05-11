package com.example.myapplication.ui.routes;

import com.example.myapplication.data.model.Place;

public class RoutePoint {
    private long id;
    private long routeId;
    private long placeId;
    private int pointOrder;
    private Place place;

    public RoutePoint() {}

    public RoutePoint(long placeId, int pointOrder) {
        this.placeId = placeId;
        this.pointOrder = pointOrder;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getRouteId() { return routeId; }
    public void setRouteId(long routeId) { this.routeId = routeId; }

    public long getPlaceId() { return placeId; }
    public void setPlaceId(long placeId) { this.placeId = placeId; }

    public int getPointOrder() { return pointOrder; }
    public void setPointOrder(int pointOrder) { this.pointOrder = pointOrder; }

    public Place getPlace() { return place; }
    public void setPlace(Place place) { this.place = place; }
}