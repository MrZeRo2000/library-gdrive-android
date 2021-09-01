package com.romanpulov.library.gdrive.testapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.romanpulov.library.gdrive.GDConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.romanpulov.jutilscore.io.FileUtils;

public class ReadRawResourceTest {
    @Test
    public void readRawResource() throws IOException, JSONException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNotNull(appContext);

        //ensure helper is configured
        GDHelper gdHelper = GDHelper.getInstance();
        assertNotNull(gdHelper);

        try (
                InputStream inputStream = appContext.getResources().openRawResource(GDConfig.get().getConfigResId());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        )
        {
            FileUtils.copyStream(inputStream, outputStream);
            //new JSONObject()
            JSONObject json = new JSONObject(outputStream.toString());
            String webClientId = json.getString("web_client_id");
            String clientSecret = json.getString("client_secret");

            assertEquals(webClientId, "381939286653-uflnts066ijk9l8v6a462lc9tm9eq83k.apps.googleusercontent.com");
            assertEquals(clientSecret, "h7ZgXo4eJfo07SGwgNRHzeKZ");

            GDConfig.GDAuthConfigData configData = GDConfig.get().getAuthConfigData(appContext);
            assertEquals(webClientId, configData.webClientId);
            assertEquals(clientSecret, configData.clientSecret);
        }

    }

}
