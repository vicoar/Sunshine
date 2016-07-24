package com.example.viko.sunshine;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.viko.sunshine.data.WeatherContract;
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
import java.util.Date;

/**
 * Created by vico on 23/01/2015.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private SimpleCursorAdapter mForecastAdapter;
    private final Context mContext;
    private final String apiKey = "02ce24004ecbaf6ffd869f3c4e62607a";

    public FetchWeatherTask(Context context, SimpleCursorAdapter forecastAdapter) {
        mContext = context;
        mForecastAdapter = forecastAdapter;
    }

    @Override
    protected String[] doInBackground(String... params){
        String format = "json";

        String unit = "metric";
        String locationQuery = params[0];
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
                    .appendQueryParameter("cnt", Integer.toString(numberOfDays))
                    .appendQueryParameter("APPID", apiKey);

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

    public long addLocation(String locationSetting, String cityName, double lat, double lon) {
        Uri locationsUri = LocationEntry.CONTENT_URI;

        long locationId;

        String selectByLocationSetting = LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ?";

        ContentResolver provider = mContext.getContentResolver();

        Cursor cursor = provider.query(
            locationsUri,
            new String[]{LocationEntry._ID},
            selectByLocationSetting,
            new String[]{locationSetting},
            null
        );

        if ( cursor.getCount() == 0 ) {
            ContentValues locationValues = new ContentValues();
            locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(LocationEntry.COLUMN_CITY_NAME,        cityName);
            locationValues.put(LocationEntry.COLUMN_COORD_LAT,        lat);
            locationValues.put(LocationEntry.COLUMN_COORD_LONG,       lon);

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

        ContentResolver provider = mContext.getContentResolver();

        provider.delete(weatherUri,null,null);

        JSONArray weatherArray = json.getJSONArray(OWM_LIST);

        String day;
        String description;
        //String highAndLow;
        ContentValues[] values = new ContentValues[weatherArray.length()];

        for(int i = 0; i < weatherArray.length(); i++) {
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            long dateTime = dayForecast.getLong(OWM_DATETIME);
            day = WeatherContract.getDbDateString(new Date(dateTime*1000));

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

    private void getWeatherDataFromJson(String forecastJsonStr, int numDays, String locationQuery)
            throws JSONException {
        JSONObject json = new JSONObject(forecastJsonStr);

        // Location Keys
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_CITY_COORD = "coord";
        final String OWM_CITY_COORD_LON = "lon";
        final String OWM_CITY_COORD_LAT = "lat";

        JSONObject cityObj    = json.getJSONObject(OWM_CITY);
        JSONObject cityCoords = cityObj.getJSONObject(OWM_CITY_COORD);

        long locationId = addLocation(
            locationQuery,
            cityObj.getString(OWM_CITY_NAME),
            cityCoords.getDouble(OWM_CITY_COORD_LAT),
            cityCoords.getDouble(OWM_CITY_COORD_LON)
        );
        createWeatherForLocation(json, numDays, locationId);
    }

}