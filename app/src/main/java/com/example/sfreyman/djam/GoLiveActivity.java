package com.example.sfreyman.djam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.TrackSearchRequest;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.Track;
import com.wrapper.spotify.models.User;

import java.util.ArrayList;
import java.util.Date;

public class GoLiveActivity extends AppCompatActivity implements
        ConnectionStateCallback, Player.NotificationCallback{

    private static Api api; //spotify
    private static User user;
    private String songURI;

    private static DatabaseReference mDatabase; //firebase

    //private Drawable play;

    private Player mPlayer;
    //private Song currentSong;
    private String streamTitle;

    private ArrayList<Song> queue = new ArrayList<Song>();

    final Handler handler = new Handler();
    ProgressBar progressBar;
    TextView currentQueueGoLiveTitle;
    CardView currentQueueGoLiveCard;

    TextView timeTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        SharedPreferences sharedPreferences = getSharedPreferences("api", Context.MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(getString(R.string.accessToken), "");
        String refreshToken = sharedPreferences.getString(getString(R.string.refreshToken), "");

        api = Api.builder()
                .clientId("c0cb1b7bf2b949eca66e7e58389bc79e")
                .clientSecret("a4db4cafe4814eddba2a5d9a2450986f")
                .redirectURI("djamspotify://callback")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        /* FIREBASE */
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //app bar (title of the screen)
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if(getSupportActionBar() != null) {
            // Set the title on the action bar
            //getSupportActionBar().setTitle("Go Live");

            // give me the back button on the action bar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Hide the keyboard
        //hideKeyboardFrom(GoLiveActivity.this, this);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        Song currentSong = (Song) bundle.get("EXTRA_SONG");
        queue.add(currentSong);
        streamTitle = bundle.getString("streamName");
        int firstSongDuration = bundle.getInt("duration");

        songURI = queue.get(0).getId();

        final AutoCompleteTextView songAutoComplete = (AutoCompleteTextView) findViewById(R.id.searchSongListGoLive);

        // Our list of songs
        final ArrayList<Song> songs = new ArrayList<>();
        final SongAdapter songAdapter = new SongAdapter(getApplicationContext(), R.layout.song_list, songs);
        songAutoComplete.setAdapter(songAdapter);

        songAutoComplete.addTextChangedListener(new TextWatcher() {
            Thread querySongs;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (querySongs !=  null && querySongs.isAlive()) {
                    querySongs.interrupt();
                }

                if (count == 0) {
                    songAdapter.clear();
                }
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final TrackSearchRequest trackSearchRequest = api.searchTracks(s.toString()).limit(7).build();

                querySongs = new Thread(new Runnable() {
                    public void run() {
                        // Query the tracks

                        try {
                            final Page<Track> trackSearchResult = trackSearchRequest.get();
                            for (Track track : trackSearchResult.getItems()) {
                                String id = track.getId();
                                String name = track.getName();
                                String artist = track.getArtists().get(0).getName();
                                String albumArtworkLarge = track.getAlbum().getImages().get(0).getUrl();
                                String albumArtworkSmall = track.getAlbum().getImages().get(1).getUrl();
                                int duration = track.getDuration();

                                songAdapter.addSong(new Song(id, name, artist, albumArtworkLarge, albumArtworkSmall, duration));
                            }
                        } catch (Exception e) {
                        }
                    }
                });

                querySongs.start();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // When they click a song in the list
        songAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {

                if (queue.size() == 2) {
                    Toast.makeText(GoLiveActivity.this, "Can Only Queue up to 2 Songs.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    currentQueueGoLiveTitle = (TextView) findViewById(R.id.currentQueueGoLiveTitle);
                    CardView currentQueueGoLiveCard = (CardView) findViewById(R.id.currentQueueGoLiveCard);
                    TextView songTitle = (TextView) findViewById(R.id.songTitleGoLiveNext);
                    TextView songArtist = (TextView) findViewById(R.id.songArtistGoLiveNext);
                    Icon songCover = (Icon) findViewById(R.id.songAlbumArtworkGoLiveNext);

                    currentQueueGoLiveTitle.setText(getText(R.string.currentQueueGoLiveTitle));
                    currentQueueGoLiveCard.setVisibility(View.VISIBLE);

                    // Grab the song that was clicked
                    Song currentSong = (Song) parent.getItemAtPosition(position);
                    // Set the views
                    songTitle.setText(currentSong.getTitle());
                    songArtist.setText(currentSong.getArtist());
                    new LoadImage(songCover).execute(currentSong.getAlbumArtworkLarge());
                    queue.add(currentSong);
                }
            }
        });

        Config playerConfig = new Config(this, accessToken, "c0cb1b7bf2b949eca66e7e58389bc79e");
        Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer spotifyPlayer) {
                mPlayer = spotifyPlayer;
                mPlayer.addConnectionStateCallback(GoLiveActivity.this);
                mPlayer.addNotificationCallback(GoLiveActivity.this);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("GoLiveActivity", "Could not initialize player!!!!! : " + throwable.getMessage());
            }
        });

        currentQueueGoLiveTitle = (TextView) findViewById(R.id.currentQueueGoLiveTitle);
        currentQueueGoLiveCard = (CardView) findViewById(R.id.currentQueueGoLiveCard);

        TextView currentTitle = (TextView) findViewById(R.id.currentlyPlayingTitle);
        TextView currentArtist = (TextView) findViewById(R.id.currentlyPlayingArtist);
        Icon currentCover = (Icon) findViewById(R.id.currentlyPlayingAlbumArt);

        currentTitle.setText(queue.get(0).getTitle());
        currentArtist.setText(queue.get(0).getArtist());
        new LoadImage(currentCover).execute(queue.get(0).getAlbumArtworkSmall());

        progressBar = (ProgressBar) findViewById(R.id.currentlyPlayingTimeBar);
        timeTV      = (TextView)    findViewById(R.id.currentlyPlayingTime);
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);

        final SharedPreferences sharedPref = this.getSharedPreferences("api", Context.MODE_PRIVATE);
        String username = sharedPref.getString(getString(R.string.username), "");
        mDatabase.child(username).removeValue();

        super.onDestroy();

    }

    /*//Overflow window
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        // If log out
        if (id == R.id.log_out) {
            // End current activity
            finish();

            // Take us to login. this is not setup correctly right now though...
            Intent intent = new Intent (this, SignInActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    // back button actually sends us back
    @Override
    public boolean onSupportNavigateUp() {
        //mPlayer.destroy();
        onBackPressed();
        return true;
    }

    // Function to hide the keyboard
    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            case kSpPlaybackNotifyPlay:
                progressBar.setProgress(0);
                int duration = queue.get(0).getDuration();
                progressBar.setMax(duration);
                timeTV.setText("0:00/0:00");

                createLiveStream(streamTitle, queue.get(0).getAlbumArtworkLarge(), queue.get(0).getTitle(),
                        queue.get(0).getArtist(), queue.get(0).getId(), queue.get(0).getDuration());

                final Runnable task = new ProgBarRunnable(duration, progressBar, handler, timeTV);
                handler.postDelayed(task, 500);

                break;
            // Handle event type as necessary
            case kSpPlaybackNotifyTrackDelivered:
                queue.remove(0);
                progressBar.setProgress(0);
                timeTV.setText("0:00/0:00");
                if (queue.size() == 0) {
                    finish();
                } else {
                    currentQueueGoLiveTitle.setText(getText(R.string.nextSongPrompt));
                    currentQueueGoLiveCard.setVisibility(View.GONE);

                    songURI = queue.get(0).getId();
                    int dur = queue.get(0).getDuration();
                    createLiveStream(streamTitle, queue.get(0).getAlbumArtworkLarge(), queue.get(0).getTitle(),
                            queue.get(0).getArtist(), queue.get(0).getId(), queue.get(0).getDuration());

                    new Thread() {
                        public void run(){
                            mPlayer.playUri(null,"spotify:track:"+songURI,0,0);
                        }
                    }.start();

                    final Runnable t = new ProgBarRunnable(dur, progressBar, handler, timeTV);
                    handler.postDelayed(t, 500);


                    TextView currentTitle = (TextView) findViewById(R.id.currentlyPlayingTitle);
                    TextView currentArtist = (TextView) findViewById(R.id.currentlyPlayingArtist);
                    Icon currentCover = (Icon) findViewById(R.id.currentlyPlayingAlbumArt);

                    currentTitle.setText(queue.get(0).getTitle());
                    currentArtist.setText(queue.get(0).getArtist());
                    new LoadImage(currentCover).execute(queue.get(0).getAlbumArtworkSmall());


                    TextView songTitle = (TextView) findViewById(R.id.songTitleGoLiveNext);
                    TextView songArtist = (TextView) findViewById(R.id.songArtistGoLiveNext);
                    Icon songCover = (Icon) findViewById(R.id.songAlbumArtworkGoLiveNext);

                    // Set the views
                    songTitle.setText("Next Song Title");
                    songArtist.setText("Next Song Artist");
                    songCover.setImageResource(R.drawable.blank);

                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.toString());

        switch (error.toString()) {
            // Handle error type as necessary
            default:
                break;
        }
    }


    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
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

    private void createLiveStream(final String liveStreamName, final String albumArtworkLarge,
                                         final String title, final String artist, final String URI, final int duration) {
        SharedPreferences sharedPreferences = getSharedPreferences("api", Context.MODE_PRIVATE);

        // create a liveStream obj
        LiveStream newLV;

        String username = sharedPreferences.getString("username", "");
        String profilePicture = sharedPreferences.getString("profilePicture", "");

        Date date = new Date();

        // Create and push the new stream
        newLV = new LiveStream(username, profilePicture, liveStreamName, title, artist, URI, albumArtworkLarge, date.toString(), duration);

        mDatabase.child(username).setValue(newLV);
    }
}
