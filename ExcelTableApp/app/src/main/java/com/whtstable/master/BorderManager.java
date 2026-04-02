package com.whtstable.master;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * BorderManager - Manages cell borders for the entire spreadsheet
 * Handles border application, storage, and retrieval
 * Auto-saves borders to SharedPreferences
 */
public class BorderManager {
    
    private static BorderManager instance;
    private SharedPreferences prefs;
    private Context context;
    
    private static final String PREF_NAME = "WhtsTableBorders";
    private static final String KEY_BORDERS = "cell_borders";
    
    // Border storage: key = "month_year_row_col", value = CellBorder
    private Map<String, CellBorder> cellBorders;
    
    // Current selection state
    private int selectionStartRow = -1;
    private int selectionEndRow = -1;
    private int selectionStartCol = -1;
    private int selectionEndCol = -1;
    private boolean isRangeSelected = false;
    
    // Constants for grid
    public static final int TOTAL_COLUMNS = 9;
    public static final int TOTAL_ROWS = 15;
    
    // Border Type Constants (matching Excel/Google Sheets)
    public static final int BORDER_TYPE_ALL = 1;
    public static final int BORDER_TYPE_INNER = 2;
    public static final int BORDER_TYPE_OUTER = 3;
    public static final int BORDER_TYPE_NONE = 4;
    public static final int BORDER_TYPE_TOP = 5;
    public static final int BORDER_TYPE_BOTTOM = 6;
    public static final int BORDER_TYPE_LEFT = 7;
    public static final int BORDER_TYPE_RIGHT = 8;
    public static final int BORDER_TYPE_HORIZONTAL_INNER = 9;
    public static final int BORDER_TYPE_VERTICAL_INNER = 10;
    public static final int BORDER_TYPE_THICK_BOTTOM = 11;
    public static final int BORDER_TYPE_DOUBLE_BOTTOM = 12;
    public static final int BORDER_TYPE_TOP_AND_BOTTOM = 13;
    public static final int BORDER_TYPE_TOP_AND_THICK_BOTTOM = 14;
    
    private String currentMonth = "January";
    private String currentYear = "2026";
    
    private BorderManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.cellBorders = new HashMap<>();
        loadBorders();
    }
    
    public static synchronized BorderManager getInstance(Context context) {
        if (instance == null) {
            instance = new BorderManager(context);
        }
        return instance;
    }
    
    // Set current month/year context
    public void setCurrentMonthYear(String month, String year) {
        this.currentMonth = month;
        this.currentYear = year;
    }
    
    // Generate cell key
    private String getCellKey(int row, int col) {
        return currentMonth + "_" + currentYear + "_" + row + "_" + col;
    }
    
    // Get border for a specific cell
    public CellBorder getCellBorder(int row, int col) {
        String key = getCellKey(row, col);
        CellBorder border = cellBorders.get(key);
        return border != null ? border : new CellBorder();
    }
    
    // Set border for a specific cell
    public void setCellBorder(int row, int col, CellBorder border) {
        String key = getCellKey(row, col);
        if (border != null && border.hasAnyBorder()) {
            cellBorders.put(key, border);
        } else {
            cellBorders.remove(key);
        }
        saveBorders();
    }
    
    // Set selection range
    public void setSelection(int startRow, int endRow, int startCol, int endCol) {
        this.selectionStartRow = Math.min(startRow, endRow);
        this.selectionEndRow = Math.max(startRow, endRow);
        this.selectionStartCol = Math.min(startCol, endCol);
        this.selectionEndCol = Math.max(startCol, endCol);
        this.isRangeSelected = true;
    }
    
    // Set single cell selection
    public void setSingleSelection(int row, int col) {
        this.selectionStartRow = row;
        this.selectionEndRow = row;
        this.selectionStartCol = col;
        this.selectionEndCol = col;
        this.isRangeSelected = true;
    }
    
    // Clear selection
    public void clearSelection() {
        this.selectionStartRow = -1;
        this.selectionEndRow = -1;
        this.selectionStartCol = -1;
        this.selectionEndCol = -1;
        this.isRangeSelected = false;
    }
    
    // Check if a cell is in current selection
    public boolean isCellSelected(int row, int col) {
        if (!isRangeSelected) return false;
        return row >= selectionStartRow && row <= selectionEndRow &&
               col >= selectionStartCol && col <= selectionEndCol;
    }
    
    // Apply border type to current selection
    public void applyBorderType(int borderType, int borderStyle) {
        if (!isRangeSelected) return;
        
        int rowCount = selectionEndRow - selectionStartRow + 1;
        int colCount = selectionEndCol - selectionStartCol + 1;
        boolean isSingleCell = (rowCount == 1 && colCount == 1);
        
        switch (borderType) {
            case BORDER_TYPE_ALL:
                applyAllBorders(borderStyle);
                break;
            case BORDER_TYPE_INNER:
                applyInnerBorders(borderStyle);
                break;
            case BORDER_TYPE_OUTER:
                applyOuterBorders(borderStyle);
                break;
            case BORDER_TYPE_NONE:
                clearAllBorders();
                break;
            case BORDER_TYPE_TOP:
                applyTopBorder(borderStyle);
                break;
            case BORDER_TYPE_BOTTOM:
                applyBottomBorder(borderStyle);
                break;
            case BORDER_TYPE_LEFT:
                applyLeftBorder(borderStyle);
                break;
            case BORDER_TYPE_RIGHT:
                applyRightBorder(borderStyle);
                break;
            case BORDER_TYPE_HORIZONTAL_INNER:
                applyHorizontalInnerBorders(borderStyle);
                break;
            case BORDER_TYPE_VERTICAL_INNER:
                applyVerticalInnerBorders(borderStyle);
                break;
            case BORDER_TYPE_THICK_BOTTOM:
                applyThickBottomBorder();
                break;
            case BORDER_TYPE_DOUBLE_BOTTOM:
                applyDoubleBottomBorder();
                break;
            case BORDER_TYPE_TOP_AND_BOTTOM:
                applyTopAndBottomBorders(borderStyle);
                break;
            case BORDER_TYPE_TOP_AND_THICK_BOTTOM:
                applyTopAndThickBottomBorders();
                break;
        }
        
        saveBorders();
    }
    
    // Apply ALL borders to selection
    private void applyAllBorders(int style) {
        for (int row = selectionStartRow; row <= selectionEndRow; row++) {
            for (int col = selectionStartCol; col <= selectionEndCol; col++) {
                CellBorder border = getCellBorder(row, col);
                border.setAll(style);
                setCellBorderInternal(row, col, border);
            }
        }
    }
    
    // Apply INNER borders only (between cells)
    private void applyInnerBorders(int style) {
        for (int row = selectionStartRow; row <= selectionEndRow; row++) {
            for (int col = selectionStartCol; col <= selectionEndCol; col++) {
                CellBorder border = getCellBorder(row, col);
                
                // Horizontal inner - bottom of all rows except last
                if (row < selectionEndRow) {
                    border.bottom = style;
                }
                // Top of all rows except first
                if (row > selectionStartRow) {
                    border.top = style;
                }
                
                // Vertical inner - right of all cols except last
                if (col < selectionEndCol) {
                    border.right = style;
                }
                // Left of all cols except first
                if (col > selectionStartCol) {
                    border.left = style;
                }
                
                setCellBorderInternal(row, col, border);
            }
        }
    }
    
    // Apply OUTER borders only (perimeter)
    private void applyOuterBorders(int style) {
        for (int row = selectionStartRow; row <= selectionEndRow; row++) {
            for (int col = selectionStartCol; col <= selectionEndCol; col++) {
                CellBorder border = getCellBorder(row, col);
                
                // Top row
                if (row == selectionStartRow) {
                    border.top = style;
                }
                // Bottom row
                if (row == selectionEndRow) {
                    border.bottom = style;
                }
                // Left column
                if (col == selectionStartCol) {
                    border.left = style;
                }
                // Right column
                if (col == selectionEndCol) {
                    border.right = style;
                }
                
                setCellBorderInternal(row, col, border);
            }
        }
    }
    
    // Clear ALL borders in selection
    private void clearAllBorders() {
        for (int row = selectionStartRow; row <= selectionEndRow; row++) {
            for (int col = selectionStartCol; col <= selectionEndCol; col++) {
                String key = getCellKey(row, col);
                cellBorders.remove(key);
            }
        }
    }
    
    // Apply TOP border only
    private void applyTopBorder(int style) {
        int row = selectionStartRow;
        for (int col = selectionStartCol; col <= selectionEndCol; col++) {
            CellBorder border = getCellBorder(row, col);
            border.top = style;
            setCellBorderInternal(row, col, border);
        }
    }
    
    // Apply BOTTOM border only
    private void applyBottomBorder(int style) {
        int row = selectionEndRow;
        for (int col = selectionStartCol; col <= selectionEndCol; col++) {
            CellBorder border = getCellBorder(row, col);
            border.bottom = style;
            setCellBorderInternal(row, col, border);
        }
    }
    
    // Apply LEFT border only
    private void applyLeftBorder(int style) {
        int col = selectionStartCol;
        for (int row = selectionStartRow; row <= selectionEndRow; row++) {
            CellBorder border = getCellBorder(row, col);
            border.left = style;
            setCellBorderInternal(row, col, border);
        }
    }
    
    // Apply RIGHT border only
    private void applyRightBorder(int style) {
        int col = selectionEndCol;
        for (int row = selectionStartRow; row <= selectionEndRow; row++) {
            CellBorder border = getCellBorder(row, col);
            border.right = style;
            setCellBorderInternal(row, col, border);
        }
    }
    
    // Apply HORIZONTAL INNER borders
    private void applyHorizontalInnerBorders(int style) {
        for (int row = selectionStartRow; row < selectionEndRow; row++) {
            for (int col = selectionStartCol; col <= selectionEndCol; col++) {
                CellBorder border = getCellBorder(row, col);
                border.bottom = style;
                setCellBorderInternal(row, col, border);
                
                // Also set top of next row
                CellBorder nextBorder = getCellBorder(row + 1, col);
                nextBorder.top = style;
                setCellBorderInternal(row + 1, col, nextBorder);
            }
        }
    }
    
    // Apply VERTICAL INNER borders
    private void applyVerticalInnerBorders(int style) {
        for (int row = selectionStartRow; row <= selectionEndRow; row++) {
            for (int col = selectionStartCol; col < selectionEndCol; col++) {
                CellBorder border = getCellBorder(row, col);
                border.right = style;
                setCellBorderInternal(row, col, border);
                
                // Also set left of next column
                CellBorder nextBorder = getCellBorder(row, col + 1);
                nextBorder.left = style;
                setCellBorderInternal(row, col + 1, nextBorder);
            }
        }
    }
    
    // Apply THICK BOTTOM border
    private void applyThickBottomBorder() {
        int row = selectionEndRow;
        for (int col = selectionStartCol; col <= selectionEndCol; col++) {
            CellBorder border = getCellBorder(row, col);
            border.bottom = CellBorder.STYLE_THICK;
            setCellBorderInternal(row, col, border);
        }
    }
    
    // Apply DOUBLE BOTTOM border
    private void applyDoubleBottomBorder() {
        int row = selectionEndRow;
        for (int col = selectionStartCol; col <= selectionEndCol; col++) {
            CellBorder border = getCellBorder(row, col);
            border.bottom = CellBorder.STYLE_DOUBLE;
            setCellBorderInternal(row, col, border);
        }
    }
    
    // Apply TOP and BOTTOM borders
    private void applyTopAndBottomBorders(int style) {
        // Top border on first row
        for (int col = selectionStartCol; col <= selectionEndCol; col++) {
            CellBorder topBorder = getCellBorder(selectionStartRow, col);
            topBorder.top = style;
            setCellBorderInternal(selectionStartRow, col, topBorder);
        }
        
        // Bottom border on last row
        for (int col = selectionStartCol; col <= selectionEndCol; col++) {
            CellBorder bottomBorder = getCellBorder(selectionEndRow, col);
            bottomBorder.bottom = style;
            setCellBorderInternal(selectionEndRow, col, bottomBorder);
        }
    }
    
    // Apply TOP (thin) and THICK BOTTOM borders
    private void applyTopAndThickBottomBorders() {
        // Thin top border on first row
        for (int col = selectionStartCol; col <= selectionEndCol; col++) {
            CellBorder topBorder = getCellBorder(selectionStartRow, col);
            topBorder.top = CellBorder.STYLE_THIN;
            setCellBorderInternal(selectionStartRow, col, topBorder);
        }
        
        // Thick bottom border on last row
        for (int col = selectionStartCol; col <= selectionEndCol; col++) {
            CellBorder bottomBorder = getCellBorder(selectionEndRow, col);
            bottomBorder.bottom = CellBorder.STYLE_THICK;
            setCellBorderInternal(selectionEndRow, col, bottomBorder);
        }
    }
    
    // Internal method to set border without auto-save
    private void setCellBorderInternal(int row, int col, CellBorder border) {
        String key = getCellKey(row, col);
        if (border != null && border.hasAnyBorder()) {
            cellBorders.put(key, border);
        } else {
            cellBorders.remove(key);
        }
    }
    
    // Save all borders to SharedPreferences
    public void saveBorders() {
        try {
            JSONObject allBorders = new JSONObject();
            for (Map.Entry<String, CellBorder> entry : cellBorders.entrySet()) {
                allBorders.put(entry.getKey(), entry.getValue().toJSON());
            }
            prefs.edit().putString(KEY_BORDERS, allBorders.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    // Load borders from SharedPreferences
    private void loadBorders() {
        cellBorders.clear();
        String jsonStr = prefs.getString(KEY_BORDERS, "{}");
        try {
            JSONObject allBorders = new JSONObject(jsonStr);
            Iterator<String> keys = allBorders.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject borderJson = allBorders.getJSONObject(key);
                CellBorder border = CellBorder.fromJSON(borderJson);
                cellBorders.put(key, border);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    // Get all borders for current month/year (for rendering)
    public Map<String, CellBorder> getCurrentBorders() {
        Map<String, CellBorder> current = new HashMap<>();
        String prefix = currentMonth + "_" + currentYear + "_";
        for (Map.Entry<String, CellBorder> entry : cellBorders.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                current.put(entry.getKey(), entry.getValue());
            }
        }
        return current;
    }
    
    // Clear all borders for current month/year
    public void clearCurrentMonthBorders() {
        String prefix = currentMonth + "_" + currentYear + "_";
        Iterator<Map.Entry<String, CellBorder>> iterator = cellBorders.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CellBorder> entry = iterator.next();
            if (entry.getKey().startsWith(prefix)) {
                iterator.remove();
            }
        }
        saveBorders();
    }
    
    // Get selection info
    public int getSelectionStartRow() { return selectionStartRow; }
    public int getSelectionEndRow() { return selectionEndRow; }
    public int getSelectionStartCol() { return selectionStartCol; }
    public int getSelectionEndCol() { return selectionEndCol; }
    public boolean hasSelection() { return isRangeSelected; }
}
