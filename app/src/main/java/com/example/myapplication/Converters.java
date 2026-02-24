package com.example.myapplication;

import androidx.room.TypeConverter;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Converters {
    @TypeConverter
    public static ArrayList<Integer> readDB(String val) {
        Type listType = new TypeToken<ArrayList<Integer>>() {}.getType();
        return new Gson().fromJson(val, listType);
    }

    @TypeConverter
    public static String saveDB(ArrayList<Integer>list){
        Gson gson = new Gson();
        return gson.toJson(list);

    }}
