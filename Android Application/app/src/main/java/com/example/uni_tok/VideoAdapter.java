package com.example.uni_tok;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;


public class VideoAdapter extends BaseAdapter {
    private ArrayList<VideoFile> videoList;
    private Context mContext;

    public VideoAdapter(Context context, ArrayList<VideoFile> videoList){
        this.videoList = videoList;
        this.mContext = context;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){

        if (convertView == null)
            convertView = LayoutInflater.from(mContext).inflate(R.layout.single_item_channel, parent, false);


        TextView videoName = (TextView) convertView.findViewById(R.id.videoTitle);
        TextView hashtags = (TextView) convertView.findViewById(R.id.hashtags);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.Thumbnail);

        Uri videoUri = Uri.parse(videoList.get(position).getFilepath());
        MediaMetadataRetriever mMMR = new MediaMetadataRetriever();
        mMMR.setDataSource(mContext, videoUri);
        Bitmap thumbnail = mMMR.getFrameAtTime(20000000);

        videoName.setText(videoList.get(position).getVideoName());

        StringBuilder stringBuilder = new StringBuilder();
        for (String hashtag : videoList.get(position).getAssociatedHashtags())
            stringBuilder.append(hashtag + ", ");
        if (stringBuilder.length() != 0)
            stringBuilder.deleteCharAt(stringBuilder.length() - 2);

        hashtags.setText(stringBuilder);

        imageView.setImageBitmap(thumbnail);

        convertView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoList.get(position).getFilepath()), "video/mp4");
            mContext.startActivity(intent);
        });

        convertView.findViewById(R.id.AddHashtag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(videoList.get(position).getVideoID());
                ChannelActivity.AddHashtag(v, videoList.get(position).getVideoID(), mContext);
            }
        });

        convertView.findViewById(R.id.RemoveHashtag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(videoList.get(position).getVideoID());
                ChannelActivity.RemoveHashtag(v, videoList.get(position).getVideoID(), mContext);
            }
        });

        convertView.findViewById(R.id.DeleteVideo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(videoList.get(position).getVideoID());
                ChannelActivity.DeleteVideo(v, videoList.get(position).getVideoID(), mContext);
            }
        });

        return convertView;
    }

    @Override
    public int getCount(){
        return videoList.size();
    }

    @Override
    public VideoFile getItem(int position) {
        return videoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
