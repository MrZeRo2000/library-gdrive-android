package com.romanpulov.library.gdrive.testapp;

import android.app.PendingIntent;
import android.os.CancellationSignal;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.credentials.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.api.services.drive.DriveScopes;
import com.romanpulov.library.gdrive.*;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_RECOVER = 10;
    private static final int REQUEST_AUTHORIZE = 20;

    private RESTDriveService rs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rs = new RESTDriveService(this);

        GDHelper.getInstance().registerActivity(this);

        Button restServiceButton = findViewById(R.id.button_rest_service);
        restServiceButton.setOnClickListener(v -> rs.connectAndStartOperation(0));

        setupCredentialsManagerButtons();
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
            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("work", ExistingWorkPolicy.KEEP, workRequest);

        });
    }

    private void setupCredentialsManagerButtons() {
        Button crLogin = findViewById(R.id.button_cr_login);
        crLogin.setOnClickListener(v -> {
            Log.d(TAG, "Cr login");

            GDConfig.configure(R.raw.gd_config, DriveScopes.DRIVE);

            String webClientId = GDConfig.get().getAuthConfigData(this).getWebClientId();
            Log.d(TAG, "web_client_id = " + webClientId);

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
                    ContextCompat.getMainExecutor(MainActivity.this),
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
                                    Toast.makeText(MainActivity.this, "Already authorized with scope:" + authorizationResult.getGrantedScopes(), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    private void setupGDLibraryButtons() {
        Button gdLogin = findViewById(R.id.button_gd_login);
        gdLogin.setOnClickListener(v -> {
            GDHelper.getInstance().login(new OnGDActionListener<Void>() {
                @Override
                public void onActionSuccess(Void data) {
                    Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
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
                    MainActivity.this.runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Successfully signed out",
                                Toast.LENGTH_SHORT)
                                .show()
                    );
                }

                @Override
                public void onActionFailure(Exception exception) {
                    MainActivity.this.runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "Error signing out:" + exception.getMessage(),
                                    Toast.LENGTH_SHORT)
                                    .show()
                    );
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
        gdListItems.setOnClickListener(v ->
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
            }
        ));

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
        });

        Button gdGetBytesByPath = findViewById(R.id.button_gd_get_bytes_by_path);
        gdGetBytesByPath.setOnClickListener(v ->
                GDHelper.getInstance().getBytesByPath(
                        MainActivity.this,
                        "AndroidBackupFolder" + File.separator + "testfilename_0",
                        new OnGDActionListener<byte[]>() {
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
        }));
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

        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.d(TAG, "Signed in successfully");

                break;
            case REQUEST_CODE_RECOVER:
                Log.d(TAG, "Recover result:" + data);
                break;
            default:
                rs.handleActivityResult(requestCode, data);
        }
    }

    final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
            intentSenderResult -> {
                try {
                    AuthorizationResult checkAuthorizationResult = Identity
                            .getAuthorizationClient(getApplicationContext())
                            .getAuthorizationResultFromIntent(intentSenderResult.getData());
                    Toast.makeText(this, "got authorization via registerForActivityResult", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onActivityResult: got authorizationResult via registerForActivityResult with scopes:" + checkAuthorizationResult.getGrantedScopes());
                } catch (ApiException e) {
                    Toast.makeText(this, "failed authorization via registerForActivityResult:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            });
}