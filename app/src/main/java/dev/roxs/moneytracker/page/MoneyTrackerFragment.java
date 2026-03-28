package dev.roxs.moneytracker.page;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.Adapter.CalendarAdapter;
import dev.roxs.moneytracker.helper.DateTimeHelper;
import dev.roxs.moneytracker.helper.SQl_Helper;

public class MoneyTrackerFragment extends Fragment implements CalendarAdapter.OnItemListener {

    private RelativeLayout dailyInputButton, todaySpentLayout, progressFill, progressContainer;
    private TextView date, balanceAmountWhole, balanceAmountFraction, vTotalSpent, vMonthStartHoldings, vPercentageOfLastMonth, vTodaySpent;
    private SQl_Helper sql;

    private CardView vInvestmentsLayout, vLoanBalanceLayout, vTotalSpentOfMonth;
    private CardView vAvgSpentLayout;
    private CardView vTotalCreditsOfMonth;

    private TextView vMonthText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;

    private ImageView leftArrow, rightArrow, vSettings;
    ArrayList<Integer> datesWithData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_money_tracker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sql = new SQl_Helper(requireContext());

        date = view.findViewById(R.id.date);
        balanceAmountWhole = view.findViewById(R.id.balanceAmountWhole);
        balanceAmountFraction = view.findViewById(R.id.balanceAmountFraction);
        dailyInputButton = view.findViewById(R.id.dailyInputButton);
        leftArrow = view.findViewById(R.id.leftArrow);
        rightArrow = view.findViewById(R.id.rightArrow);
        vTodaySpent = view.findViewById(R.id.todaySpent);
        vMonthStartHoldings = view.findViewById(R.id.monthStartHoldings);
        vTotalSpent = view.findViewById(R.id.totalSpent);
        vPercentageOfLastMonth = view.findViewById(R.id.percentageOfLastMonth);
        todaySpentLayout = view.findViewById(R.id.todaySpentLayout);
        progressFill = view.findViewById(R.id.progressBarFill);
        progressContainer = view.findViewById(R.id.progressContainer);
        vAvgSpentLayout = view.findViewById(R.id.avgSpent);
        vLoanBalanceLayout = view.findViewById(R.id.loanBalance);
        vTotalSpentOfMonth = view.findViewById(R.id.totalSpentOfTheMonth);
        vInvestmentsLayout = view.findViewById(R.id.investments);
        vSettings = view.findViewById(R.id.settings);
        vTotalCreditsOfMonth = view.findViewById(R.id.totalCreditsOfMonth);

        RelativeLayout btnDetailedStats = view.findViewById(R.id.btnDetailedStats);
        btnDetailedStats.setOnClickListener(v -> {
            Intent statsIntent = new Intent(requireContext(), Statistics_Activity.class);
            statsIntent.putExtra("selectedYear", selectedDate.getYear());
            statsIntent.putExtra("selectedMonth", selectedDate.getMonthValue());
            startActivity(statsIntent);
        });

        vSettings.setOnClickListener(v -> {
            Intent settings = new Intent(requireContext(), Settings_Activity.class);
            startActivity(settings);
        });

        calendarRecyclerView = view.findViewById(R.id.calendarRecycyleView);
        vMonthText = view.findViewById(R.id.month);
        selectedDate = LocalDate.now();

        setMonthView();

        date.setText(DateTimeHelper.getCurrentDate());

        double balanceLeft = sql.getBalanceLeft();
        String[] balance = String.valueOf(balanceLeft).split("\\.");
        balanceAmountWhole.setText(balance[0]);
        balanceAmountFraction.setText("." + balance[1]);

        double sumOfSpent = sql.getSumSpentCurrentMonth();
        double earliestDayHolding = sql.getHoldingsFromEarliestDateThisMonth();
        double todaySpent = sql.getTodaysSpent();
        double percentageOfChange = sql.getMonthlySpentPercentageChange();

        if (earliestDayHolding > 0) {
            spentProgress((sumOfSpent / earliestDayHolding) * 100.0);
        }

        if (!sql.isTodaysRecordAvailable()) {
            todaySpentLayout.setVisibility(View.INVISIBLE);
        }
        vTotalSpent.setText(String.format("Rs. %.2f", sumOfSpent));
        vMonthStartHoldings.setText(String.format("Rs. %.2f ", earliestDayHolding));
        vTodaySpent.setText(String.format("Rs. %.2f", todaySpent));

        if (percentageOfChange >= 0) {
            vPercentageOfLastMonth.setText(String.format("+%.2f %%", percentageOfChange));
        } else {
            vPercentageOfLastMonth.setText(String.format("%.2f %%", percentageOfChange));
        }

        dailyInputButton.setOnClickListener(v -> {
            Intent dailyPage = new Intent(requireContext(), DailyInput_Activity.class);
            dailyPage.putExtra("date", DateTimeHelper.getCurrentDate());
            startActivity(dailyPage);
        });

        leftArrow.setOnClickListener(v -> {
            selectedDate = selectedDate.minusMonths(1);
            setMonthView();
        });

        rightArrow.setOnClickListener(v -> {
            selectedDate = selectedDate.plusMonths(1);
            setMonthView();
        });
    }

    @Override
    public void onItemClick(int position, String dayText) {
        if (!dayText.equals("")) {
            int day = Integer.parseInt(dayText);
            LocalDate clickedDate = selectedDate.withDayOfMonth(day);
            String formattedDate = DateTimeHelper.formatToDisplayDate(clickedDate);

            Intent intent = new Intent(requireContext(), DayDataShow_Activity.class);
            intent.putExtra("date", formattedDate);
            startActivity(intent);
        }
    }

    private void setMonthView() {
        datesWithData = sql.getAllRecordedDates(
                DateTimeHelper.getCurrentMonthLocalDate(selectedDate),
                DateTimeHelper.getCurrentYearLocalDate(selectedDate));
        double averageSpent = sql.getAverageSpentForMonth(selectedDate);
        float[] dailySpends = sql.getMonthlyDailySpends(selectedDate);

        vMonthText.setText(DateTimeHelper.monthYearFromDate(selectedDate));
        ArrayList<String> daysInMonth = DateTimeHelper.daysInMonthArray(selectedDate);
        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, this, datesWithData, dailySpends, averageSpent);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireContext(), 7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);

        TextView temp;

        temp = vAvgSpentLayout.findViewById(R.id.label);
        temp.setText("Average Spent");
        temp = vAvgSpentLayout.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f", sql.getAverageSpentForMonth(selectedDate)));

        temp = vInvestmentsLayout.findViewById(R.id.label);
        temp.setText("Total Investments");
        temp = vInvestmentsLayout.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f", sql.getTotalInvestmentsForMonth(selectedDate)));

        temp = vLoanBalanceLayout.findViewById(R.id.label);
        temp.setText("Total Balance Loan");
        temp = vLoanBalanceLayout.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f", sql.getLastLoanForMonth(selectedDate)));

        temp = vTotalSpentOfMonth.findViewById(R.id.label);
        temp.setText("Total Spent");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH);
        temp = vTotalSpentOfMonth.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f", sql.getMonthlyTotalSpent(selectedDate.format(formatter))));

        temp = vTotalCreditsOfMonth.findViewById(R.id.label);
        temp.setText("Total Credits");
        temp = vTotalCreditsOfMonth.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f", sql.getTotalCreditsForMonth(selectedDate)));
    }

    private void spentProgress(double percentage) {
        progressContainer.post(() -> {
            int fullWidth = progressContainer.getWidth();
            int progressWidth = (int) (fullWidth * (Math.min(percentage, 100.0) / 100.0));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) progressFill.getLayoutParams();
            params.width = progressWidth;
            progressFill.setLayoutParams(params);
        });
    }
}
