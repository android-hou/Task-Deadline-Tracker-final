package com.example.taskdealinetracker_btl;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DetailActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private Entity_Task task;
    private Entity_User user;

    private TextView detailTitle, detailDesc, detailDeadline, detailPriority, detailStatus;
    private ImageView statusIcon;
    private Button btnToggleStatus, btnEdit, btnDelete;

    private ActivityResultLauncher<Intent> editLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        dbHelper = new DatabaseHelper(this);

        // setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết nhiệm vụ");
        }

        // nhận task_id và user
        int taskId = getIntent().getIntExtra("task_id", -1);
        user = getIntent().getParcelableExtra("user");
        if (taskId < 0) {
            finish();
            return;
        }
        task = dbHelper.getTaskById(taskId);
        if (task == null) {
            finish();
            return;
        }

        initViews();
        registerEditLauncher();
        displayTaskInfo();

        btnToggleStatus.setOnClickListener(v -> toggleStatus());
        btnEdit.setOnClickListener(v -> launchEdit());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void initViews() {
        detailTitle    = findViewById(R.id.detailTitle);
        detailDesc     = findViewById(R.id.detailDesc);
        detailDeadline = findViewById(R.id.detailDeadline);
        detailPriority = findViewById(R.id.detailPriority);
        detailStatus   = findViewById(R.id.detailStatus);
        statusIcon     = findViewById(R.id.statusIcon);
        btnToggleStatus= findViewById(R.id.btnToggleStatus);
        btnEdit        = findViewById(R.id.btnEdit);
        btnDelete      = findViewById(R.id.btnDelete);
    }

    private void registerEditLauncher() {
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Entity_Task updated = result.getData().getParcelableExtra("task");
                        if (updated != null) {
                            task = updated;
                            dbHelper.updateTask(task);
                            displayTaskInfo();
                            // trả về TaskActivity để refresh
                            Intent out = new Intent();
                            out.putExtra("task", (Parcelable) task);
                            setResult(RESULT_OK, out);
                        }
                    }
                }
        );
    }

    private void displayTaskInfo() {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        detailTitle.setText(task.getTitle());
        detailDesc.setText(task.getDescription());

        // deadline and color
        detailDeadline.setText("Hạn: " + fmt.format(task.getDeadline()));
        long days = getDaysUntil(task.getDeadline());
        detailDeadline.setTextColor(days<0 ? Color.RED : days<=3 ? Color.parseColor("#FF9800") : Color.parseColor("#4CAF50"));

        // priority and color
        detailPriority.setText("Ưu tiên: " + task.getPriority());
        switch (task.getPriority().toLowerCase(Locale.ROOT)) {
            case "high":   detailPriority.setTextColor(Color.RED); break;
            case "medium": detailPriority.setTextColor(Color.parseColor("#FF9800")); break;
            default:        detailPriority.setTextColor(Color.parseColor("#4CAF50")); break;
        }

        // status and icon
        if (task.isComplete()) {
            detailStatus.setText("Hoàn thành");
            detailStatus.setTextColor(Color.parseColor("#4CAF50"));
            statusIcon.setImageResource(R.drawable.ic_check_circle);
            btnToggleStatus.setText("Đánh dấu chưa hoàn thành");
        } else {
            detailStatus.setText("Đang làm");
            detailStatus.setTextColor(Color.parseColor("#FF9800"));
            statusIcon.setImageResource(R.drawable.ic_radio_button_unchecked);
            btnToggleStatus.setText("Đánh dấu hoàn thành");
        }
    }

    private long getDaysUntil(Date deadline) {
        long diff = deadline.getTime() - new Date().getTime();
        return TimeUnit.MILLISECONDS.toDays(diff);
    }

    private void toggleStatus() {
        task.setComplete(!task.isComplete());
        task.setUpdatedAt(new Date());
        dbHelper.updateTask(task);
        displayTaskInfo();
        Toast.makeText(this,
                task.isComplete() ? "Đã đánh dấu hoàn thành!" : "Đã đánh dấu chưa hoàn thành!",
                Toast.LENGTH_SHORT).show();
        // trả status mới để TaskActivity cập nhật
        Intent out = new Intent();
        out.putExtra("task", (Parcelable) task);
        setResult(RESULT_OK, out);
    }

    private void launchEdit() {
        Intent i = new Intent(this, EditTaskActivity.class);
        i.putExtra("task_id", task.getId());
        editLauncher.launch(i);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa nhiệm vụ này không?")
                .setPositiveButton("Xóa", (d,w) -> deleteTask())
                .setNegativeButton("Hủy", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    private void deleteTask() {
        dbHelper.deleteTask(task.getId());
        Toast.makeText(this, "Đã xóa nhiệm vụ", Toast.LENGTH_SHORT).show();
        Intent out = new Intent();
        out.putExtra("deleted_id", task.getId());
        setResult(RESULT_OK, out);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
