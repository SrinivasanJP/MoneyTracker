package dev.roxs.moneytracker.page;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.DateTimeHelper;

public class DailyInput_Activity extends AppCompatActivity {

    private TextView date, buttonText;
    private TextInputEditText softMoney, hardMoney;
    private RelativeLayout saveButton;
    private float soft,hard;

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
                soft = Float.parseFloat(softMoney.getText().toString());
                hard = Float.parseFloat(hardMoney.getText().toString());
                Log.d("UT", "onClick: s:"+ soft+" h:"+hard);
            }
        });


    }
}