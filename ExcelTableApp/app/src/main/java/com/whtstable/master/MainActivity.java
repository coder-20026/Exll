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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {
    
    // Excel Column Widths (in dp) - matching Excel proportions
    // A=34dp, B=62dp, C=169dp, D=30dp, E=45dp, F=92dp, G=95dp, H=89dp, I=38dp
    private static final int[] COLUMN_WIDTHS = {34, 62, 169, 30, 45, 92, 95, 89, 38};
    private static final int DATA_ROW_HEIGHT = 20;      // Row 3-12 height
    private static final int ADDITIONAL_ROW_HEIGHT = 15; // Row 13-17 height
    private static final int TOTAL_COLUMNS = 9;
    
    // Views
    private LinearLayout dataRowsContainer;
    private LinearLayout additionalRowsContainer;
    private EditText cellA1F1, cellG1I1;
    private EditText cellG20, cellH20, cellG21, cellH21, cellG22, cellH22, cellG23, cellH23;
    private TextView cellH24;
    private Button btnToggleFloating, btnSelectMonth, btnSelectYear, btnHistory, btnCopyAll, btnClearTable;
    private TextView tvCurrentDate, tvEntryCount;
    
    // Data rows storage
    private ArrayList<LinearLayout> dataRows;
    private ArrayList<LinearLayout> additionalRows;
    private int selectedRowIndex = -1;
    private boolean floatingButtonEnabled = false;
    
    private DatabaseHelper dbHelper;
    private String currentMonth = "January";
    private String currentYear = "2026";
    private String currentDateFull = "";
    
    // Excel Column headers for copy
    private String[] columnHeaders = {
        "SR NO", "BANK NAME", "APPLICANT NAME", "STATUS", "REASON FOR CNV", 
        "LAT-LONG FROM", "LAT-LONG TO", "AREA", "KM"
    };
    
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
        
        // Create Excel data rows (Row 3-12 = 10 rows)
        createDataRows(10);
        
        // Create additional rows (Row 13-17 = 5 rows)
        createAdditionalRows(5);
        
        // Load saved data for current month/year
        loadTableFromDatabase();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Update UI
        updateMonthYearButtons();
    }
    
    private void initializeViews() {
        // Main containers
        dataRowsContainer = (LinearLayout) findViewById(R.id.dataRowsContainer);
        additionalRowsContainer = (LinearLayout) findViewById(R.id.additionalRowsContainer);
        
        // Header cells (editable)
        cellA1F1 = (EditText) findViewById(R.id.cellA1F1);
        cellG1I1 = (EditText) findViewById(R.id.cellG1I1);
        
        // Summary table cells
        cellG20 = (EditText) findViewById(R.id.cellG20);
        cellH20 = (EditText) findViewById(R.id.cellH20);
        cellG21 = (EditText) findViewById(R.id.cellG21);
        cellH21 = (EditText) findViewById(R.id.cellH21);
        cellG22 = (EditText) findViewById(R.id.cellG22);
        cellH22 = (EditText) findViewById(R.id.cellH22);
        cellG23 = (EditText) findViewById(R.id.cellG23);
        cellH23 = (EditText) findViewById(R.id.cellH23);
        cellH24 = (TextView) findViewById(R.id.cellH24);
        
        // Buttons
        btnToggleFloating = (Button) findViewById(R.id.btnToggleFloating);
        btnSelectMonth = (Button) findViewById(R.id.btnSelectMonth);
        btnSelectYear = (Button) findViewById(R.id.btnSelectYear);
        btnHistory = (Button) findViewById(R.id.btnHistory);
        btnCopyAll = (Button) findViewById(R.id.btnCopyAll);
        btnClearTable = (Button) findViewById(R.id.btnClearTable);
        tvCurrentDate = (TextView) findViewById(R.id.tvCurrentDate);
        tvEntryCount = (TextView) findViewById(R.id.tvEntryCount);
        
        dataRows = new ArrayList<LinearLayout>();
        additionalRows = new ArrayList<LinearLayout>();
    }
    
    private void createDataRows(int count) {
        dataRowsContainer.removeAllViews();
        dataRows.clear();
        
        for (int rowIndex = 0; rowIndex < count; rowIndex++) {
            LinearLayout row = createExcelRow(rowIndex + 1, DATA_ROW_HEIGHT, true);
            dataRows.add(row);
            dataRowsContainer.addView(row);
        }
    }
    
    private void createAdditionalRows(int count) {
        additionalRowsContainer.removeAllViews();
        additionalRows.clear();
        
        for (int rowIndex = 0; rowIndex < count; rowIndex++) {
            LinearLayout row = createExcelRow(11 + rowIndex, ADDITIONAL_ROW_HEIGHT, false);
            additionalRows.add(row);
            additionalRowsContainer.addView(row);
        }
    }
    
    private LinearLayout createExcelRow(final int srNo, int height, boolean isDataRow) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dpToPx(height)
        ));
        
        // Column A: SR NO (auto-filled)
        TextView cellA = createTextCell(String.valueOf(srNo), COLUMN_WIDTHS[0], true);
        cellA.setTag("cellA");
        row.addView(cellA);
        
        // Columns B-I: Editable cells
        for (int col = 1; col < TOTAL_COLUMNS; col++) {
            EditText cell = createEditCell("", COLUMN_WIDTHS[col]);
            cell.setTag("cell" + (char)('A' + col));
            row.addView(cell);
        }
        
        // Row click handler
        final int rowIndexFinal = srNo - 1;
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRow(rowIndexFinal);
                copyRowToClipboard(rowIndexFinal);
            }
        });
        
        // Long press for options
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showRowOptionsDialog(rowIndexFinal);
                return true;
            }
        });
        
        return row;
    }
    
    private TextView createTextCell(String text, int widthDp, boolean isCenter) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(9);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(isCenter ? Gravity.CENTER : (Gravity.CENTER_VERTICAL | Gravity.START));
        tv.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
        tv.setBackgroundResource(R.drawable.excel_cell_border);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dpToPx(widthDp),
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        tv.setLayoutParams(params);
        
        return tv;
    }
    
    private EditText createEditCell(String text, int widthDp) {
        EditText et = new EditText(this);
        et.setText(text);
        et.setTextSize(9);
        et.setTextColor(Color.BLACK);
        et.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        et.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
        et.setBackgroundResource(R.drawable.excel_cell_border);
        et.setSingleLine(true);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dpToPx(widthDp),
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        et.setLayoutParams(params);
        
        return et;
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
    
    private void loadTableFromDatabase() {
        // Clear all data rows first
        clearAllDataRows();
        
        // Load entries for current month/year
        ArrayList<DatabaseHelper.EntryData> entries = dbHelper.getEntriesByMonthYear(currentMonth, currentYear);
        
        if (entries.isEmpty()) {
            currentDateFull = "";
        } else {
            currentDateFull = entries.get(entries.size() - 1).dateFull;
        }
        
        // Fill data rows with database entries
        for (int i = 0; i < entries.size() && i < dataRows.size(); i++) {
            DatabaseHelper.EntryData entry = entries.get(i);
            fillRowWithEntry(dataRows.get(i), entry, i);
        }
        
        updateCurrentDateDisplay();
        calculateTotals();
    }
    
    private void clearAllDataRows() {
        for (LinearLayout row : dataRows) {
            for (int col = 1; col < TOTAL_COLUMNS; col++) {
                EditText cell = (EditText) row.findViewWithTag("cell" + (char)('A' + col));
                if (cell != null) {
                    cell.setText("");
                }
            }
            row.setTag(null); // Clear entry ID
        }
    }
    
    private void fillRowWithEntry(LinearLayout row, DatabaseHelper.EntryData entry, int rowIndex) {
        row.setTag(entry.id); // Store entry ID
        
        // Fill cells B-I
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
        
        for (int col = 1; col < TOTAL_COLUMNS; col++) {
            EditText cell = (EditText) row.findViewWithTag("cell" + (char)('A' + col));
            if (cell != null && col - 1 < values.length) {
                cell.setText(values[col - 1] != null ? values[col - 1] : "");
            }
        }
    }
    
    private void selectRow(int rowIndex) {
        // Deselect previous row
        if (selectedRowIndex != -1 && selectedRowIndex < dataRows.size()) {
            setRowBackground(dataRows.get(selectedRowIndex), R.drawable.excel_cell_border);
        }
        
        // Select new row
        selectedRowIndex = rowIndex;
        if (rowIndex < dataRows.size()) {
            setRowBackground(dataRows.get(rowIndex), R.drawable.excel_cell_selected);
        }
    }
    
    private void setRowBackground(LinearLayout row, int backgroundRes) {
        for (int col = 0; col < TOTAL_COLUMNS; col++) {
            View cell = row.findViewWithTag("cell" + (char)('A' + col));
            if (cell != null) {
                cell.setBackgroundResource(backgroundRes);
            }
        }
        // Also update SR NO cell (TextView)
        View cellA = row.findViewWithTag("cellA");
        if (cellA != null) {
            cellA.setBackgroundResource(backgroundRes);
        }
    }
    
    private void copyRowToClipboard(int rowIndex) {
        if (rowIndex >= dataRows.size()) return;
        LinearLayout row = dataRows.get(rowIndex);
        StringBuilder rowData = new StringBuilder();
        
        // Get SR NO
        TextView cellA = (TextView) row.findViewWithTag("cellA");
        if (cellA != null) {
            rowData.append(cellA.getText().toString());
        }
        
        // Get other cells
        for (int col = 1; col < TOTAL_COLUMNS; col++) {
            rowData.append("\t");
            EditText cell = (EditText) row.findViewWithTag("cell" + (char)('A' + col));
            if (cell != null) {
                rowData.append(cell.getText().toString());
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
        StringBuilder allData = new StringBuilder();
        int filledRowCount = 0;
        
        // Add Row 1: Header info
        allData.append(cellA1F1.getText().toString());
        allData.append("\t\t\t\t\t");
        allData.append(cellG1I1.getText().toString());
        allData.append("\n");
        
        // Add Row 2: Column headers
        for (int i = 0; i < columnHeaders.length; i++) {
            allData.append(columnHeaders[i]);
            if (i < columnHeaders.length - 1) {
                allData.append("\t");
            }
        }
        allData.append("\n");
        
        // Add data rows
        for (int i = 0; i < dataRows.size(); i++) {
            LinearLayout row = dataRows.get(i);
            StringBuilder rowData = new StringBuilder();
            boolean hasData = false;
            
            // Get SR NO
            TextView cellA = (TextView) row.findViewWithTag("cellA");
            if (cellA != null) {
                rowData.append(cellA.getText().toString());
            }
            
            // Get other cells
            for (int col = 1; col < TOTAL_COLUMNS; col++) {
                rowData.append("\t");
                EditText cell = (EditText) row.findViewWithTag("cell" + (char)('A' + col));
                if (cell != null) {
                    String text = cell.getText().toString();
                    rowData.append(text);
                    if (!text.isEmpty()) {
                        hasData = true;
                    }
                }
            }
            
            if (hasData) {
                allData.append(rowData.toString());
                allData.append("\n");
                filledRowCount++;
            }
        }
        
        // Add summary section
        allData.append("\n");
        allData.append("\t\t\t\t\t\tNO. OF COUNT\tAMOUNT\n");
        allData.append("\t\t\t\t\tTOTAL KM\t").append(cellG20.getText().toString()).append("\t").append(cellH20.getText().toString()).append("\n");
        allData.append("\t\t\t\t\tLUNCH\t").append(cellG21.getText().toString()).append("\t").append(cellH21.getText().toString()).append("\n");
        allData.append("\t\t\t\t\tVISIT\t").append(cellG22.getText().toString()).append("\t").append(cellH22.getText().toString()).append("\n");
        allData.append("\t\t\t\t\tOTHER\t").append(cellG23.getText().toString()).append("\t").append(cellH23.getText().toString()).append("\n");
        allData.append("\t\t\t\t\tTOTAL AMOUNT\t\t").append(cellH24.getText().toString()).append("\n");
        
        if (filledRowCount == 0) {
            showToast("No data to copy!", false);
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Excel Table Data", allData.toString());
        clipboard.setPrimaryClip(clip);
        
        showToast(filledRowCount + " rows copied with summary!", true);
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
                clearSummaryFields();
                showToast("Table cleared!", true);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void clearSummaryFields() {
        cellG20.setText("");
        cellH20.setText("");
        cellG21.setText("");
        cellH21.setText("");
        cellG22.setText("");
        cellH22.setText("");
        cellG23.setText("");
        cellH23.setText("");
        cellH24.setText("");
    }
    
    private void calculateTotals() {
        // Calculate total KM from data rows
        double totalKm = 0;
        for (LinearLayout row : dataRows) {
            EditText kmCell = (EditText) row.findViewWithTag("cellI");
            if (kmCell != null) {
                String kmText = kmCell.getText().toString().trim();
                if (!kmText.isEmpty()) {
                    try {
                        totalKm += Double.parseDouble(kmText);
                    } catch (NumberFormatException e) {
                        // Skip invalid numbers
                    }
                }
            }
        }
        
        // Auto-fill total KM count
        cellG20.setText(String.valueOf((int) totalKm));
        
        // Calculate total amount
        calculateTotalAmount();
    }
    
    private void calculateTotalAmount() {
        double total = 0;
        
        try {
            String h20 = cellH20.getText().toString().trim();
            if (!h20.isEmpty()) total += Double.parseDouble(h20);
            
            String h21 = cellH21.getText().toString().trim();
            if (!h21.isEmpty()) total += Double.parseDouble(h21);
            
            String h22 = cellH22.getText().toString().trim();
            if (!h22.isEmpty()) total += Double.parseDouble(h22);
            
            String h23 = cellH23.getText().toString().trim();
            if (!h23.isEmpty()) total += Double.parseDouble(h23);
        } catch (NumberFormatException e) {
            // Skip invalid numbers
        }
        
        if (total > 0) {
            cellH24.setText(String.format("%.2f", total));
        } else {
            cellH24.setText("");
        }
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
                    LinearLayout row = dataRows.get(rowIndex);
                    Object tag = row.getTag();
                    if (tag != null) {
                        long entryId = (long) tag;
                        dbHelper.deleteEntry(entryId);
                    }
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
