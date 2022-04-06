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
    private final static String TAG = MainService.class.getSimpleName();

    public MainService() {
        super("MainService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "starting onHandleIntent");

        File[] files = GDHelper.generateFiles(getApplicationContext());

        try {
            Log.d(TAG, "logging in silently");
            GDHelper.getInstance().silentLogin(getApplicationContext());
            Log.d(TAG, "silent login success");
            Log.d(TAG, "putting files");
            GDHelper.getInstance().putFiles(this.getApplicationContext(), "AndroidBackupFolder", files);
            Log.d(TAG, "put files success");
            //Toast.makeText(this, "Executed successfully", Toast.LENGTH_SHORT).show();
        } catch (GDActionException e) {
            //Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error in service:" + e.getMessage());
        }
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, "service onStart");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service destroyed");
    }
}