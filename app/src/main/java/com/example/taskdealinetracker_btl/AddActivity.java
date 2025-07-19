package com.example.taskdealinetracker_btl;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddActivity extends AppCompatActivity {
    private ScrollView scrollView;
    private TextInputLayout tilTitle;
    private EditText etTitle, etDescription;
    private RadioGroup rgPriority;
    private TextView tvDeadline, tvDeadlineError;
    private LinearLayout llDeadline;
    private Switch swCompleted;
    private Button btnCancel, btnSave;
    private Entity_User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addtask);

        // lấy ScrollView để scroll khi lỗi
        scrollView      = findViewById(R.id.addTask);
        tilTitle        = findViewById(R.id.tilTitle);
        etTitle         = findViewById(R.id.etTitle);
        etDescription   = findViewById(R.id.etDescription);
        rgPriority      = findViewById(R.id.rgPriority);
        tvDeadline      = findViewById(R.id.tvDeadline);
        tvDeadlineError = findViewById(R.id.tvDeadlineError);
        llDeadline      = findViewById(R.id.llDeadline);
        swCompleted     = findViewById(R.id.swCompleted);
        btnCancel       = findViewById(R.id.btnCancel);
        btnSave         = findViewById(R.id.btnSave);

        // Nhận user demo
        user = getIntent().getParcelableExtra("user");

        llDeadline.setOnClickListener(v -> showDateTimePicker());
        btnCancel .setOnClickListener(v -> exitTask());
        btnSave   .setOnClickListener(v -> saveTask());
    }

    private void exitTask() {
        boolean isTitleEmpty    = etTitle.getText().toString().trim().isEmpty();
        boolean isDescEmpty     = etDescription.getText().toString().trim().isEmpty();
        String  deadlineStr     = tvDeadline.getText().toString().trim();
        boolean isDeadlineDefault = deadlineStr.isEmpty() || deadlineStr.equals("Chọn ngày");

        if (isTitleEmpty && isDescEmpty && isDeadlineDefault) {
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thoát")
                .setMessage("Bạn có chắc chắn muốn thoát khi task này chưa được lưu không?")
                .setPositiveButton("Thoát", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton("Ở lại", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void saveTask() {
        // Reset lỗi
        tilTitle.setError(null);
        tvDeadlineError.setVisibility(View.GONE);

        String title       = etTitle.getText().toString().trim();
        String desc        = etDescription.getText().toString().trim();
        String deadlineStr = tvDeadline.getText().toString().trim();
        boolean isCompleted= swCompleted.isChecked();
        int checkedId      = rgPriority.getCheckedRadioButtonId();
        String priority    = checkedId == R.id.rbLow  ? "LOW"
                : checkedId == R.id.rbHigh ? "HIGH"
                : "MEDIUM";

        boolean hasError = false;
        if (title.isEmpty()) {
            tilTitle.setError("Tiêu đề không được bỏ trống!");
            hasError = true;
        }
        if (deadlineStr.isEmpty() || deadlineStr.equals("Chọn ngày giờ")) {
            tvDeadlineError.setText("Ngày giờ hoàn thành không được bỏ trống!");
            tvDeadlineError.setVisibility(View.VISIBLE);
            hasError = true;
        }
        if (hasError) {
            scrollView.post(() -> scrollView.smoothScrollTo(0, tilTitle.getTop()));
            return;
        }

        // Parse ngày giờ
        Date deadline;
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            deadline = sdf.parse(deadlineStr);
        } catch (java.text.ParseException e) {
            tvDeadlineError.setText("Định dạng ngày giờ không hợp lệ!");
            tvDeadlineError.setVisibility(View.VISIBLE);
            scrollView.post(() -> scrollView.smoothScrollTo(0, tvDeadline.getTop()));
            return;
        }

        // Tạo và trả task
        Entity_Task task = new Entity_Task();
        task.setId((int) System.currentTimeMillis());       // hoặc UUID
        task.setUser(user);
        task.setTitle(title);
        task.setDescription(desc);
        task.setPriority(priority);
        task.setComplete(isCompleted);
        task.setCreatedAt(new Date());
        task.setUpdatedAt(new Date());
        task.setDeadline(deadline);

        Intent result = new Intent();
        result.putExtra("task", (Parcelable) task);
        setResult(RESULT_OK, result);
        finish();
    }

    private void showDateTimePicker() {
        // Lấy ngày giờ hiện tại làm mặc định
        final Calendar current = Calendar.getInstance();
        int year  = current.get(Calendar.YEAR);
        int month = current.get(Calendar.MONTH);
        int day   = current.get(Calendar.DAY_OF_MONTH);

        // Hiển thị DatePickerDialog
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    // Khi user chọn xong ngày, tiếp tục mở TimePicker
                    showTimePicker(y, m, d);
                },
                year, month, day
        );
        datePicker.show();
    }

    // 2. Phương thức hiển thị TimePickerDialog, truyền vào ngày đã chọn:
    private void showTimePicker(int year, int month, int day) {
        final Calendar current = Calendar.getInstance();
        int hour   = current.get(Calendar.HOUR_OF_DAY);
        int minute = current.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, h, min) -> {
                    // Khi user chọn xong giờ/phút, tổng hợp vào Calendar
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day, h, min);

                    // Ở đây bạn đã có đầy đủ: year, month, day, hour, minute
                    // Ví dụ: Format ra String hoặc gửi về Activity gọi:
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    String formatted = sdf.format(selected.getTime());
                    tvDeadline.setText(formatted);
                },
                hour, minute,
                true  // true = 24h mode, false = AM/PM
        );
        timePicker.show();
    }

}
