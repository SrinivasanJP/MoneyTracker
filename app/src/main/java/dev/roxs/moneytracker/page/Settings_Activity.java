package dev.roxs.moneytracker.page;

import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import dev.roxs.moneytracker.BuildConfig;
import dev.roxs.moneytracker.R;
import dev.roxs.moneytracker.helper.DateTimeHelper;
import dev.roxs.moneytracker.helper.Notification_Helper;
import dev.roxs.moneytracker.helper.SQl_Helper;

public class Settings_Activity extends AppCompatActivity {

    private RelativeLayout vExportButton, vImportButton, vNotificationTimeRow, vShareAppRow, vClearDataRow;
    private TextView vExportLabel, vImportLabel, vNotificationTimeValue, vVersionValue;

    private ActivityResultLauncher<Intent> importFileLauncher;

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

        // Register file picker launcher for import
        importFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            importFromExcel(fileUri);
                        }
                    }
                }
        );

        initViews();
        setupListeners();
    }

    private void initViews() {
        vExportButton = findViewById(R.id.exportData);
        vExportLabel = vExportButton.findViewById(R.id.label);
        vExportLabel.setText(getString(R.string.export_data));

        vImportButton = findViewById(R.id.importData);
        vImportLabel = vImportButton.findViewById(R.id.label);
        vImportLabel.setText(getString(R.string.import_data));

        vNotificationTimeRow = findViewById(R.id.notificationTimeRow);
        vNotificationTimeValue = findViewById(R.id.notificationTimeValue);

        vShareAppRow = findViewById(R.id.shareAppRow);
        vClearDataRow = findViewById(R.id.clearDataRow);
        vVersionValue = findViewById(R.id.versionValue);

        // Set initial values
        vNotificationTimeValue.setText(Notification_Helper.getFormattedTime(this));
        vVersionValue.setText("v" + BuildConfig.VERSION_NAME);
    }

    private void setupListeners() {
        // Export
        vExportButton.setOnClickListener(v -> exportToExcel());

        // Import
        vImportButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/vnd.ms-excel");
            importFileLauncher.launch(intent);
        });

        // Notification time picker
        vNotificationTimeRow.setOnClickListener(v -> {
            int currentHour = Notification_Helper.getNotificationHour(this);
            int currentMinute = Notification_Helper.getNotificationMinute(this);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        Notification_Helper.saveNotificationTime(this, hourOfDay, minute);
                        Notification_Helper.scheduleDailyWork(this);
                        vNotificationTimeValue.setText(Notification_Helper.getFormattedTime(this));
                        Toast.makeText(this,
                                "Reminder set for " + Notification_Helper.getFormattedTime(this),
                                Toast.LENGTH_SHORT).show();
                    },
                    currentHour, currentMinute, false);
            timePickerDialog.setTitle("Set Daily Reminder Time");
            timePickerDialog.show();
        });

        // Share app
        vShareAppRow.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message));
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // Clear all data
        vClearDataRow.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.clear_all_data))
                    .setMessage(getString(R.string.clear_data_warning))
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        SQl_Helper dbHelper = new SQl_Helper(this);
                        dbHelper.clearDatabase();
                        Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ========== EXPORT with datetime filename ==========

    private void exportToExcel() {
        SQl_Helper dbHelper = new SQl_Helper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + SQl_Helper.TABLE_NAME + " ORDER BY " + SQl_Helper.COL_DATE + " asc", null);

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
            // Datetime-named file
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH).format(new Date());
            String filename = "MoneyData_" + timestamp + ".xls";

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

    // ========== IMPORT from XLS ==========

    private void importFromExcel(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
                return;
            }

            Workbook workbook = new HSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                Toast.makeText(this, "No data sheet found in file", Toast.LENGTH_SHORT).show();
                inputStream.close();
                return;
            }

            // Read header row to find column indices
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                Toast.makeText(this, "Empty file or invalid format", Toast.LENGTH_SHORT).show();
                inputStream.close();
                return;
            }

            int colDate = -1, colDay = -1, colSpent = -1, colSoftcash = -1, colHardcash = -1;
            int colInvestments = -1, colHoldings = -1, colCredit = -1, colLoan = -1, colRemarks = -1;

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                String header = cell.getStringCellValue().trim().toLowerCase();
                switch (header) {
                    case "date": colDate = i; break;
                    case "day": colDay = i; break;
                    case "spent": colSpent = i; break;
                    case "softcash": colSoftcash = i; break;
                    case "hardcash": colHardcash = i; break;
                    case "investments": colInvestments = i; break;
                    case "holdings": colHoldings = i; break;
                    case "credit": colCredit = i; break;
                    case "friendly_loan": colLoan = i; break;
                    case "remarks": colRemarks = i; break;
                }
            }

            if (colDate == -1) {
                Toast.makeText(this, "Invalid file: missing 'date' column", Toast.LENGTH_SHORT).show();
                inputStream.close();
                return;
            }

            SQl_Helper dbHelper = new SQl_Helper(this);
            int importedCount = 0;

            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next(); // skip header

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    String date = getCellStringValue(row.getCell(colDate));
                    if (date == null || date.isEmpty()) continue;

                    String day = colDay >= 0 ? getCellStringValue(row.getCell(colDay)) : DateTimeHelper.getDayOfWeek(date);
                    double spent = colSpent >= 0 ? getCellDoubleValue(row.getCell(colSpent)) : 0;
                    double softcash = colSoftcash >= 0 ? getCellDoubleValue(row.getCell(colSoftcash)) : 0;
                    double hardcash = colHardcash >= 0 ? getCellDoubleValue(row.getCell(colHardcash)) : 0;
                    double investments = colInvestments >= 0 ? getCellDoubleValue(row.getCell(colInvestments)) : 0;
                    double holdings = colHoldings >= 0 ? getCellDoubleValue(row.getCell(colHoldings)) : 0;
                    double credit = colCredit >= 0 ? getCellDoubleValue(row.getCell(colCredit)) : 0;
                    double loan = colLoan >= 0 ? getCellDoubleValue(row.getCell(colLoan)) : 0;
                    String remarks = colRemarks >= 0 ? getCellStringValue(row.getCell(colRemarks)) : "";

                    if (day == null) day = "";
                    if (remarks == null) remarks = "";

                    dbHelper.insertOrUpdateEntry(date, day, softcash, hardcash, investments, credit, loan, remarks, holdings, spent);
                    importedCount++;
                } catch (Exception e) {
                    // Skip malformed rows
                    e.printStackTrace();
                }
            }

            workbook.close();
            inputStream.close();

            Toast.makeText(this, "Successfully imported " + importedCount + " records", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private double getCellDoubleValue(Cell cell) {
        if (cell == null) return 0;
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }
}
