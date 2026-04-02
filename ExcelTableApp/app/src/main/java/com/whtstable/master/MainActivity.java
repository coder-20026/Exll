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
import android.view.MotionEvent;
import android.os.Handler;
import android.os.Looper;

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
    
    // Excel-style Multi-Cell Selection
    private CellSelectionManager selectionManager;
    private boolean isDraggingSelection = false;
    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private static final long LONG_PRESS_DELAY = 300; // ms for drag selection start
    private int touchStartRow = -1;
    private int touchStartCol = -1;
    
    // Selection info display
    private TextView tvSelectionInfo;
    
    // Drag Handle View for selection expansion
    private View dragHandleView;
    private boolean isDraggingHandle = false;
    
    // Row header dragging for multi-row selection
    private boolean isRowHeaderDragging = false;
    private int rowDragStartRow = -1;
    
    // Column header dragging for multi-column selection
    private boolean isColumnHeaderDragging = false;
    private int columnDragStartCol = -1;
    
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
        
        // Initialize Cell Selection Manager
        selectionManager = CellSelectionManager.getInstance();
        setupSelectionListener();
        
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
        tvSelectionInfo = (TextView) findViewById(R.id.tvSelectionInfo);
        
        // Border Panel Container
        borderPanelContainer = findViewById(R.id.borderPanelContainer);
        
        allDataRows = new ArrayList<LinearLayout>();
        
        // Setup header row 1 cells (Field Executive Name, Date)
        setupHeaderRow1Selection();
        
        // Setup column header click listeners for column selection
        setupColumnHeaderSelection();
        
        // Setup summary table cell selection
        setupSummaryTableSelection();
    }
    
    /**
     * Setup touch listeners for header row 1 cells (Field Executive Name, Date)
     * TAP = Edit mode
     * LONG PRESS = Selection mode
     */
    private void setupHeaderRow1Selection() {
        // Header row 1 cells
        final EditText[] headerCells = {cellA1F1, cellG1I1};
        final int[] colStarts = {0, 6}; // Column A-F merged, Column G-I merged
        
        for (int i = 0; i < headerCells.length; i++) {
            final EditText cell = headerCells[i];
            final int colIndex = colStarts[i];
            
            if (cell == null) continue;
            
            // Store original click listener
            cell.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // Long press = select this header cell
                    selectionManager.selectHeaderCell(colIndex);
                    showToast("Header cell selected", true);
                    return true;
                }
            });
            
            // Single click just focuses for edit (default behavior)
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Clear any selection
                    selectionManager.clearSelection();
                    // Focus for editing
                    cell.requestFocus();
                }
            });
        }
    }
    
    /**
     * Setup touch listeners for summary table cells
     * TAP = Edit mode (focus for editing)
     * LONG PRESS = Selection mode
     */
    private void setupSummaryTableSelection() {
        // Summary table cells with their logical positions (row offset from main data)
        // Row 15 = TOTAL KM, Row 16 = LUNCH, Row 17 = VISIT, Row 18 = OTHER, Row 19 = TOTAL AMOUNT
        
        final View[][] summaryCells = {
            {cellTotalKmCount, cellTotalKmAmount},   // Row 15 (index 0)
            {cellLunchCount, cellLunchAmount},       // Row 16 (index 1)
            {cellVisitCount, cellVisitAmount},       // Row 17 (index 2)
            {cellOtherCount, cellOtherAmount},       // Row 18 (index 3)
            {null, cellFinalTotal}                    // Row 19 (index 4) - merged cell
        };
        
        for (int rowIdx = 0; rowIdx < summaryCells.length; rowIdx++) {
            final int summaryRow = rowIdx;
            
            for (int colIdx = 0; colIdx < summaryCells[rowIdx].length; colIdx++) {
                final View cell = summaryCells[rowIdx][colIdx];
                final int summaryCol = colIdx + 6; // Summary table starts at column G (index 6)
                
                if (cell != null) {
                    // SINGLE TAP = Edit mode
                    cell.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Clear any existing selection
                            selectionManager.clearSelection();
                            
                            // Focus for editing (if TextView, show edit dialog)
                            if (cell instanceof TextView) {
                                showSummaryCellEditDialog((TextView) cell, summaryRow, summaryCol);
                            }
                        }
                    });
                    
                    // LONG PRESS = Selection mode
                    cell.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            // Start selection on this summary cell
                            selectionManager.startSummaryDragSelection(summaryRow, summaryCol);
                            showToast("Selection mode - drag to select more", true);
                            return true;
                        }
                    });
                    
                    // Touch listener for drag selection after long press
                    cell.setOnTouchListener(new View.OnTouchListener() {
                        private boolean isInSelectionMode = false;
                        
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (selectionManager.isSelecting()) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_MOVE:
                                        // Update drag selection
                                        int[] pos = getSummaryCellFromTouch(summaryCells, event.getRawX(), event.getRawY());
                                        if (pos != null) {
                                            int actualRow = CellSelectionManager.SUMMARY_START_ROW + pos[0];
                                            selectionManager.updateDragSelection(actualRow, pos[1] + 6);
                                        }
                                        return true;
                                        
                                    case MotionEvent.ACTION_UP:
                                    case MotionEvent.ACTION_CANCEL:
                                        selectionManager.endDragSelection();
                                        int count = selectionManager.getSelectedCellCount();
                                        if (count > 1) {
                                            showToast(count + " cells selected", true);
                                        }
                                        return true;
                                }
                            }
                            return false; // Let click/long click handle it
                        }
                    });
                    
                    cell.setClickable(true);
                    cell.setFocusable(true);
                    cell.setLongClickable(true);
                }
            }
        }
    }
    
    /**
     * Show edit dialog for summary cell
     */
    private void showSummaryCellEditDialog(final TextView cell, int row, int col) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Value");
        
        final EditText input = new EditText(this);
        input.setText(cell.getText());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSelection(input.getText().length());
        builder.setView(input);
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cell.setText(input.getText().toString());
                calculateTotalAmount();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Get summary cell position from touch coordinates
     */
    private int[] getSummaryCellFromTouch(View[][] summaryCells, float rawX, float rawY) {
        for (int r = 0; r < summaryCells.length; r++) {
            for (int c = 0; c < summaryCells[r].length; c++) {
                View cell = summaryCells[r][c];
                if (cell != null) {
                    int[] loc = new int[2];
                    cell.getLocationOnScreen(loc);
                    if (rawX >= loc[0] && rawX < loc[0] + cell.getWidth() &&
                        rawY >= loc[1] && rawY < loc[1] + cell.getHeight()) {
                        return new int[]{r, c};
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Setup column header selection
     * TAP = Edit header text
     * LONG PRESS = Column selection mode (drag for multi-column)
     */
    private void setupColumnHeaderSelection() {
        // Find the row2Headers (column header row)
        final LinearLayout row2Headers = (LinearLayout) findViewById(R.id.row2Headers);
        if (row2Headers == null) return;
        
        // Add touch listeners to each column header
        for (int i = 0; i < row2Headers.getChildCount(); i++) {
            final int colIndex = i;
            final View header = row2Headers.getChildAt(i);
            
            // SINGLE TAP = Edit header (if editable)
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Clear selection
                    selectionManager.clearSelection();
                    
                    // Focus for editing if it's an EditText
                    if (header instanceof EditText) {
                        header.requestFocus();
                    } else if (header instanceof TextView) {
                        showHeaderEditDialog((TextView) header, colIndex);
                    }
                }
            });
            
            // LONG PRESS = Start column selection
            header.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // Start column selection
                    columnDragStartCol = colIndex;
                    isColumnHeaderDragging = false;
                    selectionManager.startColumnDrag(colIndex);
                    showToast("Column " + (char)('A' + colIndex) + " - drag to select more", true);
                    return true;
                }
            });
            
            // Touch listener for drag after long press
            header.setOnTouchListener(new View.OnTouchListener() {
                private float startX;
                private boolean longPressTriggered = false;
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX();
                            longPressTriggered = false;
                            return false; // Let click/long click handle initial touch
                            
                        case MotionEvent.ACTION_MOVE:
                            // Only handle move if we're in selection mode
                            if (selectionManager.isSelecting() && selectionManager.getSelectionMode() == CellSelectionManager.MODE_COLUMN) {
                                float dx = Math.abs(event.getRawX() - startX);
                                if (dx > dpToPx(10)) {
                                    isColumnHeaderDragging = true;
                                }
                                
                                if (isColumnHeaderDragging) {
                                    int newCol = getColumnFromX(row2Headers, event.getRawX());
                                    if (newCol >= 0) {
                                        selectionManager.updateColumnDrag(newCol);
                                    }
                                }
                                return true;
                            }
                            return false;
                            
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isColumnHeaderDragging) {
                                selectionManager.endDragSelection();
                                int colCount = selectionManager.getSelectedColumnCount();
                                if (colCount > 1) {
                                    showToast(colCount + " columns selected", true);
                                }
                                isColumnHeaderDragging = false;
                                return true;
                            }
                            return false;
                    }
                    return false;
                }
            });
            
            // Make headers interactive
            header.setClickable(true);
            header.setFocusable(true);
            header.setLongClickable(true);
        }
    }
    
    /**
     * Show edit dialog for header cell
     */
    private void showHeaderEditDialog(final TextView header, int colIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Header - Column " + (char)('A' + colIndex));
        
        final EditText input = new EditText(this);
        input.setText(header.getText());
        input.setSelection(input.getText().length());
        builder.setView(input);
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                header.setText(input.getText().toString());
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Get column index from X coordinate in header row
     */
    private int getColumnFromX(LinearLayout headerRow, float rawX) {
        int[] rowLocation = new int[2];
        headerRow.getLocationOnScreen(rowLocation);
        float localX = rawX - rowLocation[0];
        
        float accumulatedWidth = 0;
        for (int i = 0; i < headerRow.getChildCount(); i++) {
            View child = headerRow.getChildAt(i);
            accumulatedWidth += child.getWidth();
            if (localX < accumulatedWidth) {
                return i;
            }
        }
        return headerRow.getChildCount() - 1;
    }
    
    // ==================== EXCEL-STYLE SELECTION SYSTEM ====================
    
    private void setupSelectionListener() {
        selectionManager.setSelectionChangeListener(new CellSelectionManager.SelectionChangeListener() {
            @Override
            public void onSelectionChanged(int startRow, int endRow, int startCol, int endCol, int mode) {
                updateSelectionVisuals();
                updateSelectionInfoDisplay();
                
                // Sync with BorderManager for border operations
                borderManager.setSelection(startRow, endRow, startCol, endCol);
            }
            
            @Override
            public void onSelectionCleared() {
                clearSelectionVisuals();
                updateSelectionInfoDisplay();
                borderManager.clearSelection();
                hideDragHandle();
            }
            
            @Override
            public void onDragHandleVisible(int row, int col) {
                showDragHandle(row, col);
            }
        });
    }
    
    // ==================== DRAG HANDLE METHODS ====================
    
    private void showDragHandle(int row, int col) {
        if (row < 0 || row >= allDataRows.size() || col < 0) {
            hideDragHandle();
            return;
        }
        
        LinearLayout rowLayout = allDataRows.get(row);
        if (col >= rowLayout.getChildCount()) {
            hideDragHandle();
            return;
        }
        
        View cell = rowLayout.getChildAt(col);
        if (cell == null) {
            hideDragHandle();
            return;
        }
        
        // Create drag handle if not exists
        if (dragHandleView == null) {
            dragHandleView = new View(this);
            dragHandleView.setBackgroundResource(R.drawable.excel_drag_handle);
            
            // Set touch listener for drag handle
            dragHandleView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isDraggingHandle = true;
                            selectionManager.startDragHandle();
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            if (isDraggingHandle) {
                                int[] pos = getCellPositionFromGlobalTouch(event.getRawX(), event.getRawY());
                                if (pos != null) {
                                    selectionManager.updateDragHandle(pos[0], pos[1]);
                                }
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isDraggingHandle) {
                                selectionManager.endDragHandle();
                                isDraggingHandle = false;
                                int count = selectionManager.getSelectedCellCount();
                                if (count > 1) {
                                    showToast(count + " cells selected", true);
                                }
                            }
                            return true;
                    }
                    return false;
                }
            });
        }
        
        // Position drag handle at bottom-right of selection
        // Remove from current parent if any
        if (dragHandleView.getParent() != null) {
            ((android.view.ViewGroup) dragHandleView.getParent()).removeView(dragHandleView);
        }
        
        // Add to the cell's parent (the row) at the correct position
        int handleSize = dpToPx(10);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
            handleSize, handleSize
        );
        
        // Get cell position in screen
        int[] cellLocation = new int[2];
        cell.getLocationOnScreen(cellLocation);
        
        // Add handle to root view
        android.view.ViewGroup rootView = (android.view.ViewGroup) getWindow().getDecorView();
        android.widget.FrameLayout.LayoutParams rootParams = new android.widget.FrameLayout.LayoutParams(
            handleSize, handleSize
        );
        rootParams.leftMargin = cellLocation[0] + cell.getWidth() - handleSize / 2;
        rootParams.topMargin = cellLocation[1] + cell.getHeight() - handleSize / 2;
        
        rootView.addView(dragHandleView, rootParams);
        dragHandleView.setVisibility(View.VISIBLE);
        dragHandleView.bringToFront();
    }
    
    private void hideDragHandle() {
        if (dragHandleView != null && dragHandleView.getParent() != null) {
            ((android.view.ViewGroup) dragHandleView.getParent()).removeView(dragHandleView);
        }
    }
    
    private void updateSelectionVisuals() {
        // Get selection bounds
        int selStartRow = selectionManager.getStartRow();
        int selEndRow = selectionManager.getEndRow();
        int selStartCol = selectionManager.getStartCol();
        int selEndCol = selectionManager.getEndCol();
        
        // Reset all cells and apply selection visuals
        for (int rowIndex = 0; rowIndex < allDataRows.size(); rowIndex++) {
            LinearLayout row = allDataRows.get(rowIndex);
            boolean isLastRow = (rowIndex == TOTAL_DATA_ROWS - 1);
            
            for (int colIndex = 0; colIndex < row.getChildCount(); colIndex++) {
                View cell = row.getChildAt(colIndex);
                if (cell != null) {
                    if (selectionManager.isCellSelected(rowIndex, colIndex)) {
                        // Check if this is the active cell (where selection started)
                        if (selectionManager.isActiveCell(rowIndex, colIndex)) {
                            cell.setBackgroundResource(R.drawable.excel_cell_active);
                        } else {
                            // Check if cell is on the outer edge of selection for thick border
                            boolean isTopEdge = (rowIndex == selStartRow);
                            boolean isBottomEdge = (rowIndex == selEndRow);
                            boolean isLeftEdge = (colIndex == selStartCol);
                            boolean isRightEdge = (colIndex == selEndCol);
                            
                            // Use outer border drawable for edge cells
                            if (isTopEdge || isBottomEdge || isLeftEdge || isRightEdge) {
                                cell.setBackgroundResource(R.drawable.excel_selection_outer_border);
                            } else {
                                cell.setBackgroundResource(R.drawable.excel_cell_multi_selected);
                            }
                        }
                    } else {
                        // Apply default or saved border
                        applyCellBorderDrawable(rowIndex, colIndex, cell);
                    }
                }
            }
        }
        
        // Update summary table selection visuals
        updateSummarySelectionVisuals();
        
        // Update header row 1 selection visuals
        updateHeaderRow1SelectionVisuals();
    }
    
    /**
     * Update selection visuals for header row 1 cells
     */
    private void updateHeaderRow1SelectionVisuals() {
        EditText[] headerCells = {cellA1F1, cellG1I1};
        int[] colStarts = {0, 6};
        
        for (int i = 0; i < headerCells.length; i++) {
            EditText cell = headerCells[i];
            int col = colStarts[i];
            
            if (cell != null) {
                if (selectionManager.isCellSelected(CellSelectionManager.HEADER_ROW, col)) {
                    if (selectionManager.isActiveCell(CellSelectionManager.HEADER_ROW, col)) {
                        cell.setBackgroundResource(R.drawable.excel_cell_active);
                    } else {
                        cell.setBackgroundResource(R.drawable.excel_cell_multi_selected);
                    }
                } else {
                    // Reset to default header background
                    cell.setBackgroundResource(R.drawable.excel_header_cell);
                }
            }
        }
    }
    
    /**
     * Update selection visuals for summary table cells
     */
    private void updateSummarySelectionVisuals() {
        // Summary cells array
        View[][] summaryCells = {
            {cellTotalKmCount, cellTotalKmAmount},
            {cellLunchCount, cellLunchAmount},
            {cellVisitCount, cellVisitAmount},
            {cellOtherCount, cellOtherAmount},
            {null, cellFinalTotal}
        };
        
        for (int rowIdx = 0; rowIdx < summaryCells.length; rowIdx++) {
            int actualRow = CellSelectionManager.SUMMARY_START_ROW + rowIdx;
            
            for (int colIdx = 0; colIdx < summaryCells[rowIdx].length; colIdx++) {
                View cell = summaryCells[rowIdx][colIdx];
                int actualCol = colIdx + 6; // Summary starts at column G
                
                if (cell != null) {
                    if (selectionManager.isCellSelected(actualRow, actualCol)) {
                        if (selectionManager.isActiveCell(actualRow, actualCol)) {
                            cell.setBackgroundResource(R.drawable.excel_cell_active);
                        } else {
                            cell.setBackgroundResource(R.drawable.excel_cell_multi_selected);
                        }
                    } else {
                        // Reset to default summary cell background
                        cell.setBackgroundResource(R.drawable.excel_summary_inner);
                    }
                }
            }
        }
    }
    
    private void clearSelectionVisuals() {
        // Reset all data cells to default border
        for (int rowIndex = 0; rowIndex < allDataRows.size(); rowIndex++) {
            LinearLayout row = allDataRows.get(rowIndex);
            for (int colIndex = 0; colIndex < row.getChildCount(); colIndex++) {
                View cell = row.getChildAt(colIndex);
                if (cell != null) {
                    applyCellBorderDrawable(rowIndex, colIndex, cell);
                }
            }
        }
        
        // Reset header row 1 cells
        if (cellA1F1 != null) cellA1F1.setBackgroundResource(R.drawable.excel_header_cell);
        if (cellG1I1 != null) cellG1I1.setBackgroundResource(R.drawable.excel_header_cell);
        
        // Reset summary table cells
        View[][] summaryCells = {
            {cellTotalKmCount, cellTotalKmAmount},
            {cellLunchCount, cellLunchAmount},
            {cellVisitCount, cellVisitAmount},
            {cellOtherCount, cellOtherAmount},
            {null, cellFinalTotal}
        };
        
        for (View[] row : summaryCells) {
            for (View cell : row) {
                if (cell != null) {
                    cell.setBackgroundResource(R.drawable.excel_summary_inner);
                }
            }
        }
    }
    
    private void updateSelectionInfoDisplay() {
        if (tvSelectionInfo != null) {
            if (selectionManager.hasSelection()) {
                String selStr = selectionManager.getSelectionString();
                int count = selectionManager.getSelectedCellCount();
                int mode = selectionManager.getSelectionMode();
                
                String modeStr = "";
                switch (mode) {
                    case CellSelectionManager.MODE_SINGLE_CELL:
                        modeStr = "Cell";
                        break;
                    case CellSelectionManager.MODE_ROW:
                        int rowCount = selectionManager.getSelectedRowCount();
                        modeStr = rowCount + " Row" + (rowCount > 1 ? "s" : "");
                        break;
                    case CellSelectionManager.MODE_COLUMN:
                        int colCount = selectionManager.getSelectedColumnCount();
                        modeStr = colCount + " Col" + (colCount > 1 ? "s" : "");
                        break;
                    case CellSelectionManager.MODE_MULTI_CELL:
                    case CellSelectionManager.MODE_DRAG_HANDLE:
                        modeStr = count + " cells";
                        break;
                }
                
                tvSelectionInfo.setText(selStr + " | " + modeStr);
                tvSelectionInfo.setVisibility(View.VISIBLE);
            } else {
                tvSelectionInfo.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Get cell position from touch coordinates
     */
    private int[] getCellPositionFromTouch(View rowView, float x) {
        int rowIndex = allDataRows.indexOf(rowView);
        if (rowIndex < 0) return null;
        
        LinearLayout row = (LinearLayout) rowView;
        int colIndex = -1;
        float accumulatedWidth = 0;
        
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            accumulatedWidth += child.getWidth();
            if (x < accumulatedWidth) {
                colIndex = i;
                break;
            }
        }
        
        if (colIndex < 0) {
            colIndex = row.getChildCount() - 1; // Last column
        }
        
        return new int[]{rowIndex, colIndex};
    }
    
    /**
     * Get cell position from global touch coordinates within dataRowsContainer
     */
    private int[] getCellPositionFromGlobalTouch(float rawX, float rawY) {
        if (dataRowsContainer == null || allDataRows.isEmpty()) return null;
        
        int[] containerLocation = new int[2];
        dataRowsContainer.getLocationOnScreen(containerLocation);
        
        float localX = rawX - containerLocation[0];
        float localY = rawY - containerLocation[1];
        
        // Find row
        int rowIndex = -1;
        float accumulatedHeight = 0;
        for (int i = 0; i < allDataRows.size(); i++) {
            accumulatedHeight += allDataRows.get(i).getHeight();
            if (localY < accumulatedHeight) {
                rowIndex = i;
                break;
            }
        }
        
        // Clamp row
        if (rowIndex < 0) {
            if (localY < 0) rowIndex = 0;
            else rowIndex = allDataRows.size() - 1;
        }
        
        // Find column
        LinearLayout row = allDataRows.get(rowIndex);
        int colIndex = -1;
        float accumulatedWidth = 0;
        for (int i = 0; i < row.getChildCount(); i++) {
            accumulatedWidth += row.getChildAt(i).getWidth();
            if (localX < accumulatedWidth) {
                colIndex = i;
                break;
            }
        }
        
        // Clamp column
        if (colIndex < 0) {
            if (localX < 0) colIndex = 0;
            else colIndex = row.getChildCount() - 1;
        }
        
        return new int[]{rowIndex, colIndex};
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
        
        // Row index for touch handling
        final int rowIndexFinal = srNo - 1;
        
        // Setup touch listener for multi-cell selection
        setupRowTouchListener(row, rowIndexFinal);
        
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
    
    /**
     * Setup row touch listener for data cells
     * TAP = Edit mode (focus cell for editing)
     * LONG PRESS = Selection mode (start drag selection)
     * 
     * Special: SR NO column (col 0) long press = row selection with drag support
     */
    private void setupRowTouchListener(final LinearLayout row, final int rowIndex) {
        row.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private boolean isLongPressed = false;
            private boolean isTappedOnRowHeader = false;
            private long touchDownTime = 0;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        isLongPressed = false;
                        isRowHeaderDragging = false;
                        touchDownTime = System.currentTimeMillis();
                        
                        // Get cell position from touch
                        int[] pos = getCellPositionFromTouch(row, event.getX());
                        if (pos != null) {
                            touchStartRow = pos[0];
                            touchStartCol = pos[1];
                            
                            // Check if tapped on row header (column A - SR NO)
                            isTappedOnRowHeader = (pos[1] == 0);
                            
                            // Start long press detection for selection mode
                            final int startRow = touchStartRow;
                            final int startCol = touchStartCol;
                            final boolean isRowHeader = isTappedOnRowHeader;
                            
                            longPressRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    isLongPressed = true;
                                    isDraggingSelection = true;
                                    
                                    if (isRowHeader) {
                                        // Long press on row header = start row selection
                                        rowDragStartRow = rowIndex;
                                        selectionManager.startRowDrag(rowIndex);
                                        showToast("Row " + (rowIndex + 1) + " - drag to select more", true);
                                    } else {
                                        // Long press on data cell = start drag selection
                                        selectionManager.startDragSelection(startRow, startCol);
                                        showToast("Selection mode - drag to select", true);
                                    }
                                }
                            };
                            longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DELAY);
                        }
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getRawX() - startX);
                        float dy = Math.abs(event.getRawY() - startY);
                        
                        // If moved before long press, cancel long press (user is scrolling)
                        if (!isLongPressed && (dx > dpToPx(15) || dy > dpToPx(15))) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                        }
                        
                        // Handle drag after long press triggered selection
                        if (isLongPressed && isDraggingSelection) {
                            if (isTappedOnRowHeader) {
                                // Row header drag for multi-row selection
                                if (dy > dpToPx(10)) {
                                    isRowHeaderDragging = true;
                                }
                                if (isRowHeaderDragging) {
                                    int[] currentPos = getCellPositionFromGlobalTouch(event.getRawX(), event.getRawY());
                                    if (currentPos != null) {
                                        selectionManager.updateRowDrag(currentPos[0]);
                                    }
                                }
                            } else {
                                // Normal cell drag selection
                                int[] currentPos = getCellPositionFromGlobalTouch(event.getRawX(), event.getRawY());
                                if (currentPos != null) {
                                    selectionManager.updateDragSelection(currentPos[0], currentPos[1]);
                                }
                            }
                            return true;
                        }
                        break;
                        
                    case MotionEvent.ACTION_UP:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        
                        long touchDuration = System.currentTimeMillis() - touchDownTime;
                        float totalDx = Math.abs(event.getRawX() - startX);
                        float totalDy = Math.abs(event.getRawY() - startY);
                        boolean wasTap = (touchDuration < LONG_PRESS_DELAY && totalDx < dpToPx(10) && totalDy < dpToPx(10));
                        
                        // Handle drag selection end
                        if (isDraggingSelection) {
                            selectionManager.endDragSelection();
                            isDraggingSelection = false;
                            
                            int count = selectionManager.getSelectedCellCount();
                            if (count > 1) {
                                showToast(count + " cells selected", true);
                            }
                            
                            isRowHeaderDragging = false;
                            isTappedOnRowHeader = false;
                            return true;
                        }
                        
                        // TAP = Edit mode (only if not long pressed)
                        if (wasTap && !isLongPressed) {
                            int[] tapPos = getCellPositionFromTouch(row, event.getX());
                            if (tapPos != null) {
                                // Clear any existing selection
                                selectionManager.clearSelection();
                                
                                // Focus the cell for editing
                                View cell = row.getChildAt(tapPos[1]);
                                if (cell instanceof EditText) {
                                    ((EditText) cell).requestFocus();
                                    // Show keyboard
                                    android.view.inputmethod.InputMethodManager imm = 
                                        (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    if (imm != null) {
                                        imm.showSoftInput(cell, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                                    }
                                }
                            }
                        }
                        
                        isRowHeaderDragging = false;
                        isTappedOnRowHeader = false;
                        return true;
                        
                    case MotionEvent.ACTION_CANCEL:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        isDraggingSelection = false;
                        isRowHeaderDragging = false;
                        isTappedOnRowHeader = false;
                        break;
                }
                return false;
            }
        });
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
        // Clear selection
        selectionManager.clearSelection();
        
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
        // Use new selection manager for row selection
        selectionManager.selectRow(rowIndex);
        selectedRowIndex = rowIndex;
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
    
    /**
     * Copy selected cells to clipboard (Excel-style multi-cell copy)
     */
    private void copySelectionToClipboard() {
        if (!selectionManager.hasSelection()) {
            showToast("No cells selected", false);
            return;
        }
        
        int startRow = selectionManager.getStartRow();
        int endRow = selectionManager.getEndRow();
        int startCol = selectionManager.getStartCol();
        int endCol = selectionManager.getEndCol();
        
        StringBuilder data = new StringBuilder();
        
        for (int row = startRow; row <= endRow; row++) {
            if (row >= allDataRows.size()) break;
            LinearLayout rowLayout = allDataRows.get(row);
            
            for (int col = startCol; col <= endCol; col++) {
                if (col > startCol) {
                    data.append("\t"); // Tab separator for Excel
                }
                
                View cell = rowLayout.getChildAt(col);
                if (cell instanceof TextView) {
                    data.append(((TextView) cell).getText().toString());
                } else if (cell instanceof EditText) {
                    data.append(((EditText) cell).getText().toString());
                }
            }
            
            if (row < endRow) {
                data.append("\n"); // New line for next row
            }
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Excel Selection", data.toString());
        clipboard.setPrimaryClip(clip);
        
        int cellCount = selectionManager.getSelectedCellCount();
        showToast(cellCount + " cells copied!", true);
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
        // Build options based on selection state
        ArrayList<CharSequence> optionsList = new ArrayList<CharSequence>();
        optionsList.add("Copy Row");
        optionsList.add("Delete Row");
        
        // Add copy selection option if multiple cells are selected
        if (selectionManager.hasSelection() && selectionManager.getSelectedCellCount() > 1) {
            optionsList.add("Copy Selection (" + selectionManager.getSelectedCellCount() + " cells)");
        }
        
        optionsList.add("Copy All Data");
        optionsList.add("Clear Selection");
        
        final CharSequence[] options = optionsList.toArray(new CharSequence[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ROW OPTIONS");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selected = options[which].toString();
                
                if (selected.equals("Copy Row")) {
                    copyRowToClipboard(rowIndex);
                } else if (selected.equals("Delete Row")) {
                    deleteRow(rowIndex);
                } else if (selected.startsWith("Copy Selection")) {
                    copySelectionToClipboard();
                } else if (selected.equals("Copy All Data")) {
                    copyAllFilledRows();
                } else if (selected.equals("Clear Selection")) {
                    selectionManager.clearSelection();
                    showToast("Selection cleared", true);
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
        // Set new selection using selection manager
        selectedCellRow = row;
        selectedCellCol = col;
        selectedCellView = cellView;
        
        // Use selection manager for selection (this will update visuals)
        selectionManager.selectSingleCell(row, col);
    }
    
    private void clearCellSelection() {
        selectedCellRow = -1;
        selectedCellCol = -1;
        selectedCellView = null;
        selectionManager.clearSelection();
    }
    
    private void applyBorderToSelection(int borderType) {
        // Check if we have a selection (using selection manager)
        if (!selectionManager.hasSelection()) {
            showToast("Please select cells first", false);
            return;
        }
        
        // Sync selection from selection manager to border manager
        borderManager.setSelection(
            selectionManager.getStartRow(),
            selectionManager.getEndRow(),
            selectionManager.getStartCol(),
            selectionManager.getEndCol()
        );
        
        // Default border style is THIN
        int borderStyle = CellBorder.STYLE_THIN;
        
        // Apply border type to selection
        borderManager.applyBorderType(borderType, borderStyle);
        
        // Refresh cell borders visually
        applyBordersToAllCells();
        
        // Update selection visuals to show applied borders
        updateSelectionVisuals();
        
        // Show feedback
        int cellCount = selectionManager.getSelectedCellCount();
        String message = borderType == BorderManager.BORDER_TYPE_NONE ? 
            "Borders cleared (" + cellCount + " cells)" : 
            "Border applied (" + cellCount + " cells)";
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
