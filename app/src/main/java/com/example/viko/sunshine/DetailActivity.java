package com.example.viko.sunshine;

import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class DetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent sendIntent = new Intent(this, SettingsActivity.class);
            startActivity(sendIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment {
        String LOG_TAG = DetailFragment.class.getSimpleName();

        String mForecastStr;
        String mSunshineHashTag = " #sunshineApp";

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);
            // Locate MenuItem with ShareActionProvider
            MenuItem item = menu.findItem(R.id.action_share);
            // Fetch and store ShareActionProvider
            ShareActionProvider mShareActionProvider = new ShareActionProvider(this.getActivity());
            mShareActionProvider.setShareIntent(createShareIntent());
            MenuItemCompat.setActionProvider(item, mShareActionProvider);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            Intent detailIntent = getActivity().getIntent();
            mForecastStr = detailIntent.getStringExtra(Intent.EXTRA_TEXT);

            TextView detailTextView = (TextView) rootView.findViewById(R.id.detail_textview);
            detailTextView.setText(mForecastStr);

            return rootView;
        }

        public Intent createShareIntent(){
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            String shareStr = mForecastStr + mSunshineHashTag;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareStr);
            shareIntent.setType("text/plain");
            return shareIntent;
        }
    }
}
