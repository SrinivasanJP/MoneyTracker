package dev.roxs.moneytracker.page;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.SQl_Helper;

public class DayDataShow_Activity extends AppCompatActivity {

    private SQl_Helper sql;
    private String formattedDate;
    private TextView tvDate, tvSpent, tvSoftCash, tvHardCash, tvInvestments, tvHoldings, tvCredit, tvLoan, tvRemarks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_day_data_show);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sql = new SQl_Helper(this);
        formattedDate = getIntent().getStringExtra("date");
        // Fetch data from DB
        SQl_Helper.DB_STRUCT data = sql.getEntryByDate(formattedDate);  // This method we'll add next

        initViewsById();

        if (data != null) {
            tvDate.setText("Date: " + formattedDate);
            tvSpent.setText("Spent: ₹" + data.spent);
            tvSoftCash.setText("Soft Cash: ₹" + data.softCash);
            tvHardCash.setText("Hard Cash: ₹" + data.hardCash);
            tvInvestments.setText("Investments: ₹" + data.investments);
            tvHoldings.setText("Holdings: ₹" + data.holdings);
            tvCredit.setText("Credit: ₹" + data.credits);
            tvLoan.setText("Loan: ₹" + data.loan);
            tvRemarks.setText("Remarks: " + data.remarks);
        } else {
            tvDate.setText("No data found for: " + formattedDate);
        }
    }
    private void initViewsById(){
        // Link Views
        tvDate = findViewById(R.id.tv_date);
        tvSpent = findViewById(R.id.tv_spent);
        tvSoftCash = findViewById(R.id.tv_softcash);
        tvHardCash = findViewById(R.id.tv_hardcash);
        tvInvestments = findViewById(R.id.tv_investments);
        tvHoldings = findViewById(R.id.tv_holdings);
        tvCredit = findViewById(R.id.tv_credit);
        tvLoan = findViewById(R.id.tv_loan);
        tvRemarks = findViewById(R.id.tv_remarks);
    }
}