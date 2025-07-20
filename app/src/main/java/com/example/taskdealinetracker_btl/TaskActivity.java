package com.example.taskdealinetracker_btl;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskActivity extends AppCompatActivity {
    private Entity_User user;
    private List<Entity_Task> tasks = new ArrayList<>();
    private List<Entity_Task> viewTask = new ArrayList<>();

    private DatabaseHelper dbHelper;
    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private EditText txtSearch;
    private Button btnFilterAll, btnFilterPending, btnFilterCompleted, btnFilterOverdue, btnFilterPriority;
    private TextView tvTotalTasks, tvPendingTasks, tvCompletedTasks;
    private ImageButton btnAna;
    private String activeFilter = "all";

    private ActivityResultLauncher<Intent> addLauncher;
    private ActivityResultLauncher<Intent> detailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        dbHelper = new DatabaseHelper(this);
        user = getIntent().getParcelableExtra("user");

        initViews();
        setupLaunchers();

        if (user != null) {
            Toast.makeText(this, "Chào mừng " + user.getUsername(), Toast.LENGTH_SHORT).show();
        }

        loadTasks();
        applyFilterAndRefresh();

        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            Intent i = new Intent(this, AddActivity.class);
            i.putExtra("user", (Parcelable) user);
            addLauncher.launch(i);
        });

        btnFilterAll.setOnClickListener(v -> { activeFilter = "all"; applyFilterAndRefresh(); });
        btnFilterPending.setOnClickListener(v -> { activeFilter = "active"; applyFilterAndRefresh(); });
        btnFilterCompleted.setOnClickListener(v -> { activeFilter = "finish"; applyFilterAndRefresh(); });
        btnFilterOverdue.setOnClickListener(v -> { activeFilter = "over"; applyFilterAndRefresh(); });
        btnFilterPriority.setOnClickListener(v -> { activeFilter = "highPriority"; applyFilterAndRefresh(); });
        btnAna.setOnClickListener(v -> {
            Intent i = new Intent(this, AnalyticsActivity.class);
            i.putExtra("user", (Parcelable) user);
            startActivity(i);
        });
        txtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { applyFilterAndRefresh(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void initViews() {
        rvTasks = findViewById(R.id.recyclerViewTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter();
        rvTasks.setAdapter(adapter);

        txtSearch = findViewById(R.id.etSearch);

        btnFilterAll       = findViewById(R.id.btnFilterAll);
        btnFilterPending   = findViewById(R.id.btnFilterPending);
        btnFilterCompleted = findViewById(R.id.btnFilterCompleted);
        btnFilterOverdue   = findViewById(R.id.btnFilterOverdue);
        btnFilterPriority  = findViewById(R.id.btnFilterPriority);
        btnAna             = findViewById(R.id.btnAna);

        tvTotalTasks     = findViewById(R.id.tvTotalTasks);
        tvPendingTasks   = findViewById(R.id.tvPendingTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
    }

    private void setupLaunchers() {
        addLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Entity_Task newTask = result.getData().getParcelableExtra("task");
                        if (newTask != null) {
                            newTask.setUser(user);
                            long id = dbHelper.insertTask(newTask);
                            newTask.setId((int) id);
                            tasks.add(0, newTask);
                            applyFilterAndRefresh();
                            rvTasks.scrollToPosition(0);
                            Toast.makeText(this, "Thêm Task mới thành công!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.hasExtra("deleted_id")) {
                            int delId = data.getIntExtra("deleted_id", -1);
                            for (int i = 0; i < tasks.size(); i++) {
                                if (tasks.get(i).getId() == delId) {
                                    tasks.remove(i);
                                    break;
                                }
                            }
                            Toast.makeText(this, "Đã xóa nhiệm vụ", Toast.LENGTH_SHORT).show();
                        } else if (data.hasExtra("task")) {
                            Entity_Task updated = data.getParcelableExtra("task");
                            for (int i = 0; i < tasks.size(); i++) {
                                if (tasks.get(i).getId() == updated.getId()) {
                                    tasks.set(i, updated);
                                    break;
                                }
                            }
                            Toast.makeText(this, "Cập nhật Task thành công!", Toast.LENGTH_SHORT).show();
                        }
                        applyFilterAndRefresh();
                    }
                }
        );
    }

    private void loadTasks() {
        tasks.clear();
        tasks.addAll(dbHelper.getTasksByUserId(user.getId()));
    }

    private void applyFilterAndRefresh() {
        viewTask.clear();
        Date now = new Date();
        String q = txtSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        for (Entity_Task t : tasks) {
            boolean keep;
            switch (activeFilter) {
                case "active": keep = !t.isComplete(); break;
                case "finish": keep = t.isComplete(); break;
                case "over":   keep = t.getDeadline()!=null && t.getDeadline().before(now); break;
                case "highPriority": keep = "HIGH".equalsIgnoreCase(t.getPriority()); break;
                default: keep = true;
            }
            if (keep && (q.isEmpty() || (t.getTitle()!=null && t.getTitle().toLowerCase(Locale.ROOT).contains(q)))) {
                viewTask.add(t);
            }
        }
        adapter.setData(viewTask);
        updateStats();
    }

    private void updateStats() {
        int total = tasks.size(), pending = 0, complete = 0;
        for (Entity_Task t : tasks) {
            if (t.isComplete()) complete++; else pending++;
        }
        tvTotalTasks.setText("Tổng: " + total + " task");
        tvPendingTasks.setText("Đang thực hiện: " + pending);
        tvCompletedTasks.setText("Hoàn thành: " + complete);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
        private List<Entity_Task> data = new ArrayList<>();
        private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        void setData(List<Entity_Task> list) {
            data.clear(); data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Entity_Task t = data.get(pos);
            h.tvTitle.setText(t.getTitle());
            h.tvDeadline.setText("Hạn: " + (t.getDeadline()!=null?df.format(t.getDeadline()):"(no deadline)"));
            h.tvPriority.setText("Ưu tiên: " + t.getPriority());
            h.cbDone.setOnCheckedChangeListener(null);
            h.cbDone.setChecked(t.isComplete());
            h.tvStatus.setText(t.isComplete()?"Hoàn thành":"Đang làm");

            h.cbDone.setOnCheckedChangeListener((btn, chk) -> {
                t.setComplete(chk);
                t.setUpdatedAt(new Date());
                dbHelper.updateTask(t);
                updateStats();
            });

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(TaskActivity.this, DetailActivity.class);
                i.putExtra("task_id", t.getId());
                detailLauncher.launch(i);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDeadline, tvPriority, tvStatus;
            CheckBox cbDone;
            ViewHolder(View v) {
                super(v);
                tvTitle    = v.findViewById(R.id.item_tvTitle);
                tvDeadline = v.findViewById(R.id.item_tvDeadline);
                tvPriority = v.findViewById(R.id.item_tvPriority);
                tvStatus   = v.findViewById(R.id.item_tvStatus);
                cbDone     = v.findViewById(R.id.cbDone);
            }
        }
    }
}
