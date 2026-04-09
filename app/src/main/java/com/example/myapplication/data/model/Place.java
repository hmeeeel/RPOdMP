package com.example.myapplication.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.myapplication.data.db.Converters;

import java.util.ArrayList;

@Entity(tableName = "places")
@TypeConverters(Converters.class)
public class Place implements Parcelable {
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_YANDEX = "YANDEX";
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;

    // Поля из Яндекса
    private String address;
    private String phone;
    private String workingHours;
    private String yandexId;
    private double latitude;
    private double longitude;

    // Личные поля
    private String description;
    private String website;
    private float rating;
    private long visitDate;
    private ArrayList<String> imageIds;

    // Служебные поля
    private boolean isVisited;
    private String source;
    private long createdAt;

 // ID документа в Firestore. Передаётся между Activity через Parcel
    @Ignore // не хранится в Room, живёт только в памяти
    private String firestoreId;

    public Place() {}

    public static Place createManual(String name, String description,
                                     String phone, String website) {
        Place place = new Place();
        place.name = name;
        place.description = description;
        place.phone = phone;
        place.website = website;
        place.source = SOURCE_MANUAL;
        place.isVisited = false;
        place.createdAt = System.currentTimeMillis();
        place.imageIds = new ArrayList<>();
        place.imageIds.add("no_image");
        return place;
    }

    public static Place fromCachedPlace(CachedPlace cached) {
        Place place = new Place();
        place.name = cached.getName();
        place.address = cached.getAddress();
        place.phone = cached.getPhone();
        place.workingHours = cached.getWorkingHours();
        place.latitude = cached.getLatitude();
        place.longitude = cached.getLongitude();
        place.source = SOURCE_YANDEX;
        place.isVisited = false;  // true всё что на карте = уже посещено
        place.visitDate = System.currentTimeMillis();
        place.createdAt = System.currentTimeMillis();
        place.imageIds = new ArrayList<>();
        return place;
    }

    public boolean hasCoordinates() {
        return latitude != 0.0 || longitude != 0.0;
    }
    public String getCoordinatesDisplay() {
        if (!hasCoordinates()) return "";
        return String.format("%.4f, %.4f", latitude, longitude);
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }

    public String getYandexId() { return yandexId; }
    public void setYandexId(String yandexId) { this.yandexId = yandexId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public long getVisitDate() { return visitDate; }
    public void setVisitDate(long visitDate) { this.visitDate = visitDate; }

    public ArrayList<String> getImageIds() { return imageIds; }
    public void setImageIds(ArrayList<String> imageIds) { this.imageIds = imageIds; }

    public boolean isVisited() { return isVisited; }
    public void setVisited(boolean visited) { isVisited = visited; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    //Firestore document ID. Не хранится в Room
    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }

    protected Place(Parcel in) {
        id = in.readInt();
        name = in.readString();
        address = in.readString();
        phone = in.readString();
        workingHours = in.readString();
        yandexId = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        description = in.readString();
        website = in.readString();
        rating = in.readFloat();
        visitDate = in.readLong();
        imageIds = new ArrayList<>();
        in.readStringList(imageIds);
        isVisited = in.readByte() != 0; // byte: 1 = true, 0 = false
        source = in.readString();
        createdAt = in.readLong();
        firestoreId = in.readString(); // передаётся между Activity
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(address);
        dest.writeString(phone);
        dest.writeString(workingHours);
        dest.writeString(yandexId);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(description);
        dest.writeString(website);
        dest.writeFloat(rating);
        dest.writeLong(visitDate);
        dest.writeStringList(imageIds);
        dest.writeByte((byte) (isVisited ? 1 : 0));
        dest.writeString(source);
        dest.writeLong(createdAt);
        dest.writeString(firestoreId);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<Place> CREATOR = new Creator<Place>() {
        @Override
        public Place createFromParcel(Parcel in) { return new Place(in); }

        @Override
        public Place[] newArray(int size) { return new Place[size]; }
    };
}