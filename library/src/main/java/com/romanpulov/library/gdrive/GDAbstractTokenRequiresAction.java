package com.romanpulov.library.gdrive;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;

import java.util.Collections;
import java.util.List;

/**
 * Token requires abstract action
 * @param <D> data type to return
 */
public abstract class GDAbstractTokenRequiresAction<D> extends GDAbstractAction<D>{
    private static final String TAG = GDAbstractTokenRequiresAction.class.getSimpleName();

    protected final Context mContext;
    protected abstract void executeWithToken();

    @Override
    public void execute() {
        if ((GDAuthData.mAccessToken.get() != null) && (!GDAuthData.accessTokenExpired())) {
            Log.d(TAG, "Token already available, executing");
            executeWithToken();
        } else {
            Log.d(TAG, "Token is not available, obtaining");
            GDAuthData.clearAccessToken();

            List<Scope> requestedScopes = Collections.singletonList(new Scope(GDConfig.get().getScope()));

            AuthorizationRequest authorizationRequest = AuthorizationRequest.builder()
                    .setRequestedScopes(requestedScopes)
                    .build();

            Identity.getAuthorizationClient(mContext.getApplicationContext())
                    .authorize(authorizationRequest)
                    .addOnSuccessListener(
                            authorizationResult -> {
                                if (authorizationResult.hasResolution()) {
                                    Log.e(TAG, "Has resolution, access token:" + authorizationResult.getAccessToken());
                                    notifyError(new GDActionException("Not authorized to access resource"));
                                } else {
                                    // Access already granted, continue with user action
                                    Log.d(TAG, "Authorized, got token:" + authorizationResult.getAccessToken());
                                    GDAuthData.mAccessToken.set(authorizationResult.getAccessToken());
                                    executeWithToken();
                                }
                            })
                    .addOnFailureListener(this::notifyError);
        }
    }

    public GDAbstractTokenRequiresAction(Context context, OnGDActionListener<D> gdActionListener) {
        super(gdActionListener);
        this.mContext = context;
    }
}
