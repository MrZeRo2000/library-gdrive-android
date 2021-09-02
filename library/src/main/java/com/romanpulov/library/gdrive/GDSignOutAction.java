package com.romanpulov.library.gdrive;

import android.app.Activity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class GDSignOutAction extends GDAbstractAction<Void>{
    private final Activity mActivity;

    @Override
    public void execute() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(mActivity, signInOptions);
        client.revokeAccess()
                .addOnSuccessListener(this::notifySuccess)
                .addOnFailureListener(this::notifyError);
    }

    public GDSignOutAction(Activity activity, OnGDActionListener<Void> gdActionListener) {
        super(gdActionListener);
        mActivity = activity;
    }
}
