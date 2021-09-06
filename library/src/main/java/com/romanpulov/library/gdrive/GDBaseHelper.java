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
        new GDGetOrCreateFolderAction(activity, path, new OnGDActionListener<String>() {
            @Override
            public void onActionSuccess(String folderId) {
                new GDListItemsAction(activity, folderId, new OnGDActionListener<JSONObject>() {
                    @Override
                    public void onActionSuccess(JSONObject folderItems) {
                        Log.d(TAG, "Got list items data:" + folderItems.toString() + ", folderId:" + folderId);
                        callback.onActionSuccess(null);
                    }

                    @Override
                    public void onActionFailure(Exception exception) {
                        callback.onActionFailure(exception);
                    }
                }).execute();
            }

            @Override
            public void onActionFailure(Exception exception) {
                callback.onActionFailure(exception);
            }
        }).execute();
    }
}
