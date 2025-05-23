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
import java.util.HashMap;
import java.util.Locale;

public class SQl_Helper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "money_tracker.db";
    private static final int DATABASE_VERSION = 2;

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
                COL_DATE + " TEXT UNIQUE, " +  // <-- Make date UNIQUE
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
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null); // Deletes all rows
        db.close();
    }


    public DB_STRUCT getYesterdaysHoldings(String formattedDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
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
    public ArrayList<String> getAllRecordedDates(int month, int year) {
        ArrayList<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Format the month to 2-digits (e.g., 05)
        String monthName = new java.text.DateFormatSymbols().getShortMonths()[month - 1];
        String formattedLike = "%-" + monthName + "-" + year;

        Cursor cursor = db.rawQuery(
                "SELECT " + COL_DATE + " FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{formattedLike}
        );

        if (cursor.moveToFirst()) {
            do {
                dates.add(cursor.getString(0).split("-")[0]);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return dates;
    }


    public double getSumSpentCurrentMonth() {
        String currentMonth = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?",
                new String[]{"%-" + currentMonth}
        );

        double totalSpent = 0;
        if (cursor.moveToFirst()) {
            totalSpent = cursor.getDouble(0);
        }

        cursor.close();
        return totalSpent;
    }

    public double getHoldingsFromEarliestDateThisMonth() {
        String currentMonth = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH).format(Calendar.getInstance().getTime());
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_HOLDINGS + " FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ? ORDER BY " + COL_DATE + " ASC LIMIT 1",
                new String[]{"%-" + currentMonth}
        );

        double holdings = 0;
        if (cursor.moveToFirst()) {
            holdings = cursor.getDouble(0);
        }

        cursor.close();
        return holdings;
    }

    public double getTodaysSpent() {
        String today = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(Calendar.getInstance().getTime());
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
        String today = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(Calendar.getInstance().getTime());
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
        String currentMonth = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH).format(calendar.getTime());

        calendar.add(Calendar.MONTH, -1);
        String lastMonth = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH).format(calendar.getTime());

        SQLiteDatabase db = this.getReadableDatabase();

        // Get current month spent
        Cursor cursor1 = db.rawQuery("SELECT SUM(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?", new String[]{"%-" + currentMonth});
        double currentSpent = 0;
        if (cursor1.moveToFirst()) {
            currentSpent = cursor1.getDouble(0);
        }
        cursor1.close();

        // Get last month spent
        Cursor cursor2 = db.rawQuery("SELECT SUM(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?", new String[]{"%-" + lastMonth});
        double lastSpent = 0;
        if (cursor2.moveToFirst()) {
            lastSpent = cursor2.getDouble(0);
        }
        cursor2.close();

        if (lastSpent == 0) {
            return currentSpent == 0 ? 0 : 100; // Avoid divide-by-zero
        }

        return ((currentSpent - lastSpent) / lastSpent) * 100;
    }



    public double getAverageSpentForMonth(LocalDate date) {
        String monthName = date.getMonth().name().substring(0, 1) + date.getMonth().name().substring(1, 3).toLowerCase(); // "Jan", "Feb", ...
        String pattern = "%-" + monthName + "-" + date.getYear();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT AVG(" + COL_SPENT + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?", new String[]{pattern});

        double avgSpent = 0;
        if (cursor.moveToFirst()) {
            avgSpent = cursor.getDouble(0);
        }
        cursor.close();
        return avgSpent;
    }


    public double getTotalInvestmentsForMonth(LocalDate date) {
        String monthName = date.getMonth().name().substring(0, 1) + date.getMonth().name().substring(1, 3).toLowerCase();
        String pattern = "%-" + monthName + "-" + date.getYear();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COL_INVESTMENTS + ") FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ?", new String[]{pattern});

        double totalInvestments = 0;
        if (cursor.moveToFirst()) {
            totalInvestments = cursor.getDouble(0);
        }
        cursor.close();
        return totalInvestments;
    }

    public double getLastLoanForMonth(LocalDate date) {
        String monthName = date.getMonth().name().substring(0, 1) + date.getMonth().name().substring(1, 3).toLowerCase();
        String pattern = "%-" + monthName + "-" + date.getYear();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_LOAN + " FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ? ORDER BY " + COL_DATE + " DESC LIMIT 1",
                new String[]{pattern}
        );

        double loan = -1;
        if (cursor.moveToFirst()) {
            loan = cursor.getDouble(0);
        }
        cursor.close();
        return loan;
    }








}
