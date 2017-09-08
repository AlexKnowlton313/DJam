package com.example.sfreyman.djam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.TrackSearchRequest;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.Track;
import com.wrapper.spotify.models.User;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private static DatabaseReference mDatabase; //firebase

    private static Api api; //spotify

    private static ArrayList<LiveStream> myStreams = new ArrayList<LiveStream>(); //arraylist

    private static LiveStreamAdapter streamAdapter;

    private static ListView streamView;

    private static SharedPreferences sharedPreferences;

    private static Context context;

    private static ArrayList<LiveStream> allUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        // Hide the keyboard
        //hideKeyboardFrom(GoLiveActivity.this, this);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        /* LAYOUT */
        //app bar (title of the screen)
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Create the adapter that will return a fragment for each of the four
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        //R.id.container - the container for fragments
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        /* SHARED PREFS */
        sharedPreferences = getSharedPreferences("api", Context.MODE_PRIVATE);

        /* SPOTIFY API WRAPPER */
        api = Api.builder()
                .clientId("c0cb1b7bf2b949eca66e7e58389bc79e")
                .clientSecret("a4db4cafe4814eddba2a5d9a2450986f")
                .redirectURI("djamspotify://callback")
                .accessToken(sharedPreferences.getString(getString(R.string.accessToken), ""))
                .refreshToken(sharedPreferences.getString(getString(R.string.refreshToken), ""))
                .build();
    }

    //Overflow window
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.log_out) {;
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        //fragment transaction
        private FragmentTransaction transaction;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView;
            int section = getArguments().getInt(ARG_SECTION_NUMBER);

            // CASE 1: live streams tab. CASE 2: go live tab.
            switch (section) {
                case 1:
                    rootView = inflater.inflate(R.layout.live_streams_main, container, false);

                    // here goes the autocompetet thing
                    // Our list of songs
                    final AutoCompleteTextView userAutoComplete = (AutoCompleteTextView) rootView.findViewById(R.id.searchUsers);
                    final ArrayList<LiveStream> users = new ArrayList<>();
                    final UserAdapter userAdapter = new UserAdapter(getContext(), R.layout.user_list, users);
                    userAutoComplete.setAdapter(userAdapter);

                    userAutoComplete.addTextChangedListener(new TextWatcher() {
                        Thread queryUsers;

                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            if (queryUsers !=  null && queryUsers.isAlive()) {
                                queryUsers.interrupt();
                            }

                            if (count == 0) {
                                userAdapter.clear();
                            }
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            final CharSequence username = s;

                            queryUsers = new Thread(new Runnable() {
                                public void run() {

                                    /* FIREBASE */
                                    mDatabase = FirebaseDatabase.getInstance().getReference();

                                    mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            for (DataSnapshot stream : dataSnapshot.getChildren()) {
                                                if (stream.getValue(LiveStream.class).getUser().contains(username.toString())) {
                                                    userAdapter.addLiveStream(stream.getValue(LiveStream.class));
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            // Getting Post failed, log a message
                                        }
                                    });

                                }
                            });

                            queryUsers.start();
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                        }
                    });

                    // When they click a song in the list
                    userAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                            // Grab the song that was clicked
                            LiveStream selected = (LiveStream) parent.getItemAtPosition(position);
                            Intent intent = new Intent(getActivity(), CurrentlyPlayingActivity.class);
                            intent.putExtra("POSITION", position);
                            intent.putExtra("user", selected.getUser());
                            intent.putExtra("title", selected.getTitle());
                            intent.putExtra("cover", selected.getAlbumArtworkLarge());
                            intent.putExtra("song", selected.getSong());
                            intent.putExtra("artist", selected.getArtist());
                            intent.putExtra("profPic", selected.getProfilePic());
                            intent.putExtra("date", selected.getTimeStarted());
                            intent.putExtra("songURI", selected.getCurrSongURI());
                            intent.putExtra("duration", selected.getCurrDuration());

                            getContext().startActivity(intent);

                            // Hide the keyboard
                            hideKeyboardFrom(getContext(), rootView);
                        }
                    });

                    streamView = (ListView) rootView.findViewById(R.id.stream_list);
                    streamAdapter = new LiveStreamAdapter(getActivity().getApplicationContext(), R.layout.live_stream_item, myStreams);
                    streamView.setAdapter(streamAdapter);

                    /* FIREBASE */
                    mDatabase = FirebaseDatabase.getInstance().getReference();

                    mDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            myStreams.clear();
                            for (DataSnapshot stream : dataSnapshot.getChildren()) {
                                myStreams.add(stream.getValue(LiveStream.class));
                            }
                            streamAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // Getting Post failed, log a message
                        }
                    });

                    streamView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Intent intent = new Intent(getActivity(), CurrentlyPlayingActivity.class);
                            intent.putExtra("POSITION", position);
                            intent.putExtra("user", myStreams.get(position).getUser());
                            intent.putExtra("title", myStreams.get(position).getTitle());
                            intent.putExtra("cover", myStreams.get(position).getAlbumArtworkLarge());
                            intent.putExtra("song", myStreams.get(position).getSong());
                            intent.putExtra("artist", myStreams.get(position).getArtist());
                            intent.putExtra("profPic", myStreams.get(position).getProfilePic());
                            intent.putExtra("date", myStreams.get(position).getTimeStarted());
                            intent.putExtra("songURI", myStreams.get(position).getCurrSongURI());
                            intent.putExtra("duration", myStreams.get(position).getCurrDuration());

                            getContext().startActivity(intent);
                        }});
                    break;

                case 2:
                    rootView = inflater.inflate(R.layout.go_live_main, container, false);

                    // Grab the views we want to change
                    final AutoCompleteTextView songAutoComplete = (AutoCompleteTextView) rootView.findViewById(R.id.searchSongList);
                    final EditText currentLiveStreamTitle = (EditText) rootView.findViewById(R.id.liveStreamTitle);
                    final ImageView currentSongAlbumArtwork = (ImageView) rootView.findViewById(R.id.currentSongAlbumArtwork);
                    final TextView currentSongTitle = (TextView) rootView.findViewById(R.id.currentSongTitle);
                    final TextView currentSongArtist = (TextView) rootView.findViewById(R.id.currentSongArtist);
                    final ProgressBar albumLoading = (ProgressBar) rootView.findViewById(R.id.currentSongAlbumArtworkLoading);
                    final TextView pickSongPrompt = (TextView) rootView.findViewById(R.id.pickSongPrompt);
                    final Button goLiveButton = (Button) rootView.findViewById(R.id.goLiveButton);

                    pickSongPrompt.setVisibility(View.VISIBLE);
                    albumLoading.setVisibility(View.GONE);
                    currentLiveStreamTitle.setVisibility(View.GONE);
                    goLiveButton.setVisibility(View.GONE);

                    // Our list of songs
                    final ArrayList<Song> songs = new ArrayList<>();
                    final SongAdapter songAdapter = new SongAdapter(getContext(), R.layout.song_list, songs);
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

                                    SettableFuture<Page<Track>> trackFuture = trackSearchRequest.getAsync();

                                    // Create callbacks in case of success or failure
                                    Futures.addCallback(trackFuture, new FutureCallback<Page<Track>>() {

                                        // Print the genres of the album call is successful
                                        public void onSuccess(Page<Track> page) {
                                            for (Track track : page.getItems()) {
                                                String id = track.getId();
                                                String name = track.getName();
                                                String artist = track.getArtists().get(0).getName();
                                                String albumArtworkLarge = track.getAlbum().getImages().get(0).getUrl();
                                                String albumArtworkSmall = track.getAlbum().getImages().get(1).getUrl();
                                                int duration = track.getDuration();

                                                songAdapter.add(new Song(id, name, artist, albumArtworkLarge, albumArtworkSmall, duration));
                                            }
                                        }
                                        // In case of failure
                                        public void onFailure(Throwable thrown) {
                                        }
                                    });
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
                            // Grab the song that was clicked
                            Song currentSong = (Song) parent.getItemAtPosition(position);
                            // Set the views
                            currentSongTitle.setText(currentSong.getTitle());
                            currentSongArtist.setText(currentSong.getArtist());
                            new LoadImage(currentSongAlbumArtwork).execute(currentSong.getAlbumArtworkLarge());
                            pickSongPrompt.setVisibility(View.GONE);
                            albumLoading.setVisibility(View.VISIBLE);
                            currentLiveStreamTitle.setVisibility(View.VISIBLE);
                            goLiveButton.setVisibility(View.VISIBLE);

                            // Store the current song
                            goLiveButton.setTag(currentSong);

                            // Hide the keyboard
                            hideKeyboardFrom(getContext(), rootView);
                        }
                    });

                    goLiveButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            final Song currentSong = (Song) goLiveButton.getTag();
                            final String liveStreamTitle = currentLiveStreamTitle.getText().toString();
                            if (liveStreamTitle.equals("")) {
                                Toast.makeText(getContext(), "Please Enter the Stream Title.",
                                        Toast.LENGTH_SHORT).show();
                            } else {

                                if (currentSong != null) {
                                    // Create new intent to go to GoLiveActivity
                                    final Intent intent = new Intent(getActivity(), GoLiveActivity.class);

                                    // Pass the song through a parseable extra
                                    intent.putExtra("EXTRA_SONG", currentSong);
                                    intent.putExtra("streamName", liveStreamTitle);

                                    // Start the activity
                                    startActivity(intent);
                                }
                            }
                        }


                    });

                    // When they click the go live button
                    break;

                default:
                    rootView = inflater.inflate(R.layout.live_streams_main, container, false);
                    break;
            }
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Live Streams";
                case 1:
                    return "Go Live";
            }
            return null;
        }
    }

    // Function to hide the keyboard
    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
