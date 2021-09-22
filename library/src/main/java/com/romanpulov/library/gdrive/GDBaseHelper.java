package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.content.Context;

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

    public void load(Activity activity, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDLoadAccountAction(activity, callback));
    }

    public void listItems(Activity activity, OnGDActionListener<JSONObject> callback) {
        GDActionExecutor.execute(new GDListItemsAction(activity, "root", callback));
    }

    public void putFiles(Activity activity, String path, File[] files, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDPutFilesAction(activity, GDPathUtils.pathToFolder(path), files, callback));
    }

    public void putFiles(Activity activity, String path, File[] files) throws GDActionException {
        GDActionExecutor.executeSync(new GDPutFilesAction(activity, GDPathUtils.pathToFolder(path), files, null));
    }

    public void getBytesByPath(Activity activity, String path, OnGDActionListener<byte[]> callback) {
        GDActionExecutor.execute(new GDGetBytesByPathAction(activity, path, callback));
    }
}
