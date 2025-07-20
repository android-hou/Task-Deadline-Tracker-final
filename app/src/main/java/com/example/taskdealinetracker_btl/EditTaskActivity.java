package com.example.taskdealinetracker_btl;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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

import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditTaskActivity extends AppCompatActivity {
    private ScrollView scrollView;
    private EditText etTitle, etDescription;
    private TextView tvDeadline, tvDeadlineError;
    private RadioGroup rgPriority;
    private Switch swCompleted;
    private Button btnCancel, btnSave;

    private DatabaseHelper dbHelper;
    private Entity_Task task;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edittask);

        dbHelper = new DatabaseHelper(this);
        scrollView = findViewById(R.id.editTask);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvDeadlineError = findViewById(R.id.tvDeadlineError);
        rgPriority = findViewById(R.id.rgPriority);
        swCompleted = findViewById(R.id.swCompleted);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);

        int taskId = getIntent().getIntExtra("task_id", -1);
        if (taskId < 0) {
            Toast.makeText(this, "Task không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        task = dbHelper.getTaskById(taskId);
        if (task == null) {
            Toast.makeText(this, "Không tìm thấy Task", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindData();

        tvDeadline.setOnClickListener(v -> showDateTimePicker());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveTask());
    }

    private void bindData() {
        etTitle.setText(task.getTitle());
        etDescription.setText(task.getDescription());
        tvDeadline.setText(task.getDeadline() != null ? dateFormat.format(task.getDeadline()) : "Chọn ngày giờ");
        swCompleted.setChecked(task.isComplete());

        String pr = task.getPriority();
        if ("HIGH".equalsIgnoreCase(pr)) {
            rgPriority.check(R.id.rbHigh);
        } else if ("LOW".equalsIgnoreCase(pr)) {
            rgPriority.check(R.id.rbLow);
        } else {
            rgPriority.check(R.id.rbMedium);
        }
    }

    private void saveTask() {
        // Reset errors
        tvDeadlineError.setVisibility(View.GONE);
        etTitle.setError(null);

        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String deadlineStr = tvDeadline.getText().toString().trim();
        boolean completed = swCompleted.isChecked();
        int prId = rgPriority.getCheckedRadioButtonId();
        String priority = prId == R.id.rbHigh ? "HIGH" : prId == R.id.rbLow ? "LOW" : "MEDIUM";

        boolean hasError = false;
        if (title.isEmpty()) {
            etTitle.setError("Tiêu đề không được để trống");
            hasError = true;
        }
        if (deadlineStr.isEmpty() || deadlineStr.equals("Chọn ngày giờ")) {
            tvDeadlineError.setText("Chưa chọn ngày giờ");
            tvDeadlineError.setVisibility(View.VISIBLE);
            hasError = true;
        }
        if (hasError) {
            scrollView.post(() -> scrollView.smoothScrollTo(0, etTitle.getTop()));
            return;
        }

        Date deadline;
        try {
            deadline = dateFormat.parse(deadlineStr);
        } catch (Exception e) {
            tvDeadlineError.setText("Định dạng ngày giờ không hợp lệ");
            tvDeadlineError.setVisibility(View.VISIBLE);
            scrollView.post(() -> scrollView.smoothScrollTo(0, tvDeadline.getTop()));
            return;
        }

        // Update task object
        task.setTitle(title);
        task.setDescription(desc);
        task.setDeadline(deadline);
        task.setPriority(priority);
        task.setComplete(completed);
        task.setUpdatedAt(new Date());

        // Persist to DB
        int rows = dbHelper.updateTask(task);
        if (rows > 0) {
            Intent out = new Intent();
            out.putExtra("task", (Parcelable) task);
            setResult(RESULT_OK, out);
            finish();
        } else {
            Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (view, y, m, d) -> showTimePicker(y, m, d),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker(int year, int month, int day) {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(
                this,
                (view, h, min) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, day, h, min);
                    tvDeadline.setText(dateFormat.format(sel.getTime()));
                },
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
