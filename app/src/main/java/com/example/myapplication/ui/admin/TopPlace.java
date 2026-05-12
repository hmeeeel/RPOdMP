package com.example.myapplication.ui.admin;

public class TopPlace {
    private String placeId;
    private String placeName;
    private String placeAddress;
    private String categoryName;
    private int    timesInRoutes30Days;
    private int    totalTimesInRoutes;

    public TopPlace() {}

    public String getPlaceId()                 { return placeId; }
    public void   setPlaceId(String v)         { placeId = v; }
    public String getPlaceName()               { return placeName; }
    public void   setPlaceName(String v)       { placeName = v; }
    public String getPlaceAddress()            { return placeAddress; }
    public void   setPlaceAddress(String v)    { placeAddress = v; }
    public String getCategoryName()            { return categoryName; }
    public void   setCategoryName(String v)    { categoryName = v; }
    public int    getTimesInRoutes30Days()     { return timesInRoutes30Days; }
    public void   setTimesInRoutes30Days(int v){ timesInRoutes30Days = v; }
    public int    getTotalTimesInRoutes()      { return totalTimesInRoutes; }
    public void   setTotalTimesInRoutes(int v) { totalTimesInRoutes = v; }
}