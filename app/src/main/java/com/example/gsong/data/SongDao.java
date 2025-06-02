package com.example.gsong.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.gsong.models.Song;
import java.util.List;

/**
 * Data Access Object (DAO) interface for performing database operations related to the Song table
 */
@Dao
public interface SongDao {

    /**
     * Returns all songs from the database
     * @return A list of all Song objects
     */
    @Query("SELECT * FROM Song")
    List<Song> getAllSongs();

    /**
     * Returns a random song from the database
     * @return A randomly selected Song.
     */
    @Query("SELECT * FROM Song ORDER BY RANDOM() LIMIT 1")
    Song getRandomSong();

    /**
     * Finds a song by its title.
     * @param title = the title to search for
     * @return The Song object with the given title (if found)
     */
    @Query("SELECT * FROM Song WHERE title = :title LIMIT 1")
    Song getSongByTitle(String title);

    /**
     * Deletes all entries from the Song table
     */
    @Query("DELETE FROM Song")
    void clearAll();

    /**
     * Inserts a single Song into the database
     * @param song = the Song to insert
     */
    @Insert
    void insert(Song song);

    /**
     * Inserts multiple songs into the database
     * @param songs = the songs to insert
     */
    @Insert
    void insertAll(Song... songs);
}

