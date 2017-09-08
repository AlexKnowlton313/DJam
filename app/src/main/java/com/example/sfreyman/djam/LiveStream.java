package com.example.sfreyman.djam;

/**
 * Created by sfreyman on 4/18/17.
 */

public class LiveStream {
    private String user;
    private String streamTitle;
    private String albumArtworkLarge;
    private String currSong;
    private String currArtist;
    private String currSongURI;
    private String profilePic;
    private String timeStarted;
    private int currDuration;

    public LiveStream(String user, String profilePic, String streamTitle, String currSong,
                      String currArtist, String currSongURI, String albumArtworkLarge, String timeStarted, int currDuration) {

        this.user = user;
        this.profilePic = profilePic;
        this.streamTitle = streamTitle;
        this.currSong = currSong;
        this.currArtist = currArtist;
        this.currSongURI = currSongURI;
        this.albumArtworkLarge = albumArtworkLarge;
        this.profilePic = profilePic;
        this.timeStarted = timeStarted;
        this.currDuration = currDuration;

    }

    public LiveStream() {}
    public String getUser() {
        return user;
    }
    public String getTitle() {
        return streamTitle;
    }
    public String getSong() {
        return currSong;
    }
    public String getArtist() {
        return currArtist;
    }
    public String getAlbumArtworkLarge() {
        return albumArtworkLarge;
    }
    public String getProfilePic() {
        return profilePic;
    }
    public int getCurrDuration() { return currDuration; }


    public void setUser(String user) {
        this.user = user;
    }

    public void setTitle(String streamTitle) {
        this.streamTitle = streamTitle;
    }

    public void setAlbumArtworkLarge(String albumArtworkLarge) {
        this.albumArtworkLarge = albumArtworkLarge;
    }

    public void setSong(String currSong) {
        this.currSong = currSong;
    }

    public void setArtist(String currArtist) {
        this.currArtist = currArtist;
    }

    public String getStreamTitle() {
        return streamTitle;
    }

    public void setStreamTitle(String streamTitle) {
        this.streamTitle = streamTitle;
    }

    public String getCurrSong() {
        return currSong;
    }

    public void setCurrSong(String currSong) {
        this.currSong = currSong;
    }

    public String getCurrArtist() {
        return currArtist;
    }

    public void setCurrArtist(String currArtist) {
        this.currArtist = currArtist;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getTimeStarted() {
        return timeStarted;
    }

    public void setTimeStarted(String timeStarted) {
        this.timeStarted = timeStarted;
    }

    public String getCurrSongURI() {
        return currSongURI;
    }

    public void setCurrSongURI(String currSongURI) {
        this.currSongURI = currSongURI;
    }

    public void setCurrDuration(int currDuration) { this.currDuration = currDuration; }
}

