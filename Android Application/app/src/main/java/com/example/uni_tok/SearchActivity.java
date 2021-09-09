package com.example.uni_tok;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;


public class SearchActivity extends AppCompatActivity {

    EditText search_bar;
    Button subscribeButton;
    SharedPreferences sharedPreferences;
    TextView relatedTopic;
    int failed_attempts;
    static Context context;

    private static final int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("CREATION", "I am in search activity!\n");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        context = getApplicationContext();

        sharedPreferences = getApplicationContext()
                           .getSharedPreferences("appdata", MODE_PRIVATE);
        search_bar = (EditText)findViewById(R.id.search_bar);
        subscribeButton = (Button)findViewById(R.id.subscribeButton);

        relatedTopic = (TextView)findViewById(R.id.topicTextview);
        String topic = sharedPreferences.getString("searchKey", "None");
        relatedTopic.setText(topic);

        boolean is_subscribed;
        if (topic.charAt(0) == '#') {
            is_subscribed = AppNodeImpl.getSubscribedToHashtags().contains(topic);
        } else {
            is_subscribed = AppNodeImpl.getSubscribedToChannels().contains(topic);
        }

        if(is_subscribed) {
            subscribeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));
            subscribeButton.setText(R.string.subscribedText);
        }

        failed_attempts = 0;

        ListView listView = (ListView) findViewById(R.id.listViewSearchActivity);
        SearchVideoAdapter adapter = new SearchVideoAdapter(this, AppNodeImpl.getSearchTopicVideoList());
        listView.setAdapter(adapter);
    }

    public void subscribeAction(View v) {
        Button button = (Button) v;
        String action = button.getText().toString();

        Data data = new Data.Builder()
                .putString("TOPIC", relatedTopic.getText().toString())
                .putString("ACTION", action)
                .build();

        OneTimeWorkRequest subscriptionRequest = new OneTimeWorkRequest.Builder(UserWorker.class)
                .setInputData(data)
                .build();

        String uniqueWorkName = "Subscription" + Integer.toString(failed_attempts);
        failed_attempts += 1;

        WorkManager.getInstance(this)
                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, subscriptionRequest);

        if (button.getText().equals("SUBSCRIBE")) {

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(subscriptionRequest.getId())
                .observe(this, workInfo -> {
                    Log.d("State", workInfo.getState().name());
                    if ( workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        button.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));
                        button.setText(R.string.subscribedText);
                        Toast.makeText(getApplicationContext(), "Successful subscription",
                                       Toast.LENGTH_SHORT).show();

                    } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                        Toast.makeText(getApplicationContext(),
                                  "Failed subscription", Toast.LENGTH_SHORT).show();
                        Log.d("Status", "Status failed");
                    }
                });

        }
        else {

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(subscriptionRequest.getId())
                .observe(this, workInfo -> {
                    Log.d("State", workInfo.getState().name());
                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        button.setBackgroundColor(ContextCompat.getColor(this, R.color.app_color));
                        button.setText(R.string.subscribeText);
                        Toast.makeText(getApplicationContext(), "Successfully unsubscribed",
                                       Toast.LENGTH_SHORT).show();
                    } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                        Toast.makeText(getApplicationContext(), "Failed to unsubscribe",
                                Toast.LENGTH_SHORT).show();
                        Log.d("Status", "Status failed");
                    }
                });

        }

    }

    public void channelActivity(View v) {
        Intent intent = new Intent(this, ChannelActivity.class);

        //To check the activity in the channel
        intent.putExtra("upload", false);

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

        String uniqueWorkName = "Topic_from_channel"+ Integer.toString(failed_attempts);
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
        if (ActivityCompat.checkSelfPermission(SearchActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(SearchActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
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

    public void exit(View v) {}

    public Context getContext() {
        return this;
    }

}
