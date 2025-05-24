package com.example.gsong;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
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
    private float currentVolume = 1.0f; // Default 100%

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
        startNewGame();
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
            fade.setDuration(200);
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
            mediaPlayer.start();

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
