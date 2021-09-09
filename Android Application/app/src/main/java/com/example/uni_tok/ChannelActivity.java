package com.example.uni_tok;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;


public class ChannelActivity extends AppCompatActivity {

    EditText search_bar;
    VideoAdapter arrayAdapter;
    TextView channelName;
    SharedPreferences sharedPreferences;
    ListView lv;

    int failed_attempts = 0;
    static int failed_attempts_ = 0;

    private static final int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("CREATION", "I am in channel activity!\n");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        search_bar = (EditText)findViewById(R.id.search_bar);

        sharedPreferences = getApplicationContext()
                .getSharedPreferences("appdata", MODE_PRIVATE);

        //LOAD CHANNEL NAME
        channelName = (TextView)findViewById(R.id.channelNameTextview);
        String name = AppNodeImpl.getChannel().getChannelName();
        channelName.setText(name);

        lv = (ListView) findViewById(R.id.listView);
        arrayAdapter = new VideoAdapter(this, (AppNodeImpl.getChannel()).getVideos());
        lv.setAdapter(arrayAdapter);
    }

    public void channelActivity(View v) {}

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

        String uniqueWorkName = "Topic_from_Channel"+ Integer.toString(failed_attempts);
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
        if (ActivityCompat.checkSelfPermission(ChannelActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(ChannelActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        } else {
            Intent intent = new Intent(this, UploadVideoActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, UploadVideoActivity.class);
            startActivity(intent);
        }
    }

    public void homeActivity(View v) {

        Intent intent = new Intent(this, runUser.class);
        startActivity(intent);

    }

    public static void AddHashtag(View v, int videoID, Context context){
        Intent intent = new Intent(context, addHashtag.class);

        Bundle bundle = new Bundle();
        bundle.putInt("videoID", videoID);
        intent.putExtras(bundle);

        context.startActivity(intent);
    }

    public static void RemoveHashtag(View v, int videoID, Context context){
        Intent intent = new Intent(context, removeHashtag.class);

        Bundle bundle = new Bundle();
        bundle.putInt("videoID", videoID);
        intent.putExtras(bundle);

        context.startActivity(intent);
    }

    public static void DeleteVideo(View v, int videoID, Context context){

        String action = "Delete Video";

        Data data = new Data.Builder()
                .putInt("videoID", videoID)
                .putString("ACTION", action)
                .build();

        OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(UserWorker.class)
                .setInputData(data)
                .build();

        String uniqueWorkName = "Delete Video" + Integer.toString(failed_attempts_);
        failed_attempts_ += 1;

        WorkManager.getInstance(context)
                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, uploadRequest);

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(uploadRequest.getId())
                .observe((LifecycleOwner) context, workInfo -> {
                    Log.d("State", workInfo.getState().name());
                    if ( workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Intent intent = new Intent(context, ChannelActivity.class);
                        context.startActivity(intent);
                        Toast.makeText(context, "Successful delete video",
                                Toast.LENGTH_SHORT).show();

                    } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                        Toast.makeText(context,
                                "Failed delete video", Toast.LENGTH_SHORT).show();
                        Log.d("Status", "Status failed");
                    }
                });
    }

    public void exit(View v) {}

}
