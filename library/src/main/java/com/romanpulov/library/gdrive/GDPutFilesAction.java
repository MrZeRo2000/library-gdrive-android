package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GDPutFilesAction extends GDAbstractFolderItemsRequiresAction<Void> {
    private final String TAG = GDPutFilesAction.class.getSimpleName();

    private final File[] mFiles;

    @Override
    protected void executeWithFolderItems(String folderId, Map<String, String> items) {
        Log.d(TAG, "Executing with folder items");

        if ((mFiles == null) || (mFiles.length == 0)) {
            notifySuccess(null);
        } else {
            final AtomicInteger fileCounter = new AtomicInteger(mFiles.length);
            final AtomicBoolean errorFlag = new AtomicBoolean(false);

            for (File f: mFiles) {
                Log.d(TAG, "Processing file: " + f.getName());

                try (
                        InputStream inputStream = new FileInputStream(f);
                        ByteArrayOutputStream outputStreamStream = new ByteArrayOutputStream();
                ) {
                    FileUtils.copyStream(inputStream, outputStreamStream);
                    byte[] base64Data = Base64.encode(outputStreamStream.toByteArray(), Base64.DEFAULT);
                    String base64String = new String(base64Data, StandardCharsets.UTF_8);

                    JSONObject metadataObject = new JSONObject();

                    metadataObject.put("name", f.getName());
                    metadataObject.put("mimeType", "application/octet-stream");

                    JSONArray parentsArray = new JSONArray();
                    parentsArray.put(folderId);
                    metadataObject.put("parents", parentsArray);

                    String metadata = metadataObject.toString();
                    Log.d(TAG, "Metadata:" + metadata);


                    String boundary = "-------314159265358979323846";
                    String delimiter = "\r\n--" + boundary + "\r\n";
                    String closeDelimiter = "\r\n--" + boundary + "--";
                    String contentType = "application/octet-stream";

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

                    Map<String, String> params = new HashMap<>();
                    params.put("uploadType", "multipart");

                    String fileId = items.get(f.getName());

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
                                    if ((fileCounter.decrementAndGet() == 0)  && (!errorFlag.get())) {
                                        Log.d(TAG, "Invoking success");
                                        notifySuccess(null);
                                    }
                                },
                                error -> {
                                    if (!errorFlag.getAndSet(true)) {
                                        Log.d(TAG, "Error response from Http Request:" + HttpRequestException.fromVolleyError(error));
                                        notifyError(HttpRequestException.fromVolleyError(error));
                                    }
                                }
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
                                    if ((fileCounter.decrementAndGet() == 0)  && (!errorFlag.get())) {
                                        Log.d(TAG, "Invoking success");
                                        notifySuccess(null);
                                    }
                                },
                                error -> {
                                    if (!errorFlag.getAndSet(true)) {
                                        Log.d(TAG, "Error response from Http Request:" + HttpRequestException.fromVolleyError(error));
                                        notifyError(HttpRequestException.fromVolleyError(error));
                                    }
                                }
                        );
                    }

                } catch (IOException | JSONException e) {
                    if (!errorFlag.getAndSet(true)) {
                        Log.d(TAG, "I/O or other error:" + e.getMessage());
                        notifyError(e);
                    }
                }
            }
        }

    }

    public GDPutFilesAction(Activity activity, String path, File[] files, OnGDActionListener<Void> gdActionListener) {
        super(activity, path, gdActionListener);
        this.mFiles = files;
    }
}
