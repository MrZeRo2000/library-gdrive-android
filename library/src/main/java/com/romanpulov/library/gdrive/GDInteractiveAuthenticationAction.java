package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.app.PendingIntent;
import android.os.CancellationSignal;
import android.util.Log;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.credentials.*;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class GDInteractiveAuthenticationAction extends GDAbstractAction<Void>{
    private static final int NONCE_LENGTH = 16;
    private static final String TAG = GDInteractiveAuthenticationAction.class.getSimpleName();

    private final Activity mActivity;
    private final ActivityResultLauncher<IntentSenderRequest> mLauncher;

    public static String generateNonce() {
        byte[] nonce = new byte[NONCE_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(nonce);
        return bytesToHex(nonce);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void authorize() {
        List<Scope> requestedScopes = Collections.singletonList(new Scope(GDConfig.get().getScope()));
        AuthorizationRequest authorizationRequest = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)
                .build();
        Identity.getAuthorizationClient(GDInteractiveAuthenticationAction.this.mActivity.getApplicationContext())
                .authorize(authorizationRequest)
                .addOnSuccessListener(
                        authorizationResult -> {
                            if (authorizationResult.hasResolution()) {
                                Log.d(TAG, "authorization has resolution, launching request");
                                PendingIntent pendingIntent = Objects.requireNonNull(authorizationResult.getPendingIntent());
                                IntentSenderRequest intentSenderRequest = new IntentSenderRequest
                                        .Builder(pendingIntent.getIntentSender())
                                        .build();
                                GDInteractiveAuthenticationAction.this.mLauncher.launch(intentSenderRequest);
                            } else {
                                // Access already granted, continue with user action
                                Log.d(TAG, "already authorized");
                                notifySuccess(null);
                            }
                        })
                .addOnFailureListener(this::notifyError);
    }

    @Override
    public void execute() {
        final GDConfig.GDAuthConfigData configData = GDConfig.get().getAuthConfigData(mActivity);

        GetSignInWithGoogleOption signInWithGoogleOption = new GetSignInWithGoogleOption
                .Builder(configData.getWebClientId())
                .setNonce(GDInteractiveAuthenticationAction.generateNonce())
                .build();

        GetCredentialRequest getCredRequest = new GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(mActivity.getApplicationContext());

        credentialManager.getCredentialAsync(
                // Use activity based context to avoid undefined
                // system UI launching behavior
                this.mActivity,
                getCredRequest,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this.mActivity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Log.d(TAG, "Obtained credential, proceeding with authorization");
                        GDInteractiveAuthenticationAction.this.authorize();
                    }

                    @Override
                    public void onError(@NotNull GetCredentialException e) {
                        notifyError(e);
                    }
                });
    }

    public GDInteractiveAuthenticationAction(
            Activity activity,
            OnGDActionListener<Void> gdActionListener) {
        super(gdActionListener);
        this.mActivity = activity;
        this.mLauncher = ((ComponentActivity)activity).registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                intentSenderResult -> {
                    try {
                        Identity
                                .getAuthorizationClient(mActivity.getApplicationContext())
                                .getAuthorizationResultFromIntent(intentSenderResult.getData());
                        notifySuccess(null);
                    } catch (ApiException e) {
                        notifyError(e);
                    }
                });
    }
}
