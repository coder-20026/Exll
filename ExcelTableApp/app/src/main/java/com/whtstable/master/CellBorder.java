package com.whtstable.master;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * CellBorder - Data model for individual cell border properties
 * Supports top, bottom, left, right borders with style and thickness
 */
public class CellBorder {
    
    // Border Styles
    public static final int STYLE_NONE = 0;
    public static final int STYLE_THIN = 1;
    public static final int STYLE_MEDIUM = 2;
    public static final int STYLE_THICK = 3;
    public static final int STYLE_DOUBLE = 4;
    public static final int STYLE_DASHED = 5;
    public static final int STYLE_DOTTED = 6;
    
    // Border sides
    public int top = STYLE_NONE;
    public int bottom = STYLE_NONE;
    public int left = STYLE_NONE;
    public int right = STYLE_NONE;
    
    // Border colors (default black)
    public String topColor = "#000000";
    public String bottomColor = "#000000";
    public String leftColor = "#000000";
    public String rightColor = "#000000";
    
    public CellBorder() {
        // Default constructor - no borders
    }
    
    public CellBorder(int allSides) {
        this.top = allSides;
        this.bottom = allSides;
        this.left = allSides;
        this.right = allSides;
    }
    
    public CellBorder(int top, int bottom, int left, int right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }
    
    // Set all borders at once
    public void setAll(int style) {
        this.top = style;
        this.bottom = style;
        this.left = style;
        this.right = style;
    }
    
    // Set outer borders only
    public void setOuter(int style) {
        // This is used at range level, individual cell will get appropriate sides
    }
    
    // Set inner borders only  
    public void setInner(int style) {
        // This is used at range level
    }
    
    // Clear all borders
    public void clear() {
        this.top = STYLE_NONE;
        this.bottom = STYLE_NONE;
        this.left = STYLE_NONE;
        this.right = STYLE_NONE;
    }
    
    // Check if any border exists
    public boolean hasAnyBorder() {
        return top != STYLE_NONE || bottom != STYLE_NONE || 
               left != STYLE_NONE || right != STYLE_NONE;
    }
    
    // Copy from another CellBorder
    public void copyFrom(CellBorder other) {
        if (other != null) {
            this.top = other.top;
            this.bottom = other.bottom;
            this.left = other.left;
            this.right = other.right;
            this.topColor = other.topColor;
            this.bottomColor = other.bottomColor;
            this.leftColor = other.leftColor;
            this.rightColor = other.rightColor;
        }
    }
    
    // Clone this border
    public CellBorder clone() {
        CellBorder clone = new CellBorder();
        clone.copyFrom(this);
        return clone;
    }
    
    // Merge with another border (higher priority wins)
    public void mergeWith(CellBorder other) {
        if (other != null) {
            if (other.top > this.top) {
                this.top = other.top;
                this.topColor = other.topColor;
            }
            if (other.bottom > this.bottom) {
                this.bottom = other.bottom;
                this.bottomColor = other.bottomColor;
            }
            if (other.left > this.left) {
                this.left = other.left;
                this.leftColor = other.leftColor;
            }
            if (other.right > this.right) {
                this.right = other.right;
                this.rightColor = other.rightColor;
            }
        }
    }
    
    // Convert to JSON for storage
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("top", top);
        obj.put("bottom", bottom);
        obj.put("left", left);
        obj.put("right", right);
        obj.put("topColor", topColor);
        obj.put("bottomColor", bottomColor);
        obj.put("leftColor", leftColor);
        obj.put("rightColor", rightColor);
        return obj;
    }
    
    // Create from JSON
    public static CellBorder fromJSON(JSONObject obj) throws JSONException {
        CellBorder border = new CellBorder();
        border.top = obj.optInt("top", STYLE_NONE);
        border.bottom = obj.optInt("bottom", STYLE_NONE);
        border.left = obj.optInt("left", STYLE_NONE);
        border.right = obj.optInt("right", STYLE_NONE);
        border.topColor = obj.optString("topColor", "#000000");
        border.bottomColor = obj.optString("bottomColor", "#000000");
        border.leftColor = obj.optString("leftColor", "#000000");
        border.rightColor = obj.optString("rightColor", "#000000");
        return border;
    }
    
    // Convert to compact string for database storage
    public String toCompactString() {
        // Format: "top,bottom,left,right" (just style values for simplicity)
        return top + "," + bottom + "," + left + "," + right;
    }
    
    // Create from compact string
    public static CellBorder fromCompactString(String str) {
        CellBorder border = new CellBorder();
        if (str != null && !str.isEmpty()) {
            try {
                String[] parts = str.split(",");
                if (parts.length >= 4) {
                    border.top = Integer.parseInt(parts[0]);
                    border.bottom = Integer.parseInt(parts[1]);
                    border.left = Integer.parseInt(parts[2]);
                    border.right = Integer.parseInt(parts[3]);
                }
            } catch (NumberFormatException e) {
                // Return default border on parse error
            }
        }
        return border;
    }
    
    // Get border thickness in dp for drawing
    public static int getThicknessDp(int style) {
        switch (style) {
            case STYLE_THIN: return 1;
            case STYLE_MEDIUM: return 2;
            case STYLE_THICK: return 3;
            case STYLE_DOUBLE: return 3;
            case STYLE_DASHED: return 1;
            case STYLE_DOTTED: return 1;
            default: return 0;
        }
    }
    
    // Get style name for display
    public static String getStyleName(int style) {
        switch (style) {
            case STYLE_THIN: return "Thin";
            case STYLE_MEDIUM: return "Medium";
            case STYLE_THICK: return "Thick";
            case STYLE_DOUBLE: return "Double";
            case STYLE_DASHED: return "Dashed";
            case STYLE_DOTTED: return "Dotted";
            default: return "None";
        }
    }
    
    @Override
    public String toString() {
        return "CellBorder{T:" + top + ",B:" + bottom + ",L:" + left + ",R:" + right + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CellBorder other = (CellBorder) obj;
        return top == other.top && bottom == other.bottom && 
               left == other.left && right == other.right;
    }
}
