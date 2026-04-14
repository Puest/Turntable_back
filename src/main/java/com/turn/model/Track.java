package com.turn.model;

public class Track {
    private Long id;
    private String title;
    private String artist;
    private String album;
    private int durationSeconds;
    private String filename;
    private String coverColor;
    private String coverImage;
    private String discImage;

    public Track() {}

    public Track(Long id, String title, String artist, String album, int durationSeconds, String filename, String coverColor, String coverImage, String discImage) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationSeconds = durationSeconds;
        this.filename = filename;
        this.coverColor = coverColor;
        this.coverImage = coverImage;
        this.discImage = discImage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getCoverColor() { return coverColor; }
    public void setCoverColor(String coverColor) { this.coverColor = coverColor; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getDiscImage() { return discImage; }
    public void setDiscImage(String discImage) { this.discImage = discImage; }
}
