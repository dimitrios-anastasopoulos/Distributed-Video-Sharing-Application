package com.example.uni_tok;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class FirstConnectionWorker extends Worker {

    public FirstConnectionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            TimeUnit.SECONDS.sleep(1);
            String addressKeeperIP = getInputData().getString("AddressKeeperIP");
            SocketAddress sockAddress = new InetSocketAddress(InetAddress.getByName(addressKeeperIP),
                    4000);
            AppNodeImpl.connect(sockAddress);
            AppNodeImpl.getBrokers();
        } catch (IOException | ClassNotFoundException io) {
            Log.d("IO", io.getMessage());
            return Result.failure();
        } catch (NullPointerException npe) {
            Log.d("NPE", npe.getMessage());
            return Result.failure();
        } catch (InterruptedException ie) {
            Log.d("IE", ie.getMessage());
            return Result.failure();
        } finally{
            try {
                AppNodeImpl.disconnect();
            } catch (NullPointerException npe) {
                Log.d("Error", npe.getMessage());
            }
        }
        Log.d("No exception", "Everything went good!");
        return Result.success();

    }
}
