package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;

public abstract class GDBaseHelper {
    private final static String TAG = GDBaseHelper.class.getSimpleName();

    protected abstract void configure();

    public GDBaseHelper() {
        configure();
    }

    public void setServerAuthCode(String authCode) {
        GDAuthData.setAuthCode(authCode);
    }

    public void login(Activity activity, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDInteractiveAuthenticationAction(activity, callback));
    }

    public void logout(Activity activity, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDSignOutAction(activity, callback));
    }

    public void listItems(Activity activity, OnGDActionListener<JSONObject> callback) {
        GDActionExecutor.execute(new GDListItemsAction(activity, "root", callback));
    }

    public void putFiles(Activity activity, String path, File[] files, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDPutFilesAction(activity, path, files, callback));
    }
}
