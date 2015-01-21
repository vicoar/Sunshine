package com.example.viko.sunshine.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.viko.sunshine.data.WeatherContract.LocationEntry;
import com.example.viko.sunshine.data.WeatherContract.WeatherEntry;
import com.example.viko.sunshine.data.WeatherDbHelper;

import java.util.Map;
import java.util.Set;

/**
 * Created by viko on 15/11/2014.
 */
public class TestDb extends AndroidTestCase {
    String LOG_TAG = TestDb.class.getSimpleName();

    static public String TEST_CITY_NAME = "Buenos Aires";
    static public String TEST_LOCATION  = "99725";
    static public String TEST_DATE      = "20140101";

    public void testCreateDb() throws Throwable{
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public static ContentValues createTestLocation(){
        ContentValues values = new ContentValues();
        values.put(LocationEntry.COLUMN_CITY_NAME,        TEST_CITY_NAME);
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION);
        values.put(LocationEntry.COLUMN_COORD_LAT, 64.772);
        values.put(LocationEntry.COLUMN_COORD_LONG,  -147.355);
        return values;
    }

    public static ContentValues createTestWeather(long locationRowId){
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherEntry.COLUMN_LOC_KEY,    locationRowId);
        weatherValues.put(WeatherEntry.COLUMN_DATETEXT,   TEST_DATE);
        weatherValues.put(WeatherEntry.COLUMN_DEGREES,    1.1);
        weatherValues.put(WeatherEntry.COLUMN_HUMIDITY,   1.2);
        weatherValues.put(WeatherEntry.COLUMN_PRESSURE,   1.3);
        weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP,   75.2);
        weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP,   65.2);
        weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, 321);
        return weatherValues;
    }

    public void testInsertDb() throws Throwable{
        WeatherDbHelper dbHelper =  new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = createTestLocation();

        long locationRowId = db.insert(LocationEntry.TABLE_NAME, null, values);
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New location row id: "+locationRowId);

        Cursor cursor = db.query(
                LocationEntry.TABLE_NAME,  // Table to Query
                null,
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        compareCursorValue(cursor, values);

        ContentValues weatherValues = createTestWeather(locationRowId);

        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue(weatherRowId != -1);
        Log.d(LOG_TAG, "New weather row id: "+weatherRowId);

        Cursor weatherCursor = db.query(
                WeatherEntry.TABLE_NAME,  // Table to Query
                null,
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        compareCursorValue(weatherCursor, weatherValues);

        db.close();
    }

    public static void compareCursorValue(Cursor valueCursor, ContentValues expectedValues) {
        assertTrue(valueCursor.moveToFirst());

        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue, valueCursor.getString(idx));
        }
        valueCursor.close();
    }
}
