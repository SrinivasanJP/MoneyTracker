package dev.roxs.moneytracker.Adapter;

import android.content.Context;
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
    private final Set<String> recordedDates;

    public CalendarAdapter(ArrayList<String> daysOfMonth, OnItemListener onItemListener, List<String> recordedDates) {
        this.daysOfMonth = daysOfMonth;
        this.onItemListener = onItemListener;
        this.recordedDates = new HashSet<>(recordedDates);
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_cell, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int)(parent.getHeight()*0.12);
        return new CalendarViewHolder(view,onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String dayText = daysOfMonth.get(position);
        holder.dayOfMonth.setText(dayText);


        Context context = holder.itemView.getContext();

        if (!dayText.isEmpty()) {
            holder.dayOfMonth.setBackground(ContextCompat.getDrawable(context, R.drawable.component_rounded_border));

            // Highlight recorded date
            if (recordedDates.contains(dayText)) {
                // You can change background or text color
                holder.dayOfMonth.setBackground(ContextCompat.getDrawable(context, R.drawable.component_tags_secondary)); // use a different drawable for highlighted
                holder.dayOfMonth.setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary));
                holder.dayOfMonth.setTypeface(ResourcesCompat.getFont(context, R.font.primary_bold));

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
