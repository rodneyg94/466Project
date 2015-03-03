package com.csc466.rodney.tunesshare;

/**
 * This class is used to model the data for a single audio file
 */
public class Song {
    private long id;
    private String title;
    private String artist;

    // Constructor method
    public Song(long songID, String songTitle, String songArtist) {
        id = songID;
        title = songTitle;
        artist = songArtist;
    }

    // Get methods
    public long getID() {return id;}
    public String getTitle() {return title;}
    public String getArtist() {return artist;}


}
