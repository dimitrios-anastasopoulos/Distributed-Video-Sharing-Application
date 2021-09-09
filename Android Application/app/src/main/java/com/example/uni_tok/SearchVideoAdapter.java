package com.example.uni_tok;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import java.util.ArrayList;

public class SearchVideoAdapter extends BaseAdapter {

    private ArrayList<VideoInformation> videoList;
    private Context mContext;
    private static int failed_attempts;

    public SearchVideoAdapter(Context context, ArrayList<VideoInformation> videoList){
        this.videoList = videoList;
        this.mContext = context;
        failed_attempts = 0;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){

        if (convertView == null)
            convertView = LayoutInflater.from(mContext).inflate(R.layout.adapter_item_view, parent, false);

        TextView videoName = (TextView) convertView.findViewById(R.id.searchVideoTitle);
        TextView channelName = (TextView) convertView.findViewById(R.id.searchChannelName);
        TextView hashtags = (TextView) convertView.findViewById(R.id.searchHashtags);

        videoName.setText(videoList.get(position).getTitle());
        channelName.setText(videoList.get(position).getChannelName());

        StringBuilder stringBuilder = new StringBuilder();
        for (String hashtag : videoList.get(position).getHashtags())
            stringBuilder.append(hashtag + ", ");
        if (stringBuilder.length() != 0)
            stringBuilder.deleteCharAt(stringBuilder.length() - 2);

        hashtags.setText(stringBuilder);

        convertView.setOnClickListener(v -> {

            ChannelKey ck = getItem(position).getChannelKey();
            pullVideo(ck, position);

            //pullVideo returns before worker is executed because
            //it is a thread so intent should be executed inside pullVideo

        });

        return convertView;
    }

    public void pullVideo(ChannelKey channelKey, int position){
        String action = "Pull Video";

        Data data = new Data.Builder()
                .putString("ChannelName", channelKey.getChannelName())
                .putInt("videoID", channelKey.getVideoID())
                .putString("ACTION", action)
                .build();

        OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(UserWorker.class)
                .setInputData(data)
                .build();

        String uniqueWorkName = "Pull Video" + Integer.toString(failed_attempts);
        failed_attempts += 1;

        WorkManager.getInstance(mContext)
                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, uploadRequest);

        WorkManager.getInstance(mContext).getWorkInfoByIdLiveData(uploadRequest.getId())
                .observe((LifecycleOwner) mContext, workInfo -> {
                    Log.d("State", workInfo.getState().name());
                    if ( workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(mContext, "Successful pull video",
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(mContext, PlayVideo.class);
                        Log.d("CONTEXT", mContext.getClass().getSimpleName());
                        String filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() +
                                "/Fetched Videos/" + videoList.get(position).getChannelName() + "_" +
                                videoList.get(position).getVideoID() + ".mp4";
                        Bundle bundle = new Bundle();
                        bundle.putString("filepath", filepath);
                        bundle.putString("context", mContext.getClass().getSimpleName());
                        intent.putExtras(bundle);

                        mContext.startActivity(intent);

                    } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                        Toast.makeText(mContext,
                                "Failed pull video", Toast.LENGTH_SHORT).show();
                        Log.d("Status", "Status failed");
                    }
                });
    }

    @Override
    public int getCount(){
        return videoList.size();
    }

    @Override
    public VideoInformation getItem(int position) {
        return videoList.get(position); //returns list item at the specified position
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
