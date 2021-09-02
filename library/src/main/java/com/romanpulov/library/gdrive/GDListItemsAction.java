package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.util.Log;

import org.json.JSONObject;

public class GDListItemsAction extends GDAbstractTokenRequiresAction<JSONObject> {
    private final static String TAG = GDListItemsAction.class.getSimpleName();

    private final String mPath;

    @Override
    protected void executeWithToken() {
        Log.d(TAG, "Executing with token");

    }

    public GDListItemsAction(Activity activity, String path, OnGDActionListener<JSONObject> gdActionListener) {
        super(activity, gdActionListener);
        this.mPath = path;
    }
}
