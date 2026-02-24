package com.example.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.ArrayList;

@Entity(tableName = "museums")
@TypeConverters(Converters.class)
public class Museum implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private ArrayList<Integer> imageIds;
    private String descriprion;
    private String phone;
    private String website;
    public Museum() {
    }
    public Museum (String name, ArrayList<Integer> imageIds, String descriprion, String phone, String website){

        this.name = name;
        this.imageIds = imageIds;
        this.descriprion = descriprion;
        this.phone = phone;
        this.website = website;
    }
    public Museum (String name, ArrayList<Integer> imageIds){
        this.name = name;
        this.imageIds = imageIds;
        this.descriprion = "";
        this.phone = "";
        this.website = "";

    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return this.name; }
    public void setName(String name){ this.name = name; }
    public ArrayList<Integer> getImageIds() { return imageIds; }
    public void setImageIds(ArrayList<Integer> imageIds) { this.imageIds = imageIds; }

    public String getDescriprion() { return descriprion; }
    public void setDescriprion(String descriprion) { this.descriprion = descriprion; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    private Museum(Parcel in){
        id = in.readInt();
        name = in.readString();
        imageIds = new ArrayList<>();
        in.readList(imageIds, Integer.class.getClassLoader());
        descriprion = in.readString();
        phone = in.readString();
        website = in.readString();
    }

    public static Creator<Museum> CREATOR = new Creator<Museum>() {
        @Override
        public Museum createFromParcel(Parcel source) {
            return new Museum(source);
        }

        @Override
        public Museum[] newArray(int size) {
            return new Museum[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeList(imageIds);
        dest.writeString(descriprion);
        dest.writeString(phone);
        dest.writeString(website);
    }
}
