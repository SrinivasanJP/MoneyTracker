package dev.roxs.moneytracker.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SQl_Helper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "money_tracker.db";
    private static final int DATABASE_VERSION = 1;

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

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DATE + " TEXT, " +
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
        Cursor cursor = db.rawQuery("SELECT " + COL_SOFTCASH + " + " + COL_HARDCASH + " FROM " + TABLE_NAME + " ORDER BY date DESC LIMIT 1", null);

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
}
