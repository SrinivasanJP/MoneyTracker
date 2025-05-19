package dev.roxs.moneytracker.page;
import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.SQl_Helper;

public class Settings_Activity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

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

        initViews();

        vExportLabel.setText("Export Spent Data");
        vExportButton.setOnClickListener(v -> {
                exportToExcel();
        });
    }

    private void initViews() {
        vExportButton = findViewById(R.id.exportData);
        vExportLabel = vExportButton.findViewById(R.id.label);
    }



    private void exportToExcel() {
        SQl_Helper dbHelper = new SQl_Helper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + SQl_Helper.TABLE_NAME +" ORDER BY "+ SQl_Helper.COL_DATE+" asc", null);

        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Money Data");

        if (cursor != null) {
            Row header = sheet.createRow(0);
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cursor.getColumnName(i));
            }

            int rowIndex = 1;
            while (cursor.moveToNext()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    Cell cell = row.createCell(i);
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_STRING:
                            cell.setCellValue(cursor.getString(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                        case Cursor.FIELD_TYPE_INTEGER:
                            cell.setCellValue(cursor.getDouble(i));
                            break;
                        default:
                            cell.setCellValue("");
                            break;
                    }
                }
            }
            cursor.close();
        }

        try {
            String filename = "MoneyData.xls";
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MoneyTracker");

            ContentResolver resolver = getContentResolver();
            Uri fileUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

            if (fileUri != null) {
                OutputStream os = resolver.openOutputStream(fileUri);
                workbook.write(os);
                os.close();
                workbook.close();
                Toast.makeText(this, "Exported to Documents/MoneyTracker/" + filename, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to create file", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
