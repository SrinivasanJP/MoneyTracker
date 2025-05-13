package dev.roxs.moneytracker.page;

import android.content.ContentValues;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.DateTimeHelper;
import dev.roxs.moneytracker.helper.SQl_Helper;

public class DailyInput_Activity extends AppCompatActivity {

    private TextView date, buttonText;
    private TextInputEditText softMoney, hardMoney;
    private RelativeLayout saveButton;
    private double soft,hard, spent,yesterdaysHoldings, holdings, loan,investments, credits;
    private SQl_Helper sql;
    private String remarks;


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
        //Database Init
        sql = new SQl_Helper(getApplicationContext());
        yesterdaysHoldings = sql.getYesterdaysHoldings();
        if(yesterdaysHoldings==-1){
            showYesterdaysSpentDialog(sql);
        }



        //Referencing
        date = findViewById(R.id.date);
        saveButton = findViewById(R.id.dailyInputButton);
        buttonText = saveButton.findViewById(R.id.label);
        softMoney = findViewById(R.id.softMoneyInput);
        hardMoney = findViewById(R.id.hardMoneyInput);

        //date setting
        date.setText(DateTimeHelper.getCurrentDate());
        buttonText.setText("Save Amount");


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double soft = Double.parseDouble(softMoney.getText().toString());
                    double hard = Double.parseDouble(hardMoney.getText().toString());
//                    double investments = Double.parseDouble(investmentInput.getText().toString());
//                    double credits = Double.parseDouble(creditInput.getText().toString());
//                    double loan = Double.parseDouble(loanInput.getText().toString());
//                    String remarks = remarksInput.getText().toString();

                    investments = 0;
                    credits = 0;
                    loan = 0;
                    remarks ="No Remarks";
                    holdings = soft + hard + loan;
                    spent = yesterdaysHoldings - (holdings - investments - credits);

                    // Use DateTimeHelper
                    String date = DateTimeHelper.getCurrentDate();
                    String day = DateTimeHelper.getCurrentDayName();

                    sql.insertOrUpdateEntry(date, day, soft, hard, investments, credits, loan, remarks);

                    Toast.makeText(getApplicationContext(), "Entry saved successfully", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
                }
            }
        });



    }
    private void showYesterdaysSpentDialog(SQl_Helper sqlHelper) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Oops! It seems I have no Data give ablest Yesterday's Holdings");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String spentStr = input.getText().toString().trim();
            if (!spentStr.isEmpty()) {
                yesterdaysHoldings = Double.parseDouble(spentStr);

                // Get yesterday's date
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                String date = sdf.format(calendar.getTime());

                // Day of the week
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                String day = dayFormat.format(calendar.getTime());

                // Ask user only for spent, then you estimate softcash, hardcash, investments, credit, loan as 0
                double softcash = 0;
                double hardcash = 0;
                double investments = 0;
                double credit = 0;
                double loan = 0;
                String remarks = "Auto entry (only spent provided)";

                // Insert into table
                ContentValues values = new ContentValues();
                values.put(SQl_Helper.COL_DATE, date);
                values.put(SQl_Helper.COL_DAY, day);
                values.put(SQl_Helper.COL_SOFTCASH, softcash);
                values.put(SQl_Helper.COL_HARDCASH, hardcash);
                values.put(SQl_Helper.COL_INVESTMENTS, investments);
                values.put(SQl_Helper.COL_HOLDINGS, yesterdaysHoldings);
                values.put(SQl_Helper.COL_CREDIT, credit);
                values.put(SQl_Helper.COL_LOAN, loan);
                values.put(SQl_Helper.COL_SPENT, spent);
                values.put(SQl_Helper.COL_REMARKS, remarks);

                SQLiteDatabase db = sqlHelper.getWritableDatabase();
                db.insertWithOnConflict(SQl_Helper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                db.close();

                Toast.makeText(this, "Yesterday's entry added", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

}