package dev.roxs.moneytracker.Adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dev.roxs.moneytracker.R;

public class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public final TextView dayOfMonth;
    private final CalendarAdapter.OnItemListener onItemListener;

    public CalendarViewHolder(@NonNull View itemView, CalendarAdapter.OnItemListener onItemListener) {
        super(itemView);
        dayOfMonth = itemView.findViewById(R.id.cellDayText);
        this.onItemListener = onItemListener;
        itemView.setOnClickListener(this); // ✅ Now this works because this implements OnClickListener
    }

    @Override
    public void onClick(View view) {
        onItemListener.onItemClick(getAdapterPosition(), dayOfMonth.getText().toString());
    }
}
