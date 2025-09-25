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

    // Δήλωση των βασικών κουμπιών της αρχικής οθόνης
    Button buttonStartGame, buttonHowToPlay, buttonStats, buttonExit;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Προφόρτωση των τραγουδιών από το αρχείο JSON μόνο την πρώτη φορά
        preloadSongsFromJson(this);

        // Σύνδεση των μεταβλητών με τα κουμπιά του layout
        buttonStartGame = findViewById(R.id.button_start_game);
        buttonHowToPlay = findViewById(R.id.button_how_to_play);
        buttonStats = findViewById(R.id.buttonStats);
        buttonExit = findViewById(R.id.button_exit);

        // Εκκίνηση της οθόνης επιλογής είδους μουσικής
        buttonStartGame.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.example.gsong.GenreSelectionActivity.class);
            startActivity(intent);
        });


        // Εμφάνιση διαλόγου "Πώς να παίξεις"
        buttonHowToPlay.setOnClickListener(v -> showHowToPlayDialog());

        // Εμφάνιση της οθόνης στατιστικών
        buttonStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        // Επιβεβαίωση εξόδου από την εφαρμογή
        buttonExit.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Exit")
                    .setMessage("Are you sure you want to close the app")
                    .setPositiveButton("Yes", (dialog, which) -> finishAffinity()) // Κλείνει την εφαρμογή
                    .setNegativeButton("No", null) // Ακύρωση
                    .show();
        });
    }

    // Εμφάνιση του custom dialog "How To Play"
    private void showHowToPlayDialog() {
        HowToPlayDialogFragment dialogFragment = new HowToPlayDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "how_to_play");
    }

    /**
     * Φορτώνει τα τραγούδια από το αρχείο assets/songs.json και τα αποθηκεύει στη βάση αν δεν υπάρχουν ήδη.
     */
    private void preloadSongsFromJson(Context context) {
        SongDao songDao = SongDatabase.getInstance(context).songDao();

        try {
            // Άνοιγμα του αρχείου JSON από τα assets
            InputStream is = context.getAssets().open("songs.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            // Μετατροπή σε string
            String json = new String(buffer, StandardCharsets.UTF_8);

            // Χρήση Gson για μετατροπή του JSON σε λίστα αντικειμένων Song
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Song>>() {}.getType();
            List<Song> songList = gson.fromJson(json, listType);

            // Λήψη ήδη αποθηκευμένων τραγουδιών
            List<Song> existingSongs = songDao.getAllSongs();

            // Προσθήκη τραγουδιών που δεν υπάρχουν ήδη στη βάση
            for (Song song : songList) {
                boolean alreadyExists = false;
                for (Song existing : existingSongs) {
                    if (existing.title.equals(song.title)) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    songDao.insert(song); // Εισαγωγή νέου τραγουδιού
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Αν υπάρξει πρόβλημα με το διάβασμα του αρχείου
        }
    }
}
