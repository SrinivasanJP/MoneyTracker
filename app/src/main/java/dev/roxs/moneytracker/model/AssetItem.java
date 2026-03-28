package dev.roxs.moneytracker.model;

public class AssetItem {
    private int id;
    private int categoryId;
    private String name;
    private double value;
    private double percentage;
    private double qty;
    private double avgCost;
    private double ltp;
    private double invested;
    private double pnl;
    private double netChg;
    private double dayChg;

    public AssetItem(int id, int categoryId, String name, double value) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.value = value;
        this.percentage = 0;
    }

    public AssetItem(int id, int categoryId, String name, double value,
                     double qty, double avgCost, double ltp, double invested,
                     double pnl, double netChg, double dayChg) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.value = value;
        this.percentage = 0;
        this.qty = qty;
        this.avgCost = avgCost;
        this.ltp = ltp;
        this.invested = invested;
        this.pnl = pnl;
        this.netChg = netChg;
        this.dayChg = dayChg;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public double getQty() { return qty; }
    public void setQty(double qty) { this.qty = qty; }

    public double getAvgCost() { return avgCost; }
    public void setAvgCost(double avgCost) { this.avgCost = avgCost; }

    public double getLtp() { return ltp; }
    public void setLtp(double ltp) { this.ltp = ltp; }

    public double getInvested() { return invested; }
    public void setInvested(double invested) { this.invested = invested; }

    public double getPnl() { return pnl; }
    public void setPnl(double pnl) { this.pnl = pnl; }

    public double getNetChg() { return netChg; }
    public void setNetChg(double netChg) { this.netChg = netChg; }

    public double getDayChg() { return dayChg; }
    public void setDayChg(double dayChg) { this.dayChg = dayChg; }
}
