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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;
import com.example.taskdealinetracker_btl.utils.FileHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskActivity extends AppCompatActivity {
    private Entity_User user;
    private List<Entity_Task> tasks = new ArrayList<>();
    private List<Entity_Task> viewTask = new ArrayList<>();

    private ActivityResultLauncher<Intent> addTaskLauncher;
    private RecyclerView rvTasks;
    private TaskAdapter adapter;

    private String activeTask;
    private Button btnAllState, btnActive, btnFinish, btnOver, btnHighPriority;
    private EditText txtSearch;

    private TextView tvCompletedTasks, tvTotalTasks, tvPendingTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        initViews();

        // 1) Nhận user từ Intent
        user = getIntent().getParcelableExtra("user");
        if (user != null) {
            Toast.makeText(this, "Chào mừng " + user.getUsername(), Toast.LENGTH_SHORT).show();
        }

        // 2) Load task từ FileHelper (hoặc DB)
        List<Entity_Task> loaded = FileHelper.loadTasks(this);
        if (loaded != null) {
            tasks.clear();
            tasks.addAll(loaded);
        }
        // Khởi tạo viewTask bằng toàn bộ tasks
        viewTask.clear();
        viewTask.addAll(tasks);
        adapter.updateData(viewTask);
        updateTaskStats();

        // 3) Khai báo launcher để nhận task mới từ AddActivity
        addTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Entity_Task newTask = result.getData().getParcelableExtra("task");
                        if (newTask != null) {
                            newTask.setUser(user);
                            tasks.add(0, newTask);
                            saveAllTasks();
                            applyCurrentFilter();        // cập nhật viewTask & adapter
                            rvTasks.scrollToPosition(0);
                            updateTaskStats();
                            Toast.makeText(this, "Thêm Task mới thành công!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // 4) Nút thêm
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            Intent i = new Intent(this, AddActivity.class);
            i.putExtra("user", (Parcelable) user);
            addTaskLauncher.launch(i);
        });

        // 5) Các nút filter
        btnAllState .setOnClickListener(v -> { activeTask = "all";    applyCurrentFilter(); });
        btnActive   .setOnClickListener(v -> { activeTask = "active"; applyCurrentFilter(); });
        btnFinish   .setOnClickListener(v -> { activeTask = "finish"; applyCurrentFilter(); });
        btnOver     .setOnClickListener(v -> { activeTask = "over";   applyCurrentFilter(); });
        btnHighPriority.setOnClickListener(v-> { activeTask="highPriority"; applyCurrentFilter(); });

        // 6) Tìm kiếm
        txtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){
                applySearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s){}
        });

        // 7) Khởi mặc định
        activeTask = "all";
    }

    private void initViews() {
        tvTotalTasks     = findViewById(R.id.tvTotalTasks);
        tvPendingTasks   = findViewById(R.id.tvPendingTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);

        btnAllState      = findViewById(R.id.btnFilterAll);
        btnActive        = findViewById(R.id.btnFilterPending);
        btnFinish        = findViewById(R.id.btnFilterCompleted);
        btnOver          = findViewById(R.id.btnFilterOverdue);
        btnHighPriority  = findViewById(R.id.btnFilterPriority);

        txtSearch        = findViewById(R.id.etSearch);

        rvTasks = findViewById(R.id.recyclerViewTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new ArrayList<>());
        rvTasks.setAdapter(adapter);
    }

    /** Lưu toàn bộ tasks hiện tại */
    private void saveAllTasks() {
        FileHelper.saveTasks(this, tasks);
    }

    /** Tính và hiển thị số liệu, sau đó cập nhật viewTask & adapter */
    private void updateTaskStats() {
        int total = tasks.size(), pending = 0, complete = 0;
        for (Entity_Task t : tasks) {
            if (t.isComplete()) complete++;
            else pending++;
        }
        tvTotalTasks.setText("Tổng: " + total + " task");
        tvPendingTasks.setText("Đang thực hiện: " + pending);
        tvCompletedTasks.setText("Hoàn thành: " + complete);
    }

    /** Áp dụng filter dựa trên activeTask và searchText (nếu có) */
    private void applyCurrentFilter() {
        List<Entity_Task> filtered = new ArrayList<>();
        Date now = new Date();
        for (Entity_Task t : tasks) {
            boolean keep;
            // Thay switch-case bằng if-else:
            if ("active".equals(activeTask)) {
                keep = !t.isComplete();
            } else if ("finish".equals(activeTask)) {
                keep = t.isComplete();
            } else if ("over".equals(activeTask)) {
                keep = t.getDeadline() != null && t.getDeadline().before(now);
            } else if ("highPriority".equals(activeTask)) {
                keep = "HIGH".equalsIgnoreCase(t.getPriority());
            } else {
                keep = true;
            }
            if (keep) filtered.add(t);
        }
        // Nếu đang có search text, lọc tiếp
        String q = txtSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            filtered.removeIf(t -> t.getTitle() == null
                    || !t.getTitle().toLowerCase(Locale.ROOT).contains(q));
        }

        viewTask.clear();
        viewTask.addAll(filtered);
        adapter.updateData(viewTask);
        updateTaskStats();
    }

    /** Áp dụng tìm kiếm độc lập khi đổi text */
    private void applySearch(String q) {
        applyCurrentFilter();
    }

    // === Adapter ===
    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
        private final SimpleDateFormat dateFmt =
                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        private final List<Entity_Task> data;

        public TaskAdapter(List<Entity_Task> initialData) {
            this.data = initialData;
        }

        public void updateData(List<Entity_Task> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Entity_Task t = data.get(pos);
            h.tvTitle   .setText(t.getTitle()!=null? t.getTitle():"(No title)");
            h.tvDeadline.setText("Hạn: " +
                    (t.getDeadline()!=null? dateFmt.format(t.getDeadline()):"(no deadline)"));
            h.tvPriority.setText("Ưu tiên: " +
                    (t.getPriority()!=null? t.getPriority():"(none)"));

            h.cbDone.setOnCheckedChangeListener(null);
            h.cbDone.setChecked(t.isComplete());
            h.tvStatus.setText(t.isComplete()? "Hoàn thành":"Đang làm");

            h.cbDone.setOnCheckedChangeListener((view, checked)->{
                t.setComplete(checked);
                t.setUpdatedAt(new Date());
                updateTaskStats();
                // Bạn có thể gọi back save tại đây nếu muốn
            });

            h.itemView.setOnClickListener(v->{
                Intent i = new Intent(v.getContext(), DetailActivity.class);
                i.putExtra("task", (Parcelable)t);
                v.getContext().startActivity(i);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDeadline, tvPriority, tvStatus;
            CheckBox cbDone;
            ViewHolder(@NonNull View v) {
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
