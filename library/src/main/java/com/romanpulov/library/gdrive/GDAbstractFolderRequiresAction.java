package com.romanpulov.library.gdrive;

import android.app.Activity;

public abstract class GDAbstractFolderRequiresAction extends GDAbstractTokenRequiresAction<String>{

    private final String mPath;

    protected abstract void executeWithFolderId(String folderId);

    @Override
    protected void executeWithToken() {
        new GDGetOrCreateFolderAction(mActivity, mPath, new OnGDActionListener<String>() {
            @Override
            public void onActionSuccess(String folderId) {
                executeWithFolderId(folderId);
            }

            @Override
            public void onActionFailure(Exception exception) {
                notifyError(exception);
            }
        }).execute();
    }

    public GDAbstractFolderRequiresAction(Activity activity, String path, OnGDActionListener<String> gdActionListener) {
        super(activity, gdActionListener);
        this.mPath = path;
    }
}
