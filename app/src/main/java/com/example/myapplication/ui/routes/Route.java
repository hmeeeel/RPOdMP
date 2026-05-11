package com.example.myapplication.ui.routes;

import android.os.Parcel;
import android.os.Parcelable;

public class Route implements Parcelable {
    private long id;
    private String title;
    private String description;
    private String categoryId;
    private boolean isPublic;
    private String status;
    private long authorId;
    private long createdAt;

    public Route() {
        this.createdAt = System.currentTimeMillis();
        this.status = "draft";
    }

    public Route(String title, String description, boolean isPublic) {
        this.title = title;
        this.description = description;
        this.isPublic = isPublic;
        this.status = isPublic ? "pending" : "draft";
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    protected Route(Parcel in) {
        id = in.readLong();
        title = in.readString();
        description = in.readString();
        categoryId = in.readString();
        isPublic = in.readByte() != 0;
        status = in.readString();
        authorId = in.readLong();
        createdAt = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(categoryId);
        dest.writeByte((byte) (isPublic ? 1 : 0));
        dest.writeString(status);
        dest.writeLong(authorId);
        dest.writeLong(createdAt);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<Route> CREATOR = new Creator<Route>() {
        @Override
        public Route createFromParcel(Parcel in) { return new Route(in); }

        @Override
        public Route[] newArray(int size) { return new Route[size]; }
    };
}