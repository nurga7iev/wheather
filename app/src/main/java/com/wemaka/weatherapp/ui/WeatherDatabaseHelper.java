package com.wemaka.weatherapp.ui;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WeatherDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "WeatherDatabaseHelper"; // Для логов

    private static final String DATABASE_NAME = "weather.db";
    private static final int DATABASE_VERSION = 2; // Увеличил версию для миграции
    private static final String TABLE_SEARCH_HISTORY = "search_history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_QUERY = "query";

    // SQL-запрос для создания таблицы
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_SEARCH_HISTORY + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_QUERY + " TEXT NOT NULL)";

    public WeatherDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating table: " + CREATE_TABLE_QUERY);
        db.execSQL(CREATE_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH_HISTORY);
        onCreate(db);
    }
    public void deleteQuery(String query) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_SEARCH_HISTORY, COLUMN_QUERY + " = ?", new String[]{query});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting query: " + query, e);
        } finally {
            db.close();
        }
    }


    public List<String> getSearchHistory() {
        List<String> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_SEARCH_HISTORY, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range")
                    String city = cursor.getString(cursor.getColumnIndex(COLUMN_QUERY));
                    history.add(city);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while getting search history", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return history;
    }

    public boolean insertQuery(String query) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_QUERY, query);

        long result = -1;
        try {
            result = db.insert(TABLE_SEARCH_HISTORY, null, contentValues);
            Log.i(TAG, "Inserted query: " + query);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting query: " + query, e);
        } finally {
            db.close();
        }
        return result != -1;
    }

    public void clearSearchHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("DELETE FROM " + TABLE_SEARCH_HISTORY);
            Log.i(TAG, "Search history cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing search history", e);
        } finally {
            db.close();
        }
    }
}
