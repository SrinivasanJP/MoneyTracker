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

    // FD-specific fields
    private double interestRate;       // Annual interest rate (%)
    private int interestCycle;         // In months (1=monthly, 3=quarterly, 12=yearly)
    private String interestCreditDate; // Next credit date (yyyy-MM-dd)
    private boolean isFd;              // true if this is a Fixed Deposit
    private String interestType;       // "Simple" or "Compound"

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

    /** Constructor for FD items */
    public AssetItem(int id, int categoryId, String name, double invested,
                     double interestRate, int interestCycle, String interestCreditDate,
                     String interestType, double currentValue, double pnl) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.invested = invested;
        this.value = currentValue;
        this.pnl = pnl;
        this.interestRate = interestRate;
        this.interestCycle = interestCycle;
        this.interestCreditDate = interestCreditDate;
        this.interestType = interestType;
        this.isFd = true;
        this.percentage = 0;
    }

    /**
     * Calculates interest amount for one cycle.
     * Simple: Principal * rate * (cycleMonths / 12)
     * Compound: currentValue * rate * (cycleMonths / 12)
     */
    public double calculateInterestAmount() {
        double periodFraction = interestCycle / 12.0;
        if ("Compound".equalsIgnoreCase(interestType)) {
            return value * (interestRate / 100.0) * periodFraction;
        } else {
            // Simple interest on original invested amount
            return invested * (interestRate / 100.0) * periodFraction;
        }
    }

    /**
     * Returns a human-readable cycle label.
     */
    public String getCycleLabel() {
        switch (interestCycle) {
            case 1: return "Monthly";
            case 3: return "Quarterly";
            case 6: return "Half-Yearly";
            case 12: return "Yearly";
            default: return interestCycle + " Months";
        }
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

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public int getInterestCycle() { return interestCycle; }
    public void setInterestCycle(int interestCycle) { this.interestCycle = interestCycle; }

    public String getInterestCreditDate() { return interestCreditDate; }
    public void setInterestCreditDate(String interestCreditDate) { this.interestCreditDate = interestCreditDate; }

    public boolean isFd() { return isFd; }
    public void setFd(boolean fd) { isFd = fd; }

    public String getInterestType() { return interestType; }
    public void setInterestType(String interestType) { this.interestType = interestType; }
}
