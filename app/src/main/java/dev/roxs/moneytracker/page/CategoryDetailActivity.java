package dev.roxs.moneytracker.page;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.Adapter.AssetDetailAdapter;
import dev.roxs.moneytracker.helper.SQl_Helper;
import dev.roxs.moneytracker.model.AssetCategory;
import dev.roxs.moneytracker.model.AssetItem;

public class CategoryDetailActivity extends AppCompatActivity implements AssetDetailAdapter.OnAssetActionListener {

    private SQl_Helper sql;
    private int categoryId;
    private AssetCategory currentCategory;
    private RecyclerView rvAssets;
    private TextView tvCategoryTitle, tvCategoryValue, tvCategoryPnl, tvCategoryAllocation, tvRebalanceSuggestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#0D1117"));
        }

        sql = new SQl_Helper(getApplicationContext());
        categoryId = getIntent().getIntExtra("categoryId", -1);

        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvCategoryValue = findViewById(R.id.tvCategoryValue);
        tvCategoryPnl = findViewById(R.id.tvCategoryPnl);
        tvCategoryAllocation = findViewById(R.id.tvCategoryAllocation);
        tvRebalanceSuggestion = findViewById(R.id.tvRebalanceSuggestion);
        rvAssets = findViewById(R.id.rvAssets);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnAddAsset = findViewById(R.id.btnAddAsset);
        btnAddAsset.setOnClickListener(v -> {
            if (currentCategory != null && currentCategory.isFdCategory()) {
                showAddFdDialog();
            } else {
                showAddAssetDialog();
            }
        });

        loadData();
    }

    private void loadData() {
        List<AssetCategory> allCategories = sql.getAllCategories();
        AssetCategory category = null;
        double totalNetWorth = 0;

        for (AssetCategory c : allCategories) {
            totalNetWorth += c.getTotalValue();
            if (c.getId() == categoryId) category = c;
        }

        if (category == null) {
            finish();
            return;
        }

        currentCategory = category;

        tvCategoryTitle.setText(category.getName());
        tvCategoryValue.setText(formatCurrency(category.getTotalValue()));

        double allocation = totalNetWorth > 0 ? (category.getTotalValue() / totalNetWorth) * 100 : 0;
        tvCategoryAllocation.setText(String.format("%.1f%%", allocation));

        // Calculate P&L for this category
        double catPnl = 0;
        double catInvested = 0;
        for (AssetItem item : category.getItems()) {
            catPnl += item.getPnl();
            catInvested += item.getInvested();
        }

        if (catInvested > 0) {
            double pct = (catPnl / catInvested) * 100;
            tvCategoryPnl.setText(String.format("%s%s (%.2f%%)",
                    catPnl >= 0 ? "+" : "", formatCurrency(catPnl), pct));
            tvCategoryPnl.setTextColor(Color.parseColor(catPnl >= 0 ? "#00E676" : "#FF5252"));
        } else {
            tvCategoryPnl.setText("");
        }

        List<AssetItem> items = category.getItems();
        AssetDetailAdapter adapter = new AssetDetailAdapter(this, items, this);
        rvAssets.setLayoutManager(new LinearLayoutManager(this));
        rvAssets.setAdapter(adapter);

        // Rebalance suggestion
        double target = category.getTargetAllocation();
        if (target > 0 && totalNetWorth > 0) {
            double rebalanceAmt = category.getRebalanceAmount(totalNetWorth);
            tvRebalanceSuggestion.setVisibility(View.VISIBLE);

            if (category.isOnTarget()) {
                tvRebalanceSuggestion.setText("✓ This category is on target");
                tvRebalanceSuggestion.setTextColor(Color.parseColor("#00E676"));
            } else if (rebalanceAmt > 0) {
                tvRebalanceSuggestion.setText(String.format("↑ Add %s to reach %.0f%% target",
                        formatCurrency(rebalanceAmt), target));
                tvRebalanceSuggestion.setTextColor(Color.parseColor("#FFA000"));
            } else {
                tvRebalanceSuggestion.setText(String.format("↓ Over-allocated by %s (target: %.0f%%)",
                        formatCurrency(Math.abs(rebalanceAmt)), target));
                tvRebalanceSuggestion.setTextColor(Color.parseColor("#FF5252"));
            }
        } else {
            tvRebalanceSuggestion.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEditAsset(AssetItem item) {
        if (item.isFd()) {
            showEditFdDialog(item);
        } else {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_asset, null);
            EditText etName = dialogView.findViewById(R.id.etAssetName);
            EditText etValue = dialogView.findViewById(R.id.etAssetValue);
            TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
            dialogTitle.setText("Edit Asset");
            etName.setText(item.getName());
            etValue.setText(String.valueOf(item.getValue()));

            new AlertDialog.Builder(this, R.style.Theme_MoneyTracker_Dialog)
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
    }

    @Override
    public void onDeleteAsset(AssetItem item) {
        new AlertDialog.Builder(this, R.style.Theme_MoneyTracker_Dialog)
                .setTitle("Delete Asset")
                .setMessage("Delete \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    sql.deleteAssetItem(item.getId());
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddAssetDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_asset, null);
        EditText etName = dialogView.findViewById(R.id.etAssetName);
        EditText etValue = dialogView.findViewById(R.id.etAssetValue);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Add Asset");

        new AlertDialog.Builder(this, R.style.Theme_MoneyTracker_Dialog)
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

    // ========== FD Dialogs ==========

    private void showAddFdDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_fd, null);
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
            new DatePickerDialog(this, (dp, year, month, day) -> {
                selectedDate[0] = String.format("%04d-%02d-%02d", year, month + 1, day);
                tvCreditDate.setText(selectedDate[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this, R.style.Theme_MoneyTracker_Dialog)
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String investedStr = etInvested.getText().toString().trim();
                    String rateStr = etRate.getText().toString().trim();
                    String cycleStr = etCycle.getText().toString().trim();
                    if (name.isEmpty() || investedStr.isEmpty() || rateStr.isEmpty() ||
                            cycleStr.isEmpty() || selectedDate[0].isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_fd, null);
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
            new DatePickerDialog(this, (dp, year, month, day) -> {
                selectedDate[0] = String.format("%04d-%02d-%02d", year, month + 1, day);
                tvCreditDate.setText(selectedDate[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this, R.style.Theme_MoneyTracker_Dialog)
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String investedStr = etInvested.getText().toString().trim();
                    String rateStr = etRate.getText().toString().trim();
                    String cycleStr = etCycle.getText().toString().trim();
                    if (name.isEmpty() || investedStr.isEmpty() || rateStr.isEmpty() ||
                            cycleStr.isEmpty() || selectedDate[0].isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
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

    private String formatCurrency(double value) {
        if (value >= 10000000) {
            return String.format("₹ %.2f Cr", value / 10000000.0);
        } else if (value >= 100000) {
            return String.format("₹ %.2f L", value / 100000.0);
        } else {
            return String.format("₹ %.2f", value);
        }
    }
}
