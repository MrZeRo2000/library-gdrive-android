package com.romanpulov.library.gdrive;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;

import java.nio.charset.StandardCharsets;

public class HttpRequestException extends Exception{
    public HttpRequestException(String message) {
        super(message);
    }
    public static HttpRequestException fromVolleyError(VolleyError error) {
        if (error instanceof NoConnectionError) {
            return new HttpRequestException("No internet connection");
        } else if (error.networkResponse != null) {
            return new HttpRequestException(new String(error.networkResponse.data, StandardCharsets.UTF_8));
        } else {
            return new HttpRequestException("Unknown error");
        }
    }
}
