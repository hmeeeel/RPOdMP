package com.example.myapplication;

public class Museum {
    private String name;
    private int imageId;

    public Museum (String name, int imageId){

        this.name = name;
        this.imageId = imageId;
    }

    public String getName() { return this.name; }
    public void setName(String name){ this.name = name; }

    public int getImageId() {
        return this.imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }
}
