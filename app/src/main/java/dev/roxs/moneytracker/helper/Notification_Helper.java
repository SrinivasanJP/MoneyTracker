// ReminderWorker.java
package dev.roxs.moneytracker.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.SQl_Helper;

public class Notification_Helper extends Worker {


    public Notification_Helper(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SQl_Helper dbHelper = new SQl_Helper(getApplicationContext());

//        if (!dbHelper.isTodaysRecordAvailable()) {
//            sendReminderNotification();
//        }
            sendReminderNotification();


        return Result.success();
    }

    private void sendReminderNotification() {
        String channelId = "daily_spent_reminder";
        NotificationManager notificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Spent Reminder", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminds you to add today’s spending");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.mipmap.mm_icon_round)
                .setContentTitle("Money Tracker Reminder")
                .setContentText("You haven’t added today’s spent yet!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }

    public static void scheduleDailyWork(Context context) {
        Calendar now = Calendar.getInstance();
        Calendar dueTime = Calendar.getInstance();
        dueTime.set(Calendar.HOUR_OF_DAY, 20);
        dueTime.set(Calendar.MINUTE, 0);
        dueTime.set(Calendar.SECOND, 0);

        if (dueTime.before(now)) {
            dueTime.add(Calendar.DAY_OF_MONTH, 1); // Schedule for next day
        }

        long initialDelay = dueTime.getTimeInMillis() - now.getTimeInMillis();

        WorkRequest workRequest = new PeriodicWorkRequest.Builder(
                dev.roxs.moneytracker.helper.Notification_Helper.class,
                24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("daily_reminder")
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DailySpentReminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                (PeriodicWorkRequest) workRequest
        );
    }
}
