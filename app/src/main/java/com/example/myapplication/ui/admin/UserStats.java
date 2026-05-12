package com.example.myapplication.ui.admin;

public class UserStats {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private int    routesCreatedCount;
    private int    routesSavedCount;

    public UserStats() {}

    public String getUserId()               { return userId; }
    public void   setUserId(String v)       { userId = v; }
    public String getNickname()             { return nickname; }
    public void   setNickname(String v)     { nickname = v; }
    public String getAvatarUrl()            { return avatarUrl; }
    public void   setAvatarUrl(String v)    { avatarUrl = v; }
    public int    getRoutesCreatedCount()   { return routesCreatedCount; }
    public void   setRoutesCreatedCount(int v){ routesCreatedCount = v; }
    public int    getRoutesSavedCount()     { return routesSavedCount; }
    public void   setRoutesSavedCount(int v){ routesSavedCount = v; }
}