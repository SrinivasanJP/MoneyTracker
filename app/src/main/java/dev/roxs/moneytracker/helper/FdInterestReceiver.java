package dev.roxs.moneytracker.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.model.AssetItem;

/**
 * BroadcastReceiver that checks all FD items daily.
 * If an FD's interest_credit_date is today:
 *   1. Calculates interest amount
 *   2. Credits it to the FD's value and pnl
 *   3. Updates the next credit date
 *   4. Sends a notification
 */
public class FdInterestReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "fd_interest_channel";
    private static final String CHANNEL_NAME = "FD Interest Notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        SQl_Helper sql = new SQl_Helper(context);
        List<AssetItem> fdItems = sql.getAllFdItems();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        int notificationId = 5000;

        for (AssetItem item : fdItems) {
            String creditDate = item.getInterestCreditDate();
            if (creditDate != null && creditDate.equals(today)) {
                // Calculate interest
                double interestAmount = item.calculateInterestAmount();

                // Compute next credit date
                String nextDate = computeNextCreditDate(creditDate, item.getInterestCycle());

                // Credit interest in DB
                sql.creditFdInterest(item.getId(), interestAmount, nextDate);

                // Send notification
                String message = String.format(Locale.getDefault(),
                        "FD Interest Credit: ₹ %.2f for %s (%s @ %.2f%%)",
                        interestAmount, item.getName(),
                        item.getInterestType(), item.getInterestRate());

                sendNotification(context, notificationId++, "FD Interest Credited", message);
            }
        }

        // Reschedule for tomorrow
        Notification_Helper.scheduleFdInterestCheck(context);
    }

    private String computeNextCreditDate(String currentDate, int cycleMonths) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(currentDate));
            cal.add(Calendar.MONTH, cycleMonths);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return currentDate;
        }
    }

    private void sendNotification(Context context, int id, String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for FD interest credits");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.mm_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(id, builder.build());
    }
}
