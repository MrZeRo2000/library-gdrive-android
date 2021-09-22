package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Token requires abstract action
 * @param <D> data type to return
 */
public abstract class GDAbstractTokenRequiresAction<D> extends GDAbstractAuthCodeAvailableAction<D>{
    private static final String TAG = GDAbstractTokenRequiresAction.class.getSimpleName();

    protected abstract void executeWithToken();

    /**
     * https://developers.google.com/identity/protocols/oauth2/web-server#exchange-authorization-code
     */
    @Override
    protected void executeWithAuthCode() {
        if ((GDAuthData.mAccessToken.get() != null)
                && (GDAuthData.mAccessTokenExpireTime.get() != null)
                && (SystemClock.elapsedRealtime() < GDAuthData.mAccessTokenExpireTime.get())

        )
        {
            executeWithToken();
        } else
        {
            Log.d(TAG, "Token is not available, obtaining");
            GDAuthData.clearAccessToken();

            String postString = String.format(
                    "code=%s&client_id=%s&client_secret=%s&redirect_uri=&grant_type=authorization_code",
                    GDAuthData.mAuthCode.get(),
                    GDConfig.get().getAuthConfigData(mContext).getWebClientId(),
                    GDConfig.get().getAuthConfigData(mContext).getClientSecret()
            );
            Log.d(TAG, "Post string:" + postString);
            byte[] postData = postString.getBytes(StandardCharsets.UTF_8);

            HttpRequestWrapper.executePostRequest(
                    mContext,
                    "https://oauth2.googleapis.com/token",
                    "application/x-www-form-urlencoded",
                    postData,
                    response -> {
                        Log.d(TAG, "Got response:" + response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            GDAuthData.mAccessToken.set(jsonResponse.getString("access_token"));
                            GDAuthData.mAccessTokenExpireTime.set(SystemClock.elapsedRealtime() + jsonResponse.getLong("expires_in") * 1000);

                            Log.d(TAG, "Got the token");
                            executeWithToken();
                        } catch (JSONException e) {
                            notifyError(e);
                        }
                    },
                    error -> notifyError(HttpRequestException.fromVolleyError(error))
            );

        }
    }

    public GDAbstractTokenRequiresAction(Context context, OnGDActionListener<D> gdActionListener) {
        super(context, gdActionListener);
    }
}
