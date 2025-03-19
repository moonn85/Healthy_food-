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
     * Sets up the bottom navigation for an activity
     *
     * @param activity The current activity
     * @param activeNavId The id of the active navigation item (R.id.homeNav, R.id.cartNav, etc.)
     */
    public static void setupBottomNavigation(Activity activity, int activeNavId) {
        // Setup Home navigation
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

        // Setup Search navigation
        View searchNav = activity.findViewById(R.id.searchNav);
        searchNav.setOnClickListener(v -> {
            if (!(activity instanceof SearchActivity)) {
                activity.startActivity(new Intent(activity, SearchActivity.class));
                if (!(activity instanceof MainActivity)) {
                    activity.finish();
                }
            }
        });

        // Setup Favorite navigation
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

        // Setup Cart navigation
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

        // Setup Profile navigation
        View profileNav = activity.findViewById(R.id.profileNav);
        TextView profileText = activity.findViewById(R.id.textViewProfile);
        if (activeNavId == R.id.profileNav) {
            profileText.setTextColor(activity.getResources().getColor(R.color.yellow));
        }
        profileNav.setOnClickListener(v -> {
            if (!(activity instanceof ProfileActivity)) {
                activity.startActivity(new Intent(activity, ProfileActivity.class));
                // Don't finish here to allow returning to previous activity
            }
        });
    }
}
