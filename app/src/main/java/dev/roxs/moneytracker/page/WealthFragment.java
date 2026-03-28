package dev.roxs.moneytracker.page;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.Adapter.AssetCategoryAdapter;
import dev.roxs.moneytracker.helper.CsvImporter;
import dev.roxs.moneytracker.helper.SQl_Helper;
import dev.roxs.moneytracker.model.AssetCategory;
import dev.roxs.moneytracker.model.AssetItem;

public class WealthFragment extends Fragment implements AssetCategoryAdapter.OnCategoryActionListener {

    private SQl_Helper sql;
    private PieChart pieChart;
    private TextView tvTotalNetWorth, tvTotalPnl;
    private LinearLayout categoryContainer;
    private List<AssetCategory> categories;

    private final String[] PICKER_COLORS = {
            "#4CAF50", "#2196F3", "#FFA000", "#9C27B0",
            "#FF5722", "#00BCD4", "#E91E63"
    };

    private ActivityResultLauncher<Intent> csvPickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wealth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sql = new SQl_Helper(requireContext());

        tvTotalNetWorth = view.findViewById(R.id.tvTotalNetWorth);
        tvTotalPnl = view.findViewById(R.id.tvTotalPnl);
        pieChart = view.findViewById(R.id.pieChart);
        categoryContainer = view.findViewById(R.id.categoryContainer);

        ImageView btnAddCategory = view.findViewById(R.id.btnAddCategory);
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        TextView btnImport = view.findViewById(R.id.btnImport);
        btnImport.setOnClickListener(v -> openFilePicker());

        csvPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) importCsv(uri);
                    }
                }
        );

        setupPieChart();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(62f);
        pieChart.setTransparentCircleColor(Color.TRANSPARENT);
        pieChart.setDrawEntryLabels(false);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextColor(Color.parseColor("#8B95A5"));
        pieChart.getLegend().setTextSize(11f);
        pieChart.setExtraOffsets(5, 10, 5, 10);
        pieChart.animateY(800);
    }

    private void loadData() {
        categories = sql.getAllCategories();

        double totalNetWorth = 0;
        double totalPnl = 0;
        double totalInvested = 0;
        for (AssetCategory c : categories) {
            totalNetWorth += c.getTotalValue();
            for (AssetItem item : c.getItems()) {
                totalPnl += item.getPnl();
                totalInvested += item.getInvested();
            }
        }
        tvTotalNetWorth.setText(formatCurrencyFull(totalNetWorth));

        if (totalInvested > 0) {
            String pnlStr = (totalPnl >= 0 ? "+" : "") + formatCurrencyFull(totalPnl);
            double pnlPct = (totalPnl / totalInvested) * 100;
            tvTotalPnl.setText(String.format("%s (%.2f%%)", pnlStr, pnlPct));
            tvTotalPnl.setTextColor(Color.parseColor(totalPnl >= 0 ? "#00E676" : "#FF5252"));
        } else {
            tvTotalPnl.setText("");
        }

        updatePieChart();

        // Populate categories into LinearLayout (avoids RV-in-ScrollView issue)
        categoryContainer.removeAllViews();
        AssetCategoryAdapter adapter = new AssetCategoryAdapter(requireContext(), categories, this);
        for (int i = 0; i < adapter.getItemCount(); i++) {
            AssetCategoryAdapter.ViewHolder vh = adapter.createViewHolder(categoryContainer, 0);
            adapter.bindViewHolder(vh, i);
            categoryContainer.addView(vh.itemView);
        }
    }

    private void updatePieChart() {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (AssetCategory cat : categories) {
            if (cat.getTotalValue() > 0) {
                entries.add(new PieEntry((float) cat.getTotalValue(), cat.getName()));
                try {
                    colors.add(Color.parseColor(cat.getColor()));
                } catch (Exception e) {
                    colors.add(Color.GRAY);
                }
            }
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1f, "No Assets"));
            colors.add(Color.parseColor("#1E2A3A"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    // ========== CSV Import ==========

    private void openFilePicker() {
        // Check if categories exist first
        List<AssetCategory> cats = sql.getAllCategories();
        if (cats.isEmpty()) {
            new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                    .setTitle("No Categories")
                    .setMessage("Please add at least one category before importing holdings.\n\nTap the + button at the top right to add categories like Equity, Bonds, Metals, etc.")
                    .setPositiveButton("Add Category", (d, w) -> showAddCategoryDialog())
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        csvPickerLauncher.launch(Intent.createChooser(intent, "Select Holdings CSV"));
    }

    private void importCsv(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            List<CsvImporter.HoldingRow> rows = CsvImporter.parse(inputStream);

            List<CsvImporter.HoldingRow> unassigned = new ArrayList<>();
            int autoUpdated = 0;

            for (CsvImporter.HoldingRow row : rows) {
                AssetItem existing = sql.findAssetItemByName(row.instrument);
                if (existing != null) {
                    sql.updateFullAssetItem(existing.getId(), row.instrument,
                            row.qty, row.avgCost, row.ltp, row.invested, row.curVal,
                            row.pnl, row.netChg, row.dayChg);
                    autoUpdated++;
                } else {
                    unassigned.add(row);
                }
            }

            if (autoUpdated > 0) {
                Toast.makeText(requireContext(), autoUpdated + " instruments auto-updated", Toast.LENGTH_SHORT).show();
            }

            if (!unassigned.isEmpty()) {
                promptCategoryAssignment(unassigned, 0);
            } else {
                loadData();
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error importing CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void promptCategoryAssignment(List<CsvImporter.HoldingRow> unassigned, int index) {
        if (index >= unassigned.size()) {
            loadData();
            Toast.makeText(requireContext(), "Import complete!", Toast.LENGTH_SHORT).show();
            return;
        }

        CsvImporter.HoldingRow row = unassigned.get(index);
        List<AssetCategory> cats = sql.getAllCategories();
        String[] catNames = new String[cats.size()];
        for (int i = 0; i < cats.size(); i++) catNames[i] = cats.get(i).getName();

        View dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, null, false);

        // Build a simple dialog with a spinner
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        TextView tvInfo = new TextView(requireContext());
        tvInfo.setText(String.format("Instrument: %s\nQty: %.2f | Value: ₹ %.2f",
                row.instrument, row.qty, row.curVal));
        tvInfo.setTextColor(Color.WHITE);
        tvInfo.setTextSize(14);
        layout.addView(tvInfo);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText("\nAssign to category:");
        tvLabel.setTextColor(Color.parseColor("#8B95A5"));
        tvLabel.setTextSize(13);
        layout.addView(tvLabel);

        Spinner spinner = new Spinner(requireContext());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, catNames);
        spinner.setAdapter(spinnerAdapter);
        layout.addView(spinner);

        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setTitle("Assign \"" + row.instrument + "\" (" + (index + 1) + "/" + unassigned.size() + ")")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton("Assign", (d, w) -> {
                    int selectedIdx = spinner.getSelectedItemPosition();
                    int catId = cats.get(selectedIdx).getId();
                    sql.insertFullAssetItem(catId, row.instrument, row.qty, row.avgCost,
                            row.ltp, row.invested, row.curVal, row.pnl, row.netChg, row.dayChg);
                    promptCategoryAssignment(unassigned, index + 1);
                })
                .setNegativeButton("Skip", (d, w) -> {
                    promptCategoryAssignment(unassigned, index + 1);
                })
                .show();
    }

    // ========== Category Actions ==========

    @Override
    public void onEditCategory(AssetCategory category) {
        showEditCategoryDialog(category);
    }

    @Override
    public void onDeleteCategory(AssetCategory category) {
        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setTitle("Delete Category")
                .setMessage("Delete \"" + category.getName() + "\" and all its assets?")
                .setPositiveButton("Delete", (d, w) -> {
                    sql.deleteCategory(category.getId());
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onAddAsset(AssetCategory category) {
        showAddAssetDialog(category.getId());
    }

    @Override
    public void onEditAsset(AssetItem item) {
        showEditAssetDialog(item);
    }

    @Override
    public void onDeleteAsset(AssetItem item) {
        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setTitle("Delete Asset")
                .setMessage("Delete \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    sql.deleteAssetItem(item.getId());
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ========== Dialogs ==========

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        EditText etTarget = dialogView.findViewById(R.id.etTargetAllocation);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Add Category");

        final String[] selectedColor = {PICKER_COLORS[0]};
        setupColorPicker(dialogView, selectedColor);

        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    double target = 0;
                    try { target = Double.parseDouble(etTarget.getText().toString().trim()); } catch (Exception e) {}
                    if (!name.isEmpty()) {
                        sql.insertCategory(name, selectedColor[0], target);
                        loadData();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditCategoryDialog(AssetCategory category) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        EditText etTarget = dialogView.findViewById(R.id.etTargetAllocation);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Edit Category");
        etName.setText(category.getName());
        if (category.getTargetAllocation() > 0) {
            etTarget.setText(String.valueOf(category.getTargetAllocation()));
        }

        final String[] selectedColor = {category.getColor()};
        setupColorPicker(dialogView, selectedColor);

        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    double target = 0;
                    try { target = Double.parseDouble(etTarget.getText().toString().trim()); } catch (Exception e) {}
                    if (!name.isEmpty()) {
                        sql.updateCategory(category.getId(), name, selectedColor[0], target);
                        loadData();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddAssetDialog(int categoryId) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_asset, null);
        EditText etName = dialogView.findViewById(R.id.etAssetName);
        EditText etValue = dialogView.findViewById(R.id.etAssetValue);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Add Asset");

        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String valueStr = etValue.getText().toString().trim();
                    if (!name.isEmpty() && !valueStr.isEmpty()) {
                        double value = Double.parseDouble(valueStr);
                        sql.insertAssetItem(categoryId, name, value);
                        loadData();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditAssetDialog(AssetItem item) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_asset, null);
        EditText etName = dialogView.findViewById(R.id.etAssetName);
        EditText etValue = dialogView.findViewById(R.id.etAssetValue);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Edit Asset");
        etName.setText(item.getName());
        etValue.setText(String.valueOf(item.getValue()));

        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String valueStr = etValue.getText().toString().trim();
                    if (!name.isEmpty() && !valueStr.isEmpty()) {
                        double value = Double.parseDouble(valueStr);
                        sql.updateAssetItem(item.getId(), name, value);
                        loadData();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ========== Helpers ==========

    private void setupColorPicker(View dialogView, String[] selectedColor) {
        int[] colorViewIds = {
                R.id.color1, R.id.color2, R.id.color3, R.id.color4,
                R.id.color5, R.id.color6, R.id.color7
        };

        for (int i = 0; i < colorViewIds.length; i++) {
            View colorView = dialogView.findViewById(colorViewIds[i]);
            final String color = PICKER_COLORS[i];

            try {
                GradientDrawable bg = (GradientDrawable) colorView.getBackground().mutate();
                bg.setColor(Color.parseColor(color));
                if (color.equals(selectedColor[0])) {
                    bg.setStroke(4, Color.WHITE);
                }
            } catch (Exception e) {
                colorView.setBackgroundColor(Color.parseColor(color));
            }

            colorView.setOnClickListener(v -> {
                selectedColor[0] = color;
                for (int id : colorViewIds) {
                    View cv = dialogView.findViewById(id);
                    try {
                        GradientDrawable d = (GradientDrawable) cv.getBackground().mutate();
                        d.setStroke(0, Color.TRANSPARENT);
                    } catch (Exception ex) { /* ignore */ }
                }
                try {
                    GradientDrawable d = (GradientDrawable) v.getBackground().mutate();
                    d.setStroke(4, Color.WHITE);
                } catch (Exception ex) { /* ignore */ }
            });
        }
    }

    private String formatCurrencyFull(double value) {
        if (value >= 10000000) {
            return String.format("₹ %.2f Cr", value / 10000000.0);
        } else if (value >= 100000) {
            return String.format("₹ %.2f L", value / 100000.0);
        } else {
            return String.format("₹ %.2f", value);
        }
    }
}
