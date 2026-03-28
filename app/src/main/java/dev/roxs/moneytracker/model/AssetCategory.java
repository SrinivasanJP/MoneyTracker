package dev.roxs.moneytracker.model;

import java.util.ArrayList;
import java.util.List;

public class AssetCategory {
    private int id;
    private String name;
    private String color;
    private int sortOrder;
    private double totalValue;
    private double percentage;
    private double targetAllocation; // target % of total portfolio
    private List<AssetItem> items;
    private boolean expanded;

    public AssetCategory(int id, String name, String color, int sortOrder, double targetAllocation) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
        this.targetAllocation = targetAllocation;
        this.totalValue = 0;
        this.percentage = 0;
        this.items = new ArrayList<>();
        this.expanded = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public double getTargetAllocation() { return targetAllocation; }
    public void setTargetAllocation(double targetAllocation) { this.targetAllocation = targetAllocation; }

    public List<AssetItem> getItems() { return items; }
    public void setItems(List<AssetItem> items) { this.items = items; }

    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    /**
     * Check if current allocation is within ±2% of target.
     */
    public boolean isOnTarget() {
        if (targetAllocation <= 0) return true; // no target set
        return Math.abs(percentage - targetAllocation) <= 2.0;
    }

    /**
     * Get the difference (actual - target). Positive = over-allocated, negative = under-allocated.
     */
    public double getAllocationDiff() {
        return percentage - targetAllocation;
    }

    /**
     * Get how much money to add/remove to reach target allocation.
     * @param totalNetWorth total portfolio value
     */
    public double getRebalanceAmount(double totalNetWorth) {
        if (targetAllocation <= 0) return 0;
        double targetValue = (targetAllocation / 100.0) * totalNetWorth;
        return targetValue - totalValue;
    }

    public void computeTotalValue() {
        totalValue = 0;
        for (AssetItem item : items) {
            totalValue += item.getValue();
        }
    }

    public void computeItemPercentages() {
        double totalInvested = 0;
        for (AssetItem item : items) {
            totalInvested += item.getInvested();
        }
        if (totalInvested <= 0) return;
        for (AssetItem item : items) {
            item.setPercentage((item.getInvested() / totalInvested) * 100.0);
        }
    }
}
