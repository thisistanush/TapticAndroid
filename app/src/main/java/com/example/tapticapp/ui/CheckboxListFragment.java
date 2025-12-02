package com.example.tapticapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tapticapp.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for displaying a list of checkboxes for sounds.
 * Used for both "Monitored" and "Notify" tabs.
 */
public class CheckboxListFragment extends Fragment {

    private static final String ARG_TYPE = "type";
    public static final String TYPE_MONITORED = "monitored";
    public static final String TYPE_NOTIFY = "notify";

    private LinearLayout checkboxContainer;
    private final Map<String, CheckBox> checkboxMap = new HashMap<>();
    private String type;

    public interface OnCheckboxChangeListener {
        void onCheckboxChanged(String label, boolean checked, String type);
    }

    private OnCheckboxChangeListener listener;

    public static CheckboxListFragment newInstance(String type) {
        CheckboxListFragment fragment = new CheckboxListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getString(ARG_TYPE, TYPE_MONITORED);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_checkbox_list, container, false);
        checkboxContainer = view.findViewById(R.id.checkboxContainer);
        return view;
    }

    /**
     * Populate the checkboxes with the given labels.
     */
    public void populateCheckboxes(List<String> labels) {
        if (checkboxContainer == null) {
            return;
        }

        checkboxContainer.removeAllViews();
        checkboxMap.clear();

        for (String label : labels) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(label);
            checkBox.setTextColor(0xFFE5E9F0); // Light gray text
            checkBox.setChecked(true); // Default: all checked

            String tooltipText;
            if (TYPE_MONITORED.equals(type)) {
                tooltipText = "If checked, Taptic will pay attention to this sound.";
            } else {
                tooltipText = "If checked, you will get a notification for this sound.";
            }
            checkBox.setTooltipText(tooltipText);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onCheckboxChanged(label, isChecked, type);
                }
            });

            checkboxMap.put(label, checkBox);
            checkboxContainer.addView(checkBox);
        }
    }

    /**
     * Check if a specific label is checked.
     */
    public boolean isChecked(String label) {
        CheckBox checkBox = checkboxMap.get(label);
        return checkBox != null && checkBox.isChecked();
    }

    /**
     * Set the checked state of a specific label.
     */
    public void setChecked(String label, boolean checked) {
        CheckBox checkBox = checkboxMap.get(label);
        if (checkBox != null) {
            checkBox.setChecked(checked);
        }
    }

    /**
     * Set the checkbox change listener.
     */
    public void setOnCheckboxChangeListener(OnCheckboxChangeListener listener) {
        this.listener = listener;
    }

    public Map<String, CheckBox> getCheckboxMap() {
        return checkboxMap;
    }
}
