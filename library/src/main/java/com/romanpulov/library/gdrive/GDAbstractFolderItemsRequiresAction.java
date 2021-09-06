package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.util.Log;

import org.json.JSONObject;

public abstract class GDAbstractFolderItemsRequiresAction extends GDAbstractFolderRequiresAction {
    private final static String TAG = GDAbstractFolderItemsRequiresAction.class.getSimpleName();

    protected abstract void executeWithFolderItems(String folderId, JSONObject folderItems);

    @Override
    protected void executeWithFolderId(String folderId) {
        new GDListItemsAction(mActivity, folderId, new OnGDActionListener<JSONObject>() {
            @Override
            public void onActionSuccess(JSONObject folderItems) {
                Log.d(TAG, "Got list items data:" + folderItems.toString() + ", folderId:" + folderId);
                executeWithFolderItems(folderId, folderItems);
            }

            @Override
            public void onActionFailure(Exception exception) {
                notifyError(exception);
            }
        }).execute();
    }

    public GDAbstractFolderItemsRequiresAction(Activity activity, String path, OnGDActionListener<String> gdActionListener) {
        super(activity, path, gdActionListener);
    }
}
