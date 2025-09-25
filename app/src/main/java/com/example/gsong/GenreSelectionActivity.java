package com.example.gsong;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class GenreSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_selection);

        setupGenreButton(R.id.button_pop, "pop","pop.xlsx");
        setupGenreButton(R.id.button_rap, "rap","rap.xlsx");
        setupGenreButton(R.id.button_rock, "rock","rock.xlsx");
        setupGenreButton(R.id.button_entexno, "ελλ.εντεχνο","ελλ.εντεχνο.xlsx");
        setupGenreButton(R.id.button_laiko, "ελλ.λαικο","ελλ.λαικο.xlsx");
        setupGenreButton(R.id.button_ell_pop, "ελλ.ποπ","ελλ.ποπ.xlsx");
        setupGenreButton(R.id.button_ell_trap_rap, "ελλ.τραπ_ραπ","ελλ.τραπ_ραπ.xlsx");
    }

    private void setupGenreButton(int buttonId, String genre,String excelFile) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(v -> {
            // Προχωράμε στο GameActivity (χωρίς να αλλάξει κάτι για τώρα)
            Intent intent = new Intent(GenreSelectionActivity.this, GameActivity.class);
            intent.putExtra("selected_genre", genre);
            intent.putExtra("excel_file", excelFile);
            startActivity(intent);
        });
    }
}
