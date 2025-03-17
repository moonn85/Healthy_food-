package com.example.btlandroid.Utils;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * Lớp tiện ích để quản lý toast và tránh hiển thị quá nhiều toast
 */
public class ToastUtils {
    private static Toast currentToast;
    private static final int MIN_TOAST_INTERVAL = 1000; // 1 giây
    private static long lastToastTime = 0;

    /**
     * Hiển thị thông báo toast, hủy toast trước đó nếu còn đang hiển thị
     *
     * @param context Context của ứng dụng
     * @param message Nội dung thông báo
     * @param length  Thời gian hiển thị (Toast.LENGTH_SHORT hoặc Toast.LENGTH_LONG)
     */
    public static void showToast(Context context, String message, int length) {
        if (context == null) return;
        
        try {
            // Kiểm tra thời gian giữa các lần hiển thị toast
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastToastTime < MIN_TOAST_INTERVAL) {
                return;
            }
            
            // Hủy toast trước đó nếu có
            if (currentToast != null) {
                currentToast.cancel();
            }
            
            // Tạo toast mới với context application để tránh lỗi
            currentToast = Toast.makeText(context.getApplicationContext(), message, length);
            currentToast.show();
            
            // Cập nhật thời gian hiển thị toast cuối cùng
            lastToastTime = currentTime;
        } catch (Exception e) {
            // Log lỗi nếu có vấn đề với Toast
            Log.e("ToastUtils", "Lỗi hiển thị toast: " + e.getMessage());
        }
    }
    
    /**
     * Phiên bản rút gọn với độ dài mặc định là Toast.LENGTH_SHORT
     */
    public static void showToast(Context context, String message) {
        showToast(context, message, Toast.LENGTH_SHORT);
    }
}
