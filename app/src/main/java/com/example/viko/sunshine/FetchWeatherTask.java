package com.example.viko.sunshine;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.viko.sunshine.data.WeatherContract.LocationEntry;
import com.example.viko.sunshine.data.WeatherContract.WeatherEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by vico on 23/01/2015.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private Activity mActivity;
    private ArrayAdapter<String> mAdapter;
    private ContentResolver provider;
    private String locationQuery;

    public FetchWeatherTask(Activity activity, ArrayAdapter<String> adapter){ //constructor
        this.mActivity  = activity;
        this.mAdapter   = adapter;
        this.provider = mActivity.getContentResolver();
    }

    @Override
    protected Void doInBackground(String... params){
        String format = "json";
        String unit = "metric";
        locationQuery = params[0];
        int numberOfDays = 7;

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        try {// Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("api.openweathermap.org")
                    .appendPath("data")
                    .appendPath("2.5")
                    .appendPath("forecast")
                    .appendPath("daily")
                    .appendQueryParameter("q", locationQuery)
                    .appendQueryParameter("mode", format)
                    .appendQueryParameter("units", unit)
                    .appendQueryParameter("cnt", Integer.toString(numberOfDays));

            String apiUrl = builder.build().toString();
            URL url = new URL(apiUrl);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            getWeatherDataFromJson(forecastJsonStr, numberOfDays, locationQuery);
        } catch (Exception e ){
            Log.e(LOG_TAG, e.getMessage(), e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void x) {
        Uri weatherByLocation = WeatherEntry.buildWeatherLocation(locationQuery);
        Cursor cursor = provider.query(weatherByLocation, null ,null, null, null);

        mAdapter.clear();

        while ( cursor.moveToNext() ) {
            mAdapter.add(formatCursor(cursor));
        }
        mAdapter.notifyDataSetChanged();
        cursor.close();
    }

    private String getDateString(long time){
        Date date = new Date(time * 1000);
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyymmdd");
        String out = dbFormat.format(date).toString();
        return out;
    }

    private String getReadableDateString(String dateStr) throws ParseException{
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyymmdd");
        Date date = dbFormat.parse(dateStr);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        String out = format.format(date).toString();
        return out;
    }

    private String formatCursor(Cursor cursor) {
        int dateIdx = cursor.getColumnIndex(WeatherEntry.COLUMN_DATETEXT);
        String dateStr = cursor.getString(dateIdx);
        String day = "";

        int descIdx = cursor.getColumnIndex(WeatherEntry.COLUMN_SHORT_DESC);
        String description = cursor.getString(descIdx);

        int highIdx = cursor.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP);
        double high = cursor.getDouble(highIdx);

        int lowIdx = cursor.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP);
        double low = cursor.getDouble(lowIdx);

        try{
            day = getReadableDateString(dateStr);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        String highAndLow = formatHighLows(high, low);
        return day + " - " + description + " - " + highAndLow;
    }

    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(convertUnits(high));
        long roundedLow = Math.round(convertUnits(low));

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    private long createLocation(JSONObject json, String locationSetting) throws JSONException {
        // Location Keys
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_CITY_COORD = "coord";
        final String OWM_CITY_COORD_LON = "lon";
        final String OWM_CITY_COORD_LAT = "lat";

        Uri locationsUri = LocationEntry.CONTENT_URI;

        long locationId;

        String selectByLocationSetting = LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ?";

        Cursor cursor = provider.query(
            locationsUri,
            null,
            selectByLocationSetting,
            new String[]{locationSetting},
            null
        );

        if ( cursor.getCount() == 0 ) {
            JSONObject cityObj    = json.getJSONObject(OWM_CITY);
            JSONObject cityCoords = cityObj.getJSONObject(OWM_CITY_COORD);

            ContentValues locationValues = new ContentValues();
            locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(LocationEntry.COLUMN_CITY_NAME,        cityObj.getString(OWM_CITY_NAME));
            locationValues.put(LocationEntry.COLUMN_COORD_LAT,        cityCoords.getDouble(OWM_CITY_COORD_LON));
            locationValues.put(LocationEntry.COLUMN_COORD_LONG,       cityCoords.getDouble(OWM_CITY_COORD_LAT));

            Uri locationUri = provider.insert(locationsUri, locationValues);
            locationId = ContentUris.parseId(locationUri);
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(LocationEntry._ID);
            locationId = cursor.getLong(idx);
        }
        cursor.close();

        return locationId;
    }

    private void createWeatherForLocation(JSONObject json, int numDays, long locationId) throws JSONException {
        // Weather Keys
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_DEG = "deg";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_PRESSURE = "pressure";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";
        final String OWM_SPEED = "speed";
        final String OWM_ID = "id";

        Uri weatherUri = WeatherEntry.CONTENT_URI;

        provider.delete(weatherUri,null,null);

        JSONArray weatherArray = json.getJSONArray(OWM_LIST);

        String day;
        String description;
        //String highAndLow;
        ContentValues[] values = new ContentValues[weatherArray.length()];

        for(int i = 0; i < weatherArray.length(); i++) {
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            long dateTime = dayForecast.getLong(OWM_DATETIME);
            day = getDateString(dateTime);

            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherEntry.COLUMN_LOC_KEY,    locationId);
            weatherValues.put(WeatherEntry.COLUMN_DATETEXT,   day);
            weatherValues.put(WeatherEntry.COLUMN_DEGREES,    dayForecast.getDouble(OWM_DEG));
            weatherValues.put(WeatherEntry.COLUMN_HUMIDITY,   dayForecast.getDouble(OWM_HUMIDITY));
            weatherValues.put(WeatherEntry.COLUMN_PRESSURE,   dayForecast.getDouble(OWM_PRESSURE));
            weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP,   high);
            weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP,   low);
            weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, dayForecast.getDouble(OWM_SPEED));
            weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherObject.getInt(OWM_ID));

            values[i] = weatherValues;
        }

        provider.bulkInsert(weatherUri, values);
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getWeatherDataFromJson(String forecastJsonStr, int numDays, String locationQuery)
            throws JSONException {

        JSONObject forecastJson = new JSONObject(forecastJsonStr);

        long locationId = createLocation(forecastJson, locationQuery);
        createWeatherForLocation(forecastJson, numDays, locationId);
    }

    private double convertUnits(double temp){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
        String defaultUnits = mActivity.getString(R.string.pref_unit_default);
        String units = settings.getString(mActivity.getString(R.string.pref_unit_key), defaultUnits);

        if ( !units.equals(defaultUnits) ){
            return temp * 1.8 + 32;
        }

        return temp;
    }
}