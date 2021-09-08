package com.romanpulov.library.gdrive;

import android.app.Activity;

import com.romanpulov.jutilscore.io.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import android.util.Base64;
import android.util.Log;

public class GDCreateOrUpdateFileAction extends GDAbstractFolderItemsRequiresAction {
    private final static String TAG = GDCreateOrUpdateFileAction.class.getSimpleName();

    private final File mFile;

    @Override
    protected void executeWithFolderItems(String folderId, JSONObject folderItems) {
        String boundary = "-------314159265358979323846";
        String delimiter = "\r\n--" + boundary + "\r\n";
        String close_delim = "\r\n--" + boundary + "--";
        String contentType = "application/octet-stream";

        JSONObject metadataObject = new JSONObject();
        try {
            metadataObject.put("name", mFile.getName());
            metadataObject.put("mimeType", "application/octet-stream");

            JSONArray parentsArray = new JSONArray();
            parentsArray.put(folderId);
            metadataObject.put("parents", parentsArray);

        } catch (JSONException e) {
            notifyError(e);
        }

        String metadata = metadataObject.toString();
        Log.d(TAG, "Metadata:" + metadata);

        try (
                InputStream inputStream = new FileInputStream(mFile);
                ByteArrayOutputStream outputStreamStream = new ByteArrayOutputStream();
        ) {
            FileUtils.copyStream(inputStream, outputStreamStream);
            byte[] base64Data = Base64.encode(outputStreamStream.toByteArray(), Base64.DEFAULT);
            String base64String = new String(base64Data, StandardCharsets.UTF_8);

            Log.d(TAG, "base64String:" + base64String);

            String multipartRequestBody =
                    delimiter +
                            "Content-Type: application/json\r\n\r\n" +
                            metadata +
                            delimiter +
                            "Content-Type: " + contentType + "\r\n" +
                            "Content-Transfer-Encoding: base64\r\n" +
                            "\r\n" +
                            base64String +
                            close_delim;

            Map<String, String> params = new HashMap<>();
            params.put("uploadType", "multipart");

            String fileId = null;
            try {
                JSONArray files = (JSONArray)folderItems.get("files");
                for (int i = 0; i < files.length(); i++) {
                    if (files.getJSONObject(i).get("name").equals(mFile.getName())) {
                        fileId = (String)files.getJSONObject(i).get("id");
                        break;
                    }
                }
            } catch (JSONException e) {
                notifyError(e);
                return;
            }

            if (fileId == null) {
                String postURL = String.format(
                        "https://www.googleapis.com/upload/drive/v3/files?key=%s",
                        GDConfig.get().getAuthConfigData(mActivity).getClientSecret()
                );

                HttpRequestWrapper.executePostRequest(
                        mActivity,
                        postURL,
                        GDAuthData.mAccessToken.get(),
                        "multipart/mixed; boundary=\"" + boundary + "\"",
                        params,
                        multipartRequestBody.getBytes(StandardCharsets.UTF_8),
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
            } else {
                String patchURL = String.format(
                        "https://www.googleapis.com/upload/drive/v3/files/%s",
                        fileId
                );

                HttpRequestWrapper.executePatchRequest(
                        mActivity,
                        patchURL,
                        GDAuthData.mAccessToken.get(),
                        "multipart/mixed; boundary=\"" + boundary + "\"",
                        params,
                        multipartRequestBody.getBytes(StandardCharsets.UTF_8),
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


        } catch (IOException e) {
            notifyError(e);
        }

    }

    public GDCreateOrUpdateFileAction(Activity activity, String path, File file, OnGDActionListener<String> gdActionListener) {
        super(activity, path, gdActionListener);
        this.mFile = file;
    }
}
