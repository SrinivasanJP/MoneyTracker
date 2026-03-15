package dev.roxs.moneytracker.page;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.SQl_Helper;

public class Statistics_Activity extends AppCompatActivity {

    private ImageView backButton;
    private TextView vHighestSpentDay, vLowestSpentDay, vMonthSubtitle;
    private LineChart monthlyLineChart, investmentLineChart;
    private BarChart yearlyBarChart;

    // Yearly
    private TextView vYearTotalSpent, vYearTotalInvestments, vYearAvgDaily;

    // Lifetime
    private TextView vTotalDaysTracked, vOverallTotalSpent;

    private SQl_Helper sql;
    private LocalDate currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sql = new SQl_Helper(this);

        // Read selected month/year from Intent, fallback to current date
        int selectedYear = getIntent().getIntExtra("selectedYear", -1);
        int selectedMonth = getIntent().getIntExtra("selectedMonth", -1);
        if (selectedYear > 0 && selectedMonth > 0) {
            currentDate = LocalDate.of(selectedYear, selectedMonth, 1);
        } else {
            currentDate = LocalDate.now();
        }

        initViews();
        populateData();
        setupLineChart();
        setupInvestmentChart();
        setupBarChart();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        vMonthSubtitle = findViewById(R.id.statsMonthSubtitle);
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        vMonthSubtitle.setText(currentDate.format(monthFmt));

        vHighestSpentDay = findViewById(R.id.highestSpentDay);
        vLowestSpentDay = findViewById(R.id.lowestSpentDay);

        monthlyLineChart = findViewById(R.id.monthlyLineChart);
        investmentLineChart = findViewById(R.id.investmentLineChart);
        yearlyBarChart = findViewById(R.id.yearlyBarChart);

        vYearTotalSpent = findViewById(R.id.yearTotalSpent).findViewById(R.id.statValue);
        ((TextView) findViewById(R.id.yearTotalSpent).findViewById(R.id.statLabel)).setText(getString(R.string.year_total_spent));

        vYearTotalInvestments = findViewById(R.id.yearTotalInvestments).findViewById(R.id.statValue);
        ((TextView) findViewById(R.id.yearTotalInvestments).findViewById(R.id.statLabel)).setText(getString(R.string.year_total_investments));

        vYearAvgDaily = findViewById(R.id.yearAvgDaily).findViewById(R.id.statValue);
        ((TextView) findViewById(R.id.yearAvgDaily).findViewById(R.id.statLabel)).setText(getString(R.string.year_avg_daily));

        vTotalDaysTracked = findViewById(R.id.totalDaysTracked).findViewById(R.id.statValue);
        ((TextView) findViewById(R.id.totalDaysTracked).findViewById(R.id.statLabel)).setText(getString(R.string.total_days_tracked));

        vOverallTotalSpent = findViewById(R.id.overallTotalSpent).findViewById(R.id.statValue);
        ((TextView) findViewById(R.id.overallTotalSpent).findViewById(R.id.statLabel)).setText(getString(R.string.overall_total_spent));
    }

    private void populateData() {
        vHighestSpentDay.setText(sql.getHighestSpentDayThisMonth(currentDate));
        vLowestSpentDay.setText(sql.getLowestSpentDayThisMonth(currentDate));

        int year = currentDate.getYear();
        vYearTotalSpent.setText(String.format("Rs. %.2f", sql.getYearlyTotalSpent(year)));
        vYearTotalInvestments.setText(String.format("Rs. %.2f", sql.getYearlyTotalInvestments(year)));
        vYearAvgDaily.setText(String.format("Rs. %.2f", sql.getYearlyAverageDaily(year)));

        vTotalDaysTracked.setText(sql.getTotalDaysRecorded() + " days");
        vOverallTotalSpent.setText(String.format("Rs. %.2f", sql.getOverallTotalSpent()));
    }

    private void setupLineChart() {
        float[] dailySpends = sql.getMonthlyDailySpends(currentDate);
        ArrayList<Entry> entries = new ArrayList<>();

        for (int i = 0; i < dailySpends.length; i++) {
            entries.add(new Entry(i + 1, dailySpends[i]));
        }
        Log.d("TEST DEV", "setupLineChart: "+ entries);

        LineDataSet dataSet = new LineDataSet(entries, "Daily Spent (Rs)");
        dataSet.setColor(ContextCompat.getColor(this, R.color.accentBlue));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.accentBlue));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.accentBlueSoft));

        LineData lineData = new LineData(dataSet);
        monthlyLineChart.setData(lineData);

        // Chart Styling
        monthlyLineChart.getDescription().setEnabled(false);
        monthlyLineChart.getLegend().setEnabled(false);
        monthlyLineChart.getAxisRight().setEnabled(false);
        YAxis yAxis = monthlyLineChart.getAxisLeft();
        yAxis.setTextColor(ContextCompat.getColor(this, R.color.white));
        XAxis xAxis = monthlyLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // Show whole days
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.white));

        monthlyLineChart.animateX(1000);
        monthlyLineChart.invalidate();
    }

    private void setupInvestmentChart() {
        HashMap<String, Double> investmentMap = sql.getInvestmentEntries();

        if (investmentMap.isEmpty()) {
            investmentLineChart.setNoDataText("No investments this month");
            investmentLineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.gray600));
            investmentLineChart.invalidate();
            return;
        }

        // Sort by day using TreeMap
        TreeMap<String, Double> sortedMap = new TreeMap<>(investmentMap);

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> dayLabels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
            entries.add(new Entry(index, entry.getValue().floatValue()));
            dayLabels.add(String.valueOf(entry.getKey()));
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Investment (Rs)");
        dataSet.setColor(ContextCompat.getColor(this, R.color.accentGreen));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.accentGreen));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.white));
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.accentGreenSoft));

        LineData lineData = new LineData(dataSet);
        investmentLineChart.setData(lineData);

        // Chart Styling
        investmentLineChart.getDescription().setEnabled(false);
        investmentLineChart.getLegend().setEnabled(false);
        investmentLineChart.getAxisRight().setEnabled(false);
        YAxis yAxis = investmentLineChart.getAxisLeft();
        yAxis.setTextColor(ContextCompat.getColor(this, R.color.white));
        XAxis xAxis = investmentLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.white));
        // Show only the day numbers where investments exist
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));

        investmentLineChart.animateX(1000);
        investmentLineChart.invalidate();
    }

    private void setupBarChart() {
        float[] monthlySpends = sql.getYearlyMonthlySpends(currentDate.getYear());
        ArrayList<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < monthlySpends.length; i++) {
            entries.add(new BarEntry(i, monthlySpends[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Spent");
        dataSet.setColor(ContextCompat.getColor(this, R.color.accentAmber));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        yearlyBarChart.setData(barData);

        // Chart Styling
        yearlyBarChart.getDescription().setEnabled(false);
        yearlyBarChart.getLegend().setEnabled(false);
        yearlyBarChart.getAxisRight().setEnabled(false);

        String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        YAxis yAxis = yearlyBarChart.getAxisLeft();
        yAxis.setTextColor(ContextCompat.getColor(this, R.color.white));
        XAxis xAxis = yearlyBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.white));

        yearlyBarChart.animateY(1000);
        yearlyBarChart.invalidate();
    }
}
