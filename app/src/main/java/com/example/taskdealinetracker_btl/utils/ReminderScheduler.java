package com.example.taskdealinetracker_btl.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.reminder.ReminderReceiver;

import java.util.Date;

public class ReminderScheduler {
    private static final String TAG = "ReminderScheduler";

    // Chỉ nhắc 1 giờ trước deadline
    public static final int REMINDER_TYPE_1_HOUR = 2;

    // Thời gian nhắc nhở (milliseconds)
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;

    /**
     * Lên lịch nhắc nhở cho một task - chỉ nhắc 1 lần trước deadline 1 giờ
     */
    public static void scheduleTaskReminders(Context context, Entity_Task task) {
        if (task == null || task.getDeadline() == null || task.isComplete()) {
            return;
        }

        long now = System.currentTimeMillis();
        long deadline = task.getDeadline().getTime();

        // Chỉ schedule nhắc trước 1 giờ nếu task chưa quá hạn và còn thời gian
        long oneHourBefore = deadline - ONE_HOUR_MS;
        if (oneHourBefore > now) {
            scheduleReminder(context, task, REMINDER_TYPE_1_HOUR, oneHourBefore);
            Log.d(TAG, "Scheduled 1-hour reminder for task: " + task.getTitle() + " at " + new Date(oneHourBefore));
        } else {
            Log.d(TAG, "Skipped 1-hour reminder (past) for task: " + task.getTitle());
        }
    }

    /**
     * Lên lịch một nhắc nhở cụ thể
     */
    private static void scheduleReminder(Context context, Entity_Task task, int reminderType, long triggerTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TASK_ID, task.getId());
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TYPE, reminderType);

        // Tạo unique request code cho nhắc nhở
        int requestCode = task.getId() * 10 + reminderType;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | getPendingIntentFlags()
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }

            Log.d(TAG, String.format("Reminder scheduled: Task %d, Type %d, Time %s",
                    task.getId(), reminderType, new Date(triggerTime)));

        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule reminder", e);
        }
    }

    /**
     * Hủy nhắc nhở cho một task
     */
    public static void cancelAllReminders(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        int requestCode = taskId * 10 + REMINDER_TYPE_1_HOUR;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | getPendingIntentFlags()
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, String.format("Cancelled 1-hour reminder for task %d", taskId));
        }
    }

    /**
     * Cập nhật lịch nhắc nhở khi task thay đổi
     */
    public static void updateTaskReminders(Context context, Entity_Task task) {
        // Hủy nhắc nhở cũ
        cancelAllReminders(context, task.getId());

        // Tạo lịch mới nếu task chưa hoàn thành
        if (!task.isComplete()) {
            scheduleTaskReminders(context, task);
        }
    }

    /**
     * Lấy flags phù hợp cho PendingIntent theo API level
     */
    private static int getPendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        }
        return 0;
    }

    /**
     * Kiểm tra xem có thể lên lịch alarm được không
     */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }
}