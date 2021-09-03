package com.romanpulov.library.gdrive;

import android.app.Activity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

import org.json.JSONException;
import java.io.IOException;

public class GDInteractiveAuthenticationAction extends GDAbstractAction<Void>{
    private static final String TAG = GDInteractiveAuthenticationAction.class.getSimpleName();

    private final Activity mActivity;

    @Override
    public void execute() {
        GDConfig.GDAuthConfigData configData = GDConfig.get().getAuthConfigData(mActivity);

        final GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(GDConfig.get().getScope()))
                        .requestServerAuthCode(configData.getWebClientId())
                        .build();

        final GoogleSignInClient client = GoogleSignIn.getClient(mActivity, signInOptions);

        mActivity.startActivityForResult(client.getSignInIntent(), GDConfig.get().getAuthRequestCode());
    }

    public GDInteractiveAuthenticationAction(Activity activity, OnGDActionListener<Void> gdActionListener) {
        super(gdActionListener);
        this.mActivity = activity;
    }
}
