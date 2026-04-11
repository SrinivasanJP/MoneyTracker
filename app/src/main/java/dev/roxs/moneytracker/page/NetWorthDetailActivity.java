package dev.roxs.moneytracker.page;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.SQl_Helper;
import dev.roxs.moneytracker.model.AssetCategory;

public class NetWorthDetailActivity extends AppCompatActivity {

    private SQl_Helper sql;
    private LineChart chartOverall, chartCategories;
    private TextView tvDetailNetWorth, tvDetailChange, tvDetailPeriod;
    private LinearLayout legendItems;

    // Colors for category lines
    private final int[] LINE_COLORS = {
            Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"),
            Color.parseColor("#FFA000"), Color.parseColor("#9C27B0"),
            Color.parseColor("#FF5722"), Color.parseColor("#00BCD4"),
            Color.parseColor("#E91E63"), Color.parseColor("#8BC34A"),
            Color.parseColor("#FF9800"), Color.parseColor("#3F51B5")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_networth_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#0F3460"));
        }

        sql = new SQl_Helper(this);

        tvDetailNetWorth = findViewById(R.id.tvDetailNetWorth);
        tvDetailChange = findViewById(R.id.tvDetailChange);
        tvDetailPeriod = findViewById(R.id.tvDetailPeriod);
        chartOverall = findViewById(R.id.chartOverall);
        chartCategories = findViewById(R.id.chartCategories);
        legendItems = findViewById(R.id.legendItems);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        setupChartStyle(chartOverall);
        setupChartStyle(chartCategories);

        loadData();
    }

    private void setupChartStyle(LineChart chart) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);
        chart.setExtraOffsets(10, 10, 10, 10);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#8B95A5"));
        xAxis.setTextSize(9f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#8B95A5"));
        leftAxis.setTextSize(9f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#1E2A3A"));

        chart.getAxisRight().setEnabled(false);
        chart.animateX(800);
    }

    private void loadData() {
        // Get current net worth from categories
        List<AssetCategory> categories = sql.getAllCategories();
        double currentNetWorth = 0;
        for (AssetCategory c : categories) {
            currentNetWorth += c.getTotalValue();
        }
        tvDetailNetWorth.setText(formatCurrency(currentNetWorth));

        // Load history
        List<String[]> history = sql.getAllNetWorthHistory();

        if (history.size() >= 2) {
            double first = Double.parseDouble(history.get(0)[1]);
            double last = Double.parseDouble(history.get(history.size() - 1)[1]);
            double change = last - first;
            double changePct = first > 0 ? (change / first) * 100 : 0;
            tvDetailChange.setText(String.format("%s%s (%.2f%%)",
                    change >= 0 ? "+" : "", formatCurrency(change), changePct));
            tvDetailChange.setTextColor(Color.parseColor(change >= 0 ? "#00E676" : "#FF5252"));
            tvDetailPeriod.setText(String.format("from %s to %s", history.get(0)[0], history.get(history.size() - 1)[0]));
        }

        loadOverallChart(history);
        loadCategoryChart(history, categories);
    }

    private void loadOverallChart(List<String[]> history) {
        if (history.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            entries.add(new Entry(i, (float) Double.parseDouble(history.get(i)[1])));
            // Show abbreviated date (MM-dd)
            String date = history.get(i)[0];
            labels.add(date.length() >= 10 ? date.substring(5) : date);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Net Worth");
        dataSet.setColor(Color.parseColor("#00E5FF"));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(entries.size() <= 30);
        dataSet.setCircleColor(Color.parseColor("#00E5FF"));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#00E5FF"));
        dataSet.setFillAlpha(30);

        LineData lineData = new LineData(dataSet);
        chartOverall.setData(lineData);
        chartOverall.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartOverall.getXAxis().setLabelCount(Math.min(labels.size(), 7), true);
        chartOverall.invalidate();
    }

    private void loadCategoryChart(List<String[]> history, List<AssetCategory> categories) {
        if (history.isEmpty()) return;

        // Collect all unique category names across history
        Map<String, Integer> categoryColorMap = new HashMap<>();
        for (AssetCategory cat : categories) {
            try {
                categoryColorMap.put(cat.getName(), Color.parseColor(cat.getColor()));
            } catch (Exception e) {
                categoryColorMap.put(cat.getName(), Color.GRAY);
            }
        }

        // Parse breakdowns to find all category names
        Map<String, List<Entry>> categoryEntries = new HashMap<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            String breakdownJson = history.get(i)[2];
            String date = history.get(i)[0];
            labels.add(date.length() >= 10 ? date.substring(5) : date);

            try {
                JSONObject json = new JSONObject(breakdownJson != null ? breakdownJson : "{}");
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String catName = keys.next();
                    if (!categoryEntries.containsKey(catName)) {
                        categoryEntries.put(catName, new ArrayList<>());
                        // Backfill with 0 for previous dates
                        for (int j = 0; j < i; j++) {
                            categoryEntries.get(catName).add(new Entry(j, 0));
                        }
                    }
                    categoryEntries.get(catName).add(new Entry(i, (float) json.getDouble(catName)));
                }
                // Fill 0 for categories not in this snapshot
                for (String catName : categoryEntries.keySet()) {
                    List<Entry> entryList = categoryEntries.get(catName);
                    if (entryList.size() <= i) {
                        entryList.add(new Entry(i, 0));
                    }
                }
            } catch (Exception e) {
                // Skip malformed JSON
            }
        }

        List<LineDataSet> dataSets = new ArrayList<>();
        int colorIdx = 0;

        legendItems.removeAllViews();

        for (Map.Entry<String, List<Entry>> entry : categoryEntries.entrySet()) {
            String catName = entry.getKey();
            List<Entry> catEntries = entry.getValue();

            int color = categoryColorMap.containsKey(catName)
                    ? categoryColorMap.get(catName)
                    : LINE_COLORS[colorIdx % LINE_COLORS.length];

            LineDataSet ds = new LineDataSet(catEntries, catName);
            ds.setColor(color);
            ds.setLineWidth(2f);
            ds.setDrawCircles(false);
            ds.setDrawValues(false);
            ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSets.add(ds);

            // Add legend item
            addLegendItem(catName, color);
            colorIdx++;
        }

        if (!dataSets.isEmpty()) {
            LineData lineData = new LineData();
            for (LineDataSet ds : dataSets) {
                lineData.addDataSet(ds);
            }
            chartCategories.setData(lineData);
            chartCategories.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            chartCategories.getXAxis().setLabelCount(Math.min(labels.size(), 7), true);
            chartCategories.invalidate();
        }
    }

    private void addLegendItem(String name, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 6, 0, 6);

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(16, 16);
        dotParams.setMarginEnd(12);
        dot.setLayoutParams(dotParams);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(color);
        dot.setBackground(dotBg);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(Color.parseColor("#C5CDD9"));
        label.setTextSize(13);

        row.addView(dot);
        row.addView(label);
        legendItems.addView(row);
    }

    private String formatCurrency(double value) {
        if (Math.abs(value) >= 10000000) {
            return String.format("₹ %.2f Cr", value / 10000000.0);
        } else if (Math.abs(value) >= 100000) {
            return String.format("₹ %.2f L", value / 100000.0);
        } else {
            return String.format("₹ %.2f", value);
        }
    }
}
