package com.example.gsong;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gsong.data.SongDao;
import com.example.gsong.data.SongDatabase;
import com.example.gsong.models.Song;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameActivity extends  AppCompatActivity{
    private MediaPlayer mediaPlayer;
    private TextView feedbackText;
    private Song rightSong;
    private List<Song> allSongs;
    private List<Song> playedSongs = new ArrayList<>();
    private Button btn1, btn2, btn3, btn4, nextBtn;

    @Override
    protected void onCreate(Bundle savedInstansedState){
        super.onCreate(savedInstansedState);
        setContentView(R.layout.activity_game);

        feedbackText = findViewById(R.id.feedback_text);

        //connect the buttons with IDs
         btn1 = findViewById(R.id.option1);
         btn2 = findViewById(R.id.option2);
         btn3 = findViewById(R.id.option3);
         btn4 = findViewById(R.id.option4);
         nextBtn = findViewById(R.id.next_button);

        //Load all the songs
        SongDao songDao = SongDatabase.getInstance(this).songDao();
        allSongs = songDao.getAllSongs();

        //Listener for every button
        View.OnClickListener listener = v -> {
            Button clicked = (Button) v;
            checkAnswer(clicked.getText().toString());
        };
        btn1.setOnClickListener(listener);
        btn2.setOnClickListener(listener);
        btn3.setOnClickListener(listener);
        btn4.setOnClickListener(listener);

        nextBtn.setOnClickListener(v -> {
            feedbackText.setText("Choose a song");
            enableAllOptions();
            loadNextSong();
        });

        //Initial Load
        loadNextSong();
    }

    private void loadNextSong(){
        // Stop previous song
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Filter songs not yet played
        List<Song> remainingSongs = new ArrayList<>(allSongs);
        remainingSongs.removeAll(playedSongs);

        // Check if there are enough songs to continue
        if (remainingSongs.isEmpty()) {
            feedbackText.setText("No more unique songs. Game over!");
            disableAllOptions();
            nextBtn.setEnabled(false);
            return;
        }

        // Pick a random song from the remaining
        Collections.shuffle(remainingSongs);
        rightSong = remainingSongs.get(0);
        playedSongs.add(rightSong);

        // Build options
        List<String> options = new ArrayList<>();
        options.add(rightSong.title);

        // Pick 3 wrong songs
        List<Song> wrongOptionsPool = new ArrayList<>(allSongs);
        wrongOptionsPool.remove(rightSong);
        Collections.shuffle(wrongOptionsPool);
        for (int i = 0; i < 3 && i < wrongOptionsPool.size(); i++) {
            options.add(wrongOptionsPool.get(i).title);
        }

        Collections.shuffle(options);

        // Update UI
        btn1.setText(options.get(0));
        btn2.setText(options.get(1));
        btn3.setText(options.get(2));
        btn4.setText(options.get(3));

        btn1.setEnabled(true);
        btn2.setEnabled(true);
        btn3.setEnabled(true);
        btn4.setEnabled(true);

        // Play audio
        int songId = getResources().getIdentifier(rightSong.fileName, "raw", getPackageName());
        if(songId != 0){
            mediaPlayer = MediaPlayer.create(this, songId);
            mediaPlayer.start();
        } else {
            feedbackText.setText("Audio file not found: " + rightSong.fileName);
        }
    }


    private void checkAnswer(String answer){
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }

        if(answer.equals(rightSong.title)){
            feedbackText.setText("Correct!");
        }
        else{
            feedbackText.setText("Wrong! The correct answer was: " + rightSong.title);
        }
        disableAllOptions();
    }

    private void disableAllOptions(){
        btn1.setEnabled(false);
        btn2.setEnabled(false);
        btn3.setEnabled(false);
        btn4.setEnabled(false);
    }

    private void enableAllOptions(){
        btn1.setEnabled(true);
        btn2.setEnabled(true);
        btn3.setEnabled(true);
        btn4.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

}
