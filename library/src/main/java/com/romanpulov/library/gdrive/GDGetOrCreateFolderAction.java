package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GDGetOrCreateFolderAction extends GDAbstractTokenRequiresAction<String>{
    private final static String TAG = GDGetOrCreateFolderAction.class.getSimpleName();

    private final String mFolderName;

    @Override
    protected void executeWithToken() {
        //q: mimeType = 'application/vnd.google-apps.folder' and name = 'AndroidBackup' and trashed = false
        //fields: files(id)
        Log.d(TAG, "Executing with token:get");

        String url;
        try {
            url = String.format(
                    "https://www.googleapis.com/drive/v3/files?&q=%s&fields=%s&key=%s",
                    URLEncoder.encode(String.format("mimeType = 'application/vnd.google-apps.folder' and name = '%s' and trashed = false", mFolderName), String.valueOf(StandardCharsets.UTF_8)),
                    URLEncoder.encode("files(id)", String.valueOf(StandardCharsets.UTF_8)),
                    GDConfig.get().getAuthConfigData(mActivity).getClientSecret()
            );

        } catch (UnsupportedEncodingException e) {
            url = null;
            notifyError(e);
        }

        if (url != null) {
            HttpRequestWrapper.executeGetJSONTokenRequest(
                    mActivity,
                    url,
                    GDAuthData.mAccessToken.get(),
                    response -> {
                        Log.d(TAG, "Got data:" + response.toString());
                        try {
                            JSONArray folders = response.getJSONArray("files");
                            if (folders.length() > 0) {
                                try {
                                    JSONObject o = (JSONObject)folders.get(0);
                                    notifySuccess(o.getString("id"));
                                } catch (JSONException e) {
                                    notifyError(e);
                                }
                            } else {
                                Log.d(TAG, "Folder does not exist, creating");

                                String postURL = String.format(
                                        "https://www.googleapis.com/drive/v3/files?key=%s",
                                        GDConfig.get().getAuthConfigData(mActivity).getClientSecret()
                                );

                                String postData =
                                        String.format("{\"mimeType\":\"application/vnd.google-apps.folder\",\"name\":\"%s\"}", mFolderName);
                                Log.d(TAG, "Post data:" + postData);
                                byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);

                                HttpRequestWrapper.executePostRequest(
                                        mActivity,
                                        postURL,
                                        GDAuthData.mAccessToken.get(),
                                        "application/json",
                                        null,
                                        postDataBytes,
                                        postResponse -> {
                                            try {
                                                JSONObject o = new JSONObject(postResponse);
                                                notifySuccess(o.getString("id"));
                                            } catch (JSONException e) {
                                                notifyError(e);
                                            }
                                        },
                                        error -> notifyError(HttpRequestException.fromVolleyError(error))
                                        );


                            }
                        } catch (JSONException e) {
                            notifyError(e);
                        }
                    },
                    error -> notifyError(HttpRequestException.fromVolleyError(error))
            );
        }

    }

    public GDGetOrCreateFolderAction(Activity activity, String folderName, OnGDActionListener<String> gdActionListener) {
        super(activity, gdActionListener);
        this.mFolderName = folderName;
    }
}
