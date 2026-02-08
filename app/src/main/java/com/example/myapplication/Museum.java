package com.example.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

public class Museum implements Parcelable {
    private String name;
    private int imageId;
    private String descriprion;
    private String phone;
    private String website;

    public Museum (String name, int imageId, String descriprion, String phone, String website){

        this.name = name;
        this.imageId = imageId;
        this.descriprion = descriprion;
        this.phone = phone;
        this.website = website;
    }
    public Museum (String name, int imageId){
        this.name = name;
        this.imageId = imageId;
        this.descriprion = "";
        this.phone = "";
        this.website = "";

    }
    public String getName() { return this.name; }
    public void setName(String name){ this.name = name; }
    public int getImageId() { return this.imageId; }
    public void setImageId(int imageId) { this.imageId = imageId; }
    public String getDescriprion() { return descriprion; }
    public void setDescriprion(String descriprion) { this.descriprion = descriprion; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    private Museum(Parcel in){
        name = in.readString();
        imageId = in.readInt();
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
        dest.writeString(name);
        dest.writeInt(imageId);
        dest.writeString(descriprion);
        dest.writeString(phone);
        dest.writeString(website);
    }
}
