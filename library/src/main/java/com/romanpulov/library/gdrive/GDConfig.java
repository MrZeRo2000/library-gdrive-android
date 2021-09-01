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
    private String mScope;

    public int getConfigResId() {
        return mConfigResId;
    }

    public String getScope() {
        return mScope;
    }

    public static class GDAuthConfigData {
        public final String webClientId;
        public final String clientSecret;

        public GDAuthConfigData(String webClientId, String clientSecret) {
            this.webClientId = webClientId;
            this.clientSecret = clientSecret;
        }
    }

    private static GDConfig instance;

    private GDConfig(int mConfigResId, String mScope) {
        this.mConfigResId = mConfigResId;
        this.mScope = mScope;
    }

    public static GDConfig configure(int configResId, String scope) {
        if (instance == null) {
            instance = new GDConfig(configResId, scope);
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
