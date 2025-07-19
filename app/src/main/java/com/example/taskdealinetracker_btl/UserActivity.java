package com.example.taskdealinetracker_btl;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import com.example.taskdealinetracker_btl.modules.Entity_User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class UserActivity extends AppCompatActivity {
    private Entity_User user = new Entity_User();
    private Button btnSubmit;
    private TextInputEditText textBoxUsername;
    private TextInputLayout tilUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inforuser);

        tilUsername     = findViewById(R.id.tilUsername);
        textBoxUsername = findViewById(R.id.textBoxUsername);
        btnSubmit       = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            String username = textBoxUsername.getText().toString().trim();

            // 1. Validate: bắt buộc phải nhập username
            if (username.isEmpty()) {
                // Hiện lỗi ngay dưới ô nhập
                tilUsername.setError("Vui lòng nhập username");
                textBoxUsername.requestFocus();
                return; // dừng, không gửi Intent
            } else {
                tilUsername.setError(null); // clear error
            }

            // 2. Nếu hợp lệ, gán và trả về MainActivity
            user.setUsername(username);
            Intent result = new Intent();
            result.putExtra("user", (Parcelable) user);
            setResult(RESULT_OK, result);
            finish();
        });
    }
}
