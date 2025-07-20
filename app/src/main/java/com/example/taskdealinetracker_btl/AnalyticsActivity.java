package com.example.taskdealinetracker_btl;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;

import java.util.Date;
import java.util.List;

public class AnalyticsActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private Entity_User user;

    private TextView tvTotalCount, tvCompletionRate, tvOnTimeRate, tvLateRate, tvPendingCount;
    private ProgressBar pbCompletion, pbOnTime, pbLate;
    private Button btnHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        db = new DatabaseHelper(this);
        user = getIntent().getParcelableExtra("user");

        bindViews();
        computeAndDisplay();

        btnHome.setOnClickListener(v -> {
            finish();
        });
    }

    private void bindViews() {
        tvTotalCount    = findViewById(R.id.tvTotalCount);
        tvCompletionRate= findViewById(R.id.tvCompletionRate);
        tvOnTimeRate    = findViewById(R.id.tvOnTimeRate);
        tvLateRate      = findViewById(R.id.tvLateRate);
        tvPendingCount  = findViewById(R.id.tvPendingCount);

        pbCompletion    = findViewById(R.id.pbCompletionRate);
        pbOnTime        = findViewById(R.id.pbOnTimeRate);
        pbLate          = findViewById(R.id.pbLateRate);

        btnHome         = findViewById(R.id.btnHome);
    }

    private void computeAndDisplay() {
        List<Entity_Task> tasks = db.getTasksByUserId(user.getId());
        int total = tasks.size();
        int completed = 0, onTime = 0, late = 0, pending = 0;

        for (Entity_Task t : tasks) {
            if (t.isComplete()) {
                completed++;
                Date updated = t.getUpdatedAt();
                Date dl      = t.getDeadline();
                if (updated != null && dl != null) {
                    if (updated.getTime() <= dl.getTime()) onTime++;
                    else late++;
                }
            } else {
                pending++;
            }
        }

        int compPct = total > 0 ? (completed * 100 / total) : 0;
        int onTimePct = completed > 0 ? (onTime * 100 / completed) : 0;
        int latePct   = completed > 0 ? (late   * 100 / completed) : 0;

        tvTotalCount.setText("Tổng: " + total + " nhiệm vụ");
        tvCompletionRate.setText(compPct + "%");
        tvOnTimeRate.setText(onTimePct + "%");
        tvLateRate.setText(latePct + "%");
        tvPendingCount.setText("Chưa hoàn thành: " + pending + " nhiệm vụ");

        pbCompletion.setProgress(compPct);
        pbOnTime.setProgress(onTimePct);
        pbLate.setProgress(latePct);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
