package com.example.viko.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.example.viko.sunshine.data.WeatherContract;
import com.example.viko.sunshine.data.WeatherContract.WeatherEntry;
import com.example.viko.sunshine.data.WeatherContract.LocationEntry;

import java.sql.SQLException;


/**
 * Created by vico on 18/01/2015.
 */
public class WeatherProvider extends ContentProvider {

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    private static UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDbHelper mOpenHelper;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherEntry.TABLE_NAME + " INNER JOIN " + LocationEntry.TABLE_NAME +
                " ON " + WeatherEntry.TABLE_NAME  + "." + WeatherEntry.COLUMN_LOC_KEY +
                 " = " + LocationEntry.TABLE_NAME + "." + LocationEntry._ID
        );
    }

    private static final String sLocationSettingSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ?";

    private static final String sLocationSettingWithStartDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
            WeatherEntry.TABLE_NAME  + "." + WeatherEntry.COLUMN_DATETEXT + " >= ?";

    private static final String sLocationSettingAndDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
            WeatherEntry.TABLE_NAME  + "." + WeatherEntry.COLUMN_DATETEXT + " = ?";


    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER+"/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER+"/*/*", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION+"/#", LOCATION_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    private Cursor getWeatherByLocation(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherEntry.getStartDateFromUri(uri);

        String selection;
        String[] selectionArgs;

        if ( startDate == null ) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{ locationSetting };
        } else {
            selection = sLocationSettingWithStartDateSelection;
            selectionArgs = new String[]{ locationSetting, startDate };
        }

        return sWeatherByLocationSettingQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    private Cursor getWeatherByLocationAndDate(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherEntry.getDateFromUri(uri);

        String selection = sLocationSettingAndDateSelection;
        String[] selectionArgs = new String[]{ locationSetting, startDate };

        return sWeatherByLocationSettingQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)){
            case WEATHER_WITH_LOCATION_AND_DATE: {
                retCursor = getWeatherByLocationAndDate(uri, projection, sortOrder);
                break;
            }
            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherByLocation(uri, projection, sortOrder);
                break;
            }
            case WEATHER: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                    WeatherContract.WeatherEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
                );
                break;
            }
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case LOCATION_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        WeatherContract.LocationEntry._ID + " = '"+ ContentUris.parseId(uri) + "' ",
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri "+ uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri "+ uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values){
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long locationRowId;
        Uri returningUri = null;

        switch (sUriMatcher.match(uri)){
            case WEATHER: {
                locationRowId = db.insert(WeatherEntry.TABLE_NAME, null, values);
                if ( locationRowId > 0 ) {
                    returningUri = WeatherEntry.buildWeatherUri(locationRowId);
                }
                break;
            }
            case LOCATION: {
                locationRowId = db.insert(LocationEntry.TABLE_NAME, null, values);
                if ( locationRowId > 0 ) {
                    returningUri = LocationEntry.buildLocationUri(locationRowId);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri "+ uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returningUri;
    }

    @Override
    public int delete(Uri uri, String whereClause, String[] whereValues) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsAffected;

        switch (sUriMatcher.match(uri)){
            case WEATHER: {
                rowsAffected = db.delete(WeatherEntry.TABLE_NAME, whereClause, whereValues);
                break;
            }
            case LOCATION: {
                rowsAffected = db.delete(LocationEntry.TABLE_NAME, whereClause, whereValues);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri "+ uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }

    @Override
    public int update(Uri uri, ContentValues values, String whereClause, String[] whereValues) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsAffected;

        switch (sUriMatcher.match(uri)){
            case WEATHER: {
                rowsAffected = db.update(WeatherEntry.TABLE_NAME, values, whereClause, whereValues);
                break;
            }
            case LOCATION: {
                rowsAffected = db.update(LocationEntry.TABLE_NAME, values, whereClause, whereValues);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri "+ uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsAffected;
    }
}
