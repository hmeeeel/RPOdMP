package com.example.myapplication.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_places")
public class CachedPlace {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String searchQuery;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String phone;
    private String workingHours;
    private long lastUpdated;

    public CachedPlace() {}

    public CachedPlace(String searchQuery, String name, String address,
                       double latitude, double longitude,
                       String phone, String workingHours) {
        this.searchQuery = searchQuery;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.workingHours = workingHours;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
