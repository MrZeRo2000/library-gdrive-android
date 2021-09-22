package com.romanpulov.library.gdrive;

import android.app.Activity;

public class GDLoadAccountAction extends GDAbstractAuthCodeRequiresAction<Void>{
    @Override
    protected void executeWithAuthCode() {
        notifySuccess(null);
    }

    public GDLoadAccountAction(Activity activity, OnGDActionListener<Void> gdActionListener) {
        super(activity, gdActionListener);
    }
}
