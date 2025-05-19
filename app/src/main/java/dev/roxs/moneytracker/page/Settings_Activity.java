package dev.roxs.moneytracker.page;

import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.roxs.moneytracker.R;

public class Settings extends AppCompatActivity {

    RelativeLayout vExportButton;
    TextView vExportLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        vExportLabel.setText("Export Spent Data");


    }
    private void InitViews(){
        vExportButton = findViewById(R.id.exportData);
        vExportLabel = vExportButton.findViewById(R.id.label);
    }
}