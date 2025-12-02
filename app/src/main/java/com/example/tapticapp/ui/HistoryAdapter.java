package com.example.tapticapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tapticapp.R;
import com.example.tapticapp.data.DetectionEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying detection history in RecyclerView.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<DetectionEvent> events = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm:ss a", Locale.getDefault());

    public void setEvents(List<DetectionEvent> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetectionEvent event = events.get(position);

        holder.labelText.setText(event.label);
        holder.confidenceText.setText((int) (event.confidence * 100) + "%");
        holder.timeText.setText(timeFormat.format(new Date(event.timestamp)));

        // Set source indicator
        if (event.isRemote) {
            holder.sourceText.setText("🌐 " + (event.deviceName != null ? event.deviceName : "Remote"));
        } else {
            holder.sourceText.setText("📱 Local");
        }

        // Set emergency indicator color
        int color = event.isEmergency ? 0xFFFF5252 : 0xFF8AB4FF;
        holder.emergencyIndicator.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView labelText;
        TextView confidenceText;
        TextView timeText;
        TextView sourceText;
        View emergencyIndicator;

        ViewHolder(View view) {
            super(view);
            labelText = view.findViewById(R.id.labelText);
            confidenceText = view.findViewById(R.id.confidenceText);
            timeText = view.findViewById(R.id.timeText);
            sourceText = view.findViewById(R.id.sourceText);
            emergencyIndicator = view.findViewById(R.id.emergencyIndicator);
        }
    }
}
