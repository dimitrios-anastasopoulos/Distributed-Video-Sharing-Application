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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class runUser extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1;

    EditText search_bar;
    public static Context context;

    SharedPreferences sharedPreferences;
    int failed_attempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("CREATION", "I am in onCreate of runUser!\n");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page_activity);

        context = getApplicationContext();

        sharedPreferences = getApplicationContext()
                .getSharedPreferences("appdata", MODE_PRIVATE);

        search_bar = (EditText)findViewById(R.id.search_bar);

        ListView listView = (ListView) findViewById(R.id.homePageVideoList);
        SearchVideoAdapter adapter = new SearchVideoAdapter(this, AppNodeImpl.getHomePageVideoList());
        listView.setAdapter(adapter);

        Thread thread = new Thread() {
            public void run() {
                AppNodeImpl.handleRequest();
            }
        };
        thread.start();
        failed_attempts = 0;
    }

    public void channelActivity(View v) {
        Intent intent = new Intent(this, ChannelActivity.class);

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
        if (ActivityCompat.checkSelfPermission(runUser.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(runUser.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
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

    public void homeActivity(View v) {}

    public void exit(View v) {}
}