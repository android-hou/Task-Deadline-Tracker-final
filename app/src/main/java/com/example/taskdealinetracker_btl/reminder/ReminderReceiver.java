package com.example.taskdealinetracker_btl.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.taskdealinetracker_btl.MainActivity;
import com.example.taskdealinetracker_btl.R;
import com.example.taskdealinetracker_btl.TaskActivity;
import com.example.taskdealinetracker_btl.modules.Entity_User;
import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.utils.ReminderScheduler;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_REMINDER_TYPE = "extra_reminder_type";

    private static final String CHANNEL_ID = "task_reminder_1hour";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        int reminderType = intent.getIntExtra(EXTRA_REMINDER_TYPE, ReminderScheduler.REMINDER_TYPE_1_HOUR);

        if (taskId < 0) {
            Log.w(TAG, "Invalid task ID: " + taskId);
            return;
        }

        DatabaseHelper db = new DatabaseHelper(context);
        Entity_Task task;
        try {
            task = db.getTaskById(taskId);
        } finally {
            db.close();
        }

        if (task == null) {
            Log.w(TAG, "Task not found: " + taskId);
            return;
        }

        if (task.isComplete()) {
            Log.d(TAG, "Task already completed: " + taskId);
            return;
        }

        // Tạo notification channel nếu cần
        createNotificationChannel(context);

        // Chỉ xử lý nhắc nhở 1 giờ trước deadline
        if (reminderType == ReminderScheduler.REMINDER_TYPE_1_HOUR) {
            showReminderNotification(context, task);
            Log.d(TAG, String.format("1-hour reminder notification shown for task %d", taskId));
        } else {
            Log.w(TAG, "Unknown reminder type: " + reminderType);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) return;

            // Channel cho nhắc nhở 1 giờ trước deadline
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở Task (1 giờ trước)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo nhắc nhở task trước deadline 1 giờ");
            channel.enableVibration(true);

            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showReminderNotification(Context context, Entity_Task task) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        String deadlineText = task.getDeadline() != null ?
                DateFormat.format("dd/MM/yyyy HH:mm", task.getDeadline()).toString() : "Không có hạn";

        String priorityText = getPriorityDisplayText(task.getPriority());

        // Tạo intent để mở TaskActivity khi click vào notification
        Intent taskIntent = getTaskActivityIntent(context);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                task.getId(),
                taskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | getPendingIntentFlags()
        );

        String title = "⏰ " + task.getTitle();
        String message = "Task sắp hết hạn trong 1 giờ!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\nHạn: " + deadlineText + "\nMức độ: " + priorityText))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setColor(0xFF2196F3); // Màu xanh dương cho reminder

        notificationManager.notify(task.getId(), builder.build());
    }

    private Intent getTaskActivityIntent(Context context) {
        // Lấy user ID từ SharedPreferences
        int userId = context.getSharedPreferences("task_tracker", Context.MODE_PRIVATE)
                .getInt("current_user_id", -1);

        if (userId == -1) {
            // Fallback về MainActivity nếu không có user ID
            return new Intent(context, MainActivity.class);
        }

        // Tạo intent với user data
        DatabaseHelper db = new DatabaseHelper(context);
        try {
            Entity_User user = db.getUserById(userId);
            Intent intent = new Intent(context, TaskActivity.class);
            if (user != null) {
                intent.putExtra("user", (Parcelable) user);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            return intent;
        } finally {
            db.close();
        }
    }

    private String getPriorityDisplayText(String priority) {
        if (priority == null) return "Bình thường";

        switch (priority.toUpperCase()) {
            case "HIGH": return "Cao";
            case "MEDIUM": return "Trung bình";
            case "LOW": return "Thấp";
            default: return priority;
        }
    }

    private int getPendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        }
        return 0;
    }
}