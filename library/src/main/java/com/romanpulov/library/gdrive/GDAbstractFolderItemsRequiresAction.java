package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class GDAbstractFolderItemsRequiresAction<T> extends GDAbstractFolderRequiresAction<T> {
    private final static String TAG = GDAbstractFolderItemsRequiresAction.class.getSimpleName();

    protected abstract void executeWithFolderItems(String folderId, Map<String, String> items);

    private static Map<String, String> parseFolderItems(JSONObject items) throws JSONException {
        Map<String, String> result = new HashMap<>();

        JSONArray files = (JSONArray)items.get("files");
        for (int i = 0; i < files.length(); i++) {
            result.put(
                    (String)files.getJSONObject(i).get("name"),
                    (String)files.getJSONObject(i).get("id")
            );
        }

        return result;
    }

    @Override
    protected void executeWithFolderId(String folderId) {
        new GDListItemsAction(mActivity, folderId, new OnGDActionListener<JSONObject>() {
            @Override
            public void onActionSuccess(JSONObject folderItems) {
                Log.d(TAG, "Got list items data:" + folderItems.toString() + ", folderId:" + folderId);
                try {
                    executeWithFolderItems(folderId, parseFolderItems(folderItems));
                } catch (JSONException e) {
                    notifyError(e);
                }
            }

            @Override
            public void onActionFailure(Exception exception) {
                notifyError(exception);
            }
        }).execute();
    }

    public GDAbstractFolderItemsRequiresAction(Activity activity, String path, OnGDActionListener<T> gdActionListener) {
        super(activity, path, gdActionListener);
    }
}
