package dev.roxs.moneytracker.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.model.AssetCategory;
import dev.roxs.moneytracker.model.AssetItem;
import dev.roxs.moneytracker.page.CategoryDetailActivity;

public class AssetCategoryAdapter extends RecyclerView.Adapter<AssetCategoryAdapter.ViewHolder> {

    public interface OnCategoryActionListener {
        void onEditCategory(AssetCategory category);
        void onDeleteCategory(AssetCategory category);
        void onAddAsset(AssetCategory category);
        void onEditAsset(AssetItem item);
        void onDeleteAsset(AssetItem item);
    }

    private final List<AssetCategory> categories;
    private final Context context;
    private final OnCategoryActionListener listener;

    public AssetCategoryAdapter(Context context, List<AssetCategory> categories, OnCategoryActionListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_asset_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetCategory category = categories.get(position);

        holder.tvCategoryName.setText(category.getName());

        // Format value
        double val = category.getTotalValue();
        holder.tvCategoryValue.setText(formatCurrency(val));
        holder.tvCategoryPercentage.setText(String.format("%.1f%%", category.getPercentage()));

        // Color indicator
        try {
            int color = Color.parseColor(category.getColor());
            GradientDrawable dot = (GradientDrawable) holder.colorIndicator.getBackground().mutate();
            dot.setColor(color);

            // Progress bar fill color
            GradientDrawable fill = (GradientDrawable) holder.progressBarFill.getBackground().mutate();
            fill.setColor(color);
        } catch (Exception e) {
            // fallback - keep default
        }

        // Progress bar: dual-fill with target marker
        holder.progressBarFill.post(() -> {
            ViewGroup progressParent = (ViewGroup) holder.progressBarFill.getParent();
            int fullWidth = progressParent.getWidth();
            double actual = category.getPercentage();
            double target2 = category.getTargetAllocation();

            if (target2 > 0) {
                // Dual-fill mode
                double basePct = Math.min(actual, target2);
                double diffPct = Math.abs(actual - target2);

                // Base fill (category color, up to min of actual/target)
                int baseWidth = (int) (fullWidth * (Math.min(basePct, 100.0) / 100.0));
                ViewGroup.LayoutParams baseParams = holder.progressBarFill.getLayoutParams();
                baseParams.width = baseWidth;
                holder.progressBarFill.setLayoutParams(baseParams);

                // Diff fill (red if over, green if under)
                holder.progressBarDiff.setVisibility(View.VISIBLE);
                int diffWidth = (int) (fullWidth * (Math.min(diffPct, 100.0 - basePct) / 100.0));
                RelativeLayout.LayoutParams diffParams = (RelativeLayout.LayoutParams) holder.progressBarDiff.getLayoutParams();
                diffParams.width = diffWidth;
                diffParams.setMarginStart(baseWidth);
                holder.progressBarDiff.setLayoutParams(diffParams);

                if (actual > target2) {
                    // Over-allocated: red diff
                    holder.progressBarDiff.setBackgroundColor(Color.parseColor("#66FF5252"));
                } else {
                    // Under-allocated: green diff (shows what you need to fill)
                    holder.progressBarDiff.setBackgroundColor(Color.parseColor("#4400E676"));
                }

                // Target marker line
                holder.targetMarker.setVisibility(View.VISIBLE);
                int markerPos = (int) (fullWidth * (Math.min(target2, 100.0) / 100.0));
                RelativeLayout.LayoutParams markerParams = (RelativeLayout.LayoutParams) holder.targetMarker.getLayoutParams();
                markerParams.setMarginStart(markerPos - 1);
                holder.targetMarker.setLayoutParams(markerParams);
            } else {
                // Simple mode: no target set
                int progressWidth = (int) (fullWidth * (Math.min(actual, 100.0) / 100.0));
                ViewGroup.LayoutParams params = holder.progressBarFill.getLayoutParams();
                params.width = progressWidth;
                holder.progressBarFill.setLayoutParams(params);

                holder.progressBarDiff.setVisibility(View.GONE);
                holder.targetMarker.setVisibility(View.GONE);
            }
        });

        // Target allocation row
        double target = category.getTargetAllocation();
        if (target > 0) {
            holder.allocationRow.setVisibility(View.VISIBLE);
            holder.tvTargetLabel.setText(String.format("Target: %.0f%%  |  Actual: %.1f%%", target, category.getPercentage()));

            if (category.isOnTarget()) {
                holder.tvAllocationStatus.setText("✓ On Track");
                holder.tvAllocationStatus.setTextColor(Color.parseColor("#00E676"));
            } else {
                double diff = category.getAllocationDiff();
                holder.tvAllocationStatus.setText(String.format("%s%.1f%%", diff > 0 ? "+" : "", diff));
                holder.tvAllocationStatus.setTextColor(Color.parseColor("#FF5252"));
            }
        } else {
            holder.allocationRow.setVisibility(View.GONE);
        }

        // Always show expandable section (no collapse toggle)
        holder.expandableSection.setVisibility(View.VISIBLE);

        holder.categoryHeader.setOnClickListener(v -> {
            Intent intent = new Intent(context, CategoryDetailActivity.class);
            intent.putExtra("categoryId", category.getId());
            context.startActivity(intent);
        });

        // Name click -> open detail page
        holder.tvCategoryName.setOnClickListener(v -> {
            Intent intent = new Intent(context, CategoryDetailActivity.class);
            intent.putExtra("categoryId", category.getId());
            context.startActivity(intent);
        });

        // Populate sub-assets
        holder.subAssetContainer.removeAllViews();
        List<AssetItem> items = category.getItems();
        if (items != null && !items.isEmpty()) {
            for (AssetItem item : items) {
                if (item.isFd()) {
                    // Inflate FD-specific sub-item layout
                    View fdView = LayoutInflater.from(context).inflate(R.layout.item_asset_fd_sub, holder.subAssetContainer, false);

                    TextView tvFdName = fdView.findViewById(R.id.tvFdName);
                    TextView tvFdInterestType = fdView.findViewById(R.id.tvFdInterestType);
                    TextView tvFdInvested = fdView.findViewById(R.id.tvFdInvested);
                    TextView tvFdRate = fdView.findViewById(R.id.tvFdRate);
                    TextView tvFdCycle = fdView.findViewById(R.id.tvFdCycle);
                    TextView tvFdCurrentValue = fdView.findViewById(R.id.tvFdCurrentValue);
                    TextView tvFdEarned = fdView.findViewById(R.id.tvFdEarned);
                    TextView tvFdNextCredit = fdView.findViewById(R.id.tvFdNextCredit);
                    ImageView btnEditFd = fdView.findViewById(R.id.btnEditFd);
                    ImageView btnDeleteFd = fdView.findViewById(R.id.btnDeleteFd);

                    tvFdName.setText(item.getName());
                    tvFdInterestType.setText(item.getInterestType() != null ? item.getInterestType() : "Simple");
                    tvFdInvested.setText(formatCurrency(item.getInvested()));
                    tvFdRate.setText(String.format("%.2f%%", item.getInterestRate()));
                    tvFdCycle.setText(item.getCycleLabel());
                    tvFdCurrentValue.setText(formatCurrency(item.getValue()));
                    tvFdEarned.setText("+" + formatCurrency(item.getPnl()));
                    tvFdNextCredit.setText(item.getInterestCreditDate() != null ? item.getInterestCreditDate() : "--");

                    btnEditFd.setOnClickListener(v -> {
                        if (listener != null) listener.onEditAsset(item);
                    });
                    btnDeleteFd.setOnClickListener(v -> {
                        if (listener != null) listener.onDeleteAsset(item);
                    });

                    holder.subAssetContainer.addView(fdView);
                } else {
                    // Regular asset sub-item
                    View subView = LayoutInflater.from(context).inflate(R.layout.item_asset_sub, holder.subAssetContainer, false);

                    TextView tvName = subView.findViewById(R.id.tvItemName);
                    TextView tvValue = subView.findViewById(R.id.tvItemValue);
                    TextView tvPercentage = subView.findViewById(R.id.tvItemPercentage);
                    ImageView btnEdit = subView.findViewById(R.id.btnEditItem);
                    ImageView btnDelete = subView.findViewById(R.id.btnDeleteItem);

                    tvName.setText(item.getName());
                    tvValue.setText(formatCurrency(item.getValue()));
                    tvPercentage.setText(String.format("(%.1f%%)", item.getPercentage()));

                    btnEdit.setOnClickListener(v -> {
                        if (listener != null) listener.onEditAsset(item);
                    });
                    btnDelete.setOnClickListener(v -> {
                        if (listener != null) listener.onDeleteAsset(item);
                    });

                    holder.subAssetContainer.addView(subView);
                }
            }
        }

        // Action buttons
        holder.btnEditCategory.setOnClickListener(v -> {
            if (listener != null) listener.onEditCategory(category);
        });

        holder.btnDeleteCategory.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteCategory(category);
        });

        holder.btnAddAsset.setOnClickListener(v -> {
            if (listener != null) listener.onAddAsset(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    private String formatCurrency(double value) {
        if (value >= 10000000) {
            return String.format("₹ %.2f Cr", value / 10000000.0);
        } else if (value >= 100000) {
            return String.format("₹ %.2f L", value / 100000.0);
        } else if (value >= 1000) {
            return String.format("₹ %.1f K", value / 1000.0);
        } else {
            return String.format("₹ %.0f", value);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View colorIndicator;
        TextView tvCategoryName, tvCategoryValue, tvCategoryPercentage;
        ImageView btnEditCategory, btnDeleteCategory;
        View progressBarFill, progressBarDiff, targetMarker;
        RelativeLayout categoryHeader;
        LinearLayout expandableSection, subAssetContainer, allocationRow;
        TextView btnAddAsset, tvTargetLabel, tvAllocationStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryValue = itemView.findViewById(R.id.tvCategoryValue);
            tvCategoryPercentage = itemView.findViewById(R.id.tvCategoryPercentage);
            btnEditCategory = itemView.findViewById(R.id.btnEditCategory);
            btnDeleteCategory = itemView.findViewById(R.id.btnDeleteCategory);
            progressBarFill = itemView.findViewById(R.id.progressBarFill);
            progressBarDiff = itemView.findViewById(R.id.progressBarDiff);
            targetMarker = itemView.findViewById(R.id.targetMarker);
            categoryHeader = itemView.findViewById(R.id.categoryHeader);
            expandableSection = itemView.findViewById(R.id.expandableSection);
            subAssetContainer = itemView.findViewById(R.id.subAssetContainer);
            btnAddAsset = itemView.findViewById(R.id.btnAddAsset);
            allocationRow = itemView.findViewById(R.id.allocationRow);
            tvTargetLabel = itemView.findViewById(R.id.tvTargetLabel);
            tvAllocationStatus = itemView.findViewById(R.id.tvAllocationStatus);
        }
    }
}
