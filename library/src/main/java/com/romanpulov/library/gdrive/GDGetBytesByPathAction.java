package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GDGetBytesByPathAction extends GDAbstractTokenRequiresAction<byte[]>{
    private final static String TAG = GDGetBytesByPathAction.class.getSimpleName();

    private final String mPath;

    @Override
    protected void executeWithToken() {
        String[] ffn = GDPathUtils.pathToFolderNameAndFileName(mPath);
        String folderName = ffn[0];
        String fileName = ffn[1];
        if (ffn[1] == null) {
            notifyError(new GDActionException("File name not provided in path:" + mPath));
            return;
        }

        // getting root folder
        String url;
        try {
            url = String.format(
                    "https://www.googleapis.com/drive/v3/files?&q=%s&fields=%s&key=%s",
                    URLEncoder.encode(String.format("mimeType = 'application/vnd.google-apps.folder' and name = '%s' and trashed = false and 'root' in parents ", folderName), String.valueOf(StandardCharsets.UTF_8)),
                    URLEncoder.encode("files(id)", String.valueOf(StandardCharsets.UTF_8)),
                    GDConfig.get().getAuthConfigData(mActivity).getClientSecret()
            );

        } catch (UnsupportedEncodingException e) {
            notifyError(e);
            return;
        }

        HttpRequestWrapper.executeGetJSONTokenRequest(
                mActivity,
                url,
                GDAuthData.mAccessToken.get(),
                response -> {
                    Log.d(TAG, "Got some data:" + response.toString());

                    try {
                        JSONArray folders = response.getJSONArray("files");
                        if (folders.length() > 0) {
                            //folder exists, looking for file
                            JSONObject o = (JSONObject)folders.get(0);
                            String folderId = o.getString("id");

                            //getting file id
                            String fileURL;
                            try {
                                fileURL = String.format(
                                        "https://www.googleapis.com/drive/v3/files?&q=%s&fields=%s&key=%s",
                                        URLEncoder.encode(String.format("mimeType != 'application/vnd.google-apps.folder' and name = '%s' and trashed = false and '%s' in parents ", fileName, folderId), String.valueOf(StandardCharsets.UTF_8)),
                                        URLEncoder.encode("files(id, name)", String.valueOf(StandardCharsets.UTF_8)),
                                        GDConfig.get().getAuthConfigData(mActivity).getClientSecret()
                                );

                                HttpRequestWrapper.executeGetJSONTokenRequest(
                                        mActivity,
                                        fileURL,
                                        GDAuthData.mAccessToken.get(),
                                        fileResponse -> {
                                            Log.d(TAG, "Got file response:" + fileResponse.toString());
                                            try {
                                                JSONArray files = fileResponse.getJSONArray("files");
                                                if (files.length() > 0) {
                                                    Log.d(TAG, "File exists, reading");
                                                    JSONObject fo = (JSONObject)files.get(0);
                                                    String fileId = fo.getString("id");

                                                    String fileGetURL = String.format("https://www.googleapis.com/drive/v3/files/%s?alt=media", fileId);
                                                    HttpRequestWrapper.executeGetBytesRequest(
                                                            mActivity,
                                                            fileGetURL,
                                                            GDAuthData.mAccessToken.get(),
                                                            this::notifySuccess,
                                                            error -> notifyError(HttpRequestException.fromVolleyError(error))
                                                    );

                                                } else {
                                                    notifyError(new GDActionException("File does not exist"));
                                                }
                                            } catch (JSONException e) {
                                                notifyError(e);
                                            }

                                        },
                                        error -> notifyError(HttpRequestException.fromVolleyError(error))
                                );

                            } catch (UnsupportedEncodingException e) {
                                notifyError(e);
                            }

                        } else {
                            notifyError(new GDActionException("Folder does not exist"));
                        }
                    } catch (JSONException e) {
                        notifyError(e);
                    }
                },
                error -> notifyError(HttpRequestException.fromVolleyError(error))
        );

    }

    public GDGetBytesByPathAction(Activity activity, String path, OnGDActionListener<byte[]> gdActionListener) {
        super(activity, gdActionListener);
        this.mPath = path;
    }
}
