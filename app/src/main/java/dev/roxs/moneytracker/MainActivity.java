package dev.roxs.moneytracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import dev.roxs.moneytracker.Adapter.CalendarAdapter;
import dev.roxs.moneytracker.helper.DateTimeHelper;
import dev.roxs.moneytracker.helper.Notification_Helper;
import dev.roxs.moneytracker.helper.SQl_Helper;
import dev.roxs.moneytracker.page.DailyInput_Activity;
import dev.roxs.moneytracker.page.DayDataShow_Activity;
import dev.roxs.moneytracker.page.Settings_Activity;
import dev.roxs.moneytracker.page.Statistics_Activity;

import android.Manifest;

public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Notification_Helper.scheduleDailyWork(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        sql = new SQl_Helper(getApplicationContext());

        // Referencing existing views
        date = findViewById(R.id.date);
        balanceAmountWhole = findViewById(R.id.balanceAmountWhole);
        balanceAmountFraction = findViewById(R.id.balanceAmountFraction);
        dailyInputButton = findViewById(R.id.dailyInputButton);
        leftArrow = findViewById(R.id.leftArrow);
        rightArrow = findViewById(R.id.rightArrow);
        vTodaySpent = findViewById(R.id.todaySpent);
        vMonthStartHoldings = findViewById(R.id.monthStartHoldings);
        vTotalSpent = findViewById(R.id.totalSpent);
        vPercentageOfLastMonth = findViewById(R.id.percentageOfLastMonth);
        todaySpentLayout = findViewById(R.id.todaySpentLayout);
        progressFill = findViewById(R.id.progressBarFill);
        progressContainer = findViewById(R.id.progressContainer);
        vAvgSpentLayout = findViewById(R.id.avgSpent);
        vLoanBalanceLayout = findViewById(R.id.loanBalance);
        vTotalSpentOfMonth = findViewById(R.id.totalSpentOfTheMonth);
        vInvestmentsLayout = findViewById(R.id.investments);
        vSettings = findViewById(R.id.settings);

        // New stat views
        vTotalCreditsOfMonth = findViewById(R.id.totalCreditsOfMonth);
        
        RelativeLayout btnDetailedStats = findViewById(R.id.btnDetailedStats);
        btnDetailedStats.setOnClickListener(v -> {
            Intent statsIntent = new Intent(getApplicationContext(), Statistics_Activity.class);
            statsIntent.putExtra("selectedYear", selectedDate.getYear());
            statsIntent.putExtra("selectedMonth", selectedDate.getMonthValue());
            startActivity(statsIntent);
        });

        vSettings.setOnClickListener(v -> {
            Intent settings = new Intent(getApplicationContext(), Settings_Activity.class);
            startActivity(settings);
        });

        calendarRecyclerView = findViewById(R.id.calendarRecycyleView);
        vMonthText = findViewById(R.id.month);
        selectedDate = LocalDate.now();

        setMonthView();

        // Date setting
        date.setText(DateTimeHelper.getCurrentDate());

        // Balance display
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

        // Lifetime stats (don't change with month navigation)
        updateLifetimeStats();

        // Button actions
        dailyInputButton.setOnClickListener(v -> {
            Intent dailyPage = new Intent(MainActivity.this, DailyInput_Activity.class);
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

            Intent intent = new Intent(getApplicationContext(), DayDataShow_Activity.class);
            intent.putExtra("date", formattedDate);
            startActivity(intent);
        }
    }

    private void setMonthView() {
        datesWithData = sql.getAllRecordedDates(
                DateTimeHelper.getCurrentMonthLocalDate(selectedDate),
                DateTimeHelper.getCurrentYearLocalDate(selectedDate));
        vMonthText.setText(DateTimeHelper.monthYearFromDate(selectedDate));
        ArrayList<String> daysInMonth = DateTimeHelper.daysInMonthArray(selectedDate);
        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, this, datesWithData);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);

        // Monthly summary cards
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

        // Total Credits
        temp = vTotalCreditsOfMonth.findViewById(R.id.label);
        temp.setText("Total Credits");
        temp = vTotalCreditsOfMonth.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f", sql.getTotalCreditsForMonth(selectedDate)));
    }

    private void updateLifetimeStats() {
        // Implementation moved to Statistics_Activity
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