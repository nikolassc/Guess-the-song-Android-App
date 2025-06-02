package com.example.gsong.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.gsong.models.Song;

/**
 * The Room database class for the app
 * It contains the Song table and provides access to the SongDao for queries
 */
@Database(entities = {Song.class}, version = 2)
public abstract class SongDatabase extends RoomDatabase {
    //Singleton instance of the database
    private static SongDatabase instance;

    /**
     * Returns the Data Access Object (DAO) for the Song entity
     * @return The SongDao object
     */
    public abstract SongDao songDao();

    /**
     * Returns a singleton instance of the SongDatabase.
     * Uses fallbackToDestructiveMigration to clear the database if the schema changes
     * @param context = the application context
     * @return The singleton SongDatabase instance
     */
    public static synchronized SongDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            SongDatabase.class, "song_database")
                    .fallbackToDestructiveMigration() //Clears DB on schema change
                    .allowMainThreadQueries() //TEMPORARY: allows queries on main thread for testing
                    .build();
        }
        return instance;
    }
}

