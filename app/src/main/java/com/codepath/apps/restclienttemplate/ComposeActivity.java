package com.codepath.apps.restclienttemplate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONException;
import org.parceler.Parcels;

import java.util.Objects;

import okhttp3.Headers;

public class ComposeActivity extends AppCompatActivity {

    public static final String TAG = "ComposeActivity";
    public static final int MAX_TWEET_LENGTH = 280;

    EditText etCompose;
    Button btnTweet;

    TwitterClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        etCompose = findViewById(R.id.etCompose);
        btnTweet = findViewById(R.id.btnTweet);

        client = TwitterApplication.getRestClient(this);

        /*
         * Add twitter compose logo to ComposeActivity's ActionBar
         */
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_vector_compose);
        getSupportActionBar().setDisplayUseLogoEnabled(true);


        // Set click listener on button
        btnTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String tweetContent = etCompose.getText().toString();

                // Edge case: empty tweet content
                if (tweetContent.isEmpty()) {
                    Toast.makeText(ComposeActivity.this,
                            "Sorry, your tweet cannot be empty.",
                            Toast.LENGTH_LONG).show();
                    return; // nothing
                }

                // Edge case: tweet characters exceed max char count
                if (tweetContent.length() > MAX_TWEET_LENGTH) {
                    Toast.makeText(ComposeActivity.this,
                            "Sorry, your tweet is too long.",
                            Toast.LENGTH_LONG).show();
                    return; // nothing
                }

                // Make an API call to Twitter to publish the new tweet via POST request
                client.postTweet(tweetContent, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Headers headers, JSON json) {
                        Log.i(TAG, "onSuccess to publish tweet!");

                        try {
                            Tweet tweet = Tweet.fromJson(json.jsonObject);
                            Log.i(TAG, "Published tweet says: " + tweet.body);

                            /*
                             * Publish tweet back to parent activity,
                             * locally (rather than publish and having to refresh the page again)
                             */
                            Intent intent = new Intent();
                            // Pass relevant data back as a result
                            intent.putExtra("tweet", Parcels.wrap(tweet));
                            // Activity finished ok, set result code and bundle data for response
                            setResult(RESULT_OK, intent);
                            // closes the activity, pass data to parent
                            finish();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                        Log.e(TAG, "onFailure to publish tweet", throwable);
                    }
                });

                //Toast.makeText(ComposeActivity.this, "New Tweet: "+ tweetContent, Toast.LENGTH_LONG).show();
            }
        });

        //
    }
}