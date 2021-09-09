package com.example.uni_tok;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class removeHashtag extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1;

    EditText search_bar;
    TextView hashtags;
    Button remove;
    int videoID;
    SharedPreferences sharedPreferences;
    int failed_attempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("CREATION", "I am in addHashtag activity!\n");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remove_hashtag);

        sharedPreferences = getApplicationContext()
                .getSharedPreferences("appdata", MODE_PRIVATE);

        search_bar = (EditText)findViewById(R.id.search_bar);

        Bundle get = this.getIntent().getExtras();
        videoID = get.getInt("videoID");

        hashtags = (TextView) findViewById(R.id.hashtagsRemove);
        remove = (Button) findViewById(R.id.RemoveButton);

        failed_attempts = 0;
    }

    public void channelActivity(View v) {
        Button button = (Button) v;
        String action = button.getText().toString();

        String [] removedHashtags = hashtags.getText().toString().split(" ");

        Data data = new Data.Builder()
                .putStringArray("removedHashtags", removedHashtags)
                .putInt("videoID", videoID)
                .putString("ACTION", action)
                .build();

        OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(UserWorker.class)
                .setInputData(data)
                .build();

        String uniqueWorkName = "Remove Hashtags" + Integer.toString(failed_attempts);
        failed_attempts += 1;

        WorkManager.getInstance(this)
                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, uploadRequest);

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(uploadRequest.getId())
                .observe(this, workInfo -> {
                    Log.d("State", workInfo.getState().name());
                    if ( workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Intent intent = new Intent(this, ChannelActivity.class);
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "Successful remove hashtags",
                                Toast.LENGTH_SHORT).show();

                    } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                        Toast.makeText(getApplicationContext(),
                                "Failed remove hashtags", Toast.LENGTH_SHORT).show();
                        Log.d("Status", "Status failed");
                    }
                });
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
        if (ActivityCompat.checkSelfPermission(removeHashtag.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(removeHashtag.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
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
