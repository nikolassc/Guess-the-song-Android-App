package com.example.gsong;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gsong.data.SongDao;
import com.example.gsong.data.SongDatabase;
import com.example.gsong.models.Song;

public class AddSongActivity extends AppCompatActivity {

    EditText editTextTitle, editTextArtist;
    Button buttonAdd;

    SongDao songDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_song);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Προσθήκη Νέου Τραγουδιού");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // κουμπί "πίσω"

        // Views
        editTextTitle = findViewById(R.id.edit_text_title);
        editTextArtist = findViewById(R.id.edit_text_artist);
        buttonAdd = findViewById(R.id.button_add_song);

        // DAO
        songDao = SongDatabase.getInstance(this).songDao();

        buttonAdd.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String artist = editTextArtist.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(artist)) {
                Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show();
                return;
            }

            // Έλεγχος αν υπάρχει ήδη τραγούδι με αυτόν τον τίτλο
            Song existingSong = songDao.getSongByTitle(title);
            if (existingSong != null) {
                Toast.makeText(this, "Το τραγούδι υπάρχει ήδη στη βάση!", Toast.LENGTH_LONG).show();
                return;
            }

            // Αν δεν υπάρχει, το προσθέτουμε
            Song newSong = new Song(title, artist);
            songDao.insert(newSong);

            Toast.makeText(this, "Το τραγούδι προστέθηκε επιτυχώς!", Toast.LENGTH_SHORT).show();

            // Καθαρίζουμε τα πεδία
            editTextTitle.setText("");
            editTextArtist.setText("");
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
