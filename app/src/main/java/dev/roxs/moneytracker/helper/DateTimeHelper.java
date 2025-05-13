package dev.roxs.moneytracker.helper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeHelper {

    // Get current date like "13-May-2025"
    public static String getCurrentDate() {
        return formatDate(new Date());
    }
    public static String getCurrentDayName() {
        return new SimpleDateFormat("EEEE", Locale.ENGLISH).format(new Date());
    }

    // Get current time like "10:42 AM"
    public static String getCurrentTime() {
        return formatTime(new Date());
    }

    // Get current date and time like "13-May-2025 10:42 AM"
    public static String getCurrentDateTime() {
        return formatDateTime(new Date());
    }

    // Format Date to "dd-MMM-yyyy"
    public static String formatDate(Date date) {
        return new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(date);
    }

    // Format Date to "hh:mm a"
    public static String formatTime(Date date) {
        return new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(date);
    }

    // Format Date to "dd-MMM-yyyy hh:mm a"
    public static String formatDateTime(Date date) {
        return new SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.ENGLISH).format(date);
    }

    // Format from millis
    public static String formatDate(long millis) {
        return formatDate(new Date(millis));
    }

    public static String formatTime(long millis) {
        return formatTime(new Date(millis));
    }

    public static String formatDateTime(long millis) {
        return formatDateTime(new Date(millis));
    }
}
