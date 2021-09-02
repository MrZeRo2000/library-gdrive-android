package com.romanpulov.library.gdrive;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestWrapper {
    private static final String TAG = HttpRequestWrapper.class.getSimpleName();

    public static HttpRequestException getErrorResponseException(VolleyError error) {
        if (error instanceof NoConnectionError) {
            return new HttpRequestException("No internet connection");
        } else if (error.networkResponse != null) {
            return new HttpRequestException(new String(error.networkResponse.data, StandardCharsets.UTF_8));
        } else {
            return new HttpRequestException("Unknown error");
        }
    }

    public static void executePostRequest(
            @NonNull final Context context,
            @NonNull final String url,
            @NonNull final String contentType,
            @NonNull final byte[] body,
            @NonNull final Response.Listener<String> responseListener,
            @NonNull final Response.ErrorListener errorListener) {

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", contentType);
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return body;
            }

        };

        Log.d(TAG, "Adding HTTP POST to Queue, Request: " + request.toString());

        request.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    };
}
