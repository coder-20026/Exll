package com.whtstable.master;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * DataManager - Premium Data Persistence Layer
 * Handles all data storage and retrieval using SharedPreferences
 */
public class DataManager {
    
    private static DataManager instance;
    private SharedPreferences prefs;
    
    private static final String PREF_NAME = "WhtsTableMasterData";
    private static final String KEY_ROWS = "rows_data";
    private static final String KEY_ACTIVE_ROW = "active_row_index";
    private static final String KEY_ACTIVE_ROW_LOCKED = "active_row_locked";
    
    private ArrayList<RowData> rows;
    private int activeRowIndex = -1;
    private boolean activeRowLocked = false;
    
    /**
     * Row Data Model
     */
    public static class RowData {
        public String bankName = "";
        public String applicantName = "";
        public String status = "";
        public String reasonOfCNV = "";
        public String latitude = "";
        public String longitude = "";
        public String area = "";
        public String km = "";
        public boolean hasText1 = false;
        public boolean hasText2 = false;
        
        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("bankName", bankName);
            obj.put("applicantName", applicantName);
            obj.put("status", status);
            obj.put("reasonOfCNV", reasonOfCNV);
            obj.put("latitude", latitude);
            obj.put("longitude", longitude);
            obj.put("area", area);
            obj.put("km", km);
            obj.put("hasText1", hasText1);
            obj.put("hasText2", hasText2);
            return obj;
        }
        
        public static RowData fromJSON(JSONObject obj) throws JSONException {
            RowData row = new RowData();
            row.bankName = obj.optString("bankName", "");
            row.applicantName = obj.optString("applicantName", "");
            row.status = obj.optString("status", "");
            row.reasonOfCNV = obj.optString("reasonOfCNV", "");
            row.latitude = obj.optString("latitude", "");
            row.longitude = obj.optString("longitude", "");
            row.area = obj.optString("area", "");
            row.km = obj.optString("km", "");
            row.hasText1 = obj.optBoolean("hasText1", false);
            row.hasText2 = obj.optBoolean("hasText2", false);
            return row;
        }
        
        public boolean isEmpty() {
            return bankName.isEmpty() && applicantName.isEmpty() && 
                   reasonOfCNV.isEmpty() && longitude.isEmpty();
        }
    }
    
    private DataManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadData();
    }
    
    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private void loadData() {
        rows = new ArrayList<RowData>();
        String jsonStr = prefs.getString(KEY_ROWS, "[]");
        activeRowIndex = prefs.getInt(KEY_ACTIVE_ROW, -1);
        activeRowLocked = prefs.getBoolean(KEY_ACTIVE_ROW_LOCKED, false);
        
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                rows.add(RowData.fromJSON(jsonArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public void saveData() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (RowData row : rows) {
                jsonArray.put(row.toJSON());
            }
            prefs.edit()
                .putString(KEY_ROWS, jsonArray.toString())
                .putInt(KEY_ACTIVE_ROW, activeRowIndex)
                .putBoolean(KEY_ACTIVE_ROW_LOCKED, activeRowLocked)
                .apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public int createNewActiveRow(String bankName, String applicantName, String reasonOfCNV) {
        RowData newRow = new RowData();
        newRow.bankName = bankName;
        newRow.applicantName = applicantName;
        newRow.reasonOfCNV = reasonOfCNV;
        newRow.hasText1 = true;
        
        rows.add(newRow);
        activeRowIndex = rows.size() - 1;
        activeRowLocked = false;
        
        saveData();
        return activeRowIndex;
    }
    
    public boolean updateActiveRowWithText2(String longitude) {
        if (activeRowIndex < 0 || activeRowIndex >= rows.size()) {
            return false;
        }
        
        if (activeRowLocked) {
            return false;
        }
        
        RowData activeRow = rows.get(activeRowIndex);
        activeRow.longitude = longitude;
        activeRow.hasText2 = true;
        
        saveData();
        return true;
    }
    
    public boolean updateActiveRowWithCoordinatesAndArea(String longitude, String area) {
        if (activeRowIndex < 0 || activeRowIndex >= rows.size()) {
            return false;
        }
        
        if (activeRowLocked) {
            return false;
        }
        
        RowData activeRow = rows.get(activeRowIndex);
        activeRow.longitude = longitude;
        activeRow.area = area;
        activeRow.hasText2 = true;
        
        saveData();
        return true;
    }
    
    public void lockActiveRow() {
        activeRowLocked = true;
        saveData();
    }
    
    public RowData getActiveRow() {
        if (activeRowIndex >= 0 && activeRowIndex < rows.size()) {
            return rows.get(activeRowIndex);
        }
        return null;
    }
    
    public int getActiveRowIndex() {
        return activeRowIndex;
    }
    
    public boolean hasActiveRow() {
        return activeRowIndex >= 0 && activeRowIndex < rows.size() && !activeRowLocked;
    }
    
    public ArrayList<RowData> getAllRows() {
        return rows;
    }
    
    public void updateRow(int index, RowData rowData) {
        if (index >= 0 && index < rows.size()) {
            rows.set(index, rowData);
            saveData();
        }
    }
    
    public void deleteRow(int index) {
        if (index >= 0 && index < rows.size()) {
            rows.remove(index);
            if (activeRowIndex == index) {
                activeRowIndex = -1;
                activeRowLocked = false;
            } else if (activeRowIndex > index) {
                activeRowIndex--;
            }
            saveData();
        }
    }
    
    public void swapRows(int index1, int index2) {
        if (index1 >= 0 && index1 < rows.size() && 
            index2 >= 0 && index2 < rows.size()) {
            RowData temp = rows.get(index1);
            rows.set(index1, rows.get(index2));
            rows.set(index2, temp);
            
            if (activeRowIndex == index1) {
                activeRowIndex = index2;
            } else if (activeRowIndex == index2) {
                activeRowIndex = index1;
            }
            
            saveData();
        }
    }
    
    public int getFilledRowsCount() {
        int count = 0;
        for (RowData row : rows) {
            if (!row.isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    public void clearAllData() {
        rows.clear();
        activeRowIndex = -1;
        activeRowLocked = false;
        saveData();
    }
}
