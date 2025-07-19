package com.example.taskdealinetracker_btl.utils;

import android.content.Context;
import android.util.Log;

import com.example.taskdealinetracker_btl.modules.Entity_Task;
import com.example.taskdealinetracker_btl.modules.Entity_User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {
    private static final String TAG = "FileHelper";
    private static final String USER_FILE_NAME = "users.dat";
    private static final String TASK_FILE_NAME = "tasks.dat";

    // --- USER methods ---

    /** Lưu một user duy nhất */
    public static synchronized boolean saveUser(Context context, Entity_User user) {
        if (context == null || user == null) return false;
        try (FileOutputStream fos = context.openFileOutput(USER_FILE_NAME, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(user);
            oos.flush();
            Log.d(TAG, "Saved user: " + user.getUsername());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving user", e);
            return false;
        }
    }

    /** Đọc user đã lưu, trả null nếu chưa có */
    public static synchronized Entity_User loadUser(Context context) {
        if (context == null) return null;
        File file = new File(context.getFilesDir(), USER_FILE_NAME);
        if (!file.exists()) {
            Log.d(TAG, "No user file found");
            return null;
        }
        try (FileInputStream fis = context.openFileInput(USER_FILE_NAME);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            if (obj instanceof Entity_User) {
                return (Entity_User) obj;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user", e);
        }
        return null;
    }

    /** Xóa trắng dữ liệu user (ví dụ logout) */
    public static synchronized void clearUser(Context context) {
        if (context == null) return;
        File file = new File(context.getFilesDir(), USER_FILE_NAME);
        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "User file deleted");
            } else {
                Log.e(TAG, "Failed to delete user file");
            }
        }
    }

    public static synchronized boolean saveTasks(Context context, List<Entity_Task> tasks) {
        if (context == null || tasks == null) {
            Log.e(TAG, "Context or tasks list is null");
            return false;
        }

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = context.openFileOutput(TASK_FILE_NAME, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(new ArrayList<>(tasks));
            oos.flush();
            Log.d(TAG, "Saved " + tasks.size() + " tasks successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving tasks", e);
            return false;
        } finally {
            closeStreams(oos, fos);
        }
    }

    public static synchronized List<Entity_Task> loadTasks(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return new ArrayList<>();
        }

        List<Entity_Task> tasks = new ArrayList<>();

        // Check if file exists
        File file = new File(context.getFilesDir(), TASK_FILE_NAME);
        if (!file.exists()) {
            Log.d(TAG, "Task file doesn't exist, returning empty list");
            return tasks;
        }

        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = context.openFileInput(TASK_FILE_NAME);
            ois = new ObjectInputStream(fis);
            Object obj = ois.readObject();
            if (obj instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Entity_Task> loadedTasks = (List<Entity_Task>) obj;
                tasks = loadedTasks;
                Log.d(TAG, "Loaded " + tasks.size() + " tasks successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tasks", e);
            tasks = new ArrayList<>();
        } finally {
            closeStreams(ois, fis);
        }
        return tasks;
    }

    // Helper method để đóng streams
    private static void closeStreams(Object... streams) {
        for (Object stream : streams) {
            if (stream != null) {
                try {
                    if (stream instanceof ObjectOutputStream) {
                        ((ObjectOutputStream) stream).close();
                    } else if (stream instanceof ObjectInputStream) {
                        ((ObjectInputStream) stream).close();
                    } else if (stream instanceof FileOutputStream) {
                        ((FileOutputStream) stream).close();
                    } else if (stream instanceof FileInputStream) {
                        ((FileInputStream) stream).close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
    }

    // Utility methods
    public static void clearAllData(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }

        try {
            File userFile = new File(context.getFilesDir(), USER_FILE_NAME);
            File taskFile = new File(context.getFilesDir(), TASK_FILE_NAME);

            if (userFile.exists() && userFile.delete()) {
                Log.d(TAG, "User file deleted");
            }

            if (taskFile.exists() && taskFile.delete()) {
                Log.d(TAG, "Task file deleted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing data", e);
        }
    }

    public static boolean hasUserData(Context context) {
        if (context == null) return false;
        File file = new File(context.getFilesDir(), USER_FILE_NAME);
        return file.exists() && file.length() > 0;
    }

    public static boolean hasTaskData(Context context) {
        if (context == null) return false;
        File file = new File(context.getFilesDir(), TASK_FILE_NAME);
        return file.exists() && file.length() > 0;
    }

    // Backup methods
    public static boolean backupData(Context context) {
        if (context == null) return false;

        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            File userFile = new File(context.getFilesDir(), USER_FILE_NAME);
            File taskFile = new File(context.getFilesDir(), TASK_FILE_NAME);

            if (userFile.exists()) {
                File backupUserFile = new File(context.getFilesDir(), USER_FILE_NAME + ".backup." + timestamp);
                copyFile(userFile, backupUserFile);
            }

            if (taskFile.exists()) {
                File backupTaskFile = new File(context.getFilesDir(), TASK_FILE_NAME + ".backup." + timestamp);
                copyFile(taskFile, backupTaskFile);
            }

            Log.d(TAG, "Data backup completed");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error backing up data", e);
            return false;
        }
    }

    private static void copyFile(File source, File destination) throws Exception {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(destination);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }

        fis.close();
        fos.close();
    }
}
