package dev.roxs.moneytracker.page;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
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
    private TextView tvDate, tvSpent, tvSoftCash, tvHardCash, tvInvestments, tvHoldings, tvCredit, tvLoan, tvRemarks,vBtnLable;
    private RelativeLayout vModifyOrAddBtn, vDataShowLayout;


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
            vBtnLable.setText("Modify Spent Data");
            tvDate.setText(""+ formattedDate);
            tvSpent.setText("₹ " + data.spent);
            tvSoftCash.setText("₹ " + data.softCash);
            tvHardCash.setText("₹ " + data.hardCash);
            tvInvestments.setText("₹ " + data.investments);
            tvHoldings.setText("₹ " + data.holdings);
            tvCredit.setText("₹ " + data.credits);
            tvLoan.setText("₹ " + data.loan);
            tvRemarks.setText("Remarks: " + data.remarks);

        } else {
            vDataShowLayout.setVisibility(View.INVISIBLE);
            vBtnLable.setText("Add Spent Data");
            tvDate.setText("No data found for: " + formattedDate);
        }

        vModifyOrAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DailyInput_Activity.class);
                intent.putExtra("date", formattedDate);
                startActivity(intent);
            }
        });
    }
    private void initViewsById(){
        // Link Views
        tvDate = findViewById(R.id.tv_date);
        tvSpent = findViewById(R.id.tv_spent);
        tvSoftCash = findViewById(R.id.tv_softcash);
        tvHardCash = findViewById(R.id.tv_hardcash);
        tvInvestments = findViewById(R.id.tv_investments);
        tvHoldings = findViewById(R.id.tv_holdings);
        tvCredit = findViewById(R.id.tv_credits);
        tvLoan = findViewById(R.id.tv_loan);
        tvRemarks = findViewById(R.id.tv_remarks);
        vModifyOrAddBtn = findViewById(R.id.modifyOrAddBtn);
        vBtnLable = vModifyOrAddBtn.findViewById(R.id.label);
        vDataShowLayout = findViewById(R.id.dataShowLayout);
    }
}