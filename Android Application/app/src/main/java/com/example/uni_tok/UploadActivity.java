package com.example.uni_tok;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class UploadActivity extends AppCompatActivity {

    EditText search_bar;
    EditText videoNameEditText;
    EditText hashtagsEditText;
    Uri video;
    SharedPreferences sharedPreferences;
    int failed_attempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        search_bar = (EditText) findViewById(R.id.search_bar);

        sharedPreferences = getApplicationContext()
                .getSharedPreferences("appdata", MODE_PRIVATE);

        Bundle getUri = this.getIntent().getExtras();
        video = getUri.getParcelable("video");

        //Get video name
        videoNameEditText = (EditText) findViewById(R.id.videoNameEditText);

        //Get hashtags
        hashtagsEditText = (EditText) findViewById(R.id.hashtagsEditText);

        failed_attempts = 0;
    }

    public void channelActivity(View v) {

        String target =  videoNameEditText.getText().toString() + "_" + Channel.getVideoID() + ".mp4";
        String path = getRealPathFromURI(this, video, target);
        Log.d("PATH", path);

        if(path != null) {
            Button button = (Button) v;
            String action = button.getText().toString();

            String [] hashtags = hashtagsEditText.getText().toString().split(" ");

            Data data = new Data.Builder()
                    .putString("path",path)
                    .putString("videoName", videoNameEditText.getText().toString())
                    .putStringArray("associatedHashtags", hashtags)
                    .putString("ACTION", action)
                    .build();

            OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(UserWorker.class)
                    .setInputData(data)
                    .build();

            String uniqueWorkName = "Upload" + Integer.toString(failed_attempts);
            failed_attempts += 1;

            WorkManager.getInstance(this)
                    .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, uploadRequest);

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(uploadRequest.getId())
                    .observe(this, workInfo -> {
                        Log.d("State", workInfo.getState().name());
                        if ( workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            Intent intent = new Intent(this, ChannelActivity.class);
                            startActivity(intent);
                            Toast.makeText(getApplicationContext(), "Successful upload",
                                    Toast.LENGTH_SHORT).show();

                        } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                            Toast.makeText(getApplicationContext(),
                                    "Failed upload", Toast.LENGTH_SHORT).show();
                            Log.d("Status", "Status failed");
                        }
                    });
        } else {
            Log.d("GET PATH", "FAILED");
        }

    }

    public void searchActivity(View v) {
        String topic = search_bar.getText().toString();
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
    }

    public void uploadVideoActivity(View v) {
        Intent intent = new Intent(this, UploadVideoActivity.class);
        startActivity(intent);
    }

    public void homeActivity(View v) {
        Intent intent = new Intent(this, runUser.class);
        startActivity(intent);
    }

    public void exit(View v) {}

    public void uploadActivity(View view) {
        Intent intent = new Intent(this, UploadActivity.class);
        startActivity(intent);
    }

    public static void copy(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            copyStream(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1)
                break;
            out.write(buffer, 0, bytesRead);
        }
    }

    private String getRealPathFromURI(Context context, Uri contentUri, String fileName) {
        Log.d("CONTENT URI", contentUri.toString());
        Log.d("FILENAME", fileName);
        //copy file and send new file path
        if (!TextUtils.isEmpty(fileName)) {
            Log.d("ENVIRONMENT PATH", Environment.getExternalStorageDirectory().getAbsolutePath());
            File copyFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()+ "/Uploaded Videos/", fileName);
            copy(context, contentUri, copyFile);
            return copyFile.getAbsolutePath();
        }
        return null;

        }

}