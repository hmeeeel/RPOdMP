package com.example.myapplication.ui.routes;

public class RouteReview {
    private String id;
    private String userId;
    private String routeId;
    private int    rating;
    private String comment;
    private String authorNickname;
    private String createdAt;

    public RouteReview() {}

    public String getId()                  { return id; }
    public void   setId(String id)         { this.id = id; }
    public String getUserId()              { return userId; }
    public void   setUserId(String u)      { this.userId = u; }
    public String getRouteId()             { return routeId; }
    public void   setRouteId(String r)     { this.routeId = r; }
    public int    getRating()              { return rating; }
    public void   setRating(int r)         { this.rating = r; }
    public String getComment()             { return comment; }
    public void   setComment(String c)     { this.comment = c; }
    public String getAuthorNickname()      { return authorNickname; }
    public void   setAuthorNickname(String n) { this.authorNickname = n; }
    public String getCreatedAt()           { return createdAt; }
    public void   setCreatedAt(String c)   { this.createdAt = c; }
}