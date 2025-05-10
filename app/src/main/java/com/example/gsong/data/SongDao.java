package com.example.gsong.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.gsong.models.Song;

import java.util.List;

@Dao
public interface SongDao {

    @Query("SELECT * FROM Song")
    List<Song> getAllSongs();

    @Query("SELECT * FROM Song ORDER BY RANDOM() LIMIT 1")
    Song getRandomSong();

    @Insert
    void insert(Song song);
    @Insert
    void insertAll(Song... songs);
}

