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

    private boolean hasSkipped = false;
    private FrameLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        rootLayout = findViewById(R.id.splashRoot);
        VideoView videoView = findViewById(R.id.splashVideo);

        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.logo_intro;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);

        videoView.setOnCompletionListener(mp -> {
            if (!hasSkipped) {
                hasSkipped = true;
                fadeOutAndStartMain();
            }
        });

        videoView.setOnTouchListener((v, event) -> {
            if (!hasSkipped) {
                hasSkipped = true;
                fadeOutAndStartMain();
            }
            return true;
        });

        videoView.start();
    }

    private void fadeOutAndStartMain() {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(600);
        fadeOut.setFillAfter(true);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });

        rootLayout.startAnimation(fadeOut);
    }
}
