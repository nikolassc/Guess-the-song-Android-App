package com.example.gsong.ui.theme;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.gsong.R;
import com.example.gsong.StatisticsManager;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    // Δηλώσεις μεταβλητών για τα UI στοιχεία
    private TextView gamesPlayedTextView, correctAnswersTextView, wrongAnswersTextView;
    private TextView successRateTextView, highScoreTextView, avgCorrectTextView;
    private PieChart pieChart;
    private Button resetStatsButton;

    private StatisticsManager statsManager; // Διαχειρίζεται τα στατιστικά του χρήστη

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Αρχικοποίηση του StatisticsManager για πρόσβαση στα αποθηκευμένα δεδομένα
        statsManager = new StatisticsManager(this);

        // Σύνδεση μεταβλητών με τα στοιχεία του layout
        gamesPlayedTextView = findViewById(R.id.gamesPlayedTextView);
        correctAnswersTextView = findViewById(R.id.correctAnswersTextView);
        wrongAnswersTextView = findViewById(R.id.wrongAnswersTextView);
        successRateTextView = findViewById(R.id.successRateTextView);
        highScoreTextView = findViewById(R.id.highScoreTextView);
        avgCorrectTextView = findViewById(R.id.avgCorrectTextView);
        pieChart = findViewById(R.id.pie_chart);
        resetStatsButton = findViewById(R.id.btn_reset_stats);

        // Φόρτωση και εμφάνιση στατιστικών με την είσοδο στην activity
        loadAndDisplayStats();

        // Όταν πατηθεί το κουμπί επαναφοράς στατιστικών
        resetStatsButton.setOnClickListener(v -> {
            statsManager.resetStats(); // Εκκαθάριση δεδομένων
            Toast.makeText(this, "Statistics reset!", Toast.LENGTH_SHORT).show();
            loadAndDisplayStats(); // Επαναφόρτωση της οθόνης με μηδενισμένα στατιστικά
        });
    }

    // Συνάρτηση που φορτώνει τα δεδομένα και τα εμφανίζει στην οθόνη
    private void loadAndDisplayStats() {
        int gamesPlayed = statsManager.getGamesPlayed();
        int correct = statsManager.getCorrectAnswers();
        int wrong = statsManager.getWrongAnswers();
        int totalAnswers = correct + wrong;

        // Υπολογισμός ποσοστού επιτυχίας
        float successRate = totalAnswers > 0 ? (100f * correct / totalAnswers) : 0f;

        // Υψηλότερη βαθμολογία και μέσος όρος σωστών ανά παιχνίδι
        int highScore = statsManager.getHighScore();
        float avgCorrect = gamesPlayed > 0 ? (float) correct / gamesPlayed : 0f;

        // Εμφάνιση δεδομένων στα TextViews
        gamesPlayedTextView.setText("Games Played: " + gamesPlayed);
        correctAnswersTextView.setText("Correct Answers: " + correct);
        wrongAnswersTextView.setText("Wrong Answers: " + wrong);
        successRateTextView.setText(String.format("Success Rate: %.1f%%", successRate));
        highScoreTextView.setText("High Score: " + highScore);
        avgCorrectTextView.setText(String.format("Avg Correct/Game: %.2f", avgCorrect));

        // Ενημέρωση του γραφήματος
        setupPieChart(correct, wrong);
    }

    // Δημιουργία και ρύθμιση του Pie Chart για σωστές/λάθος απαντήσεις
    private void setupPieChart(int correct, int wrong) {
        List<PieEntry> entries = new ArrayList<>();

        // Προσθήκη των δεδομένων στο γράφημα
        if (correct > 0) entries.add(new PieEntry(correct, "Correct"));
        if (wrong > 0) entries.add(new PieEntry(wrong, "Wrong"));

        // Δημιουργία του DataSet και καθορισμός χρωμάτων
        PieDataSet dataSet = new PieDataSet(entries, "Answers");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);

        // Εισαγωγή των δεδομένων στο γράφημα
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        // Ρυθμίσεις εμφάνισης γραφήματος
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true); // Κυκλική τρύπα στο κέντρο
        pieChart.setEntryLabelTextSize(14f);
        pieChart.setCenterText("Answer Stats");
        pieChart.setCenterTextSize(18f);
        pieChart.getDescription().setEnabled(false); // Απόκρυψη περιγραφής

        // Ρυθμίσεις υπομνήματος (legend)
        Legend legend = pieChart.getLegend();
        legend.setTextSize(14f);
        legend.setFormSize(14f);
        legend.setTextColor(ContextCompat.getColor(this, R.color.white));

        pieChart.invalidate(); // Ανανεώνει το γράφημα στην οθόνη
    }
}
