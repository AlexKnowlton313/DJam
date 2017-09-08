package com.example.sfreyman.djam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sfreyman on 4/16/17.
 */


public class CurrentlyPlayingActivity extends AppCompatActivity implements
        ConnectionStateCallback, Player.NotificationCallback{

    private TextView streamTitleTV, streamerNameTV, songTitleTV, artistTitleTV;
    private ImageView streamerProfPicIV, songArtworkIV;
    private boolean playState;  // True if currently playing music; false otherwise.

    private Player mPlayer;
    private String songURI;
    private Date streamStartTime;
    private int differenceTime;
    private static DatabaseReference mDatabase; //firebase
    private Bundle bn;
    private LiveStream update;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currently_playing);
        
        SharedPreferences sharedPreferences = getSharedPreferences("api", Context.MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(getString(R.string.accessToken), "");

        int position = getIntent().getIntExtra("POSITION", -1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setTitle("Currently Playing");

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Get the back button on the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();
        bn = intent.getExtras();

        // UI Element References.
        streamTitleTV       = (TextView) findViewById(R.id.streamTitleTV);
        streamerNameTV      = (TextView) findViewById(R.id.streamerNameTV);
        songTitleTV         = (TextView) findViewById(R.id.songTitleTV);
        artistTitleTV       = (TextView) findViewById(R.id.artistTitleTV);

        streamerProfPicIV   = (ImageView) findViewById(R.id.streamerProfPicIV);
        songArtworkIV       = (ImageView) findViewById(R.id.songArtworkIV);

        /* FIREBASE */
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean found = false;
                for (DataSnapshot stream : dataSnapshot.getChildren()) {
                    if (stream.getValue(LiveStream.class).getUser().equals(bn.getString("user"))) {
                        update = stream.getValue(LiveStream.class);
                        songURI = update.getCurrSongURI();
                        streamStartTime = parseDate(update.getTimeStarted());
                        songTitleTV.setText(update.getSong());
                        artistTitleTV.setText(update.getArtist());
                        new LoadImage(songArtworkIV).execute(update.getAlbumArtworkLarge());
                        found = true;
                    }
                }
                if (!found) {
                    //mPlayer.destroy();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
            }
        });

        // End UI Element References.

        playState = true;  // Not currently playing (listening) at start.

        Config playerConfig = new Config(this, accessToken, "c0cb1b7bf2b949eca66e7e58389bc79e");
        Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer spotifyPlayer) {
                mPlayer = spotifyPlayer;
                mPlayer.addConnectionStateCallback(CurrentlyPlayingActivity.this);
                mPlayer.addNotificationCallback(CurrentlyPlayingActivity.this);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("GoLiveActivity", "Could not initialize player!!!!! : " + throwable.getMessage());
            }
        });

        songURI = bn.getString("songURI");
        streamStartTime = parseDate(bn.getString("date"));

        streamTitleTV.setText(bn.getString("title"));
        streamerNameTV.setText(bn.getString("user"));
        songTitleTV.setText(bn.getString("song"));
        artistTitleTV.setText(bn.getString("artist"));
        new LoadImage(songArtworkIV).execute(bn.getString("cover"));
        new LoadImage(streamerProfPicIV).execute(bn.getString("profPic"));
    }

    /** Method so that back button actually sends us back. */
    @Override
    public boolean onSupportNavigateUp() {
        //mPlayer.destroy();
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            case kSpPlaybackNotifyPlay:
                differenceTime = (int) (new Date().getTime() - streamStartTime.getTime());
                mPlayer.seekToPosition(null, differenceTime);
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in " + differenceTime);
        mPlayer.playUri(null, "spotify:track:" + songURI, 0, 0);
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    private Date parseDate(String date) {
        DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
        try {
            return df.parse(date);
        } catch (Exception e){
            return new Date();
        }
    }

}
