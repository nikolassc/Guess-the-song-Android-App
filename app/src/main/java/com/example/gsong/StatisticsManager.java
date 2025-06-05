package com.example.gsong;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Διαχειρίζεται τα στατιστικά του χρήστη (παιχνίδια, σωστές/λάθος απαντήσεις, σκορ).
 * Χρησιμοποιεί SharedPreferences για αποθήκευση των δεδομένων τοπικά.
 */
public class StatisticsManager {

    // Όνομα αρχείου για αποθήκευση των στατιστικών
    private static final String PREFS_NAME = "game_stats";

    // Το αντικείμενο SharedPreferences για προσπέλαση/αποθήκευση τιμών
    private final SharedPreferences prefs;

    // Κατασκευαστής - δημιουργεί το SharedPreferences αντικείμενο
    public StatisticsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Καταγράφει ένα νέο παιχνίδι προσθέτοντας σωστές, λάθος απαντήσεις και σκορ.
     * Αν το σκορ είναι μεγαλύτερο από το προηγούμενο High Score, το ενημερώνει.
     */
    public void recordGame(int correct, int wrong, int score) {
        SharedPreferences.Editor editor = prefs.edit();

        int gamesPlayed = getGamesPlayed() + 1;
        editor.putInt("games_played", gamesPlayed); // Ενημέρωση αριθμού παιχνιδιών
        editor.putInt("correct_answers", getCorrectAnswers() + correct); // Αθροιστικά σωστές
        editor.putInt("wrong_answers", getWrongAnswers() + wrong); // Αθροιστικά λάθος

        // Αν το νέο σκορ είναι μεγαλύτερο από το προηγούμενο, αποθήκευσέ το
        if (score > getHighScore()) {
            editor.putInt("high_score", score);
        }

        editor.apply(); // Εφαρμογή αλλαγών
    }

    // Επιστρέφει τον αριθμό παιχνιδιών που έχουν παιχτεί
    public int getGamesPlayed() {
        return prefs.getInt("games_played", 0);
    }

    // Επιστρέφει το σύνολο των σωστών απαντήσεων
    public int getCorrectAnswers() {
        return prefs.getInt("correct_answers", 0);
    }

    // Επιστρέφει το σύνολο των λάθος απαντήσεων
    public int getWrongAnswers() {
        return prefs.getInt("wrong_answers", 0);
    }

    // Επιστρέφει το μεγαλύτερο σκορ που έχει πετύχει ο χρήστης
    public int getHighScore() {
        return prefs.getInt("high_score", 0);
    }

    // Διαγράφει όλα τα αποθηκευμένα στατιστικά (reset)
    public void resetStats() {
        prefs.edit().clear().apply();
    }
}
