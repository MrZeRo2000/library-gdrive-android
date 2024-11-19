package com.romanpulov.library.gdrive.testapp;

import android.app.PendingIntent;
import android.os.CancellationSignal;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.credentials.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;


import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.romanpulov.library.gdrive.*;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_RECOVER = 10;
    private static final int REQ_ONE_TAP = 2;
    private static final int REQUEST_AUTHORIZE = 20;

    private static final String APPLICATION_NAME = "library-gdrive-testapp";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private RESTDriveService rs;

    private String getWebClientId() {
        String webClientId = "";
        try(InputStream inputStream = getResources().openRawResource(R.raw.gd_config)) {
            String text = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            try {
                JSONObject jo = new JSONObject(text);
                webClientId = jo.getString("web_client_id");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return webClientId;
    }

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

        Button startServiceButton = findViewById(R.id.button_start_service);
        startServiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainService.class);
            startService(intent);
        });

        Button startWorkerButton = findViewById(R.id.button_start_worker);
        Data inputData = (new Data.Builder()).putString("ClassName", this.getClass().getSimpleName()).build();
        startWorkerButton.setOnClickListener(v -> {
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MainWorker.class)
                    .addTag("tag")
                    .setInputData(inputData)
                    .build();
            Operation op = WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("work", ExistingWorkPolicy.KEEP, workRequest);

        });
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

        Button gdLoad = findViewById(R.id.button_gd_load);
        gdLoad.setOnClickListener( v -> {
            GDHelper.getInstance().load(this, new OnGDActionListener<Void>() {
                @Override
                public void onActionSuccess(Void data) {
                    Toast.makeText(MainActivity.this, "Successfully loaded", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onActionFailure(Exception exception) {
                    Toast.makeText(MainActivity.this, "Error loading:" + exception.getMessage(), Toast.LENGTH_SHORT).show();
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

        Button gdCreateReplaceFolder = findViewById(R.id.button_gd_create_replace_folder);
        gdCreateReplaceFolder.setOnClickListener(v -> {
            GDHelper.getInstance();
            new GDGetOrCreateFolderAction(MainActivity.this, "AndroidBackupTest", new OnGDActionListener<String>() {
                @Override
                public void onActionSuccess(String data) {
                    Toast.makeText(MainActivity.this, "Executed successfully, data:" + data.toString(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Executed successfully, data:" + data.toString());
                }

                @Override
                public void onActionFailure(Exception exception) {
                    Toast.makeText(MainActivity.this, "Error:" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Error:" + exception.getMessage());
                }
            }).execute();
        });

        Button gdPutFiles = findViewById(R.id.button_gd_put_files);
        gdPutFiles.setOnClickListener( v -> {
            //generate files
            String[] fileStrings = new String[] {"1-dfwkerkwerw", "2-6,vdfgdfg", "3-gkkkrk444", "4-,fkfkfkdrtrrewwerwer"};
            File[] files = new File[fileStrings.length];

            for (int i = 0; i < fileStrings.length; i++) {
                try {
                    File file = new File(getFilesDir().getAbsolutePath() + File.separator + "testfilename_" + i);
                    try (FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath())) {
                        outputStream.write(fileStrings[i].getBytes(StandardCharsets.UTF_8));
                    }
                    files[i] = file;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            GDHelper.getInstance().putFiles(MainActivity.this, "AndroidBackupFolder", files, new OnGDActionListener<Void>() {
                @Override
                public void onActionSuccess(Void data) {
                    Toast.makeText(MainActivity.this, "Executed successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Executed successfully");
                }

                @Override
                public void onActionFailure(Exception exception) {
                    Toast.makeText(MainActivity.this, "Error:" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Error:" + exception.getMessage());
                }
            });
        });

        Button gdPutFilesSync = findViewById(R.id.button_gd_put_files_sync);
        gdPutFilesSync.setOnClickListener(v -> {
            //generate files
            String[] fileStrings = new String[] {"1-dfwkerkwerw", "2-6,vdfgdfg", "3-gkkkrk444", "4-,fkfkfkdrtrrewwerwer"};
            File[] files = new File[fileStrings.length];

            for (int i = 0; i < fileStrings.length; i++) {
                try {
                    File file = new File(getFilesDir().getAbsolutePath() + File.separator + "testfilename_" + i);
                    try (FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath())) {
                        outputStream.write(fileStrings[i].getBytes(StandardCharsets.UTF_8));
                    }
                    files[i] = file;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    GDHelper.getInstance().putFiles(MainActivity.this, "AndroidBackupFolder", files);
                } catch (GDActionException e) {

                }
            });


        });

        Button gdPutFilesLastAuthenticated = findViewById(R.id.button_gd_put_files_last_auth);
        gdPutFilesLastAuthenticated.setOnClickListener(v -> {

            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    Log.d(TAG, "Before silent log in");
                    GDHelper.getInstance().silentLogin(getApplicationContext());;
                    Log.d(TAG, "Silently logged in, auth code:");
                } catch (GDActionException e) {
                    Log.e(TAG, "Error silent authentication:" + e.getMessage());
                }
            });
            /*
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

            if (account != null) {

                final GoogleSignInOptions signInOptions =
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(new Scope(DriveScopes.DRIVE))
                                .requestServerAuthCode(getString(R.string.default_web_client_id))
                                .build();

                final GoogleSignInClient client = GoogleSignIn.getClient(getApplicationContext(), signInOptions);

                Task<GoogleSignInAccount> task = client.silentSignIn()
                        .addOnSuccessListener(a -> {
                           Log.d(TAG, "silentSignIn success");

                           GDHelper.getInstance().setServerAuthCode(a.getServerAuthCode());

                            //generate files
                            String[] fileStrings = new String[] {"1-dfwkerkwerw", "2-6,vdfgdfg", "3-gkkkrk444", "4-,fkfkfkdrtrrewwerwer"};
                            File[] files = new File[fileStrings.length];

                            for (int i = 0; i < fileStrings.length; i++) {
                                try {
                                    File file = new File(getFilesDir().getAbsolutePath() + File.separator + "testfilename_" + i);
                                    try (FileOutputStream outputStream = new FileOutputStream(file.getAbsolutePath())) {
                                        outputStream.write(fileStrings[i].getBytes(StandardCharsets.UTF_8));
                                    }
                                    files[i] = file;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            GDHelper.getInstance().putFiles(MainActivity.this, "AndroidBackupFolder", files, new OnGDActionListener<Void>() {
                                @Override
                                public void onActionSuccess(Void data) {
                                    Toast.makeText(MainActivity.this, "Executed successfully", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Executed successfully");
                                }

                                @Override
                                public void onActionFailure(Exception exception) {
                                    Toast.makeText(MainActivity.this, "Error:" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Error:" + exception.getMessage());
                                }
                            });

                        })
                        .addOnFailureListener(e ->  {
                            Log.d(TAG, "silentSignIn failure:" + e.getMessage());
                        });

            } else {
                Log.d(TAG, "Error: could not get last signed in account");
            }

             */

        });

        Button gdGetBytesByPath = findViewById(R.id.button_gd_get_bytes_by_path);
        gdGetBytesByPath.setOnClickListener(v -> {
            GDHelper.getInstance().getBytesByPath(MainActivity.this, "AndroidBackupFolder" + File.separator + "testfilename_0", new OnGDActionListener<byte[]>() {
                @Override
                public void onActionSuccess(byte[] data) {
                    Toast.makeText(MainActivity.this, "Executed successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Got the data:" + new String(data, StandardCharsets.UTF_8));
                }

                @Override
                public void onActionFailure(Exception exception) {
                    Toast.makeText(MainActivity.this, "Error:" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Error:" + exception.getMessage());
                }
            });
        });

        Button crLogin = findViewById(R.id.button_cr_login);
        crLogin.setOnClickListener(v -> {
           Log.d(TAG, "Cr login");

           String webClientId = getWebClientId();
           Log.d(TAG, "web_client_id = " + webClientId);
            GetGoogleIdOption googleIdOption = (new GetGoogleIdOption.Builder())
                   .setFilterByAuthorizedAccounts(true)
                   .setServerClientId(webClientId)
                   .setAutoSelectEnabled(true)
                   .setNonce("<nonce string to use when generating a Google ID token>")
                   .build();

            GetSignInWithGoogleOption signInWithGoogleOption = (new GetSignInWithGoogleOption.Builder(webClientId))
                    .setNonce("<nonce string to use when generating a Google ID token>")
                    .build();

            GetCredentialRequest getCredRequest = new GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build();

            CredentialManager credentialManager = CredentialManager.create(getApplicationContext());

            credentialManager.getCredentialAsync(
                    // Use activity based context to avoid undefined
                    // system UI launching behavior
                    this,
                    getCredRequest,
                    new CancellationSignal(),
                    Executors.newSingleThreadExecutor(),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            Log.d(TAG, "onResult: Got the credential:" + result);
                            MainActivity.this.runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Got the credential, requesting scopes ...", Toast.LENGTH_SHORT).show();

                                List<Scope> requestedScopes =  Arrays.asList(new Scope(DriveScopes.DRIVE));
                                AuthorizationRequest authorizationRequest = AuthorizationRequest.builder()
                                        .setRequestedScopes(requestedScopes)
                                        .build();
                                Identity.getAuthorizationClient(MainActivity.this.getApplicationContext())
                                        .authorize(authorizationRequest)
                                        .addOnSuccessListener(
                                                authorizationResult -> {
                                                    if (authorizationResult.hasResolution()) {
                                                        Log.d(TAG, "Has resolution");



                                                        PendingIntent pendingIntent = Objects.requireNonNull(authorizationResult.getPendingIntent());
                                                        IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
                                                        launcher.launch(intentSenderRequest);
                                                        // Access needs to be granted by the user
                                                        /*
                                                        PendingIntent pendingIntent = authorizationResult.getPendingIntent();
                                                        try {
                                                            startIntentSenderForResult(pendingIntent.getIntentSender(),
                                                                    MainActivity.REQUEST_AUTHORIZE, null, 0, 0, 0, null);
                                                        } catch (IntentSender.SendIntentException e) {
                                                            Log.e(TAG, "Couldn't start Authorization UI: " + e.getLocalizedMessage());
                                                        }

                                                         */
                                                    } else {
                                                        // Access already granted, continue with user action
                                                        MainActivity.this.runOnUiThread(() -> {
                                                            Toast.makeText(MainActivity.this, "Already authorized with scopes:" + authorizationResult.getGrantedScopes(), Toast.LENGTH_SHORT).show();
                                                        });
                                                        Log.d(TAG, "Already authorized, token:" + authorizationResult.getAccessToken());
                                                        Log.d(TAG, "Scopes:" + authorizationResult.getGrantedScopes());

                                                        //GDHelper.getInstance().setServerAuthCode(authorizationResult.getAccessToken());
                                                        //saveToDriveAppFolder(authorizationResult);
                                                    }
                                                })
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to authorize", e));
                            });

                        }

                        @Override
                        public void onError(GetCredentialException e) {
                            Log.d(TAG, "onError: " + e.getMessage());
                            //handleFailure(e);
                        }
                    });
        });

        Button crSilentLogin = findViewById(R.id.button_cr_silent_login);
        crSilentLogin.setOnClickListener(v -> {
            List<Scope> requestedScopes =  Arrays.asList(new Scope(DriveScopes.DRIVE));
            AuthorizationRequest authorizationRequest = AuthorizationRequest.builder()
                    .setRequestedScopes(requestedScopes)
                    .build();
            Identity.getAuthorizationClient(MainActivity.this.getApplicationContext())
                    .authorize(authorizationRequest)
                    .addOnSuccessListener(
                            authorizationResult -> {
                                if (authorizationResult.hasResolution()) {
                                    Log.e(TAG, "Has resolution, access token:" + authorizationResult.getAccessToken());
                                } else {
                                    // Access already granted, continue with user action
                                    MainActivity.this.runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, "Already authorized with scope:" + authorizationResult.getGrantedScopes(), Toast.LENGTH_SHORT).show();
                                    });
                                    Log.d(TAG, "Authorized, token:" + authorizationResult.getAccessToken());
                                    GDHelper.getInstance().setServerAccessToken(authorizationResult.getAccessToken());
                                }
                            })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to authorize", e));

        });

        Button crLogout = findViewById(R.id.button_cr_logout);
        crLogout.setOnClickListener(v -> {
            ClearCredentialStateRequest clearCredentialStateRequest = new ClearCredentialStateRequest();
            CredentialManager credentialManager = CredentialManager.create(getApplicationContext());

            credentialManager.clearCredentialStateAsync(
                    clearCredentialStateRequest,
                    new CancellationSignal(),
                    Executors.newSingleThreadExecutor(),
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override
                        public void onError(@NotNull ClearCredentialException e) {
                            Log.d(TAG, "onError: " + e.getMessage());
                        }

                        @Override
                        public void onResult(Void unused) {
                            Log.d(TAG, "onResult: Got the clear credential state");
                            MainActivity.this.runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                            });
                            requestSignOut();
                        }
                    }
            );
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MainActivity.REQUEST_AUTHORIZE) {
            try {
                AuthorizationResult checkAuthorizationResult = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data);
                Toast.makeText(this, "got authorization", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onActivityResult: got authorizationResult with scopes:" + checkAuthorizationResult.getGrantedScopes());

            } catch (ApiException e) {
                Log.e(TAG, e.getMessage());
            }
        }

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

    final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
            intentSenderResult -> {
                AuthorizationResult checkAuthorizationResult = null;
                try {
                    checkAuthorizationResult = Identity
                            .getAuthorizationClient(getApplicationContext())
                            .getAuthorizationResultFromIntent(intentSenderResult.getData());
                    Toast.makeText(this, "got authorization via registerForActivityResult", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onActivityResult: got authorizationResult via registerForActivityResult with scopes:" + checkAuthorizationResult.getGrantedScopes());
                } catch (ApiException e) {
                    Toast.makeText(this, "failed authorization via registerForActivityResult:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            });
}