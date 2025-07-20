package com.example.taskdealinetracker_btl.modules;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;
public class Entity_User implements Parcelable, Serializable {
    private static int total = 1;
    private int id;
    private String username;

    public Entity_User(){
        this.id = total;
        total ++;
    }

    public Entity_User(String username){
        this.id = total;
        this.username = username;
        total ++;
    }

    // 1. Constructor nhận Parcel để tái tạo đối tượng
    protected Entity_User(Parcel in) {
        // phải đọc các trường đúng thứ tự mà bạn đã ghi trong writeToParcel()
        id = in.readInt();
        username = in.readString();
    }


    // 2. CREATOR: Nhà máy sinh ra đối tượng từ Parcel
    public static final Creator<Entity_User> CREATOR = new Creator<Entity_User>() {
        // Tạo một thể hiện Entity_User từ dữ liệu đọc được trong Parcel
        @Override
        public Entity_User createFromParcel(Parcel in) {
            return new Entity_User(in);
        }
        // Tạo mảng Entity_User với kích thước size
        @Override
        public Entity_User[] newArray(int size) {
            return new Entity_User[size];
        }
    };

    // 3. Miêu tả nội dung đặc biệt trong Parcel (thường để 0)
    @Override
    public int describeContents() {
        return 0;
    }

    // 4. Viết dữ liệu của đối tượng vào Parcel (đóng gói)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // Ghi các trường vào dest theo thứ tự
        dest.writeInt(id);
        dest.writeString(username);
    }

    public int getId() {
        return id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setId(int id) {
        this.id = id;
    }
}
