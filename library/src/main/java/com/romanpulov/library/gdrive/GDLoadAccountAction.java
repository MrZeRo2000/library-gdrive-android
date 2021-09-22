package com.romanpulov.library.gdrive;

import android.app.Activity;

public class GDLoadAccountAction extends GDAbstractTokenRequiresAction<Void>{
    @Override
    protected void executeWithToken() {
        notifySuccess(null);
    }

    public GDLoadAccountAction(Activity activity, OnGDActionListener<Void> gdActionListener) {
        super(activity, gdActionListener);
    }
}
