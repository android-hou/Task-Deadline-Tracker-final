package com.example.taskdealinetracker_btl;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.taskdealinetracker_btl.modules.Entity_User;
import com.example.taskdealinetracker_btl.utils.FileHelper;

public class MainActivity extends AppCompatActivity {
    private Entity_User user;
    private ActivityResultLauncher<Intent> userLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Không đặt setContentView vì đây chỉ là activity khởi động

        // 1) Load user từ file (nội bộ)
        user = FileHelper.loadUser(this);

        // 2) Đăng ký launcher để nhận user mới
        userLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Entity_User newUser = result.getData().getParcelableExtra("user");
                        if (newUser != null) {
                            user = newUser;
                            // Lưu user vào file
                            FileHelper.saveUser(this, user);
                            // Chuyển sang TaskActivity
                            goToTasks();
                        }
                    } else {
                        // Nếu userActivity bị hủy, thoát app
                        finish();
                    }
                }
        );

        // 3) Kiểm tra user đã tồn tại hay chưa
        if (user == null) {
            // Chưa có user → mở UserActivity để đăng nhập/đăng ký
            userLauncher.launch(new Intent(this, UserActivity.class));
        } else {
            // Đã có user → chuyển thẳng sang TaskActivity
            goToTasks();
        }
    }

    /**
     * Chuyển sang TaskActivity kèm Entity_User
     */
    private void goToTasks() {
        Toast.makeText(this, "Chào " + user.getUsername(), Toast.LENGTH_SHORT).show();
        Intent taskIntent = new Intent(this, TaskActivity.class);
        taskIntent.putExtra("user", (Parcelable) user);
        startActivity(taskIntent);
        finish();
    }
}
