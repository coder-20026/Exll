package com.whtstable.master;

/**
 * CellSelectionManager - Excel-style multi-cell selection system
 * Supports:
 * - Single cell selection
 * - Multi-cell drag selection (rectangular area)
 * - Row header drag for multi-row selection
 * - Column header drag for multi-column selection
 * - Drag handle for expanding selection
 * - Header and Summary table selection
 */
public class CellSelectionManager {
    
    private static CellSelectionManager instance;
    
    // Selection bounds (normalized - always startRow <= endRow, startCol <= endCol)
    private int startRow = -1;
    private int endRow = -1;
    private int startCol = -1;
    private int endCol = -1;
    
    // Original touch start position (before normalization)
    private int touchStartRow = -1;
    private int touchStartCol = -1;
    
    // Selection state
    private boolean isSelecting = false;
    private boolean hasSelection = false;
    
    // Selection mode
    public static final int MODE_NONE = 0;
    public static final int MODE_SINGLE_CELL = 1;
    public static final int MODE_MULTI_CELL = 2;
    public static final int MODE_ROW = 3;
    public static final int MODE_COLUMN = 4;
    public static final int MODE_DRAG_HANDLE = 5;  // NEW: Drag handle mode
    
    private int selectionMode = MODE_NONE;
    
    // Grid bounds - Extended to include header (row -1) and summary table (rows 15-19)
    public static final int TOTAL_DATA_ROWS = 15;     // Main data rows (0-14)
    public static final int TOTAL_COLUMNS = 9;
    
    // Extended grid for header + summary
    public static final int HEADER_ROW = -1;          // Column headers row
    public static final int SUMMARY_START_ROW = 15;   // Summary table starts
    public static final int SUMMARY_END_ROW = 19;     // Summary table ends
    public static final int MIN_ROW = -1;             // Include header
    public static final int MAX_ROW = 19;             // Include summary
    
    // Current grid mode (which part of the table is being selected)
    public static final int GRID_MAIN = 0;
    public static final int GRID_HEADER = 1;
    public static final int GRID_SUMMARY = 2;
    private int currentGridMode = GRID_MAIN;
    
    // Active cell (the cell where selection started - shows darker border)
    private int activeCellRow = -1;
    private int activeCellCol = -1;
    
    // Drag handle position (bottom-right of selection)
    private boolean isDraggingHandle = false;
    
    // Selection change listener
    public interface SelectionChangeListener {
        void onSelectionChanged(int startRow, int endRow, int startCol, int endCol, int mode);
        void onSelectionCleared();
        void onDragHandleVisible(int row, int col);  // NEW: Notify when drag handle should show
    }
    
    private SelectionChangeListener listener;
    
    private CellSelectionManager() {
        // Private constructor for singleton
    }
    
    public static synchronized CellSelectionManager getInstance() {
        if (instance == null) {
            instance = new CellSelectionManager();
        }
        return instance;
    }
    
    public void setSelectionChangeListener(SelectionChangeListener listener) {
        this.listener = listener;
    }
    
    // ==================== SINGLE CELL SELECTION ====================
    
    /**
     * Select a single cell (user tap)
     */
    public void selectSingleCell(int row, int col) {
        clearSelectionInternal();
        
        if (isValidCell(row, col)) {
            startRow = row;
            endRow = row;
            startCol = col;
            endCol = col;
            touchStartRow = row;
            touchStartCol = col;
            activeCellRow = row;
            activeCellCol = col;
            selectionMode = MODE_SINGLE_CELL;
            hasSelection = true;
            currentGridMode = getGridModeForRow(row);
            
            notifySelectionChanged();
            notifyDragHandleVisible();
        }
    }
    
    // ==================== MULTI-CELL DRAG SELECTION ====================
    
    /**
     * Start drag selection (ACTION_DOWN / long press)
     * For free rectangular selection
     */
    public void startDragSelection(int row, int col) {
        if (isValidCell(row, col)) {
            touchStartRow = row;
            touchStartCol = col;
            startRow = row;
            endRow = row;
            startCol = col;
            endCol = col;
            activeCellRow = row;
            activeCellCol = col;
            isSelecting = true;
            hasSelection = true;
            selectionMode = MODE_MULTI_CELL;
            currentGridMode = getGridModeForRow(row);
            isDraggingHandle = false;
            
            notifySelectionChanged();
        }
    }
    
    /**
     * Update drag selection (ACTION_MOVE)
     * Supports reverse drag for rectangular selection
     */
    public void updateDragSelection(int row, int col) {
        if (!isSelecting) return;
        
        // Clamp to valid range based on current grid mode
        row = clampRow(row);
        col = clampCol(col);
        
        // Calculate normalized bounds (handle reverse drag)
        int newStartRow = Math.min(touchStartRow, row);
        int newEndRow = Math.max(touchStartRow, row);
        int newStartCol = Math.min(touchStartCol, col);
        int newEndCol = Math.max(touchStartCol, col);
        
        // Only update if bounds changed
        if (newStartRow != startRow || newEndRow != endRow || 
            newStartCol != startCol || newEndCol != endCol) {
            
            startRow = newStartRow;
            endRow = newEndRow;
            startCol = newStartCol;
            endCol = newEndCol;
            
            notifySelectionChanged();
        }
    }
    
    /**
     * End drag selection (ACTION_UP)
     */
    public void endDragSelection() {
        isSelecting = false;
        isDraggingHandle = false;
        
        // If only one cell selected, switch to single cell mode
        if (startRow == endRow && startCol == endCol) {
            selectionMode = MODE_SINGLE_CELL;
        }
        
        notifySelectionChanged();
        notifyDragHandleVisible();
    }
    
    // ==================== ROW SELECTION (With Multi-Row Drag) ====================
    
    /**
     * Select entire row (tap on row header / SR NO column)
     */
    public void selectRow(int row) {
        clearSelectionInternal();
        
        if (isValidRow(row)) {
            startRow = row;
            endRow = row;
            startCol = 0;
            endCol = TOTAL_COLUMNS - 1;
            touchStartRow = row;
            touchStartCol = 0;
            activeCellRow = row;
            activeCellCol = 0;
            selectionMode = MODE_ROW;
            hasSelection = true;
            currentGridMode = getGridModeForRow(row);
            
            notifySelectionChanged();
            notifyDragHandleVisible();
        }
    }
    
    /**
     * Start row header drag for multi-row selection
     */
    public void startRowDrag(int row) {
        if (isValidRow(row)) {
            touchStartRow = row;
            startRow = row;
            endRow = row;
            startCol = 0;
            endCol = TOTAL_COLUMNS - 1;
            activeCellRow = row;
            activeCellCol = 0;
            isSelecting = true;
            hasSelection = true;
            selectionMode = MODE_ROW;
            currentGridMode = getGridModeForRow(row);
            
            notifySelectionChanged();
        }
    }
    
    /**
     * Update row drag selection (for multi-row selection)
     */
    public void updateRowDrag(int row) {
        if (!isSelecting || selectionMode != MODE_ROW) return;
        
        row = clampRow(row);
        
        int newStartRow = Math.min(touchStartRow, row);
        int newEndRow = Math.max(touchStartRow, row);
        
        if (newStartRow != startRow || newEndRow != endRow) {
            startRow = newStartRow;
            endRow = newEndRow;
            // Keep full row selection
            startCol = 0;
            endCol = TOTAL_COLUMNS - 1;
            
            notifySelectionChanged();
        }
    }
    
    /**
     * Select multiple rows (drag on row headers)
     */
    public void selectRowRange(int fromRow, int toRow) {
        fromRow = clampRow(fromRow);
        toRow = clampRow(toRow);
        
        startRow = Math.min(fromRow, toRow);
        endRow = Math.max(fromRow, toRow);
        startCol = 0;
        endCol = TOTAL_COLUMNS - 1;
        activeCellRow = fromRow;
        activeCellCol = 0;
        selectionMode = MODE_ROW;
        hasSelection = true;
        currentGridMode = getGridModeForRow(fromRow);
        
        notifySelectionChanged();
        notifyDragHandleVisible();
    }
    
    // ==================== COLUMN SELECTION (With Multi-Column Drag) ====================
    
    /**
     * Select entire column (tap on column header)
     */
    public void selectColumn(int col) {
        clearSelectionInternal();
        
        if (isValidCol(col)) {
            startRow = 0;
            endRow = TOTAL_DATA_ROWS - 1;
            startCol = col;
            endCol = col;
            touchStartRow = 0;
            touchStartCol = col;
            activeCellRow = 0;
            activeCellCol = col;
            selectionMode = MODE_COLUMN;
            hasSelection = true;
            currentGridMode = GRID_MAIN;
            
            notifySelectionChanged();
            notifyDragHandleVisible();
        }
    }
    
    /**
     * Start column header drag for multi-column selection
     */
    public void startColumnDrag(int col) {
        if (isValidCol(col)) {
            touchStartCol = col;
            startRow = 0;
            endRow = TOTAL_DATA_ROWS - 1;
            startCol = col;
            endCol = col;
            activeCellRow = 0;
            activeCellCol = col;
            isSelecting = true;
            hasSelection = true;
            selectionMode = MODE_COLUMN;
            currentGridMode = GRID_MAIN;
            
            notifySelectionChanged();
        }
    }
    
    /**
     * Update column drag selection (for multi-column selection)
     */
    public void updateColumnDrag(int col) {
        if (!isSelecting || selectionMode != MODE_COLUMN) return;
        
        col = clampCol(col);
        
        int newStartCol = Math.min(touchStartCol, col);
        int newEndCol = Math.max(touchStartCol, col);
        
        if (newStartCol != startCol || newEndCol != endCol) {
            startCol = newStartCol;
            endCol = newEndCol;
            // Keep full column selection
            startRow = 0;
            endRow = TOTAL_DATA_ROWS - 1;
            
            notifySelectionChanged();
        }
    }
    
    /**
     * Select multiple columns (drag on column headers)
     */
    public void selectColumnRange(int fromCol, int toCol) {
        fromCol = clampCol(fromCol);
        toCol = clampCol(toCol);
        
        startRow = 0;
        endRow = TOTAL_DATA_ROWS - 1;
        startCol = Math.min(fromCol, toCol);
        endCol = Math.max(fromCol, toCol);
        activeCellRow = 0;
        activeCellCol = fromCol;
        selectionMode = MODE_COLUMN;
        hasSelection = true;
        currentGridMode = GRID_MAIN;
        
        notifySelectionChanged();
        notifyDragHandleVisible();
    }
    
    // ==================== DRAG HANDLE (Selection Expand Dot) ====================
    
    /**
     * Start drag handle expansion
     */
    public void startDragHandle() {
        if (!hasSelection) return;
        
        isSelecting = true;
        isDraggingHandle = true;
        selectionMode = MODE_DRAG_HANDLE;
        
        // Keep original touch position as anchor
        // Drag handle always expands from the bottom-right corner
    }
    
    /**
     * Update drag handle selection
     */
    public void updateDragHandle(int row, int col) {
        if (!isSelecting || !isDraggingHandle) return;
        
        row = clampRow(row);
        col = clampCol(col);
        
        // Expand selection from original start point to new position
        int newEndRow = Math.max(activeCellRow, row);
        int newEndCol = Math.max(activeCellCol, col);
        int newStartRow = Math.min(activeCellRow, row);
        int newStartCol = Math.min(activeCellCol, col);
        
        if (newStartRow != startRow || newEndRow != endRow || 
            newStartCol != startCol || newEndCol != endCol) {
            
            startRow = newStartRow;
            endRow = newEndRow;
            startCol = newStartCol;
            endCol = newEndCol;
            
            notifySelectionChanged();
        }
    }
    
    /**
     * End drag handle expansion
     */
    public void endDragHandle() {
        isSelecting = false;
        isDraggingHandle = false;
        
        // Update mode based on selection size
        if (startRow == endRow && startCol == endCol) {
            selectionMode = MODE_SINGLE_CELL;
        } else if (startCol == 0 && endCol == TOTAL_COLUMNS - 1) {
            selectionMode = MODE_ROW;
        } else if (startRow == 0 && endRow == TOTAL_DATA_ROWS - 1) {
            selectionMode = MODE_COLUMN;
        } else {
            selectionMode = MODE_MULTI_CELL;
        }
        
        notifySelectionChanged();
        notifyDragHandleVisible();
    }
    
    /**
     * Check if position is on drag handle
     */
    public boolean isOnDragHandle(int row, int col) {
        if (!hasSelection) return false;
        return row == endRow && col == endCol;
    }
    
    /**
     * Get drag handle position (bottom-right of selection)
     */
    public int[] getDragHandlePosition() {
        if (!hasSelection) return null;
        return new int[]{endRow, endCol};
    }
    
    // ==================== SUMMARY TABLE SELECTION ====================
    
    /**
     * Select summary table cell
     */
    public void selectSummaryCell(int summaryRow, int col) {
        // Summary rows are 15-19 (relative to data rows)
        int actualRow = SUMMARY_START_ROW + summaryRow;
        
        clearSelectionInternal();
        
        startRow = actualRow;
        endRow = actualRow;
        startCol = col;
        endCol = col;
        touchStartRow = actualRow;
        touchStartCol = col;
        activeCellRow = actualRow;
        activeCellCol = col;
        selectionMode = MODE_SINGLE_CELL;
        hasSelection = true;
        currentGridMode = GRID_SUMMARY;
        
        notifySelectionChanged();
        notifyDragHandleVisible();
    }
    
    /**
     * Start drag selection in summary table
     */
    public void startSummaryDragSelection(int summaryRow, int col) {
        int actualRow = SUMMARY_START_ROW + summaryRow;
        
        touchStartRow = actualRow;
        touchStartCol = col;
        startRow = actualRow;
        endRow = actualRow;
        startCol = col;
        endCol = col;
        activeCellRow = actualRow;
        activeCellCol = col;
        isSelecting = true;
        hasSelection = true;
        selectionMode = MODE_MULTI_CELL;
        currentGridMode = GRID_SUMMARY;
        
        notifySelectionChanged();
    }
    
    // ==================== HEADER ROW SELECTION ====================
    
    /**
     * Select header cell
     */
    public void selectHeaderCell(int col) {
        clearSelectionInternal();
        
        startRow = HEADER_ROW;
        endRow = HEADER_ROW;
        startCol = col;
        endCol = col;
        touchStartRow = HEADER_ROW;
        touchStartCol = col;
        activeCellRow = HEADER_ROW;
        activeCellCol = col;
        selectionMode = MODE_SINGLE_CELL;
        hasSelection = true;
        currentGridMode = GRID_HEADER;
        
        notifySelectionChanged();
    }
    
    // ==================== SELECTION QUERIES ====================
    
    /**
     * Check if a specific cell is within current selection
     */
    public boolean isCellSelected(int row, int col) {
        if (!hasSelection) return false;
        return row >= startRow && row <= endRow && col >= startCol && col <= endCol;
    }
    
    /**
     * Check if a cell is the active cell (where selection started)
     */
    public boolean isActiveCell(int row, int col) {
        return hasSelection && row == activeCellRow && col == activeCellCol;
    }
    
    /**
     * Check if a row is within selection (for row highlighting)
     */
    public boolean isRowInSelection(int row) {
        if (!hasSelection) return false;
        return row >= startRow && row <= endRow;
    }
    
    /**
     * Check if a column is within selection (for column highlighting)
     */
    public boolean isColumnInSelection(int col) {
        if (!hasSelection) return false;
        return col >= startCol && col <= endCol;
    }
    
    /**
     * Get number of selected cells
     */
    public int getSelectedCellCount() {
        if (!hasSelection) return 0;
        return (endRow - startRow + 1) * (endCol - startCol + 1);
    }
    
    /**
     * Get number of selected rows
     */
    public int getSelectedRowCount() {
        if (!hasSelection) return 0;
        return endRow - startRow + 1;
    }
    
    /**
     * Get number of selected columns
     */
    public int getSelectedColumnCount() {
        if (!hasSelection) return 0;
        return endCol - startCol + 1;
    }
    
    /**
     * Get selection as string (e.g., "A1:C3" or "B2")
     */
    public String getSelectionString() {
        if (!hasSelection) return "";
        
        String start = columnLetter(startCol) + (startRow + 1);
        if (startRow == endRow && startCol == endCol) {
            return start;
        }
        String end = columnLetter(endCol) + (endRow + 1);
        return start + ":" + end;
    }
    
    // ==================== CLEAR SELECTION ====================
    
    /**
     * Clear all selection
     */
    public void clearSelection() {
        clearSelectionInternal();
        
        if (listener != null) {
            listener.onSelectionCleared();
        }
    }
    
    private void clearSelectionInternal() {
        startRow = -1;
        endRow = -1;
        startCol = -1;
        endCol = -1;
        touchStartRow = -1;
        touchStartCol = -1;
        activeCellRow = -1;
        activeCellCol = -1;
        isSelecting = false;
        hasSelection = false;
        isDraggingHandle = false;
        selectionMode = MODE_NONE;
        currentGridMode = GRID_MAIN;
    }
    
    // ==================== GETTERS ====================
    
    public int getStartRow() { return startRow; }
    public int getEndRow() { return endRow; }
    public int getStartCol() { return startCol; }
    public int getEndCol() { return endCol; }
    public int getActiveCellRow() { return activeCellRow; }
    public int getActiveCellCol() { return activeCellCol; }
    public boolean hasSelection() { return hasSelection; }
    public boolean isSelecting() { return isSelecting; }
    public boolean isDraggingHandle() { return isDraggingHandle; }
    public int getSelectionMode() { return selectionMode; }
    public int getCurrentGridMode() { return currentGridMode; }
    
    // ==================== HELPER METHODS ====================
    
    private boolean isValidCell(int row, int col) {
        return isValidRow(row) && isValidCol(col);
    }
    
    private boolean isValidRow(int row) {
        // Valid rows: -1 (header), 0-14 (data), 15-19 (summary)
        return row >= MIN_ROW && row <= MAX_ROW;
    }
    
    private boolean isValidCol(int col) {
        return col >= 0 && col < TOTAL_COLUMNS;
    }
    
    private int clampRow(int row) {
        // Clamp based on current grid mode
        switch (currentGridMode) {
            case GRID_HEADER:
                return HEADER_ROW;
            case GRID_SUMMARY:
                return Math.max(SUMMARY_START_ROW, Math.min(SUMMARY_END_ROW, row));
            case GRID_MAIN:
            default:
                return Math.max(0, Math.min(TOTAL_DATA_ROWS - 1, row));
        }
    }
    
    private int clampCol(int col) {
        return Math.max(0, Math.min(TOTAL_COLUMNS - 1, col));
    }
    
    private int getGridModeForRow(int row) {
        if (row == HEADER_ROW) {
            return GRID_HEADER;
        } else if (row >= SUMMARY_START_ROW && row <= SUMMARY_END_ROW) {
            return GRID_SUMMARY;
        } else {
            return GRID_MAIN;
        }
    }
    
    private String columnLetter(int col) {
        return String.valueOf((char) ('A' + col));
    }
    
    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(startRow, endRow, startCol, endCol, selectionMode);
        }
    }
    
    private void notifyDragHandleVisible() {
        if (listener != null && hasSelection) {
            listener.onDragHandleVisible(endRow, endCol);
        }
    }
}
