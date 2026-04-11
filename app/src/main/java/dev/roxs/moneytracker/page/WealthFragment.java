package dev.roxs.moneytracker.page;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.widget.LinearLayout;

import android.view.Gravity;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.Adapter.AssetCategoryAdapter;
import dev.roxs.moneytracker.helper.CsvImporter;
import dev.roxs.moneytracker.helper.Notification_Helper;
import dev.roxs.moneytracker.helper.SQl_Helper;
import dev.roxs.moneytracker.model.AssetCategory;
import dev.roxs.moneytracker.model.AssetItem;

public class WealthFragment extends Fragment implements AssetCategoryAdapter.OnCategoryActionListener {

    private SQl_Helper sql;
    private LinearLayout stackedBar;
    private LinearLayout allocationLegend;
    private TextView tvTotalNetWorth, tvTotalPnl;
    private LinearLayout categoryContainer;
    private List<AssetCategory> categories;

    private final String[] PICKER_COLORS = {
            "#4CAF50", "#2196F3", "#FFA000", "#9C27B0",
            "#FF5722", "#00BCD4", "#E91E63"
    };

    private ActivityResultLauncher<Intent> csvPickerLauncher;
    private ActivityResultLauncher<Intent> importPickerLauncher;

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
        stackedBar = view.findViewById(R.id.stackedBar);
        allocationLegend = view.findViewById(R.id.allocationLegend);
        categoryContainer = view.findViewById(R.id.categoryContainer);

        ImageView btnAddCategory = view.findViewById(R.id.btnAddCategory);
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // Net worth card click → detail activity
        View netWorthCard = view.findViewById(R.id.netWorthCard);
        netWorthCard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NetWorthDetailActivity.class);
            startActivity(intent);
        });

        // Export
        TextView btnExport = view.findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> exportWealthToExcel());

        // Import CSV
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

        // Schedule FD interest check
        Notification_Helper.scheduleFdInterestCheck(requireContext());


        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }



    // ========== Load Data ==========

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

        // Record daily snapshot
        recordNetWorthSnapshot(totalNetWorth);

        updateStackedBar(totalNetWorth);

        // Populate categories
        categoryContainer.removeAllViews();
        AssetCategoryAdapter adapter = new AssetCategoryAdapter(requireContext(), categories, this);
        for (int i = 0; i < adapter.getItemCount(); i++) {
            AssetCategoryAdapter.ViewHolder vh = adapter.createViewHolder(categoryContainer, 0);
            adapter.bindViewHolder(vh, i);
            categoryContainer.addView(vh.itemView);
        }
    }

    private void recordNetWorthSnapshot(double totalNetWorth) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // Build category breakdown JSON
        JSONObject breakdown = new JSONObject();
        try {
            for (AssetCategory c : categories) {
                breakdown.put(c.getName(), c.getTotalValue());
            }
        } catch (Exception e) { /* ignore */ }

        sql.insertNetWorthSnapshot(today, totalNetWorth, breakdown.toString());
    }

    // ========== Chart Updates ==========

    private void updateStackedBar(double totalNetWorth) {
        stackedBar.removeAllViews();
        allocationLegend.removeAllViews();

        if (categories.isEmpty() || totalNetWorth <= 0) {
            return;
        }

        // Sort categories by value descending (optional, looks better)
        List<AssetCategory> sortedCategories = new ArrayList<>(categories);
        sortedCategories.sort((c1, c2) -> Double.compare(c2.getTotalValue(), c1.getTotalValue()));

        for (AssetCategory cat : sortedCategories) {
            double value = cat.getTotalValue();
            if (value <= 0) continue;

            float weight = (float) (value / totalNetWorth);
            int color;
            try {
                color = Color.parseColor(cat.getColor());
            } catch (Exception e) {
                color = Color.GRAY;
            }

            // 1) Add segment to horizontal stacked bar
            View segment = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
            segment.setLayoutParams(params);
            segment.setBackgroundColor(color);
            stackedBar.addView(segment);

            // 2) Add legend item
            addLegendItem(cat.getName(), color, weight * 100);
        }
    }

    private void addLegendItem(String name, int color, float pct) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 6, 0, 6);

        // Color dot
        View dot = new View(requireContext());
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density));
        dotParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        dot.setLayoutParams(dotParams);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(color);
        dot.setBackground(dotBg);

        // Name
        TextView tvName = new TextView(requireContext());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameParams);
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#E0E6ED"));
        tvName.setTextSize(14f);
        // tvName.setTypeface(...) if you want to set custom font

        // Percentage
        TextView tvPct = new TextView(requireContext());
        tvPct.setText(String.format(Locale.getDefault(), "%.1f%%", pct));
        tvPct.setTextColor(Color.parseColor("#8B95A5"));
        tvPct.setTextSize(13f);

        row.addView(dot);
        row.addView(tvName);
        row.addView(tvPct);

        allocationLegend.addView(row);
    }

    // ========== Export ==========

    private void exportWealthToExcel() {
        try {
            Workbook workbook = new HSSFWorkbook();

            // Sheet 1: Categories
            Sheet catSheet = workbook.createSheet("Categories");
            Cursor catCursor = sql.getAssetCategoriesCursor();
            writeSheetFromCursor(catSheet, catCursor);
            catCursor.close();

            // Sheet 2: Assets
            Sheet itemSheet = workbook.createSheet("Assets");
            Cursor itemCursor = sql.getAssetItemsCursor();
            writeSheetFromCursor(itemSheet, itemCursor);
            itemCursor.close();

            // Sheet 3: Net Worth History
            Sheet histSheet = workbook.createSheet("NetWorthHistory");
            Cursor histCursor = sql.getNetWorthHistoryCursor();
            writeSheetFromCursor(histSheet, histCursor);
            histCursor.close();

            // Save to Documents/MoneyTracker/
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH).format(new Date());
            String filename = "WealthData_" + timestamp + ".xls";

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MoneyTracker");

            ContentResolver resolver = requireContext().getContentResolver();
            Uri fileUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

            if (fileUri != null) {
                OutputStream os = resolver.openOutputStream(fileUri);
                workbook.write(os);
                if (os != null) os.close();
                workbook.close();
                Toast.makeText(requireContext(), "Exported to Documents/MoneyTracker/" + filename, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writeSheetFromCursor(Sheet sheet, Cursor cursor) {
        if (cursor == null) return;

        // Header row
        Row header = sheet.createRow(0);
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            header.createCell(i).setCellValue(cursor.getColumnName(i));
        }

        int rowIndex = 1;
        while (cursor.moveToNext()) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                Cell cell = row.createCell(i);
                switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_STRING:
                        cell.setCellValue(cursor.getString(i));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                    case Cursor.FIELD_TYPE_INTEGER:
                        cell.setCellValue(cursor.getDouble(i));
                        break;
                    default:
                        cell.setCellValue("");
                        break;
                }
            }
        }
    }

    // ========== CSV Import ==========

    private void openFilePicker() {
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

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
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
        if (category.isFdCategory()) {
            showAddFdDialog(category.getId());
        } else {
            showAddAssetDialog(category.getId());
        }
    }

    @Override
    public void onEditAsset(AssetItem item) {
        if (item.isFd()) {
            showEditFdDialog(item);
        } else {
            showEditAssetDialog(item);
        }
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

    // ========== Category Dialogs ==========

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        EditText etTarget = dialogView.findViewById(R.id.etTargetAllocation);
        CheckBox cbIsFd = dialogView.findViewById(R.id.cbIsFdCategory);
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
                    boolean isFd = cbIsFd.isChecked();
                    if (!name.isEmpty()) {
                        sql.insertCategory(name, selectedColor[0], target, isFd);
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
        CheckBox cbIsFd = dialogView.findViewById(R.id.cbIsFdCategory);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Edit Category");
        etName.setText(category.getName());
        cbIsFd.setChecked(category.isFdCategory());
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
                    boolean isFd = cbIsFd.isChecked();
                    if (!name.isEmpty()) {
                        sql.updateCategory(category.getId(), name, selectedColor[0], target, isFd);
                        loadData();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ========== Regular Asset Dialogs ==========

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

    // ========== FD Dialogs ==========

    private void showAddFdDialog(int categoryId) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_fd, null);
        EditText etName = dialogView.findViewById(R.id.etFdName);
        EditText etInvested = dialogView.findViewById(R.id.etFdInvested);
        EditText etRate = dialogView.findViewById(R.id.etFdInterestRate);
        EditText etCycle = dialogView.findViewById(R.id.etFdInterestCycle);
        RadioGroup rgType = dialogView.findViewById(R.id.rgInterestType);
        TextView tvCreditDate = dialogView.findViewById(R.id.tvFdCreditDate);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Add Fixed Deposit");

        final String[] selectedDate = {""};

        tvCreditDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                selectedDate[0] = String.format("%04d-%02d-%02d", year, month + 1, day);
                tvCreditDate.setText(selectedDate[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String investedStr = etInvested.getText().toString().trim();
                    String rateStr = etRate.getText().toString().trim();
                    String cycleStr = etCycle.getText().toString().trim();

                    if (name.isEmpty() || investedStr.isEmpty() || rateStr.isEmpty() ||
                            cycleStr.isEmpty() || selectedDate[0].isEmpty()) {
                        Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double invested = Double.parseDouble(investedStr);
                    double rate = Double.parseDouble(rateStr);
                    int cycle = Integer.parseInt(cycleStr);
                    String interestType = rgType.getCheckedRadioButtonId() == R.id.rbCompound ? "Compound" : "Simple";

                    sql.insertFdAssetItem(categoryId, name, invested, rate, cycle, selectedDate[0], interestType);
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditFdDialog(AssetItem item) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_fd, null);
        EditText etName = dialogView.findViewById(R.id.etFdName);
        EditText etInvested = dialogView.findViewById(R.id.etFdInvested);
        EditText etRate = dialogView.findViewById(R.id.etFdInterestRate);
        EditText etCycle = dialogView.findViewById(R.id.etFdInterestCycle);
        RadioGroup rgType = dialogView.findViewById(R.id.rgInterestType);
        RadioButton rbSimple = dialogView.findViewById(R.id.rbSimple);
        RadioButton rbCompound = dialogView.findViewById(R.id.rbCompound);
        TextView tvCreditDate = dialogView.findViewById(R.id.tvFdCreditDate);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Edit Fixed Deposit");

        etName.setText(item.getName());
        etInvested.setText(String.valueOf(item.getInvested()));
        etRate.setText(String.valueOf(item.getInterestRate()));
        etCycle.setText(String.valueOf(item.getInterestCycle()));

        if ("Compound".equalsIgnoreCase(item.getInterestType())) {
            rbCompound.setChecked(true);
        } else {
            rbSimple.setChecked(true);
        }

        final String[] selectedDate = {item.getInterestCreditDate() != null ? item.getInterestCreditDate() : ""};
        tvCreditDate.setText(selectedDate[0].isEmpty() ? "Tap to select date" : selectedDate[0]);

        tvCreditDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                selectedDate[0] = String.format("%04d-%02d-%02d", year, month + 1, day);
                tvCreditDate.setText(selectedDate[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(requireContext(), R.style.Theme_MoneyTracker_Dialog)
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String investedStr = etInvested.getText().toString().trim();
                    String rateStr = etRate.getText().toString().trim();
                    String cycleStr = etCycle.getText().toString().trim();

                    if (name.isEmpty() || investedStr.isEmpty() || rateStr.isEmpty() ||
                            cycleStr.isEmpty() || selectedDate[0].isEmpty()) {
                        Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double invested = Double.parseDouble(investedStr);
                    double rate = Double.parseDouble(rateStr);
                    int cycle = Integer.parseInt(cycleStr);
                    String interestType = rgType.getCheckedRadioButtonId() == R.id.rbCompound ? "Compound" : "Simple";

                    sql.updateFdAssetItem(item.getId(), name, invested, rate, cycle, selectedDate[0], interestType);
                    loadData();
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
        if (Math.abs(value) >= 10000000) {
            return String.format("₹ %.2f Cr", value / 10000000.0);
        } else if (Math.abs(value) >= 100000) {
            return String.format("₹ %.2f L", value / 100000.0);
        } else {
            return String.format("₹ %.2f", value);
        }
    }
}
