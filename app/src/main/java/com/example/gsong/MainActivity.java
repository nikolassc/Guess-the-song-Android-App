package com.example.gsong;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gsong.data.SongDao;
import com.example.gsong.data.SongDatabase;
import com.example.gsong.models.Song;
import com.example.gsong.ui.theme.HowToPlayDialogFragment;
import com.example.gsong.ui.theme.StatisticsActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import androidx.appcompat.app.AlertDialog;


public class MainActivity extends AppCompatActivity {

    Button buttonStartGame, buttonHowToPlay, buttonStats, buttonExit ;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Προφόρτωση τραγουδιών από JSON (πχ. assets/songs.json)
        preloadSongsFromJson(this);



        // Κουμπιά
        buttonStartGame = findViewById(R.id.button_start_game);
        buttonHowToPlay = findViewById(R.id.button_how_to_play);
        buttonStats = findViewById(R.id.buttonStats);
        buttonExit = findViewById(R.id.button_exit);

        buttonStartGame.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        Button howToPlayButton = findViewById(R.id.button_how_to_play);
        howToPlayButton.setOnClickListener(v -> showHowToPlayDialog());


        buttonStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
                startActivity(intent);
            }
        });

        buttonExit.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Exit")
                    .setMessage("Are you sure you want to close the app")
                    .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                    .setNegativeButton("No", null)
                    .show();
        });


    }

    private void showHowToPlayDialog() {
        HowToPlayDialogFragment dialogFragment = new HowToPlayDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "how_to_play");
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
