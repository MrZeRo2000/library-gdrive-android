package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import org.json.JSONObject;

import java.io.File;

public abstract class GDBaseHelper {

    private GDInteractiveAuthenticationAction mGDInteractiveAuthenticationAction;

    protected abstract void configure();

    public GDBaseHelper() {
        configure();
    }

    public void setServerAccessToken(String accessToken) {
        GDAuthData.setAccessToken(accessToken);
    }

    public void registerActivity(@NonNull Activity activity) {
        this.mGDInteractiveAuthenticationAction =
                new GDInteractiveAuthenticationAction(activity, null);
    }

    public void unregisterActivity() {
        this.mGDInteractiveAuthenticationAction = null;
    }

    public void login(OnGDActionListener<Void> callback) {
        this.mGDInteractiveAuthenticationAction.setGDActionListener(callback);
        GDActionExecutor.execute(this.mGDInteractiveAuthenticationAction);
    }

    public void logout(Context context, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDSignOutAction(context, callback));
    }

    public void silentLogin(Context context) throws GDActionException {
        GDActionExecutor.executeSync(new GDLoadAccountAction(context, null));
   }

    public void load(Context context, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDLoadAccountAction(context, callback));
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
