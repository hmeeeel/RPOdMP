package com.example.myapplication;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Converters {
    @TypeConverter
    public static ArrayList<String> readDB(String val) {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return new Gson().fromJson(val, listType);
    }

    @TypeConverter
    public static String saveDB(ArrayList<String>list){
        Gson gson = new Gson();
        return gson.toJson(list);

    }}
