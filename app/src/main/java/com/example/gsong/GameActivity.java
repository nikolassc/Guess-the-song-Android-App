package com.example.gsong;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Main activity for the game mode where the user listens to songs and guesses their titles.
 */

public class GameActivity extends AppCompatActivity {

    //Counter to track the number of correct answers given by the player
    private int correctAnswersCount = 0;

    //Counter to track the number of wrong answers given by the player
    private int wrongAnswersCount = 0;

    //The player's current score in the game
    private int currentScore = 0;

    //MediaPlayer instance for playing song audio
    private MediaPlayer mediaPlayer;

    //TextView that shows feedback to the user "Correct" or "Wrong"
    private TextView feedbackText;

    //The correct song for the current question
    private Song rightSong;

    //List of all songs loaded from the database
    private List<Song> allSongs;

    //List of songs that have already been played in this session to avoid repetition
    private List<Song> playedSongs = new ArrayList<>();

    //Buttons for the four possible answers and the next button
    private Button btn1, btn2, btn3, btn4, nextBtn;

    //Index of the current question
    private int currentQuestionIndex = 0;

    //The answer selected by the player
    private String selectedAnswer = null;

    //The current vinyl animation from json file
    private String currentVinyl = null;

    //Current audio playback volume
    private float currentVolume = 1.0f; //100% volume

    //Flag to indicate if the user has already answered the current question
    private boolean answered = false;

    //Flag to indicate if the Game Over dialog is shown
    private boolean gameOverDialog = false;

    //Flag to indicate if the Settings dialog is shown
    private boolean settingsDialog = false;

    //Number of skips the user has remaining, has 3 skips
    private int skipsRemaining = 3;

    //Stores the current set of answer options (to keep them the same after rotation of the phone)
    private List<String> currentOptions;

    //Handler for managing delayed transitions
    private final Handler transitionHandler = new Handler();

    //Runnable to highlight the correct answer after a delay
    private Runnable correctAnswerRunnable;

    //Flag to track if the music should be resumed after the app was minimized
    private boolean minimizePause = false;

    //Keeps a single instance of the Game Over dialog to prevent recreating it during screen rotation
    private AlertDialog gameOverDialogInstance;

    //Array of json file names for vinyl animations
    private final String[] vinylFiles = {
            "vinyl1.json",
            "vinyl2.json",
            "vinyl3.json",
            "vinyl4.json",
            "vinyl5.json"
    };

    //Array of ImageViews representing the lives (music_instruments/icons)
    private ImageView[] lifeIcons;

    //Array of drawable resource IDs for different life icons for variety
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

    //The specific life icon selected for this game session
    private int selectedLifeDrawable;

    //Class that manages the player's statistics for this game
    private StatisticsManager statisticsManager;

    //Getter for the number of correct answers
    public int getCorrectAnswerCount() {
        return correctAnswersCount;
    }

    //Getter for the number of wrong answers
    public int getWrongAnswerCount() {
        return wrongAnswersCount;
    }

    //Calculates and returns the current score
    public int calculateScore() {
        return correctAnswersCount;
    }

    @Override
    protected void onCreate(Bundle savedInstansedState){
        //Load the songs into the database if it's the first time the app is opened
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("db_reset_done", false)) {
            SongDao songDao = SongDatabase.getInstance(this).songDao();
            songDao.clearAll(); //Clear any existing songs
            loadSongsFromJson(); //Load new songs from the json file
            //Mark the database as initialized to avoid reloading next time
            prefs.edit().putBoolean("db_reset_done", true).apply();
        }

        super.onCreate(savedInstansedState);
        setContentView(R.layout.activity_game);

        if (savedInstansedState != null) {
            gameOverDialog = savedInstansedState.getBoolean("gameOverDialog", false);
            if (gameOverDialog) {
                showGameOverDialog();
            }
        }

        if (savedInstansedState != null) {
            settingsDialog = savedInstansedState.getBoolean("settingsDialog", false);
            if (settingsDialog) {
                showSettingsDialog();
            }
        }

        //Initialize the statistics manager
        statisticsManager = new StatisticsManager(this);

        int highScore = statisticsManager.getHighScore();

        //Get a reference to the feedback text where the app shows "Correct" or "Wrong" feedback
        feedbackText = findViewById(R.id.feedback_text);

        //Initialize the three life icons
        lifeIcons = new ImageView[] {
                findViewById(R.id.life1),
                findViewById(R.id.life2),
                findViewById(R.id.life3)
        };

        //Find and connect all answer buttons and the "Next Song" button
        btn1 = findViewById(R.id.option1);
        btn2 = findViewById(R.id.option2);
        btn3 = findViewById(R.id.option3);
        btn4 = findViewById(R.id.option4);
        nextBtn = findViewById(R.id.next_button);

        //Set up the Settings button to open the settings dialog
        ImageButton settingsBtn = findViewById(R.id.settings_button);
        settingsBtn.setOnClickListener(v -> showSettingsDialog());

        //Load all songs from the database
        SongDao songDao = SongDatabase.getInstance(this).songDao();
        allSongs = songDao.getAllSongs();

        //Check if the activity was recreated (rotation)
        int seekPosition = 0;
        if (savedInstansedState != null) {
            // Restore the current question and game state
            currentQuestionIndex = savedInstansedState.getInt("currentQuestionIndex", 0);
            selectedAnswer = savedInstansedState.getString("selectedAnswer");
            seekPosition = savedInstansedState.getInt("songPosition", 0);
            currentVinyl = savedInstansedState.getString("currentVinyl");
            correctAnswersCount = savedInstansedState.getInt("correctAnswersCount");
            wrongAnswersCount = savedInstansedState.getInt("wrongAnswersCount");
            lives = savedInstansedState.getInt("lives", 3);
            selectedLifeDrawable = savedInstansedState.getInt("selectedLifeDrawable", R.drawable.life_placeholder);
            currentVolume = savedInstansedState.getFloat("currentVolume", 1.0f); // Default to full volume
            answered = savedInstansedState.getBoolean("answered", false);
            selectedAnswer = savedInstansedState.getString("selectedAnswer", null);
            currentOptions = savedInstansedState.getStringArrayList("currentOptions");
            skipsRemaining = savedInstansedState != null ? savedInstansedState.getInt("skipsRemaining", 3) : 3;

            //Restore the list of songs already played
            Serializable played = savedInstansedState.getSerializable("playedSongs");
            if (played instanceof ArrayList<?>) {
                ArrayList<?> rawList = (ArrayList<?>) played;
                if (!rawList.isEmpty() && rawList.get(0) instanceof Song) {
                    playedSongs = (ArrayList<Song>) rawList;
                } else if (rawList.isEmpty()) {
                    playedSongs = new ArrayList<>();
                }
            }

            //Restore the current song
            Serializable song = savedInstansedState.getSerializable("rightSong");
            if (song instanceof Song) {
                rightSong = (Song) song;
            }

            //Restore the visual state of the lives
            for (int i=0; i<lifeIcons.length; i++){
                lifeIcons[i].setImageResource(selectedLifeDrawable);
                if (i >= lives){
                    //Dim the lost lives
                    lifeIcons[i].setAlpha(0.4f);
                    lifeIcons[i].setColorFilter(Color.rgb(20, 110, 100), PorterDuff.Mode.SRC_IN);
                } else {
                    //Normal brightness for remaining lives
                    lifeIcons[i].setAlpha(1f);
                    lifeIcons[i].setColorFilter(null);
                }
            }

            if (rightSong != null && !playedSongs.isEmpty()) {
                //If we have a valid song and state, reload the current song
                feedbackText.setText("Choose a Song");
                reloadSameSong(seekPosition);
            } else {
                //Fallback, start fresh if restore data is incomplete
                playedSongs = new ArrayList<>();
                loadNextSong();
            }
        } else {
            //First launch, start fresh new game
            loadNextSong();
            startNewGame();
        }

        //Set click listeners for the answer buttons
        View.OnClickListener listener = v -> {
            Button clicked = (Button) v;
            checkAnswer(clicked.getText().toString(), clicked);
        };
        btn1.setOnClickListener(listener);
        btn2.setOnClickListener(listener);
        btn3.setOnClickListener(listener);
        btn4.setOnClickListener(listener);

        //Set listener for the "Next Song" button
        nextBtn.setOnClickListener(v -> {
            if (!answered) {
                //If user skips without guessing
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

    /**
     * Displays a temporary message on the screen indicating how many skips remain.
     * It uses fade-in and fade-out animations to smoothly show and hide the message.
     * @param skipsLeft The number of skips the player has remaining.
     */
    private void showSkipToast(int skipsLeft) {
        //Find the TextView that will display the skip status message
        TextView skipText = findViewById(R.id.skipStatusText);

        //Create the message based on the number of skips remaining
        String message = skipsLeft == 0
                ? "No skips remaining"
                : skipsLeft + " skips remaining";

        //Set the text to the TextView and make it fully transparent
        skipText.setText(message);
        skipText.setAlpha(0f);
        skipText.setVisibility(View.VISIBLE);

        //Animate the TextView to fade in (fully visible)
        skipText.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> {
                    //After fade in, wait, then fade out
                    skipText.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .setStartDelay(1000) // Keep it visible for 1 second
                            .withEndAction(() -> skipText.setVisibility(View.GONE))
                            .start();
                })
                .start();
    }

    /**
     * Loads the list of songs from a json file in the app's assets folder and inserts them into the local database
     */
    private void loadSongsFromJson() {
        try {
            // Open the "songs.json" file from the assets folder
            InputStream is = getAssets().open("songs.json");
            int size = is.available(); //Determine file size
            byte[] buffer = new byte[size]; //Create buffer
            is.read(buffer); //Read file contents into buffer
            is.close();

            //Convert the read bytes into a String
            String json = new String(buffer, StandardCharsets.UTF_8);

            //Parse the json into a list of Song objects using Gson
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Song>>() {}.getType();
            List<Song> songs = gson.fromJson(json, listType);

            //Insert the list of songs into the local database
            SongDao songDao = SongDatabase.getInstance(this).songDao();
            songDao.insertAll(songs.toArray(new Song[0]));

        } catch (IOException e) {
            //Handle error if the file couldn't be loaded
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

    //When app is minimized  the music pauses
    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            minimizePause = true;
        } else {
            minimizePause = false;
        }
    }

    //When app is  reopened the music resumes
    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && minimizePause && !answered) {
            mediaPlayer.start();
        }
    }

    /**
     * Handles item selections in the app's menu
     * Each menu item triggers a different action in the game
     * @param item = The selected menu item
     * @return true if the item was handled, false if not
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Get the ID of the selected menu item
        int id = item.getItemId();

        if (id == R.id.action_restart) {
            //Restart the current game
            restartGame();
            return true;
        } else if (id == R.id.action_volume) {
            //Show a dialog to adjust the audio volume
            showVolumeDialog();
            return true;
        } else if (id == R.id.action_main_menu) {
            //Go back to the main menu
            goToMainMenu();
            return true;
        } else if (id == R.id.action_exit) {
            //Exit the game
            finish();
            return true;
        }

        //If no recognized item was selected, call the default implementation
        return super.onOptionsItemSelected(item);
    }

    /**
     * Starts a new game by selecting a random life icon and resetting the life indicators to full visibility.
     */
    private void startNewGame(){
        //Randomly choose one of the available life icons for the new game
        Random randIcon = new Random();
        selectedLifeDrawable = lifeDrawables[randIcon.nextInt(lifeDrawables.length)];

        for (ImageView icon : lifeIcons) {
            icon.setImageResource(selectedLifeDrawable); //Set the drawable image
            icon.setAlpha(1f); //Not faded
            icon.setColorFilter(null); //Remove any color filters
            icon.setVisibility(View.VISIBLE); //Icon is visible
        }
    }
    // Number of lives the player has (starts with 3)
    private int lives = 3;

    /**
     * Handles the logic when the player gives a wrong answer.
     * Decreases the number of lives, visually fades out the lost life icon
     * If no lives remain, ends the game.
     */
    private void onWrongAnswer() {
        //Decrement the number of lives
        lives--;

        if (lives >= 0 && lives < lifeIcons.length) {
            //Make the lost life icon darker to visually indicate the loss
            int darker = Color.rgb(20, 110, 100); //Dark greenish color for effect
            lifeIcons[lives].setColorFilter(darker, PorterDuff.Mode.SRC_IN);

            //Apply fade animation to visually fade out the lost life icon
            ObjectAnimator fade = ObjectAnimator.ofFloat(lifeIcons[lives], "alpha", 1f, 0.4f);
            fade.setDuration(300); //Animation duration
            fade.start();
        }

        if (lives == 0) {
            //If no lives left, show "Game Over!" message
            feedbackText.setText("Game Over!");
            disableAllOptions(); //Disable answer buttons
            nextBtn.setEnabled(false); //Disable next button

            //Record final game statistics
            //   statisticsManager.recordGame(
            //         /* correct= */ getCorrectAnswerCount(),
            //       /* wrong= */ getWrongAnswerCount(),
            //     /* score= */ calculateScore()
            // );

            //Show game over dialog to the user
            showGameOverDialog();
        }
    }

    /**
     * Reloads the current song and restores its state when the activity is recreated (screen rotation)
     * @param seek = The position to seek to
     */
    private void reloadSameSong(int seek) {
        //Load the audio file for the current song
        int songID = getResources().getIdentifier(rightSong.fileName, "raw", getPackageName());
        if (songID != 0) {
            mediaPlayer = MediaPlayer.create(this, songID);
            mediaPlayer.seekTo(seek); //Seek to saved playback position
            mediaPlayer.setVolume(currentVolume, currentVolume);

            if (!answered) {
                //Start playing only if the user hasnt answered yet
                mediaPlayer.start();
            }

            //Stop the vinyl animation when the song finishes
            mediaPlayer.setOnCompletionListener(mp -> {
                LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
                if (vinylAnimation != null) {
                    vinylAnimation.cancelAnimation();
                    vinylAnimation.setProgress(0f);
                }
            });
        }

        //Prepare the 4 answer options
        List<String> options = new ArrayList<>();
        options.add(rightSong.title); //The correct answer

        //Separate wrong options by language
        List<Song> sameLang = new ArrayList<>();
        List<Song> otherLang = new ArrayList<>();

        for (Song s : allSongs) {
            if (s.equals(rightSong)) continue; //Skip the correct song
            if (s.language.equals(rightSong.language)) {
                sameLang.add(s); //Same language
            } else {
                otherLang.add(s); //Different language
            }
        }

        //Shuffle both groups to randomize
        Collections.shuffle(sameLang);
        Collections.shuffle(otherLang);

        int needed = 3; //Need 3 wrong options

        //Add as many same-language wrong options as possible
        for (int i = 0; i < Math.min(needed, sameLang.size()); i++) {
            options.add(sameLang.get(i).title);
        }
        needed -= Math.min(needed, sameLang.size());

        //If not enough, fill with other-language options
        for (int i = 0; i < Math.min(needed, otherLang.size()); i++) {
            options.add(otherLang.get(i).title);
        }

        //If we already have saved option order due to rotation, reuse it
        if (currentOptions != null && currentOptions.size() == 4) {
            btn1.setText(currentOptions.get(0));
            btn2.setText(currentOptions.get(1));
            btn3.setText(currentOptions.get(2));
            btn4.setText(currentOptions.get(3));
        } else {
            //Otherwise, shuffle and save the new order
            Collections.shuffle(options);
            currentOptions = new ArrayList<>(options);
            btn1.setText(currentOptions.get(0));
            btn2.setText(currentOptions.get(1));
            btn3.setText(currentOptions.get(2));
            btn4.setText(currentOptions.get(3));
        }

        //Enable the answer buttons again
        enableAllOptions();

        //If no vinyl animation is set, choose a random one
        if (currentVinyl == null) {
            currentVinyl = vinylFiles[new Random().nextInt(vinylFiles.length)];
        }

        //If the user has already answered, show the result highlight again
        if (answered && selectedAnswer != null) {
            disableAllOptions();

            for (Button btn : new Button[]{btn1, btn2, btn3, btn4}) {
                String btnText = btn.getText().toString();

                if (btnText.equals(rightSong.title)) {
                    //Highlight the correct answer
                    TransitionDrawable correctTransition = (TransitionDrawable)
                            ContextCompat.getDrawable(this, R.drawable.button_correct_transition);
                    btn.setBackground(correctTransition);
                    correctTransition.startTransition(400);
                } else if (btnText.equals(selectedAnswer)) {
                    //Highlight the wrong answer
                    TransitionDrawable wrongTransition = (TransitionDrawable)
                            ContextCompat.getDrawable(this, R.drawable.button_wrong_transition);
                    btn.setBackground(wrongTransition);
                    wrongTransition.startTransition(400);
                }
            }

            //Enable the "Next Song" button so the user can proceed
            nextBtn.setEnabled(true);
        }

        //Start the vinyl animation for this song
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

        //Cancel any pending highlight transitions
        transitionHandler.removeCallbacks(correctAnswerRunnable);
    }


    /**
     * Called before the activity is destroyed (screen rotation)
     * Saves the current state of the game so it can be restored in onCreate().
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the current question index
        outState.putInt("currentQuestionIndex", currentQuestionIndex);

        //Save the answer that the user has selected
        outState.putString("selectedAnswer", selectedAnswer);

        //Save the list of songs that have already been played
        outState.putSerializable("playedSongs", new ArrayList<>(playedSongs));

        //Save the current correct song
        outState.putSerializable("rightSong", (Serializable) rightSong);

        // Save the current volume
        outState.putFloat("currentVolume", currentVolume);

        //Save the vinyl animation that is currently showing
        outState.putString("currentVinyl", currentVinyl);

        //Save the number of lives remaining
        outState.putInt("lives", lives);

        //Save the currently selected life icon
        outState.putInt("selectedLifeDrawable", selectedLifeDrawable);

        //Save the number of correct answers
        outState.putInt("correctAnswersCount", correctAnswersCount);

        //Save the number of wrong answers
        outState.putInt("wrongAnswersCount", wrongAnswersCount);

        //Save whether the user has already answered
        outState.putBoolean("answered", answered);

        //Save the Game Over menu
        outState.putBoolean("gameOverDialog", gameOverDialog);

        //Save the Settings menu
        outState.putBoolean("settingsDialog", settingsDialog);

        //Save the current order of the answer options
        outState.putStringArrayList("currentOptions", new ArrayList<>(currentOptions));

        //Save the number of skips remaining
        outState.putInt("skipsRemaining", skipsRemaining);

        //Save the current song playback position (if playing)
        if (mediaPlayer != null) {
            try {
                outState.putInt("songPosition", mediaPlayer.getCurrentPosition());
            } catch (IllegalStateException e) {
                outState.putInt("songPosition", 0);
            }
        }

        outState.putSerializable("rightSong", rightSong);

        if (mediaPlayer != null) {
            outState.putInt("songPosition", mediaPlayer.getCurrentPosition());
        }
    }

    /**
     * Loads the next song for the player to guess
     * Handles updating the UI, choosing answer options and playing the song
     */
    private void loadNextSong() {
        //Stop and release any previous audio playback
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        //Stop the vinyl animation
        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        if (vinylAnimation != null) {
            vinylAnimation.cancelAnimation();
            vinylAnimation.setProgress(0f);
        }

        //Determine which songs haven't been played yet
        List<Song> remainingSongs = new ArrayList<>(allSongs);
        remainingSongs.removeAll(playedSongs);

        //Check if there are no more unique songs to play
        if (remainingSongs.isEmpty()) {
            //End the game if all songs have been played
            feedbackText.setText("No more unique songs. Game over!");
            disableAllOptions();
            nextBtn.setEnabled(false);

            //Save the player's game statistics
            //  statisticsManager.recordGame(
            //        /* correct= */ getCorrectAnswerCount(),
            //      /* wrong= */ getWrongAnswerCount(),
            //    /* score= */ calculateScore()
            //);
            return;
        }

        //Randomly select a new song from the remaining songs
        Collections.shuffle(remainingSongs);
        rightSong = remainingSongs.get(0);
        playedSongs.add(rightSong); // Mark as played

        //Build the list of answer options (1 correct + 3 wrong)
        List<String> options = new ArrayList<>();
        options.add(rightSong.title); //Include the correct answer

        //Separate songs by same language and other language
        List<Song> sameLang = new ArrayList<>();
        List<Song> otherLang = new ArrayList<>();

        for (Song s : allSongs) {
            if (s.equals(rightSong)) continue; //Skip the correct song
            if (s.language.equals(rightSong.language)) {
                sameLang.add(s);
            } else {
                otherLang.add(s);
            }
        }

        //Shuffle to randomize wrong answer options
        Collections.shuffle(sameLang);
        Collections.shuffle(otherLang);

        int needed = 3; //We need 3 wrong options

        //Fill as much as possible with same-language wrong answers
        for (int i = 0; i < Math.min(needed, sameLang.size()); i++) {
            options.add(sameLang.get(i).title);
        }
        needed -= Math.min(needed, sameLang.size());

        //If still needed, fill with other-language options
        for (int i = 0; i < Math.min(needed, otherLang.size()); i++) {
            options.add(otherLang.get(i).title);
        }

        //Shuffle the final 4 options to randomize their order
        Collections.shuffle(options);

        //Reset previous answers
        answered = false;
        selectedAnswer = null;

        //Save the current option sequence for restoring on rotation
        currentOptions = new ArrayList<>(options);

        //Update the UI with the new options
        btn1.setText(currentOptions.get(0));
        btn2.setText(currentOptions.get(1));
        btn3.setText(currentOptions.get(2));
        btn4.setText(currentOptions.get(3));

        //Enable the answer buttons
        enableAllOptions();

        //Prepare and play the selected song
        int songId = getResources().getIdentifier(rightSong.fileName, "raw", getPackageName());

        if (songId != 0) {
            mediaPlayer = MediaPlayer.create(this, songId);
            mediaPlayer.setVolume(currentVolume, currentVolume);
            mediaPlayer.start();

            //Stop the vinyl animation when the song finishes
            mediaPlayer.setOnCompletionListener(mp -> {
                if (vinylAnimation != null) {
                    vinylAnimation.cancelAnimation();
                    vinylAnimation.setProgress(0f);
                }
            });
        } else {
            //Show an error if the audio file wasn't found
            feedbackText.setText("Audio file not found: " + rightSong.fileName);
        }

        //Randomly choose a vinyl animation to play
        Random rand = new Random();
        currentVinyl = vinylFiles[rand.nextInt(vinylFiles.length)];

        //Set up and start the vinyl animation
        if (vinylAnimation != null) {
            vinylAnimation.setAnimation(currentVinyl);
            vinylAnimation.setRepeatCount(LottieDrawable.INFINITE);
            vinylAnimation.setRepeatMode(LottieDrawable.RESTART);
            vinylAnimation.invalidate();
            if (!vinylAnimation.isAnimating()) {
                vinylAnimation.playAnimation();
            }
        }

        //Remove any delayed highlight transitions
        transitionHandler.removeCallbacks(correctAnswerRunnable);
    }

    /**
     * Handles the logic when the user selects an answer
     * Determines if it is correct, updates the score and lives and visually shows the result (green for correct, red for wrong)
     * @param answer = the answer text selected by the user
     * @param clickedButton = the button the user clicked
     */
    private void checkAnswer(String answer, Button clickedButton) {
        //Stop any playing audio when the user makes a guess
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0); // Reset to start
        }

        //Check if the answer is correct
        boolean isCorrect = answer.equals(rightSong.title);
        answered = true; // Mark this question as answered
        selectedAnswer = answer; // Save the selected answer

        //Update statistics counters
        if (isCorrect) {
            correctAnswersCount++;
            currentScore += 1; //Increment score
        } else {
            wrongAnswersCount++;
        }

        //Disable answer buttons immediately so no more answers can be selected
        disableAllOptions();

        //Temporarily disable the next song button to prevent rapid skipping
        nextBtn.setEnabled(false);

        //Clear feedback text
        feedbackText.setText("");

        //Show pressed state for clicked button (brief flash)
        clickedButton.setBackgroundResource(R.drawable.button_background);

        //Delay to show transitions smoothly
        new Handler().postDelayed(() -> {
            if (isCorrect) {
                //Animate correct answer (green transition effect)
                TransitionDrawable transition = (TransitionDrawable)
                        ContextCompat.getDrawable(this, R.drawable.button_correct_transition);
                clickedButton.setBackground(transition);
                transition.startTransition(400);
            } else {
                //Animate wrong answer (red transition effect)
                TransitionDrawable wrongTransition = (TransitionDrawable)
                        ContextCompat.getDrawable(this, R.drawable.button_wrong_transition);
                clickedButton.setBackground(wrongTransition);
                wrongTransition.startTransition(400);

                //After a delay, highlight the correct answer
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

                //Reduce lives if answer was wrong
                onWrongAnswer();
            }

            //Enable the "Next Song" button again
            nextBtn.setEnabled(true);
        }, 250); //Delay before showing transitions

        //Stop the vinyl animation while showing result
        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        vinylAnimation.pauseAnimation();
    }

    /**
     * Displays the game settings dialog
     * Allows the user to adjust the volume and offers buttons to restart, go to main menu or exit the game
     */
    private void showSettingsDialog() {
        settingsDialog = true;
        //Create a custom AlertDialog with a custom style
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);

        //Inflate the custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.game_settings_dialog, null);
        builder.setView(dialogView);

        //Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.dialog_background));
        dialog.show();

        //Set up the volume seekbar
        SeekBar volumeSeekBar = dialogView.findViewById(R.id.volume_seekbar);
        volumeSeekBar.setMax(100); //Maximum volume value
        volumeSeekBar.setProgress((int)(currentVolume * 100)); //Set the seekbar to the current volume

        //Listener for adjusting volume in real-time
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f; //Convert to 0-1 range
                currentVolume = volume; //Save current volume
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(currentVolume, currentVolume); //Apply volume to media player
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Button: Restart game
        dialogView.findViewById(R.id.btn_restart).setOnClickListener(v -> {
            dialog.dismiss();
            restartGame(); //Restart the game
        });

        //Button: Go back to main menu
        dialogView.findViewById(R.id.btn_home).setOnClickListener(v -> {
            dialog.dismiss();
            goToMainMenu(); //Navigate to main menu
        });

        //Button: Exit app
        dialogView.findViewById(R.id.btn_exit).setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity(); //Exit the app
        });
    }

    /**
     * Displays the game over dialog when the player has no more lives
     * The dialog allows the user to restart the game, return to the main menu or exit the app.
     */

    private void showGameOverDialog() {
        gameOverDialog = true;

        // Save the player's final statistics )
        int finalScore = calculateScore();
        statisticsManager.recordGame(correctAnswersCount, wrongAnswersCount, finalScore);

        //Check if the dialog instance already exists
        if (gameOverDialogInstance == null) {
            //Create the dialog using a custom style
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);

            //Inflate the custom layout for the game over dialog
            View dialogView = getLayoutInflater().inflate(R.layout.game_over_dialog, null);
            builder.setView(dialogView);

            //Create the dialog only once
            gameOverDialogInstance = builder.create();
            gameOverDialogInstance.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.dialog_background));
            gameOverDialogInstance.setCancelable(false); //Prevent closing the dialog by tapping outside

            // Button: Restart the game
            dialogView.findViewById(R.id.btn_restart).setOnClickListener(v -> {
                gameOverDialogInstance.dismiss();
                restartGame(); //Restart the game
            });

            //Button: Go to main menu
            dialogView.findViewById(R.id.btn_home).setOnClickListener(v -> {
                gameOverDialogInstance.dismiss();
                goToMainMenu(); //Navigate to main menu
            });

            //Button: Exit the application
            dialogView.findViewById(R.id.btn_exit).setOnClickListener(v -> {
                gameOverDialogInstance.dismiss();
                finishAffinity(); //Exit the app
            });
        }

        // Show the dialog
        gameOverDialogInstance.show();
    }


    /**
     * Restarts the game by resetting all relevant variables and UI
     * This is called when the user chooses to play again after game over or from settings
     */
    private void restartGame() {
        playedSongs.clear(); //Clear the list of already played songs
        currentQuestionIndex = 0; //Reset the question index to the start
        selectedAnswer = null; //Clear any selected answer
        feedbackText.setText("Choose a Song"); //Reset feedback message
        enableAllOptions(); //Enable answer buttons
        lives = 3; //Reset lives to full (3)
        skipsRemaining = 3; //Reset skips to 3
        answered = false; //No answer selected at the start

        startNewGame(); //Initialize visuals and life icons
        loadNextSong(); //Load the first song of the new game
    }

    /**
     * Shows a simple dialog with a SeekBar that allows the user to adjust the volume of the music playback
     */
    private void showVolumeDialog() {
        //Create a basic alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adjust Volume"); //Title of the dialog

        //Create a SeekBar slider for volume adjustment
        final SeekBar volumeSeekBar = new SeekBar(this);
        volumeSeekBar.setMax(100); //Max volume value (100%)

        //Set default volume to 100% at start
        volumeSeekBar.setProgress(100);

        //Add the SeekBar to the dialog's view
        builder.setView(volumeSeekBar);

        //Handle volume changes
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f; //Convert to 0.0-1.0 float range
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(volume, volume); //Update media player's volume
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Add a "Close" button to dismiss the dialog
        builder.setNegativeButton("Close", null);

        //Show the dialog to the user
        builder.show();
    }

    /**
     * Navigates back to the main menu of the app.
     * Clears the activity stack to ensure a fresh start of the main activity
     */
    private void goToMainMenu() {
        //Create an intent to launch the MainActivity
        Intent intent = new Intent(this, MainActivity.class);

        //Clear the activity stack so that pressing "Back" wont return to the game
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        //Start the main menu activity
        startActivity(intent);

        // Finish this activity so it is removed from the stack
        finish();
    }

    /**
     * Resets a button to its default appearance
     * @param btn = the button to reset
     */
    private void resetButtons(Button btn) {
        //Reset the button's background to the default
        btn.setBackgroundResource(R.drawable.button_background);

        //Reset the text color to the default color
        btn.setTextColor(ContextCompat.getColor(this, R.color.text));
    }

    /**
     * Disables all answer buttons so the player cant select any more answers
     */
    private void disableAllOptions() {
        btn1.setEnabled(false);
        btn2.setEnabled(false);
        btn3.setEnabled(false);
        btn4.setEnabled(false);
    }

    /**
     * Enables all answer buttons and resets their appearance to default
     */
    private void enableAllOptions() {
        //Reset the visual appearance of each button
        resetButtons(btn1);
        resetButtons(btn2);
        resetButtons(btn3);
        resetButtons(btn4);

        //Enable the buttons for user interaction
        btn1.setEnabled(true);
        btn2.setEnabled(true);
        btn3.setEnabled(true);
        btn4.setEnabled(true);
    }

    /**
     * Called when the activity is being destroyed
     * Releases resources like the media player and stops any running animations
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Release the media player to free up resources
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        //Stop the vinyl animation if it's still running
        LottieAnimationView vinylAnimation = findViewById(R.id.vinyl_animation);
        if (vinylAnimation != null) {
            vinylAnimation.cancelAnimation();
            vinylAnimation.setProgress(0f); //Reset the animation to the start
        }
    }

}