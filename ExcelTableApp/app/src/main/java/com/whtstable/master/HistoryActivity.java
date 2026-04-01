package com.whtstable.master;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * History Activity - Shows all entries grouped by date
 */
public class HistoryActivity extends Activity {
    
    private static final String COLOR_HEADER = "#1A237E";
    private static final String COLOR_DATE_HEADER = "#3F51B5";
    private static final String COLOR_ROW_DEFAULT = "#FFFFFF";
    private static final String COLOR_BORDER = "#E0E0E0";
    private static final String COLOR_TEXT_HEADER = "#FFFFFF";
    private static final String COLOR_TEXT_CELL = "#333333";
    
    private LinearLayout tablesContainer;
    private Button btnBack, btnCopyAll;
    private TextView tvTitle;
    
    private DatabaseHelper dbHelper;
    private String currentMonth;
    private String currentYear;
    
    private String[] columnHeaders = {
        "BANK NAME", "APPLICANT NAME", "STATUS", "REASON OF CNV", 
        "LATLONG FROM", "LATLONG TO", "AREA", "KM"
    };
    
    private int[] columnWidths = {150, 300, 100, 150, 180, 180, 120, 80};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        dbHelper = DatabaseHelper.getInstance(this);
        
        // Get month/year from intent
        currentMonth = getIntent().getStringExtra("month");
        currentYear = getIntent().getStringExtra("year");
        
        if (currentMonth == null) currentMonth = dbHelper.getSelectedMonth();
        if (currentYear == null) currentYear = dbHelper.getSelectedYear();
        
        initializeViews();
        setupListeners();
        loadHistory();
    }
    
    private void initializeViews() {
        tablesContainer = (LinearLayout) findViewById(R.id.tablesContainer);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnCopyAll = (Button) findViewById(R.id.btnCopyAll);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        
        tvTitle.setText("HISTORY: " + currentMonth.toUpperCase() + " " + currentYear);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnCopyAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAllData();
            }
        });
    }
    
    private void loadHistory() {
        tablesContainer.removeAllViews();
        
        // Get entries grouped by date
        LinkedHashMap<String, ArrayList<DatabaseHelper.EntryData>> groupedEntries = 
            dbHelper.getEntriesGroupedByDate(currentMonth, currentYear);
        
        if (groupedEntries.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No entries found for " + currentMonth + " " + currentYear);
            emptyText.setTextSize(16);
            emptyText.setTextColor(Color.parseColor("#757575"));
            emptyText.setPadding(32, 64, 32, 64);
            tablesContainer.addView(emptyText);
            return;
        }
        
        // Create a table for each date
        for (Map.Entry<String, ArrayList<DatabaseHelper.EntryData>> entry : groupedEntries.entrySet()) {
            String date = entry.getKey();
            ArrayList<DatabaseHelper.EntryData> entries = entry.getValue();
            
            // Create date header
            LinearLayout dateSection = createDateSection(date, entries);
            tablesContainer.addView(dateSection);
        }
    }
    
    private LinearLayout createDateSection(String date, ArrayList<DatabaseHelper.EntryData> entries) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, 0, 0, 16);
        
        // Date Header
        TextView dateHeader = new TextView(this);
        dateHeader.setText("DATE :- " + date);
        dateHeader.setTextSize(14);
        dateHeader.setTextStyle(Typeface.BOLD);
        dateHeader.setTextColor(Color.WHITE);
        dateHeader.setBackgroundColor(Color.parseColor(COLOR_DATE_HEADER));
        dateHeader.setPadding(16, 12, 16, 12);
        section.addView(dateHeader);
        
        // Entry count
        TextView countText = new TextView(this);
        countText.setText(entries.size() + " entries");
        countText.setTextSize(11);
        countText.setTextColor(Color.parseColor("#757575"));
        countText.setPadding(16, 4, 16, 8);
        section.addView(countText);
        
        // Scrollable table container
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setFillViewport(false);
        hScroll.setHorizontalScrollBarEnabled(true);
        
        // Create table
        TableLayout table = new TableLayout(this);
        table.setBackgroundColor(Color.WHITE);
        
        // Add header row
        TableRow headerRow = createHeaderRow();
        table.addView(headerRow);
        
        // Add data rows
        for (DatabaseHelper.EntryData entryData : entries) {
            TableRow dataRow = createDataRow(entryData);
            table.addView(dataRow);
        }
        
        hScroll.addView(table);
        section.addView(hScroll);
        
        return section;
    }
    
    private TableRow createHeaderRow() {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.parseColor(COLOR_HEADER));
        
        for (int i = 0; i < columnHeaders.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(columnHeaders[i]);
            tv.setPadding(12, 10, 12, 10);
            tv.setTextSize(10);
            tv.setTextColor(Color.parseColor(COLOR_TEXT_HEADER));
            tv.setTypeface(null, Typeface.BOLD);
            tv.setWidth(columnWidths[i]);
            tv.setMinWidth(columnWidths[i]);
            row.addView(tv);
        }
        
        return row;
    }
    
    private TableRow createDataRow(DatabaseHelper.EntryData entry) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.parseColor(COLOR_ROW_DEFAULT));
        
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
        
        for (int i = 0; i < values.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(values[i] != null ? values[i] : "");
            tv.setPadding(12, 8, 12, 8);
            tv.setTextSize(10);
            tv.setTextColor(Color.parseColor(COLOR_TEXT_CELL));
            tv.setWidth(columnWidths[i]);
            tv.setMinWidth(columnWidths[i]);
            tv.setMaxLines(2);
            
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.parseColor(COLOR_ROW_DEFAULT));
            border.setStroke(1, Color.parseColor(COLOR_BORDER));
            tv.setBackground(border);
            
            row.addView(tv);
        }
        
        return row;
    }
    
    private void copyAllData() {
        LinkedHashMap<String, ArrayList<DatabaseHelper.EntryData>> groupedEntries = 
            dbHelper.getEntriesGroupedByDate(currentMonth, currentYear);
        
        if (groupedEntries.isEmpty()) {
            Toast.makeText(this, "No data to copy!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder allData = new StringBuilder();
        int totalEntries = 0;
        
        for (Map.Entry<String, ArrayList<DatabaseHelper.EntryData>> entry : groupedEntries.entrySet()) {
            String date = entry.getKey();
            ArrayList<DatabaseHelper.EntryData> entries = entry.getValue();
            
            // Add date header
            allData.append("\nDATE :- " + date + "\n");
            
            // Add column headers
            for (int i = 0; i < columnHeaders.length; i++) {
                allData.append(columnHeaders[i]);
                if (i < columnHeaders.length - 1) allData.append("\t");
            }
            allData.append("\n");
            
            // Add data
            for (DatabaseHelper.EntryData entryData : entries) {
                allData.append(entryData.bankName != null ? entryData.bankName : "").append("\t");
                allData.append(entryData.applicantName != null ? entryData.applicantName : "").append("\t");
                allData.append(entryData.status != null ? entryData.status : "").append("\t");
                allData.append(entryData.reasonCnv != null ? entryData.reasonCnv : "").append("\t");
                allData.append(entryData.latlongFrom != null ? entryData.latlongFrom : "").append("\t");
                allData.append(entryData.latlongTo != null ? entryData.latlongTo : "").append("\t");
                allData.append(entryData.area != null ? entryData.area : "").append("\t");
                allData.append(entryData.km != null ? entryData.km : "").append("\n");
                totalEntries++;
            }
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("History Data", allData.toString());
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(this, totalEntries + " entries copied!", Toast.LENGTH_SHORT).show();
    }
}
