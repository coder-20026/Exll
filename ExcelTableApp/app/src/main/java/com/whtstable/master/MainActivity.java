package com.whtstable.master;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {
    
    // Premium Color Constants
    private static final String COLOR_PRIMARY = "#1A237E";
    private static final String COLOR_HEADER = "#1A237E";
    private static final String COLOR_ROW_SELECTED = "#C5CAE9";
    private static final String COLOR_ROW_DEFAULT = "#FFFFFF";
    private static final String COLOR_BORDER = "#E0E0E0";
    private static final String COLOR_TEXT_HEADER = "#FFFFFF";
    private static final String COLOR_TEXT_CELL = "#333333";
    
    private TableLayout tableLayout;
    private Button btnToggleFloating, btnSelectMonth, btnSelectYear, btnHistory, btnCopyAll, btnClearTable;
    private TextView tvCurrentDate, tvEntryCount;
    private ArrayList<TableRow> dataRows;
    private int selectedRowIndex = -1;
    private boolean floatingButtonEnabled = false;
    
    private DatabaseHelper dbHelper;
    private String currentMonth = "January";
    private String currentYear = "2026";
    private String currentDateFull = "";
    
    // Column headers - ALL CAPS with renamed columns
    private String[] columnHeaders = {
        "BANK NAME", "APPLICANT NAME", "STATUS", "REASON OF CNV", 
        "LATLONG FROM", "LATLONG TO", "AREA", "KM"
    };
    
    // Premium Column Widths
    private int[] columnWidths = {150, 300, 100, 150, 180, 180, 120, 80};
    
    // Months array
    private String[] months = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Database
        dbHelper = DatabaseHelper.getInstance(this);
        
        // Load saved month/year
        currentMonth = dbHelper.getSelectedMonth();
        currentYear = dbHelper.getSelectedYear();
        
        // Initialize views
        initializeViews();
        
        // Create premium table
        createPremiumTable();
        
        // Load saved data for current month/year
        loadTableFromDatabase();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Update UI
        updateMonthYearButtons();
    }
    
    private void initializeViews() {
        tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        btnToggleFloating = (Button) findViewById(R.id.btnToggleFloating);
        btnSelectMonth = (Button) findViewById(R.id.btnSelectMonth);
        btnSelectYear = (Button) findViewById(R.id.btnSelectYear);
        btnHistory = (Button) findViewById(R.id.btnHistory);
        btnCopyAll = (Button) findViewById(R.id.btnCopyAll);
        btnClearTable = (Button) findViewById(R.id.btnClearTable);
        tvCurrentDate = (TextView) findViewById(R.id.tvCurrentDate);
        tvEntryCount = (TextView) findViewById(R.id.tvEntryCount);
        
        dataRows = new ArrayList<TableRow>();
    }
    
    private void updateMonthYearButtons() {
        btnSelectMonth.setText(currentMonth);
        btnSelectYear.setText(currentYear);
        updateCurrentDateDisplay();
    }
    
    private void updateCurrentDateDisplay() {
        if (currentDateFull.isEmpty()) {
            tvCurrentDate.setText("No entries yet");
        } else {
            tvCurrentDate.setText(currentDateFull);
        }
        
        // Count entries for current date
        ArrayList<DatabaseHelper.EntryData> entries = dbHelper.getEntriesByMonthYear(currentMonth, currentYear);
        tvEntryCount.setText(entries.size() + " Entries");
    }
    
    private void setupButtonListeners() {
        btnToggleFloating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFloatingButton();
            }
        });
        
        btnSelectMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMonthPicker();
            }
        });
        
        btnSelectYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showYearPicker();
            }
        });
        
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistoryPage();
            }
        });
        
        btnCopyAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAllFilledRows();
            }
        });
        
        btnClearTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearTable();
            }
        });
    }
    
    private void showMonthPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SELECT MONTH");
        builder.setItems(months, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentMonth = months[which];
                dbHelper.setSelectedMonth(currentMonth);
                updateMonthYearButtons();
                loadTableFromDatabase();
            }
        });
        builder.show();
    }
    
    private void showYearPicker() {
        final String[] years = {"2024", "2025", "2026", "2027", "2028", "2029", "2030"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SELECT YEAR");
        builder.setItems(years, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentYear = years[which];
                dbHelper.setSelectedYear(currentYear);
                updateMonthYearButtons();
                loadTableFromDatabase();
            }
        });
        builder.show();
    }
    
    private void openHistoryPage() {
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra("month", currentMonth);
        intent.putExtra("year", currentYear);
        startActivity(intent);
    }
    
    private void createPremiumTable() {
        tableLayout.removeAllViews();
        dataRows.clear();
        
        // Create premium header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor(COLOR_HEADER));
        headerRow.setPadding(0, 0, 0, 0);
        
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = createPremiumHeaderCell(columnHeaders[i], columnWidths[i]);
            headerRow.addView(tv);
        }
        tableLayout.addView(headerRow);
    }
    
    private TextView createPremiumHeaderCell(String text, int width) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(12, 10, 12, 10);
        tv.setTextSize(11);
        tv.setTextColor(Color.parseColor(COLOR_TEXT_HEADER));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setWidth(width);
        tv.setMinWidth(width);
        tv.setMaxLines(2);
        tv.setSingleLine(false);
        
        return tv;
    }
    
    private void loadTableFromDatabase() {
        // Clear existing data rows
        int childCount = tableLayout.getChildCount();
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1);
        }
        dataRows.clear();
        
        // Load entries for current month/year
        ArrayList<DatabaseHelper.EntryData> entries = dbHelper.getEntriesByMonthYear(currentMonth, currentYear);
        
        if (entries.isEmpty()) {
            currentDateFull = "";
        } else {
            currentDateFull = entries.get(entries.size() - 1).dateFull;
        }
        
        for (int i = 0; i < entries.size(); i++) {
            DatabaseHelper.EntryData entry = entries.get(i);
            TableRow dataRow = createDataRow(i, entry);
            dataRows.add(dataRow);
            tableLayout.addView(dataRow);
        }
        
        updateCurrentDateDisplay();
    }
    
    private TableRow createDataRow(final int rowIndex, DatabaseHelper.EntryData entry) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.parseColor(COLOR_ROW_DEFAULT));
        row.setPadding(0, 0, 0, 0);
        row.setTag(entry.id);  // Store entry ID in tag
        
        String[] values = {
            entry.bankName,
            entry.applicantName,
            entry.status,
            entry.reasonCnv,
            entry.latlongFrom,
            entry.latlongTo,
            entry.area,
            entry.km
        };
        
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = createPremiumDataCell(values[i], columnWidths[i]);
            tv.setTag("col_" + i);
            row.addView(tv);
        }
        
        // Click to select and copy row
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRow(rowIndex);
                copyRowToClipboard(rowIndex);
            }
        });
        
        // Long press for row options
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showRowOptionsDialog(rowIndex);
                return true;
            }
        });
        
        return row;
    }
    
    private TextView createPremiumDataCell(String text, int width) {
        TextView tv = new TextView(this);
        tv.setText(text != null ? text : "");
        tv.setPadding(12, 10, 12, 10);
        tv.setTextSize(11);
        tv.setTextColor(Color.parseColor(COLOR_TEXT_CELL));
        tv.setWidth(width);
        tv.setMinWidth(width);
        tv.setMaxLines(3);
        tv.setSingleLine(false);
        
        // Premium border effect
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.parseColor(COLOR_ROW_DEFAULT));
        border.setStroke(1, Color.parseColor(COLOR_BORDER));
        tv.setBackground(border);
        
        return tv;
    }
    
    private void selectRow(int rowIndex) {
        // Deselect previous row
        if (selectedRowIndex != -1 && selectedRowIndex < dataRows.size()) {
            updateRowBackground(selectedRowIndex, COLOR_ROW_DEFAULT);
        }
        
        // Select new row
        selectedRowIndex = rowIndex;
        updateRowBackground(rowIndex, COLOR_ROW_SELECTED);
    }
    
    private void updateRowBackground(int rowIndex, String color) {
        if (rowIndex >= dataRows.size()) return;
        TableRow row = dataRows.get(rowIndex);
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = (TextView) row.findViewWithTag("col_" + i);
            if (tv != null) {
                GradientDrawable border = new GradientDrawable();
                border.setColor(Color.parseColor(color));
                border.setStroke(1, Color.parseColor(COLOR_BORDER));
                tv.setBackground(border);
            }
        }
    }
    
    private void copyRowToClipboard(int rowIndex) {
        if (rowIndex >= dataRows.size()) return;
        TableRow row = dataRows.get(rowIndex);
        StringBuilder rowData = new StringBuilder();
        
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = (TextView) row.findViewWithTag("col_" + i);
            if (tv != null) {
                rowData.append(tv.getText().toString());
                if (i < columnHeaders.length - 1) {
                    rowData.append("\t");
                }
            }
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Row Data", rowData.toString());
        clipboard.setPrimaryClip(clip);
        
        showToast("Row copied! Paste in Excel", true);
    }
    
    private void toggleFloatingButton() {
        if (floatingButtonEnabled) {
            stopService(new Intent(this, FloatingButtonService.class));
            floatingButtonEnabled = false;
            btnToggleFloating.setText("ENABLE");
            btnToggleFloating.setBackgroundResource(R.drawable.btn_success_bg);
            showToast("Quick Entry disabled", true);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                    );
                    startActivityForResult(intent, 1234);
                    return;
                }
            }
            
            startService(new Intent(this, FloatingButtonService.class));
            floatingButtonEnabled = true;
            btnToggleFloating.setText("DISABLE");
            btnToggleFloating.setBackgroundResource(R.drawable.btn_danger_bg);
            showToast("Quick Entry ON! Minimize app.", true);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadTableFromDatabase();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    toggleFloatingButton();
                } else {
                    showToast("Permission denied!", false);
                }
            }
        }
    }
    
    private void copyAllFilledRows() {
        if (dataRows.isEmpty()) {
            showToast("No data to copy!", false);
            return;
        }
        
        StringBuilder allData = new StringBuilder();
        
        // Add headers first
        for (int i = 0; i < columnHeaders.length; i++) {
            allData.append(columnHeaders[i]);
            if (i < columnHeaders.length - 1) {
                allData.append("\t");
            }
        }
        allData.append("\n");
        
        // Add data rows
        for (int i = 0; i < dataRows.size(); i++) {
            TableRow row = dataRows.get(i);
            for (int j = 0; j < columnHeaders.length; j++) {
                TextView tv = (TextView) row.findViewWithTag("col_" + j);
                if (tv != null) {
                    allData.append(tv.getText().toString());
                    if (j < columnHeaders.length - 1) {
                        allData.append("\t");
                    }
                }
            }
            allData.append("\n");
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("All Rows Data", allData.toString());
        clipboard.setPrimaryClip(clip);
        
        showToast(dataRows.size() + " rows copied!", true);
    }
    
    private void confirmClearTable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("CLEAR TABLE?");
        builder.setMessage("This will delete all entries for " + currentMonth + " " + currentYear + ". This cannot be undone.");
        builder.setPositiveButton("DELETE ALL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Delete entries for current month/year
                ArrayList<DatabaseHelper.EntryData> entries = dbHelper.getEntriesByMonthYear(currentMonth, currentYear);
                for (DatabaseHelper.EntryData entry : entries) {
                    dbHelper.deleteEntry(entry.id);
                }
                loadTableFromDatabase();
                showToast("Table cleared!", true);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showRowOptionsDialog(final int rowIndex) {
        final CharSequence[] options = {"Delete Row", "Copy Row"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ROW OPTIONS");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        deleteRow(rowIndex);
                        break;
                    case 1:
                        copyRowToClipboard(rowIndex);
                        break;
                }
            }
        });
        builder.show();
    }
    
    private void deleteRow(final int rowIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("DELETE ROW?");
        builder.setMessage("Are you sure you want to delete this row?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (rowIndex < dataRows.size()) {
                    TableRow row = dataRows.get(rowIndex);
                    long entryId = (long) row.getTag();
                    dbHelper.deleteEntry(entryId);
                    loadTableFromDatabase();
                    showToast("Row deleted!", true);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showToast(String message, boolean isSuccess) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
