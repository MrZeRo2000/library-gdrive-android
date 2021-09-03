package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

import org.json.JSONException;

import java.io.IOException;

public abstract class GDAbstractAuthCodeRequiresAction<D> extends GDAbstractAction<D> {
    private static final String TAG = GDAbstractAuthCodeRequiresAction.class.getSimpleName();

    protected final Activity mActivity;

    public GDAbstractAuthCodeRequiresAction(Activity activity, OnGDActionListener<D> gdActionListener) {
        super(gdActionListener);
        this.mActivity = activity;
    }

    protected abstract void executeWithAuthCode();

    @Override
    public void execute() {
        if (GDAuthData.mAuthCode.get() == null) {
            Log.d(TAG, "Auth code is null, obtaining");

            GDConfig.GDAuthConfigData configData = GDConfig.get().getAuthConfigData(mActivity);

            final GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(new Scope(GDConfig.get().getScope()))
                    .requestServerAuthCode(configData.getWebClientId())
                    .build();

            final GoogleSignInClient client = GoogleSignIn.getClient(mActivity, signInOptions);

            client.silentSignIn()
                    .addOnSuccessListener(account -> {
                        Log.d(TAG, "Silent sign-in success got authCode");
                        GDAuthData.setAuthCode(account.getServerAuthCode());
                        executeWithAuthCode();
                    })
                    .addOnFailureListener(this::notifyError);

        } else {
            executeWithAuthCode();
        }
    }
}
