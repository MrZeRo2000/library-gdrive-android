package com.romanpulov.library.gdrive.testapp;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.romanpulov.library.gdrive.OnGDActionListener;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executors;

    public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_RECOVER = 10;
    private static final int REQ_ONE_TAP = 2;

    private static final String APPLICATION_NAME = "library-gdrive-testapp";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private RESTDriveService rs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rs = new RESTDriveService(this);

        Button signInButton = findViewById(R.id.button_sign_in);
        signInButton.setOnClickListener(v -> {
            oneTapClient = Identity.getSignInClient(this);
            signInRequest = BeginSignInRequest.builder()
                    .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                            .setSupported(true)
                            .build())
                    .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            // Your server's client ID, not your Android client ID.
                            .setServerClientId(getString(R.string.default_web_client_id))
                            // Only show accounts previously used to sign in.
                            .setFilterByAuthorizedAccounts(false)
                            .build())
                    // Automatically sign in when exactly one credential is retrieved.
                    .setAutoSelectEnabled(false)
                    .build();

            oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener(this, new OnSuccessListener<BeginSignInResult>() {
                        @Override
                        public void onSuccess(BeginSignInResult result) {
                            try {
                                startIntentSenderForResult(
                                        result.getPendingIntent().getIntentSender(), REQ_ONE_TAP,
                                        null, 0, 0, 0);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                            }
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // No saved credentials found. Launch the One Tap sign-up flow, or
                            // do nothing and continue presenting the signed-out UI.
                            Log.d(TAG, "OneTapClient signing failure: " + e.getLocalizedMessage());
                        }
                    });
        });

        Button checkSignInButton = findViewById(R.id.button_check_sign_in);
        checkSignInButton.setOnClickListener(v -> {
            GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
            if (signInAccount == null) {
                Log.d(TAG, "signInAccount is null");
            } else {
                Log.d(TAG, "signInAccount is not null!!");
                Log.d(TAG, "Signed in as " + signInAccount.getEmail());

                // Use the authenticated account to sign in to the Drive service.
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                this, Collections.singleton(DriveScopes.DRIVE));
                credential.setSelectedAccount(signInAccount.getAccount());
                Drive googleDriveService =
                        new Drive.Builder(
                                new NetHttpTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName("Drive API Migration")
                                .build();

                // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                // Its instantiation is required before handling any onClick actions.
                DriveServiceHelper driveServiceHelper = new DriveServiceHelper(googleDriveService);

                driveServiceHelper.queryFiles().addOnSuccessListener(new OnSuccessListener<FileList>() {
                    @Override
                    public void onSuccess(FileList fileList) {
                        Log.d(TAG, "Obtained files:" + fileList.getFiles().toString());
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof UserRecoverableAuthIOException) {
                        startActivityForResult(((UserRecoverableAuthIOException)e).getIntent(), REQUEST_CODE_RECOVER);
                    }

                    Log.d(TAG, "Error obtaining files:" + e.getMessage() + ", cause:" + e.getCause().getMessage());
                });
            }
        });

        Button requestSignInButton = findViewById(R.id.button_request_sign_in);
        requestSignInButton.setOnClickListener(v -> {
            requestSignIn();
        });

        Button requestSignOutButton = findViewById(R.id.button_request_sign_out);
        requestSignOutButton.setOnClickListener(v -> {
            GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
            if (signInAccount == null) {
                Toast.makeText(this, "Already signed out", Toast.LENGTH_SHORT).show();
            } else {
                requestSignOut();
            }
        });

        Button restServiceButton = findViewById(R.id.button_rest_service);
        restServiceButton.setOnClickListener(v -> {

            rs.connectAndStartOperation(0);

        });

        setupGDLibraryButtons();
    }

    private void setupGDLibraryButtons() {
        Button gdLogin = findViewById(R.id.button_gd_login);
        gdLogin.setOnClickListener(v -> {
            GDHelper.getInstance().login(this, new OnGDActionListener<Void>() {
                @Override
                public void onActionSuccess(Void data) {
                    //never executed
                }

                @Override
                public void onActionFailure(Exception exception) {
                    Toast.makeText(MainActivity.this, "Error signing in:" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        Button gdLogout = findViewById(R.id.button_gd_logout);
        gdLogout.setOnClickListener( v -> {
            GDHelper.getInstance().logout(this, new OnGDActionListener<Void>() {
                @Override
                public void onActionSuccess(Void data) {
                    Toast.makeText(MainActivity.this, "Successfully signed out", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onActionFailure(Exception exception) {
                    Toast.makeText(MainActivity.this, "Error signing out:" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        Button gdListItems = findViewById(R.id.button_gd_list_items);
        gdListItems.setOnClickListener(v -> {
            GDHelper.getInstance().listItems(this, new OnGDActionListener<JSONObject>() {
                @Override
                public void onActionSuccess(JSONObject data) {
                    Toast.makeText(MainActivity.this, "Got items", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Got some items:" + data.toString());
                }

                @Override
                public void onActionFailure(Exception exception) {
                    Toast.makeText(MainActivity.this, "Error getting items:" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Error getting items:" + exception.getMessage());
                }
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        GDHelper.handleActivityResult(this, requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_ONE_TAP:
                try {
                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                    String idToken = credential.getGoogleIdToken();
                    String username = credential.getId();
                    String password = credential.getPassword();
                    if (idToken !=  null) {
                        // Got an ID token from Google. Use it to authenticate
                        // with your backend.
                        Log.d(TAG, "Got ID token:" + idToken);

                    } else if (password != null) {
                        // Got a saved username and password. Use them to authenticate
                        // with your backend.
                        Log.d(TAG, "Got password.");
                    }

                } catch (ApiException e) {
                    // ...
                    Log.d(TAG, "ApiException:" + e.getLocalizedMessage());
                }
                break;
            case REQUEST_CODE_SIGN_IN:
                Log.d(TAG, "Signed in successfully");

                break;
            case REQUEST_CODE_RECOVER:
                Log.d(TAG, "Recover result:" + data.toString());
                break;
            default:
                rs.handleActivityResult(requestCode, data);
        }
    }

    private void callWithToken(String idToken) {
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
                this,
                "https://www.googleapis.com/drive/v3/files?key=AIzaSyBsyDJPnThE-LwMHb1Bs7MraYsHGqN53Vg",
                idToken,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Got response from Google:" + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error response Google: " + MSGraphRequestWrapper.getErrorResponseBody(error));
                    }
                }
        );
    }

    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE))
                        .requestServerAuthCode(getString(R.string.default_web_client_id))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private void requestSignOut () {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
        /*
        client.revokeAccess().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(TAG, "Revoked access");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Revoke access failure:" + e.getMessage());
                    }
                });


         */
        client.signOut().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(TAG, "Signed out successfully!");
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Signed out error: " + e.getMessage());
            }
        });
    }
}