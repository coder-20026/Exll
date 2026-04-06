package com.whtstable.master;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * ResizeManager - Excel-style Column Width & Row Height Resize System
 * Handles:
 * - Column width adjustment (drag right edge of column header)
 * - Row height adjustment (drag bottom edge of row)
 * - Persistence of resize values
 * - Sync across header, data, and summary tables
 */
public class ResizeManager {

    private static ResizeManager instance;
    private SharedPreferences prefs;
    private Context context;

    private static final String PREF_NAME = "WhtsTableResizeData";
    private static final String KEY_COLUMN_WIDTHS = "column_widths";
    private static final String KEY_ROW_HEIGHTS = "row_heights";

    // Default column widths (in dp) - matching Excel proportions
    // A=34dp, B=62dp, C=200dp, D=30dp, E=45dp, F=92dp, G=95dp, H=89dp, I=38dp
    public static final int[] DEFAULT_COLUMN_WIDTHS = {34, 62, 200, 30, 45, 92, 95, 89, 38};
    
    // Default row height (in dp)
    public static final int DEFAULT_ROW_HEIGHT = 21;
    public static final int DEFAULT_HEADER_ROW_HEIGHT = 24;
    public static final int DEFAULT_SUMMARY_ROW_HEIGHT = 25;

    // Minimum sizes (in dp)
    public static final int MIN_COLUMN_WIDTH = 25;  // Minimum width to prevent collapse
    public static final int MAX_COLUMN_WIDTH = 400; // Maximum width
    public static final int MIN_ROW_HEIGHT = 18;    // Minimum height to keep text visible
    public static final int MAX_ROW_HEIGHT = 100;   // Maximum height

    // Total columns and rows
    public static final int TOTAL_COLUMNS = 9;
    public static final int TOTAL_DATA_ROWS = 15;

    // Storage for custom widths and heights
    private Map<Integer, Integer> columnWidths;  // col index -> width in dp
    private Map<Integer, Integer> rowHeights;    // row index -> height in dp

    // Current month/year for data isolation
    private String currentMonth = "January";
    private String currentYear = "2026";

    // Resize change listener
    public interface ResizeChangeListener {
        void onColumnWidthChanged(int colIndex, int newWidth);
        void onRowHeightChanged(int rowIndex, int newHeight);
        void onResizeComplete();
    }

    private ResizeChangeListener listener;

    private ResizeManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        columnWidths = new HashMap<>();
        rowHeights = new HashMap<>();
        loadResizeData();
    }

    public static synchronized ResizeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ResizeManager(context);
        }
        return instance;
    }

    public void setResizeChangeListener(ResizeChangeListener listener) {
        this.listener = listener;
    }

    public void setCurrentMonthYear(String month, String year) {
        this.currentMonth = month;
        this.currentYear = year;
        loadResizeData();
    }

    // ==================== COLUMN WIDTH METHODS ====================

    /**
     * Get column width in dp
     */
    public int getColumnWidth(int colIndex) {
        if (colIndex < 0 || colIndex >= TOTAL_COLUMNS) {
            return DEFAULT_COLUMN_WIDTHS[0];
        }
        
        if (columnWidths.containsKey(colIndex)) {
            return columnWidths.get(colIndex);
        }
        return DEFAULT_COLUMN_WIDTHS[colIndex];
    }

    /**
     * Set column width in dp
     */
    public void setColumnWidth(int colIndex, int widthDp) {
        if (colIndex < 0 || colIndex >= TOTAL_COLUMNS) return;

        // Clamp to min/max
        widthDp = Math.max(MIN_COLUMN_WIDTH, Math.min(MAX_COLUMN_WIDTH, widthDp));

        columnWidths.put(colIndex, widthDp);
        saveResizeData();

        if (listener != null) {
            listener.onColumnWidthChanged(colIndex, widthDp);
        }
    }

    /**
     * Reset column width to default
     */
    public void resetColumnWidth(int colIndex) {
        if (colIndex < 0 || colIndex >= TOTAL_COLUMNS) return;

        columnWidths.remove(colIndex);
        saveResizeData();

        int defaultWidth = DEFAULT_COLUMN_WIDTHS[colIndex];
        if (listener != null) {
            listener.onColumnWidthChanged(colIndex, defaultWidth);
        }
    }

    /**
     * Get all column widths as array
     */
    public int[] getAllColumnWidths() {
        int[] widths = new int[TOTAL_COLUMNS];
        for (int i = 0; i < TOTAL_COLUMNS; i++) {
            widths[i] = getColumnWidth(i);
        }
        return widths;
    }

    // ==================== ROW HEIGHT METHODS ====================

    /**
     * Get row height in dp
     */
    public int getRowHeight(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= TOTAL_DATA_ROWS) {
            return DEFAULT_ROW_HEIGHT;
        }

        if (rowHeights.containsKey(rowIndex)) {
            return rowHeights.get(rowIndex);
        }
        return DEFAULT_ROW_HEIGHT;
    }

    /**
     * Set row height in dp
     */
    public void setRowHeight(int rowIndex, int heightDp) {
        if (rowIndex < 0 || rowIndex >= TOTAL_DATA_ROWS) return;

        // Clamp to min/max
        heightDp = Math.max(MIN_ROW_HEIGHT, Math.min(MAX_ROW_HEIGHT, heightDp));

        rowHeights.put(rowIndex, heightDp);
        saveResizeData();

        if (listener != null) {
            listener.onRowHeightChanged(rowIndex, heightDp);
        }
    }

    /**
     * Reset row height to default
     */
    public void resetRowHeight(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= TOTAL_DATA_ROWS) return;

        rowHeights.remove(rowIndex);
        saveResizeData();

        if (listener != null) {
            listener.onRowHeightChanged(rowIndex, DEFAULT_ROW_HEIGHT);
        }
    }

    /**
     * Get all row heights as array
     */
    public int[] getAllRowHeights() {
        int[] heights = new int[TOTAL_DATA_ROWS];
        for (int i = 0; i < TOTAL_DATA_ROWS; i++) {
            heights[i] = getRowHeight(i);
        }
        return heights;
    }

    // ==================== RESIZE CALCULATION METHODS ====================

    /**
     * Calculate new column width during drag
     * @param colIndex Column index
     * @param startX Initial touch X position
     * @param currentX Current touch X position
     * @param initialWidth Initial column width before drag started
     * @return New column width in dp
     */
    public int calculateNewColumnWidth(int colIndex, float startX, float currentX, int initialWidth) {
        float deltaX = currentX - startX;
        float density = context.getResources().getDisplayMetrics().density;
        int deltaDp = Math.round(deltaX / density);

        int newWidth = initialWidth + deltaDp;
        return Math.max(MIN_COLUMN_WIDTH, Math.min(MAX_COLUMN_WIDTH, newWidth));
    }

    /**
     * Calculate new row height during drag
     * @param rowIndex Row index
     * @param startY Initial touch Y position
     * @param currentY Current touch Y position
     * @param initialHeight Initial row height before drag started
     * @return New row height in dp
     */
    public int calculateNewRowHeight(int rowIndex, float startY, float currentY, int initialHeight) {
        float deltaY = currentY - startY;
        float density = context.getResources().getDisplayMetrics().density;
        int deltaDp = Math.round(deltaY / density);

        int newHeight = initialHeight + deltaDp;
        return Math.max(MIN_ROW_HEIGHT, Math.min(MAX_ROW_HEIGHT, newHeight));
    }

    // ==================== RESIZE HANDLE DETECTION ====================

    /**
     * Check if touch is on column resize handle (right edge of column header)
     * @param touchX Touch X relative to column header view
     * @param columnWidth Current column width in pixels
     * @param handleTouchArea Touch area width in pixels (typically 20-30px)
     * @return true if touch is on resize handle
     */
    public boolean isOnColumnResizeHandle(float touchX, int columnWidth, int handleTouchArea) {
        return touchX >= (columnWidth - handleTouchArea);
    }

    /**
     * Check if touch is on row resize handle (bottom edge of row)
     * @param touchY Touch Y relative to row view
     * @param rowHeight Current row height in pixels
     * @param handleTouchArea Touch area height in pixels (typically 15-20px)
     * @return true if touch is on resize handle
     */
    public boolean isOnRowResizeHandle(float touchY, int rowHeight, int handleTouchArea) {
        return touchY >= (rowHeight - handleTouchArea);
    }

    // ==================== AUTO-FIT (Double Tap) ====================

    /**
     * Auto-fit column width based on content (placeholder - needs content measurement)
     */
    public void autoFitColumnWidth(int colIndex) {
        // For now, reset to default. In future, measure content and adjust
        resetColumnWidth(colIndex);
    }

    /**
     * Auto-fit row height based on content
     */
    public void autoFitRowHeight(int rowIndex) {
        // For now, reset to default. In future, measure content and adjust
        resetRowHeight(rowIndex);
    }

    // ==================== PERSISTENCE ====================

    private String getDataKey() {
        return currentMonth + "_" + currentYear;
    }

    private void loadResizeData() {
        columnWidths.clear();
        rowHeights.clear();

        String key = getDataKey();
        
        // Load column widths
        String colWidthsJson = prefs.getString(KEY_COLUMN_WIDTHS + "_" + key, "{}");
        try {
            JSONObject colObj = new JSONObject(colWidthsJson);
            Iterator<String> keys = colObj.keys();
            while (keys.hasNext()) {
                String colKey = keys.next();
                int colIndex = Integer.parseInt(colKey);
                int width = colObj.getInt(colKey);
                columnWidths.put(colIndex, width);
            }
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
        }

        // Load row heights
        String rowHeightsJson = prefs.getString(KEY_ROW_HEIGHTS + "_" + key, "{}");
        try {
            JSONObject rowObj = new JSONObject(rowHeightsJson);
            Iterator<String> keys = rowObj.keys();
            while (keys.hasNext()) {
                String rowKey = keys.next();
                int rowIndex = Integer.parseInt(rowKey);
                int height = rowObj.getInt(rowKey);
                rowHeights.put(rowIndex, height);
            }
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void saveResizeData() {
        String key = getDataKey();

        // Save column widths
        JSONObject colObj = new JSONObject();
        try {
            for (Map.Entry<Integer, Integer> entry : columnWidths.entrySet()) {
                colObj.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        prefs.edit().putString(KEY_COLUMN_WIDTHS + "_" + key, colObj.toString()).apply();

        // Save row heights
        JSONObject rowObj = new JSONObject();
        try {
            for (Map.Entry<Integer, Integer> entry : rowHeights.entrySet()) {
                rowObj.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        prefs.edit().putString(KEY_ROW_HEIGHTS + "_" + key, rowObj.toString()).apply();
    }

    /**
     * Reset all resize data for current month/year
     */
    public void resetAllResizeData() {
        columnWidths.clear();
        rowHeights.clear();
        saveResizeData();

        if (listener != null) {
            listener.onResizeComplete();
        }
    }

    /**
     * Notify resize complete (call after drag ends)
     */
    public void notifyResizeComplete() {
        if (listener != null) {
            listener.onResizeComplete();
        }
    }
}
