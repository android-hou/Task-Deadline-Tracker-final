package com.example.taskdealinetracker_btl.modules;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Date;

public class Entity_Task implements Parcelable, Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Entity_User user;        // Tham chiếu đến đối tượng User
    private String title;
    private String description;
    private Date deadline;
    private String priority;
    private boolean isComplete;
    private Date createdAt;
    private Date updatedAt;

    public Entity_Task() {}

    // Constructor đầy đủ
    public Entity_Task(int id, Entity_User user, String title, String description,
                       Date deadline, String priority, boolean isComplete) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.isComplete = isComplete;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Constructor dùng khi khôi phục từ Parcel
    protected Entity_Task(Parcel in) {
        id = in.readInt();
        // Đọc user (Entity_User phải implement Parcelable)
        user = in.readParcelable(Entity_User.class.getClassLoader());
        title = in.readString();
        description = in.readString();
        long dl = in.readLong();
        deadline = dl == -1 ? null : new Date(dl);
        priority = in.readString();
        isComplete = in.readByte() != 0;
        long ca = in.readLong();
        createdAt = ca == -1 ? null : new Date(ca);
        long ua = in.readLong();
        updatedAt = ua == -1 ? null : new Date(ua);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        // Ghi user vào Parcel
        dest.writeParcelable(user, flags);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeLong(deadline != null ? deadline.getTime() : -1);
        dest.writeString(priority);
        dest.writeByte((byte) (isComplete ? 1 : 0));
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(updatedAt != null ? updatedAt.getTime() : -1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Entity_Task> CREATOR = new Creator<Entity_Task>() {
        @Override
        public Entity_Task createFromParcel(Parcel in) {
            return new Entity_Task(in);
        }

        @Override
        public Entity_Task[] newArray(int size) {
            return new Entity_Task[size];
        }
    };

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Entity_User getUser() {
        return user;
    }

    public void setUser(Entity_User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isHighPriority() {
        return "HIGH".equalsIgnoreCase(this.priority);
    }
}
