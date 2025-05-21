package com.example.gsong.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity
public class Song implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String fileName;

    // Default constructor (χρειάζεται για Room)
    public Song() {
    }

    // Constructor με παραμέτρους (για χρήση στον AddSongActivity)
    public Song(String title, String fileName) {
        this.title = title;
        this.fileName = fileName;
    }

    // Προαιρετικά: μπορείς να προσθέσεις και getters/setters αν θέλεις να είσαι πιο OOP
}

