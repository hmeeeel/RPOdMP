package com.example.myapplication.ui.admin;

public class ModerationRoute {
    private String routeId;
    private String title;
    private String description;
    private String authorNickname;
    private String authorId;
    private String createdAt;
    private int    pointsCount;
    private String pointsPreview;
    private String previousAdminNote;
    private String statusCode;

    public ModerationRoute() {}

    public String getRouteId()            { return routeId; }
    public void   setRouteId(String v)    { routeId = v; }
    public String getTitle()              { return title; }
    public void   setTitle(String v)      { title = v; }
    public String getDescription()        { return description; }
    public void   setDescription(String v){ description = v; }
    public String getAuthorNickname()     { return authorNickname; }
    public void   setAuthorNickname(String v){ authorNickname = v; }
    public String getAuthorId()           { return authorId; }
    public void   setAuthorId(String v)   { authorId = v; }
    public String getCreatedAt()          { return createdAt; }
    public void   setCreatedAt(String v)  { createdAt = v; }
    public int    getPointsCount()        { return pointsCount; }
    public void   setPointsCount(int v)   { pointsCount = v; }
    public String getPointsPreview()      { return pointsPreview; }
    public void   setPointsPreview(String v){ pointsPreview = v; }
    public String getPreviousAdminNote()  { return previousAdminNote; }
    public void   setPreviousAdminNote(String v){ previousAdminNote = v; }
    public String getStatusCode()         { return statusCode; }
    public void   setStatusCode(String v) { statusCode = v; }
}