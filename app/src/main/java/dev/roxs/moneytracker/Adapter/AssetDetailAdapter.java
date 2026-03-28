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

public class AssetDetailAdapter extends RecyclerView.Adapter<AssetDetailAdapter.ViewHolder> {

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_asset_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssetItem item = items.get(position);

        holder.tvAssetName.setText(item.getName());
        holder.tvQty.setText(formatNumber(item.getQty()));
        holder.tvAvgCost.setText("₹ " + formatNumber(item.getAvgCost()));
        holder.tvLtp.setText("₹ " + formatNumber(item.getLtp()));
        holder.tvInvested.setText("₹ " + formatNumber(item.getInvested()));
        holder.tvCurrent.setText("₹ " + formatNumber(item.getValue()));

        // P&L coloring
        double pnl = item.getPnl();
        holder.tvPnl.setText((pnl >= 0 ? "+" : "") + "₹ " + formatNumber(pnl));
        holder.tvPnl.setTextColor(Color.parseColor(pnl >= 0 ? "#00E676" : "#FF5252"));

        // Day change
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

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatNumber(double val) {
        if (val == (long) val) return String.valueOf((long) val);
        return String.format("%.2f", val);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAssetName, tvQty, tvAvgCost, tvLtp, tvInvested, tvCurrent, tvPnl, tvDayChg;
        ImageView btnEditAsset, btnDeleteAsset;

        public ViewHolder(@NonNull View itemView) {
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
}
