package dev.roxs.moneytracker.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
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


    public double getYesterdaysHoldings() {
        // Get yesterday's date in the same format as stored in the DB (assumed "dd-MMM-yyyy")
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = sdf.format(calendar.getTime());

        // Query the database for yesterday's holdings
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_HOLDINGS + " FROM " + TABLE_NAME + " WHERE " + COL_DATE + " = ?", new String[]{yesterday});

        double holdings = -1;
        if (cursor.moveToFirst()) {
            holdings = cursor.getDouble(0);
        }

        cursor.close();
        return holdings;
    }
    public void insertOrUpdateEntry(String date, String day, double softcash, double hardcash, double investments, double credit, double loan, String remarks) {
        SQLiteDatabase db = this.getWritableDatabase();

        double holdings = softcash + hardcash;
        double prevHoldings = getPreviousHoldings(date);
        double spent = prevHoldings - (holdings - investments - credit);

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


}
