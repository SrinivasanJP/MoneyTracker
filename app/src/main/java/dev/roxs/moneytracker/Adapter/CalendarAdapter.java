package dev.roxs.moneytracker.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.roxs.moneytracker.R;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarViewHolder> {

    private final ArrayList<String> daysOfMonth;
    private final OnItemListener onItemListener;
    private final Set<Integer> recordedDates;
    private final float[] dailySpends;
    private final double averageSpent;

    public CalendarAdapter(ArrayList<String> daysOfMonth, OnItemListener onItemListener, List<Integer> datesWithData, float[] dailySpends, double averageSpent) {
        this.daysOfMonth = daysOfMonth;
        this.onItemListener = onItemListener;
        this.recordedDates = new HashSet<>(datesWithData);
        this.dailySpends = dailySpends;
        this.averageSpent = averageSpent;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_cell, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int)(parent.getHeight() * 0.12);
        return new CalendarViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String dayText = daysOfMonth.get(position);
        holder.dayOfMonth.setText(dayText);
        Context context = holder.itemView.getContext();

        if (!dayText.isEmpty()) {
            int day = Integer.parseInt(dayText);
            
            // Set default styling (no border, gray text)
            holder.dayOfMonth.setBackground(ContextCompat.getDrawable(context, R.drawable.component_rounded_border));
            holder.dayOfMonth.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.dayOfMonth.setTypeface(ResourcesCompat.getFont(context, R.font.primary_medium));

            // Highlight recorded dates with spend logic
            if (recordedDates.contains(day)) {
                float spent = dailySpends[day - 1]; // dailySpends is 0-indexed where index 0 is day 1

                // Determine border color based on spending compared to average
                int backgroundRes;
                if (spent <= averageSpent) {
                    backgroundRes = R.drawable.calendar_border_green;
                } else if (spent < 2 * averageSpent) {
                    backgroundRes = R.drawable.calendar_border_yellow;
                } else {
                    backgroundRes = R.drawable.calendar_border_red;
                }

                holder.dayOfMonth.setBackground(ContextCompat.getDrawable(context, backgroundRes));
                holder.dayOfMonth.setTypeface(ResourcesCompat.getFont(context, R.font.primary_bold));
                holder.dayOfMonth.setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary)); // Optional: to make numbers stand out when highlighted if desired, currently sticking to user spec
            }
        } else {
            holder.dayOfMonth.setText(""); // in case of empty cell
        }
    }

    @Override
    public int getItemCount() {
        return daysOfMonth.size();
    }

    public interface OnItemListener{
        void onItemClick(int position, String dayText);
    }
}
