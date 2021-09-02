package com.romanpulov.library.gdrive;

import android.app.Activity;

import org.json.JSONObject;

public abstract class GDBaseHelper {
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

    public void listItems(Activity activity, String path, OnGDActionListener<JSONObject> callback) {
        GDActionExecutor.execute(new GDListItemsAction(activity, path, callback));
    }
}
