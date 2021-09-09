package com.example.uni_tok;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class VideoFile {

    /** Class Variables */
    private String filepath;
    private ArrayList<String> associatedHashtags;
    private int videoID;
    private String videoName;
    private Date date;


    /** Constructor */
    public VideoFile (String filepath, ArrayList<String> associatedHashtags, String videoName) {
        this.filepath = filepath;
        this.associatedHashtags = associatedHashtags;
        this.videoName = videoName;
        date = new Date();
    }

    public byte[] getVideoFileChunk() {

        String filepath = this.getFilepath();
        byte[] buffer = null;
        try {
            File file = new File(filepath);
            FileInputStream fis = new FileInputStream(file);
            buffer = new byte[(int)file.length()];
            fis.read(buffer);
            fis.close();
            return buffer;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    /** Getters */
    public String getFilepath() {
        return this.filepath;
    }

    public ArrayList<String> getAssociatedHashtags() {
        return associatedHashtags;
    }

    public int getVideoID() {
        return videoID;
    }

    public String getVideoName() {
        return this.videoName;
    }

    public Date getDate() {return this.date;}


    /** Setters */
    public void addHashtag(String hashtag) {
        associatedHashtags.add(hashtag);
    }

    public void removeHashtag(String hashtag) {
        associatedHashtags.remove(hashtag);
    }

    public void setVideoID(int videoID) {
        this.videoID = videoID;
    }

}