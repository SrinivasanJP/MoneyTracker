package dev.roxs.moneytracker.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Zerodha-style holdings CSV and returns structured data.
 */
public class CsvImporter {

    public static class HoldingRow {
        public String instrument;
        public double qty;
        public double avgCost;
        public double ltp;
        public double invested;
        public double curVal;
        public double pnl;
        public double netChg;
        public double dayChg;

        public HoldingRow(String instrument, double qty, double avgCost, double ltp,
                          double invested, double curVal, double pnl, double netChg, double dayChg) {
            this.instrument = instrument;
            this.qty = qty;
            this.avgCost = avgCost;
            this.ltp = ltp;
            this.invested = invested;
            this.curVal = curVal;
            this.pnl = pnl;
            this.netChg = netChg;
            this.dayChg = dayChg;
        }
    }

    /**
     * Parse a CSV InputStream. Expects Zerodha-style format:
     * "Instrument","Qty.","Avg. cost","LTP","Invested","Cur. val","P&L","Net chg.","Day chg.",""
     */
    public static List<HoldingRow> parse(InputStream inputStream) throws Exception {
        List<HoldingRow> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        boolean isHeader = true;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (isHeader) {
                isHeader = false;
                continue; // Skip header row
            }

            List<String> fields = parseCsvLine(line);
            if (fields.size() < 9) continue;

            try {
                String instrument = fields.get(0).trim();
                double qty = parseDouble(fields.get(1));
                double avgCost = parseDouble(fields.get(2));
                double ltp = parseDouble(fields.get(3));
                double invested = parseDouble(fields.get(4));
                double curVal = parseDouble(fields.get(5));
                double pnl = parseDouble(fields.get(6));
                double netChg = parseDouble(fields.get(7));
                double dayChg = parseDouble(fields.get(8));

                rows.add(new HoldingRow(instrument, qty, avgCost, ltp, invested, curVal, pnl, netChg, dayChg));
            } catch (NumberFormatException e) {
                // Skip malformed rows
            }
        }
        reader.close();
        return rows;
    }

    /**
     * Parse a CSV line handling quoted fields (fields may contain commas inside quotes).
     */
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    private static double parseDouble(String s) {
        s = s.trim().replace("\"", "");
        if (s.isEmpty()) return 0;
        return Double.parseDouble(s);
    }
}
