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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    
    // Premium Color Constants
    private static final String COLOR_PRIMARY = "#1A237E";
    private static final String COLOR_ACCENT = "#7C4DFF";
    private static final String COLOR_SUCCESS = "#4CAF50";
    private static final String COLOR_DANGER = "#FF5252";
    private static final String COLOR_HEADER = "#1A237E";
    private static final String COLOR_ROW_SELECTED = "#C5CAE9";
    private static final String COLOR_ROW_DEFAULT = "#FFFFFF";
    private static final String COLOR_BORDER = "#E0E0E0";
    private static final String COLOR_TEXT_HEADER = "#FFFFFF";
    private static final String COLOR_TEXT_CELL = "#333333";
    
    private TableLayout tableLayout;
    private EditText textBox1, textBox2;
    private Button btnParse, btnClear, btnCopyAll, btnToggleFloating;
    private ArrayList<TableRow> dataRows;
    private int selectedRowIndex = -1;
    private int currentAutoFillRow = 0;
    private DataManager dataManager;
    private boolean floatingButtonEnabled = false;
    
    private String[] columnHeaders = {
        "Bank Name", "Applicant Name", "Status", "Reason of CNV", 
        "Latitude", "Longitude", "Area", "KM"
    };
    
    // Premium Column Widths
    private int[] columnWidths = {180, 350, 120, 180, 150, 250, 120, 100};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize DataManager
        dataManager = DataManager.getInstance(this);
        
        // Initialize views
        initializeViews();
        
        // Create premium table
        createPremiumTable();
        
        // Load saved data
        loadTableFromDataManager();
        
        // Set up button listeners
        setupButtonListeners();
    }
    
    private void initializeViews() {
        tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        textBox1 = (EditText) findViewById(R.id.textBox1);
        textBox2 = (EditText) findViewById(R.id.textBox2);
        btnParse = (Button) findViewById(R.id.btnParse);
        btnClear = (Button) findViewById(R.id.btnClear);
        btnCopyAll = (Button) findViewById(R.id.btnCopyAll);
        btnToggleFloating = (Button) findViewById(R.id.btnToggleFloating);
        
        dataRows = new ArrayList<TableRow>();
    }
    
    private void setupButtonListeners() {
        btnParse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parseAndFillData();
            }
        });
        
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearInputs();
            }
        });
        
        btnCopyAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAllFilledRows();
            }
        });
        
        btnToggleFloating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFloatingButton();
            }
        });
    }
    
    private void createPremiumTable() {
        // Create premium header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor(COLOR_HEADER));
        headerRow.setPadding(0, 0, 0, 0);
        
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = createPremiumHeaderCell(columnHeaders[i], columnWidths[i]);
            headerRow.addView(tv);
        }
        tableLayout.addView(headerRow);
        
        // Create 10 empty data rows with premium styling
        String[] emptyRow = {"", "", "", "", "", "", "", ""};
        
        for (int i = 0; i < 10; i++) {
            TableRow dataRow = createPremiumDataRow(i, emptyRow);
            dataRows.add(dataRow);
            tableLayout.addView(dataRow);
        }
    }
    
    private TextView createPremiumHeaderCell(String text, int width) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 14, 16, 14);
        tv.setTextSize(13);
        tv.setTextColor(Color.parseColor(COLOR_TEXT_HEADER));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setWidth(width);
        tv.setMinWidth(width);
        tv.setMaxLines(2);
        tv.setSingleLine(false);
        
        return tv;
    }
    
    private TableRow createPremiumDataRow(final int rowIndex, String[] data) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.parseColor(COLOR_ROW_DEFAULT));
        row.setPadding(0, 0, 0, 0);
        
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = createPremiumDataCell(data[i], columnWidths[i]);
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
        tv.setText(text);
        tv.setPadding(16, 12, 16, 12);
        tv.setTextSize(12);
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
    
    private void parseAndFillData() {
        String text1 = textBox1.getText().toString().trim();
        
        if (text1.isEmpty()) {
            showPremiumToast("WhatsApp text is required!", false);
            return;
        }
        
        // Auto-select next empty row
        int targetRow = findNextEmptyRow();
        if (targetRow == -1) {
            showPremiumToast("All rows filled! Delete a row first.", false);
            return;
        }
        
        // Parse Text Box 1
        String bankName = extractBankName(text1);
        String reasonOfCNV = extractReasonOfCNV(text1);
        String applicantName = extractApplicantName(text1);
        
        // Parse Text Box 2 (optional)
        String longitude = "";
        String text2 = textBox2.getText().toString().trim();
        if (!text2.isEmpty()) {
            longitude = extractLongitude(text2);
        }
        
        // Fill the row
        fillRow(targetRow, bankName, applicantName, reasonOfCNV, longitude);
        selectRow(targetRow);
        currentAutoFillRow = targetRow + 1;
        
        showPremiumToast("Row " + (targetRow + 1) + " filled!", true);
    }
    
    private int findNextEmptyRow() {
        for (int i = 0; i < dataRows.size(); i++) {
            if (isRowEmpty(i)) {
                return i;
            }
        }
        return -1;
    }
    
    private boolean isRowEmpty(int rowIndex) {
        TableRow row = dataRows.get(rowIndex);
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = (TextView) row.findViewWithTag("col_" + i);
            if (tv != null && !tv.getText().toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    private String extractBankName(String text) {
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    
    private String extractReasonOfCNV(String text) {
        Pattern pattern = Pattern.compile("([^(]+)\\(");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    
    private String extractApplicantName(String text) {
        Pattern pattern = Pattern.compile(
            "(?i)Applic[a-z]*\\s*(?:Name)?\\s*:?\\s*([^\\n\\d]+?)(?=\\n|\\d+\\)|$)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            name = name.replaceAll("[:\\-]+$", "").trim();
            name = name.replaceAll("\\s*\\d+[:\\)].*", "").trim();
            return name;
        }
        return "";
    }
    
    private String extractLongitude(String text) {
        Pattern pattern = Pattern.compile("(\\d+\\.\\d+)N(\\d+\\.\\d+)E");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String lat = matcher.group(1);
            String lon = matcher.group(2);
            return lat + "," + lon;
        }
        return "";
    }
    
    private void fillRow(int rowIndex, String bankName, String applicantName, 
                         String reasonOfCNV, String longitude) {
        TableRow row = dataRows.get(rowIndex);
        
        setTableCell(row, 0, bankName);
        setTableCell(row, 1, applicantName);
        // Column 2 (Status) - blank
        setTableCell(row, 3, reasonOfCNV);
        // Column 4 (Latitude) - blank
        setTableCell(row, 5, longitude);
        // Column 6 (Area) - blank
        // Column 7 (KM) - blank
    }
    
    private void setTableCell(TableRow row, int colIndex, String value) {
        TextView tv = (TextView) row.findViewWithTag("col_" + colIndex);
        if (tv != null) {
            tv.setText(value);
        }
    }
    
    private void copyRowToClipboard(int rowIndex) {
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
        
        showPremiumToast("Row copied! Paste in Excel", true);
    }
    
    private void clearInputs() {
        textBox1.setText("");
        textBox2.setText("");
        showPremiumToast("Inputs cleared", true);
    }
    
    private void loadTableFromDataManager() {
        ArrayList<DataManager.RowData> savedRows = dataManager.getAllRows();
        
        for (int i = 0; i < savedRows.size() && i < dataRows.size(); i++) {
            DataManager.RowData rowData = savedRows.get(i);
            TableRow tableRow = dataRows.get(i);
            
            setTableCell(tableRow, 0, rowData.bankName);
            setTableCell(tableRow, 1, rowData.applicantName);
            setTableCell(tableRow, 2, rowData.status);
            setTableCell(tableRow, 3, rowData.reasonOfCNV);
            setTableCell(tableRow, 4, rowData.latitude);
            setTableCell(tableRow, 5, rowData.longitude);
            setTableCell(tableRow, 6, rowData.area);
            setTableCell(tableRow, 7, rowData.km);
        }
    }
    
    private void toggleFloatingButton() {
        if (floatingButtonEnabled) {
            stopService(new Intent(this, FloatingButtonService.class));
            floatingButtonEnabled = false;
            btnToggleFloating.setText("Enable Quick Entry Mode");
            showPremiumToast("Quick Entry disabled", true);
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
            btnToggleFloating.setText("Disable Quick Entry Mode");
            showPremiumToast("Quick Entry enabled! Minimize app to use.", true);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadTableFromDataManager();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    toggleFloatingButton();
                } else {
                    showPremiumToast("Permission denied!", false);
                }
            }
        }
    }
    
    private void copyAllFilledRows() {
        StringBuilder allData = new StringBuilder();
        int filledCount = 0;
        
        for (int i = 0; i < dataRows.size(); i++) {
            if (!isRowEmpty(i)) {
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
                filledCount++;
            }
        }
        
        if (filledCount == 0) {
            showPremiumToast("No data to copy!", false);
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("All Rows Data", allData.toString());
        clipboard.setPrimaryClip(clip);
        
        showPremiumToast(filledCount + " rows copied!", true);
    }
    
    private void showRowOptionsDialog(final int rowIndex) {
        final CharSequence[] options = {"Edit Row", "Delete Row", "Move Up", "Move Down"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Row " + (rowIndex + 1) + " Options");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        editRow(rowIndex);
                        break;
                    case 1:
                        deleteRow(rowIndex);
                        break;
                    case 2:
                        moveRow(rowIndex, rowIndex - 1);
                        break;
                    case 3:
                        moveRow(rowIndex, rowIndex + 1);
                        break;
                }
            }
        });
        builder.show();
    }
    
    private void editRow(final int rowIndex) {
        final TableRow row = dataRows.get(rowIndex);
        final String[] currentValues = new String[columnHeaders.length];
        
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = (TextView) row.findViewWithTag("col_" + i);
            currentValues[i] = tv != null ? tv.getText().toString() : "";
        }
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        
        final EditText[] editTexts = new EditText[columnHeaders.length];
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView label = new TextView(this);
            label.setText(columnHeaders[i] + ":");
            label.setTextSize(13);
            label.setTextColor(Color.parseColor(COLOR_PRIMARY));
            label.setPadding(0, 16, 0, 4);
            layout.addView(label);
            
            editTexts[i] = new EditText(this);
            editTexts[i].setText(currentValues[i]);
            editTexts[i].setTextSize(14);
            editTexts[i].setPadding(16, 12, 16, 12);
            layout.addView(editTexts[i]);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Row " + (rowIndex + 1));
        builder.setView(layout);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int i = 0; i < columnHeaders.length; i++) {
                    TextView tv = (TextView) row.findViewWithTag("col_" + i);
                    if (tv != null) {
                        tv.setText(editTexts[i].getText().toString());
                    }
                }
                showPremiumToast("Row updated!", true);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void deleteRow(final int rowIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Row?");
        builder.setMessage("Are you sure you want to delete Row " + (rowIndex + 1) + "?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TableRow row = dataRows.get(rowIndex);
                for (int i = 0; i < columnHeaders.length; i++) {
                    TextView tv = (TextView) row.findViewWithTag("col_" + i);
                    if (tv != null) {
                        tv.setText("");
                    }
                }
                updateRowBackground(rowIndex, COLOR_ROW_DEFAULT);
                showPremiumToast("Row deleted!", true);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void moveRow(int fromIndex, int toIndex) {
        if (toIndex < 0 || toIndex >= dataRows.size()) {
            showPremiumToast("Cannot move row!", false);
            return;
        }
        
        TableRow fromRow = dataRows.get(fromIndex);
        TableRow toRow = dataRows.get(toIndex);
        
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView fromTv = (TextView) fromRow.findViewWithTag("col_" + i);
            TextView toTv = (TextView) toRow.findViewWithTag("col_" + i);
            
            if (fromTv != null && toTv != null) {
                String temp = fromTv.getText().toString();
                fromTv.setText(toTv.getText().toString());
                toTv.setText(temp);
            }
        }
        
        showPremiumToast("Row moved!", true);
    }
    
    private void showPremiumToast(String message, boolean isSuccess) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
