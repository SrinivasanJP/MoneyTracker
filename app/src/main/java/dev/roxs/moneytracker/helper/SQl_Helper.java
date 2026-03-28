package dev.roxs.moneytracker.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import dev.roxs.moneytracker.model.AssetCategory;
import dev.roxs.moneytracker.model.AssetItem;

public class SQl_Helper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "money_tracker.db";
    private static final int DATABASE_VERSION = 5;

    public static final String TABLE_NAME = "money_tracking";
    public static final String COL_ID = "id";
    public static final String COL_DATE = "date";
    public static final String COL_DAY = "day";
    public static final String COL_SPENT = "spent";
    public static final String COL_SOFTCASH = "softcash";
    public static final String COL_HARDCASH = "hardcash";
    public static final String COL_INVESTMENTS = "investments";
    public static final String COL_HOLDINGS = "holdings";
    public static final String COL_CREDIT = "credit";
    public static final String COL_LOAN = "friendly_loan";
    public static final String COL_REMARKS = "remarks";

    // ========== ASSET ALLOCATION TABLES ==========
    public static final String TABLE_ASSET_CATEGORIES = "asset_categories";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";
    public static final String COL_CAT_COLOR = "color";
    public static final String COL_CAT_SORT_ORDER = "sort_order";
    public static final String COL_CAT_TARGET_ALLOC = "target_allocation";

    public static final String TABLE_ASSET_ITEMS = "asset_items";
    public static final String COL_ITEM_ID = "id";
    public static final String COL_ITEM_CATEGORY_ID = "category_id";
    public static final String COL_ITEM_NAME = "name";
    public static final String COL_ITEM_VALUE = "value";
    public static final String COL_ITEM_QTY = "qty";
    public static final String COL_ITEM_AVG_COST = "avg_cost";
    public static final String COL_ITEM_LTP = "ltp";
    public static final String COL_ITEM_INVESTED = "invested";
    public static final String COL_ITEM_PNL = "pnl";
    public static final String COL_ITEM_NET_CHG = "net_chg";
    public static final String COL_ITEM_DAY_CHG = "day_chg";

    public SQl_Helper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static class DB_STRUCT{
        public Double spent;
        public Double softCash;
        public Double hardCash;
        public Double investments;
        public Double holdings;
        public Double credits;
        public Double loan;
        public String remarks;

        public DB_STRUCT(Double spent, Double softCash, Double hardCash, Double investments, Double holdings, Double credits, Double loan, String remarks) {
            this.spent = spent;
            this.softCash = softCash;
            this.hardCash = hardCash;
            this.investments = investments;
            this.holdings = holdings;
            this.credits = credits;
            this.loan = loan;
            this.remarks = remarks;
        }
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DATE + " TEXT UNIQUE, " +
                COL_DAY + " TEXT, " +
                COL_SPENT + " REAL, " +
                COL_SOFTCASH + " REAL, " +
                COL_HARDCASH + " REAL, " +
                COL_INVESTMENTS + " REAL, " +
                COL_HOLDINGS + " REAL, " +
                COL_CREDIT + " REAL, " +
                COL_LOAN + " REAL, " +
                COL_REMARKS + " TEXT" +
                ")";
        db.execSQL(CREATE_TABLE);
        createAssetTables(db);
    }

    private void createAssetTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSET_CATEGORIES + " (" +
                COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CAT_NAME + " TEXT, " +
                COL_CAT_COLOR + " TEXT, " +
                COL_CAT_SORT_ORDER + " INTEGER, " +
                COL_CAT_TARGET_ALLOC + " REAL DEFAULT 0" +
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSET_ITEMS + " (" +
                COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ITEM_CATEGORY_ID + " INTEGER, " +
                COL_ITEM_NAME + " TEXT, " +
                COL_ITEM_VALUE + " REAL, " +
                COL_ITEM_QTY + " REAL DEFAULT 0, " +
                COL_ITEM_AVG_COST + " REAL DEFAULT 0, " +
                COL_ITEM_LTP + " REAL DEFAULT 0, " +
                COL_ITEM_INVESTED + " REAL DEFAULT 0, " +
                COL_ITEM_PNL + " REAL DEFAULT 0, " +
                COL_ITEM_NET_CHG + " REAL DEFAULT 0, " +
                COL_ITEM_DAY_CHG + " REAL DEFAULT 0, " +
                "FOREIGN KEY(" + COL_ITEM_CATEGORY_ID + ") REFERENCES " + TABLE_ASSET_CATEGORIES + "(" + COL_CAT_ID + ") ON DELETE CASCADE" +
                ")");

        seedDefaultCategories(db);
    }

    private void seedDefaultCategories(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ASSET_CATEGORIES, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        if (count > 0) return;

        String[][] defaults = {
                {"Equity", "#4CAF50"},
                {"Bonds", "#2196F3"},
                {"Metals", "#FFA000"},
                {"Cash", "#9C27B0"},
                {"Real Estate", "#FF5722"}
        };
        for (int i = 0; i < defaults.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(COL_CAT_NAME, defaults[i][0]);
            cv.put(COL_CAT_COLOR, defaults[i][1]);
            cv.put(COL_CAT_SORT_ORDER, i);
            db.insert(TABLE_ASSET_CATEGORIES, null, cv);
        }
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            createAssetTables(db);
        }
        if (oldVersion < 4) {
            // Add new columns to asset_items
            String[] newCols = {COL_ITEM_QTY, COL_ITEM_AVG_COST, COL_ITEM_LTP, COL_ITEM_INVESTED, COL_ITEM_PNL, COL_ITEM_NET_CHG, COL_ITEM_DAY_CHG};
            for (String col : newCols) {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_ASSET_ITEMS + " ADD COLUMN " + col + " REAL DEFAULT 0");
                } catch (Exception e) {
                    // Column may already exist
                }
            }
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ASSET_CATEGORIES + " ADD COLUMN " + COL_CAT_TARGET_ALLOC + " REAL DEFAULT 0");
            } catch (Exception e) { /* already exists */ }
        }
    }
    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null); // Deletes all rows
        db.close();
    }


    public DB_STRUCT getYesterdaysHoldings(String formattedDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        LocalDate dateLocalDate = LocalDate.parse(formattedDate, formatter);
        dateLocalDate = dateLocalDate.minusDays(1); // Move to yesterday
        formattedDate = DateTimeHelper.formatToDisplayDate(dateLocalDate); // Adjust date to string format

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + COL_SOFTCASH + ", " + COL_HARDCASH + ", " + COL_HOLDINGS +
                        " FROM " + TABLE_NAME +
                        " WHERE " + COL_DATE + " = ?",
                new String[]{formattedDate}
        );

        DB_STRUCT result = null;
        if (cursor.moveToFirst()) {
            double softCash = cursor.getDouble(0);
            double hardCash = cursor.getDouble(1);
            double holdings = cursor.getDouble(2);

            result = new DB_STRUCT(
                    0.0,       // spent
                    softCash,
                    hardCash,
                    0.0,       // investments
                    holdings,
                    0.0,       // credits
                    0.0,       // loan
                    ""        // remarks
            );
        }

        cursor.close();
        return result;
    }


    public void insertOrUpdateEntry(String date, String day, double softcash, double hardcash, double investments, double credit, double loan, String remarks, double holdings, double spent) {
        SQLiteDatabase db = this.getWritableDatabase();



        ContentValues values = new ContentValues();
        values.put(COL_DATE, date);
        values.put(COL_DAY, day);
        values.put(COL_SOFTCASH, softcash);
        values.put(COL_HARDCASH, hardcash);
        values.put(COL_INVESTMENTS, investments);
        values.put(COL_HOLDINGS, holdings);
        values.put(COL_CREDIT, credit);
        values.put(COL_LOAN, loan);
        values.put(COL_SPENT, spent);
        values.put(COL_REMARKS, remarks);

        long id = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    private double getPreviousHoldings(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_HOLDINGS + " FROM " + TABLE_NAME + " WHERE date < ? ORDER BY date DESC LIMIT 1", new String[]{date});

        double previousHoldings = 0;
        if (cursor.moveToFirst()) {
            previousHoldings = cursor.getDouble(0);
        }
        cursor.close();
        return previousHoldings;
    }

    public double getBalanceLeft() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_HOLDINGS + " FROM " + TABLE_NAME + " ORDER BY date DESC LIMIT 1", null);

        double balance = 0;
        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(0);
        }
        cursor.close();
        return balance;
    }

    public double getDay1Holdings() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_HOLDINGS + " FROM " + TABLE_NAME + " ORDER BY date ASC LIMIT 1", null);

        double holdings = 0;
        if (cursor.moveToFirst()) {
            holdings = cursor.getDouble(0);
        }
        cursor.close();
        return holdings;
    }

    public double getTotalSpentThisMonth(String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE date LIKE ?", new String[]{month + "%"});

        double totalSpent = 0;
        if (cursor.moveToFirst()) {
            totalSpent = cursor.getDouble(0);
        }
        cursor.close();
        return totalSpent;
    }
    public DB_STRUCT getEntryByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_DATE + " = ?", new String[]{date});

        DB_STRUCT SQlHelper_Struct = null;
        if (cursor.moveToFirst()) {
            double spent = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SPENT));
            double softcash = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SOFTCASH));
            double hardcash = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_HARDCASH));
            double investments = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_INVESTMENTS));
            double holdings = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_HOLDINGS));
            double credit = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CREDIT));
            double loan = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOAN));
            String remarks = cursor.getString(cursor.getColumnIndexOrThrow(COL_REMARKS));

            SQlHelper_Struct = new DB_STRUCT(spent,softcash,hardcash,investments,holdings,credit,loan,remarks);

        }

        cursor.close();
        return SQlHelper_Struct;
    }
    public ArrayList<Integer> getAllRecordedDates(int month, int year) {
        ArrayList<Integer> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Format as "yyyy-MM"
        String formattedLike = String.format("%04d-%02d", year, month);  // e.g., "2025-07"

        Cursor cursor = db.rawQuery(
                "SELECT " + COL_DATE + " FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{formattedLike + "%"}
        );

        if (cursor.moveToFirst()) {
            do {
                String fullDate = cursor.getString(0);  // "2025-07-21"
                int day = Integer.parseInt(fullDate.split("-")[2]);  // Get "21"
                dates.add(day);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return dates;
    }



    public double getSumSpentCurrentMonth() {
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        Log.d("UT CM", "getSumSpentCurrentMonth: " + currentMonth);
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{currentMonth + "%"} // e.g., '2025-07%'
        );

        double totalSpent = 0;
        if (cursor.moveToFirst()) {
            totalSpent = cursor.getDouble(0);
        }

        cursor.close();
        return totalSpent;
    }


    public double getHoldingsFromEarliestDateThisMonth() {
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_HOLDINGS + " FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ? ORDER BY " + COL_DATE + " ASC LIMIT 1",
                new String[]{currentMonth+"%"}
        );

        double holdings = 0;
        if (cursor.moveToFirst()) {
            holdings = cursor.getDouble(0);
        }

        cursor.close();
        return holdings;
    }

    public double getTodaysSpent() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_SPENT + " FROM " + TABLE_NAME + " WHERE " + COL_DATE + " = ?",
                new String[]{today}
        );

        double spent = -1;
        if (cursor.moveToFirst()) {
            spent = cursor.getDouble(0);
        }

        cursor.close();
        return spent;
    }
    public boolean isTodaysRecordAvailable() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + TABLE_NAME + " WHERE " + COL_DATE + " = ? LIMIT 1",
                new String[]{today}
        );

        boolean exists = cursor.moveToFirst(); // true if a row exists
        cursor.close();
        return exists;
    }



    public double getMonthlySpentPercentageChange() {
        Calendar calendar = Calendar.getInstance();
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(calendar.getTime());

        calendar.add(Calendar.MONTH, -1);
        String lastMonth = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(calendar.getTime());

        double currentSpent = getMonthlyTotalSpent(currentMonth);
        double lastSpent = getMonthlyTotalSpent(lastMonth);

        Log.d("UT", "getMonthlySpentPercentageChange: "+ currentSpent+"---"+ lastSpent);

        if (lastSpent == 0) {
            return currentSpent == 0 ? 0 : 100; // Avoid divide-by-zero
        }

        return ((currentSpent - lastSpent) / lastSpent) * 100;
    }

    public double getMonthlyTotalSpent(String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalSpent = 0;

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{monthYear+"%"}
        );

        if (cursor.moveToFirst()) {
            totalSpent = cursor.getDouble(0);
        }
        cursor.close();

        return totalSpent;
    }




    public double getAverageSpentForMonth(LocalDate date) {
        String pattern = String.format("%04d-%02d", date.getYear(), date.getMonthValue()) + "%"; // yyyy-MM%
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT AVG(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{pattern}
        );

        double avgSpent = 0;
        if (cursor.moveToFirst()) {
            avgSpent = cursor.getDouble(0);
        }
        cursor.close();
        return avgSpent;
    }



    public double getTotalInvestmentsForMonth(LocalDate date) {
        String pattern = String.format("%04d-%02d", date.getYear(), date.getMonthValue()) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_INVESTMENTS + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{pattern}
        );

        double totalInvestments = 0;
        if (cursor.moveToFirst()) {
            totalInvestments = cursor.getDouble(0);
        }
        cursor.close();
        return totalInvestments;
    }

    public double getLastLoanForMonth(LocalDate date) {
        String pattern = String.format("%04d-%02d", date.getYear(), date.getMonthValue()) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_LOAN + " FROM " + TABLE_NAME +
                        " WHERE " + COL_DATE + " LIKE ? ORDER BY " + COL_DATE + " DESC LIMIT 1",
                new String[]{pattern}
        );

        double loan = -1;
        if (cursor.moveToFirst()) {
            loan = cursor.getDouble(0);
        }
        cursor.close();
        return loan;
    }

    // ========== NEW STATISTICS METHODS ==========

    /**
     * Returns yearly total spent for a given year.
     */
    public double getYearlyTotalSpent(int year) {
        String pattern = String.format("%04d", year) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{pattern}
        );
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    /**
     * Returns yearly total investments for a given year.
     */
    public double getYearlyTotalInvestments(int year) {
        String pattern = String.format("%04d", year) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_INVESTMENTS + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{pattern}
        );
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    /**
     * Returns yearly average daily spending for a given year.
     */
    public double getYearlyAverageDaily(int year) {
        String pattern = String.format("%04d", year) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT AVG(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{pattern}
        );
        double avg = 0;
        if (cursor.moveToFirst()) avg = cursor.getDouble(0);
        cursor.close();
        return avg;
    }

    /**
     * Returns the date string of the highest spent day in a given month.
     * Returns "N/A" if no data.
     */
    public String getHighestSpentDayThisMonth(LocalDate date) {
        String pattern = String.format("%04d-%02d", date.getYear(), date.getMonthValue()) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_DATE + ", " + COL_SPENT + " FROM " + TABLE_NAME +
                        " WHERE " + COL_DATE + " LIKE ? ORDER BY " + COL_SPENT + " DESC LIMIT 1",
                new String[]{pattern}
        );
        String result = "N/A";
        if (cursor.moveToFirst()) {
            String d = cursor.getString(0);
            double s = cursor.getDouble(1);
            int day = Integer.parseInt(d.split("-")[2]);
            result = "Day " + day + " — Rs. " + String.format("%.2f", s);
        }
        cursor.close();
        return result;
    }

    /**
     * Returns the date string of the lowest spent day (non-zero) in a given month.
     * Returns "N/A" if no data.
     */
    public String getLowestSpentDayThisMonth(LocalDate date) {
        String pattern = String.format("%04d-%02d", date.getYear(), date.getMonthValue()) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_DATE + ", " + COL_SPENT + " FROM " + TABLE_NAME +
                        " WHERE " + COL_DATE + " LIKE ? AND " + COL_SPENT + " > 0 ORDER BY " + COL_SPENT + " ASC LIMIT 1",
                new String[]{pattern}
        );
        String result = "N/A";
        if (cursor.moveToFirst()) {
            String d = cursor.getString(0);
            double s = cursor.getDouble(1);
            int day = Integer.parseInt(d.split("-")[2]);
            result = "Day " + day + " — Rs. " + String.format("%.2f", s);
        }
        cursor.close();
        return result;
    }

    /**
     * Returns total number of unique days with recorded data.
     */
    public int getTotalDaysRecorded() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT " + COL_DATE + ") FROM " + TABLE_NAME, null
        );
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /**
     * Returns all-time total spent across all records.
     */
    public double getOverallTotalSpent() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_SPENT + ") FROM " + TABLE_NAME, null
        );
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    /**
     * Returns total credits for a given month.
     */
    public double getTotalCreditsForMonth(LocalDate date) {
        String pattern = String.format("%04d-%02d", date.getYear(), date.getMonthValue()) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_CREDIT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{pattern}
        );
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    /**
     * Returns an array of spending sums mapped to days 1-31 of a month.
     * Index 0 = Day 1, Index 30 = Day 31.
     */
    public float[] getMonthlyDailySpends(LocalDate date) {
        float[] dailySpends = new float[31];
        String pattern = String.format("%04d-%02d", date.getYear(), date.getMonthValue()) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_DATE + ", " + COL_SPENT + " FROM " + TABLE_NAME +
                        " WHERE " + COL_DATE + " LIKE ?",
                new String[]{pattern}
        );
        while (cursor.moveToNext()) {
            String dateStr = cursor.getString(0); // e.g. "2026-03-15"
            double spent = cursor.getDouble(1);
            int day = Integer.parseInt(dateStr.split("-")[2]); // extract day
            if (day >= 1 && day <= 31) {
                dailySpends[day - 1] = (float) spent;
            }
        }
        cursor.close();
        return dailySpends;
    }

    /**
     * Returns an array of spending sums mapped to months 1-12 of a year.
     * Index 0 = Jan, Index 11 = Dec.
     */
    public float[] getYearlyMonthlySpends(int year) {
        float[] monthlySpends = new float[12];
        String pattern = String.format("%04d", year) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT CAST(SUBSTR(" + COL_DATE + ", 6, 2) AS INTEGER) as month, SUM(" + COL_SPENT + ") FROM " + TABLE_NAME +
                        " WHERE " + COL_DATE + " LIKE ? GROUP BY month",
                new String[]{pattern}
        );
        while (cursor.moveToNext()) {
            int month = cursor.getInt(0); // 1 to 12
            double sum = cursor.getDouble(1);
            if (month >= 1 && month <= 12) {
                monthlySpends[month - 1] = (float) sum;
            }
        }
        cursor.close();
        return monthlySpends;
    }
    /**
     * Returns investment entries for a given month, only for days where investments > 0.
     * Key = day number, Value = investment amount.
     */
    public HashMap<String, Double> getInvestmentEntries() {
        HashMap<String, Double> investmentMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT "+ COL_DATE +" ,"+ COL_INVESTMENTS +
                        " FROM " + TABLE_NAME +
                        " WHERE " + COL_INVESTMENTS + " > 0 ORDER BY " + COL_DATE, null
        );
        while (cursor.moveToNext()) {
            String dateStr = cursor.getString(0); // e.g. "2026-03-15"
            double investment = cursor.getDouble(1);
            investmentMap.put(dateStr, investment);
        }
        cursor.close();
        return investmentMap;
    }

    /**
     * Returns investment entries for a given month, only for days where investments > 0.
     * Key = day number, Value = investment amount.
     */
    public HashMap<Integer, Float> getMonthlyInvestmentEntries(LocalDate date) {
        HashMap<Integer, Float> investmentMap = new HashMap<>();
        String pattern = String.format("%04d-%02d", date.getYear(), date.getMonthValue()) + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT CAST(SUBSTR(" + COL_DATE + ", 9, 2) AS INTEGER) as day, " + COL_INVESTMENTS +
                        " FROM " + TABLE_NAME +
                        " WHERE " + COL_DATE + " LIKE ? AND " + COL_INVESTMENTS + " > 0 ORDER BY " + COL_DATE,
                new String[]{pattern}
        );
        while (cursor.moveToNext()) {
            int day = cursor.getInt(0);
            float amount = cursor.getFloat(1);
            investmentMap.put(day, amount);
        }
        cursor.close();
        return investmentMap;
    }


    // ========== ASSET CATEGORY CRUD ==========

    public long insertCategory(String name, String color) {
        return insertCategory(name, color, 0);
    }

    public long insertCategory(String name, String color, double targetAllocation) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COL_CAT_SORT_ORDER + ") FROM " + TABLE_ASSET_CATEGORIES, null);
        int maxOrder = 0;
        if (cursor.moveToFirst()) maxOrder = cursor.getInt(0) + 1;
        cursor.close();

        ContentValues cv = new ContentValues();
        cv.put(COL_CAT_NAME, name);
        cv.put(COL_CAT_COLOR, color);
        cv.put(COL_CAT_SORT_ORDER, maxOrder);
        cv.put(COL_CAT_TARGET_ALLOC, targetAllocation);
        return db.insert(TABLE_ASSET_CATEGORIES, null, cv);
    }

    public void updateCategory(int id, String name, String color) {
        updateCategory(id, name, color, -1);
    }

    public void updateCategory(int id, String name, String color, double targetAllocation) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CAT_NAME, name);
        cv.put(COL_CAT_COLOR, color);
        if (targetAllocation >= 0) cv.put(COL_CAT_TARGET_ALLOC, targetAllocation);
        db.update(TABLE_ASSET_CATEGORIES, cv, COL_CAT_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteCategory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ASSET_ITEMS, COL_ITEM_CATEGORY_ID + " = ?", new String[]{String.valueOf(id)});
        db.delete(TABLE_ASSET_CATEGORIES, COL_CAT_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<AssetCategory> getAllCategories() {
        List<AssetCategory> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_ASSET_CATEGORIES + " ORDER BY " + COL_CAT_SORT_ORDER,
                null);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME));
            String color = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_COLOR));
            int sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_SORT_ORDER));
            double targetAlloc = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CAT_TARGET_ALLOC));
            AssetCategory cat = new AssetCategory(id, name, color, sortOrder, targetAlloc);
            cat.setItems(getItemsForCategory(id));
            cat.computeTotalValue();
            categories.add(cat);
        }
        cursor.close();

        // Compute percentages
        double totalNetWorth = 0;
        for (AssetCategory c : categories) totalNetWorth += c.getTotalValue();
        if (totalNetWorth > 0) {
            for (AssetCategory c : categories) {
                c.setPercentage((c.getTotalValue() / totalNetWorth) * 100.0);
                c.computeItemPercentages();
            }
        }
        return categories;
    }

    // ========== ASSET ITEM CRUD ==========

    public long insertAssetItem(int categoryId, String name, double value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ITEM_CATEGORY_ID, categoryId);
        cv.put(COL_ITEM_NAME, name);
        cv.put(COL_ITEM_VALUE, value);
        return db.insert(TABLE_ASSET_ITEMS, null, cv);
    }

    public void updateAssetItem(int id, String name, double value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ITEM_NAME, name);
        cv.put(COL_ITEM_VALUE, value);
        db.update(TABLE_ASSET_ITEMS, cv, COL_ITEM_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteAssetItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ASSET_ITEMS, COL_ITEM_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<AssetItem> getItemsForCategory(int categoryId) {
        List<AssetItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_ASSET_ITEMS + " WHERE " + COL_ITEM_CATEGORY_ID + " = ?",
                new String[]{String.valueOf(categoryId)});
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_NAME));
            double value = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_VALUE));
            double qty = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_QTY));
            double avgCost = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_AVG_COST));
            double ltp = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_LTP));
            double invested = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_INVESTED));
            double pnl = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_PNL));
            double netChg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_NET_CHG));
            double dayChg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_DAY_CHG));
            items.add(new AssetItem(id, categoryId, name, value, qty, avgCost, ltp, invested, pnl, netChg, dayChg));
        }
        cursor.close();
        return items;
    }

    public double getTotalAssetValue() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_ITEM_VALUE + ") FROM " + TABLE_ASSET_ITEMS, null);
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    /**
     * Find an asset item by name (case-insensitive). Returns null if not found.
     */
    public AssetItem findAssetItemByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_ASSET_ITEMS + " WHERE " + COL_ITEM_NAME + " = ? COLLATE NOCASE",
                new String[]{name});
        AssetItem item = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_ID));
            int catId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_CATEGORY_ID));
            String n = cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_NAME));
            double value = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_VALUE));
            double qty = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_QTY));
            double avgCost = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_AVG_COST));
            double ltp = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_LTP));
            double invested = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_INVESTED));
            double pnl = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_PNL));
            double netChg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_NET_CHG));
            double dayChg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ITEM_DAY_CHG));
            item = new AssetItem(id, catId, n, value, qty, avgCost, ltp, invested, pnl, netChg, dayChg);
        }
        cursor.close();
        return item;
    }

    /**
     * Insert a full asset item with all holding details.
     */
    public long insertFullAssetItem(int categoryId, String name, double qty, double avgCost,
                                     double ltp, double invested, double curVal,
                                     double pnl, double netChg, double dayChg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ITEM_CATEGORY_ID, categoryId);
        cv.put(COL_ITEM_NAME, name);
        cv.put(COL_ITEM_VALUE, curVal);
        cv.put(COL_ITEM_QTY, qty);
        cv.put(COL_ITEM_AVG_COST, avgCost);
        cv.put(COL_ITEM_LTP, ltp);
        cv.put(COL_ITEM_INVESTED, invested);
        cv.put(COL_ITEM_PNL, pnl);
        cv.put(COL_ITEM_NET_CHG, netChg);
        cv.put(COL_ITEM_DAY_CHG, dayChg);
        return db.insert(TABLE_ASSET_ITEMS, null, cv);
    }

    /**
     * Update a full asset item with all holding details.
     */
    public void updateFullAssetItem(int id, String name, double qty, double avgCost,
                                     double ltp, double invested, double curVal,
                                     double pnl, double netChg, double dayChg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ITEM_NAME, name);
        cv.put(COL_ITEM_VALUE, curVal);
        cv.put(COL_ITEM_QTY, qty);
        cv.put(COL_ITEM_AVG_COST, avgCost);
        cv.put(COL_ITEM_LTP, ltp);
        cv.put(COL_ITEM_INVESTED, invested);
        cv.put(COL_ITEM_PNL, pnl);
        cv.put(COL_ITEM_NET_CHG, netChg);
        cv.put(COL_ITEM_DAY_CHG, dayChg);
        db.update(TABLE_ASSET_ITEMS, cv, COL_ITEM_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
