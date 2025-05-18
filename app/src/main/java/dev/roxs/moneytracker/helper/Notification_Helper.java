package dev.roxs.moneytracker.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import dev.roxs.moneytracker.R;

public class Notification_Helper extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            SQl_Helper dbHelper = new SQl_Helper(context);
            boolean isTodayRecorded = dbHelper.isTodaysRecordAvailable();

            Log.d("UT", "onReceive: Notify is called");
            if (!isTodayRecorded) {
                sendReminderNotification(context);
            }
        }

        private void sendReminderNotification(Context context) {
            String channelId = "daily_spent_reminder";
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId, "Spent Reminder", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Reminds you to add today's spending");
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.mm_icon_round)
                    .setContentTitle("Money Tracker Reminder")
                    .setContentText("You haven’t added today’s spent yet!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            notificationManager.notify(1001, builder.build());
        }

    }
