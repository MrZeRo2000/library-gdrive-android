package com.romanpulov.library.gdrive;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.romanpulov.jutilscore.io.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GDPutBytesByPathAction extends GDAbstractFolderItemsRequiresAction<Void> {
    private final String TAG = GDPutBytesByPathAction.class.getSimpleName();

    private final String mFileName;
    private final byte[] mData;

    public GDPutBytesByPathAction(Context context, String path, String fileName, byte[] data, OnGDActionListener<Void> gdActionListener) {
        super(context, path, gdActionListener);
        this.mFileName = fileName;
        this.mData = data;
    }

    @Override
    protected void executeWithFolderItems(String folderId, Map<String, String> items) {
        Log.d(TAG, "Executing with folder items");

        try {

            byte[] base64Data = Base64.encode(mData, Base64.DEFAULT);
            String base64String = new String(base64Data, StandardCharsets.UTF_8);

            String boundary = "-------314159265358979323846";
            String delimiter = "\r\n--" + boundary + "\r\n";
            String closeDelimiter = "\r\n--" + boundary + "--";
            String contentType = "application/octet-stream";

            Map<String, String> params = new HashMap<>();
            params.put("uploadType", "multipart");

            String fileId = items.get(mFileName);

            if (fileId == null) {
                //New file, POST request

                String postURL = String.format(
                        "https://www.googleapis.com/upload/drive/v3/files?key=%s?uploadType=multipart",
                        GDConfig.get().getAuthConfigData(mContext).getClientSecret()
                );

                JSONObject metadataObject = new JSONObject();

                metadataObject.put("name", mFileName);
                metadataObject.put("mimeType", "application/octet-stream");

                JSONArray parentsArray = new JSONArray();
                parentsArray.put(folderId);
                metadataObject.put("parents", parentsArray);

                String metadata = metadataObject.toString();

                String multipartRequestBody =
                        delimiter +
                                "Content-Type: application/json\r\n\r\n" +
                                metadata +
                                delimiter +
                                "Content-Type: " + contentType + "\r\n" +
                                "Content-Transfer-Encoding: base64\r\n" +
                                "\r\n" +
                                base64String +
                                closeDelimiter;

                HttpRequestWrapper.executePostRequest(
                        mContext,
                        postURL,
                        GDAuthData.mAccessToken.get(),
                        "multipart/mixed; boundary=\"" + boundary + "\"",
                        params,
                        multipartRequestBody.getBytes(StandardCharsets.UTF_8),
                        postResponse -> {
                            Log.d(TAG, "Invoking success");
                            notifySuccess(null);
                        },
                        error -> {
                            Log.d(TAG, "Error response from Http Request:" + HttpRequestException.fromVolleyError(error));
                            notifyError(HttpRequestException.fromVolleyError(error));
                        }
                );
            } else {
                //update file, PATCH request

                String patchURL = String.format(
                        "https://www.googleapis.com/upload/drive/v3/files/%s?uploadType=multipart",
                        fileId
                );

                JSONObject updateMetadataObject = new JSONObject();
                updateMetadataObject.put("name", mFileName);
                updateMetadataObject.put("mimeType", "application/octet-stream");
                String updateMetadata = updateMetadataObject.toString();

                String updateMultipartRequestBody =
                        delimiter +
                                "Content-Type: application/json\r\n\r\n" +
                                updateMetadata +
                                delimiter +
                                "Content-Type: " + contentType + "\r\n" +
                                "Content-Transfer-Encoding: base64\r\n" +
                                "\r\n" +
                                base64String +
                                closeDelimiter;

                HttpRequestWrapper.executePatchRequest(
                        mContext,
                        patchURL,
                        GDAuthData.mAccessToken.get(),
                        "multipart/mixed; boundary=\"" + boundary + "\"",
                        params,
                        updateMultipartRequestBody.getBytes(StandardCharsets.UTF_8),
                        postResponse -> {
                            Log.d(TAG, "Invoking success");
                            notifySuccess(null);
                        },
                        error -> {
                            Log.d(TAG, "Error response from Http Request:" + HttpRequestException.fromVolleyError(error));
                            notifyError(HttpRequestException.fromVolleyError(error));
                        }
                );
            }

        } catch (JSONException e) {
            Log.d(TAG, "I/O or other error:" + e.getMessage());
            notifyError(e);
        }

    }
}
