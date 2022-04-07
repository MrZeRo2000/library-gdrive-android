package com.romanpulov.library.gdrive.testapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.romanpulov.library.gdrive.GDActionException;

import java.io.File;

public class MainWorker extends Worker {
    private final static String TAG = MainWorker.class.getSimpleName();

    public MainWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        File[] files = GDHelper.generateFiles(getApplicationContext());

        Log.d(TAG, "Got data:" + getInputData().getString("ClassName"));

        try {
            Log.d(TAG, "logging in silently");
            GDHelper.getInstance().silentLogin(getApplicationContext());
            Log.d(TAG, "silent login success");
            Log.d(TAG, "putting files");
            GDHelper.getInstance().putFiles(this.getApplicationContext(), "AndroidBackupFolder", files);
            Log.d(TAG, "put files success");
            //Toast.makeText(this, "Executed successfully", Toast.LENGTH_SHORT).show();
            return Result.success();
        } catch (GDActionException e) {
            //Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error in service:" + e.getMessage());
            return Result.failure();
        }

    }
}
