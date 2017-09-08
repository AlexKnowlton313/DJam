package com.example.sfreyman.djam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.annotation.NonNull;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * An adapter for the songs when searching for a song.
 */

class SongAdapter extends ArrayAdapter<Song> {
    private ArrayList<Song> songs; //items
    private ArrayList<Song> allSongs; //itemsAll
    private ArrayList<Song> suggestions;
    private int viewResourceId;

    SongAdapter(Context context, int viewResourceId, ArrayList<Song> songs) {
        super(context, viewResourceId, songs);
        this.songs = songs;
        this.allSongs = (ArrayList<Song>) songs.clone();
        this.suggestions = new ArrayList<>();
        this.viewResourceId = viewResourceId;
    }

    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(viewResourceId, null);
        }

        // Get the current song
        Song song = songs.get(position);


        if (song != null) {
            // Get views
            //ProgressBar albumArtworkLoading = (ProgressBar) view.findViewById(R.id.albumArtworkLoading);
            TextView songTitle = (TextView) view.findViewById(R.id.songTitle);
            TextView songArtist = (TextView) view.findViewById(R.id.songArtist);
            //ImageView songAlbumArtwork = (ImageView) view.findViewById(R.id.songAlbumArtwork);

            // This is where we set the data
            songTitle.setText(song.getTitle());
            songArtist.setText(song.getArtist());
            //new LoadImage(songAlbumArtwork).execute(song.getAlbumArtworkSmall());
        }

        return view;
    }

    public void addSong(Song song){
        songs.add(song);
        allSongs = (ArrayList<Song>) songs.clone();
        notifyDataSetChanged();
    }

    public void clear(){
        songs.clear();
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    // TODO make a better alg for filtering songs
    private Filter nameFilter = new Filter() {
        // the text that populates the text view after clicking
        @Override
        public String convertResultToString(Object resultValue) {
            return null;
        }

        // Actual filter function
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if(constraint != null) {
                suggestions.clear();
                for (Song song : allSongs) {
                    if(song.getTitle().toLowerCase().contains(constraint.toString().toLowerCase())){
                        suggestions.add(song);
                    } else if (song.getArtist().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        suggestions.add(song);
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<Song> filteredList = (ArrayList<Song>) results.values;
            if(results.count > 0) {
                clear();
                for (Song song : filteredList) {
                    add(song);
                }
                notifyDataSetChanged();
            }
        }
    };

}
