package dev.roxs.moneytracker.page;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import dev.roxs.moneytracker.MainActivity;
import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.DateTimeHelper;
import dev.roxs.moneytracker.helper.SQl_Helper;

public class DailyInput_Activity extends AppCompatActivity {

    private TextView date, buttonText, vYesterdayHoldings;
    private TextInputEditText softMoney, hardMoney, vInvestments, vCredits, vloan, vReview;
    private RelativeLayout saveButton;
    private double soft,hard, spent, holdings, loan,investments, credits;
    private SQl_Helper sql;
    private String remarks;
    private SQl_Helper.DB_STRUCT yesterdaysData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_daily_input);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String formatted = getIntent().getStringExtra("date");
//        LocalDate dateLocalDate= LocalDate.parse();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
//        LocalDate dateLocalDate = LocalDate.parse(clickedDate, formatter);
//        dateLocalDate = dateLocalDate.minusDays(1); // Fix here
//        String formatted = DateTimeHelper.formatToDisplayDate(dateLocalDate);


        //Database Init
        sql = new SQl_Helper(getApplicationContext());
        yesterdaysData = sql.getYesterdaysHoldings(formatted);
        Log.d("UT", "onCreate: "+yesterdaysData);
        if(yesterdaysData==null){
            showYesterdaysSpentDialog(sql,formatted);
        }
//        sql.clearDatabase(); // TODO: delete this once the testing is done

        //Referencing
        date = findViewById(R.id.date);
        vYesterdayHoldings = findViewById(R.id.yesterdaysHoldings);
        saveButton = findViewById(R.id.dailyInputButton);
        buttonText = saveButton.findViewById(R.id.label);
        softMoney = findViewById(R.id.softMoneyInput);
        hardMoney = findViewById(R.id.hardMoneyInput);
        vInvestments = findViewById(R.id.investmentsInput);
        vCredits = findViewById(R.id.creditsInput);
        vloan = findViewById(R.id.loanInput);
        vReview = findViewById(R.id.remarks);

        //date setting
        date.setText(formatted);
        buttonText.setText("Save Amount");
        if(yesterdaysData!=null){
            vYesterdayHoldings.setText(String.valueOf(yesterdaysData.holdings));
        }



        SQl_Helper.DB_STRUCT data = sql.getEntryByDate(formatted);

        if (data != null){
            setDataInViews(data, formatted);
        }else if(yesterdaysData !=null){

            setDataInViews(yesterdaysData, formatted);
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double soft = Double.parseDouble(Objects.requireNonNull(softMoney.getText()).toString());
                    double hard = Double.parseDouble(Objects.requireNonNull(hardMoney.getText()).toString());
                    double investments = Double.parseDouble(Objects.requireNonNull(vInvestments.getText()).toString());
                    double credits = Double.parseDouble(Objects.requireNonNull(vCredits.getText()).toString());
                    double loan = Double.parseDouble(Objects.requireNonNull(vloan.getText()).toString());
                    String remarks = Objects.requireNonNull(vReview.getText()).toString();


                    holdings = soft + hard + loan;

                    spent = yesterdaysData.holdings - (holdings - investments - credits);

                    Log.d("UT", "onClick: y:"+yesterdaysData.holdings+"h:"+holdings+"s:"+spent);

                    // Use DateTimeHelper
                    String date = formatted;
                    String day = DateTimeHelper.getDayOfWeek(formatted);

                    sql.insertOrUpdateEntry(date, day, soft, hard, investments, credits, loan, remarks, holdings, spent);

                    Toast.makeText(getApplicationContext(), "Entry saved successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
                }
            }
        });



    }
    private void setDataInViews(SQl_Helper.DB_STRUCT data, String formatted){

        date.setText(formatted);
//        spent.setText("Spent: ₹" + data.spent);
        softMoney.setText(String.valueOf(data.softCash));
        hardMoney.setText(String.valueOf(data.hardCash));
        vInvestments.setText("" + data.investments);
//        vHolding.setText("Holdings: ₹" + data.holdings);
        vCredits.setText("" + data.credits);
        vloan.setText("" + data.loan);
        vReview.setText("" + data.remarks);
    }
    private void showYesterdaysSpentDialog(SQl_Helper sqlHelper, String formatted) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Oops! It seems I have no Data. Please provide Yesterday's Holdings");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String spentStr = input.getText().toString().trim();
            if (!spentStr.isEmpty()) {
                Double yesterdaysHoldings = Double.parseDouble(spentStr);

                // Use the formatted string from the activity (the selected date)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
                LocalDate selectedDate = LocalDate.parse(formatted, formatter);
                LocalDate yesterday = selectedDate.minusDays(1);
                String yesterdayDateStr = yesterday.format(formatter);

                // Day of the week
                String day = DateTimeHelper.getDayName(yesterday); // Use your helper if it supports LocalDate

                // Default placeholder values
                double softcash = 0;
                double hardcash = 0;
                double investments = 0;
                double credit = 0;
                double loan = 0;
                String remarks = "Auto entry (only spent provided)";

                // Calculate holdings from spent if you need, or use it directly
                double holdings = yesterdaysHoldings;

                // Insert into table
                ContentValues values = new ContentValues();
                values.put(SQl_Helper.COL_DATE, yesterdayDateStr);
                values.put(SQl_Helper.COL_DAY, day);
                values.put(SQl_Helper.COL_SOFTCASH, softcash);
                values.put(SQl_Helper.COL_HARDCASH, hardcash);
                values.put(SQl_Helper.COL_INVESTMENTS, investments);
                values.put(SQl_Helper.COL_HOLDINGS, holdings);
                values.put(SQl_Helper.COL_CREDIT, credit);
                values.put(SQl_Helper.COL_LOAN, loan);
                values.put(SQl_Helper.COL_SPENT, spent);
                values.put(SQl_Helper.COL_REMARKS, remarks);

                SQLiteDatabase db = sqlHelper.getWritableDatabase();
                db.insertWithOnConflict(SQl_Helper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                db.close();

                vYesterdayHoldings.setText(String.valueOf(yesterdaysHoldings));
                Toast.makeText(this, "Yesterday's entry added", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


}