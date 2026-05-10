package com.example.myapplication.data.supabase;

public class SupabaseUser {
    private final String uid;
    private final String email;

    public SupabaseUser(String uid, String email) {
        this.uid   = uid;
        this.email = email;
    }

    public String getUid()         { return uid; }
    public String getEmail()       { return email; }
    public String getDisplayName() { return email; }
}