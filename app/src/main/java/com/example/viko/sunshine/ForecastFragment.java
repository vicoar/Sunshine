package com.example.viko.sunshine;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;

/**
 * Created by viko on 04/10/2014.
 */
public class ForecastFragment extends Fragment {

    public static ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
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
        final Activity act = this.getActivity();

        mForecastAdapter = new ArrayAdapter<String>(
               act,
               R.layout.list_item_forecast,
               R.id.list_item_forecast_textview,
               new ArrayList<String>());

       final ListView forecastList = (ListView) rootView.findViewById(R.id.listview_forecast);
       forecastList.setAdapter(mForecastAdapter);
       forecastList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent sendIntent = new Intent(act, DetailActivity.class);
                sendIntent.putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(i));
                startActivity(sendIntent);
            }
       });

       return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
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

            // Create the text message with a string
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="+location));

            // Verify that the intent will resolve to an activity
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        //get the sharepref
        String location = settings.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_label_default));

        FetchWeatherTask fetchWeatherTask= new FetchWeatherTask(this);
        fetchWeatherTask.execute(location);
    }
}