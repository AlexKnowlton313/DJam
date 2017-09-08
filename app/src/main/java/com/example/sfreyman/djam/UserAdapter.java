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

class UserAdapter extends ArrayAdapter<LiveStream> {
    private ArrayList<LiveStream> users; //items
    private ArrayList<LiveStream> allUsers; //itemsAll
    private ArrayList<LiveStream> suggestions;
    private int viewResourceId;

    UserAdapter(Context context, int viewResourceId, ArrayList<LiveStream> streams) {
        super(context, viewResourceId, streams);
        this.users = streams;
        this.allUsers = (ArrayList<LiveStream>) streams.clone();
        this.suggestions = new ArrayList<>();
        this.viewResourceId = viewResourceId;
    }

    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(viewResourceId, null);
        }

        // Get the current user
        LiveStream user = users.get(position);


        if (user != null) {
            // Get views
            //ProgressBar albumArtworkLoading = (ProgressBar) view.findViewById(R.id.albumArtworkLoading);
            TextView username = (TextView) view.findViewById(R.id.username);
            //TextView songArtist = (TextView) view.findViewById(R.id.songArtist);
            //ImageView songAlbumArtwork = (ImageView) view.findViewById(R.id.songAlbumArtwork);

            // This is where we set the data
            username.setText(user.getUser());
            //songArtist.setText(song.getArtist());
            //new LoadImage(songAlbumArtwork).execute(song.getAlbumArtworkSmall());
        }

        return view;
    }

    public void addLiveStream(LiveStream user){
        users.add(user);
        allUsers = (ArrayList<LiveStream>) users.clone();
        notifyDataSetChanged();
    }

    public void clear(){
        users.clear();
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
                for (LiveStream user : allUsers) {
                    if(user.getUser().toLowerCase().contains(constraint.toString().toLowerCase())){
                        suggestions.add(user);
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
            ArrayList<LiveStream> filteredList = (ArrayList<LiveStream>) results.values;
            if(results.count > 0) {
                clear();
                for (LiveStream user : filteredList) {
                    add(user);
                }
                notifyDataSetChanged();
            }
        }
    };

}
