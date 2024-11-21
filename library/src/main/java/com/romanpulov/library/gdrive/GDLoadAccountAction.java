package com.romanpulov.library.gdrive;

import android.content.Context;

public class GDLoadAccountAction extends GDAbstractTokenRequiresAction<Void>{
    @Override
    protected void executeWithToken() {
        notifySuccess(null);
    }

    public GDLoadAccountAction(Context context, OnGDActionListener<Void> gdActionListener) {
        super(context, gdActionListener);
    }
}
