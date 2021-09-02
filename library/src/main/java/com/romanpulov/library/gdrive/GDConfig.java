package com.romanpulov.library.gdrive;

import android.content.Context;

import com.romanpulov.jutilscore.io.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Google Drive configuration class
 */
public class GDConfig {
    private int mConfigResId;

    public int getConfigResId() {
        return mConfigResId;
    }

    private String mScope;

    public String getScope() {
        return mScope;
    }

    private int mAuthRequestCode;

    public int getAuthRequestCode() {
        return mAuthRequestCode;
    }

    public static class GDAuthConfigData {
        private final String mWebClientId;
        private final String mClientSecret;

        public String getWebClientId() {
            return mWebClientId;
        }

        public String getClientSecret() {
            return mClientSecret;
        }

        public GDAuthConfigData(String webClientId, String clientSecret) {
            this.mWebClientId = webClientId;
            this.mClientSecret = clientSecret;
        }
    }

    private static GDConfig instance;

    private GDConfig(int configResId, String scope, int authRequestCode) {
        this.mConfigResId = configResId;
        this.mScope = scope;
        this.mAuthRequestCode = authRequestCode;
    }

    public static GDConfig configure(int configResId, String scope, int authRequestCode) {
        if (instance == null) {
            instance = new GDConfig(configResId, scope, authRequestCode);
        } else {
            instance.mConfigResId = configResId;
            instance.mScope = scope;
        }

        return instance;
    }

    public static GDConfig get() {
        if (instance == null) {
            throw new RuntimeException("GD configuration not complete. Please, configure first");
        }
        return instance;
    }

    public GDAuthConfigData getAuthConfigData(Context context) throws IOException, JSONException {
        try (
                InputStream inputStream = context.getResources().openRawResource(GDConfig.get().getConfigResId());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        )
        {
            FileUtils.copyStream(inputStream, outputStream);

            JSONObject json = new JSONObject(outputStream.toString());

            return new GDAuthConfigData(
                    json.getString("web_client_id"),
                    json.getString("client_secret")
            );
        }
    }
}
