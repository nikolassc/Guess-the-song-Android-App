package com.example.gsong;

import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.example.gsong.data.SongDao;
import com.example.gsong.data.SongDatabase;
import com.example.gsong.models.Song;

import java.io.Serializable;
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

    private int currentQuestionIndex = 0;
    private String selectedAnswer = null;
    private String currentVinyl = null;

    //Animations of disks array
    private final String[] vinylFiles = {
            "vinyl1.json",
            "vinyl2.json",
            "vinyl3.json",
            "vinyl4.json",
            "vinyl5.json"
    };


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

        //Saved instance after the rotate
        int seekPosition = 0;
        if (savedInstansedState != null) {
            currentQuestionIndex = savedInstansedState.getInt("currentQuestionIndex", 0);
            selectedAnswer = savedInstansedState.getString("selectedAnswer");
            seekPosition = savedInstansedState.getInt("songPosition", 0);
            currentVinyl = savedInstansedState.getString("currentVinyl");

            Serializable played = savedInstansedState.getSerializable("playedSongs");
            if (played instanceof ArrayList<?>) {
                ArrayList<?> rawList = (ArrayList<?>) played;
                if (!rawList.isEmpty() && rawList.get(0) instanceof Song) {
                    playedSongs = (ArrayList<Song>) rawList;
                } else if (rawList.isEmpty()) {
                    playedSongs = new ArrayList<>();
                }
            }

            Serializable song = savedInstansedState.getSerializable("rightSong");
            if (song instanceof Song) {
                rightSong = (Song) song;
            }

            if (rightSong != null && !playedSongs.isEmpty()) {
                feedbackText.setText("Choose a Song");
                reloadSameSong(seekPosition);
            } else {
                // fallback
                playedSongs = new ArrayList<>();
                loadNextSong();
            }
        } else {
            loadNextSong();
        }

        //Listener for every button
        View.OnClickListener listener = v -> {
            Button clicked = (Button) v;
            checkAnswer(clicked.getText().toString(), clicked);
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

    }

    private void reloadSameSong(int seek){
        //Make the sound
        int songID = getResources().getIdentifier(rightSong.fileName, "raw", getPackageName());
        if(songID != 0){
            mediaPlayer = MediaPlayer.create(this, songID);
            mediaPlayer.seekTo(seek);
            mediaPlayer.start();
        }

        List<String> options = new ArrayList<>();
        options.add(rightSong.title);

        List<Song> wrongOptionsPool = new ArrayList<>(allSongs);
        wrongOptionsPool.remove(rightSong);
        Collections.shuffle(wrongOptionsPool);
        for (int i = 0; i < 3 && i < wrongOptionsPool.size(); i++) {
            options.add(wrongOptionsPool.get(i).title);
        }

        Collections.shuffle(options);
        btn1.setText(options.get(0));
        btn2.setText(options.get(1));
        btn3.setText(options.get(2));
        btn4.setText(options.get(3));

        enableAllOptions();

        if(currentVinyl == null){
            currentVinyl = vinylFiles[new Random().nextInt(vinylFiles.length)];
        }

        // Vinyl animation
        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        vinylAnimation.setAnimation(currentVinyl);
        vinylAnimation.setRepeatCount(LottieDrawable.INFINITE);
        vinylAnimation.setRepeatMode(LottieDrawable.RESTART);
        vinylAnimation.playAnimation();
    }

    //Save the current state
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentQuestionIndex", currentQuestionIndex);
        outState.putString("selectedAnswer", selectedAnswer);
        outState.putSerializable("playedSongs", new ArrayList<>(playedSongs));
        outState.putSerializable("rightSong", (Serializable) rightSong);
        outState.putString("currentVinyl", currentVinyl);

        if(mediaPlayer != null){
            try{
                outState.putInt("songPosition", mediaPlayer.getCurrentPosition());
            }catch (IllegalStateException e){
                outState.putInt("songPosition", 0);
            }
        }
    }
    private void loadNextSong(){
        // Stop previous song
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        vinylAnimation.pauseAnimation();;

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

       enableAllOptions();

        // Play audio
        int songId = getResources().getIdentifier(rightSong.fileName, "raw", getPackageName());
        if(songId != 0){
            mediaPlayer = MediaPlayer.create(this, songId);
            mediaPlayer.start();
        } else {
            feedbackText.setText("Audio file not found: " + rightSong.fileName);
        }

        // Randomly select one of the vinyl animations
        Random rand = new Random();
        currentVinyl = vinylFiles[rand.nextInt(vinylFiles.length)];

        // Find the Lottie view and play the animation
        vinylAnimation.setAnimation(currentVinyl);
        vinylAnimation.setRepeatCount(LottieDrawable.INFINITE);
        vinylAnimation.setRepeatMode(LottieDrawable.RESTART);
        vinylAnimation.playAnimation();
    }

    private void checkAnswer(String answer, Button clickedButton) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }

        boolean isCorrect = answer.equals(rightSong.title);

        // Disable all buttons immediately
        disableAllOptions();

        // Feedback text (can adjust here if needed)
        feedbackText.setText("");

        // Show pressed state (optional visual feedback)
        clickedButton.setBackgroundResource(R.drawable.button_background);

        new Handler().postDelayed(() -> {
            if (isCorrect) {
                // Correct answer transition
                TransitionDrawable transition = (TransitionDrawable) ContextCompat.getDrawable(this, R.drawable.button_correct_transition);
                clickedButton.setBackground(transition);
                transition.startTransition(400);
            } else {
                // Wrong answer transition
                TransitionDrawable wrongTransition = (TransitionDrawable) ContextCompat.getDrawable(this, R.drawable.button_wrong_transition);
                clickedButton.setBackground(wrongTransition);
                wrongTransition.startTransition(400);

                // Highlight the correct answer
                for (Button btn : new Button[]{btn1, btn2, btn3, btn4}) {
                    if (btn.getText().toString().equals(rightSong.title)) {
                        new Handler().postDelayed(() -> {
                            TransitionDrawable correctTransition = (TransitionDrawable)
                                    ContextCompat.getDrawable(this, R.drawable.button_correct_transition);
                            btn.setBackground(correctTransition);
                            correctTransition.startTransition(400);
                        }, 600); // delay after wrong shown
                        break;
                    }
                }

            }

        }, 250);

        // Stop animation
        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        vinylAnimation.pauseAnimation();

    }



    private void resetButtons(Button btn) {
        btn.setBackgroundResource(R.drawable.button_background);
        btn.setTextColor(ContextCompat.getColor(this, R.color.text));
    }
    private void disableAllOptions(){
        btn1.setEnabled(false);
        btn2.setEnabled(false);
        btn3.setEnabled(false);
        btn4.setEnabled(false);
    }

    private void enableAllOptions(){
        resetButtons(btn1);
        resetButtons(btn2);
        resetButtons(btn3);
        resetButtons(btn4);

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
            mediaPlayer = null;
        }

        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        if (vinylAnimation != null) {
            vinylAnimation.pauseAnimation();
        }
    }

}
