package com.romanpulov.library.gdrive;

import android.content.Context;
import android.os.CancellationSignal;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;

public class GDSignOutAction extends GDAbstractAction<Void>{
    private final Context mContext;

    @Override
    public void execute() {
        ClearCredentialStateRequest clearCredentialStateRequest = new ClearCredentialStateRequest();
        CredentialManager credentialManager = CredentialManager.create(mContext.getApplicationContext());

        credentialManager.clearCredentialStateAsync(
                clearCredentialStateRequest,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onError(@NotNull ClearCredentialException e) {
                        notifyError(e);
                    }

                    @Override
                    public void onResult(Void unused) {
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
