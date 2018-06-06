package com.example.andrea.guardian2;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<wildlife>> {
    private static final String GUARDIAN_REQUEST_URL = "http://content.guardianapis.com/search";
    // Constant value for the news loader ID. We can choose any integer.
    private static final int NEWS_LOADER_ID = 1;

    //adapter for News
    private WildlifeAdapter newsAdapter;

    //warning message
    private String messageForUser;
    // Empty text view
    private TextView mEmptyStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);
        //find ListView in news_list.xml
        ListView newsListView = findViewById(R.id.newsList);

        //no news were found, display info on screen
        mEmptyStateTextView = findViewById(R.id.noNews);
        newsListView.setEmptyView(mEmptyStateTextView);

        //create new adapter
        newsAdapter = new WildlifeAdapter(this, new ArrayList<wildlife>());
        //set adapter on ListView
        newsListView.setAdapter(newsAdapter);
        //set item onItemClick listener on ListView and open web page of news
        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                //find the news which was clicked on
                wildlife clickedNews = newsAdapter.getItem(position);
                //convert String URL into URI object
                assert clickedNews != null;
                Uri newsURI = Uri.parse(clickedNews.getUrl());
                // create new intent
                Intent webNewsIntent = new Intent(Intent.ACTION_VIEW, newsURI);
                // check if any browser is available, if not display toast message
                PackageManager packageManager = getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(webNewsIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                boolean isIntentSafe = activities.size() > 0;
                if (isIntentSafe) {
                    // start created intent
                    startActivity(webNewsIntent);

                } else {
                    String message = getString(R.string.no_browser);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }

            }
        });

        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connectivityMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get details on the currently active default data network
        assert connectivityMgr != null;
        NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
        // If there is a network connection, fetch data
        if (networkInfo != null && networkInfo.isConnected()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();
            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            loaderManager.initLoader(NEWS_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            View loadingIndicator = findViewById(R.id.loading_info);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            messageForUser = (String) getText(R.string.no_connection);
            warningMessage(messageForUser);
        }
    }

    //Loader methods
    @Override
    public Loader<List<wildlife>> onCreateLoader(int id, Bundle args) {

        //rewrite the query according to users preference

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // getString retrieves a String value from the preferences. The second parameter is the default value for this preference.
        String sectionChoice = sharedPrefs.getString(
                getString(R.string.settings_section_key),
                getString(R.string.settings_section_default));

        //for date order
        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default)
        );

        // parse breaks apart the URI string that's passed into its parameter
        Uri baseUri = Uri.parse(GUARDIAN_REQUEST_URL);

        // buildUpon prepares the baseUri that we just parsed so we can add query parameters to it
        Uri.Builder uriBuilder = baseUri.buildUpon();

        //http://content.guardianapis.com/search?show-fields=thumbnail&show-tags=contributor&q=future&order-by=newest&from-date=2017-06-01&api-key=test

        // Append query parameter and its value.
        uriBuilder.appendQueryParameter("show-fields", "thumbnail");
        uriBuilder.appendQueryParameter("show-tags", "contributor");
        uriBuilder.appendQueryParameter("q", "future");
        uriBuilder.appendQueryParameter("section", sectionChoice);
        uriBuilder.appendQueryParameter("order-by", orderBy);
        uriBuilder.appendQueryParameter("from-date", "2015-01-01");
        uriBuilder.appendQueryParameter("api-key", "test");

// Return the completed uri https://content.guardianapis.com/search?section=business&api-key=test
        // Create a new loader for the given URL
        return new WildLifeLoader( this, uriBuilder.toString());

    }

    @Override
    public void onLoadFinished(Loader<List<wildlife>> loader, List<wildlife> news) {


        //Hide loading indicator because data were loaded
        View loadingIndicator = findViewById(R.id.loading_info);
        loadingIndicator.setVisibility(View.GONE);
        // Clear the adapter of previous earthquake data
        newsAdapter.clear();
        // If there is a valid list of {@link Earthquake}s, then add them to the adapter's
        // data set. This will trigger the ListView to update.
        if (news != null && !news.isEmpty()) {
            newsAdapter.addAll(news);
            if (news.isEmpty()) {
                // Set empty state text view to display
                messageForUser = (String) getText(R.string.sorry_there_are_no_news_to_display);
                warningMessage(messageForUser);
            }


        }


    }

    @Override
    public void onLoaderReset(Loader<List<wildlife>> loader) {
// Loader reset, so we can clear out our existing data.
        newsAdapter.clear();
    }

    /**
     * hides the loading indicator and displays a message with explanation
     */
    private void warningMessage(String messageForUser) {
        // Hide progress indicator
        View loadingIndicator = findViewById(R.id.loading_info);
        loadingIndicator.setVisibility(View.GONE);
        // set text
        mEmptyStateTextView.setVisibility(View.VISIBLE);
        mEmptyStateTextView.setText(messageForUser);
    }

    // for preferences
    @Override
    // This method initialize the contents of the Activity's options menu.
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the Options Menu we specified in XML
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // for preferences
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //this method passes the menuitem that is selected
        int id = item.getItemId(); //In our case, our menu only has one item, android:id="@+id/action_settings".
        if (id == R.id.action_settings) { //in the main.xml
            Intent settingsIntent = new Intent(this, settings_activity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }





}
