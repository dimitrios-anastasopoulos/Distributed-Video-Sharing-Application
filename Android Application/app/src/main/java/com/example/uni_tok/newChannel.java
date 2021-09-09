package com.example.uni_tok;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.concurrent.TimeUnit;

public class newChannel extends AppCompatActivity {

    EditText channelName;
    Button submitButton;
    SharedPreferences sharedPreferences;

    OneTimeWorkRequest oneTimeRequest;
    WorkManager workManager;
    Data data;
    int failed_attempts;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_channel);

        sharedPreferences = getApplicationContext()
                           .getSharedPreferences("appdata", MODE_PRIVATE);

        channelName = (EditText) findViewById(R.id.ChannelName);

        submitButton = (Button) findViewById(R.id.channelButton);
        submitButton.setOnClickListener(v -> runUser());

        workManager = WorkManager.getInstance(this);

        failed_attempts = 0;
    }

    public void runUser() {

        data = new Data.Builder()
                            .putString("ChannelName", channelName.getText().toString())
                            .build();

        oneTimeRequest = new OneTimeWorkRequest.Builder(SetChannelBrokerWorker.class)
                .keepResultsForAtLeast(1, TimeUnit.SECONDS)
                .setInputData(data)
                .build();

        String uniqueWorkName = "Connect to Broker_" + Integer.toString(failed_attempts);
        Log.d("WORK", uniqueWorkName);
        failed_attempts += 1;

        Toast.makeText(getApplicationContext(), "Starting worker...", Toast.LENGTH_SHORT)
                .show();

        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.APPEND_OR_REPLACE, oneTimeRequest);

        workManager.getWorkInfoByIdLiveData(oneTimeRequest.getId())
                .observe(this, workInfo -> {

                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        Log.d("STATE", "SUCCEEDED");
                        Log.d("NAME", channelName.getText().toString());
                        boolean unique = workInfo.getOutputData().getBoolean("UNIQUE", true);
                        if (!unique) {
                            Toast.makeText(getApplicationContext(),
                                    "Channel name already exists.", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(this, runUser.class);
                            startActivity(intent);
                            finish();
                        }
                    } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                        Log.d("STATE", "FAILED");
                    }

                });

    }

}