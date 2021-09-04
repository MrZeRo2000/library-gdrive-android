package com.romanpulov.library.gdrive;

import android.app.Activity;

import org.json.JSONObject;

public class GDGetOrCreateFolderAction extends GDAbstractTokenRequiresAction<String>{
    private final String mFolderName;

    @Override
    protected void executeWithToken() {
        //q: mimeType = 'application/vnd.google-apps.folder' and name = 'AndroidBackup' and trashed = false
        //fields: files(id)

    }

    public GDGetOrCreateFolderAction(Activity activity, String folderName, OnGDActionListener<String> gdActionListener) {
        super(activity, gdActionListener);
        this.mFolderName = folderName;
    }
}
