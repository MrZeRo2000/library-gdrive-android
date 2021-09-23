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

    public void listItems(Context context, OnGDActionListener<JSONObject> callback) {
        GDActionExecutor.execute(new GDListItemsAction(context, "root", callback));
    }

    public void putFiles(Context context, String path, File[] files, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDPutFilesAction(context, GDPathUtils.pathToFolder(path), files, callback));
    }

    public void putFiles(Context context, String path, File[] files) throws GDActionException {
        GDActionExecutor.executeSync(new GDPutFilesAction(context, GDPathUtils.pathToFolder(path), files, null));
    }

    public void putBytesByPath(Context context, String path, byte[] data) throws GDActionException {
        GDActionExecutor.executeSync(new GDPutBytesByPathAction(context, path, data, null));
    }

    public void getBytesByPath(Context context, String path, OnGDActionListener<byte[]> callback) {
        GDActionExecutor.execute(new GDGetBytesByPathAction(context, path, callback));
    }

    public byte[] getBytesByPath(Context context, String path)  throws GDActionException {
        return GDActionExecutor.executeSync(new GDGetBytesByPathAction(context, path, null));
    }
}
