package com.whtstable.master;

/**
 * CellSelectionManager - Excel-style multi-cell selection system
 * Handles single cell, drag selection, row selection, and column selection
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
    
    private int selectionMode = MODE_NONE;
    
    // Grid bounds
    public static final int TOTAL_ROWS = 15;
    public static final int TOTAL_COLUMNS = 9;
    
    // Active cell (the cell where selection started - shows darker border)
    private int activeCellRow = -1;
    private int activeCellCol = -1;
    
    // Selection change listener
    public interface SelectionChangeListener {
        void onSelectionChanged(int startRow, int endRow, int startCol, int endCol, int mode);
        void onSelectionCleared();
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
        clearSelection();
        
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
            
            notifySelectionChanged();
        }
    }
    
    // ==================== MULTI-CELL DRAG SELECTION ====================
    
    /**
     * Start drag selection (ACTION_DOWN / long press)
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
            
            notifySelectionChanged();
        }
    }
    
    /**
     * Update drag selection (ACTION_MOVE)
     */
    public void updateDragSelection(int row, int col) {
        if (!isSelecting) return;
        
        // Clamp to valid range
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
        
        // If only one cell selected, switch to single cell mode
        if (startRow == endRow && startCol == endCol) {
            selectionMode = MODE_SINGLE_CELL;
        }
        
        notifySelectionChanged();
    }
    
    // ==================== ROW SELECTION ====================
    
    /**
     * Select entire row (tap on row header / SR NO column)
     */
    public void selectRow(int row) {
        clearSelection();
        
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
        
        notifySelectionChanged();
    }
    
    // ==================== COLUMN SELECTION ====================
    
    /**
     * Select entire column (tap on column header)
     */
    public void selectColumn(int col) {
        clearSelection();
        
        if (isValidCol(col)) {
            startRow = 0;
            endRow = TOTAL_ROWS - 1;
            startCol = col;
            endCol = col;
            touchStartRow = 0;
            touchStartCol = col;
            activeCellRow = 0;
            activeCellCol = col;
            selectionMode = MODE_COLUMN;
            hasSelection = true;
            
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
        endRow = TOTAL_ROWS - 1;
        startCol = Math.min(fromCol, toCol);
        endCol = Math.max(fromCol, toCol);
        activeCellRow = 0;
        activeCellCol = fromCol;
        selectionMode = MODE_COLUMN;
        hasSelection = true;
        
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
     * Get number of selected cells
     */
    public int getSelectedCellCount() {
        if (!hasSelection) return 0;
        return (endRow - startRow + 1) * (endCol - startCol + 1);
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
        selectionMode = MODE_NONE;
        
        if (listener != null) {
            listener.onSelectionCleared();
        }
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
    public int getSelectionMode() { return selectionMode; }
    
    // ==================== HELPER METHODS ====================
    
    private boolean isValidCell(int row, int col) {
        return isValidRow(row) && isValidCol(col);
    }
    
    private boolean isValidRow(int row) {
        return row >= 0 && row < TOTAL_ROWS;
    }
    
    private boolean isValidCol(int col) {
        return col >= 0 && col < TOTAL_COLUMNS;
    }
    
    private int clampRow(int row) {
        return Math.max(0, Math.min(TOTAL_ROWS - 1, row));
    }
    
    private int clampCol(int col) {
        return Math.max(0, Math.min(TOTAL_COLUMNS - 1, col));
    }
    
    private String columnLetter(int col) {
        return String.valueOf((char) ('A' + col));
    }
    
    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(startRow, endRow, startCol, endCol, selectionMode);
        }
    }
}
