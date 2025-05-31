package com.example.gsong.ui.theme;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

    private TextView gamesPlayedTextView, correctAnswersTextView, wrongAnswersTextView;
    private TextView successRateTextView, highScoreTextView, avgCorrectTextView;
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        StatisticsManager statsManager = new StatisticsManager(this);

        gamesPlayedTextView = findViewById(R.id.gamesPlayedTextView);
        correctAnswersTextView = findViewById(R.id.correctAnswersTextView);
        wrongAnswersTextView = findViewById(R.id.wrongAnswersTextView);
        successRateTextView = findViewById(R.id.successRateTextView);
        highScoreTextView = findViewById(R.id.highScoreTextView);
        avgCorrectTextView = findViewById(R.id.avgCorrectTextView);
        pieChart = findViewById(R.id.pie_chart);

        int gamesPlayed = statsManager.getGamesPlayed();
        int correct = statsManager.getCorrectAnswers();
        int wrong = statsManager.getWrongAnswers();
        int totalAnswers = correct + wrong;
        float successRate = totalAnswers > 0 ? (100f * correct / totalAnswers) : 0f;
        int highScore = statsManager.getHighScore();
        float avgCorrect = gamesPlayed > 0 ? (float) correct / gamesPlayed : 0f;

        gamesPlayedTextView.setText("Games Played: " + gamesPlayed);
        correctAnswersTextView.setText("Correct Answers: " + correct);
        wrongAnswersTextView.setText("Wrong Answers: " + wrong);
        successRateTextView.setText(String.format("Success Rate: %.1f%%", successRate));
        highScoreTextView.setText("High Score: " + highScore);
        avgCorrectTextView.setText(String.format("Avg Correct/Game: %.2f", avgCorrect));

        setupPieChart(correct, wrong);
    }

    private void setupPieChart(int correct, int wrong) {
        List<PieEntry> entries = new ArrayList<>();
        if (correct > 0) entries.add(new PieEntry(correct, "Correct"));
        if (wrong > 0) entries.add(new PieEntry(wrong, "Wrong"));

        PieDataSet dataSet = new PieDataSet(entries, "Answers");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setEntryLabelTextSize(14f);
        pieChart.setCenterText("Answer Stats");
        pieChart.setCenterTextSize(18f);
        pieChart.getDescription().setEnabled(false);

        Legend legend = pieChart.getLegend();
        legend.setTextSize(14f);
        legend.setFormSize(14f);

        pieChart.invalidate(); // refresh chart
    }
}
