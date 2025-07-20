package com.example.taskdealinetracker_btl.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;
import com.example.taskdealinetracker_btl.utils.ReminderScheduler;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Log.d(TAG, "Device boot completed, rescheduling reminders...");

        // Chạy trong background thread để tránh ANR
        new Thread(() -> {
            try {
                restoreReminders(context);
            } catch (Exception e) {
                Log.e(TAG, "Error restoring reminders after boot", e);
            }
        }).start();
    }

    private void restoreReminders(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);

        try {
            // Lấy user ID từ SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("task_tracker", Context.MODE_PRIVATE);
            int currentUserId = prefs.getInt("current_user_id", -1);

            if (currentUserId == -1) {
                // Nếu không có user hiện tại, thử lấy tất cả users và restore cho từng user
                List<Entity_User> users = db.getAllUsers();
                for (Entity_User user : users) {
                    restoreUserReminders(context, db, user.getId());
                }
            } else {
                // Restore cho user hiện tại
                restoreUserReminders(context, db, currentUserId);
            }

        } finally {
            db.close();
        }
    }

    private void restoreUserReminders(Context context, DatabaseHelper db, int userId) {
        List<Entity_Task> tasks = db.getTasksByUserId(userId);
        int restoredCount = 0;

        for (Entity_Task task : tasks) {
            if (!task.isComplete() && task.getDeadline() != null) {
                ReminderScheduler.scheduleTaskReminders(context, task);
                restoredCount++;

                // Thêm delay nhỏ giữa các scheduling để tránh quá tải system
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        Log.d(TAG, String.format("Restored %d reminders for user %d", restoredCount, userId));
    }
}