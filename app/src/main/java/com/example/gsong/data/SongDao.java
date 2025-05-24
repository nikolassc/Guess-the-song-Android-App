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

    @Query("SELECT * FROM Song WHERE title = :title LIMIT 1")
    Song getSongByTitle(String title);

    @Query("DELETE FROM Song")
    void clearAll();

    @Insert
    void insert(Song song);

    @Insert
    void insertAll(Song... songs);
}
