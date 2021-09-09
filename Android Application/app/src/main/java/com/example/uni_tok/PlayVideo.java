package com.example.uni_tok;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class PlayVideo extends AppCompatActivity {

    private EditText search_bar;
    private VideoView myVideo;
    private String filepath;
    private MediaController media;
    private String parent;
    private static int failed_attempts;
    SharedPreferences sharedPreferences;

    private static final int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_data);

        media = new MediaController(this);

        sharedPreferences = getApplicationContext()
                .getSharedPreferences("appdata", MODE_PRIVATE);

        failed_attempts = 0;

        search_bar = (EditText) findViewById(R.id.search_bar);

        Bundle getUri = this.getIntent().getExtras();
        filepath = getUri.getString("filepath");
        parent = getUri.getString("context");

        myVideo = (VideoView) findViewById(R.id.videoViewForPlayData);
        myVideo.setVideoPath(filepath);

        //controller(pause, navigate forward, navigate backward)
        myVideo.setMediaController(media);
        media.setAnchorView(myVideo);

        //start the video
        myVideo.start();
    }

    public void Back(View v){
        if (parent.equals("SearchActivity")){
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        } else if (parent.equals("runUser")){
            Intent intent = new Intent(this, runUser.class);
            startActivity(intent);
        }
    }

    public void channelActivity(View v) {
        Intent intent = new Intent(this, ChannelActivity.class);

        startActivity(intent);
    }

    public void homeActivity(View v) {

        Intent intent = new Intent(this, runUser.class);
        startActivity(intent);

    }

    public void searchActivity(View v) {

        String topic = search_bar.getText().toString();
        String action = "TOPIC VIDEO LIST";
        Intent intent = new Intent(this, SearchActivity.class);

        Data data = new Data.Builder()
                .putString("TOPIC", search_bar.getText().toString())
                .putString("ACTION", action)
                .build();

        OneTimeWorkRequest topicRequest = new OneTimeWorkRequest.Builder(UserWorker.class)
                .setInputData(data)
                .build();

        String uniqueWorkName = "Topic"+ Integer.toString(failed_attempts);
        failed_attempts += 1;

        WorkManager.getInstance(this)
                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, topicRequest);

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(topicRequest.getId())
                .observe(this, workInfo -> {

                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Log.d("STATE", "SUCCEEDED");
                        startActivity(intent);
                    } else if(workInfo.getState() == WorkInfo.State.FAILED) {
                        Log.d("STATE", "FAILED");
                        Toast.makeText(getApplicationContext(), "Error in fetching results..",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        //STORE SEARCH TOPIC
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("searchKey", topic );
        editor.apply();

    }

    public void uploadVideoActivity(View v) {
        if (ActivityCompat.checkSelfPermission(PlayVideo.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(PlayVideo.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        } else {
            Intent intent = new Intent(this, UploadVideoActivity.class);
            startActivity(intent);
        }
    }
}
