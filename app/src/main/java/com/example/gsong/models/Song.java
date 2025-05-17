package com.example.gsong.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity
public class Song implements Serializable{

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String fileName;
}
