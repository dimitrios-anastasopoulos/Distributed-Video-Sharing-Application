package com.example.uni_tok;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    EditText IP;
    Button submitButton;
    int failed_attempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IP = (EditText) findViewById(R.id.AddressKeeperIP);

        submitButton = (Button) findViewById(R.id.AddressKeeperSubmit);
        submitButton.setOnClickListener((View v) -> newChannel());
        failed_attempts = 0;

    }

    public void newChannel() {

        Data data = new Data.Builder().putString("AddressKeeperIP", IP.getText().toString())
                            .build();

        OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(FirstConnectionWorker.class)
                                                                  .setInputData(data)
                                                                  .build();

        String uniqueWorkName = "Connect to address Keeper_" + Integer.toString(failed_attempts);
        failed_attempts += 1;

        Toast.makeText(getApplicationContext(), "Starting worker...", Toast.LENGTH_SHORT)
             .show();

        WorkManager.getInstance(this)
                   .enqueueUniqueWork(uniqueWorkName,ExistingWorkPolicy.REPLACE, oneTimeRequest);

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(oneTimeRequest.getId())
                   .observe(this, workInfo -> {
                       Log.d("State", workInfo.getState().name());
                       if ( workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                           Intent intent = new Intent(getApplicationContext(), newChannel.class);
                           startActivity(intent);
                           finish();
                       } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                           Toast.makeText(getApplicationContext(),
                                   "Couldn't connect to Address Keeper. " +
                                   "Try again.", Toast.LENGTH_SHORT).show();
                           Log.d("Status", "Status failed");

                       }

                   });

    }

}