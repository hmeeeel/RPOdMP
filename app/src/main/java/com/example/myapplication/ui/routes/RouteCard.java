package com.example.myapplication.ui.routes;

import android.os.Parcel;
import android.os.Parcelable;

public class RouteCard implements Parcelable {
    private String id;
    private String title;
    private String description;
    private String authorId;
    private String authorNickname;
    private String statusCode; // draft | pending | published | unavailable
    private int    pointsCount;
    private double averageRating;
    private int    likesCount;
    private int    reviewsCount;
    private boolean isSaved;
    private String  adminNote;
    private boolean isAuthorCompleted;
    private String  createdAt;

    public RouteCard() {}

    public String  getId()               { return id; }
    public void    setId(String id)      { this.id = id; }
    public String  getTitle()            { return title; }
    public void    setTitle(String t)    { this.title = t; }
    public String  getDescription()      { return description; }
    public void    setDescription(String d) { this.description = d; }
    public String  getAuthorId()         { return authorId; }
    public void    setAuthorId(String a) { this.authorId = a; }
    public String  getAuthorNickname()   { return authorNickname; }
    public void    setAuthorNickname(String n) { this.authorNickname = n; }
    public String  getStatusCode()       { return statusCode; }
    public void    setStatusCode(String s) { this.statusCode = s; }
    public int     getPointsCount()      { return pointsCount; }
    public void    setPointsCount(int p) { this.pointsCount = p; }
    public double  getAverageRating()    { return averageRating; }
    public void    setAverageRating(double r) { this.averageRating = r; }
    public int     getLikesCount()       { return likesCount; }
    public void    setLikesCount(int l)  { this.likesCount = l; }
    public int     getReviewsCount()     { return reviewsCount; }
    public void    setReviewsCount(int r) { this.reviewsCount = r; }
    public boolean isSaved()             { return isSaved; }
    public void    setSaved(boolean s)   { this.isSaved = s; }
    public String  getAdminNote()        { return adminNote; }
    public void    setAdminNote(String a) { this.adminNote = a; }
    public boolean isAuthorCompleted()   { return isAuthorCompleted; }
    public void    setAuthorCompleted(boolean c) { this.isAuthorCompleted = c; }
    public String  getCreatedAt()        { return createdAt; }
    public void    setCreatedAt(String c) { this.createdAt = c; }

    protected RouteCard(Parcel in) {
        id               = in.readString();
        title            = in.readString();
        description      = in.readString();
        authorId         = in.readString();
        authorNickname   = in.readString();
        statusCode       = in.readString();
        pointsCount      = in.readInt();
        averageRating    = in.readDouble();
        likesCount       = in.readInt();
        reviewsCount     = in.readInt();
        isSaved          = in.readByte() != 0;
        adminNote        = in.readString();
        isAuthorCompleted = in.readByte() != 0;
        createdAt        = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(authorId);
        dest.writeString(authorNickname);
        dest.writeString(statusCode);
        dest.writeInt(pointsCount);
        dest.writeDouble(averageRating);
        dest.writeInt(likesCount);
        dest.writeInt(reviewsCount);
        dest.writeByte((byte) (isSaved ? 1 : 0));
        dest.writeString(adminNote);
        dest.writeByte((byte) (isAuthorCompleted ? 1 : 0));
        dest.writeString(createdAt);
    }

    @Override public int describeContents() { return 0; }

    public static final Creator<RouteCard> CREATOR = new Creator<RouteCard>() {
        @Override public RouteCard createFromParcel(Parcel in) { return new RouteCard(in); }
        @Override public RouteCard[] newArray(int size)        { return new RouteCard[size]; }
    };
}