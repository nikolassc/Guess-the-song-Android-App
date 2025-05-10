package com.example.gsong.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Song {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String fileName;
}
