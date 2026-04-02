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
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

import java.util.ArrayList;

public class MainActivity extends Activity {
    
    // Excel Column Widths (in dp) - matching Excel proportions
    // A=34dp, B=62dp, C=200dp (40.0), D=30dp, E=45dp, F=92dp, G=95dp, H=89dp, I=38dp
    private static final int[] COLUMN_WIDTHS = {34, 62, 200, 30, 45, 92, 95, 89, 38};
    private static final int DATA_ROW_HEIGHT = 21;       // All 15 rows height (20.25 ~ 21dp)
    private static final int TOTAL_COLUMNS = 9;
    private static final int TOTAL_DATA_ROWS = 15;       // Row 3-17 (15 data rows total)
    
    // Views
    private LinearLayout dataRowsContainer;
    private EditText cellA1F1, cellG1I1;
    
    // Summary table cells (IDs matching layout - cellG20, cellH20 etc.)
    private TextView cellTotalKmCount, cellTotalKmAmount;
    private TextView cellLunchCount, cellLunchAmount;
    private TextView cellVisitCount, cellVisitAmount;
    private TextView cellOtherCount, cellOtherAmount;
    private TextView cellFinalTotal;
    
    private Button btnToggleFloating, btnSelectMonth, btnSelectYear, btnHistory, btnBorders;
    private TextView tvCurrentDate, tvEntryCount;
    
    // Border Panel Views
    private View borderPanelContainer;
    private boolean isBorderPanelVisible = false;
    private BorderManager borderManager;
    
    // Cell Selection for borders
    private int selectedCellRow = -1;
    private int selectedCellCol = -1;
    private View selectedCellView = null;
    
    // Data rows storage
    private ArrayList<LinearLayout> allDataRows;
    private int selectedRowIndex = -1;
    private boolean floatingButtonEnabled = false;
    
    private DatabaseHelper dbHelper;
    private String currentMonth = "January";
    private String currentYear = "2026";
    private String currentDateFull = "";
    
    // Excel Column headers for copy
    private String[] columnHeaders = {
        "SR NO", "BANK NAME", "APPLICANT NAME", "STATUS", "REASON FOR CNV", 
        "LATLONG FROM", "LATLONG TO", "AREA", "KM"
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
        
        // Initialize Border Manager
        borderManager = BorderManager.getInstance(this);
        
        // Load saved month/year
        currentMonth = dbHelper.getSelectedMonth();
        currentYear = dbHelper.getSelectedYear();
        
        // Set border manager context
        borderManager.setCurrentMonthYear(currentMonth, currentYear);
        
        // Initialize views
        initializeViews();
        
        // Create all data rows (Row 3-15 = 13 rows total)
        createAllDataRows();
        
        // Load saved data for current month/year
        loadTableFromDatabase();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Initialize Border Panel
        initializeBorderPanel();
        
        // Update UI
        updateMonthYearButtons();
        
        // Apply saved borders to cells
        applyBordersToAllCells();
    }
    
    private void initializeViews() {
        // Main container
        dataRowsContainer = (LinearLayout) findViewById(R.id.dataRowsContainer);
        
        // Header cells (editable)
        cellA1F1 = (EditText) findViewById(R.id.cellA1F1);
        cellG1I1 = (EditText) findViewById(R.id.cellG1I1);
        
        // Summary table cells (IDs from layout: cellG20 = TOTAL KM count, cellH20 = amount, etc.)
        cellTotalKmCount = (TextView) findViewById(R.id.cellG20);
        cellTotalKmAmount = (TextView) findViewById(R.id.cellH20);
        cellLunchCount = (TextView) findViewById(R.id.cellG21);
        cellLunchAmount = (TextView) findViewById(R.id.cellH21);
        cellVisitCount = (TextView) findViewById(R.id.cellG22);
        cellVisitAmount = (TextView) findViewById(R.id.cellH22);
        cellOtherCount = (TextView) findViewById(R.id.cellG23);
        cellOtherAmount = (TextView) findViewById(R.id.cellH23);
        cellFinalTotal = (TextView) findViewById(R.id.cellH24);
        
        // Buttons
        btnToggleFloating = (Button) findViewById(R.id.btnToggleFloating);
        btnSelectMonth = (Button) findViewById(R.id.btnSelectMonth);
        btnSelectYear = (Button) findViewById(R.id.btnSelectYear);
        btnHistory = (Button) findViewById(R.id.btnHistory);
        btnBorders = (Button) findViewById(R.id.btnBorders);
        tvCurrentDate = (TextView) findViewById(R.id.tvCurrentDate);
        tvEntryCount = (TextView) findViewById(R.id.tvEntryCount);
        
        // Border Panel Container
        borderPanelContainer = findViewById(R.id.borderPanelContainer);
        
        allDataRows = new ArrayList<LinearLayout>();
    }
    
    private void createAllDataRows() {
        dataRowsContainer.removeAllViews();
        allDataRows.clear();
        
        // Create all 15 data rows (Row 3-17) with same height (21dp ~ 20.25)
        // Last row (row 15) will have thick bottom border
        for (int rowIndex = 0; rowIndex < TOTAL_DATA_ROWS; rowIndex++) {
            boolean isLastRow = (rowIndex == TOTAL_DATA_ROWS - 1);
            LinearLayout row = createExcelRow(rowIndex + 1, DATA_ROW_HEIGHT, isLastRow);
            allDataRows.add(row);
            dataRowsContainer.addView(row);
        }
    }
    
    private LinearLayout createExcelRow(final int srNo, int height, boolean isLastRow) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dpToPx(height)
        ));
        
        // Use thin border for data cells, thick bottom border for last row
        int borderDrawable = isLastRow ? R.drawable.excel_data_last_row : R.drawable.excel_data_thin;
        
        // Column A: SR NO (auto-filled, centered)
        TextView cellA = new TextView(this);
        cellA.setText(String.valueOf(srNo));
        cellA.setTextSize(14);
        cellA.setTextColor(Color.BLACK);
        cellA.setGravity(Gravity.CENTER);
        cellA.setPadding(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
        cellA.setBackgroundResource(borderDrawable);
        cellA.setTag("cellA");
        LinearLayout.LayoutParams paramsA = new LinearLayout.LayoutParams(
            dpToPx(COLUMN_WIDTHS[0]),
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        cellA.setLayoutParams(paramsA);
        row.addView(cellA);
        
        // Column B: BANK NAME (left align)
        row.addView(createEditCell("", COLUMN_WIDTHS[1], "cellB", Gravity.START | Gravity.CENTER_VERTICAL, 14, borderDrawable));
        
        // Column C: APPLICANT NAME (left align, auto-adjust based on content)
        row.addView(createEditCell("", COLUMN_WIDTHS[2], "cellC", Gravity.START | Gravity.CENTER_VERTICAL, 14, borderDrawable));
        
        // Column D: STATUS (center)
        row.addView(createEditCell("", COLUMN_WIDTHS[3], "cellD", Gravity.CENTER, 14, borderDrawable));
        
        // Column E: REASON FOR CNV (left align, smaller font)
        row.addView(createEditCell("", COLUMN_WIDTHS[4], "cellE", Gravity.START | Gravity.CENTER_VERTICAL, 12, borderDrawable));
        
        // Column F: LATLONG FROM (left align, smaller font)
        row.addView(createEditCell("", COLUMN_WIDTHS[5], "cellF", Gravity.START | Gravity.CENTER_VERTICAL, 12, borderDrawable));
        
        // Column G: LATLONG TO (left align, smaller font)
        row.addView(createEditCell("", COLUMN_WIDTHS[6], "cellG", Gravity.START | Gravity.CENTER_VERTICAL, 12, borderDrawable));
        
        // Column H: AREA (left align)
        row.addView(createEditCell("", COLUMN_WIDTHS[7], "cellH", Gravity.START | Gravity.CENTER_VERTICAL, 14, borderDrawable));
        
        // Column I: KM (right align, number input)
        EditText cellI = createEditCell("", COLUMN_WIDTHS[8], "cellI", Gravity.END | Gravity.CENTER_VERTICAL, 14, borderDrawable);
        cellI.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        row.addView(cellI);
        
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
    
    private EditText createEditCell(String text, int widthDp, final String tag, int gravity, int textSizeSp, int borderDrawable) {
        final EditText et = new EditText(this);
        et.setText(text);
        et.setTextSize(textSizeSp);
        et.setTextColor(Color.BLACK);
        et.setGravity(gravity);
        et.setPadding(dpToPx(2), dpToPx(1), dpToPx(2), dpToPx(1));
        et.setBackgroundResource(borderDrawable);
        et.setSingleLine(true);
        et.setTag(tag);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dpToPx(widthDp),
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        et.setLayoutParams(params);
        
        // Add focus change listener for cell selection (for borders)
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && isBorderPanelVisible) {
                    // Find row index and column index
                    LinearLayout parent = (LinearLayout) v.getParent();
                    int rowIndex = allDataRows.indexOf(parent);
                    int colIndex = getColumnIndex(tag);
                    
                    if (rowIndex >= 0 && colIndex >= 0) {
                        selectCellForBorder(rowIndex, colIndex, v);
                    }
                }
            }
        });
        
        return et;
    }
    
    private int getColumnIndex(String tag) {
        if (tag == null || tag.length() < 5) return -1;
        char colChar = tag.charAt(4); // "cellA" -> 'A'
        return colChar - 'A';
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private void updateMonthYearButtons() {
        // Show short month name
        String shortMonth = currentMonth.substring(0, 3);
        btnSelectMonth.setText(shortMonth);
        btnSelectYear.setText(currentYear);
        updateCurrentDateDisplay();
    }
    
    private void updateCurrentDateDisplay() {
        if (currentDateFull.isEmpty()) {
            tvCurrentDate.setText(currentMonth + " " + currentYear);
        } else {
            tvCurrentDate.setText(currentDateFull);
        }
        
        // Count entries for current month/year
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
        
        btnBorders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBorderPanel();
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
                borderManager.setCurrentMonthYear(currentMonth, currentYear);
                updateMonthYearButtons();
                loadTableFromDatabase();
                applyBordersToAllCells();
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
                borderManager.setCurrentMonthYear(currentMonth, currentYear);
                updateMonthYearButtons();
                loadTableFromDatabase();
                applyBordersToAllCells();
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
        for (int i = 0; i < entries.size() && i < allDataRows.size(); i++) {
            DatabaseHelper.EntryData entry = entries.get(i);
            fillRowWithEntry(allDataRows.get(i), entry, i);
        }
        
        updateCurrentDateDisplay();
        calculateTotals();
    }
    
    private void clearAllDataRows() {
        for (LinearLayout row : allDataRows) {
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
        if (selectedRowIndex != -1 && selectedRowIndex < allDataRows.size()) {
            // Use thick bottom border for last row, thin for others
            boolean wasLastRow = (selectedRowIndex == TOTAL_DATA_ROWS - 1);
            int borderDrawable = wasLastRow ? R.drawable.excel_data_last_row : R.drawable.excel_data_thin;
            setRowBackground(allDataRows.get(selectedRowIndex), borderDrawable);
        }
        
        // Select new row
        selectedRowIndex = rowIndex;
        if (rowIndex < allDataRows.size()) {
            setRowBackground(allDataRows.get(rowIndex), R.drawable.excel_cell_selected);
        }
    }
    
    private void setRowBackground(LinearLayout row, int backgroundRes) {
        for (int col = 0; col < row.getChildCount(); col++) {
            View cell = row.getChildAt(col);
            if (cell != null) {
                cell.setBackgroundResource(backgroundRes);
            }
        }
    }
    
    private void copyRowToClipboard(int rowIndex) {
        if (rowIndex >= allDataRows.size()) return;
        LinearLayout row = allDataRows.get(rowIndex);
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
            btnToggleFloating.setText("Enable");
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
            btnToggleFloating.setText("Disable");
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
    
    private void calculateTotals() {
        // Calculate total KM from data rows
        double totalKm = 0;
        for (LinearLayout row : allDataRows) {
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
        if (cellTotalKmCount != null) {
            cellTotalKmCount.setText(totalKm > 0 ? String.valueOf((int) totalKm) : "");
        }
        
        // Calculate total amount
        calculateTotalAmount();
    }
    
    private void calculateTotalAmount() {
        double total = 0;
        
        try {
            if (cellTotalKmAmount != null) {
                String val = cellTotalKmAmount.getText().toString().trim();
                if (!val.isEmpty()) total += Double.parseDouble(val);
            }
            
            if (cellLunchAmount != null) {
                String val = cellLunchAmount.getText().toString().trim();
                if (!val.isEmpty()) total += Double.parseDouble(val);
            }
            
            if (cellVisitAmount != null) {
                String val = cellVisitAmount.getText().toString().trim();
                if (!val.isEmpty()) total += Double.parseDouble(val);
            }
            
            if (cellOtherAmount != null) {
                String val = cellOtherAmount.getText().toString().trim();
                if (!val.isEmpty()) total += Double.parseDouble(val);
            }
        } catch (NumberFormatException e) {
            // Skip invalid numbers
        }
        
        if (cellFinalTotal != null) {
            if (total > 0) {
                cellFinalTotal.setText(String.format("%.2f", total));
            } else {
                cellFinalTotal.setText("");
            }
        }
    }
    
    private void showRowOptionsDialog(final int rowIndex) {
        final CharSequence[] options = {"Copy Row", "Delete Row", "Copy All Data"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ROW OPTIONS");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        copyRowToClipboard(rowIndex);
                        break;
                    case 1:
                        deleteRow(rowIndex);
                        break;
                    case 2:
                        copyAllFilledRows();
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
                if (rowIndex < allDataRows.size()) {
                    LinearLayout row = allDataRows.get(rowIndex);
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
        for (int i = 0; i < allDataRows.size(); i++) {
            LinearLayout row = allDataRows.get(i);
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
        
        String totalKmCount = cellTotalKmCount != null ? cellTotalKmCount.getText().toString() : "";
        String totalKmAmount = cellTotalKmAmount != null ? cellTotalKmAmount.getText().toString() : "";
        String lunchCount = cellLunchCount != null ? cellLunchCount.getText().toString() : "";
        String lunchAmount = cellLunchAmount != null ? cellLunchAmount.getText().toString() : "";
        String visitCount = cellVisitCount != null ? cellVisitCount.getText().toString() : "";
        String visitAmount = cellVisitAmount != null ? cellVisitAmount.getText().toString() : "";
        String otherCount = cellOtherCount != null ? cellOtherCount.getText().toString() : "";
        String otherAmount = cellOtherAmount != null ? cellOtherAmount.getText().toString() : "";
        String finalTotal = cellFinalTotal != null ? cellFinalTotal.getText().toString() : "";
        
        allData.append("\t\t\t\t\tTOTAL KM\t").append(totalKmCount).append("\t").append(totalKmAmount).append("\n");
        allData.append("\t\t\t\t\tLUNCH\t").append(lunchCount).append("\t").append(lunchAmount).append("\n");
        allData.append("\t\t\t\t\tVISIT\t").append(visitCount).append("\t").append(visitAmount).append("\n");
        allData.append("\t\t\t\t\tOTHER\t").append(otherCount).append("\t").append(otherAmount).append("\n");
        allData.append("\t\t\t\t\tTOTAL AMOUNT\t\t").append(finalTotal).append("\n");
        
        if (filledRowCount == 0) {
            showToast("No data to copy!", false);
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Excel Table Data", allData.toString());
        clipboard.setPrimaryClip(clip);
        
        showToast(filledRowCount + " rows copied with summary!", true);
    }
    
    private void showToast(String message, boolean isSuccess) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    // ==================== BORDER PANEL METHODS ====================
    
    private void initializeBorderPanel() {
        if (borderPanelContainer == null) return;
        
        // Close button
        View btnClose = borderPanelContainer.findViewById(R.id.btnCloseBorderPanel);
        if (btnClose != null) {
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideBorderPanel();
                }
            });
        }
        
        // Border option buttons
        setupBorderOptionButton(R.id.btnBorderAll, BorderManager.BORDER_TYPE_ALL);
        setupBorderOptionButton(R.id.btnBorderInner, BorderManager.BORDER_TYPE_INNER);
        setupBorderOptionButton(R.id.btnBorderOuter, BorderManager.BORDER_TYPE_OUTER);
        setupBorderOptionButton(R.id.btnBorderNone, BorderManager.BORDER_TYPE_NONE);
        setupBorderOptionButton(R.id.btnBorderTop, BorderManager.BORDER_TYPE_TOP);
        setupBorderOptionButton(R.id.btnBorderBottom, BorderManager.BORDER_TYPE_BOTTOM);
        setupBorderOptionButton(R.id.btnBorderLeft, BorderManager.BORDER_TYPE_LEFT);
        setupBorderOptionButton(R.id.btnBorderRight, BorderManager.BORDER_TYPE_RIGHT);
        setupBorderOptionButton(R.id.btnBorderHorizontalInner, BorderManager.BORDER_TYPE_HORIZONTAL_INNER);
        setupBorderOptionButton(R.id.btnBorderVerticalInner, BorderManager.BORDER_TYPE_VERTICAL_INNER);
        setupBorderOptionButton(R.id.btnBorderTopBottom, BorderManager.BORDER_TYPE_TOP_AND_BOTTOM);
        setupBorderOptionButton(R.id.btnBorderThickBottom, BorderManager.BORDER_TYPE_THICK_BOTTOM);
        setupBorderOptionButton(R.id.btnBorderDoubleBottom, BorderManager.BORDER_TYPE_DOUBLE_BOTTOM);
        setupBorderOptionButton(R.id.btnBorderTopThickBottom, BorderManager.BORDER_TYPE_TOP_AND_THICK_BOTTOM);
    }
    
    private void setupBorderOptionButton(int buttonId, final int borderType) {
        View btn = borderPanelContainer.findViewById(buttonId);
        if (btn != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyBorderToSelection(borderType);
                }
            });
        }
    }
    
    private void toggleBorderPanel() {
        android.util.Log.d("v0", "toggleBorderPanel called, visible=" + isBorderPanelVisible + ", container=" + (borderPanelContainer != null));
        if (isBorderPanelVisible) {
            hideBorderPanel();
        } else {
            showBorderPanel();
        }
    }
    
    private void showBorderPanel() {
        android.util.Log.d("v0", "showBorderPanel called, container=" + (borderPanelContainer != null));
        if (borderPanelContainer != null) {
            borderPanelContainer.setVisibility(View.VISIBLE);
            isBorderPanelVisible = true;
            btnBorders.setBackgroundResource(R.drawable.btn_danger_bg);
            android.util.Log.d("v0", "Border panel shown");
            
            // If a row is selected, select its first cell
            if (selectedRowIndex >= 0 && selectedRowIndex < allDataRows.size()) {
                selectCellForBorder(selectedRowIndex, 0, allDataRows.get(selectedRowIndex).getChildAt(0));
            }
            
            showToast("Tap cell to select, then choose border", true);
        } else {
            android.util.Log.d("v0", "borderPanelContainer is NULL!");
            showToast("Border panel not found!", false);
        }
    }
    
    private void hideBorderPanel() {
        if (borderPanelContainer != null) {
            borderPanelContainer.setVisibility(View.GONE);
            isBorderPanelVisible = false;
            btnBorders.setBackgroundResource(R.drawable.btn_primary_bg);
            clearCellSelection();
        }
    }
    
    private void selectCellForBorder(int row, int col, View cellView) {
        // Clear previous selection highlight
        if (selectedCellView != null) {
            applyCellBorderDrawable(selectedCellRow, selectedCellCol, selectedCellView);
        }
        
        // Set new selection
        selectedCellRow = row;
        selectedCellCol = col;
        selectedCellView = cellView;
        
        // Update border manager selection
        borderManager.setSingleSelection(row, col);
        
        // Highlight selected cell
        if (cellView != null) {
            cellView.setBackgroundResource(R.drawable.excel_cell_selected);
        }
    }
    
    private void clearCellSelection() {
        if (selectedCellView != null) {
            applyCellBorderDrawable(selectedCellRow, selectedCellCol, selectedCellView);
        }
        selectedCellRow = -1;
        selectedCellCol = -1;
        selectedCellView = null;
        borderManager.clearSelection();
    }
    
    private void applyBorderToSelection(int borderType) {
        if (!borderManager.hasSelection()) {
            showToast("Please select a cell first", false);
            return;
        }
        
        // Default border style is THIN
        int borderStyle = CellBorder.STYLE_THIN;
        
        // Apply border type to selection
        borderManager.applyBorderType(borderType, borderStyle);
        
        // Refresh cell borders visually
        applyBordersToAllCells();
        
        // Show feedback
        String message = borderType == BorderManager.BORDER_TYPE_NONE ? "Borders cleared" : "Border applied";
        showToast(message, true);
    }
    
    private void applyBordersToAllCells() {
        for (int rowIndex = 0; rowIndex < allDataRows.size(); rowIndex++) {
            LinearLayout row = allDataRows.get(rowIndex);
            for (int colIndex = 0; colIndex < row.getChildCount(); colIndex++) {
                View cell = row.getChildAt(colIndex);
                applyCellBorderDrawable(rowIndex, colIndex, cell);
            }
        }
    }
    
    private void applyCellBorderDrawable(int row, int col, View cell) {
        if (cell == null) return;
        
        CellBorder border = borderManager.getCellBorder(row, col);
        boolean isLastRow = (row == TOTAL_DATA_ROWS - 1);
        
        // If cell has any custom border, use BorderDrawable
        if (border.hasAnyBorder()) {
            float density = getResources().getDisplayMetrics().density;
            BorderDrawable borderDrawable = new BorderDrawable(border, density);
            cell.setBackground(borderDrawable);
        } else {
            // Use default border based on row position
            int defaultBorder = isLastRow ? R.drawable.excel_data_last_row : R.drawable.excel_data_thin;
            cell.setBackgroundResource(defaultBorder);
        }
    }
}
