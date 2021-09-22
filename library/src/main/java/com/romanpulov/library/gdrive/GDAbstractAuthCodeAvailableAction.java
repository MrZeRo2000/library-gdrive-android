package com.romanpulov.library.gdrive;

import android.content.Context;

public abstract class GDAbstractAuthCodeAvailableAction<D> extends GDAbstractAction<D> {

    protected final Context mContext;

    protected abstract void executeWithAuthCode();

    public GDAbstractAuthCodeAvailableAction(Context context, OnGDActionListener<D> gdActionListener) {
        super(gdActionListener);
        this.mContext = context;
    }

    @Override
    public void execute() {
        if (GDAuthData.mAuthCode.get() == null) {
            notifyError(new GDActionException("Not authenticated"));
        } else {
            executeWithAuthCode();
        }
    }
}
