package com.example.myapplication.data.firestore;

public class UserPathProvider implements PathProvider {
    private final String uid;

    public UserPathProvider(String uid) {
        this.uid = uid;
    }

    @Override
    public String getPlacesPath() {
        return "users/" + uid + "/places";
    }
}
