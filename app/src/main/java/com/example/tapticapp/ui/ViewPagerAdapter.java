package com.example.tapticapp.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * ViewPager adapter for tab navigation between Sound Dashboard and Live
 * Captions.
 */
public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new HomeFragment();
        } else {
            return new CaptionsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Sound Dashboard + Live Captions
    }
}
