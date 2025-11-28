package com.example.tapticapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.View;


public class MainPage extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);
        BottomSheetBehavior<View> behavior =
                BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

// we want our own positions
        behavior.setFitToContents(false);

// halfway = 50% of screen
        behavior.setHalfExpandedRatio(0.5f);

// we want it to start at the bottom
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

// don’t let user hide it off-screen
        behavior.setHideable(false);

// keep collapsed available
        behavior.setSkipCollapsed(false);

// stop it from going to EXPANDED (full)
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // push it back down to half
                    behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // no-op
            }
        });


    }
}
