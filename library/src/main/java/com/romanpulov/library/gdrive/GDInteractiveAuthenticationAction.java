package com.romanpulov.library.gdrive;

import android.app.Activity;
import org.json.JSONException;
import java.io.IOException;

public class GDInteractiveAuthenticationAction extends GDAbstractAction<Boolean>{
    private static final String TAG = GDInteractiveAuthenticationAction.class.getSimpleName();

    private final Activity mActivity;

    @Override
    public void execute() {
        try {
            GDConfig.GDAuthConfigData configData = GDConfig.get().getAuthConfigData(mActivity);

        } catch (IOException | JSONException e) {
            notifyError(e);
        }
    }

    public GDInteractiveAuthenticationAction(Activity activity, OnGDActionListener<Boolean> gdActionListener) {
        super(gdActionListener);
        this.mActivity = activity;
    }
}
