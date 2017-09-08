package com.example.sfreyman.djam;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class for song objects.
 * Parcelable allows putExtra between activities
 */

class Song implements Parcelable {
    private String id;
    private String title;
    private String artist;
    private String albumArtworkLarge;
    private String albumArtworkSmall;
    private int    duration;

    Song() {
        this.title = "default";
        this.artist = "default";
    }

    Song(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    Song(String songId, String title, String artist, String albumArtworkLarge, String albumArtworkSmall, int duration) {
        this.id = songId;
        this.title = title;
        this.artist = artist;
        this.albumArtworkLarge = albumArtworkLarge;
        this.albumArtworkSmall = albumArtworkSmall;
        this.duration = duration;
    }

    // Function used to regenerate object
    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Song(Parcel in) {
        this.id = in.readString();
        this.title = in.readString();
        this.artist = in.readString();
        this.albumArtworkLarge = in.readString();
        this.albumArtworkSmall = in.readString();
        this.duration = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Write Song data to passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(id);
        out.writeString(title);
        out.writeString(artist);
        out.writeString(albumArtworkLarge);
        out.writeString(albumArtworkSmall);
        out.writeInt(duration);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumArtworkLarge() {
        return albumArtworkLarge;
    }

    public String getAlbumArtworkSmall() {
        return  albumArtworkSmall;
    }

    public int getDuration() {
        return duration;
    }

    public void setId(String songId) {
        this.id = songId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
