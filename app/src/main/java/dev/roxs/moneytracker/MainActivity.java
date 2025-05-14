package dev.roxs.moneytracker;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;

import dev.roxs.moneytracker.Adapter.CalendarAdapter;
import dev.roxs.moneytracker.helper.DateTimeHelper;
import dev.roxs.moneytracker.helper.SQl_Helper;
import dev.roxs.moneytracker.page.DailyInput_Activity;
import dev.roxs.moneytracker.page.DayDataShow_Activity;

public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener {

    private RelativeLayout dailyInputButton;
    private TextView date, balanceAmountWhole, balanceAmountFraction;
    private SQl_Helper sql;

    private TextView vMonthText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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

        sql = new SQl_Helper(getApplicationContext());

        ArrayList<String> datesWithData = sql.getAllRecordedDates(DateTimeHelper.getCurrentMonthLocalDate(),DateTimeHelper.getCurrentYearLocalDate());


        Log.d("UT", "onCreate: dateswithdata: "+datesWithData.get(0));
        //referencing
        date = findViewById(R.id.date);
        balanceAmountWhole = findViewById(R.id.balanceAmountWhole);
        balanceAmountFraction = findViewById(R.id.balanceAmountFraction);
        dailyInputButton = findViewById(R.id.dailyInputButton);

        calendarRecyclerView = findViewById(R.id.calendarRecycyleView);
        vMonthText = findViewById(R.id.month);
        selectedDate = LocalDate.now();

        vMonthText.setText(DateTimeHelper.getCurrentMonth());
        ArrayList<String> daysInMonth = daysInMonthArray(selectedDate);
        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth, this,datesWithData);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);



        //Date setting
        date.setText(DateTimeHelper.getCurrentDate());
        //TODO: Left amount display
        String[] balance = String.valueOf(sql.getBalanceLeft()).split("\\.");
        balanceAmountWhole.setText(balance[0]);
        balanceAmountFraction.setText("."+balance[1]);

        //Button actions
        dailyInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dailyPage = new Intent(MainActivity.this, DailyInput_Activity.class);
                startActivity(dailyPage);
            }
        });




    }

    private ArrayList<String> daysInMonthArray(LocalDate date){
        ArrayList<String> daysInMonthArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);
        int daysInMonth = yearMonth.lengthOfMonth();

        LocalDate firstOfMonth = date.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1=Monday ... 7=Sunday

        int firstDayIndex = dayOfWeek % 7; // Convert to 0-based index (0=Sunday, 6=Saturday)

        for (int i = 0; i < firstDayIndex; i++) {
            daysInMonthArray.add("");
        }

        for (int day = 1; day <= daysInMonth; day++) {
            daysInMonthArray.add(String.valueOf(day));
        }

        // Pad the end of the list to make a complete 6-week grid (42 cells)
        while (daysInMonthArray.size() < 42) {
            daysInMonthArray.add("");
        }

        return daysInMonthArray;
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
}