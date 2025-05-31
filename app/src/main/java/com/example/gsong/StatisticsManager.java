package com.example.gsong;

import android.content.Context;
import android.content.SharedPreferences;

public class StatisticsManager {
    private static final String PREFS_NAME = "game_stats";
    private final SharedPreferences prefs;

    public StatisticsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void recordGame(int correct, int wrong, int score) {
        SharedPreferences.Editor editor = prefs.edit();

        int gamesPlayed = getGamesPlayed() + 1;
        editor.putInt("games_played", gamesPlayed);
        editor.putInt("correct_answers", getCorrectAnswers() + correct);
        editor.putInt("wrong_answers", getWrongAnswers() + wrong);

        if (score > getHighScore()) {
            editor.putInt("high_score", score);
        }

        editor.apply();
    }

    public int getGamesPlayed() {
        return prefs.getInt("games_played", 0);
    }

    public int getCorrectAnswers() {
        return prefs.getInt("correct_answers", 0);
    }

    public int getWrongAnswers() {
        return prefs.getInt("wrong_answers", 0);
    }

    public int getHighScore() {
        return prefs.getInt("high_score", 0);
    }

    public void resetStats() {
        prefs.edit().clear().apply();
    }
}
