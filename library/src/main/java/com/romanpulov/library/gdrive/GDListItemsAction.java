package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.util.Log;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GDListItemsAction extends GDAbstractTokenRequiresAction<JSONObject> {
    private final static String TAG = GDListItemsAction.class.getSimpleName();

    @Override
    protected void executeWithToken() {
        Log.d(TAG, "Executing with token");

        try {
            String url = String.format(
                    "https://www.googleapis.com/drive/v3/files?orderBy=%s&q=%s&fields=%s&key=%s",
                    URLEncoder.encode("folder,name", String.valueOf(StandardCharsets.UTF_8)),
                    URLEncoder.encode("parents in 'root'", String.valueOf(StandardCharsets.UTF_8)),
                    URLEncoder.encode("files(id,mimeType,name)", String.valueOf(StandardCharsets.UTF_8)),
                    GDConfig.get().getAuthConfigData(mActivity).getClientSecret()
            );

            HttpRequestWrapper.executeGetJSONTokenRequest(
                    mActivity,
                    url,
                    GDAuthData.mAccessToken.get(),
                    this::notifySuccess,
                    error -> notifyError(HttpRequestException.fromVolleyError(error))
            );
        } catch (UnsupportedEncodingException e) {
            notifyError(e);
        }

    }

    public GDListItemsAction(Activity activity, OnGDActionListener<JSONObject> gdActionListener) {
        super(activity, gdActionListener);
    }
}
