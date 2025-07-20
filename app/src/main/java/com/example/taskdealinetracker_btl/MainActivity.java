package com.example.taskdealinetracker_btl;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;
import com.example.taskdealinetracker_btl.reminder.ReminderReceiver;
import com.example.taskdealinetracker_btl.utils.ReminderScheduler;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Entity_User user;
    private ActivityResultLauncher<Intent> userLauncher;
    private DatabaseHelper dbHelper;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Khởi DB helper
        dbHelper = new DatabaseHelper(this);

        // 2) Kiểm tra và yêu cầu quyền thông báo
        checkAndRequestNotificationPermission();

        // 3) Load user
        List<Entity_User> users = dbHelper.getAllUsers();
        user = users.isEmpty() ? null : users.get(0);

        // 4) Đăng ký launcher
        userLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Entity_User newUser = result.getData().getParcelableExtra("user");
                        if (newUser != null) {
                            long id = dbHelper.insertUser(newUser);
                            newUser.setId((int)id); // Giả sử có setter cho ID
                            user = newUser;
                            goToTasks();
                        }
                    } else {
                        finish();
                    }
                }
        );

        // 5) Chuyển hoặc tạo user
        if (user == null) {
            userLauncher.launch(new Intent(this, UserActivity.class));
        } else {
            goToTasks();
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền thông báo đã được cấp", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền thông báo để nhắc nhở task", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void goToTasks() {
        if (user == null) return;

        // Lưu user ID vào SharedPreferences để sử dụng trong BootReceiver
        getSharedPreferences("task_tracker", MODE_PRIVATE)
                .edit()
                .putInt("current_user_id", user.getId())
                .apply();

        // Schedule reminders cho tất cả task chưa hoàn thành trong background thread
        new Thread(() -> {
            try {
                List<Entity_Task> incompleteTasks = dbHelper.getTasksByUserId(user.getId());
                int scheduledCount = 0;

                for (Entity_Task task : incompleteTasks) {
                    if (!task.isComplete() && task.getDeadline() != null) {
                        ReminderScheduler.scheduleTaskReminders(this, task);
                        scheduledCount++;
                    }
                }

                final int finalCount = scheduledCount;
                runOnUiThread(() -> {
                    if (finalCount > 0) {
                        Toast.makeText(this,
                                "Đã thiết lập nhắc nhở cho " + finalCount + " task",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Lỗi khi thiết lập nhắc nhở", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();

        Intent intent = new Intent(this, TaskActivity.class);
        intent.putExtra("user", (Parcelable) user);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}