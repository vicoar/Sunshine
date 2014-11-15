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

import java.util.Set;

/**
 * Created by viko on 15/11/2014.
 */
public class TestDb extends AndroidTestCase {
    String LOG_TAG = TestDb.class.getSimpleName();

    public void testCreateDb() throws Throwable{
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public void testInsertDb() throws Throwable{
        WeatherDbHelper dbHelper =  new WeatherDbHelper(mContext);
        SQLiteDatabase db =dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LocationEntry.COLUMN_CITY_NAME, "North Pole");
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, "99785");
        values.put(LocationEntry.COLUMN_COORD_LAT, 64.772);
        values.put(LocationEntry.COLUMN_COORD_LONG,  -147.355);

        long locationRowId = db.insert(LocationEntry.TABLE_NAME, null, values);
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

        // Fantastic.  Now that we have a location, add some weather!
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherEntry.COLUMN_LOC_KEY,    locationRowId);
        weatherValues.put(WeatherEntry.COLUMN_DATETEXT,   "20141205");
        weatherValues.put(WeatherEntry.COLUMN_DEGREES,    1.1);
        weatherValues.put(WeatherEntry.COLUMN_HUMIDITY,   1.2);
        weatherValues.put(WeatherEntry.COLUMN_PRESSURE,   1.3);
        weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP,   75.2);
        weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP,   65.2);
        weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, 321);

        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
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

    private void compareCursorValue(Cursor c, ContentValues v) {
        assertTrue(c.moveToFirst());

        ContentValues map = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(c, map);

        Set<String> keySet = v.keySet();

        for (String key : keySet) {
            int idx = c.getColumnIndex(key);
            assertFalse(idx == -1);
            String val = c.getString(idx);
            assertEquals(v.getAsString(key), val);
        }

        c.close();
    }
}
