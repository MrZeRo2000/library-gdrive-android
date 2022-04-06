package com.romanpulov.library.gdrive;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

public class GDSilentAuthenticationAction extends GDAbstractAction<String>{

    private final Context mContext;

    public GDSilentAuthenticationAction(Context context, OnGDActionListener<String> gdActionListener) {
        super(gdActionListener);
        this.mContext = context;
    }

    @Override
    public void execute() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
        if (account == null) {
            this.notifyError(new Exception("No account signed in"));
            return;
        }

        GDConfig.GDAuthConfigData configData = GDConfig.get().getAuthConfigData(mContext);

        final GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(GDConfig.get().getScope()))
                        .requestServerAuthCode(configData.getWebClientId())
                        .build();

        final GoogleSignInClient client = GoogleSignIn.getClient(mContext, signInOptions);

        client.silentSignIn()
                .addOnSuccessListener(a -> {
                    notifySuccess(a.getServerAuthCode());

        })
                .addOnFailureListener(this::notifyError);
    }
}
