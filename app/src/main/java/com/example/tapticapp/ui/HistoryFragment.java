package com.example.tapticapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tapticapp.R;
import com.example.tapticapp.data.HistoryRepository;

/**
 * History fragment showing past detections.
 */
public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private Button clearButton;
    private HistoryAdapter adapter;
    private HistoryRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.historyRecyclerView);
        emptyText = view.findViewById(R.id.emptyText);
        clearButton = view.findViewById(R.id.clearHistoryButton);

        // Setup RecyclerView
        adapter = new HistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Setup repository
        repository = HistoryRepository.getInstance(requireContext());
        repository.addCallback(events -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setEvents(events);
                    if (events.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyText.setVisibility(View.GONE);
                    }
                });
            }
        });

        // Load initial history
        repository.loadHistory(events -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setEvents(events);
                    if (events.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyText.setVisibility(View.GONE);
                    }
                });
            }
        });

        clearButton.setOnClickListener(v -> repository.clearAll());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (repository != null) {
            repository.removeCallback(events -> {
            });
        }
    }
}
