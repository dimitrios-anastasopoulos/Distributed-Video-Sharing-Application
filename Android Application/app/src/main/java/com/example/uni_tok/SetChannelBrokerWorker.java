package com.example.uni_tok;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class SetChannelBrokerWorker extends Worker {

    public SetChannelBrokerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String channelName;
        boolean unique;
        Data outputData;

        try {
            Log.d("ENTERED", "DO WORK");
            TimeUnit.SECONDS.sleep(1);
            channelName = getInputData().getString("ChannelName");
            unique = AppNodeImpl.setChannelBroker(channelName);
            Log.d("IS UNIQUE?", Boolean.toString(unique));

            if (!unique) return Result.failure();
            outputData = new Data.Builder().putBoolean("UNIQUE", unique).build();
            return Result.success(outputData);

        } catch (InterruptedException ie) {
            Log.d("IE", ie.getMessage());
            outputData = new Data.Builder().putString("ERROR", "EXCEPTION").build();
            return Result.failure(outputData);
        } catch (IOException | ClassNotFoundException io) {
            Log.d("IO", io.getMessage());
            outputData = new Data.Builder().putString("ERROR", "EXCEPTION").build();
            return Result.failure(outputData);
        }

    }

    @Override
    public void onStopped() {
        Log.d("STATE", "STOPPED");
        super.onStopped();
    }
}