package com.example.viko.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.viko.sunshine.app.Utility;
import com.example.viko.sunshine.data.WeatherContract;
import com.example.viko.sunshine.data.WeatherContract.WeatherEntry;
import com.example.viko.sunshine.data.WeatherContract.LocationEntry;

import java.util.Date;

/**
 * Created by viko on 04/10/2014.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    String LOG_TAG = ForecastFragment.class.getSimpleName();

    public static SimpleCursorAdapter mForecastAdapter;

    private static final int FORECAST_LOADER = 0;
    private String mLocation;

    private static final String[] FORECAST_COLUMNS = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING
    };

    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;

    public ForecastFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mForecastAdapter = new SimpleCursorAdapter(
            getActivity(),
            R.layout.list_item_forecast,
            null,
            new String[]{
                WeatherEntry.COLUMN_DATETEXT,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_MAX_TEMP,
                WeatherEntry.COLUMN_MIN_TEMP
            },
            new int[]{
                R.id.list_item_date_textview,
                R.id.list_item_forecast_textview,
                R.id.list_item_high_textview,
                R.id.list_item_low_textview
            },
            0
        );

        mForecastAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                boolean isMetric = Utility.isMetric(getActivity());
                switch (columnIndex) {
                    case COL_WEATHER_MAX_TEMP:
                    case COL_WEATHER_MIN_TEMP: {
                        // we have to do some formatting and possibly a conversion
                        ((TextView) view).setText(Utility.formatTemperature(
                                cursor.getDouble(columnIndex), isMetric));
                        return true;
                    }
                    case COL_WEATHER_DATE: {
                        String dateString = cursor.getString(columnIndex);
                        TextView dateView = (TextView) view;
                        dateView.setText(Utility.formatDate(dateString));
                        return true;
                    }
                }
                return false;
            }
        });

       final ListView forecastList = (ListView) rootView.findViewById(R.id.listview_forecast);
       forecastList.setAdapter(mForecastAdapter);
       forecastList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
               Cursor cursor = mForecastAdapter.getCursor();
               if (cursor != null && cursor.moveToPosition(position)) {
                   String dateString = Utility.formatDate(cursor.getString(COL_WEATHER_DATE));
                   String weatherDescription = cursor.getString(COL_WEATHER_DESC);

                   boolean isMetric = Utility.isMetric(getActivity());
                   String high = Utility.formatTemperature(
                           cursor.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
                   String low = Utility.formatTemperature(
                           cursor.getDouble(COL_WEATHER_MIN_TEMP), isMetric);

                   String detailString = String.format("%s - %s - %s/%s",
                           dateString, weatherDescription, high, low);

                   Intent intent = new Intent(getActivity(), DetailActivity.class)
                           .putExtra(Intent.EXTRA_TEXT, detailString);
                   startActivity(intent);
               }
           }
       });

       return rootView;
    }

    @Override
    public Loader onCreateLoader(int i, Bundle bundle) {
        String startDate = WeatherContract.getDbDateString(new Date());

        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocation(mLocation);

        Log.d(LOG_TAG, "Uri: " +weatherForLocationUri.toString());

        return new CursorLoader(
            getActivity(),
            weatherForLocationUri,
            FORECAST_COLUMNS,
            null,
            null,
            sortOrder
        );
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mForecastAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if ( id == R.id.action_refresh ){
            updateWeather();
            return true;
        }
        if ( id == R.id.action_show_location ){
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String location = settings.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_label_default));

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="+location));

            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void updateWeather() {
        String location = Utility.getPreferredLocation(this.getActivity());

        FetchWeatherTask fetchWeatherTask= new FetchWeatherTask(getActivity());
        fetchWeatherTask.execute(location);
    }
}