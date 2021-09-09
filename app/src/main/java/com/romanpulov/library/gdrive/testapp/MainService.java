package com.romanpulov.library.gdrive.testapp;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.romanpulov.library.gdrive.GDActionException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainService extends IntentService {

    public MainService() {
        super("MainService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        //generate files
        String[] fileStrings = new String[] {"1-dfwkerkwerw", "2-6,vdfgdfg", "3-gkkkrk444", "4-,fkfkfkdrtrrewwerwer"};
        File[] files = new File[fileStrings.length];

        for (int i = 0; i < fileStrings.length; i++) {
            try {
                File file = new File(getFilesDir().getAbsolutePath() + File.separator + "testfilename_" + i);
                try (FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath())) {
                    outputStream.write(fileStrings[i].getBytes(StandardCharsets.UTF_8));
                }
                files[i] = file;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            GDHelper.getInstance().putFiles((Activity)this.getApplicationContext(), "AndroidBackupFolder", files);
            Toast.makeText(this, "Executed successfully", Toast.LENGTH_SHORT).show();
        } catch (GDActionException e) {
            Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();
    }
}