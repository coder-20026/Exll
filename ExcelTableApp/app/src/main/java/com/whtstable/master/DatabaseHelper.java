package com.whtstable.master;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * DatabaseHelper - SQLite Database for persistent storage
 * Supports date-based table grouping
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "WhtsTableMaster.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table names
    private static final String TABLE_ENTRIES = "entries";
    private static final String TABLE_SETTINGS = "settings";
    
    // Entries table columns
    private static final String COL_ID = "id";
    private static final String COL_DATE = "date_full";  // DD Month YYYY
    private static final String COL_DAY = "day";
    private static final String COL_MONTH = "month";
    private static final String COL_YEAR = "year";
    private static final String COL_BANK_NAME = "bank_name";
    private static final String COL_APPLICANT_NAME = "applicant_name";
    private static final String COL_STATUS = "status";
    private static final String COL_REASON_CNV = "reason_cnv";
    private static final String COL_LATLONG_FROM = "latlong_from";
    private static final String COL_LATLONG_TO = "latlong_to";
    private static final String COL_AREA = "area";
    private static final String COL_KM = "km";
    private static final String COL_CREATED_AT = "created_at";
    
    // Settings table columns
    private static final String COL_KEY = "setting_key";
    private static final String COL_VALUE = "setting_value";
    
    private static DatabaseHelper instance;
    
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create entries table
        String createEntriesTable = "CREATE TABLE " + TABLE_ENTRIES + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_DATE + " TEXT, " +
            COL_DAY + " TEXT, " +
            COL_MONTH + " TEXT, " +
            COL_YEAR + " TEXT, " +
            COL_BANK_NAME + " TEXT, " +
            COL_APPLICANT_NAME + " TEXT, " +
            COL_STATUS + " TEXT, " +
            COL_REASON_CNV + " TEXT, " +
            COL_LATLONG_FROM + " TEXT, " +
            COL_LATLONG_TO + " TEXT, " +
            COL_AREA + " TEXT, " +
            COL_KM + " TEXT, " +
            COL_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createEntriesTable);
        
        // Create settings table
        String createSettingsTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
            COL_KEY + " TEXT PRIMARY KEY, " +
            COL_VALUE + " TEXT)";
        db.execSQL(createSettingsTable);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }
    
    // =========== SETTINGS METHODS ===========
    
    public void setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_KEY, key);
        values.put(COL_VALUE, value);
        db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    public String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COL_VALUE}, 
            COL_KEY + "=?", new String[]{key}, null, null, null);
        
        String result = defaultValue;
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }
    
    public void setSelectedMonth(String month) {
        setSetting("selected_month", month);
    }
    
    public String getSelectedMonth() {
        return getSetting("selected_month", "January");
    }
    
    public void setSelectedYear(String year) {
        setSetting("selected_year", year);
    }
    
    public String getSelectedYear() {
        return getSetting("selected_year", "2026");
    }
    
    // =========== ENTRY METHODS ===========
    
    public static class EntryData {
        public long id;
        public String dateFull;
        public String day;
        public String month;
        public String year;
        public String bankName = "";
        public String applicantName = "";
        public String status = "";
        public String reasonCnv = "";
        public String latlongFrom = "";
        public String latlongTo = "";
        public String area = "";
        public String km = "";
    }
    
    public long addEntry(EntryData entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DATE, entry.dateFull);
        values.put(COL_DAY, entry.day);
        values.put(COL_MONTH, entry.month);
        values.put(COL_YEAR, entry.year);
        values.put(COL_BANK_NAME, entry.bankName);
        values.put(COL_APPLICANT_NAME, entry.applicantName);
        values.put(COL_STATUS, entry.status);
        values.put(COL_REASON_CNV, entry.reasonCnv);
        values.put(COL_LATLONG_FROM, entry.latlongFrom);
        values.put(COL_LATLONG_TO, entry.latlongTo);
        values.put(COL_AREA, entry.area);
        values.put(COL_KM, entry.km);
        
        return db.insert(TABLE_ENTRIES, null, values);
    }
    
    public void updateEntry(long id, EntryData entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DATE, entry.dateFull);
        values.put(COL_DAY, entry.day);
        values.put(COL_MONTH, entry.month);
        values.put(COL_YEAR, entry.year);
        values.put(COL_BANK_NAME, entry.bankName);
        values.put(COL_APPLICANT_NAME, entry.applicantName);
        values.put(COL_STATUS, entry.status);
        values.put(COL_REASON_CNV, entry.reasonCnv);
        values.put(COL_LATLONG_FROM, entry.latlongFrom);
        values.put(COL_LATLONG_TO, entry.latlongTo);
        values.put(COL_AREA, entry.area);
        values.put(COL_KM, entry.km);
        
        db.update(TABLE_ENTRIES, values, COL_ID + "=?", new String[]{String.valueOf(id)});
    }
    
    public void deleteEntry(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ENTRIES, COL_ID + "=?", new String[]{String.valueOf(id)});
    }
    
    public ArrayList<EntryData> getEntriesByDate(String dateFull) {
        ArrayList<EntryData> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_ENTRIES, null, 
            COL_DATE + "=?", new String[]{dateFull}, 
            null, null, COL_CREATED_AT + " ASC");
        
        while (cursor.moveToNext()) {
            EntryData entry = cursorToEntry(cursor);
            entries.add(entry);
        }
        cursor.close();
        return entries;
    }
    
    public ArrayList<EntryData> getEntriesByMonthYear(String month, String year) {
        ArrayList<EntryData> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_ENTRIES, null, 
            COL_MONTH + "=? AND " + COL_YEAR + "=?", new String[]{month, year}, 
            null, null, COL_DAY + " ASC, " + COL_CREATED_AT + " ASC");
        
        while (cursor.moveToNext()) {
            EntryData entry = cursorToEntry(cursor);
            entries.add(entry);
        }
        cursor.close();
        return entries;
    }
    
    public LinkedHashMap<String, ArrayList<EntryData>> getEntriesGroupedByDate(String month, String year) {
        LinkedHashMap<String, ArrayList<EntryData>> grouped = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_ENTRIES, null, 
            COL_MONTH + "=? AND " + COL_YEAR + "=?", new String[]{month, year}, 
            null, null, COL_DAY + " ASC, " + COL_CREATED_AT + " ASC");
        
        while (cursor.moveToNext()) {
            EntryData entry = cursorToEntry(cursor);
            String dateKey = entry.dateFull;
            
            if (!grouped.containsKey(dateKey)) {
                grouped.put(dateKey, new ArrayList<EntryData>());
            }
            grouped.get(dateKey).add(entry);
        }
        cursor.close();
        return grouped;
    }
    
    public ArrayList<String> getDistinctDates(String month, String year) {
        ArrayList<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT DISTINCT " + COL_DATE + " FROM " + TABLE_ENTRIES + 
            " WHERE " + COL_MONTH + "=? AND " + COL_YEAR + "=?" +
            " ORDER BY " + COL_DAY + " ASC", 
            new String[]{month, year});
        
        while (cursor.moveToNext()) {
            dates.add(cursor.getString(0));
        }
        cursor.close();
        return dates;
    }
    
    private EntryData cursorToEntry(Cursor cursor) {
        EntryData entry = new EntryData();
        entry.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        entry.dateFull = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));
        entry.day = cursor.getString(cursor.getColumnIndexOrThrow(COL_DAY));
        entry.month = cursor.getString(cursor.getColumnIndexOrThrow(COL_MONTH));
        entry.year = cursor.getString(cursor.getColumnIndexOrThrow(COL_YEAR));
        entry.bankName = cursor.getString(cursor.getColumnIndexOrThrow(COL_BANK_NAME));
        entry.applicantName = cursor.getString(cursor.getColumnIndexOrThrow(COL_APPLICANT_NAME));
        entry.status = cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS));
        entry.reasonCnv = cursor.getString(cursor.getColumnIndexOrThrow(COL_REASON_CNV));
        entry.latlongFrom = cursor.getString(cursor.getColumnIndexOrThrow(COL_LATLONG_FROM));
        entry.latlongTo = cursor.getString(cursor.getColumnIndexOrThrow(COL_LATLONG_TO));
        entry.area = cursor.getString(cursor.getColumnIndexOrThrow(COL_AREA));
        entry.km = cursor.getString(cursor.getColumnIndexOrThrow(COL_KM));
        return entry;
    }
    
    public void clearAllEntries() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ENTRIES, null, null);
    }
}
