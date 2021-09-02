package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Token requires abstract action
 * @param <D> data type to return
 */
public abstract class GDAbstractTokenRequiresAction<D> extends GDAbstractAuthCodeRequiresAction<D>{
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

            try {
                String postString = String.format(
                        "code=%s&client_id=%s&client_secret=%s&redirect_uri=&grant_type=authorization_code",
                        GDAuthData.mAuthCode.get(),
                        GDConfig.get().getAuthConfigData(mActivity).getWebClientId(),
                        GDConfig.get().getAuthConfigData(mActivity).getClientSecret()
                );
                byte[] postData = postString.getBytes(StandardCharsets.UTF_8);

                HttpRequestWrapper.executePostRequest(
                        mActivity,
                        "https://oauth2.googleapis.com/token",
                        "application/x-www-form-urlencoded",
                        postData,
                        response -> {
                            Log.d(TAG, "Got response:" + response);
                            executeWithToken();
                        },
                        error -> {
                            notifyError(HttpRequestException.fromVolleyError(error));
                        }
                );

            } catch (IOException | JSONException e) {
                notifyError(e);
            }

        }
    }

    public GDAbstractTokenRequiresAction(Activity activity, OnGDActionListener<D> gdActionListener) {
        super(activity, gdActionListener);
    }
}
