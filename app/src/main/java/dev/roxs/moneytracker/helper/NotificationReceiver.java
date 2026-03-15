package dev.roxs.moneytracker.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.page.DailyInput_Activity;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String NOTIFICATION_CHANNEL_ID = "daily_spent_reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        sendReminderNotification(context);

        // Reschedule for the next day
        Notification_Helper.scheduleDailyWork(context);
    }

    private void sendReminderNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, "Spent Reminder", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminds you to add today's spending");
            notificationManager.createNotificationChannel(channel);
        }

        Intent activityIntent = new Intent(context, DailyInput_Activity.class);
        activityIntent.putExtra("date", DateTimeHelper.getCurrentDate());
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, (int) System.currentTimeMillis(), activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.mm_icon_round)
                .setContentTitle("Money Tracker Reminder")
                .setContentText("You haven't added today's spent yet!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(1001, builder.build());
    }
}
