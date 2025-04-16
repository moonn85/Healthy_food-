package com.example.btlandroid.Utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.example.btlandroid.Activity.CartActivity;
import com.example.btlandroid.Activity.FavoriteActivity;
import com.example.btlandroid.Activity.MainActivity;
import com.example.btlandroid.Activity.ProfileActivity;
import com.example.btlandroid.Activity.SearchActivity;
import com.example.btlandroid.R;

public class BottomNavigationUtils {

    /**
     * Thiết lập điều hướng bottom navigation cho một activity
     *
     * @param activity Activity hiện tại
     * @param activeNavId Id của navigation item đang được chọn (R.id.homeNav, R.id.cartNav, v.v.)
     */
    public static void setupBottomNavigation(Activity activity, int activeNavId) {
        // Thiết lập điều hướng Trang chủ
        View homeNav = activity.findViewById(R.id.homeNav);
        TextView homeText = activity.findViewById(R.id.textViewHome);
        if (activeNavId == R.id.homeNav) {
            homeText.setTextColor(activity.getResources().getColor(R.color.yellow));
        }
        homeNav.setOnClickListener(v -> {
            if (!(activity instanceof MainActivity)) {
                activity.startActivity(new Intent(activity, MainActivity.class));
                activity.finish();
            }
        });

        // Thiết lập điều hướng Tìm kiếm
        View searchNav = activity.findViewById(R.id.searchNav);
        searchNav.setOnClickListener(v -> {
            if (!(activity instanceof SearchActivity)) {
                activity.startActivity(new Intent(activity, SearchActivity.class));
                if (!(activity instanceof MainActivity)) {
                    activity.finish();
                }
            }
        });

        // Thiết lập điều hướng Yêu thích
        View favoriteNav = activity.findViewById(R.id.favoriteNav);
        TextView favoriteText = activity.findViewById(R.id.textViewYeuthich);
        if (activeNavId == R.id.favoriteNav) {
            favoriteText.setTextColor(activity.getResources().getColor(R.color.yellow));
        }
        favoriteNav.setOnClickListener(v -> {
            if (!(activity instanceof FavoriteActivity)) {
                activity.startActivity(new Intent(activity, FavoriteActivity.class));
                activity.finish();
            }
        });

        // Thiết lập điều hướng Giỏ hàng
        View cartNav = activity.findViewById(R.id.cartNav);
        TextView cartText = activity.findViewById(R.id.textViewGiohang);
        if (activeNavId == R.id.cartNav) {
            cartText.setTextColor(activity.getResources().getColor(R.color.yellow));
        }
        cartNav.setOnClickListener(v -> {
            if (!(activity instanceof CartActivity)) {
                activity.startActivity(new Intent(activity, CartActivity.class));
                activity.finish();
            }
        });

        // Thiết lập điều hướng Hồ sơ
        View profileNav = activity.findViewById(R.id.profileNav);
        TextView profileText = activity.findViewById(R.id.textViewProfile);
        if (activeNavId == R.id.profileNav) {
            profileText.setTextColor(activity.getResources().getColor(R.color.yellow));
        }
        profileNav.setOnClickListener(v -> {
            if (!(activity instanceof ProfileActivity)) {
                activity.startActivity(new Intent(activity, ProfileActivity.class));
                // Không kết thúc activity ở đây để cho phép quay lại activity trước đó
            }
        });
    }
}
