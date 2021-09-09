package com.example.uni_tok;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Channel {

    private String channelName;
    private ArrayList<String> hashtagsPublished;
    private HashMap<String, ArrayList<VideoFile>> hashtagVideoFilesMap;
    private static int counterVideoID;
    private static HashMap<Integer, VideoFile> ID_VideoFileMap;
    private HashMap<ChannelKey, String> ID_VideoNameMap;
    private HashMap<ChannelKey, ArrayList<String>> ID_AssociatedHashtagsMap;

    /** Constructors */

    //For new users
    public Channel (String channelName) {
        this.channelName = channelName;
        hashtagsPublished = new ArrayList<>();
        hashtagVideoFilesMap = new HashMap<>();
        counterVideoID = 0;
        ID_VideoFileMap = new HashMap<>();
        ID_VideoNameMap = new HashMap<>();
        ID_AssociatedHashtagsMap = new HashMap<>();

    }

    //To create existing channels
    public Channel (String channelName, ArrayList<String> hashtagsPublished, HashMap<String, ArrayList<VideoFile>>
            hashtagVideoFilesMap, int counterVideoID, HashMap<Integer, VideoFile> ID_VideoFileMap,
                    HashMap<ChannelKey, String> ID_VideoNameMap) {
        this.channelName = channelName;
        this.hashtagsPublished = hashtagsPublished;
        this.hashtagVideoFilesMap = hashtagVideoFilesMap;
        this.counterVideoID = counterVideoID;
        this.ID_VideoFileMap = ID_VideoFileMap;
        this.ID_VideoNameMap = ID_VideoNameMap;

    }

    public HashMap<String, String> addVideoFile(VideoFile video, ChannelKey channelKey) {
        video.setVideoID(counterVideoID);
        ID_VideoFileMap.put(counterVideoID, video);
        ID_VideoNameMap.put(channelKey, video.getVideoName());
        ID_AssociatedHashtagsMap.put(channelKey, video.getAssociatedHashtags());
        counterVideoID++;

        HashMap<String, String> hashtagsNeedNotification = new HashMap<>();
        ArrayList<String> hashtags = video.getAssociatedHashtags();
        for (String hashtag : hashtags) {
            if (hashtagsPublished.contains(hashtag)) {
                ArrayList<VideoFile> value = hashtagVideoFilesMap.get(hashtag);
                value.add(video);
                hashtagVideoFilesMap.put(hashtag, value);
            } else {
                ArrayList<VideoFile> value = new ArrayList<>();
                value.add(video);
                hashtagVideoFilesMap.put(hashtag, value);

                hashtagsPublished.add(hashtag);

                hashtagsNeedNotification.put(hashtag, "ADD");
            }
        }
        return hashtagsNeedNotification;
    }

    public HashMap<String, String> removeVideoFile(VideoFile video) {
        ID_VideoFileMap.remove(video.getVideoID());
        ID_VideoNameMap.remove(new ChannelKey(this.channelName, video.getVideoID()));
        ID_AssociatedHashtagsMap.remove(new ChannelKey(this.channelName, video.getVideoID()));

        HashMap<String, String> hashtagsNeedNotification = new HashMap<>();
        ArrayList<String> hashtags = video.getAssociatedHashtags();
        for (String hashtag : hashtags) {
            if (hashtagVideoFilesMap.get(hashtag).size() == 1) {
                hashtagVideoFilesMap.remove(hashtag);

                hashtagsPublished.remove(hashtag);

                hashtagsNeedNotification.put(hashtag, "REMOVE");
            } else {
                ArrayList<VideoFile> value = hashtagVideoFilesMap.get(hashtag);
                value.remove(video);
                hashtagVideoFilesMap.put(hashtag, value);
            }
        }
        return hashtagsNeedNotification;
    }

    public HashMap<String, String> updateVideoFile(VideoFile video, ArrayList<String> hashtags, String method) {
        HashMap<String, String> hashtagsNeedNotification = new HashMap<>();
        if (method.equals("ADD")) {
            for (String hashtag : hashtags) {
                video.addHashtag(hashtag);
                if (hashtagsPublished.contains(hashtag)) {
                    // Add video to the hashtagVideoFilesMap.
                    ArrayList<VideoFile> associatedVideos = getVideoFiles_byHashtag(hashtag);
                    associatedVideos.add(video);
                    hashtagVideoFilesMap.put(hashtag, associatedVideos);
                } else {
                    // Add video to the hashtagVideoFilesMap.
                    ArrayList<VideoFile> associatedVideos = new ArrayList<>();
                    associatedVideos.add(video);
                    hashtagVideoFilesMap.put(hashtag, associatedVideos);

                    // Add hashtag to the channel's Published Hashtags.
                    hashtagsPublished.add(hashtag);
                    // Brokers notification needed about new hashtag in channel.
                    hashtagsNeedNotification.put(hashtag, "ADD");
                }
            }
        } else if (method.equals("REMOVE")) {
            for (String hashtag : hashtags) {
                video.removeHashtag(hashtag);
                if (getVideoFiles_byHashtag(hashtag).size() > 1) {
                    // Remove video from the hashtagVideoFilesMap.
                    ArrayList<VideoFile> associatedVideos = getVideoFiles_byHashtag(hashtag);
                    associatedVideos.remove(video);
                    hashtagVideoFilesMap.put(hashtag, associatedVideos);
                } else {
                    // Remove video from the hashtagVideoFilesMap.
                    hashtagVideoFilesMap.remove(hashtag);

                    // Remove hashtag from the channel's Published Hashtags.
                    hashtagsPublished.remove(hashtag);
                    // Brokers notification needed about new hashtag in channel.
                    hashtagsNeedNotification.put(hashtag, "REMOVE");
                }
            }
        } else {
            System.out.println("MAJOR ERROR");
        }
        return hashtagsNeedNotification;
    }

    /** Getters */

    public ArrayList<VideoFile> getVideoFiles_byHashtag(String hashtag) {
        return hashtagVideoFilesMap.get(hashtag);
    }

    public VideoFile getVideoFile_byID (int ID) {
        return ID_VideoFileMap.get(ID);
    }

    public HashMap<Integer, VideoFile> getID_VideoFileMap() {
        return ID_VideoFileMap;
    }


    public HashMap<ChannelKey, String> getChannelVideoNames() {
        return ID_VideoNameMap;
    }

    //DIMITRIS
    public HashMap<ChannelKey, ArrayList<String>> getChannelAssociatedHashtags() {
        return ID_AssociatedHashtagsMap;
    }

    public String getChannelName(){
        return this.channelName;
    }

    public HashMap<ChannelKey, String> getChannelVideoNamesByHashtag(String hashtag) {

        //We store a hashmap with all videos that have specific hashtag.
        //Key is the video id, Value is videoName (we might change it to metadata)
        HashMap<ChannelKey, String> hashtagVideosHashmap = new HashMap<>();

        //Get all files with specific hashtag
        ArrayList<VideoFile> hashtag_files = hashtagVideoFilesMap.get(hashtag);

        //Get hashmap needed
        for (VideoFile video : hashtag_files) {
            hashtagVideosHashmap.put(new ChannelKey(channelName, video.getVideoID()), video.getVideoName());
        }

        return hashtagVideosHashmap;

    }

    public int getCounterVideoID() {return counterVideoID;}

    public String toString() {

        String channelString;
        channelString = "Contents of channel " + channelName + "\r\n";
        for (Map.Entry<ChannelKey, String> key : ID_VideoNameMap.entrySet()) {
            int id = key.getKey().getVideoID();
            channelString += String.valueOf(id) + ": " + getVideoFile_byID(id).getVideoName() + "\r\n";
        }
        return channelString;
    }



    public static ArrayList<VideoFile> getVideos(){
        ArrayList<VideoFile> temp = new ArrayList<>();
        if (ID_VideoFileMap != null) {
            for (Map.Entry<Integer, VideoFile> video : ID_VideoFileMap.entrySet())
                temp.add(video.getValue());
        }
        return temp;
    }

    public static int getVideoID(){
        return counterVideoID;
    }

}