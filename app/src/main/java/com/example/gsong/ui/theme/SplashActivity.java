package com.example.gsong.ui.theme;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gsong.MainActivity;
import com.example.gsong.R;

public class SplashActivity extends AppCompatActivity {

    // Μεταβλητή για να αποτρέψουμε διπλή μετάβαση στην MainActivity
    private boolean hasSkipped = false;

    // Το ριζικό layout ώστε να εφαρμόσουμε πάνω του fade-out animation
    private FrameLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Φόρτωση του layout του splash screen

        rootLayout = findViewById(R.id.splashRoot); // Το κύριο layout
        VideoView videoView = findViewById(R.id.splashVideo); // Το VideoView για την εισαγωγική animation

        // Δημιουργία URI για το video που βρίσκεται στον φάκελο res/raw
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.logo_intro;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri); // Αντιστοίχιση του video με το VideoView

        // Όταν τελειώσει η αναπαραγωγή του video, ξεκινάμε το fade-out και την μετάβαση
        videoView.setOnCompletionListener(mp -> {
            if (!hasSkipped) {
                hasSkipped = true;
                fadeOutAndStartMain(); // Εκκίνηση κύριας δραστηριότητας
            }
        });

        // Αν ο χρήστης αγγίξει την οθόνη πριν τελειώσει το video, πηγαίνουμε κατευθείαν στη MainActivity
        videoView.setOnTouchListener((v, event) -> {
            if (!hasSkipped) {
                hasSkipped = true;
                fadeOutAndStartMain(); // Παράκαμψη intro
            }
            return true; // Επιστρέφουμε true για να δηλώσουμε ότι καταναλώσαμε το touch
        });

        videoView.start(); // Ξεκινάει η αναπαραγωγή του video
    }

    // Μέθοδος για fade-out animation και μετάβαση στην MainActivity
    private void fadeOutAndStartMain() {
        // Δημιουργία animation fade-out (από πλήρη διαφάνεια σε μηδενική)
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(600); // Διάρκεια 600ms
        fadeOut.setFillAfter(true); // Κρατάει την τελική κατάσταση (δηλαδή το 0 opacity)

        // Listener για όταν τελειώσει το animation
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Ξεκινάμε την MainActivity και τελειώνουμε την splash
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish(); // Κλείνουμε αυτή τη δραστηριότητα ώστε να μη γυρίσει ο χρήστης πίσω
            }
        });

        // Εκκίνηση του fade-out στο root layout
        rootLayout.startAnimation(fadeOut);
    }
}
