package com.romanpulov.library.gdrive;

import android.app.Activity;

public abstract class GDBaseHelper {
    protected abstract void configure();

    public GDBaseHelper() {
        configure();
    }

    public void login(Activity activity, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDInteractiveAuthenticationAction(activity, callback));
    }

    public void logout(Activity activity, OnGDActionListener<Void> callback) {
        GDActionExecutor.execute(new GDSignOutAction(activity, callback));
    }
}
