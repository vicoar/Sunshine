package com.example.viko.sunshine.test;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.viko.sunshine.data.WeatherContract.LocationEntry;
import com.example.viko.sunshine.data.WeatherContract.WeatherEntry;
import com.example.viko.sunshine.data.WeatherDbHelper;

/**
 * Created by viko on 15/11/2014.
 */
public class TestProvider extends AndroidTestCase {
    String LOG_TAG = TestProvider.class.getSimpleName();

    public void testDeleteDb() throws Throwable{
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    public void testGetType() throws Throwable{
        String type = mContext.getContentResolver().getType(WeatherEntry.CONTENT_URI);
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "94074";
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocation(testLocation));
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testDate = "2014-01-01";
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        assertEquals(WeatherEntry.CONTENT_ITEM_TYPE, type);

        type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
        assertEquals(LocationEntry.CONTENT_TYPE, type);

        int testLocationId = 123;
        type = mContext.getContentResolver().getType(LocationEntry.buildLocationUri(testLocationId));
        assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);
    }

    public void testInsertReadProvider() throws Throwable{
        ContentResolver provider = mContext.getContentResolver();

        ContentValues values = TestDb.createTestLocation();
        Uri locationUri = provider.insert(LocationEntry.CONTENT_URI, values);
        Log.d(LOG_TAG, "New Location URI " + locationUri.toString());
        long locationRowId = ContentUris.parseId(locationUri);

        Cursor cursor = provider.query(
                LocationEntry.buildLocationUri(locationRowId),
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        TestDb.compareCursorValue(cursor, values);

        ContentValues weatherValues = TestDb.createTestWeather(locationRowId);
        Uri weatherUri = provider.insert(WeatherEntry.CONTENT_URI, weatherValues);
        Log.d(LOG_TAG, "New Weather: " + weatherUri.toString());

        Cursor weatherCursor = provider.query(
                WeatherEntry.CONTENT_URI,
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        TestDb.compareCursorValue(weatherCursor, weatherValues);

        weatherCursor = provider.query(
                WeatherEntry.buildWeatherLocation(TestDb.TEST_LOCATION),
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        TestDb.compareCursorValue(weatherCursor, weatherValues);

        weatherCursor = provider.query(
                WeatherEntry.buildWeatherLocationWithStartDate(TestDb.TEST_LOCATION, TestDb.TEST_DATE),
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        TestDb.compareCursorValue(weatherCursor, weatherValues);

        weatherCursor = provider.query(
                WeatherEntry.buildWeatherLocationWithDate(TestDb.TEST_LOCATION, TestDb.TEST_DATE),
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        TestDb.compareCursorValue(weatherCursor, weatherValues);
    }

}
