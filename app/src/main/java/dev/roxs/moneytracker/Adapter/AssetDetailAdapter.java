package dev.roxs.moneytracker.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.model.AssetItem;

public class AssetDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_STOCK = 0;
    private static final int TYPE_FD = 1;

    public interface OnAssetActionListener {
        void onEditAsset(AssetItem item);
        void onDeleteAsset(AssetItem item);
    }

    private final Context context;
    private final List<AssetItem> items;
    private final OnAssetActionListener listener;

    public AssetDetailAdapter(Context context, List<AssetItem> items, OnAssetActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isFd() ? TYPE_FD : TYPE_STOCK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_asset_fd_detail, parent, false);
            return new FdViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_asset_detail, parent, false);
            return new StockViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AssetItem item = items.get(position);

        if (holder instanceof FdViewHolder) {
            bindFd((FdViewHolder) holder, item);
        } else {
            bindStock((StockViewHolder) holder, item);
        }
    }

    private void bindStock(StockViewHolder holder, AssetItem item) {
        holder.tvAssetName.setText(item.getName());
        holder.tvQty.setText(formatNumber(item.getQty()));
        holder.tvAvgCost.setText("₹ " + formatNumber(item.getAvgCost()));
        holder.tvLtp.setText("₹ " + formatNumber(item.getLtp()));
        holder.tvInvested.setText(String.format("₹ %s (%.1f%%)", formatNumber(item.getInvested()), item.getPercentage()));
        holder.tvCurrent.setText("₹ " + formatNumber(item.getValue()));

        double pnl = item.getPnl();
        holder.tvPnl.setText((pnl >= 0 ? "+" : "") + "₹ " + formatNumber(pnl));
        holder.tvPnl.setTextColor(Color.parseColor(pnl >= 0 ? "#00E676" : "#FF5252"));

        double dayChg = item.getDayChg();
        holder.tvDayChg.setText(String.format("Day: %s%.2f%%", dayChg >= 0 ? "+" : "", dayChg));
        holder.tvDayChg.setTextColor(Color.parseColor(dayChg >= 0 ? "#00E676" : "#FF5252"));

        holder.btnEditAsset.setOnClickListener(v -> {
            if (listener != null) listener.onEditAsset(item);
        });
        holder.btnDeleteAsset.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteAsset(item);
        });
    }

    private void bindFd(FdViewHolder holder, AssetItem item) {
        holder.tvFdDetailName.setText(item.getName());
        holder.tvFdDetailType.setText(item.getInterestType() != null ? item.getInterestType() : "Simple");
        holder.tvFdDetailInvested.setText("₹ " + formatNumber(item.getInvested()));
        holder.tvFdDetailRate.setText(String.format("%.2f%%", item.getInterestRate()));
        holder.tvFdDetailCycle.setText(item.getCycleLabel());
        holder.tvFdDetailCurrent.setText("₹ " + formatNumber(item.getValue()));
        holder.tvFdDetailEarned.setText("+₹ " + formatNumber(item.getPnl()));
        holder.tvFdDetailNextCredit.setText(item.getInterestCreditDate() != null ? item.getInterestCreditDate() : "--");

        holder.btnEditFdDetail.setOnClickListener(v -> {
            if (listener != null) listener.onEditAsset(item);
        });
        holder.btnDeleteFdDetail.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteAsset(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatNumber(double val) {
        if (val == (long) val) return String.valueOf((long) val);
        return String.format("%.2f", val);
    }

    // ========== ViewHolders ==========

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvAssetName, tvQty, tvAvgCost, tvLtp, tvInvested, tvCurrent, tvPnl, tvDayChg;
        ImageView btnEditAsset, btnDeleteAsset;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAssetName = itemView.findViewById(R.id.tvAssetName);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvAvgCost = itemView.findViewById(R.id.tvAvgCost);
            tvLtp = itemView.findViewById(R.id.tvLtp);
            tvInvested = itemView.findViewById(R.id.tvInvested);
            tvCurrent = itemView.findViewById(R.id.tvCurrent);
            tvPnl = itemView.findViewById(R.id.tvPnl);
            tvDayChg = itemView.findViewById(R.id.tvDayChg);
            btnEditAsset = itemView.findViewById(R.id.btnEditAsset);
            btnDeleteAsset = itemView.findViewById(R.id.btnDeleteAsset);
        }
    }

    public static class FdViewHolder extends RecyclerView.ViewHolder {
        TextView tvFdDetailName, tvFdDetailType, tvFdDetailInvested, tvFdDetailRate;
        TextView tvFdDetailCycle, tvFdDetailCurrent, tvFdDetailEarned, tvFdDetailNextCredit;
        ImageView btnEditFdDetail, btnDeleteFdDetail;

        public FdViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFdDetailName = itemView.findViewById(R.id.tvFdDetailName);
            tvFdDetailType = itemView.findViewById(R.id.tvFdDetailType);
            tvFdDetailInvested = itemView.findViewById(R.id.tvFdDetailInvested);
            tvFdDetailRate = itemView.findViewById(R.id.tvFdDetailRate);
            tvFdDetailCycle = itemView.findViewById(R.id.tvFdDetailCycle);
            tvFdDetailCurrent = itemView.findViewById(R.id.tvFdDetailCurrent);
            tvFdDetailEarned = itemView.findViewById(R.id.tvFdDetailEarned);
            tvFdDetailNextCredit = itemView.findViewById(R.id.tvFdDetailNextCredit);
            btnEditFdDetail = itemView.findViewById(R.id.btnEditFdDetail);
            btnDeleteFdDetail = itemView.findViewById(R.id.btnDeleteFdDetail);
        }
    }
}
