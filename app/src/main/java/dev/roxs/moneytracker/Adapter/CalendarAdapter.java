package dev.roxs.moneytracker.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import dev.roxs.moneytracker.R;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarViewHolder> {

    private final ArrayList<String> daysOfMonth;
    private final OnItemListener onItemListener;

    public CalendarAdapter(ArrayList<String> daysOfMonth, OnItemListener onItemListener) {
        this.daysOfMonth = daysOfMonth;
        this.onItemListener = onItemListener;
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
        holder.dayOfMonth.setText(daysOfMonth.get(position));
        if(!daysOfMonth.get(position).isEmpty()){
            holder.dayOfMonth.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(),R.drawable.component_rounded_border));
//            holder.dayOfMonth.setWidth(400);
//            holder.dayOfMonth.setPadding(40,20,40,20);
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
