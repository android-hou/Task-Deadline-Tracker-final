package com.example.taskdealinetracker_btl.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.taskdealinetracker_btl.R;
import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_TASK_ID = "extra_task_id";
    private static final String CHANNEL_ID = "task_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        if (taskId < 0) return;

        DatabaseHelper db = new DatabaseHelper(context);
        Entity_Task task = db.getTaskById(taskId);
        db.close();
        if (task == null || task.isComplete()) return;

        long now = System.currentTimeMillis();
        if (task.getDeadline() != null && now <= task.getDeadline().getTime()) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(new NotificationChannel(
                        CHANNEL_ID, "Nhắc Task", NotificationManager.IMPORTANCE_HIGH));
            }
            NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Nhắc: " + task.getTitle())
                    .setContentText("Hạn: " + android.text.format.DateFormat.format("dd/MM HH:mm", task.getDeadline()))
                    .setAutoCancel(true);
            nm.notify(taskId, b.build());
        }
        scheduleNext(context, taskId);
    }

    public static void scheduleNext(Context ctx, int taskId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.putExtra(EXTRA_TASK_ID, taskId);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, taskId, i, PendingIntent.FLAG_UPDATE_CURRENT);
        long next = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
    }

    public static void cancel(Context ctx, int taskId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, taskId, i, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) am.cancel(pi);
    }
}