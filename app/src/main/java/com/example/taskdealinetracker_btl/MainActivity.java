package com.example.taskdealinetracker_btl;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_User;
import com.example.taskdealinetracker_btl.reminder.ReminderReceiver;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Entity_User user;
    private ActivityResultLauncher<Intent> userLauncher;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Khởi DB helper
        dbHelper = new DatabaseHelper(this);

        // 2) Load user
        List<Entity_User> users = dbHelper.getAllUsers();
        user = users.isEmpty() ? null : users.get(0);

        // 3) Đăng ký launcher
        userLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Entity_User newUser = result.getData().getParcelableExtra("user");
                        if (newUser != null) {
                            long id = dbHelper.insertUser(newUser);
                            user = dbHelper.getUserById((int)id);
                            goToTasks();
                        }
                    } else {
                        finish();
                    }
                }
        );

        // 4) Chuyển hoặc tạo user
        if (user == null) {
            userLauncher.launch(new Intent(this, UserActivity.class));
        } else {
            goToTasks();
        }
    }

    private void goToTasks() {
        // Trước khi đi, schedule reminders cho tất cả task chưa hoàn thành
        new Thread(() -> {
            dbHelper.getTasksByUserId(user.getId()).forEach(t -> {
                if (!t.isComplete())
                    ReminderReceiver.scheduleNext(this, t.getId());
            });
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