package com.romanpulov.library.gdrive;

import android.app.Activity;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;

public class GDInteractiveAuthenticationAction extends GDAbstractAction<Boolean>{
    private static final String TAG = GDInteractiveAuthenticationAction.class.getSimpleName();

    private final Activity mActivity;

    @Override
    public void execute() {
        try (InputStream inputStream = mActivity.getResources().openRawResource(GDConfig.get().getConfigResId()))
        {

        } catch (IOException e) {
            notifyError(e);
        }

    }

    public GDInteractiveAuthenticationAction(Activity activity, OnGDActionListener<Boolean> gdActionListener) {
        super(gdActionListener);
        this.mActivity = activity;
    }
}
