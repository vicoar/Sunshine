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
        String testName = "North Pole";
        String testLocation = "99785";
        double testLat = 64.772;
        double testLong = -147.355;

        WeatherDbHelper dbHelper =  new WeatherDbHelper(mContext);
        SQLiteDatabase db =dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LocationEntry.COLUMN_CITY_NAME, testName);
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, testLocation);
        values.put(LocationEntry.COLUMN_COORD_LAT, testLat);
        values.put(LocationEntry.COLUMN_COORD_LONG, testLong);

        long locationRowId = db.insert(LocationEntry.TABLE_NAME, null, values);
        Log.d(LOG_TAG, "New location row id: "+locationRowId);

        String[] columns = {
                LocationEntry._ID,
                LocationEntry.COLUMN_LOCATION_SETTING,
                LocationEntry.COLUMN_CITY_NAME,
                LocationEntry.COLUMN_COORD_LAT,
                LocationEntry.COLUMN_COORD_LONG
        };

        Cursor cursor = db.query(
                LocationEntry.TABLE_NAME,  // Table to Query
                columns,
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        compareCursorValue(cursor, values);

        // Fantastic.  Now that we have a location, add some weather!
        String testDateText = "20141205";
        double testDegrees = 1.1;
        double testHumidity = 1.2;
        double testPressure = 1.3;
        double testMaxTemp = 75.2;
        double testMinTemp = 65.2;
        String testShortDesc = "Asteroids";
        double testWindSpeed = 5.5;
        int testWeatherId = 321;

        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherEntry.COLUMN_LOC_KEY,    locationRowId);
        weatherValues.put(WeatherEntry.COLUMN_DATETEXT,   testDateText);
        weatherValues.put(WeatherEntry.COLUMN_DEGREES,    testDegrees);
        weatherValues.put(WeatherEntry.COLUMN_HUMIDITY,   testHumidity);
        weatherValues.put(WeatherEntry.COLUMN_PRESSURE,   testPressure);
        weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP,   testMaxTemp);
        weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP,   testMinTemp);
        weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, testShortDesc);
        weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, testWindSpeed);
        weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, testWeatherId);

        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
        Log.d(LOG_TAG, "New weather row id: "+weatherRowId);

        String[] weatherColumns = {
                WeatherEntry._ID,
                WeatherEntry.COLUMN_LOC_KEY,
                WeatherEntry.COLUMN_DATETEXT,
                WeatherEntry.COLUMN_DEGREES,
                WeatherEntry.COLUMN_HUMIDITY,
                WeatherEntry.COLUMN_PRESSURE,
                WeatherEntry.COLUMN_MAX_TEMP,
                WeatherEntry.COLUMN_MIN_TEMP,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_WIND_SPEED,
                WeatherEntry.COLUMN_WEATHER_ID
        };

        Cursor weatherCursor = db.query(
                WeatherEntry.TABLE_NAME,  // Table to Query
                weatherColumns,
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
