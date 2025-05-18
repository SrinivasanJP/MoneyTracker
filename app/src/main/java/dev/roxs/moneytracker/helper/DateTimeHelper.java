package dev.roxs.moneytracker.helper;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DateTimeHelper {

    // Get current date like "13-May-2025"
    public static String getCurrentDate() {
        return formatDate(new Date());
    }
    public static LocalDate getCurrentDateLocaldate() {
        return LocalDate.now();
    }
    public static String getDayName(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
    public static String getDayOfWeek(String formattedDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
            Date date = format.parse(formattedDate);

            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
            return dayFormat.format(date);  // e.g., "Saturday"
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
    // Get current month like "May"
    public static String getCurrentMonth() {
        return new SimpleDateFormat("MMMM", Locale.ENGLISH).format(new Date());
    }


    public static int getCurrentMonthLocalDate(LocalDate date) {
        return date.getMonthValue();
    }
    public static int getCurrentYearLocalDate(LocalDate date) {
        return date.getYear();
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

    public static String formatToDisplayDate(LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH));
    }
    public static String monthYearFromDate(LocalDate date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        return  date.format(formatter);
    }
    public static ArrayList<String> daysInMonthArray(LocalDate date){
        ArrayList<String> daysInMonthArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);
        int daysInMonth = yearMonth.lengthOfMonth();

        LocalDate firstOfMonth = date.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1=Monday ... 7=Sunday

        int firstDayIndex = dayOfWeek % 7; // Convert to 0-based index (0=Sunday, 6=Saturday)

        for (int i = 0; i < firstDayIndex; i++) {
            daysInMonthArray.add("");
        }

        for (int day = 1; day <= daysInMonth; day++) {
            daysInMonthArray.add(String.valueOf(day));
        }

        // Pad the end of the list to make a complete 6-week grid (42 cells)
        while (daysInMonthArray.size() < 42) {
            daysInMonthArray.add("");
        }

        return daysInMonthArray;
    }

}
