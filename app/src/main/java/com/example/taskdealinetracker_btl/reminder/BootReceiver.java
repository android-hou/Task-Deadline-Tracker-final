package com.example.taskdealinetracker_btl.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.taskdealinetracker_btl.utils.DatabaseHelper;
import com.example.taskdealinetracker_btl.modules.Entity_Task;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            DatabaseHelper db = new DatabaseHelper(context);
            for (Entity_Task t : db.getTasksByUserId( /* TODO: lấy user id */ 1)) {
                if (!t.isComplete()) ReminderReceiver.scheduleNext(context, t.getId());
            }
            db.close();
        }
    }
}