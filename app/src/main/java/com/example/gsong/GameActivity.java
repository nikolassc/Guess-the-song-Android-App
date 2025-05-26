package com.example.gsong;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.example.gsong.data.SongDao;
import com.example.gsong.data.SongDatabase;
import com.example.gsong.models.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
    private float currentVolume = 1.0f; // Default 100%
    private boolean answered = false;

    private int skipsRemaining = 3;
    private Toast skipToast;

    private List<String> currentOptions;

    private final Handler transitionHandler = new Handler();
    private Runnable correctAnswerRunnable;


    //Animations of disks array
    private final String[] vinylFiles = {
            "vinyl1.json",
            "vinyl2.json",
            "vinyl3.json",
            "vinyl4.json",
            "vinyl5.json"
    };

    //Life Icons
    private ImageView[] lifeIcons;
    private int[] lifeDrawables = {
            R.drawable.mini_drum,
            R.drawable.saxophone,
            R.drawable.violin,
            R.drawable.el_guitar,
            R.drawable.guitar,
            R.drawable.trumpet,
            R.drawable.drums,
            R.drawable.piano
    };
    private int selectedLifeDrawable;

    @Override
    protected void onCreate(Bundle savedInstansedState){
        //Load the songs to the database (only the first time the app is opened)
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("db_reset_done", false)) {
          SongDao songDao = SongDatabase.getInstance(this).songDao();
          songDao.clearAll();
          loadSongsFromJson();
          prefs.edit().putBoolean("db_reset_done", true).apply(); // if you want to reset the database make the value "false"
        }

        super.onCreate(savedInstansedState);
        setContentView(R.layout.activity_game);

        feedbackText = findViewById(R.id.feedback_text);

        lifeIcons = new ImageView[] {
                findViewById(R.id.life1),
                findViewById(R.id.life2),
                findViewById(R.id.life3)
        };

        //connect the buttons with IDs
         btn1 = findViewById(R.id.option1);
         btn2 = findViewById(R.id.option2);
         btn3 = findViewById(R.id.option3);
         btn4 = findViewById(R.id.option4);
         nextBtn = findViewById(R.id.next_button);

        // Settings button
        ImageButton settingsBtn = findViewById(R.id.settings_button);
        settingsBtn.setOnClickListener(v -> showSettingsDialog());

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
            lives = savedInstansedState.getInt("lives", 3);
            selectedLifeDrawable = savedInstansedState.getInt("selectedLifeDrawable", R.drawable.life_placeholder);
            answered = savedInstansedState.getBoolean("answered", false);
            selectedAnswer = savedInstansedState.getString("selectedAnswer", null);
            currentOptions = savedInstansedState.getStringArrayList("currentOptions");
            skipsRemaining = savedInstansedState != null ? savedInstansedState.getInt("skipsRemaining", 3) : 3;



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

            for (int i=0; i<lifeIcons.length; i++){
                lifeIcons[i].setImageResource(selectedLifeDrawable);
                if (i>= lives){
                    lifeIcons[i].setAlpha(0.4f);
                    lifeIcons[i].setColorFilter(Color.rgb(20, 110, 100), PorterDuff.Mode.SRC_IN);
                }else{
                    lifeIcons[i].setAlpha(1f);
                    lifeIcons[i].setColorFilter(null);
                }
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
            startNewGame();
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
            if (!answered) {
                // Skip attempt
                if (skipsRemaining > 0) {
                    skipsRemaining--;
                    showSkipToast(skipsRemaining);
                } else {
                    Toast.makeText(this, "No skips remaining!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            feedbackText.setText("Choose a song");
            enableAllOptions();
            loadNextSong();
        });

    }

    private void showSkipToast(int skipsLeft) {
        TextView skipText = findViewById(R.id.skipStatusText);

        String message = skipsLeft == 0
                ? "No skips remaining"
                : skipsLeft + " skips remaining";

        skipText.setText(message);
        skipText.setAlpha(0f);
        skipText.setVisibility(View.VISIBLE);

        // Fade in
        skipText.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> {
                    // Wait then fade out
                    skipText.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .setStartDelay(1000)
                            .withEndAction(() -> skipText.setVisibility(View.GONE))
                            .start();
                })
                .start();
    }

    private void loadSongsFromJson() {
        try {
            // 1. Άνοιγμα αρχείου JSON από assets
            InputStream is = getAssets().open("songs.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);

            // 2. Κάνουμε parsing σε λίστα Song
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Song>>() {}.getType();
            List<Song> songs = gson.fromJson(json, listType);

            // 3. Κάνουμε insert στη βάση
            SongDao songDao = SongDatabase.getInstance(this).songDao();
            songDao.insertAll(songs.toArray(new Song[0]));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load songs.json", Toast.LENGTH_SHORT).show();
        }
    }


    //Show the menu settings
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_restart) {
            restartGame();
            return true;
        } else if (id == R.id.action_volume) {
            showVolumeDialog();
            return true;
        } else if (id == R.id.action_main_menu) {
            goToMainMenu();
            return true;
        } else if (id == R.id.action_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startNewGame(){
        //Random pick of the life icon
        Random randIcon = new Random();
        selectedLifeDrawable = lifeDrawables[randIcon.nextInt(lifeDrawables.length)];

        for(ImageView icon : lifeIcons){
            icon.setImageResource(selectedLifeDrawable);
            icon.setAlpha(1f);
            icon.setColorFilter(null);
            icon.setVisibility(View.VISIBLE);
        }
    }
    private int lives = 3;
    private void onWrongAnswer(){
        lives--;
        if(lives >= 0 && lives < lifeIcons.length){
            //Make the icons darker (to show the lost life)
            int darker = Color.rgb(20, 110, 100);
            lifeIcons[lives].setColorFilter(darker, PorterDuff.Mode.SRC_IN);

            //Fade animation
            ObjectAnimator fade = ObjectAnimator.ofFloat(lifeIcons[lives], "alpha", 1f, 0.4f);
            fade.setDuration(300);
            fade.start();
        }
        if(lives == 0){
            feedbackText.setText("Game Over!");
            disableAllOptions();
            nextBtn.setEnabled(false);
            showGameOverDialog();
        }

    }

    private void reloadSameSong(int seek){
        //Make the sound
        int songID = getResources().getIdentifier(rightSong.fileName, "raw", getPackageName());
        if(songID != 0){
            mediaPlayer = MediaPlayer.create(this, songID);
            mediaPlayer.seekTo(seek);
            mediaPlayer.setVolume(currentVolume, currentVolume);
            if (!answered) {
                mediaPlayer.start();
            }

            mediaPlayer.setOnCompletionListener(mp -> {
                LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
                if (vinylAnimation != null) {
                    vinylAnimation.cancelAnimation();
                    vinylAnimation.setProgress(0f);
                }
            });
        }

        List<String> options = new ArrayList<>();
        options.add(rightSong.title);

        List<Song> sameLang = new ArrayList<>();
        List<Song> otherLang = new ArrayList<>();

        for (Song s : allSongs) {
            if (s.equals(rightSong)) continue;
            if (s.language.equals(rightSong.language)) {
                sameLang.add(s);
            } else {
                otherLang.add(s);
            }
        }

        Collections.shuffle(sameLang);
        Collections.shuffle(otherLang);

        int needed = 3;

// Add as much options from the same language as we can
        for (int i = 0; i < Math.min(needed, sameLang.size()); i++) {
            options.add(sameLang.get(i).title);
        }
        needed -= Math.min(needed, sameLang.size());

// Fill with other option if needed
        for (int i = 0; i < Math.min(needed, otherLang.size()); i++) {
            options.add(otherLang.get(i).title);
        }


        if (currentOptions != null && currentOptions.size() == 4) {
            btn1.setText(currentOptions.get(0));
            btn2.setText(currentOptions.get(1));
            btn3.setText(currentOptions.get(2));
            btn4.setText(currentOptions.get(3));
        } else {
            Collections.shuffle(options);
            currentOptions = new ArrayList<>(options); //saves the sequence of the buttons
            btn1.setText(currentOptions.get(0));
            btn2.setText(currentOptions.get(1));
            btn3.setText(currentOptions.get(2));
            btn4.setText(currentOptions.get(3));
        }


        enableAllOptions();

        if(currentVinyl == null){
            currentVinyl = vinylFiles[new Random().nextInt(vinylFiles.length)];
        }

        if (answered && selectedAnswer != null) {
            disableAllOptions();

            for (Button btn : new Button[]{btn1, btn2, btn3, btn4}) {
                String btnText = btn.getText().toString();

                if (btnText.equals(rightSong.title)) {
                    TransitionDrawable correctTransition = (TransitionDrawable)
                            ContextCompat.getDrawable(this, R.drawable.button_correct_transition);
                    btn.setBackground(correctTransition);
                    correctTransition.startTransition(400);
                } else if (btnText.equals(selectedAnswer)) {
                    TransitionDrawable wrongTransition = (TransitionDrawable)
                            ContextCompat.getDrawable(this, R.drawable.button_wrong_transition);
                    btn.setBackground(wrongTransition);
                    wrongTransition.startTransition(400);
                }
            }

            nextBtn.setEnabled(true);
        }



        // Vinyl animation
        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        if (vinylAnimation != null) {
            vinylAnimation.cancelAnimation();
            vinylAnimation.setProgress(0f);
            vinylAnimation.setAnimation(currentVinyl);
            vinylAnimation.setRepeatCount(LottieDrawable.INFINITE);
            vinylAnimation.setRepeatMode(LottieDrawable.RESTART);
            vinylAnimation.invalidate();
            if (!vinylAnimation.isAnimating()) {
                vinylAnimation.playAnimation();
            }
        }
        transitionHandler.removeCallbacks(correctAnswerRunnable);
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
        outState.putInt("lives", lives);
        outState.putInt("selectedLifeDrawable", selectedLifeDrawable);
        outState.putBoolean("answered", answered);
        outState.putString("selectedAnswer", selectedAnswer);
        outState.putStringArrayList("currentOptions", new ArrayList<>(currentOptions));
        outState.putInt("skipsRemaining", skipsRemaining);

        if(mediaPlayer != null){
            try{
                outState.putInt("songPosition", mediaPlayer.getCurrentPosition());
            }catch (IllegalStateException e){
                outState.putInt("songPosition", 0);
            }
        }

        outState.putSerializable("rightSong", rightSong);
        if (mediaPlayer != null) {
            outState.putInt("songPosition", mediaPlayer.getCurrentPosition());
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
        if (vinylAnimation != null) {
            vinylAnimation.cancelAnimation();
            vinylAnimation.setProgress(0f);
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
        List<Song> sameLang = new ArrayList<>();
        List<Song> otherLang = new ArrayList<>();

        for (Song s : allSongs) {
            if (s.equals(rightSong)) continue;
            if (s.language.equals(rightSong.language)) {
                sameLang.add(s);
            } else {
                otherLang.add(s);
            }
        }

        Collections.shuffle(sameLang);
        Collections.shuffle(otherLang);

        int needed = 3;

// Add as much options from the same language as we can
        for (int i = 0; i < Math.min(needed, sameLang.size()); i++) {
            options.add(sameLang.get(i).title);
        }
        needed -= Math.min(needed, sameLang.size());

// Fill with other option if needed
        for (int i = 0; i < Math.min(needed, otherLang.size()); i++) {
            options.add(otherLang.get(i).title);
        }

        Collections.shuffle(options);

        // Reset the prev answer
        answered = false;
        selectedAnswer = null;

        // Update UI
        currentOptions = new ArrayList<>(options); // save the sequence of the option buttons

        btn1.setText(currentOptions.get(0));
        btn2.setText(currentOptions.get(1));
        btn3.setText(currentOptions.get(2));
        btn4.setText(currentOptions.get(3));


        enableAllOptions();

        // Play audio
        int songId = getResources().getIdentifier(rightSong.fileName, "raw", getPackageName());

        if(songId != 0){
            mediaPlayer = MediaPlayer.create(this, songId);
            mediaPlayer.setVolume(currentVolume, currentVolume);
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(mp -> {
                if (vinylAnimation != null) {
                    vinylAnimation.cancelAnimation();
                    vinylAnimation.setProgress(0f);
                }
            });
        } else {
            feedbackText.setText("Audio file not found: " + rightSong.fileName);
        }

        // Randomly select one of the vinyl animations
        Random rand = new Random();
        currentVinyl = vinylFiles[rand.nextInt(vinylFiles.length)];

        // Find the Lottie view and play the animation
        if (vinylAnimation != null) {
            vinylAnimation.setAnimation(currentVinyl);
            vinylAnimation.setRepeatCount(LottieDrawable.INFINITE);
            vinylAnimation.setRepeatMode(LottieDrawable.RESTART);
            vinylAnimation.invalidate();
            if (!vinylAnimation.isAnimating()) {
                vinylAnimation.playAnimation();
            }
        }
        transitionHandler.removeCallbacks(correctAnswerRunnable);
    }

    private void checkAnswer(String answer, Button clickedButton) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }

        boolean isCorrect = answer.equals(rightSong.title);
        answered = true;
        selectedAnswer = answer;

        // Disable all buttons immediately
        disableAllOptions();

        //Temporary diasable the next song button
        nextBtn.setEnabled(false);

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

                // Prepare delayed highlight for correct button
                correctAnswerRunnable = () -> {
                    for (Button btn : new Button[]{btn1, btn2, btn3, btn4}) {
                        if (btn.getText().toString().equals(rightSong.title)) {
                            TransitionDrawable correctTransition = (TransitionDrawable)
                                    ContextCompat.getDrawable(this, R.drawable.button_correct_transition);
                            btn.setBackground(correctTransition);
                            correctTransition.startTransition(400);
                        }
                    }
                };
                transitionHandler.postDelayed(correctAnswerRunnable, 600);
                onWrongAnswer();
            }
            nextBtn.setEnabled(true);
        }, 250);

        // Stop animation
        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        vinylAnimation.pauseAnimation();

    }
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.game_settings_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.dialog_background));
        dialog.show();

        SeekBar volumeSeekBar = dialogView.findViewById(R.id.volume_seekbar);
        volumeSeekBar.setMax(100);
        volumeSeekBar.setProgress((int)(currentVolume * 100));

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;
                currentVolume = volume;
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(currentVolume, currentVolume);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        dialogView.findViewById(R.id.btn_restart).setOnClickListener(v -> {
            dialog.dismiss();
            restartGame();
        });

        dialogView.findViewById(R.id.btn_home).setOnClickListener(v -> {
            dialog.dismiss();
            goToMainMenu();
        });

        dialogView.findViewById(R.id.btn_exit).setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
        });
    }

    private void showGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.game_over_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.dialog_background));
        dialog.setCancelable(false);
        dialog.show();

        dialogView.findViewById(R.id.btn_restart).setOnClickListener(v -> {
            dialog.dismiss();
            restartGame();
        });

        dialogView.findViewById(R.id.btn_home).setOnClickListener(v -> {
            dialog.dismiss();
            goToMainMenu();
        });

        dialogView.findViewById(R.id.btn_exit).setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
        });
    }


    private void restartGame() {
        playedSongs.clear();
        currentQuestionIndex = 0;
        selectedAnswer = null;
        feedbackText.setText("Choose a Song");
        enableAllOptions();
        lives = 3;
        skipsRemaining = 3;
        answered = false;
        startNewGame();
        loadNextSong();
    }

    private void showVolumeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adjust Volume");

        final SeekBar volumeSeekBar = new SeekBar(this);
        volumeSeekBar.setMax(100);

        // Default at 100% volume
        volumeSeekBar.setProgress(100);

        builder.setView(volumeSeekBar);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(volume, volume);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void goToMainMenu() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
            vinylAnimation.cancelAnimation();
            vinylAnimation.setProgress(0f);
        }
    }

}
