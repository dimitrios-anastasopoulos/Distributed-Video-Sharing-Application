package com.example.uni_tok;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class UserWorker extends Worker {

    public UserWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String topic;
        String action;
        String videoName;
        String path;
        String channelName;
        String [] hashtags;
        String [] hashtagsAdded;
        String [] hashtagsRemoved;
        int videoID;
        ArrayList<String> associatedHashtags = new ArrayList<>();
        ArrayList<String> addedAssociatedHashtags = new ArrayList<>();
        ArrayList<String> removedAssociatedHashtags = new ArrayList<>();
        SocketAddress socketAddress;

        try {
            TimeUnit.SECONDS.sleep(1);
            action = getInputData().getString("ACTION");
            if (action == null) return Result.failure();

            switch (action) {

                case "SUBSCRIBE":
                    boolean successful_subscription;
                    topic = getInputData().getString("TOPIC");
                    if (topic != null) {
                        socketAddress = AppNodeImpl.hashTopic(topic);
                        successful_subscription = AppNodeImpl.register(socketAddress, topic);
                        if (successful_subscription) {
                            AppNodeImpl.refreshHomePage(AppNodeImpl.getSearchTopicVideoList());
                            return Result.success();
                        }
                    }
                    break;


                case "SUBSCRIBED":
                    boolean successful_unsubscription;
                    topic = getInputData().getString("TOPIC");
                    if (topic!=null) {
                        socketAddress = AppNodeImpl.hashTopic(topic);
                        successful_unsubscription = AppNodeImpl.unregister(socketAddress, topic);
                        if (successful_unsubscription) {
                            AppNodeImpl.refreshHomePage(topic);
                            return Result.success();
                        }
                    }
                    break;

                case "TOPIC VIDEO LIST" :
                    boolean fetched_successfully;
                    topic = getInputData().getString("TOPIC");
                    if (topic != null) {
                        fetched_successfully = AppNodeImpl.setSearchTopicVideoList(topic);
                        if (fetched_successfully) return Result.success();
                    }
                    break;

                case "Upload":
                    path = getInputData().getString("path");
                    videoName = getInputData().getString("videoName");
                    hashtags = getInputData().getStringArray("associatedHashtags");
                    if (hashtags != null) {
                        Collections.addAll(associatedHashtags, hashtags);
                        boolean successful_upload = AppNodeImpl.Upload(path, associatedHashtags, videoName);
                        if (successful_upload) return Result.success();
                    }
                    break;

                case "Add Hashtags":
                    videoID = getInputData().getInt("videoID", -1);
                    hashtagsAdded = getInputData().getStringArray("addedHashtags");
                    if (hashtagsAdded != null) {
                        Collections.addAll(addedAssociatedHashtags, hashtagsAdded);
                        if (videoID != -1){
                            boolean successful_addedHashtags = AppNodeImpl.
                                addHashTag(AppNodeImpl.getChannel().getVideoFile_byID(videoID), addedAssociatedHashtags);
                            if (successful_addedHashtags) return Result.success();
                        } else {
                            Log.d("Error", "Something Bad happened!");
                        }
                    }
                    break;

                case "Remove Hashtags":
                    videoID = getInputData().getInt("videoID", -1);
                    hashtagsRemoved = getInputData().getStringArray("removedHashtags");
                    if (hashtagsRemoved != null) {
                        Collections.addAll(removedAssociatedHashtags, hashtagsRemoved);
                        if (videoID != -1){
                            boolean successful_addedHashtags = AppNodeImpl.
                                    removeHashTag(AppNodeImpl.getChannel().getVideoFile_byID(videoID), removedAssociatedHashtags);
                            if (successful_addedHashtags) return Result.success();
                        } else {
                            Log.d("Error", "Something Bad happened!");
                        }
                    }
                    break;

                case "Delete Video":
                    videoID = getInputData().getInt("videoID", -1);
                    if (videoID != -1){
                        VideoFile video = AppNodeImpl.getChannel().getVideoFile_byID(videoID);
                        HashMap<String, String> notificationHashtags = AppNodeImpl.getChannel().removeVideoFile(video);
                        String filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()
                                + "/Uploaded Videos/" + video.getVideoName() + "_" + videoID + ".mp4";
                        try {
                            File file = new File(filepath);
                            if (file.exists()) {
                                file.getCanonicalFile().delete();
                                if (file.exists())
                                    getApplicationContext().deleteFile(file.getName());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (!notificationHashtags.isEmpty()) {
                            for (Map.Entry<String, String> item : notificationHashtags.entrySet())
                                AppNodeImpl.notifyBrokersForHashTags(item.getKey(), item.getValue());
                        }
                        return Result.success();
                    } else {
                        Log.d("Error", "Something Bad happened!");
                    }
                    break;

                case "Pull Video":
                    channelName = getInputData().getString("ChannelName");
                    videoID = getInputData().getInt("videoID", -1);
                    boolean successful_pull = AppNodeImpl.playData(new ChannelKey(channelName, videoID));
                    if (successful_pull) return Result.success();
                    break;
            }

        }
        catch (InterruptedException ie) {
                Log.d("IE", ie.getMessage());
        }

        return Result.failure();

    }


    @Override
    public void onStopped() {
        Log.d("STATE", "STOPPED");
        super.onStopped();
    }
}
