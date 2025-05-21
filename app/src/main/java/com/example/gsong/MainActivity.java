package com.example.gsong;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gsong.data.SongDao;
import com.example.gsong.data.SongDatabase;
import com.example.gsong.models.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button buttonStartGame, buttonAddSongs, buttonStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Προφόρτωση τραγουδιών από JSON (πχ. assets/songs.json)
        preloadSongsFromJson(this);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Guess the Song");

        // Κουμπιά
        buttonStartGame = findViewById(R.id.button_start_game);
        buttonAddSongs = findViewById(R.id.button_add_songs);
        buttonStats = findViewById(R.id.button_stats);

        buttonStartGame.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        buttonAddSongs.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddSongActivity.class);
            startActivity(intent);
        });

       /* buttonStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });  */
    }

    private void preloadSongsFromJson(Context context) {
        SongDao songDao = SongDatabase.getInstance(context).songDao();

        try {
            InputStream is = context.getAssets().open("songs.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Song>>() {}.getType();
            List<Song> songList = gson.fromJson(json, listType);

            List<Song> existingSongs = songDao.getAllSongs();

            for (Song song : songList) {
                boolean alreadyExists = false;
                for (Song existing : existingSongs) {
                    if (existing.title.equals(song.title)) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    songDao.insert(song);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
