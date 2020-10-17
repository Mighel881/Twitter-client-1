package com.codepath.apps.restclienttemplate;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.models.TweetDao;
import com.codepath.apps.restclienttemplate.models.TweetWithUser;
import com.codepath.apps.restclienttemplate.models.User;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.github.scribejava.apis.TwitterApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Headers;

public class TimelineActivity extends AppCompatActivity {

    public static final String TAG = "TimelineActivity";
    // REQUEST_CODE can be any value we like, used to determine the result type later
    public static final int REQUEST_CODE = 20;

    TwitterClient client;
    RecyclerView rvTweets;
    List<Tweet> tweets;
    TweetsAdapter adapter;
    SwipeRefreshLayout swipeContainer;
    FloatingActionButton fabCompose;

    TweetDao tweetDao;

    // Instantiate Endless Recycler View Scroll Listener
    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        client = TwitterApplication.getRestClient(this);
        tweetDao = ((TwitterApplication) getApplicationContext()).getMyDatabase().tweetDao();

        /*
         * Add twitter bird logo to ActionBar
         */
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_launcher_twitter);
        getSupportActionBar().setDisplayUseLogoEnabled(true);


        /*
         * Setup swipe refresh listener which triggers new data loading
         */
        swipeContainer = findViewById(R.id.swipeContainer);
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "onRefresh, fetching new data!");
                // Your code to refresh the list here.
                populateHomeTimeline();
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
//                fetchTimelineAsync(0);
            }
        });


        // Find the recycler view for list of tweets
        rvTweets = findViewById(R.id.rvTweets);
        // Init the list of tweets and adapter
        tweets = new ArrayList<>();
        adapter = new TweetsAdapter(this, tweets);
        // Configure Recycle view setup: layout manager and adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(layoutManager);
        rvTweets.setAdapter(adapter);


        /*
         * Setup Endless Recycler View Scroll Listener to new endless tweets
         */
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.i(TAG, "onLoadMore: " + page);
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadMoreData();
            }
        };
        // Adds the scroll listener to RecyclerView
        rvTweets.addOnScrollListener(scrollListener);


        /*
         * Add floating action bar to compose tweet
         */
        fabCompose = findViewById(R.id.fabCompose);
//        fabCompose.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Navigate to the compose activity
//                Intent intent = new Intent();
//                startActivityForResult(intent, REQUEST_CODE);
//            }
//        });


        // Query for existing tweets in the DB
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: Showing data from DB");

                List<TweetWithUser> tweetWithUsers = tweetDao.recentItems();
                List<Tweet> tweetsFromDB = TweetWithUser.getTweetList(tweetWithUsers);

                // Refresh the adapter that contains tweets
                adapter.clear();
                adapter.addAll(tweetsFromDB);
            }
        });

        populateHomeTimeline();
    }

    private void loadMoreData() {
        // 1. Send an API request to retrieve appropriate paginated data
        client.getNextPageOfTweets(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess for loadMoreData!" + json.toString());

                // 2. Deserialize and construct new model objects from the API response
                JSONArray jsonArray = json.jsonArray;
                try {
                    List<Tweet> tweets = Tweet.fromJsonArray(jsonArray);

                    // 3. Append the new data objects to the existing set of items inside the array of items
                    // 4. Notify the adapter of the new items made with `notifyItemRangeInserted()`
                    adapter.addAll(tweets);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure for loadMoreData!", throwable);

            }
        },
                // second parameter, get id of the last tweet
                tweets.get(tweets.size()-1).id
        );
    }


    /**
     * Adding "Compose" Tweet as action item on ActionBar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Check if Compose icon is selected
        if (item.getItemId() == R.id.compose || item.getItemId() == R.id.fabCompose) {
            // Compose icon has been selected
            // Toast.makeText(this, "Compose!", Toast.LENGTH_SHORT).show();

            // Navigate to the compose activity
            Intent intent = new Intent(this, ComposeActivity.class);
            startActivityForResult(intent, REQUEST_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle the result of the sub-activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            // Get data from sub-activity intent (tweet)
            Tweet tweet = Parcels.unwrap( data.getParcelableExtra("tweet") );

            // Update recyclerView with the new tweet
            // Modify data source of tweets
            tweets.add(0, tweet);
            // Update the adapter
            adapter.notifyItemChanged(0);
            rvTweets.smoothScrollToPosition(0);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    private void populateHomeTimeline() {
        client.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess populateHomeTimeline: " + json.toString());
                // create an Adapter and populate RecycleView with the data we get back from json

                JSONArray jsonArray = json.jsonArray;
                try {
                    final List<Tweet> tweetsFromNetwork = Tweet.fromJsonArray(jsonArray);
                    // clear out data first
                    adapter.clear();
                    // change list of tweets
                    adapter.addAll(tweetsFromNetwork);
                    // Now we call setRefreshing(false) to signal refresh has finished
                    swipeContainer.setRefreshing(false);


                    // Query for existing tweets in the DB
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "run: Saving data into DB");
                            // Inserting both Tweets and Users to their respective tables
                            List<User> usersFromNetwork = User.fromJsonTweetArray(tweetsFromNetwork);
                            tweetDao.insertModel( usersFromNetwork.toArray(new User[0]) );
                            tweetDao.insertModel( tweetsFromNetwork.toArray(new Tweet[0]) );
                        }
                    });


                    // methods used before adapter has the above methods implemented:
                    //tweets.addAll( Tweet.fromJsonArray(jsonArray) );
                    //adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    Log.e(TAG, "Json Exception", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure " + response, throwable);
            }
        });
    }
}