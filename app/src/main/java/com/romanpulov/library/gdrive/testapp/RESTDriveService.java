package com.romanpulov.library.gdrive.testapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Source: https://stackoverflow.com/questions/54178308/new-google-drive-rest-api-too-much-dependencies
 */
public class RESTDriveService implements OnSuccessListener<GoogleSignInAccount>, OnFailureListener {
    private final static String TAG = RESTDriveService.class.getSimpleName();
    private static final int REQUEST_CODE_SIGN_IN = 1000;

        private static final String LINE_FEED = "\r\n";
        private static final String APP_FOLDER_ID = "appDataFolder";
        private static final String SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata";
    private static final String SCOPE_DRIVE = "https://www.googleapis.com/auth/drive";

        private static final String FILES_REST_URL = "https://www.googleapis.com/drive/v3/files";
        private static final String AUTH_REST_URL = "https://www.googleapis.com/oauth2/v4/token";

        private static final String AUTHORIZATION_PARAM = "Authorization";
        private static final String BEARER_VAL = "Bearer ";
        private static final String CONTENT_TYPE_PARAM = "Content-Type: ";

        private static final String DB_NAME = "prana_breath.sqlite";
        private static final String SQLITE_MIME = "application/x-sqlite3";

        private AppCompatActivity mActivity;

        private int mNextGoogleApiOperation = -1;

        private String mAccessToken;
        private long mTokenExpired;
        private String mAuthCode;

        public RESTDriveService(final AppCompatActivity activity) {
            mActivity = activity;
        }

        public final void disconnect() {
            mActivity = null;
            mNextGoogleApiOperation = -1;
            mAuthCode = null;
            mAccessToken = null;
            mTokenExpired = 0;
        }

        public final void connectAndStartOperation(final int nextOperation) {
            mNextGoogleApiOperation = nextOperation;
            GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(mActivity);
            if (signInAccount == null) {
                Log.d(TAG, "Need to sign in");
                internalSignIn();
            } else {
                final GoogleSignInOptions signInOptions =
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(new Scope(SCOPE_DRIVE))
                                .requestServerAuthCode(mActivity.getString(R.string.default_web_client_id))
                                .build();

                final GoogleSignInClient client = GoogleSignIn.getClient(mActivity, signInOptions);

                Log.d(TAG, "Silently signing in");

                client.silentSignIn().addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        mAuthCode = googleSignInAccount.getServerAuthCode();
                        Log.d(TAG, "Silent sign-in success, authCode:" + mAuthCode);
                        onGoogleDriveConnected(mNextGoogleApiOperation);
                    }
                })
                .addOnFailureListener(e -> Log.d(TAG, "Silent sign-in failure:" + e.getMessage()));
            }
        }

        public final void handleActivityResult(final int requestCode, final Intent data) {
            if (requestCode == REQUEST_CODE_SIGN_IN) {
                GoogleSignIn.getSignedInAccountFromIntent(data)
                        .addOnSuccessListener(this)
                        .addOnFailureListener(this);
            }
        }

    private void internalSignIn() {
        final GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(SCOPE_DRIVE))
                        .requestServerAuthCode(mActivity.getString(R.string.default_web_client_id))
                        .build();

        final GoogleSignInClient client = GoogleSignIn.getClient(mActivity, signInOptions);

        mActivity.startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

//--------------------------------------------------------------------------------------------------
//  Event handlers
//--------------------------------------------------------------------------------------------------

        @Override
        public void onSuccess(GoogleSignInAccount googleAccount) {
            mAuthCode = googleAccount.getServerAuthCode();
//        DebugHelper.log("getServerAuthCode:", googleAccount.getServerAuthCode());

            onGoogleDriveConnected(mNextGoogleApiOperation);
            mNextGoogleApiOperation = -1;
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            mNextGoogleApiOperation = -1;
            Log.d(TAG, "Failure:"+ e.getMessage());
        }

        private void onGoogleDriveConnected(final int operation) {
            Log.d(TAG, "Ready to do something");

            TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
            Executors.newSingleThreadExecutor().submit(() -> {
                readFiles();
                taskCompletionSource.setResult("Completed");
            });
            taskCompletionSource.getTask().addOnSuccessListener(s -> {
                Log.d(TAG, "Success from taskCompletionSource");
            })
            .addOnFailureListener(e -> {
                Log.d(TAG, "Error from taskCompletionSource:" + e.getMessage());
            });

            /*
            switch (operation) {
                case CloudHelper.BACKUP_CODE:
                    onBackupToDriveAsync();
                    break;

                case CloudHelper.RESTORE_CODE:
                    onRestoreFromDriveAsync();
                    break;
            }

             */
        }

//--------------------------------------------------------------------------------------------------
//  Private methods
//--------------------------------------------------------------------------------------------------

        private boolean isRequestInvalid() {
            return mActivity == null;
        }

        @SuppressLint("StaticFieldLeak")
        private void onBackupToDriveAsync() {
            final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... parameters) {
                    writeDbToDrive();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                }
            };
            asyncTask.execute();
        }

        @SuppressLint("StaticFieldLeak")
        private void onRestoreFromDriveAsync() {
            final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... parameters) {
                    readDbFromDrive();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                }
            };
            asyncTask.execute();
        }

    private void readFiles() {
        HttpURLConnection conn = null;

        final String accessToken = requestAccessToken();
        if (accessToken == null || isRequestInvalid()) return;

        try {
            final URL url = new URL("https://www.googleapis.com/drive/v3/files?key=" + mActivity.getString(R.string.client_secret));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            //conn.setDoOutput(true);
            //conn.setDoInput(true);
            conn.setConnectTimeout(5000);
            conn.setRequestProperty(AUTHORIZATION_PARAM, BEARER_VAL + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");

            /////// Prepare data

            //os = conn.read
            final int code = conn.getResponseCode();
            final String msg = conn.getResponseMessage();

            if (code == 200) {
                Log.d(TAG, "Success:" + msg);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                Log.d(TAG, "Content:" + content.toString());

            } else {
                Log.d(TAG, "Failure:"+ msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


        /**
         * https://developers.google.com/drive/api/v3/multipart-upload
         */
        private void writeDbToDrive() {
            HttpURLConnection conn = null;
            OutputStream os = null;

            final String accessToken = requestAccessToken();
            if (accessToken == null || isRequestInvalid()) return;

            try {
                final String boundary = "pb" + System.currentTimeMillis();
                final URL url = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setRequestProperty(AUTHORIZATION_PARAM, BEARER_VAL + accessToken);
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);

                /////// Prepare data
                final String timestamp =  new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.US).format(new Date());
                // Prepare file metadata (Change your backup file name here)
                final StringBuilder b = new StringBuilder();
                b.append('{')
                        .append("\"name\":").append('\"').append("prana_breath_").append(timestamp).append(".db").append('\"').append(',')
                        .append("\"mimeType\":").append("\"application\\/x-sqlite3\"").append(',')
                        .append("\"parents\":").append("[\"").append(APP_FOLDER_ID).append("\"]")
                        .append('}');
                final String metadata = b.toString();
                final byte[] data = readFile(getAppDbFile());

                /////// Calculate body length
                int bodyLength = 0;
                // MetaData part
                b.setLength(0);
                b.append("--").append(boundary).append(LINE_FEED);
                b.append(CONTENT_TYPE_PARAM).append("application/json; charset=UTF-8").append(LINE_FEED);
                b.append(LINE_FEED);
                b.append(metadata).append(LINE_FEED);
                b.append(LINE_FEED);
                b.append("--").append(boundary).append(LINE_FEED);
                b.append(CONTENT_TYPE_PARAM).append(SQLITE_MIME).append(LINE_FEED);
                b.append(LINE_FEED);
                final byte[] beforeFilePart = b.toString().getBytes(StandardCharsets.UTF_8);
                bodyLength += beforeFilePart.length;

                bodyLength += data.length; // File

                b.setLength(0);
                b.append(LINE_FEED);
                b.append("--").append(boundary).append("--");
                final byte[] afterFilePart = b.toString().getBytes(StandardCharsets.UTF_8);
                bodyLength += afterFilePart.length;

                conn.setRequestProperty("Content-Length", String.valueOf(bodyLength));

                /////// Write to socket
                os = conn.getOutputStream();

                os.write(beforeFilePart);
                os.write(data);
                os.write(afterFilePart);
                os.flush();

                final String msg = conn.getResponseMessage();
                final int code = conn.getResponseCode();

                if (code == 200) {
                    Log.d(TAG, "Success");
                } else {
                    Log.d(TAG, "Failure:"+ msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        /**
         * https://developers.google.com/drive/api/v3/manage-downloads
         */
        private void readDbFromDrive() {
            if (isRequestInvalid()) return;

            HttpURLConnection conn = null;
            InputStream is = null;

            final String accessToken = requestAccessToken();
            if (accessToken == null || isRequestInvalid()) return;

            try {
                final String dbFileId = getLatestDbFileIdOnDrive();

                if (isRequestInvalid()) return;

                if (dbFileId == null || dbFileId.length() == 0 || dbFileId.equals("")) {
                    return;
                }

                final String request = FILES_REST_URL + '/' + dbFileId + "?alt=media";
                final URL url = new URL(request);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setRequestProperty(AUTHORIZATION_PARAM, BEARER_VAL + accessToken);

                is = conn.getInputStream();

            } catch (Exception e) {
                Log.d(TAG, "Error:" + e.getMessage());
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        /**
         * https://developers.google.com/drive/api/v3/reference/files/list
         * @return
         */
        private String getLatestDbFileIdOnDrive() {
            HttpURLConnection conn = null;
            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                final StringBuilder b = new StringBuilder();
                b.append(FILES_REST_URL).append('?')
                        .append("spaces=").append(APP_FOLDER_ID).append('&')
                        .append("orderBy=").append(URLEncoder.encode("createdTime desc", "UTF-8")).append('&')
                        .append("pageSize=").append("2");

                final URL url = new URL(b.toString());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setRequestProperty(AUTHORIZATION_PARAM, BEARER_VAL + mAccessToken);

                final int responseCode = conn.getResponseCode();
                if (200 <= responseCode && responseCode <= 299) {
                    is = conn.getInputStream();
                    isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                } else {
                    Log.d(TAG, "Response:" + conn.getResponseMessage());
                    return null;
                /*is = conn.getErrorStream();
                isr = new InputStreamReader(is);
                br = new BufferedReader(isr);*/
                }
                b.setLength(0);
                String output;
                while ((output = br.readLine()) != null) {
                    b.append(output);
                }

                final JSONObject jsonResponse = new JSONObject(b.toString());
                final JSONArray files = jsonResponse.getJSONArray("files");
                if (files.length() == 0) {
                    return null;
                }
                final JSONObject file = files.getJSONObject(0);
                return file.getString("id");
            } catch (Exception e) {
                Log.d(TAG, "Error:" + e.getMessage());
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
                if (isr != null) {
                    try {
                        isr.close();
                    } catch (IOException e) {
                    }
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return null;
        }

        /**
         * https://developers.google.com/identity/protocols/OAuth2WebServer#exchange-authorization-code
         *
         */
        private String requestAccessToken() {
            if (mAccessToken != null && SystemClock.elapsedRealtime() < mTokenExpired) return mAccessToken;
            mTokenExpired = 0;
            mAccessToken = null;

            HttpURLConnection conn = null;
            OutputStream os = null;
            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader br = null;

            try {
                final URL url = new URL(AUTH_REST_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setConnectTimeout(3000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                final StringBuilder b = new StringBuilder();
                b.append("code=").append(mAuthCode).append('&')
                        .append("client_id=").append(mActivity.getString(R.string.default_web_client_id)).append('&')
                        .append("client_secret=").append(mActivity.getString(R.string.client_secret)).append('&')
                        .append("redirect_uri=").append("").append('&')
                        .append("grant_type=").append("authorization_code");

                final byte[] postData = b.toString().getBytes(StandardCharsets.UTF_8);

                os = conn.getOutputStream();
                os.write(postData);

                final int responseCode = conn.getResponseCode();
                if (200 <= responseCode && responseCode <= 299) {
                    is = conn.getInputStream();
                    isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                } else {
                    Log.d(TAG, "Response:" + conn.getResponseMessage());
                    return null;
                }

                b.setLength(0);
                String output;
                while ((output = br.readLine()) != null) {
                    b.append(output);
                }

                final JSONObject jsonResponse = new JSONObject(b.toString());
                mAccessToken = jsonResponse.getString("access_token");
                mTokenExpired = SystemClock.elapsedRealtime() + jsonResponse.getLong("expires_in") * 1000;
                return mAccessToken;
            } catch (Exception e) {
                Log.d(TAG, "Error:" + e.getMessage());
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
                if (isr != null) {
                    try {
                        isr.close();
                    } catch (IOException e) {
                    }
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return null;
        }

        private static byte[] readFile(File file) throws IOException {
            RandomAccessFile f = new RandomAccessFile(file, "r");
            try {
                long longlength = f.length();
                int length = (int) longlength;
                if (length != longlength)
                    throw new IOException("File size >= 10 Mb");

                byte[] data = new byte[length];
                f.readFully(data);
                return data;
            } finally {
                f.close();
            }
        }

        public static void writeStreamToFileOutput(final InputStream src, final FileOutputStream dst) throws IOException {
            try {
                final byte[] buffer = new byte[4 * 1024]; // or other buffer size
                int read;

                while ((read = src.read(buffer)) != -1) {
                    dst.write(buffer, 0, read);
                }

                dst.flush();
            } finally {
                src.close();
                dst.close();
            }
        }

        private static File getAppDbFile() {
            return null;
            //return mActivity.getApplicationContext().getDatabasePath(DB_NAME);
        }
}
