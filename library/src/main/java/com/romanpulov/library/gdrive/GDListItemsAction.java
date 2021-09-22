package com.romanpulov.library.gdrive;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GDListItemsAction extends GDAbstractTokenRequiresAction<JSONObject> {
    private final static String TAG = GDListItemsAction.class.getSimpleName();

    private final String mParent;

    @Override
    protected void executeWithToken() {
        Log.d(TAG, "Executing with token");

        try {
            String url = String.format(
                    "https://www.googleapis.com/drive/v3/files?orderBy=%s&q=%s&fields=%s&key=%s",
                    URLEncoder.encode("folder,name", String.valueOf(StandardCharsets.UTF_8)),
                    URLEncoder.encode(String.format("parents in '%s' and trashed = false", mParent), String.valueOf(StandardCharsets.UTF_8)),
                    URLEncoder.encode("files(id,mimeType,name)", String.valueOf(StandardCharsets.UTF_8)),
                    GDConfig.get().getAuthConfigData(mContext).getClientSecret()
            );

            HttpRequestWrapper.executeGetJSONTokenRequest(
                    mContext,
                    url,
                    GDAuthData.mAccessToken.get(),
                    this::notifySuccess,
                    error -> notifyError(HttpRequestException.fromVolleyError(error))
            );
        } catch (UnsupportedEncodingException e) {
            notifyError(e);
        }

    }

    public GDListItemsAction(Context context, String parent, OnGDActionListener<JSONObject> gdActionListener) {
        super(context, gdActionListener);
        this.mParent = parent;
    }
}
