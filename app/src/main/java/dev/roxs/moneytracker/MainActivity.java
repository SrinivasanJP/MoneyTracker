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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;

import dev.roxs.moneytracker.Adapter.CalendarAdapter;
import dev.roxs.moneytracker.helper.DateTimeHelper;
import dev.roxs.moneytracker.helper.Notification_Helper;
import dev.roxs.moneytracker.helper.SQl_Helper;
import dev.roxs.moneytracker.page.DailyInput_Activity;
import dev.roxs.moneytracker.page.DayDataShow_Activity;
import dev.roxs.moneytracker.page.Settings_Activity;

import android.Manifest;
public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener {

    private RelativeLayout dailyInputButton, todaySpentLayout,progressFill,progressContainer;
    private TextView date, balanceAmountWhole, balanceAmountFraction, vTotalSpent, vMonthStartHoldings,vPercentageOfLastMonth,vTodaySpent;
    private SQl_Helper sql;

    private RelativeLayout vAvgSpentLayout, vInvestmentsLayout, vLoanBalanceLayout;

    private TextView vMonthText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;

    private ImageView leftArrow,rightArrow, vSettings;
    ArrayList<String> datesWithData;


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




        //referencing
        date = findViewById(R.id.date);
        balanceAmountWhole = findViewById(R.id.balanceAmountWhole);
        balanceAmountFraction = findViewById(R.id.balanceAmountFraction);
        dailyInputButton = findViewById(R.id.dailyInputButton);
        leftArrow =findViewById(R.id.leftArrow);
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
        vInvestmentsLayout = findViewById(R.id.investments);
        vSettings =findViewById(R.id.settings);

        vSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settings = new Intent(getApplicationContext(), Settings_Activity.class);
                startActivity(settings);
            }
        });

        calendarRecyclerView = findViewById(R.id.calendarRecycyleView);
        vMonthText = findViewById(R.id.month);
        selectedDate = LocalDate.now();



        setMonthView();


        //Date setting
        date.setText(DateTimeHelper.getCurrentDate());
        //TODO: Left amount display
        String[] balance = String.valueOf(sql.getBalanceLeft()).split("\\.");
        balanceAmountWhole.setText(balance[0]);
        balanceAmountFraction.setText("."+balance[1]);




        double sumOfSpent = sql.getSumSpentCurrentMonth();
        double earliestDayHolding = sql.getHoldingsFromEarliestDateThisMonth();
        double todaySpent = (sql.getTodaysSpent());
        double percentageOfChange = sql.getMonthlySpentPercentageChange();

        if (earliestDayHolding > 0) {
            spentProgress((sumOfSpent / earliestDayHolding) * 100.0);
        }

        if(!sql.isTodaysRecordAvailable()){
            todaySpentLayout.setVisibility(View.INVISIBLE);
        }
        vTotalSpent.setText(""+sumOfSpent);
        vMonthStartHoldings.setText(""+earliestDayHolding+" ");
        vTodaySpent.setText(""+todaySpent);

        if(percentageOfChange>=0){
            vPercentageOfLastMonth.setText("+"+percentageOfChange+"%");
        }else{
            vPercentageOfLastMonth.setText("-"+percentageOfChange+"%");
        }



        //Button actions
        dailyInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dailyPage = new Intent(MainActivity.this, DailyInput_Activity.class);
                dailyPage.putExtra("date",DateTimeHelper.getCurrentDate());
                startActivity(dailyPage);
            }
        });

        leftArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDate = selectedDate.minusMonths(1);
                setMonthView();
            }
        });

        rightArrow.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDate = selectedDate.plusMonths(1);
                setMonthView();
            }
        }));
    }



    @Override
    public void onItemClick(int position, String dayText) {
        if (!dayText.equals("")) {
            int day = Integer.parseInt(dayText);
            LocalDate clickedDate = selectedDate.withDayOfMonth(day);

            // Format to "dd-MMM-yyyy"
            String formattedDate = DateTimeHelper.formatToDisplayDate(clickedDate);  // Helper method you can add

            Intent intent = new Intent(getApplicationContext(), DayDataShow_Activity.class);
            intent.putExtra("date",formattedDate);
            startActivity(intent);

        }
    }
    private void setMonthView(){
        datesWithData = sql.getAllRecordedDates(DateTimeHelper.getCurrentMonthLocalDate(selectedDate),DateTimeHelper.getCurrentYearLocalDate(selectedDate));
        vMonthText.setText(DateTimeHelper.monthYearFromDate(selectedDate));
        ArrayList<String> daysInMonth = DateTimeHelper.daysInMonthArray(selectedDate);
        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, this,datesWithData);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);

        TextView temp = vAvgSpentLayout.findViewById(R.id.label);
        temp.setText("Average spent on "+ DateTimeHelper.monthYearFromDate(selectedDate));
        temp = vAvgSpentLayout.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f",sql.getAverageSpentForMonth(selectedDate)));
        temp = vInvestmentsLayout.findViewById(R.id.label);
        temp.setText("Total Investments on "+DateTimeHelper.monthYearFromDate(selectedDate));
        temp = vInvestmentsLayout.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f",sql.getTotalInvestmentsForMonth(selectedDate)));
        temp = vLoanBalanceLayout.findViewById(R.id.label);
        temp.setText("Total Balance Loan");
        temp = vLoanBalanceLayout.findViewById(R.id.amount);
        temp.setText(String.format("Rs. %.2f",sql.getLastLoanForMonth(selectedDate)));



    }

    private void spentProgress(double percentage){
        // Get the full width after layout pass
        progressContainer.post(() -> {
            int fullWidth = progressContainer.getWidth();
            int progressWidth = (int) (fullWidth * (percentage / 100.0));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) progressFill.getLayoutParams();
            params.width = progressWidth;
            progressFill.setLayoutParams(params);
        });

    }

}