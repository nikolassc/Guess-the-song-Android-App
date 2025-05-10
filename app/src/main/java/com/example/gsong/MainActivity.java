package com.example.gsong;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preloadSongsFromJson(this);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Guess the Song");

        Button gameButton = findViewById(R.id.game_button);
        gameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });
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

