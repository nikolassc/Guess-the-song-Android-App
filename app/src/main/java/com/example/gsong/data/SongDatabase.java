package com.example.gsong.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.gsong.models.Song;

@Database(entities = {Song.class}, version = 2)
public abstract class SongDatabase extends RoomDatabase {
    private static SongDatabase instance;

    public abstract SongDao songDao();

    public static synchronized SongDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            SongDatabase.class, "song_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()  // Προσωρινά για δοκιμές
                    .build();
        }
        return instance;
    }
}
