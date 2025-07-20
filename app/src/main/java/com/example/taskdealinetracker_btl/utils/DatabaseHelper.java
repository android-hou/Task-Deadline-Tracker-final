package com.example.taskdealinetracker_btl.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "task_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // Table User
    private static final String TABLE_USER = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USERNAME = "username";

    // Table Task
    private static final String TABLE_TASK = "tasks";
    private static final String COLUMN_TASK_ID = "id";
    private static final String COLUMN_TASK_USER_ID = "user_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_DEADLINE = "deadline";
    private static final String COLUMN_PRIORITY = "priority";
    private static final String COLUMN_IS_COMPLETE = "is_complete";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USER + " ("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT NOT NULL"
                + ");";

        String createTaskTable = "CREATE TABLE " + TABLE_TASK + " ("
                + COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TASK_USER_ID + " INTEGER NOT NULL,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_DEADLINE + " INTEGER,"
                + COLUMN_PRIORITY + " TEXT,"
                + COLUMN_IS_COMPLETE + " INTEGER DEFAULT 0,"
                + COLUMN_CREATED_AT + " INTEGER,"
                + COLUMN_UPDATED_AT + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_TASK_USER_ID + ") REFERENCES " + TABLE_USER + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE"
                + ");";

        db.execSQL(createUserTable);
        db.execSQL(createTaskTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    // ===== User CRUD =====
    public long insertUser(Entity_User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, user.getUsername());
        long id = db.insertOrThrow(TABLE_USER, null, values);
        db.close();
        return id;
    }

    public Entity_User getUserById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER,
                new String[]{COLUMN_USER_ID, COLUMN_USERNAME},
                COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        Entity_User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = new Entity_User(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
            // Manually set ID
            // Reflection or setter required; assume a setter exists:
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
            cursor.close();
        }
        db.close();
        return user;
    }

    public List<Entity_User> getAllUsers() {
        List<Entity_User> users = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_USER;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Entity_User user = new Entity_User(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
                users.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }

    public int updateUser(Entity_User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, user.getUsername());
        int rows = db.update(TABLE_USER,
                values,
                COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(user.getId())});
        db.close();
        return rows;
    }

    public int deleteUser(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_USER,
                COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        db.close();
        return rows;
    }

    // ===== Task CRUD =====
    public long insertTask(Entity_Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_USER_ID, task.getUser().getId());
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_DEADLINE, task.getDeadline() != null ? task.getDeadline().getTime() : null);
        values.put(COLUMN_PRIORITY, task.getPriority());
        values.put(COLUMN_IS_COMPLETE, task.isComplete() ? 1 : 0);
        values.put(COLUMN_CREATED_AT, task.getCreatedAt() != null ? task.getCreatedAt().getTime() : null);
        values.put(COLUMN_UPDATED_AT, task.getUpdatedAt() != null ? task.getUpdatedAt().getTime() : null);
        long id = db.insertOrThrow(TABLE_TASK, null, values);
        db.close();
        return id;
    }

    public Entity_Task getTaskById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASK,
                null,
                COLUMN_TASK_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        Entity_Task task = null;
        if (cursor != null && cursor.moveToFirst()) {
            task = cursorToTask(cursor);
            cursor.close();
        }
        db.close();
        return task;
    }

    public List<Entity_Task> getTasksByUserId(int userId) {
        List<Entity_Task> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASK,
                null,
                COLUMN_TASK_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                tasks.add(cursorToTask(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public int updateTask(Entity_Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_DEADLINE, task.getDeadline() != null ? task.getDeadline().getTime() : null);
        values.put(COLUMN_PRIORITY, task.getPriority());
        values.put(COLUMN_IS_COMPLETE, task.isComplete() ? 1 : 0);
        values.put(COLUMN_UPDATED_AT, new Date().getTime());
        int rows = db.update(TABLE_TASK,
                values,
                COLUMN_TASK_ID + "=?",
                new String[]{String.valueOf(task.getId())});
        db.close();
        return rows;
    }

    public int deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_TASK,
                COLUMN_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        db.close();
        return rows;
    }

    // Helper: convert cursor to Entity_Task
    private Entity_Task cursorToTask(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID));
        int userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_USER_ID));
        Entity_User user = getUserById(userId);
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        String desc = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
        long dl = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DEADLINE));
        long ca = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
        long ua = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
        boolean complete = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETE)) == 1;
        String priority = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY));

        Entity_Task task = new Entity_Task(id, user, title, desc,
                dl > 0 ? new Date(dl) : null,
                priority, complete);
        task.setCreatedAt(ca > 0 ? new Date(ca) : null);
        task.setUpdatedAt(ua > 0 ? new Date(ua) : null);
        return task;
    }
}
