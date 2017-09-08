package com.example.sfreyman.djam;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.httpclient.HttpStatus;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by sfreyman on 4/18/17.
 */

public class LiveStreamAdapter extends ArrayAdapter<LiveStream> {

    int resource;
    Cursor curse;

    public LiveStreamAdapter(Context ctx, int res, List<LiveStream> items)
    {
        super(ctx, res, items);
        resource = res;
    }

    @Override
    public View getView(final int position, View logsView, ViewGroup parent) {
        LinearLayout itemView;
        LiveStream stream = getItem(position);
        Context context = getContext();

        if (logsView == null) {
            itemView = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
            vi.inflate(resource, itemView, true);
        } else {
            itemView = (LinearLayout) logsView;
        }

        TextView streamTitleTV, streamerNameTV;
        ImageView streamerProfPicIV, songArtworkIV;
        Button playStopBtn;

        // UI Element References.
        streamTitleTV       = (TextView) itemView.findViewById(R.id.streamTitleTVMain);
        streamerNameTV      = (TextView) itemView.findViewById(R.id.streamerNameTVMain);

        streamerProfPicIV   = (ImageView) itemView.findViewById(R.id.streamerProfPicIVMain);
        songArtworkIV       = (ImageView) itemView.findViewById(R.id.songArtworkIVMain);
        // End UI Element References.

        LiveStream item = getItem(position);

        streamTitleTV.setText(item.getTitle());
        streamerNameTV.setText(item.getUser());
        new LoadImage(songArtworkIV).execute(item.getAlbumArtworkLarge());
        new LoadImage(streamerProfPicIV).execute(item.getProfilePic());

        return itemView;
    }
}

