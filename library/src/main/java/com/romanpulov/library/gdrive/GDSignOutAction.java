package com.romanpulov.library.gdrive;

import android.content.Context;
import android.os.CancellationSignal;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import org.jetbrains.annotations.NotNull;

public class GDSignOutAction extends GDAbstractAction<Void>{
    private static final String TAG = GDSignOutAction.class.getSimpleName();
    private final Context mContext;

    @Override
    public void execute() {
        ClearCredentialStateRequest clearCredentialStateRequest = new ClearCredentialStateRequest();
        CredentialManager credentialManager = CredentialManager.create(mContext.getApplicationContext());

        credentialManager.clearCredentialStateAsync(
                clearCredentialStateRequest,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(mContext),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onError(@NotNull ClearCredentialException e) {
                        Log.e(TAG, "Error clearing credential", e);
                        notifyError(e);
                    }

                    @Override
                    public void onResult(Void unused) {
                        Log.d(TAG, "Clear credentials successful");
                        notifySuccess(null);
                    }
                }
        );

    }

    public GDSignOutAction(Context context, OnGDActionListener<Void> gdActionListener) {
        super(gdActionListener);
        mContext = context;
    }
}
